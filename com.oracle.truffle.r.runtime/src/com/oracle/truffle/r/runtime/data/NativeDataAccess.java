/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.data;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.ffi.RFFILog;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

import sun.misc.Unsafe;

abstract class InteropRootNode extends RootNode {
    InteropRootNode() {
        super(RContext.getInstance().getLanguage());
    }

    @Override
    public final SourceSection getSourceSection() {
        return RSyntaxNode.INTERNAL;
    }
}

class UnsafeAdapter {
    public static final Unsafe UNSAFE = initUnsafe();

    private static Unsafe initUnsafe() {
        try {
            return Unsafe.getUnsafe();
        } catch (SecurityException se) {
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                return (Unsafe) theUnsafe.get(Unsafe.class);
            } catch (Exception e) {
                throw new RuntimeException("exception while trying to get Unsafe", e);
            }
        }
    }
}

/**
 * Provides API to work with objects returned by {@link RObject#getNativeMirror()}. The native
 * mirror represents what on the native side is SEXP, but not directly the raw data of the vector.
 * Use {@link #asPointer(Object)} to assign a native mirror object to the given vector. The raw data
 * in native memory for a vector that already has a native mirror object assigned can be allocated
 * using e.g. {@link #allocateNativeContents(RIntVector, int[], int)} .
 *
 * There is a registry of weak references to all native mirrors ever assigned to some vector object.
 * We use the finalizer to free the native memory (if allocated).
 */
public final class NativeDataAccess {
    private NativeDataAccess() {
        // no instances
    }

    public interface CustomNativeMirror {
        long getCustomMirrorAddress();
    }

    public interface Releasable {
        void release();
    }

    private static final boolean TRACE_MIRROR_ALLOCATION_SITES = false;

    private static final long EMPTY_DATA_ADDRESS = 0xBAD;

    private static final ReferenceQueue<Object> nativeRefQueue = new ReferenceQueue<>();

    private static final AtomicReference<Thread> nativeRefQueueThread = new AtomicReference<>(null);

