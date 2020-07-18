/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.FastROptions;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.NativeDataAccessFactory.ToNativeNodeGen;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.ShareObjectNode;
import com.oracle.truffle.r.runtime.ffi.FFIMaterializeNode;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.ElementType;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.NativeMemoryWrapper;
import com.oracle.truffle.r.runtime.ffi.util.ResourcesCleaner.ReleasableWeakReference;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import static com.oracle.truffle.r.runtime.RLogger.LOGGER_RFFI;

/**
 * Provides API to work with objects returned by {@link RBaseObject#getNativeMirror()}. The native
 * mirror represents what on the native side is SEXP, but not directly the raw data of the vector.
 * Use {@link #toNative(RBaseObject)} to assign a native mirror object to the given vector. The raw
 * data in native memory for a vector that already has a native mirror object assigned can be
 * allocated using e.g. {@link #allocateNativeContents(RIntVector, int[], int)} .
 * <p>
 * There is a registry of weak references to all native mirrors ever assigned to some vector object.
 * We use the finalizer to free the native memory (if allocated).
 */
public final class NativeDataAccess {
    private NativeDataAccess() {
        // no instances
    }

    /**
     * Objects implementing this interface can provide custom pointer that will be used as the SEXP
     * opaque pointer. This value is also used for the data-pointer and it will be cleaned
     * automatically, i.e., the implementor is not responsible for freeing the memory.
     */
    public interface CustomNativeMirror {
        long getCustomMirrorAddress();
    }

    /**
     * @see RLogger#LOGGER_RFFI
     */
    private static final TruffleLogger LOGGER = RLogger.getLogger(LOGGER_RFFI);

    private static final boolean TRACE_MIRROR_ALLOCATION_SITES = false;

    private static final AtomicLong emptyDataAddress;

    static {
        emptyDataAddress = new AtomicLong(0);
    }

    private static long getEmptyDataAddress() {
        long addr = emptyDataAddress.get();
        if (addr == 0L) {
            addr = NativeMemory.allocate(8, "getEmptyDataAddress");
            if (!emptyDataAddress.compareAndSet(0L, addr)) {
                NativeMemory.free(addr, "getEmptyDataAddress compareAndSet");
            }
        }
        return emptyDataAddress.get();
    }

    /**
     * Wraps a handle (number) reserved for a {@link NativeMirror} that escaped to native memory.
     * Takes care of putting and removing the mapping for that handle from {@link #nativeMirrors}.
     */
    private static final class NativeHandleWrapper extends ReleasableWeakReference<RBaseObject> {
        private final long id;

        private NativeHandleWrapper(long id, RBaseObject referent) {
            super(referent);
            this.id = id;
            addToMirrors(id);
        }

        @TruffleBoundary
        private void addToMirrors(long thisId) {
            nativeMirrors.put(thisId, this);
        }

        public long getId() {
            return id;
        }

        @Override
        public void release() {
            if (nativeMirrorInfo != null) {
                // Possible id(address)-clashing entries not handled, this is for debugging purposes
                // anyway
                nativeMirrorInfo.remove(id);
            }
            nativeMirrors.remove(id, this);
        }
    }

    /**
     * Native mirror represents a {@code SEXP}, opaque pointer to an R object, passed to the native
     * code. Native mirror wraps FastR objects {@link RBaseObject} and alters the interop protocol
     * for the purposes of making them look like actual {@code SEXP} to the native extensions run
     * either via NFI or LLVM.
     * <p>
     * When native mirror leaks to actual native code, we create a handle for it (number) and put it
     * into a hash map. Once a native code returns a value or calls back to Java passing some
     * arguments, we convert the handles back to the NativeMirror object and to the corresponding
     * {@link RBaseObject}.
     * <p>
     * For now, native mirror also holds reference to native memory allocated for "nativized"
     * vectors. See {@link com.oracle.truffle.r.runtime.ffi.RObjectDataPtr} for more details.
     */
    @ExportLibrary(InteropLibrary.class)
    public static final class NativeMirror implements TruffleObject {
        /**
         * Artificial member accessible via the Truffle interop that allows to inspect the wrapped R
         * object in tools.
         */
        private static final String R_OBJ_MEMBER_NAME = "r_obj";
        /**
         * Wrapped R object.
         */
        private final RBaseObject delegate;
        /**
         * ID of the mirror, this will be used as the value for SEXP. When native up-calls to Java,
         * we get this value and find the corresponding object for it.
         */
        private NativeHandleWrapper nativeHandle;
        /**
         * Address of the start of the native memory array. Zero if not allocated yet.
         */
        private NativeMemoryWrapper dataAddress;
        /**
         * Length of the native data array in terms of the items stored in the vector. There are two
         * situations where this is not straightforward: for complex vectors this is the count of
         * complex numbers, and for CHARSXP this is the length of the Java string + one for the
         * terminating byte.
         */
        private long length;

        /**
         * The truly allocated length of the native data array. Expected to be either kept at zero
         * if not used, or to be a value &gt;= length.<br>
         * <p>
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
         * comparison would fail if the same <code>RBaseObject</code> instance were wrapped by two
         * different native wrappers.
         */
        private NativeWrapperReference nativeWrapperRef;

        /**
         * Creates a new mirror with a specified native address as both ID and address. The buffer
         * will be freed when the Java object is collected.
         */
        NativeMirror(RBaseObject ownerVec, long address) {
            assert !(ownerVec instanceof RForeignObjectWrapper);
            delegate = ownerVec;
            if (address != 0) {
                this.nativeHandle = new NativeHandleWrapper(address, ownerVec);
                setDataAddress(address);
            }
        }

        public RBaseObject getDelegate() {
            return delegate;
        }

        public long getDataAddress() {
            return dataAddress == null ? 0L : dataAddress.getAddress();
        }

