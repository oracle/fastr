/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Fallback;
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
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.CharSXPWrapper;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.NativeDataAccess.NativeMirror;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RInteropNA;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractAtomicVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.altrep.RAltIntegerVec;

@ExportLibrary(InteropLibrary.class)
@ExportLibrary(NativeTypeLibrary.class)
public final class VectorRFFIWrapper implements TruffleObject {

    protected final TruffleObject vector;
    protected long pointer;

    private VectorRFFIWrapper(TruffleObject vector) {
        assert vector instanceof RBaseObject;
        this.vector = vector;
        // Establish the 1-1 relationship between the object and its native wrapper
        NativeDataAccess.setNativeWrapper((RBaseObject) vector, this);
        pointer = NativeDataAccess.getNativeDataAddress((RBaseObject) vector);
    }

    public static VectorRFFIWrapper get(TruffleObject x) {
        assert x instanceof RBaseObject;
        Object wrapper = NativeDataAccess.getNativeWrapper((RBaseObject) x);
        if (wrapper != null) {
            assert wrapper instanceof VectorRFFIWrapper;
            return (VectorRFFIWrapper) wrapper;
        } else {
            // TODO: create subclasses of VectorRFFIWrapper specialized for concrete vector types
            // and use directly the vector methods (possibly via VectorAccess) not interop library
            wrapper = new VectorRFFIWrapper(x);
            return (VectorRFFIWrapper) wrapper;
        }
    }