    private static void initNativeRefQueueThread() {
        Thread thread = nativeRefQueueThread.get();
        if (thread == null) {
            thread = new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        while (true) {
                                            Reference<?> ref = nativeRefQueue.remove();
                                            if (ref instanceof Releasable) {
                                                ((Releasable) ref).release();
                                            }
                                        }
                                    } catch (InterruptedException ex) {
                                    }
                                }
                            },
                            "Native-Reference-Queue-Worker");
            if (nativeRefQueueThread.compareAndSet(null, thread)) {
                thread.setDaemon(true);
                thread.start();
            }
        }
    }

    public static ReferenceQueue<Object> nativeReferenceQueue() {
        initNativeRefQueueThread();
        return nativeRefQueue;
    }

    private static final class NativeMirror extends WeakReference<RObject> implements Releasable {
        /**
         * ID of the mirror, this will be used as the value for SEXP. When native up-calls to Java,
         * we get this value and find the corresponding object for it.
         */
        private final long id;
        /**
         * Address of the start of the native memory array. Zero if not allocated yet.
         */
        private long dataAddress;
        /**
         * Length of the native data array. E.g. for CHARSXP this is not just the length of the Java
         * String.
         */
        private long length;

        /**
         * The truly allocated length of the native data array. Expected to be either kept at zero
         * if not used, or to be a value &gt;= length.<br>
         *
         * Expected usage in native code:
         *
         * <pre>
         * SEXP vector = allocVector(STRSXP, 1024);
         * SETLENGTH(newnames, 10);
         * SET_TRUELENGTH(newnames, 1024);
         * </pre>
         */
        private long truelength;

        /**
         * It maintains the <code>1-?</code> relationship between this object and its native wrapper
         * through which the native code accesses it. For instance, Sulong implements the "pointer"
         * equality of two objects that are not pointers (i.e. <code>IS_POINTER</code> returns
         * <code>false</code>) as the reference equality of the objects. It follows that the pointer
         * comparison would fail if the same <code>RObject</code> instance were wrapped by two
         * different native wrappers.
         */
        private Reference<Object> nativeWrapperRef;

        NativeMirror(RObject owner) {
            super(owner, nativeReferenceQueue());
            this.id = counter.addAndGet(2);
            nativeMirrors.put(id, this);
        }

        /**
         * Creates a new mirror with a specified native address as both ID and address. The buffer
         * will be freed when the Java object is collected.
         */
        NativeMirror(RObject ownerVec, long address) {
            // address == 0 means no nativeMirrors registration and no release() call
            super(ownerVec, (address != 0) ? nativeReferenceQueue() : null);
            this.id = address;
            this.dataAddress = address;
            if (address != 0) {
                nativeMirrors.put(id, this);
            }
        }

        @TruffleBoundary
        void allocateNative(Object source, int len, int trueLen, int elementBase, int elementSize) {
            assert dataAddress == 0;
            if (len != 0) {
                dataAddress = allocateNativeMemory(trueLen * elementSize);
                UnsafeAdapter.UNSAFE.copyMemory(source, elementBase, null, dataAddress, trueLen * elementSize);
            } else {
                dataAddress = EMPTY_DATA_ADDRESS;
            }
            this.length = len;

            // ensure that marker address is not used
            assert this.length == 0 || dataAddress != EMPTY_DATA_ADDRESS;
        }

        @TruffleBoundary
        void allocateNativeString(byte[] bytes) {
            assert dataAddress == 0;
            dataAddress = allocateNativeMemory(bytes.length + 1);
            UnsafeAdapter.UNSAFE.copyMemory(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, null, dataAddress, bytes.length);
            UnsafeAdapter.UNSAFE.putByte(dataAddress + bytes.length, (byte) 0); // C strings
                                                                                // terminator
            this.length = bytes.length + 1;

            // ensure that marker address is not used
            assert this.length == 0 || dataAddress != EMPTY_DATA_ADDRESS;
        }

        @TruffleBoundary
        void allocateNative(CharSXPWrapper[] wrappers) {
            long addr = dataAddress = allocateNativeMemory(wrappers.length * Long.BYTES);
            for (int i = 0; i < wrappers.length; i++) {
                UnsafeAdapter.UNSAFE.putLong(addr + i * Long.BYTES, asPointer(wrappers[i]));
            }
        }

        @TruffleBoundary
        void allocateNative(Object[] elements) {
            long addr = dataAddress = allocateNativeMemory(elements.length * Long.BYTES);
            for (int i = 0; i < elements.length; i++) {
                Object element = elements[i];
                if (element instanceof RSequence) {
                    element = ((RSequence) element).createVector();
                }
                UnsafeAdapter.UNSAFE.putLong(addr + i * Long.BYTES, asPointer(element));
            }
        }

        @Override
        public void release() {
            if (id != 0) {
                nativeMirrors.remove(id, this);
            }
            // System.out.println(String.format("gc'ing %16x", id));
            if (dataAddress == EMPTY_DATA_ADDRESS) {
                assert (dataAddress = 0xbadbad) != 0;
            } else if (dataAddress != 0) {
                // System.out.println(String.format("freeing data at %16x", dataAddress));
                freeNativeMemory(dataAddress);
                assert (dataAddress = 0xbadbad) != 0;
            }
            if (nativeMirrorInfo != null) {
                nativeMirrorInfo.remove(id); // Possible id(address)-clashing entries not handled
            }
        }

        @Override
        public String toString() {
            return "mirror:" + dataAddress;
        }
    }

    // The counter is initialized to invalid address and incremented by 2 to always get invalid
    // address value
    private static final AtomicLong counter = new AtomicLong(0xdef000000000001L);
    private static final ConcurrentHashMap<Long, NativeMirror> nativeMirrors = new ConcurrentHashMap<>(512);
    private static final ConcurrentHashMap<Long, RuntimeException> nativeMirrorInfo = TRACE_MIRROR_ALLOCATION_SITES ? new ConcurrentHashMap<>() : null;

    public static CallTarget createIsPointer() {
        return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                return isPointer(ForeignAccess.getReceiver(frame));
            }
        });
    }

    public static boolean isPointer(Object obj) {
        return obj instanceof RObject;
    }

    public static CallTarget createAsPointer() {
        return Truffle.getRuntime().createCallTarget(new InteropRootNode() {
            @Override
            public Object execute(VirtualFrame frame) {
                return asPointer(ForeignAccess.getReceiver(frame));
            }
        });
    }

    /**
     * Assigns a native mirror object to the given RObject object.
     */
    public static long asPointer(Object arg) {
        if (arg instanceof RObject) {
            RObject obj = (RObject) arg;
            NativeMirror mirror = (NativeMirror) obj.getNativeMirror();
            if (mirror == null || mirror.id == 0) {
                mirror = putMirrorObject(arg, obj, mirror);
            }
            return mirror.id;
        }
        throw UnsupportedMessageException.raise(Message.AS_POINTER);
    }

    @TruffleBoundary
    private static NativeMirror putMirrorObject(Object arg, RObject obj, NativeMirror oldMirror) {
        NativeMirror newMirror;
        obj.setNativeMirror(newMirror = arg instanceof CustomNativeMirror ? new NativeMirror(obj, ((CustomNativeMirror) arg).getCustomMirrorAddress()) : new NativeMirror(obj));
        if (oldMirror != null) {
            newMirror.nativeWrapperRef = oldMirror.nativeWrapperRef;
        }
        // System.out.println(String.format("adding %16x = %s", mirror.id,
        // obj.getClass().getSimpleName()));
        if (TRACE_MIRROR_ALLOCATION_SITES) {
            registerAllocationSite(arg, newMirror);
        }
        if (RFFILog.traceEnabled()) {
            RFFILog.printf("NativeMirror:" + newMirror.id + "->" + obj.getClass().getSimpleName() + ',' + obj.hashCode());
        }
        return newMirror;
    }

    @TruffleBoundary
    private static void registerAllocationSite(Object arg, NativeMirror mirror) {
        String argInfo;
        if (arg instanceof RVector<?> && ((RVector<?>) arg).hasNativeMemoryData()) {
            // this must be vector created by fromNative factory method, it has data == null, but
            // does not have its address assigned yet
            argInfo = "[empty]";
        } else {
            argInfo = arg.toString();
        }
        nativeMirrorInfo.put(mirror.id, new RuntimeException(arg.getClass().getSimpleName() + " " + argInfo));
    }

    public static Object toNative(Object obj) {
        assert obj instanceof RObject : "non-RObjects will not be able to provide native pointers";
        return obj;
    }

    /**
     * For given native mirror ID returns the Java side object (vector). TruffleBoundary because it
     * calls into HashMap.
     */
    @TruffleBoundary
    public static Object lookup(long address) {
        NativeMirror nativeMirror = nativeMirrors.get(address);
        if (nativeMirror == null) {
            CompilerDirectives.transferToInterpreter();
            throw reportDataAccessError(address);
        }
        RObject result = nativeMirror.get();
        if (result == null) {
            CompilerDirectives.transferToInterpreter();
            throw reportDataAccessError(address);
        }
        return result;
    }

    private static RuntimeException reportDataAccessError(long address) {
        RuntimeException location = TRACE_MIRROR_ALLOCATION_SITES ? nativeMirrorInfo.get(address) : null;
        printDataAccessErrorLocation(location);
        throw RInternalError.shouldNotReachHere("unknown native reference " + address + "L / 0x" + Long.toHexString(address) + " (current id count: " + Long.toHexString(counter.get()) + ")");
    }

    private static void printDataAccessErrorLocation(RuntimeException location) {
        if (location != null) {
            System.out.println("Location at which the native mirror was allocated:");
            location.printStackTrace();
        }
    }

    // methods operating on the native mirror object directly:

    public static int getIntNativeMirrorData(Object nativeMirror, int index) {
        long address = ((NativeMirror) nativeMirror).dataAddress;
        assert address != 0;
        assert index < ((NativeMirror) nativeMirror).length;
        return UnsafeAdapter.UNSAFE.getInt(address + index * Unsafe.ARRAY_INT_INDEX_SCALE);
    }

    public static double getDoubleNativeMirrorData(Object nativeMirror, int index) {
        long address = ((NativeMirror) nativeMirror).dataAddress;
        assert address != 0;
        assert index < ((NativeMirror) nativeMirror).length;
        return UnsafeAdapter.UNSAFE.getDouble(address + index * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
    }

    public static byte getLogicalNativeMirrorData(Object nativeMirror, int index) {
        long address = ((NativeMirror) nativeMirror).dataAddress;
        assert address != 0;
        assert index < ((NativeMirror) nativeMirror).length;
        return RRuntime.int2logical(UnsafeAdapter.UNSAFE.getInt(address + index * Unsafe.ARRAY_INT_INDEX_SCALE));
    }

    public static byte getRawNativeMirrorData(Object nativeMirror, int index) {
        long address = ((NativeMirror) nativeMirror).dataAddress;
        assert address != 0;
        assert index < ((NativeMirror) nativeMirror).length;
        return UnsafeAdapter.UNSAFE.getByte(address + index * Unsafe.ARRAY_BYTE_INDEX_SCALE);
    }

    public static RComplex getComplexNativeMirrorData(Object nativeMirror, int index) {
        long address = ((NativeMirror) nativeMirror).dataAddress;
        assert address != 0;
        assert index < ((NativeMirror) nativeMirror).length;
        return RComplex.valueOf(UnsafeAdapter.UNSAFE.getDouble(address + index * 2 * Unsafe.ARRAY_DOUBLE_INDEX_SCALE),
                        UnsafeAdapter.UNSAFE.getDouble(address + (index * 2 + 1) * Unsafe.ARRAY_DOUBLE_INDEX_SCALE));
    }

    public static double getComplexNativeMirrorDataR(Object nativeMirror, int index) {
        long address = ((NativeMirror) nativeMirror).dataAddress;
        assert address != 0;
        assert index < ((NativeMirror) nativeMirror).length;
        return UnsafeAdapter.UNSAFE.getDouble(address + index * 2 * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
    }

    public static double getComplexNativeMirrorDataI(Object nativeMirror, int index) {
        long address = ((NativeMirror) nativeMirror).dataAddress;
        assert address != 0;
        assert index < ((NativeMirror) nativeMirror).length;
        return UnsafeAdapter.UNSAFE.getDouble(address + (index * 2 + 1) * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
    }

    public static void setNativeMirrorDoubleData(Object nativeMirror, int index, double value) {
        long address = ((NativeMirror) nativeMirror).dataAddress;
        assert address != 0;
        assert index < ((NativeMirror) nativeMirror).length;
        UnsafeAdapter.UNSAFE.putDouble(address + index * Unsafe.ARRAY_DOUBLE_INDEX_SCALE, value);
    }

    public static void setNativeMirrorRawData(Object nativeMirror, int index, byte value) {
        long address = ((NativeMirror) nativeMirror).dataAddress;
        assert address != 0;
        assert index < ((NativeMirror) nativeMirror).length;
        UnsafeAdapter.UNSAFE.putByte(address + index * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void setNativeMirrorIntData(Object nativeMirror, int index, int value) {
        long address = ((NativeMirror) nativeMirror).dataAddress;
        assert address != 0;
        assert index < ((NativeMirror) nativeMirror).length;
        UnsafeAdapter.UNSAFE.putInt(address + index * Unsafe.ARRAY_INT_INDEX_SCALE, value);
    }

    public static void setNativeMirrorLogicalData(Object nativeMirror, int index, byte logical) {
        long address = ((NativeMirror) nativeMirror).dataAddress;
        assert address != 0;
        assert index < ((NativeMirror) nativeMirror).length;
        UnsafeAdapter.UNSAFE.putInt(address + index * Unsafe.ARRAY_INT_INDEX_SCALE, RRuntime.logical2int(logical));
    }

    public static double[] copyDoubleNativeData(Object mirrorObj) {
        NativeMirror mirror = (NativeMirror) mirrorObj;
        long address = mirror.dataAddress;
        assert address != 0;
        double[] data = new double[(int) mirror.length];
        UnsafeAdapter.UNSAFE.copyMemory(null, address, data, Unsafe.ARRAY_DOUBLE_BASE_OFFSET, data.length * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
        return data;
    }

    public static double[] copyComplexNativeData(Object mirrorObj) {
        NativeMirror mirror = (NativeMirror) mirrorObj;
        long address = mirror.dataAddress;
        assert address != 0;
        double[] data = new double[(int) (mirror.length << 1)];
        UnsafeAdapter.UNSAFE.copyMemory(null, address, data, Unsafe.ARRAY_DOUBLE_BASE_OFFSET, data.length * Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
        return data;
    }

    public static int[] copyIntNativeData(Object mirrorObj) {
        NativeMirror mirror = (NativeMirror) mirrorObj;
        long address = mirror.dataAddress;
        assert address != 0;
        int[] data = new int[(int) mirror.length];
        UnsafeAdapter.UNSAFE.copyMemory(null, address, data, Unsafe.ARRAY_INT_BASE_OFFSET, data.length * Unsafe.ARRAY_INT_INDEX_SCALE);
        return data;
    }

    @TruffleBoundary
    public static byte[] copyByteNativeData(Object mirrorObj) {
        NativeMirror mirror = (NativeMirror) mirrorObj;
        long address = mirror.dataAddress;
        assert address != 0;
        byte[] data = new byte[(int) mirror.length];
        UnsafeAdapter.UNSAFE.copyMemory(null, address, data, Unsafe.ARRAY_BYTE_BASE_OFFSET, data.length * Unsafe.ARRAY_BYTE_INDEX_SCALE);
        return data;
    }

    // methods operating on vectors that may have a native mirror assigned:

    private static final Assumption noIntNative = Truffle.getRuntime().createAssumption();
    private static final Assumption noLogicalNative = Truffle.getRuntime().createAssumption();
    private static final Assumption noDoubleNative = Truffle.getRuntime().createAssumption();
    private static final Assumption noComplexNative = Truffle.getRuntime().createAssumption();
    private static final Assumption noRawNative = Truffle.getRuntime().createAssumption();
    private static final Assumption noCharSXPNative = Truffle.getRuntime().createAssumption();

    static int getData(RIntVector vector, int[] data, int index) {
        if (noIntNative.isValid() || data != null) {
            return data[index];
        } else {
            return getIntNativeMirrorData(vector.getNativeMirror(), index);
        }
    }

    static int getDataLength(RIntVector vector, int[] data) {
        if (noIntNative.isValid() || data != null) {
            return data.length;
        } else {
            return getDataLengthFromMirror(vector.getNativeMirror());
        }
    }

    static void setDataLength(RIntVector vector, int[] data, int length) {
        if (noIntNative.isValid() || data != null) {
            allocateNativeContents(vector, data, length);
        } else {
            ((NativeMirror) vector.getNativeMirror()).length = length;
        }
    }

    static int getTrueDataLength(RIntVector vector) {
        return (int) ((NativeMirror) vector.getNativeMirror()).truelength;
    }

    static void setTrueDataLength(RIntVector vector, int[] data, int truelength) {
        if (noIntNative.isValid() || data != null) {
            allocateNativeContents(vector, data, data.length);
        }
        ((NativeMirror) vector.getNativeMirror()).truelength = truelength;
    }

    static int getDataLengthFromMirror(Object mirror) {
        return (int) ((NativeMirror) mirror).length;
    }

    static void setData(RIntVector vector, int[] data, int index, int value) {
        if (noIntNative.isValid() || data != null) {
            data[index] = value;
        } else {
            long address = ((NativeMirror) vector.getNativeMirror()).dataAddress;
            assert address != 0;
            UnsafeAdapter.UNSAFE.putInt(address + index * Unsafe.ARRAY_INT_INDEX_SCALE, value);
        }
    }

    static byte getData(RLogicalVector vector, byte[] data, int index) {
        if (noLogicalNative.isValid() || data != null) {
            return data[index];
        } else {
            long address = ((NativeMirror) vector.getNativeMirror()).dataAddress;
            assert address != 0;
            return RRuntime.int2logical(UnsafeAdapter.UNSAFE.getInt(address + index * Unsafe.ARRAY_INT_INDEX_SCALE));
        }
    }

    static int getDataLength(RLogicalVector vector, byte[] data) {
        if (noLogicalNative.isValid() || data != null) {
            return data.length;
        } else {
            return (int) ((NativeMirror) vector.getNativeMirror()).length;
        }
    }

    static void setData(RLogicalVector vector, byte[] data, int index, byte value) {
        if (noLogicalNative.isValid() || data != null) {
            data[index] = value;
        } else {
            long address = ((NativeMirror) vector.getNativeMirror()).dataAddress;
            assert address != 0;
            UnsafeAdapter.UNSAFE.putInt(address + index * Unsafe.ARRAY_INT_INDEX_SCALE, RRuntime.logical2int(value));
        }
    }

    static void setDataLength(RLogicalVector vector, byte[] data, int length) {
        if (noLogicalNative.isValid() || data != null) {
            allocateNativeContents(vector, data, length);
        } else {
            ((NativeMirror) vector.getNativeMirror()).length = length;
        }
    }

    static int getTrueDataLength(RLogicalVector vector) {

        return (int) ((NativeMirror) vector.getNativeMirror()).truelength;
    }

    static void setTrueDataLength(RLogicalVector vector, byte[] data, int truelength) {
        if (noLogicalNative.isValid() || data != null) {
            allocateNativeContents(vector, data, data.length);
        }
        ((NativeMirror) vector.getNativeMirror()).truelength = truelength;
    }

    static byte getData(RRawVector vector, byte[] data, int index) {
        if (noRawNative.isValid() || data != null) {
            return data[index];
        } else {
            return getRawNativeMirrorData(vector.getNativeMirror(), index);
        }
    }

    static int getDataLength(RRawVector vector, byte[] data) {
        if (noRawNative.isValid() || data != null) {
            return data.length;
        } else {
            return (int) ((NativeMirror) vector.getNativeMirror()).length;
        }
    }

    static void setDataLength(RRawVector vector, byte[] data, int length) {
        if (noRawNative.isValid() || data != null) {
            allocateNativeContents(vector, data, length);
        } else {
            ((NativeMirror) vector.getNativeMirror()).length = length;
        }
    }

    static int getTrueDataLength(RRawVector vector) {
        return (int) ((NativeMirror) vector.getNativeMirror()).truelength;
    }

    static void setTrueDataLength(RRawVector vector, byte[] data, int truelength) {
        if (noRawNative.isValid() || data != null) {
            allocateNativeContents(vector, data, data.length);
        }
        ((NativeMirror) vector.getNativeMirror()).truelength = truelength;
    }

    static void setData(RRawVector vector, byte[] data, int index, byte value) {
        if (noRawNative.isValid() || data != null) {
            data[index] = value;
        } else {
            long address = ((NativeMirror) vector.getNativeMirror()).dataAddress;
            assert address != 0;
            UnsafeAdapter.UNSAFE.putInt(address + index * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
        }
    }

    static double getData(RDoubleVector vector, double[] data, int index) {
        if (noDoubleNative.isValid() || data != null) {
            return data[index];
        } else {
            return getDoubleNativeMirrorData(vector.getNativeMirror(), index);
        }
    }

    static int getDataLength(RDoubleVector vector, double[] data) {
        if (noDoubleNative.isValid() || data != null) {
            return data.length;
        } else {
            return (int) ((NativeMirror) vector.getNativeMirror()).length;
        }
    }

    static void setDataLength(RDoubleVector vector, double[] data, int length) {
        if (noDoubleNative.isValid() || data != null) {
            allocateNativeContents(vector, data, length);
        } else {
            ((NativeMirror) vector.getNativeMirror()).length = length;
        }
    }

    static int getTrueDataLength(RDoubleVector vector) {
        return (int) ((NativeMirror) vector.getNativeMirror()).truelength;
    }

    static void setTrueDataLength(RDoubleVector vector, double[] data, int truelength) {
        if (noDoubleNative.isValid() || data != null) {
            allocateNativeContents(vector, data, data.length);
        }
        ((NativeMirror) vector.getNativeMirror()).truelength = truelength;
    }

    static void setData(RDoubleVector vector, double[] data, int index, double value) {
        if (noDoubleNative.isValid() || data != null) {
            data[index] = value;
        } else {
            setNativeMirrorDoubleData(vector.getNativeMirror(), index, value);
        }
    }

    static RComplex getData(RComplexVector vector, double[] data, int index) {
        if (noComplexNative.isValid() || data != null) {
            return RComplex.valueOf(data[index * 2], data[index * 2 + 1]);
        } else {
            return getComplexNativeMirrorData(vector.getNativeMirror(), index);
        }
    }

    static double getDataR(RComplexVector vector, double[] data, int index) {
        if (noComplexNative.isValid() || data != null) {
            return data[index * 2];
        } else {
            return getComplexNativeMirrorDataR(vector.getNativeMirror(), index);
        }
    }

    static double getDataI(RComplexVector vector, double[] data, int index) {
        if (noComplexNative.isValid() || data != null) {
            return data[index * 2 + 1];
        } else {
            return getComplexNativeMirrorDataI(vector.getNativeMirror(), index);
        }
    }

    static int getDataLength(RComplexVector vector, double[] data) {
        if (noComplexNative.isValid() || data != null) {
            return data.length >> 1;
        } else {
            return (int) ((NativeMirror) vector.getNativeMirror()).length;
        }
    }

    static void setDataLength(RComplexVector vector, double[] data, int length) {
        if (noComplexNative.isValid() || data != null) {
            allocateNativeContents(vector, data, length);
        } else {
            ((NativeMirror) vector.getNativeMirror()).length = length;
        }
    }

    static int getTrueDataLength(RComplexVector vector) {
        return (int) ((NativeMirror) vector.getNativeMirror()).truelength;
    }

    static void setTrueDataLength(RComplexVector vector, double[] data, int truelength) {
        if (noComplexNative.isValid() || data != null) {
            allocateNativeContents(vector, data, data.length);
        }
        ((NativeMirror) vector.getNativeMirror()).truelength = truelength;
    }

    static void setData(RComplexVector vector, double[] data, int index, double re, double im) {
        if (noComplexNative.isValid() || data != null) {
            data[index * 2] = re;
            data[index * 2 + 1] = im;
        } else {
            long address = ((NativeMirror) vector.getNativeMirror()).dataAddress;
            assert address != 0;
            UnsafeAdapter.UNSAFE.putDouble(address + index * 2 * Unsafe.ARRAY_DOUBLE_INDEX_SCALE, re);
            UnsafeAdapter.UNSAFE.putDouble(address + (index * 2 + 1) * Unsafe.ARRAY_DOUBLE_INDEX_SCALE, im);
        }
    }

    static void setDataLength(RStringVector vector, CharSXPWrapper[] data) {
        ((NativeMirror) vector.getNativeMirror()).length = data.length;
    }

    static int getTrueDataLength(RStringVector vector) {
        return (int) ((NativeMirror) vector.getNativeMirror()).truelength;
    }

    static void setTrueDataLength(RStringVector vector, int truelength) {
        ((NativeMirror) vector.getNativeMirror()).truelength = truelength;
    }

    static int getTrueDataLength(CharSXPWrapper charsxp) {
        return (int) ((NativeMirror) charsxp.getNativeMirror()).truelength;
    }

    static void setTrueDataLength(CharSXPWrapper charsxp, int truelength) {
        ((NativeMirror) charsxp.getNativeMirror()).truelength = truelength;
    }

    static int getTrueDataLength(RListBase list) {
        return (int) ((NativeMirror) list.getNativeMirror()).truelength;
    }

    static void setTrueDataLength(RListBase list, int truelength) {
        ((NativeMirror) list.getNativeMirror()).truelength = truelength;
    }

    static String getData(CharSXPWrapper charSXPWrapper, String data) {
        if (noCharSXPNative.isValid() || data != null) {
            return data;
        } else {
            NativeMirror mirror = (NativeMirror) charSXPWrapper.getNativeMirror();
            long address = mirror.dataAddress;
            assert address != 0;
            int length = 0;
            while (length < mirror.length && UnsafeAdapter.UNSAFE.getByte(address + length) != 0) {
                length++;
            }
            byte[] bytes = new byte[length];
            UnsafeAdapter.UNSAFE.copyMemory(null, address, bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, length);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    static byte getDataAt(CharSXPWrapper charSXPWrapper, byte[] data, int index) {
        if (noCharSXPNative.isValid() || data != null) {
            return data[index];
        } else {
            NativeMirror mirror = (NativeMirror) charSXPWrapper.getNativeMirror();
            long address = mirror.dataAddress;
            assert address != 0;
            assert index < mirror.length;
            return UnsafeAdapter.UNSAFE.getByte(address + index);
        }
    }

    static int getDataLength(CharSXPWrapper charSXPWrapper, byte[] data) {
        if (noCharSXPNative.isValid() || data != null) {
            return data.length;
        } else {
            NativeMirror mirror = (NativeMirror) charSXPWrapper.getNativeMirror();
            long address = mirror.dataAddress;
            assert address != 0;
            int length = 0;
            while (length < mirror.length && UnsafeAdapter.UNSAFE.getByte(address + length) != 0) {
                length++;
            }
            return length;
        }
    }

    static boolean isAllocated(RObject obj) {
        NativeMirror mirror = (NativeMirror) obj.getNativeMirror();
        return mirror != null && mirror.dataAddress != 0;
    }

    static long allocateNativeContents(RLogicalVector vector, byte[] data, int length) {
        NativeMirror mirror = (NativeMirror) vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == 0 ^ data == null : "mirror.dataAddress=" + mirror.dataAddress + ", data=" + data;
        if (mirror.dataAddress == 0) {
            assert mirror.length == 0 && mirror.truelength == 0 : "mirror.length=" + mirror.length + ", mirror.truelength=" + mirror.truelength;
            int[] intArray = new int[data.length];
            for (int i = 0; i < data.length; i++) {
                intArray[i] = RRuntime.logical2int(data[i]);
            }
            noLogicalNative.invalidate();
            mirror.allocateNative(intArray, length, data.length, Unsafe.ARRAY_INT_BASE_OFFSET, Unsafe.ARRAY_INT_INDEX_SCALE);
        }
        return mirror.dataAddress;
    }

    static long allocateNativeContents(RIntVector vector, int[] data, int length) {
        NativeMirror mirror = (NativeMirror) vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == 0 ^ data == null : "mirror.dataAddress=" + mirror.dataAddress + ", data=" + data;
        if (mirror.dataAddress == 0) {
            assert mirror.length == 0 && mirror.truelength == 0 : "mirror.length=" + mirror.length + ", mirror.truelength=" + mirror.truelength;
            noIntNative.invalidate();
            mirror.allocateNative(data, length, data.length, Unsafe.ARRAY_INT_BASE_OFFSET, Unsafe.ARRAY_INT_INDEX_SCALE);
        }
        return mirror.dataAddress;
    }

    static long allocateNativeContents(RRawVector vector, byte[] data, int length) {
        NativeMirror mirror = (NativeMirror) vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == 0 ^ data == null : "mirror.dataAddress=" + mirror.dataAddress + ", data=" + data;
        if (mirror.dataAddress == 0) {
            assert mirror.length == 0 && mirror.truelength == 0 : "mirror.length=" + mirror.length + ", mirror.truelength=" + mirror.truelength;
            noRawNative.invalidate();
            mirror.allocateNative(data, length, data.length, Unsafe.ARRAY_BYTE_BASE_OFFSET, Unsafe.ARRAY_BYTE_INDEX_SCALE);
        }
        return mirror.dataAddress;
    }

    static long allocateNativeContents(RDoubleVector vector, double[] data, int length) {
        NativeMirror mirror = (NativeMirror) vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == 0 ^ data == null : "mirror.dataAddress=" + mirror.dataAddress + ", data=" + data;
        if (mirror.dataAddress == 0) {
            assert mirror.length == 0 && mirror.truelength == 0 : "mirror.length=" + mirror.length + ", mirror.truelength=" + mirror.truelength;
            noDoubleNative.invalidate();
            mirror.allocateNative(data, length, data.length, Unsafe.ARRAY_DOUBLE_BASE_OFFSET, Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
        }
        return mirror.dataAddress;
    }

    static long allocateNativeContents(RComplexVector vector, double[] data, int length) {
        NativeMirror mirror = (NativeMirror) vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == 0 ^ data == null : "mirror.dataAddress=" + mirror.dataAddress + ", data=" + data;
        if (mirror.dataAddress == 0) {
            assert mirror.length == 0 && mirror.truelength == 0 : "mirror.length=" + mirror.length + ", mirror.truelength=" + mirror.truelength;
            noComplexNative.invalidate();
            mirror.allocateNative(data, length, data.length, Unsafe.ARRAY_DOUBLE_BASE_OFFSET, Unsafe.ARRAY_DOUBLE_INDEX_SCALE * 2);
        }
        return mirror.dataAddress;
    }

    static long allocateNativeContents(RStringVector vector, CharSXPWrapper[] charSXPdata) {
        NativeMirror mirror = (NativeMirror) vector.getNativeMirror();
        assert mirror != null;
        if (mirror.dataAddress == 0) {
            // Note: shall the character vector become writeable and not only read-only, we should
            // crate assumption like for other vector types
            mirror.allocateNative(charSXPdata);
        }
        return mirror.dataAddress;
    }

    static long allocateNativeContents(CharSXPWrapper vector, byte[] data) {
        NativeMirror mirror = (NativeMirror) vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == 0 ^ data == null;
        if (mirror.dataAddress == 0) {
            noCharSXPNative.invalidate();
            mirror.allocateNativeString(data);
        }
        return mirror.dataAddress;
    }

    static long allocateNativeContents(RList list, Object[] elements) {
        NativeMirror mirror = (NativeMirror) list.getNativeMirror();
        assert mirror != null;
        if (mirror.dataAddress == 0) {
            // Note: shall the list become writeable and not only read-only, we should
            // crate assumption like for other vector types
            mirror.allocateNative(elements);
        }
        return mirror.dataAddress;
    }

    @TruffleBoundary
    public static long allocateNativeStringArray(String[] data) {
        // We allocate contiguous memory that we'll use to store both the array of pointers (char**)
        // and the arrays of characters (char*). Given vector of size N, we allocate memory for N
        // addresses (long) and after those we put individual strings character by character, the
        // pointers from the first segment of this memory will be pointing to the starts of those
        // strings.
        int length = data.length;
        int size = data.length * Long.BYTES;
        byte[][] bytes = new byte[data.length][];
        for (int i = 0; i < length; i++) {
            String element = data[i];
            bytes[i] = element.getBytes(StandardCharsets.US_ASCII);
            size += bytes[i].length + 1;
        }
        long dataAddress = allocateNativeMemory(size);
        long ptr = dataAddress + length * Long.BYTES; // start of the actual character data
        for (int i = 0; i < length; i++) {
            UnsafeAdapter.UNSAFE.putLong(dataAddress + i * 8, ptr);
            UnsafeAdapter.UNSAFE.copyMemory(bytes[i], Unsafe.ARRAY_BYTE_BASE_OFFSET, null, ptr, bytes[i].length);
            ptr += bytes[i].length;
            UnsafeAdapter.UNSAFE.putByte(ptr++, (byte) 0);
        }
        assert ptr == dataAddress + size : "should have filled everything";
        return dataAddress;
    }

    @TruffleBoundary
    public static String[] releaseNativeStringArray(long address, int length) {
        assert address != 0;
        try {
            String[] data = new String[length];
            for (int i = 0; i < length; i++) {
                long ptr = UnsafeAdapter.UNSAFE.getLong(address + i * 8);
                data[i] = readNativeString(ptr);
            }
            return data;
        } finally {
            freeNativeMemory(address);
        }
    }

    @TruffleBoundary
    public static String readNativeString(long addr) {
        int len;
        for (len = 0; UnsafeAdapter.UNSAFE.getByte(addr + len) != 0; len++) {
        }
        byte[] bytes = new byte[len];
        UnsafeAdapter.UNSAFE.copyMemory(null, addr, bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, len);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    public static void setNativeContents(RObject obj, long address, int length) {
        assert obj.getNativeMirror() != null;
        if (noDoubleNative.isValid() && obj instanceof RDoubleVector) {
            noDoubleNative.invalidate();
        } else if (noComplexNative.isValid() && obj instanceof RComplexVector) {
            noComplexNative.invalidate();
        } else if (noIntNative.isValid() && obj instanceof RIntVector) {
            noIntNative.invalidate();
        } else if (noRawNative.isValid() && obj instanceof RRawVector) {
            noRawNative.invalidate();
        } else if (noLogicalNative.isValid() && obj instanceof RLogicalVector) {
            noLogicalNative.invalidate();
        }
        NativeMirror mirror = (NativeMirror) obj.getNativeMirror();
        mirror.dataAddress = address;
        mirror.length = length;

    }

    public static void setNativeWrapper(RObject obj, Object wrapper) {
        NativeMirror mirror = (NativeMirror) obj.getNativeMirror();
        if (mirror == null) {
            mirror = new NativeMirror(obj, 0);
            obj.setNativeMirror(mirror);
        }
        mirror.nativeWrapperRef = new WeakReference<>(wrapper);
    }

    public static Object getNativeWrapper(RObject obj) {
        NativeMirror mirror = (NativeMirror) obj.getNativeMirror();
        if (mirror == null) {
            return null;
        } else {
            Reference<?> ref = mirror.nativeWrapperRef;
            return (ref != null) ? ref.get() : null;
        }
    }

    private static long allocateNativeMemory(long bytes) {
        long result = UnsafeAdapter.UNSAFE.allocateMemory(bytes);
        // Uncomment for debugging
        // System.out.printf("DEBUG: allocated %x, length %d\n", result, bytes);
        return result;
    }

    private static void freeNativeMemory(long address) {
        // Uncomment for debugging
        // System.out.printf("DEBUG: feeing %x\n", address);
        UnsafeAdapter.UNSAFE.freeMemory(address);
    }
}