        @TruffleBoundary
        private void initMirror() {
            assert nativeHandle == null;
            long id = counter.addAndGet(2);
            nativeHandle = new NativeHandleWrapper(id, delegate);
        }

        @TruffleBoundary
        private void initMirror(long address) {
            assert nativeHandle == null;
            assert address != 0;
            this.nativeHandle = new NativeHandleWrapper(address, delegate);
            setDataAddress(address);
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        public boolean hasMembers() {
            return true;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        public Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
            return new String[]{R_OBJ_MEMBER_NAME};
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        public boolean isMemberReadable(String name) {
            return R_OBJ_MEMBER_NAME.equals(name);
        }

        @ExportMessage
        public Object readMember(String name) throws UnsupportedMessageException {
            if (R_OBJ_MEMBER_NAME.equals(name)) {
                return delegate;
            }
            throw UnsupportedMessageException.create();
        }

        @ExportMessage
        public boolean isPointer() {
            return nativeHandle != null;
        }

        @ExportMessage
        public long asPointer(@Cached("createBinaryProfile()") ConditionProfile isPointer) throws UnsupportedMessageException {
            if (isPointer.profile(isPointer())) {
                return nativeHandle.getId();
            }
            throw UnsupportedMessageException.create();
        }

        @ExportMessage
        public void toNative(@Cached() ToNativeNode toNative) {
            toNative.execute(this);
        }

        public void setExternalDataAddress(long address) {
            this.dataAddress = NativeMemory.wrapExternalNativeMemory(address, delegate);
            if (dataAddressToNativeMirrors != null) {
                addToAddressDebugMapping(address);
            }
        }

        NativeMemoryWrapper setDataAddress(long address) {
            // use setExternalDataAddress for empty data address
            assert address != getEmptyDataAddress();
            this.dataAddress = NativeMemory.wrapNativeMemory(address, delegate);
            if (dataAddressToNativeMirrors != null) {
                addToAddressDebugMapping(address);
            }
            return dataAddress;
        }

        @TruffleBoundary
        private void addToAddressDebugMapping(long address) {
            dataAddressToNativeMirrors.put(address, this);
        }

        @TruffleBoundary
        void allocateNative(Object source, int vectorLength, long elementsCount, ElementType type) {
            assert getDataAddress() == 0;
            if (vectorLength != 0) {
                setDataAddress(NativeMemory.allocate(type, elementsCount, source));
                NativeMemory.copyMemory(source, dataAddress, type, elementsCount);
            } else {
                setExternalDataAddress(getEmptyDataAddress());
            }
            this.length = vectorLength;

            // ensure that marker address is not used
            assert this.length == 0 || dataAddress.getAddress() != getEmptyDataAddress();
        }

        @TruffleBoundary
        void initializeAltrep(RBaseObject altrepVec, long address, int altrepLength) {
            assert altrepVec.isAltRep();
            assert address != 0;
            assert dataAddress == null;
            this.length = altrepLength;
            setExternalDataAddress(address);
        }

        @TruffleBoundary
        void allocateNativeString(byte[] bytes) {
            assert getDataAddress() == 0;
            setDataAddress(NativeMemory.allocate(bytes.length + 1L, "NativeString"));
            NativeMemory.copyMemory(bytes, dataAddress, ElementType.BYTE, bytes.length);
            // append C strings termination
            NativeMemory.putByte(dataAddress, bytes.length, (byte) 0);
            this.length = bytes.length + 1;

            // ensure that marker address is not used
            assert dataAddress.getAddress() != getEmptyDataAddress();
        }

        @TruffleBoundary
        void allocateNative(CharSXPWrapper[] wrappers) {
            if (wrappers.length == 0) {
                setExternalDataAddress(getEmptyDataAddress());
            } else {
                NativeMemoryWrapper addr = setDataAddress(NativeMemory.allocate(wrappers.length * (long) Long.BYTES, "CharSXPWrapper"));
                for (int i = 0; i < wrappers.length; i++) {
                    NativeMemory.putLong(addr, i, getPointer(wrappers[i]));
                }
            }
        }

        @TruffleBoundary
        void allocateNative(Object[] elements) {
            if (elements.length == 0) {
                setExternalDataAddress(getEmptyDataAddress());
            } else {
                NativeMemoryWrapper addr = setDataAddress(NativeMemory.allocate(elements.length * (long) Long.BYTES, "SEXP array"));
                for (int i = 0; i < elements.length; i++) {
                    Object element = elements[i];
                    Object materialized = FFIMaterializeNode.uncachedMaterialize(element);
                    if (element != materialized) {
                        elements[i] = ShareObjectNode.executeUncached(materialized);
                    }
                    if (materialized instanceof RBaseObject) {
                        NativeMemory.putLong(addr, i, getPointer((RBaseObject) materialized));
                    } else {
                        throw RInternalError.shouldNotReachHere(materialized == null ? "null" : materialized.getClass().getSimpleName());
                    }
                }
            }
        }

        @Override
        public String toString() {
            long id = nativeHandle == null ? 0 : nativeHandle.getId();
            return "mirror: address=" + Long.toHexString(getDataAddress()) + ", id=" + Long.toHexString(id);
        }
    }

    // The map from handles sent to the native code to the RBaseObjects they represent
    // The counter is initialized to invalid address and incremented by 2 to always get invalid
    // address value
    private static final AtomicLong counter = new AtomicLong(0xdef000000000001L);
    private static final ConcurrentHashMap<Long, NativeHandleWrapper> nativeMirrors = new ConcurrentHashMap<>(512);

    // For debugging purposes:
    private static final ConcurrentHashMap<Long, NativeMirror> dataAddressToNativeMirrors = System.getenv(FastROptions.NATIVE_DATA_INSPECTOR) != null ? new ConcurrentHashMap<>(512) : null;
    private static final ConcurrentHashMap<Long, RuntimeException> nativeMirrorInfo = TRACE_MIRROR_ALLOCATION_SITES ? new ConcurrentHashMap<>() : null;

    public static NativeMirror createNativeMirror(RBaseObject obj) {
        assert obj.getNativeMirror() == null;
        NativeMirror mirror = new NativeMirror(obj, 0);
        obj.setNativeMirror(mirror);
        return mirror;
    }

    /**
     * Assigns a native mirror object ID to the given RBaseObject object.
     */
    public static void toNative(RBaseObject obj) {
        NativeMirror mirror = obj.getNativeMirror();
        if (mirror == null) {
            createNativeMirror(obj);
            mirror = obj.getNativeMirror();
        }
        ToNativeNodeGen.getUncached().execute(mirror);
    }

    @ImportStatic(NativeDataAccess.class)
    @GenerateUncached
    abstract static class ToNativeNode extends Node {
        abstract void execute(NativeMirror mirror);

        @Specialization
        void toNative(NativeMirror mirror,
                        @Cached("createBinaryProfile()") ConditionProfile hasID,
                        @Cached("createBinaryProfile()") ConditionProfile isCustomNativeMirror,
                        @Cached("createBinaryProfile()") ConditionProfile isInNative,
                        @Cached("createBinaryProfile()") ConditionProfile registerNativeRefNopProfile,
                        @Cached BranchProfile refRegProfile,
                        @CachedContext(TruffleRLanguage.class) ContextReference<RContext> ctxRef) {
            if (hasID.profile(mirror.nativeHandle == null)) {
                RBaseObject obj = mirror.delegate;
                if (isCustomNativeMirror.profile(obj instanceof CustomNativeMirror)) {
                    mirror.initMirror(((CustomNativeMirror) obj).getCustomMirrorAddress());
                } else {
                    mirror.initMirror();
                }
                RContext rContext = ctxRef.get();
                if (isInNative.profile(rContext.getStateRFFI().getCallDepth() > 0)) {
                    rContext.getStateRFFI().registerReferenceUsedInNative(obj, registerNativeRefNopProfile, refRegProfile);
                }
                assert mirror.nativeHandle != null && mirror.nativeHandle.getId() != 0;
                logAndTrace(mirror.delegate, mirror);
            }
        }

    }

    private static long getPointer(RBaseObject obj) {
        toNative(obj);
        return obj.getNativeMirror().nativeHandle.getId();
    }

    @TruffleBoundary
    private static void logAndTrace(RBaseObject obj, NativeMirror mirror) {
        if (TRACE_MIRROR_ALLOCATION_SITES) {
            registerAllocationSite(obj, mirror);
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            long id = mirror.nativeHandle == null ? 0 : mirror.nativeHandle.getId();
            LOGGER.log(Level.FINE, "NativeMirror: {0}->{1} ({2})", new Object[]{Long.toHexString(id), obj.getClass().getSimpleName(), Utils.getDebugInfo(obj)});
        }
    }

    @TruffleBoundary
    private static void registerAllocationSite(Object arg, NativeMirror mirror) {
        String argInfo;
        if (RRuntime.isMaterializedVector(arg) && ((RAbstractVector) arg).hasNativeMemoryData()) {
            // this must be vector created by fromNative factory method, it has data == null, but
            // does not have its address assigned yet
            argInfo = "[empty]";
        } else {
            argInfo = arg.toString();
        }
        nativeMirrorInfo.put(mirror.nativeHandle.getId(), new RuntimeException(arg.getClass().getSimpleName() + " " + argInfo));
    }

    /**
     * For given native mirror ID returns the Java side object (vector). TruffleBoundary because it
     * calls into HashMap.
     */
    @TruffleBoundary
    public static Object lookup(long address) {
        NativeHandleWrapper nativeMirror = nativeMirrors.get(address);
        RBaseObject result = nativeMirror != null ? nativeMirror.get() : null;
        if (result == null) {
            CompilerDirectives.transferToInterpreter();
            throw reportDataAccessError(address);
        }
        return result;
    }

    private static RuntimeException reportDataAccessError(long address) {
        if (TRACE_MIRROR_ALLOCATION_SITES) {
            printDataAccessErrorLocation(address);
        }
        throw RInternalError.shouldNotReachHere("unknown native reference " + address + "L / 0x" + Long.toHexString(address) + " (current id count: " + Long.toHexString(counter.get()) + ")");
    }

    private static void printDataAccessErrorLocation(long address) {
        RuntimeException location = nativeMirrorInfo.get(address);
        if (location != null) {
            System.out.println("Location at which the native mirror was allocated:");
            location.printStackTrace();
        } else {
            System.out.println("Location at which the native mirror was allocated was not recorded.");
        }
    }

    // methods operating on the native mirror object directly:

    public static int getIntNativeMirrorData(NativeMirror nativeMirror, int index) {
        assert nativeMirror.getDataAddress() != 0;
        assert index < nativeMirror.length;
        return NativeMemory.getInt(nativeMirror.dataAddress, index);
    }

    public static double getDoubleNativeMirrorData(NativeMirror nativeMirror, int index) {
        assert nativeMirror.getDataAddress() != 0;
        assert index < nativeMirror.length;
        return NativeMemory.getDouble(nativeMirror.dataAddress, index);
    }

    public static byte getLogicalNativeMirrorData(NativeMirror nativeMirror, int index) {
        assert nativeMirror.getDataAddress() != 0;
        assert index < nativeMirror.length;
        return RRuntime.int2logical(NativeMemory.getInt(nativeMirror.dataAddress, index));
    }

    public static byte getRawNativeMirrorData(NativeMirror nativeMirror, int index) {
        assert nativeMirror.getDataAddress() != 0;
        assert index < nativeMirror.length;
        return NativeMemory.getByte(nativeMirror.dataAddress, index);
    }

    public static RComplex getComplexNativeMirrorData(NativeMirror nativeMirror, int index) {
        assert nativeMirror.getDataAddress() != 0;
        assert index < nativeMirror.length;
        return RComplex.valueOf(NativeMemory.getDouble(nativeMirror.dataAddress, index * 2L),
                        NativeMemory.getDouble(nativeMirror.dataAddress, index * 2L + 1L));
    }

    public static double getComplexNativeMirrorDataR(NativeMirror nativeMirror, int index) {
        assert nativeMirror.getDataAddress() != 0;
        assert index < nativeMirror.length;
        return NativeMemory.getDouble(nativeMirror.dataAddress, +index * 2L);
    }

    public static double getComplexNativeMirrorDataI(NativeMirror nativeMirror, int index) {
        assert nativeMirror.getDataAddress() != 0;
        assert index < nativeMirror.length;
        return NativeMemory.getDouble(nativeMirror.dataAddress, +index * 2L + 1L);
    }

    public static double getComplexNativeMirrorRawData(NativeMirror nativeMirror, int index) {
        long address = nativeMirror.dataAddress.getAddress();
        assert address != 0;
        assert index < nativeMirror.length * 2;
        return NativeMemory.getDouble(nativeMirror.dataAddress, index);
    }

    public static CharSXPWrapper getStringNativeMirrorData(NativeMirror nativeMirror, int index) {
        assert nativeMirror.getDataAddress() != 0;
        assert index < nativeMirror.length;
        long elemAddr = NativeMemory.getLong(nativeMirror.dataAddress, index);
        assert elemAddr != 0L;
        return (CharSXPWrapper) NativeDataAccess.lookup(elemAddr);
    }

    public static Object getListElementNativeMirrorData(NativeMirror nativeMirror, int index) {
        assert nativeMirror.getDataAddress() != 0;
        assert index < nativeMirror.length;
        long elemAddr = NativeMemory.getLong(nativeMirror.dataAddress, index);
        assert elemAddr != 0L;
        return NativeDataAccess.lookup(elemAddr);
    }

    public static void setNativeMirrorDoubleData(NativeMirror nativeMirror, int index, double value) {
        assert nativeMirror.getDataAddress() != 0;
        assert index < nativeMirror.length;
        NativeMemory.putDouble(nativeMirror.dataAddress, index, value);
    }

    public static void setNativeMirrorComplexRealPartData(NativeMirror nativeMirror, int index, double value) {
        assert nativeMirror.getDataAddress() != 0;
        assert 2L * index < nativeMirror.length;
        NativeMemory.putDouble(nativeMirror.dataAddress, 2L * index, value);
    }

    public static void setNativeMirrorComplexImaginaryPartData(NativeMirror nativeMirror, int index, double value) {
        assert nativeMirror.getDataAddress() != 0;
        assert 2L * index + 1L < nativeMirror.length;
        NativeMemory.putDouble(nativeMirror.dataAddress, 2L * index + 1L, value);
    }

    public static void setNativeMirrorRawData(NativeMirror nativeMirror, int index, byte value) {
        assert nativeMirror.getDataAddress() != 0;
        assert index < nativeMirror.length;
        NativeMemory.putByte(nativeMirror.dataAddress, index, value);
    }

    public static void setNativeMirrorIntData(NativeMirror nativeMirror, int index, int value) {
        assert nativeMirror.getDataAddress() != 0;
        assert index < nativeMirror.length;
        NativeMemory.putInt(nativeMirror.dataAddress, index, value);
    }

    public static void setNativeMirrorLogicalData(NativeMirror nativeMirror, int index, byte logical) {
        setNativeMirrorIntData(nativeMirror, index, RRuntime.logical2int(logical));
    }

    public static void setNativeMirrorStringData(NativeMirror nativeMirror, int index, CharSXPWrapper value) {
        assert nativeMirror.getDataAddress() != 0;
        assert index < nativeMirror.length;
        long asPointer = getPointer(value);
        NativeMemory.putLong(nativeMirror.dataAddress, index, asPointer);
    }

    public static void setNativeMirrorListData(NativeMirror nativeMirror, int index, Object value) {
        assert nativeMirror.getDataAddress() != 0;
        assert index < nativeMirror.length;

        if (value instanceof RBaseObject) {
            long asPointer = getPointer((RBaseObject) value);
            NativeMemory.putLong(nativeMirror.dataAddress, index, asPointer);
        } else {
            throw RInternalError.shouldNotReachHere("value: " + value);
        }
    }

    public static double[] copyDoubleNativeData(NativeMirror mirror) {
        assert mirror.getDataAddress() != 0;
        double[] data = new double[(int) mirror.length];
        NativeMemory.copyMemory(mirror.dataAddress, data, ElementType.DOUBLE, data.length);
        return data;
    }

    public static double[] copyComplexNativeData(NativeMirror mirror) {
        assert mirror.getDataAddress() != 0;
        double[] data = new double[(int) (mirror.length << 1)];
        NativeMemory.copyMemory(mirror.dataAddress, data, ElementType.DOUBLE, data.length);
        return data;
    }

    public static int[] copyIntNativeData(NativeMirror mirror) {
        assert mirror.getDataAddress() != 0;
        int[] data = new int[(int) mirror.length];
        NativeMemory.copyMemory(mirror.dataAddress, data, ElementType.INT, data.length);
        return data;
    }

    public static byte[] copyByteNativeData(NativeMirror mirror) {
        assert mirror.getDataAddress() != 0;
        byte[] data = new byte[(int) mirror.length];
        NativeMemory.copyMemory(mirror.dataAddress, data, ElementType.BYTE, data.length);
        return data;
    }

    public static String[] copyStringNativeData(NativeMirror mirror) {
        assert mirror.getDataAddress() != 0;
        String[] data = new String[(int) mirror.length];
        for (int i = 0; i < mirror.length; i++) {
            long elemAddr = NativeMemory.getLong(mirror.dataAddress, i);
            assert elemAddr != 0L;
            Object elem = lookup(elemAddr);
            assert elem instanceof CharSXPWrapper;
            data[i] = ((CharSXPWrapper) elem).getContents();
        }
        return data;
    }

    public static Object[] copyListNativeData(NativeMirror mirror) {
        assert mirror.getDataAddress() != 0;
        Object[] data = new Object[(int) mirror.length];
        for (int i = 0; i < mirror.length; i++) {
            long elemAddr = NativeMemory.getLong(mirror.dataAddress, i);
            assert elemAddr != 0L;
            Object elem = lookup(elemAddr);
            data[i] = elem;
        }
        return data;
    }

    // methods operating on vectors that may have a native mirror assigned:

    private static final Assumption noIntNative = Truffle.getRuntime().createAssumption("noIntNative");
    private static final Assumption noLogicalNative = Truffle.getRuntime().createAssumption("noLogicalNative");
    private static final Assumption noDoubleNative = Truffle.getRuntime().createAssumption("noDoubleNative");
    private static final Assumption noComplexNative = Truffle.getRuntime().createAssumption("noComplexNative");
    private static final Assumption noRawNative = Truffle.getRuntime().createAssumption("noRawNative");
    private static final Assumption noCharSXPNative = Truffle.getRuntime().createAssumption("noCharSXPNative");
    private static final Assumption noStringNative = Truffle.getRuntime().createAssumption("noStringNative");
    private static final Assumption noListNative = Truffle.getRuntime().createAssumption("noListNative");

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
            toNative(vector);
            allocateNativeContents(vector, data, length);
        } else {
            (vector.getNativeMirror()).length = length;
        }
    }

