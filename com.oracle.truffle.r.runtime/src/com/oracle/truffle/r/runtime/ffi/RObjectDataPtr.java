/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.llvm.spi.NativeTypeLibrary;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.AbstractContainerLibrary;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.altrep.AltrepUtilities;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

import java.util.Objects;

@ExportLibrary(NativeTypeLibrary.class)
public abstract class RObjectDataPtr implements TruffleObject {

    public static RObjectDataPtr getUncached(RBaseObject obj) {
        GetObjectDataPtrNode getObjectDataPtrNode = GetObjectDataPtrNode.getUncached();
        return getObjectDataPtrNode.execute(obj);
    }

    private RObjectDataPtr() {
    }

    /**
     * The wrapped vector can be either {@link RAbstractVector} or {@link CharSXPWrapper}.
     */
    public abstract RBaseObject getVector();

    protected abstract Object getNativeTypeImpl(RFFIContext rffi);

    @ExportMessage(library = NativeTypeLibrary.class)
    @SuppressWarnings("static-method")
    public boolean hasNativeType() {
        return true;
    }

    @ExportMessage(library = NativeTypeLibrary.class)
    public Object getNativeType(@CachedContext(TruffleRLanguage.class) RContext ctx) {
        return getNativeTypeImpl(ctx.getRFFI());
    }

    @GenerateUncached
    @ImportStatic({NativeDataAccess.class, AltrepUtilities.class})
    public static abstract class GetObjectDataPtrNode extends Node {
        public abstract RObjectDataPtr execute(RBaseObject object);

        public static GetObjectDataPtrNode create() {
            return RObjectDataPtrFactory.GetObjectDataPtrNodeGen.create();
        }

        public static GetObjectDataPtrNode getUncached() {
            return RObjectDataPtrFactory.GetObjectDataPtrNodeGen.getUncached();
        }

        @Specialization(guards = "getNativeWrapper(object) != null")
        protected RObjectDataPtr getFromNativeWrapper(RBaseObject object) {
            Object nativeWrapper = NativeDataAccess.getNativeWrapper(object);
            assert nativeWrapper instanceof RObjectDataPtr;
            return (RObjectDataPtr) nativeWrapper;
        }

        @Specialization(guards = "getNativeWrapper(rNull) == null")
        protected RNullDataPtr getNullPtr(@SuppressWarnings("unused") RNull rNull) {
            return new RNullDataPtr();
        }

        @Specialization(guards = {"isAltrep(altrepVector)", "getNativeWrapper(altrepVector) == null"})
        protected AltrepVectorDataPtr getAltrepVectorDataPtr(RAbstractVector altrepVector,
                        @Cached AltrepRFFI.DataptrNode dataptrNode) {
            return createAltrepVectorDataPtr(altrepVector, dataptrNode);
        }

        @Specialization(guards = "getNativeWrapper(charWrapper) == null")
        protected CharSXPDataPtr getCharDataPtr(CharSXPWrapper charWrapper) {
            CharSXPDataPtr charSXPDataPtr = new CharSXPDataPtr(charWrapper);
            NativeDataAccess.setNativeWrapper(charWrapper, charSXPDataPtr);
            return charSXPDataPtr;
        }

        @Specialization(guards = {"!isAltrep(intVector)", "getNativeWrapper(intVector) == null"})
        protected IntVectorDataPtr getIntVectorDataPtr(RIntVector intVector) {
            IntVectorDataPtr intVectorDataPtr = new IntVectorDataPtr(intVector);
            return setNativeWrapper(intVector, intVectorDataPtr);
        }

        @Specialization(guards = {"!isAltrep(doubleVector)", "getNativeWrapper(doubleVector) == null"})
        protected DoubleVectorDataPtr getDoubleVectorDataPtr(RDoubleVector doubleVector) {
            DoubleVectorDataPtr doubleVectorDataPtr = new DoubleVectorDataPtr(doubleVector);
            return setNativeWrapper(doubleVector, doubleVectorDataPtr);
        }

        @Specialization(guards = {"!isAltrep(logicalVector)", "getNativeWrapper(logicalVector) == null"})
        protected LogicalVectorDataPtr getLogicalVectorDataPtr(RLogicalVector logicalVector) {
            LogicalVectorDataPtr logicalVectorDataPtr = new LogicalVectorDataPtr(logicalVector);
            return setNativeWrapper(logicalVector, logicalVectorDataPtr);
        }

