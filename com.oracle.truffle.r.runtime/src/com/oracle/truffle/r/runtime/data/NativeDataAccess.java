/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.data;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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

    private static final boolean TRACE_MIRROR_ALLOCATION_SITES = false;

    private static final long EMPTY_DATA_ADDRESS = 0xBAD;

    public static boolean isNativeMirror(Object o) {
        return o instanceof NativeMirror;
    }

    private static final class NativeMirror {
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

        NativeMirror() {
            this.id = counter.incrementAndGet();
        }

        /**
         * Creates a new mirror with a specified native address as both ID and address. The buffer
         * will be freed when the Java object is collected.
         */
        NativeMirror(long address) {
            this.id = address;
            this.dataAddress = address;
        }

        @TruffleBoundary
        void allocateNative(Object source, int len, int elementBase, int elementSize) {
            assert dataAddress == 0;
            if (len != 0) {
                dataAddress = UnsafeAdapter.UNSAFE.allocateMemory(len * elementSize);
                UnsafeAdapter.UNSAFE.copyMemory(source, elementBase, null, dataAddress, len * elementSize);
            } else {
                dataAddress = EMPTY_DATA_ADDRESS;
            }
            this.length = len;

            // ensure that marker address is not used
            assert this.length == 0 || dataAddress != EMPTY_DATA_ADDRESS;
        }

        @TruffleBoundary
        void allocateNative(String source) {
            assert dataAddress == 0;
            byte[] bytes = source.getBytes(StandardCharsets.US_ASCII);
            dataAddress = UnsafeAdapter.UNSAFE.allocateMemory(bytes.length + 1);
            UnsafeAdapter.UNSAFE.copyMemory(bytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, null, dataAddress, bytes.length);
            UnsafeAdapter.UNSAFE.putByte(dataAddress + bytes.length, (byte) 0); // C strings
                                                                                // terminator
            this.length = bytes.length + 1;

            // ensure that marker address is not used
            assert this.length == 0 || dataAddress != EMPTY_DATA_ADDRESS;
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            nativeMirrors.remove(id);
            // System.out.println(String.format("gc'ing %16x", id));
            if (dataAddress == EMPTY_DATA_ADDRESS) {
                assert (dataAddress = 0xbadbad) != 0;
            } else if (dataAddress != 0) {
                // System.out.println(String.format("freeing data at %16x", dataAddress));
                UnsafeAdapter.UNSAFE.freeMemory(dataAddress);
                assert (dataAddress = 0xbadbad) != 0;
            }
        }
    }

    private static final AtomicLong counter = new AtomicLong(0xdef000000000000L);
    private static final ConcurrentHashMap<Long, WeakReference<RObject>> nativeMirrors = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, RuntimeException> nativeMirrorInfo = new ConcurrentHashMap<>();

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
            if (mirror == null) {
                mirror = putMirrorObject(arg, obj);
            }
            return mirror.id;
        }
        throw UnsupportedMessageException.raise(Message.AS_POINTER);
    }

    @TruffleBoundary
    private static NativeMirror putMirrorObject(Object arg, RObject obj) {
        NativeMirror mirror;
        obj.setNativeMirror(mirror = arg instanceof CustomNativeMirror ? new NativeMirror(((CustomNativeMirror) arg).getCustomMirrorAddress()) : new NativeMirror());
        // System.out.println(String.format("adding %16x = %s", mirror.id,
        // obj.getClass().getSimpleName()));
        nativeMirrors.put(mirror.id, new WeakReference<>(obj));
        if (TRACE_MIRROR_ALLOCATION_SITES) {
            registerAllocationSite(arg, mirror);
        }
        return mirror;
    }

    @TruffleBoundary
    private static void registerAllocationSite(Object arg, NativeMirror mirror) {
        String argInfo;
        if (arg instanceof RVector<?> && ((RVector) arg).hasNativeMemoryData()) {
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
        WeakReference<RObject> reference = nativeMirrors.get(address);
        if (reference == null) {
            CompilerDirectives.transferToInterpreter();
            throw reportDataAccessError(address);
        }
        RObject result = reference.get();
        if (result == null) {
            CompilerDirectives.transferToInterpreter();
            throw reportDataAccessError(address);
        }
        return result;
    }

    private static RuntimeException reportDataAccessError(long address) {
        RuntimeException location = nativeMirrorInfo.get(address);
        if (location != null) {
            System.out.println("Location at which the native mirror was allocated:");
            location.printStackTrace();
        }
        throw RInternalError.shouldNotReachHere("unknown native reference " + address + "L / 0x" + Long.toHexString(address) + " (current id count: " + Long.toHexString(counter.get()) + ")");
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

    public static void setNativeMirrorData(Object nativeMirror, int index, double value) {
        long address = ((NativeMirror) nativeMirror).dataAddress;
        assert address != 0;
        assert index < ((NativeMirror) nativeMirror).length;
        UnsafeAdapter.UNSAFE.putDouble(address + index * Unsafe.ARRAY_DOUBLE_INDEX_SCALE, value);
    }

    public static void setNativeMirrorData(Object nativeMirror, int index, byte value) {
        long address = ((NativeMirror) nativeMirror).dataAddress;
        assert address != 0;
        assert index < ((NativeMirror) nativeMirror).length;
        UnsafeAdapter.UNSAFE.putByte(address + index * Unsafe.ARRAY_BYTE_INDEX_SCALE, value);
    }

    public static void setNativeMirrorData(Object nativeMirror, int index, int value) {
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

    public static int[] copyIntNativeData(Object mirrorObj) {
        NativeMirror mirror = (NativeMirror) mirrorObj;
        long address = mirror.dataAddress;
        assert address != 0;
        int[] data = new int[(int) mirror.length];
        UnsafeAdapter.UNSAFE.copyMemory(null, address, data, Unsafe.ARRAY_INT_BASE_OFFSET, data.length * Unsafe.ARRAY_INT_INDEX_SCALE);
        return data;
    }

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
    private static final Assumption noCahrSXPNative = Truffle.getRuntime().createAssumption();

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
            return (int) ((NativeMirror) vector.getNativeMirror()).length;
        }
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

    static void setData(RDoubleVector vector, double[] data, int index, double value) {
        if (noDoubleNative.isValid() || data != null) {
            data[index] = value;
        } else {
            setNativeMirrorData(vector.getNativeMirror(), index, value);
        }
    }

    static RComplex getData(RComplexVector vector, double[] data, int index) {
        if (noComplexNative.isValid() || data != null) {
            return RComplex.valueOf(data[index * 2], data[index * 2 + 1]);
        } else {
            return getComplexNativeMirrorData(vector.getNativeMirror(), index);
        }
    }

    static int getDataLength(RComplexVector vector, double[] data) {
        if (noComplexNative.isValid() || data != null) {
            return data.length >> 1;
        } else {
            return (int) ((NativeMirror) vector.getNativeMirror()).length;
        }
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

    static String getData(CharSXPWrapper vector, String data) {
        if (noCahrSXPNative.isValid() || data != null) {
            return data;
        } else {
            NativeMirror mirror = (NativeMirror) vector.getNativeMirror();
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

    static long allocateNativeContents(RLogicalVector vector, byte[] data, int length) {
        NativeMirror mirror = (NativeMirror) vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == 0 ^ data == null;
        if (mirror.dataAddress == 0) {
            int[] intArray = new int[data.length];
            for (int i = 0; i < data.length; i++) {
                intArray[i] = RRuntime.logical2int(data[i]);
            }
            noLogicalNative.invalidate();
            mirror.allocateNative(intArray, length, Unsafe.ARRAY_INT_BASE_OFFSET, Unsafe.ARRAY_INT_INDEX_SCALE);
        }
        return mirror.dataAddress;
    }

    static long allocateNativeContents(RIntVector vector, int[] data, int length) {
        NativeMirror mirror = (NativeMirror) vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == 0 ^ data == null;
        if (mirror.dataAddress == 0) {
            noIntNative.invalidate();
            mirror.allocateNative(data, length, Unsafe.ARRAY_INT_BASE_OFFSET, Unsafe.ARRAY_INT_INDEX_SCALE);
        }
        return mirror.dataAddress;
    }

    static long allocateNativeContents(RRawVector vector, byte[] data, int length) {
        NativeMirror mirror = (NativeMirror) vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == 0 ^ data == null;
        if (mirror.dataAddress == 0) {
            noRawNative.invalidate();
            mirror.allocateNative(data, length, Unsafe.ARRAY_BYTE_BASE_OFFSET, Unsafe.ARRAY_BYTE_INDEX_SCALE);
        }
        return mirror.dataAddress;
    }

    static long allocateNativeContents(RDoubleVector vector, double[] data, int length) {
        NativeMirror mirror = (NativeMirror) vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == 0 ^ data == null;
        if (mirror.dataAddress == 0) {
            noDoubleNative.invalidate();
            mirror.allocateNative(data, length, Unsafe.ARRAY_DOUBLE_BASE_OFFSET, Unsafe.ARRAY_DOUBLE_INDEX_SCALE);
        }
        return mirror.dataAddress;
    }

    static long allocateNativeContents(RComplexVector vector, double[] data, int length) {
        NativeMirror mirror = (NativeMirror) vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == 0 ^ data == null;
        if (mirror.dataAddress == 0) {
            noComplexNative.invalidate();
            mirror.allocateNative(data, length, Unsafe.ARRAY_DOUBLE_BASE_OFFSET, Unsafe.ARRAY_DOUBLE_INDEX_SCALE * 2);
        }
        return mirror.dataAddress;
    }

    static long allocateNativeContents(CharSXPWrapper vector, String contents) {
        NativeMirror mirror = (NativeMirror) vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == 0 ^ contents == null;
        if (mirror.dataAddress == 0) {
            noCahrSXPNative.invalidate();
            mirror.allocateNative(contents);
        }
        return mirror.dataAddress;
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
}