    static int getTrueDataLength(RIntVector vector) {
        if (vector.getNativeMirror() == null) {
            return 0;
        }
        return (int) vector.getNativeMirror().truelength;
    }

    static void setTrueDataLength(RIntVector vector, int truelength) {
        toNative(vector);
        vector.getNativeMirror().truelength = truelength;
    }

    private static int getDataLengthFromMirror(NativeMirror mirror) {
        return (int) mirror.length;
    }

    static void setData(RIntVector vector, int[] data, int index, int value) {
        if (noIntNative.isValid() || data != null) {
            data[index] = value;
        } else {
            NativeMirror mirror = vector.getNativeMirror();
            assert mirror.getDataAddress() != 0;
            NativeMemory.putInt(mirror.dataAddress, index, value);
        }
    }

    static byte getData(RLogicalVector vector, byte[] data, int index) {
        if (noLogicalNative.isValid() || data != null) {
            return data[index];
        } else {
            NativeMirror mirror = vector.getNativeMirror();
            assert mirror.getDataAddress() != 0;
            return RRuntime.int2logical(NativeMemory.getInt(mirror.dataAddress, index));
        }
    }

    static int getDataLength(RLogicalVector vector, byte[] data) {
        if (noLogicalNative.isValid() || data != null) {
            return data.length;
        } else {
            return (int) vector.getNativeMirror().length;
        }
    }