        @Specialization(guards = {"!isAltrep(rawVector)", "getNativeWrapper(rawVector) == null"})
        protected RawVectorDataPtr getRawVectorDataPtr(RRawVector rawVector) {
            RawVectorDataPtr rawVectorDataPtr = new RawVectorDataPtr(rawVector);
            return setNativeWrapper(rawVector, rawVectorDataPtr);
        }

        @Specialization(guards = {"!isAltrep(complexVector)", "getNativeWrapper(complexVector) == null"})
        protected ComplexVectorDataPtr getComplexVectorDataPtr(RComplexVector complexVector) {
            ComplexVectorDataPtr complexVectorDataPtr = new ComplexVectorDataPtr(complexVector);
            return setNativeWrapper(complexVector, complexVectorDataPtr);
        }

        @Specialization(guards = {"!isAltrep(stringVector)", "getNativeWrapper(stringVector) == null"})
        protected StringVectorDataPtr getStringVectorDataPtr(RStringVector stringVector) {
            StringVectorDataPtr stringVectorDataPtr = new StringVectorDataPtr(stringVector);
            return setNativeWrapper(stringVector, stringVectorDataPtr);
        }

        @Specialization(guards = "getNativeWrapper(list) == null")
        protected ListDataPtr getListDataPtr(RList list) {
            ListDataPtr listDataPtr = new ListDataPtr(list);
            return setNativeWrapper(list, listDataPtr);
        }

        private static <T extends RObjectDataPtr> T setNativeWrapper(RBaseObject object, T nativeWrapper) {
            NativeDataAccess.setNativeWrapper(object, nativeWrapper);
            return nativeWrapper;
        }

        private static AltrepVectorDataPtr createAltrepVectorDataPtr(RAbstractVector altrepVector, AltrepRFFI.DataptrNode dataptrNode) {
            long dataptrAddr = dataptrNode.execute(altrepVector, true);
            AltrepVectorDataPtr altrepVectorDataPtr = new AltrepVectorDataPtr(altrepVector, dataptrAddr);
            return setNativeWrapper(altrepVector, altrepVectorDataPtr);
        }
    }

    /**
     * Whenever DATAPTR C function is called on an altrep instance, an instance of this class is
     * returned. In Sulong, it should resemble native array.
     */
    @ExportLibrary(InteropLibrary.class)
    protected static final class AltrepVectorDataPtr extends RObjectDataPtr {
        private final RAbstractVector altrepVec;
        private final long dataptrAddr;