    public TruffleObject getVector() {
        return vector;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean hasNativeType() {
        return true;
    }

    @ExportMessage
    public Object getNativeType(@CachedContext(TruffleRLanguage.class) RContext ctx, @Cached GetNativeTypeDispatcher getNativeType) {
        return getNativeType.execute(vector, ctx.getRFFI());
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    public long getArraySize(@CachedLibrary("this.vector") InteropLibrary interop, @Exclusive @Cached("createBinaryProfile()") ConditionProfile isComplex) throws UnsupportedMessageException {
        if (isComplex.profile(vector instanceof RComplexVector)) {
            return ((RComplexVector) vector).getLength() << 1;
        } else {
            return interop.getArraySize(this.vector);
        }
    }

    @ExportMessage
    public boolean isArrayElementReadable(long index, @CachedLibrary("this.vector") InteropLibrary interop) {
        return interop.isArrayElementReadable(this.vector, index);
    }

    @ExportMessage
    public Object readArrayElement(long index,
                    @Cached ReadElementDispatcher readElement,
                    @Cached.Shared("setterNode") @Cached AtomicVectorSetterNode setterNode,
                    @Cached() FFIMaterializeNode materializeNode,
                    @Cached() FFIToNativeMirrorNode wrapperNode,
                    @Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile isList,
                    @Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile isCharSxp) throws InvalidArrayIndexException {
        assert vector instanceof RAbstractVector || vector instanceof CharSXPWrapper;
        Object elem = readElement.execute(vector, (int) index);
        if (isList.profile(!(vector instanceof RAbstractAtomicVector) && !(vector instanceof CharSXPWrapper))) {
            elem = materializeNode.execute(elem, false);
            setterNode.execute(vector, (int) index, elem);
            return wrapperNode.execute(elem);
        } else if (isCharSxp.profile(elem instanceof CharSXPWrapper)) {
            return wrapperNode.execute(elem);
        } else {
            return elem;
        }
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isArrayElementInsertable(@SuppressWarnings("unused") long index) {
        return false;
    }

    @ExportMessage
    public boolean isArrayElementModifiable(long index,
                    @CachedLibrary("this.vector") InteropLibrary interop) {
        try {
            return index > 0 && index < interop.getArraySize(vector);
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @ExportMessage
    public void writeArrayElement(long index, Object value,
                    @Cached.Shared("setterNode") @Cached AtomicVectorSetterNode setterNode,
                    @Cached() FFIUnwrapNode unwrap,
                    @Cached.Exclusive @Cached("createBinaryProfile()") ConditionProfile isNative) {
        if (isNative.profile(value instanceof NativeMirror)) {
            setterNode.execute(vector, (int) index, unwrap.execute(value));
        } else {
            setterNode.execute(vector, (int) index, value);
        }
    }

    @ExportMessage
    public boolean isPointer() {
        return this.pointer != 0;
    }

    @ExportMessage
    public long asPointer() {
        assert pointer != 0;
        return this.pointer;
    }

    @ExportMessage
    public void toNative(@Cached DispatchAllocate dispatchAllocate) {
        if (pointer == 0) {
            pointer = dispatchAllocate.execute(vector);
        }
    }

    // Following two messages are used in our Sulong specific C code

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isString() {
        return vector instanceof CharSXPWrapper;
    }

    @ExportMessage
    public String asString() {
        return ((CharSXPWrapper) vector).getContents();
    }

    @Override
    public int hashCode() {
        return vector.hashCode();
    }

    @GenerateUncached
    @ImportStatic(DSLConfig.class)
    public abstract static class ReadElementDispatcher extends Node {
        public abstract Object execute(Object vector, int index) throws InvalidArrayIndexException;

        @Specialization
        protected static Object doString(RStringVector vec, int index,
                        @Cached("createBinaryProfile()") ConditionProfile isNativized,
                        @Cached("createBinaryProfile()") ConditionProfile needsWrapping) {
            vec.wrapStrings(isNativized, needsWrapping);
            return vec.getWrappedDataAt(index);
        }

        @Specialization
        protected static double doComplex(RComplexVector vec, int index) {
            return vec.getRawDataAt(index);
        }

        protected static boolean isNotStringOrComplex(Object value) {
            return !(value instanceof RStringVector || value instanceof RComplexVector);
        }

        @Specialization(guards = "isNotStringOrComplex(value)", limit = "getInteropLibraryCacheSize()")
        protected static Object others(Object value, int index,
                        @CachedLibrary("value") InteropLibrary interop,
                        @Cached BranchProfile naProfile) throws InvalidArrayIndexException {
            try {
                Object elem = interop.readArrayElement(value, index);
                if (elem instanceof RInteropNA) {
                    naProfile.enter();
                    return ((RInteropNA) elem).getNativeValue();
                }
                return elem;
            } catch (UnsupportedMessageException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    @GenerateUncached
    public abstract static class GetNativeTypeDispatcher extends Node {
        public abstract Object execute(Object vector, RFFIContext ctx);

        @Specialization
        protected static Object doInt(@SuppressWarnings("unused") RIntVector vec, RFFIContext ctx) {
            return ctx.getSulongArrayType(42);
        }

        @Specialization
        protected static Object doLogical(@SuppressWarnings("unused") RLogicalVector vec, RFFIContext ctx) {
            return ctx.getSulongArrayType(42);
        }

        @Specialization
        protected static Object doDouble(@SuppressWarnings("unused") RDoubleVector vec, RFFIContext ctx) {
            return ctx.getSulongArrayType(42.42);
        }

        @Specialization
        protected static Object doComplex(@SuppressWarnings("unused") RComplexVector vec, RFFIContext ctx) {
            return ctx.getSulongArrayType(42.42);
        }

        @Specialization
        protected static Object doRaw(@SuppressWarnings("unused") RRawVector vec, RFFIContext ctx) {
            return ctx.getSulongArrayType((byte) 42);
        }

        @Specialization
        protected static Object doCharSXP(@SuppressWarnings("unused") CharSXPWrapper vec, RFFIContext ctx) {
            return ctx.getSulongArrayType((byte) 42);
        }

        @Specialization
        protected static Object doString(@SuppressWarnings("unused") RStringVector vec, RFFIContext ctx) {
            return ctx.getSulongArrayType(42L);
        }

        @Specialization
        protected static Object doList(@SuppressWarnings("unused") RList vec, RFFIContext ctx) {
            return ctx.getSulongArrayType(42L);
        }

        @Specialization
        protected static Object doAltrepInt(@SuppressWarnings("unused") RAltIntegerVec vec, RFFIContext ctx) {
            return ctx.getSulongArrayType(42);
        }

        @Fallback
        protected static Object others(@SuppressWarnings("unused") Object vec, @SuppressWarnings("unused") RFFIContext ctx) {
            CompilerDirectives.transferToInterpreter();
            throw RInternalError.shouldNotReachHere(vec.toString());
        }
    }

    @GenerateUncached
    public abstract static class DispatchAllocate extends Node {
        private static final long EMPTY_DATA_ADDRESS = 0x1BAD;

        public abstract long execute(Object vector);

        @Specialization
        @TruffleBoundary
        protected static long get(RList list) {
            return list.allocateNativeContents();
        }

        @Specialization
        @TruffleBoundary
        protected static long get(RIntVector vector) {
            return vector.allocateNativeContents();
        }

        @Specialization(limit = "1")
        protected static long get(RAltIntegerVec altIntVector,
                      @CachedLibrary("altIntVector.getDescriptor().getDataptrMethod()") InteropLibrary dataptrMethodInteropLibrary,
                      @CachedLibrary(limit = "1") InteropLibrary dataptrInteropLibrary,
                                  @Cached("createBinaryProfile()") ConditionProfile hasMirrorProfile) {
            return altIntVector.getDescriptor().invokeDataptrMethodCached(altIntVector, true,
                    dataptrMethodInteropLibrary, dataptrInteropLibrary, hasMirrorProfile);
        }

        @Specialization
        @TruffleBoundary
        protected static long get(RLogicalVector vector) {
            return vector.allocateNativeContents();
        }

        @Specialization
        @TruffleBoundary
        protected static long get(RRawVector vector) {
            return vector.allocateNativeContents();
        }

        @Specialization
        @TruffleBoundary
        protected static long get(RDoubleVector vector) {
            return vector.allocateNativeContents();
        }

        @Specialization
        @TruffleBoundary
        protected static long get(RComplexVector vector) {
            return vector.allocateNativeContents();
        }

        @Specialization
        @TruffleBoundary
        protected static long get(RStringVector vector) {
            return vector.allocateNativeContents();
        }

        @Specialization
        @TruffleBoundary
        protected static long get(CharSXPWrapper vector) {
            return vector.allocateNativeContents();
        }

        @Specialization
        protected static long get(@SuppressWarnings("unused") RNull nullValue) {
            // Note: GnuR is OK with, e.g., INTEGER(NULL), but it's illegal to read from or
            // write to the resulting address.
            return EMPTY_DATA_ADDRESS;
        }

        @Fallback
        protected static long get(Object vector) {
            throw RInternalError.shouldNotReachHere("invalid wrapped object " + vector.getClass().getSimpleName());
        }
    }

    @GenerateUncached
    public abstract static class AtomicVectorSetterNode extends Node {

        public abstract Object execute(Object vector, int index, Object value);

        @Specialization
        protected static Object doIntVector(RIntVector vector, int index, int value, @Cached("create()") BranchProfile naProfile) {
            if (RRuntime.isNA(value)) {
                naProfile.enter();
                vector.setComplete(false);
            }
            vector.setDataAt(vector.getInternalStore(), index, value);
            return vector;
        }

        @Specialization
        protected static Object doDoubleVector(RDoubleVector vector, int index, double value, @Cached("create()") BranchProfile naProfile) {
            if (RRuntime.isNA(value)) {
                naProfile.enter();
                vector.setComplete(false);
            }
            vector.setDataAt(vector.getInternalStore(), index, value);
            return vector;
        }

        @Specialization
        protected static Object doDoubleVector(RDoubleVector vector, int index, long value, @Cached("create()") BranchProfile naProfile) {
            if (RRuntime.isNA(value)) {
                naProfile.enter();
                vector.setComplete(false);
            }
            vector.setDataAt(vector.getInternalStore(), index, value);
            return vector;
        }

        @Specialization
        protected static Object doDoubleVector(RDoubleVector vector, int index, int value, @Cached("create()") BranchProfile naProfile) {
            if (RRuntime.isNA(value)) {
                naProfile.enter();
                vector.setComplete(false);
            }
            vector.setDataAt(vector.getInternalStore(), index, value);
            return vector;
        }

        @Specialization
        protected static Object doDoubleVector(RDoubleVector vector, int index, float value, @Cached("create()") BranchProfile naProfile) {
            if (RRuntime.isNA(value)) {
                naProfile.enter();
                vector.setComplete(false);
            }
            vector.setDataAt(vector.getInternalStore(), index, value);
            return vector;
        }

        @Specialization
        protected static Object doDoubleVector(@SuppressWarnings("unused") RDoubleVector vector, @SuppressWarnings("unused") int index, @SuppressWarnings("unused") byte value) {
            // TODO: called from memcpy
            throw RInternalError.shouldNotReachHere();
        }

        @Specialization
        protected static Object doDoubleVector(RDoubleVector vector, int index, short value, @Cached("create()") BranchProfile naProfile) {
            if (RRuntime.isNA(value)) {
                naProfile.enter();
                vector.setComplete(false);
            }
            vector.setDataAt(vector.getInternalStore(), index, value);
            return vector;
        }

        @Specialization
        protected static Object doRawVector(RRawVector vector, int index, byte value) {
            vector.setRawDataAt(vector.getInternalStore(), index, value);
            return vector;
        }

        @Specialization
        protected static Object doLogicalVector(RLogicalVector vector, int index, int value,
                        @Cached("createBinaryProfile()") ConditionProfile booleanProfile,
                        @Cached("create()") BranchProfile naProfile) {
            if (RRuntime.isNA(value)) {
                naProfile.enter();
                vector.setComplete(false);
                vector.setDataAt(vector.getInternalStore(), index, RRuntime.LOGICAL_NA);
                return vector;
            }
            vector.setDataAt(vector.getInternalStore(), index, booleanProfile.profile(value == 0) ? RRuntime.LOGICAL_FALSE : RRuntime.LOGICAL_TRUE);
            return vector;
        }

        @Specialization
        protected static Object doList(RList vector, int index, Object value,
                        @Cached("createBinaryProfile()") ConditionProfile lookupProfile) {
            Object usedValue = value;
            if (lookupProfile.profile(value instanceof Long)) {
                usedValue = NativeDataAccess.lookup((long) value);
            }
            vector.setDataAt(index, usedValue);
            return vector;
        }

        @Specialization
        protected static Object doStringVector(RStringVector vector, int index, long value, @Cached("create()") BranchProfile naProfile) {
            Object usedValue = value;
            usedValue = NativeDataAccess.lookup(value);
            assert usedValue instanceof CharSXPWrapper;
            if (RRuntime.isNA(((CharSXPWrapper) usedValue).getContents())) {
                naProfile.enter();
                vector.setComplete(false);
            }
            vector.setWrappedDataAt(index, (CharSXPWrapper) usedValue);
            return vector;
        }

        @Specialization
        protected static Object doStringVector(RStringVector vector, int index, CharSXPWrapper value, @Cached("create()") BranchProfile naProfile) {
            if (RRuntime.isNA(value.getContents())) {
                naProfile.enter();
                vector.setComplete(false);
            }
            vector.setWrappedDataAt(index, value);
            return vector;
        }

        @Specialization
        protected static Object doComplexVector(RComplexVector vector, int index, double value, @Cached("create()") BranchProfile naProfile) {
            if (RRuntime.isNA(value)) {
                naProfile.enter();
                vector.setComplete(false);
            }
            vector.setDataAt(vector.getInternalStore(), index, value);
            return vector;
        }

        @Specialization
        protected static Object doComplexVector(RComplexVector vector, int index, long value, @Cached("create()") BranchProfile naProfile) {
            if (RRuntime.isNA(value)) {
                naProfile.enter();
                vector.setComplete(false);
            }
            vector.setDataAt(vector.getInternalStore(), index, value);
            return vector;
        }

        @Specialization
        protected static Object doComplexVector(RComplexVector vector, int index, int value, @Cached("create()") BranchProfile naProfile) {
            if (RRuntime.isNA(value)) {
                naProfile.enter();
                vector.setComplete(false);
            }
            vector.setDataAt(vector.getInternalStore(), index, value);
            return vector;
        }

        // TODO: Add other specializations
        @Specialization(limit = "1")
        protected static Object doAltIntegerVector(RAltIntegerVec vector, int index, int value,
                                                   @CachedLibrary("vector.getDescriptor().getDataptrMethod()") InteropLibrary dataptrMethodInterop,
                                                   @CachedLibrary(limit = "1") InteropLibrary dataptrInterop,
                                                   @Cached("createBinaryProfile()") ConditionProfile hasMirrorProfile) {
            // TODO: Add dataptrAddrComputed binary profile.
            long addr = vector.getDescriptor().invokeDataptrMethodCached(vector, true, dataptrMethodInterop, dataptrInterop, hasMirrorProfile);
            NativeDataAccess.setData(vector, addr, index, value);
            return vector;
        }

        @Fallback
        protected Object doOther(Object target, int index, Object value) {
            CompilerDirectives.transferToInterpreter();
            String targetName = target == null ? "null" : target.getClass().getSimpleName();
            String valueName = value == null ? "null" : value.getClass().getSimpleName();
            throw RInternalError.shouldNotReachHere("target=" + targetName + ", index=" + index + "value=" + valueName + " " + value);
        }
    }
}