    static void setData(RLogicalVector vector, byte[] data, int index, byte value) {
        if (noLogicalNative.isValid() || data != null) {
            data[index] = value;
        } else {
            NativeMirror mirror = vector.getNativeMirror();
            assert mirror.getDataAddress() != 0;
            NativeMemory.putInt(mirror.dataAddress, index, RRuntime.logical2int(value));
        }
    }

    static void setDataLength(RLogicalVector vector, byte[] data, int length) {
        if (noLogicalNative.isValid() || data != null) {
            toNative(vector);
            allocateNativeContents(vector, data, length);
        } else {
            vector.getNativeMirror().length = length;
        }
    }

    static int getTrueDataLength(RLogicalVector vector) {
        if (vector.getNativeMirror() == null) {
            return 0;
        }
        return (int) vector.getNativeMirror().truelength;
    }

    static void setTrueDataLength(RLogicalVector vector, int truelength) {
        toNative(vector);
        vector.getNativeMirror().truelength = truelength;
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
            return (int) vector.getNativeMirror().length;
        }
    }

    static void setDataLength(RRawVector vector, byte[] data, int length) {
        if (noRawNative.isValid() || data != null) {
            toNative(vector);
            allocateNativeContents(vector, data, length);
        } else {
            vector.getNativeMirror().length = length;
        }
    }

    static int getTrueDataLength(RRawVector vector) {
        if (vector.getNativeMirror() == null) {
            return 0;
        }
        return (int) vector.getNativeMirror().truelength;
    }

    static void setTrueDataLength(RRawVector vector, int truelength) {
        toNative(vector);
        vector.getNativeMirror().truelength = truelength;
    }

    static void setData(RRawVector vector, byte[] data, int index, byte value) {
        if (noRawNative.isValid() || data != null) {
            data[index] = value;
        } else {
            NativeMirror mirror = vector.getNativeMirror();
            assert mirror.getDataAddress() != 0;
            NativeMemory.putByte(mirror.dataAddress, index, value);
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
            return (int) vector.getNativeMirror().length;
        }
    }

    static void setDataLength(RDoubleVector vector, double[] data, int length) {
        if (noDoubleNative.isValid() || data != null) {
            toNative(vector);
            allocateNativeContents(vector, data, length);
        } else {
            vector.getNativeMirror().length = length;
        }
    }

    static int getTrueDataLength(RDoubleVector vector) {
        if (vector.getNativeMirror() == null) {
            return 0;
        }
        return (int) vector.getNativeMirror().truelength;
    }

    static void setTrueDataLength(RDoubleVector vector, int truelength) {
        toNative(vector);
        vector.getNativeMirror().truelength = truelength;
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

    static double getRawComplexData(RComplexVector vector, double[] data, int index) {
        if (noComplexNative.isValid() || data != null) {
            return data[index];
        } else {
            return getComplexNativeMirrorRawData(vector.getNativeMirror(), index);
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

    static double getComplexPart(RComplexVector vector, double[] data, int index) {
        if (noComplexNative.isValid() || data != null) {
            return data[index];
        } else {
            return getDoubleNativeMirrorData(vector.getNativeMirror(), index);
        }
    }

    static int getDataLength(RComplexVector vector, double[] data) {
        if (noComplexNative.isValid() || data != null) {
            return data.length >> 1;
        } else {
            return (int) vector.getNativeMirror().length;
        }
    }

    static void setDataLength(RComplexVector vector, double[] data, int length) {
        if (noComplexNative.isValid() || data != null) {
            toNative(vector);
            allocateNativeContents(vector, data, length);
        } else {
            vector.getNativeMirror().length = length;
        }
    }

    static int getTrueDataLength(RComplexVector vector) {
        if (vector.getNativeMirror() == null) {
            return 0;
        }
        return (int) vector.getNativeMirror().truelength;
    }

    static void setTrueDataLength(RComplexVector vector, int truelength) {
        toNative(vector);
        vector.getNativeMirror().truelength = truelength;
    }

    static void setData(RComplexVector vector, double[] data, int index, double re, double im) {
        if (noComplexNative.isValid() || data != null) {
            data[index * 2] = re;
            data[index * 2 + 1] = im;
        } else {
            NativeMirror mirror = vector.getNativeMirror();
            assert mirror.getDataAddress() != 0;
            NativeMemory.putDouble(mirror.dataAddress, index * 2L, re);
            NativeMemory.putDouble(mirror.dataAddress, index * 2L + 1L, im);
        }
    }

    static void setData(RComplexVector vector, double[] data, int index, double value) {
        if (noComplexNative.isValid() || data != null) {
            data[index] = value;
        } else {
            NativeMirror mirror = vector.getNativeMirror();
            assert mirror.getDataAddress() != 0;
            NativeMemory.putDouble(mirror.dataAddress, index, value);
        }
    }

    static void setDataLength(RStringVector vector, CharSXPWrapper[] data, int length) {
        if (noStringNative.isValid() || data != null) {
            toNative(vector);
            allocateNativeContents(vector, data, length);
        } else {
            vector.getNativeMirror().length = length;
        }
    }

    static int getTrueDataLength(RStringVector vector) {
        if (vector.getNativeMirror() == null) {
            return 0;
        }
        return (int) vector.getNativeMirror().truelength;
    }

    static void setTrueDataLength(RStringVector vector, int truelength) {
        toNative(vector);
        vector.getNativeMirror().truelength = truelength;
    }

    static int getTrueDataLength(CharSXPWrapper charsxp) {
        if (charsxp.getNativeMirror() == null) {
            return 0;
        }
        return (int) charsxp.getNativeMirror().truelength;
    }

    static void setTrueDataLength(CharSXPWrapper charsxp, int truelength) {
        toNative(charsxp);
        charsxp.getNativeMirror().truelength = truelength;
    }

    static Object getData(RList list, Object[] data, int index) {
        if (noListNative.isValid() || data != null) {
            return data[index];
        } else {
            return getListElementNativeMirrorData(list.getNativeMirror(), index);
        }
    }

    static void setData(RList list, Object[] data, int index, Object value) {
        assert data != null;
        data[index] = value;
        if (!noListNative.isValid() && list.isNativized()) {
            NativeDataAccess.setNativeMirrorListData(list.getNativeMirror(), index, value);
        }
    }

    static int getDataLength(RList vector, Object[] data) {
        if (noListNative.isValid() || data != null) {
            return data.length;
        } else {
            return getDataLengthFromMirror(vector.getNativeMirror());
        }
    }

    static void setDataLength(RList list, Object[] data, int length) {
        if (noListNative.isValid() || data != null) {
            toNative(list);
            allocateNativeContents(list, data, length);
        } else {
            list.getNativeMirror().length = length;
        }
    }

    static int getTrueDataLength(RList list) {
        if (list.getNativeMirror() == null) {
            return 0;
        }
        return (int) list.getNativeMirror().truelength;
    }

    static void setTrueDataLength(RList list, int truelength) {
        toNative(list);
        list.getNativeMirror().truelength = truelength;
    }

    static String getData(CharSXPWrapper charSXPWrapper, String data) {
        if (noCharSXPNative.isValid() || data != null) {
            return data;
        } else {
            NativeMirror mirror = charSXPWrapper.getNativeMirror();
            assert mirror.getDataAddress() != 0;
            return NativeMemory.copyCString(mirror.dataAddress, mirror.length, StandardCharsets.UTF_8);
        }
    }

    static byte getDataAt(CharSXPWrapper charSXPWrapper, byte[] data, int index) {
        if (noCharSXPNative.isValid() || data != null) {
            return data[index];
        } else {
            NativeMirror mirror = charSXPWrapper.getNativeMirror();
            assert mirror.getDataAddress() != 0;
            assert index < mirror.length;
            return NativeMemory.getByte(mirror.dataAddress, index);
        }
    }

    static int getDataLength(CharSXPWrapper charSXPWrapper, byte[] data) {
        if (noCharSXPNative.isValid() || data != null) {
            return data.length;
        } else {
            NativeMirror mirror = charSXPWrapper.getNativeMirror();
            assert mirror.getDataAddress() != 0;
            int length = 0;
            while (length < mirror.length && NativeMemory.getByte(mirror.dataAddress, length) != 0) {
                length++;
            }
            return length;
        }
    }

    static int getDataLength(RStringVector vector, Object[] data) {
        if (noStringNative.isValid() || data != null) {
            return data.length;
        } else {
            return getDataLengthFromMirror(vector.getNativeMirror());
        }
    }

    static String getData(RStringVector vector, Object data, int index) {
        if (noStringNative.isValid() || data != null) {
            Object localData = data;
            if (localData instanceof String[]) {
                return ((String[]) localData)[index];
            }
            assert data instanceof CharSXPWrapper[] : localData;
            assert ((CharSXPWrapper[]) localData)[index] != null;
            return ((CharSXPWrapper[]) localData)[index].getContents();
        } else {
            return getStringNativeMirrorData(vector.getNativeMirror(), index).getContents();
        }
    }

    static void setData(RStringVector vector, Object data, int index, String value) {
        assert data != null;
        if (data instanceof String[]) {
            assert !vector.isNativized();
            ((String[]) data)[index] = value;
        } else {
            assert data instanceof CharSXPWrapper[] : data;
            CharSXPWrapper elem = CharSXPWrapper.create(value);
            ((CharSXPWrapper[]) data)[index] = elem;

            if (!noStringNative.isValid() && vector.isNativized()) {
                NativeDataAccess.setNativeMirrorStringData(vector.getNativeMirror(), index, elem);
            }
        }
    }

    static void setData(RStringVector vector, CharSXPWrapper[] data, int index, CharSXPWrapper value) {
        assert data != null;
        data[index] = value;
        if (!noStringNative.isValid() && vector.isNativized()) {
            NativeDataAccess.setNativeMirrorStringData(vector.getNativeMirror(), index, value);
        }
    }

    public static long getNativeDataAddress(RBaseObject obj) {
        NativeMirror mirror = obj.getNativeMirror();
        return mirror == null ? 0 : mirror.getDataAddress();
    }

    static boolean isAllocated(RStringVector obj) {
        if (!noStringNative.isValid()) {
            NativeMirror mirror = obj.getNativeMirror();
            return mirror != null && mirror.dataAddress != null;
        }
        return false;
    }

    static boolean isAllocated(CharSXPWrapper obj) {
        if (!noCharSXPNative.isValid()) {
            NativeMirror mirror = obj.getNativeMirror();
            return mirror != null && mirror.dataAddress != null;
        }
        return false;
    }

    static boolean isAllocated(RList obj) {
        if (!noListNative.isValid()) {
            NativeMirror mirror = obj.getNativeMirror();
            return mirror != null && mirror.dataAddress != null;
        }
        return false;
    }

    static long allocateNativeContents(RLogicalVector vector, byte[] data, int length) {
        NativeMirror mirror = vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == null ^ data == null : mirror;
        if (mirror.dataAddress == null) {
            assert mirror.length == 0 && mirror.truelength == 0 : "mirror.length=" + mirror.length + ", mirror.truelength=" + mirror.truelength;
            int[] intArray = new int[data.length];
            for (int i = 0; i < data.length; i++) {
                intArray[i] = RRuntime.logical2int(data[i]);
            }
            noLogicalNative.invalidate();
            mirror.allocateNative(intArray, length, data.length, ElementType.INT);
        }
        return mirror.dataAddress.getAddress();
    }

    static long allocateNativeContents(RIntVector vector, int[] data, int length) {
        NativeMirror mirror = vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == null ^ data == null : mirror;
        if (mirror.dataAddress == null) {
            assert mirror.length == 0 && mirror.truelength == 0 : "mirror.length=" + mirror.length + ", mirror.truelength=" + mirror.truelength;
            noIntNative.invalidate();
            mirror.allocateNative(data, length, data.length, ElementType.INT);
        }
        return mirror.dataAddress.getAddress();
    }

    static void initializeAltrep(RBaseObject altrepVec, int length, long address) {
        NativeMirror mirror = altrepVec.getNativeMirror();
        assert altrepVec.isAltRep();
        assert address != 0;
        assert length >= 0;
        if (mirror.dataAddress == null) {
            assert mirror.length == 0 && mirror.truelength == 0 : "mirror.length=" + mirror.length + ", mirror.truelength=" + mirror.truelength;
            mirror.initializeAltrep(altrepVec, address, length);
        }
    }

    static long allocateNativeContents(RRawVector vector, byte[] data, int length) {
        NativeMirror mirror = vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == null ^ data == null : mirror;
        if (mirror.dataAddress == null) {
            assert mirror.length == 0 && mirror.truelength == 0 : "mirror.length=" + mirror.length + ", mirror.truelength=" + mirror.truelength;
            noRawNative.invalidate();
            mirror.allocateNative(data, length, data.length, ElementType.BYTE);
        }
        return mirror.dataAddress.getAddress();
    }

    static long allocateNativeContents(RDoubleVector vector, double[] data, int length) {
        NativeMirror mirror = vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == null ^ data == null : mirror;
        if (mirror.dataAddress == null) {
            assert mirror.length == 0 && mirror.truelength == 0 : "mirror.length=" + mirror.length + ", mirror.truelength=" + mirror.truelength;
            noDoubleNative.invalidate();
            mirror.allocateNative(data, length, data.length, ElementType.DOUBLE);
        }
        return mirror.dataAddress.getAddress();
    }

    static long allocateNativeContents(RComplexVector vector, double[] data, int length) {
        NativeMirror mirror = vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == null ^ data == null : mirror;
        if (mirror.dataAddress == null) {
            assert mirror.length == 0 && mirror.truelength == 0 : "mirror.length=" + mirror.length + ", mirror.truelength=" + mirror.truelength;
            noComplexNative.invalidate();
            mirror.allocateNative(data, length, data.length * 2L, ElementType.DOUBLE);
        }
        return mirror.dataAddress.getAddress();
    }

    static long allocateNativeContents(RStringVector vector, CharSXPWrapper[] charSXPdata, int length) {
        NativeMirror mirror = vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == null ^ charSXPdata == null : mirror;
        if (mirror.dataAddress == null) {
            noStringNative.invalidate();
            // Note: shall the character vector become writeable and not only read-only, we should
            // create assumption like for other vector types
            mirror.allocateNative(charSXPdata);
            mirror.length = length;
        }
        return mirror.dataAddress.getAddress();
    }

    static long allocateNativeContents(CharSXPWrapper vector, byte[] data) {
        NativeMirror mirror = vector.getNativeMirror();
        assert mirror != null;
        assert mirror.dataAddress == null ^ data == null;
        if (mirror.dataAddress == null) {
            noCharSXPNative.invalidate();
            mirror.allocateNativeString(data);
        }
        return mirror.dataAddress.getAddress();
    }

    static long allocateNativeContents(RList list, Object[] elements, int length) {
        NativeMirror mirror = list.getNativeMirror();
        assert mirror != null;
        if (mirror.dataAddress == null) {
            noListNative.invalidate();
            // Note: shall the list become writeable and not only read-only, we should
            // crate assumption like for other vector types
            mirror.allocateNative(elements);
            mirror.length = length;
        }
        return mirror.dataAddress.getAddress();
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
        long dataAddress = NativeMemory.allocate(size, "StringArray");
        long nextStringPtr = dataAddress + length * Long.BYTES;
        // start of the actual character data:
        for (int i = 0; i < length; i++) {
            NativeMemory.putLong(dataAddress, i, nextStringPtr);
            NativeMemory.copyMemory(bytes[i], nextStringPtr, ElementType.BYTE, bytes[i].length);
            NativeMemory.putByte(nextStringPtr, bytes[i].length, (byte) 0);
            nextStringPtr += bytes[i].length + 1;
        }
        return dataAddress;
    }

    @TruffleBoundary
    public static String[] copyBackNativeStringArray(long address, int length) {
        assert address != 0;
        String[] data = new String[length];
        for (int i = 0; i < length; i++) {
            long ptr = NativeMemory.getLong(address, i);
            data[i] = readNativeString(ptr);
        }
        return data;
    }

    @TruffleBoundary
    public static String readNativeString(long addr) {
        int len;
        for (len = 0; NativeMemory.getByte(addr, len) != 0; len++) {
        }
        byte[] bytes = new byte[len];
        NativeMemory.copyMemory(addr, bytes, ElementType.BYTE, len);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    public static void setNativeContents(RBaseObject obj, long address, int length) {
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
        } else if (noStringNative.isValid() && obj instanceof RStringVector) {
            noStringNative.invalidate();
        }
        NativeMirror mirror = obj.getNativeMirror();
        mirror.setExternalDataAddress(address);
        mirror.length = length;
    }

    public static void setNativeWrapper(RBaseObject obj, Object wrapper) {
        NativeMirror mirror = obj.getNativeMirror();
        if (mirror == null) {
            mirror = new NativeMirror(obj, 0);
            obj.setNativeMirror(mirror);
        }
        mirror.nativeWrapperRef = new NativeWrapperReference(wrapper);
    }

    public static Object getNativeWrapper(RBaseObject obj) {
        NativeMirror mirror = obj.getNativeMirror();
        if (mirror == null) {
            return null;
        } else {
            Reference<?> ref = mirror.nativeWrapperRef;
            return (ref != null) ? ref.get() : null;
        }
    }

    /**
     * This final class is needed so that the {@link Reference#get()} method can be inlined.
     */
    private static final class NativeWrapperReference extends WeakReference<Object> {
        NativeWrapperReference(Object nativeWrapper) {
            super(nativeWrapper);
        }
    }

    public interface NativeDataInspectorMBean {
        int getNativeMirrorsSize();

        String getObject(String idString);

        String getAttribute(String idString, String attrName);

        String getNativeIdFromAddress(String dataAddressString);
    }

    public static class NativeDataInspector implements NativeDataInspectorMBean {

        @Override
        public String getObject(String nativeIdString) {
            return lookup(Long.decode(nativeIdString)).toString();
        }

        @Override
        public String getAttribute(String idString, String attrName) {
            RBaseObject obj = (RBaseObject) lookup(Long.decode(idString));
            if (obj instanceof RAttributable) {
                return "" + ((RAttributable) obj).getAttr(attrName);
            } else {
                return "";
            }
        }

        @Override
        public int getNativeMirrorsSize() {
            return NativeDataAccess.nativeMirrors.size();
        }

        @Override
        public String getNativeIdFromAddress(String dataAddressString) {
            assert NativeDataAccess.dataAddressToNativeMirrors != null;
            NativeMirror nativeMirror = NativeDataAccess.dataAddressToNativeMirrors.get(Long.decode(dataAddressString));
            if (nativeMirror == null) {
                return "";
            }
            long id = nativeMirror.nativeHandle == null ? 0 : nativeMirror.nativeHandle.getId();
            return String.format("%16x", id);
        }

    }

    static void initMBean() {
        if (System.getenv(FastROptions.NATIVE_DATA_INSPECTOR) != null) {
            try {
                MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
                ObjectName name = new ObjectName("FastR:type=JMX,name=NativeDataInspector");
                NativeDataInspector resource = new NativeDataInspector();
                mbs.registerMBean(resource, name);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static {
        initMBean();
    }
}