        AltrepVectorDataPtr(RAbstractVector altrepVec, long dataptrAddr) {
            assert AltrepUtilities.isAltrep(altrepVec);
            this.altrepVec = altrepVec;
            this.dataptrAddr = dataptrAddr;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean isPointer() {
            return true;
        }

        @ExportMessage
        public long asPointer() {
            return dataptrAddr;
        }

        @Override
        public RBaseObject getVector() {
            return altrepVec;
        }

        @Override
        protected Object getNativeTypeImpl(RFFIContext rffi) {
            // TODO: Implement for more types, or remove this method completely.
            return rffi.getSulongArrayType(42);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    @ExportLibrary(NativeTypeLibrary.class)
    protected abstract static class VectorDataPtrBase extends RObjectDataPtr {
        protected static final int DATA_LIB_LIMIT = 3;

        protected final RAbstractVector vector;
        protected Object vectorData;
        private long ptr;

        private VectorDataPtrBase(RAbstractVector vector) {
            this.vector = vector;
            this.vectorData = vector.getData();
        }

        @Override
        public RBaseObject getVector() {
            return vector;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean hasArrayElements() {
            return true;
        }

        @ExportMessage
        public long getArraySize(@CachedLibrary(limit = "DATA_LIB_LIMIT") VectorDataLibrary dataLib) {
            return dataLib.getLength(vectorData);
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        public boolean isArrayElementInsertable(@SuppressWarnings("unused") long index) {
            return false;
        }

        @ExportMessage
        public boolean isArrayElementReadable(long index,
                        @CachedLibrary(limit = "DATA_LIB_LIMIT") VectorDataLibrary dataLib) {
            return index >= 0 && index < getArraySize(dataLib);
        }

        @ExportMessage
        public Object readArrayElement(long index,
                        @CachedLibrary(limit = "DATA_LIB_LIMIT") VectorDataLibrary dataLib) {
            // The message is overridden by some subclasses
            return dataLib.getDataAtAsObject(vectorData, RRuntime.interopArrayIndexToInt(index, vector));
        }

        @ExportMessage
        public boolean isArrayElementModifiable(long index,
                        @CachedLibrary(limit = "DATA_LIB_LIMIT") VectorDataLibrary dataLib) {
            return isArrayElementReadable(index, dataLib);
        }

        @ExportMessage
        public void writeArrayElement(long index, Object value,
                        @Cached BranchProfile notMaterializedBranchProfile,
                        @CachedLibrary(limit = "DATA_LIB_LIMIT") AbstractContainerLibrary containerLib,
                        @CachedLibrary(limit = "DATA_LIB_LIMIT") VectorDataLibrary dataLib) {
            // The message is overridden by some subclasses
            if (!dataLib.isWriteable(vectorData)) {
                notMaterializedBranchProfile.enter();
                containerLib.materializeData(vector);
                vectorData = vector.getData();
                assert dataLib.isWriteable(vectorData);
            }
            try {
                dataLib.setDataAtAsObject(vectorData, RRuntime.interopArrayIndexToInt(index, vector), value);
            } catch (ClassCastException ex) {
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(ex,
                                String.format("Data: %s, vector: %s, index: %d, value: %s", vectorData.getClass().getSimpleName(), vector.getClass().getSimpleName(), index, value));
            }
        }

        @ExportMessage
        public boolean isPointer() {
            return this.ptr != 0;
        }

        @ExportMessage
        public long asPointer() {
            assert ptr != 0;
            return this.ptr;
        }

        @ExportMessage
        public void toNative(
                        @Cached("createBinaryProfile()") ConditionProfile needsTransitionProfile,
                        @CachedLibrary(limit = "DATA_LIB_LIMIT") AbstractContainerLibrary containerLib,
                        @CachedLibrary(limit = "DATA_LIB_LIMIT") VectorDataLibrary nativeDataLib) {
            // Note: it is possible that the vector is already nativized, but we have not
            // initialized the ptr field yet
            // This could be improved: we can initialize the ptr in ctor if the vector is already
            // nativized
            if (needsTransitionProfile.profile(ptr == 0)) {
                containerLib.toNative(vector);
                vectorData = vector.getData();
                try {
                    ptr = nativeDataLib.asPointer(vectorData);
                    assert ptr != 0;
                } catch (UnsupportedMessageException e) {
                    CompilerDirectives.transferToInterpreter();
                    throw RInternalError.shouldNotReachHere(Objects.toString(vectorData));
                }
            }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    @ExportLibrary(NativeTypeLibrary.class)
    protected static final class IntVectorDataPtr extends VectorDataPtrBase {
        private IntVectorDataPtr(RIntVector vector) {
            super(vector);
        }

        @Override
        protected Object getNativeTypeImpl(RFFIContext rffi) {
            return rffi.getSulongArrayType(42);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    @ExportLibrary(NativeTypeLibrary.class)
    protected static final class DoubleVectorDataPtr extends VectorDataPtrBase {
        private DoubleVectorDataPtr(RDoubleVector vector) {
            super(vector);
        }

        @Override
        protected Object getNativeTypeImpl(RFFIContext rffi) {
            return rffi.getSulongArrayType(42.42);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    @ExportLibrary(NativeTypeLibrary.class)
    protected static final class RawVectorDataPtr extends VectorDataPtrBase {
        private RawVectorDataPtr(RRawVector vector) {
            super(vector);
        }

        @Override
        protected Object getNativeTypeImpl(RFFIContext rffi) {
            return rffi.getSulongArrayType((byte) 42);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    @ExportLibrary(NativeTypeLibrary.class)
    protected static final class StringVectorDataPtr extends VectorDataPtrBase {
        private StringVectorDataPtr(RStringVector vector) {
            super(vector);
            assert vector.isWrapped();
        }

        @Override
        protected Object getNativeTypeImpl(RFFIContext rffi) {
            return rffi.getSulongArrayType(42L);
        }

        @ExportMessage
        public Object readArrayElement(long index,
                        @Cached FFIToNativeMirrorNode wrapperNode,
                        @CachedLibrary(limit = "DATA_LIB_LIMIT") VectorDataLibrary dataLib) {
            String result = dataLib.getStringAt(vectorData, (int) index);
            Object wrappedInCharSXP = CharSXPWrapper.create(result);
            return wrapperNode.execute(wrappedInCharSXP);
        }

        @ExportMessage
        protected void writeArrayElement(long index, Object value,
                        @Cached FFIUnwrapNode unwrapNode,
                        @Cached FFIUnwrapString unwrapString,
                        @Cached("createBinaryProfile()") ConditionProfile isWrappedProfile,
                        @Cached("createBinaryProfile()") ConditionProfile isStringWrappedProfile,
                        @Cached BranchProfile notMaterializedBranchProfile,
                        @CachedLibrary(limit = "DATA_LIB_LIMIT") VectorDataLibrary dataLib,
                        @CachedLibrary(limit = "DATA_LIB_LIMIT") AbstractContainerLibrary containerLib) {
            Object unwrappedValue = value;
            if (isWrappedProfile.profile(value instanceof NativeDataAccess.NativeMirror)) {
                unwrappedValue = unwrapNode.execute(value);
            }
            if (isStringWrappedProfile.profile(unwrappedValue instanceof CharSXPWrapper)) {
                unwrappedValue = unwrapString.execute(unwrappedValue);
            }
            if (!dataLib.isWriteable(vectorData)) {
                notMaterializedBranchProfile.enter();
                containerLib.materializeData(vector);
                vectorData = vector.getData();
                assert dataLib.isWriteable(vectorData);
            }
            dataLib.setElementAt(vectorData, RRuntime.interopArrayIndexToInt(index, vector), unwrappedValue);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    @ExportLibrary(NativeTypeLibrary.class)
    protected static final class LogicalVectorDataPtr extends VectorDataPtrBase {
        private LogicalVectorDataPtr(RLogicalVector vector) {
            super(vector);
        }

        @Override
        protected Object getNativeTypeImpl(RFFIContext rffi) {
            return rffi.getSulongArrayType(42);
        }

        @ExportMessage
        @Override
        public Object readArrayElement(long index,
                        @CachedLibrary(limit = "DATA_LIB_LIMIT") VectorDataLibrary dataLib) {
            // The message is overridden by subclass handling lists
            byte value = dataLib.getLogicalAt(vectorData, RRuntime.interopArrayIndexToInt(index, vector));
            return RRuntime.logical2int(value);
        }

        @ExportMessage
        @Override
        public void writeArrayElement(long index, Object value,
                        @Cached BranchProfile notMaterializedBranchProfile,
                        @CachedLibrary(limit = "DATA_LIB_LIMIT") AbstractContainerLibrary containerLib,
                        @CachedLibrary(limit = "DATA_LIB_LIMIT") VectorDataLibrary dataLib) {
            assert value instanceof Integer : value;
            super.writeArrayElement(index, RRuntime.int2logical((Integer) value), notMaterializedBranchProfile, containerLib, dataLib);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    @ExportLibrary(NativeTypeLibrary.class)
    protected static final class ComplexVectorDataPtr extends VectorDataPtrBase {
        private ComplexVectorDataPtr(RComplexVector vector) {
            super(vector);
        }

        @Override
        protected Object getNativeTypeImpl(RFFIContext rffi) {
            return rffi.getSulongArrayType(42.42);
        }

        @ExportMessage
        @Override
        public long getArraySize(@CachedLibrary(limit = "DATA_LIB_LIMIT") VectorDataLibrary dataLib) {
            return dataLib.getLength(vectorData) * 2;
        }

        @ExportMessage
        @Override
        public Object readArrayElement(long index,
                        @CachedLibrary(limit = "DATA_LIB_LIMIT") VectorDataLibrary dataLib) {
            return dataLib.getComplexComponentAt(vectorData, RRuntime.interopArrayIndexToInt(index, vector));
        }

        @ExportMessage
        @Override
        public void writeArrayElement(long index, Object value,
                        @Cached BranchProfile notMaterializedBranchProfile,
                        @CachedLibrary(limit = "DATA_LIB_LIMIT") AbstractContainerLibrary containerLib,
                        @CachedLibrary(limit = "DATA_LIB_LIMIT") VectorDataLibrary dataLib) {
            assert value instanceof Double : value;
            if (!dataLib.isWriteable(vectorData)) {
                notMaterializedBranchProfile.enter();
                containerLib.materializeData(vector);
                vectorData = vector.getData();
                assert dataLib.isWriteable(vectorData);
            }
            dataLib.setComplexComponentAt(vectorData, RRuntime.interopArrayIndexToInt(index, vector), (Double) value);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    @ExportLibrary(NativeTypeLibrary.class)
    protected static final class ListDataPtr extends VectorDataPtrBase {
        private ListDataPtr(RList vector) {
            super(vector);
        }

        @Override
        protected Object getNativeTypeImpl(RFFIContext rffi) {
            return rffi.getSulongArrayType(42L);
        }

        @ExportMessage
        public Object readArrayElement(long index,
                        @Cached FFIMaterializeNode materializeNode,
                        @Cached FFIToNativeMirrorNode wrapperNode,
                        @Exclusive @Cached("createBinaryProfile()") ConditionProfile wasMaterializedProfile,
                        @CachedLibrary(limit = "DATA_LIB_LIMIT") VectorDataLibrary dataLib) {
            Object result = super.readArrayElement(index, dataLib);
            Object materialized = materializeNode.materialize(result);
            if (wasMaterializedProfile.profile(materialized != result)) {
                dataLib.setElementAt(vectorData, RRuntime.interopArrayIndexToInt(index, vector), materialized);
            }
            return wrapperNode.execute(materialized);
        }

        @ExportMessage
        public void writeArrayElement(long index, Object value,
                        @Cached FFIUnwrapNode unwrap,
                        @Cached BranchProfile notMaterializedBranchProfile,
                        @CachedLibrary(limit = "DATA_LIB_LIMIT") AbstractContainerLibrary containerLib,
                        @Exclusive @Cached("createBinaryProfile()") ConditionProfile isWrappedProfile,
                        @CachedLibrary(limit = "DATA_LIB_LIMIT") VectorDataLibrary dataLib) {
            Object unwrappedValue = value;
            if (isWrappedProfile.profile(value instanceof NativeDataAccess.NativeMirror)) {
                unwrappedValue = unwrap.execute(value);
            }
            if (!dataLib.isWriteable(vectorData)) {
                notMaterializedBranchProfile.enter();
                containerLib.materializeData(vector);
                vectorData = vector.getData();
                assert dataLib.isWriteable(vectorData);
            }
            dataLib.setElementAt(vectorData, RRuntime.interopArrayIndexToInt(index, vector), unwrappedValue);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    @ExportLibrary(NativeTypeLibrary.class)
    protected static final class CharSXPDataPtr extends RObjectDataPtr {
        protected final CharSXPWrapper value;
        private long ptr;

        protected CharSXPDataPtr(CharSXPWrapper value) {
            this.value = value;
        }

        @Override
        public RBaseObject getVector() {
            return value;
        }

        @Override
        protected Object getNativeTypeImpl(RFFIContext rffi) {
            return rffi.getSulongArrayType((byte) 42);
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean hasArrayElements() {
            return true;
        }

        @ExportMessage
        public long getArraySize() {
            return value.getLength();
        }

        @ExportMessage
        public boolean isArrayElementReadable(long index) {
            return index >= 0 && index < value.getLength();
        }

        @ExportMessage
        public byte readArrayElement(long index,
                        @Exclusive @Cached("createBinaryProfile()") ConditionProfile prof1,
                        @Exclusive @Cached("createBinaryProfile()") ConditionProfile prof2) throws InvalidArrayIndexException {
            return value.readArrayElement(index, prof1, prof2);
        }

        @ExportMessage
        public boolean isPointer() {
            return ptr != 0;
        }

        @ExportMessage
        public long asPointer() {
            assert ptr != 0;
            return ptr;
        }

        @ExportMessage
        public void toNative(
                        @Exclusive @Cached("createBinaryProfile()") ConditionProfile needsTransitionProfile) {
            if (needsTransitionProfile.profile(ptr == 0)) {
                ptr = value.allocateNativeContents();
            }
        }

        // Following two messages are used in our Sulong specific C code

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean isString() {
            return true;
        }

        @ExportMessage
        public String asString() {
            return value.getContents();
        }
    }

    @ExportLibrary(InteropLibrary.class)
    @ExportLibrary(NativeTypeLibrary.class)
    protected static final class RNullDataPtr extends RObjectDataPtr {
        private static final long EMPTY_DATA_ADDRESS = 0x1BAD;

        @Override
        public RBaseObject getVector() {
            return RNull.instance;
        }

        @Override
        protected Object getNativeTypeImpl(RFFIContext rffi) {
            return rffi.getSulongArrayType(42L);
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean hasArrayElements() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public long getArraySize() {
            return 0;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean isArrayElementReadable(@SuppressWarnings("unused") long index) {
            return false;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public Object readArrayElement(@SuppressWarnings("unused") long index) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "It is not allowed to read data of R NULL value");
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean isPointer() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public long asPointer() {
            return EMPTY_DATA_ADDRESS;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public void toNative() {
            // noop
        }
    }
}
