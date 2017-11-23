/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.GetNextNodeGen.GetNextGenericNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.VectorIterator.IteratorData;

abstract class VectorIteratorNodeAdapter extends Node {
    public static boolean hasNoNativeMemoryData(RAbstractVector vector, Class<? extends RAbstractVector> vecClass) {
        return !(RVector.class.isAssignableFrom(vecClass)) || !((RVector<?>) vecClass.cast(vector)).hasNativeMemoryData();
    }

    public static boolean isRVector(Class<? extends RAbstractVector> vecClass) {
        return RVector.class.isAssignableFrom(vecClass);
    }
}

abstract class GetIteratorNode extends VectorIteratorNodeAdapter {
    public abstract IteratorData<?> execute(RAbstractVector vector);

    @Specialization(guards = "vector.hasNativeMemoryData()")
    protected IteratorData<?> nativeMirrorIterator(RVector<?> vector) {
        return new IteratorData<>(vector.getNativeMirror(), vector.getLength());
    }

    @Specialization(guards = {"vectorClass == vector.getClass()", "hasNoNativeMemoryData(vector, vectorClass)"}, limit = "10")
    protected IteratorData<?> cached(RAbstractVector vector,
                    @Cached("vector.getClass()") Class<? extends RAbstractVector> vectorClass) {
        RAbstractVector profiledVec = vectorClass.cast(vector);
        return new IteratorData<>(profiledVec.getInternalStore(), profiledVec.getLength());
    }

    @Specialization(replaces = "cached", guards = {"hasNoNativeMemoryData(vector, vector.getClass())"})
    protected IteratorData<?> generic(RAbstractVector vector) {
        RAbstractVector profiledVec = vector;
        return new IteratorData<>(profiledVec.getInternalStore(), profiledVec.getLength());
    }

    @Fallback
    protected IteratorData<?> fallback(RAbstractVector vector) {
        return new IteratorData<>(vector.getInternalStore(), vector.getLength());
    }
}

@SuppressWarnings("unused")
abstract class HasNextNode extends VectorIteratorNodeAdapter {
    public abstract boolean execute(RAbstractVector vector, IteratorData<?> iterator);

    @Specialization(guards = "!iter.hasNativeMirror()")
    protected boolean intVector(RIntVector vector, IteratorData<?> iter) {
        return iter.index < ((int[]) iter.store).length;
    }

    @Specialization(guards = "!iter.hasNativeMirror()")
    protected boolean doubleVector(RDoubleVector vector, IteratorData<?> iter) {
        return iter.index < ((double[]) iter.store).length;
    }

    @Specialization(guards = "!iter.hasNativeMirror()")
    protected boolean logicalVector(RLogicalVector vector, IteratorData<?> iter) {
        return iter.index < ((byte[]) iter.store).length;
    }

    @Specialization(guards = "!iter.hasNativeMirror()")
    protected boolean rawVector(RRawVector vector, IteratorData<?> iter) {
        return iter.index < ((byte[]) iter.store).length;
    }

    @Specialization(guards = "!iter.hasNativeMirror()")
    protected boolean stringVector(RStringVector vector, IteratorData<?> iter) {
        return iter.index < ((String[]) iter.store).length;
    }

    @Specialization(guards = {"vectorClass == vector.getClass()", "!isRVector(vectorClass)"}, limit = "10")
    protected boolean generic(RAbstractVector vector, IteratorData<?> iter,
                    @Cached("vector.getClass()") Class<? extends RAbstractVector> vectorClass) {
        RAbstractVector profiledVec = vectorClass.cast(vector);
        return iter.index < profiledVec.getLength();
    }

    @Specialization
    protected boolean generic(RAbstractVector vector, IteratorData<?> iter) {
        return iter.index < vector.getLength();
    }
}

@SuppressWarnings("unused")
abstract class GetNextNode extends VectorIteratorNodeAdapter {
    public abstract Object execute(RAbstractVector vector, IteratorData<?> iterator);

    @Specialization(guards = "!iter.hasNativeMirror()")
    protected int intVector(RIntVector vector, IteratorData<?> iter) {
        return ((int[]) iter.store)[iter.index];
    }

    @Specialization(guards = "!iter.hasNativeMirror()")
    protected double doubleVector(RDoubleVector vector, IteratorData<?> iter) {
        return ((double[]) iter.store)[iter.index];
    }

    @Specialization(guards = "!iter.hasNativeMirror()")
    protected byte logicalVector(RLogicalVector vector, IteratorData<?> iter) {
        return ((byte[]) iter.store)[iter.index];
    }

    @Specialization(guards = "!iter.hasNativeMirror()")
    protected RComplex doComplexVector(RComplexVector vector, IteratorData<?> iter) {
        double[] arr = (double[]) iter.store;
        return RComplex.valueOf(arr[iter.index * 2], arr[iter.index * 2 + 1]);
    }

    @Specialization(guards = "!iter.hasNativeMirror()")
    protected byte doRawVector(RRawVector vector, IteratorData<?> iter) {
        return ((byte[]) iter.store)[iter.index];
    }

    @Specialization(guards = "!iter.hasNativeMirror()")
    protected String doStringVector(RStringVector vector, IteratorData<?> iter) {
        return ((String[]) iter.store)[iter.index];
    }

    @Specialization(guards = "iter.hasNativeMirror()")
    protected int intVectorNative(RIntVector vector, IteratorData<?> iter) {
        return NativeDataAccess.getIntNativeMirrorData(iter.store, iter.index);
    }

    @Specialization(guards = "iter.hasNativeMirror()")
    protected double doubleVectorNative(RDoubleVector vector, IteratorData<?> iter) {
        return NativeDataAccess.getDoubleNativeMirrorData(iter.store, iter.index);
    }

    @Specialization(guards = "iter.hasNativeMirror()")
    protected byte logicalVectorNative(RLogicalVector vector, IteratorData<?> iter) {
        return NativeDataAccess.getLogicalNativeMirrorData(iter.store, iter.index);
    }

    @Specialization(guards = "iter.hasNativeMirror()")
    protected byte doubleVectorNative(RRawVector vector, IteratorData<?> iter) {
        return NativeDataAccess.getRawNativeMirrorData(iter.store, iter.index);
    }

    @Specialization(guards = "iter.hasNativeMirror()")
    protected RComplex complexVectorNative(RComplexVector vector, IteratorData<?> iter) {
        return NativeDataAccess.getComplexNativeMirrorData(iter.store, iter.index);
    }

    @Specialization(guards = "iter.hasNativeMirror()")
    protected byte stringVectorNative(RStringVector vector, IteratorData<?> iter) {
        throw RInternalError.unimplemented("string vectors backed by native memory");
    }

    @Child private GetNextGenericNode getNextGenericNode;

    @Fallback
    protected Object doGeneric(RAbstractVector vector, IteratorData<?> iter) {
        // we use fallback and extra node so that we do not have to explicitly check that the vector
        // is not
        // RVector, DSL generates fallback guard that compares the class with the all RVector
        // subclasses
        // used in the specializations above, "vector instanceof RVector" would not be a leaf-check.
        if (getNextGenericNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getNextGenericNode = insert(GetNextGenericNode.create());
        }
        return getNextGenericNode.execute(vector, iter);
    }

    abstract static class GetNextGenericNode extends Node {
        public abstract Object execute(RAbstractVector vector, IteratorData<?> iter);

        public static GetNextGenericNode create() {
            return GetNextGenericNodeGen.create();
        }

        @Specialization
        protected int intVectorGeneric(RAbstractIntVector vector, IteratorData<?> iter,
                        @Cached("create()") GetDataAt.Int getDataAtNode) {
            return getDataAtNode.get(vector, iter.store, iter.index);
        }

        @Specialization
        protected double doubleVectorGeneric(RAbstractDoubleVector vector, IteratorData<?> iter,
                        @Cached("create()") GetDataAt.Double getDataAtNode) {
            return getDataAtNode.get(vector, iter.store, iter.index);
        }

        @Specialization
        protected String stringVectorGeneric(RAbstractStringVector vector, IteratorData<?> iter,
                        @Cached("create()") GetDataAt.String getDataAtNode) {
            return getDataAtNode.get(vector, iter.store, iter.index);
        }

        @Specialization
        protected byte rawVectorGeneric(RAbstractRawVector vector, IteratorData<?> iter,
                        @Cached("create()") GetDataAt.Raw getDataAtNode) {
            return getDataAtNode.get(vector, iter.store, iter.index);
        }

        @Specialization
        protected byte logicalVectorGeneric(RAbstractLogicalVector vector, IteratorData<?> iter,
                        @Cached("create()") GetDataAt.Logical getDataAtNode) {
            return getDataAtNode.get(vector, iter.store, iter.index);
        }

        @Specialization
        protected RComplex complexVectorGeneric(RAbstractComplexVector vector, IteratorData<?> iter,
                        @Cached("create()") GetDataAt.Complex getDataAtNode) {
            return getDataAtNode.get(vector, iter.store, iter.index);
        }
    }
}

// Checkstyle: stop final class check
// Note: the VectorIterator cannot be final, it has inner subclasses, which probably confuses check
// style

/**
 * This node wraps 3 nodes needed to sequentially iterate a given vector and provides convenience
 * methods to invoke those nodes: {@link #init(RAbstractVector)},
 * {@link #next(RAbstractVector, Object)} and {@link #hasNext(RAbstractVector, Object)}.
 *
 * To construct use factory methods from type specialized inner classes, e.g. {@link Int#create()},
 * or generic version if the iterated vector could be of any type {@link Generic#create()}.
 *
 * Iterator can wrap around, i.e. once the iteration ends, it starts again from the first element.
 */
public abstract class VectorIterator<T> extends Node {

    // Note: it could be worth adding a LoopConditionProfile, however, it must be shared between
    // getIteratorNode and getNextNode

    @Child private GetIteratorNode getIteratorNode = GetIteratorNodeGen.create();
    @Child private HasNextNode hasNextNode;
    @Child private GetNextNode getNextNode = GetNextNodeGen.create();
    private final boolean wrapAround;

    private VectorIterator(boolean wrapAround) {
        this.wrapAround = wrapAround;
        if (!wrapAround) {
            hasNextNode = HasNextNodeGen.create();
        }
    }

    public Object init(RAbstractVector vector) {
        return getIteratorNode.execute(vector);
    }

    public boolean hasNext(RAbstractVector vector, Object iterator) {
        assert !wrapAround : "wrap-around iteration does not support hasNext";
        return hasNextNode.execute(vector, (IteratorData<?>) iterator);
    }

    @SuppressWarnings("unchecked")
    public T next(RAbstractVector vector, Object iterator) {
        IteratorData<T> it = (IteratorData<T>) iterator;
        assert it.index < it.length;
        Object result = getNextNode.execute(vector, it);
        if (wrapAround) {
            it.index = Utils.incMod(it.index, it.length);
        } else {
            it.index++;
        }
        return (T) result;
    }

    public static final class Generic extends VectorIterator<Object> {

        private Generic(boolean wrapAround) {
            super(wrapAround);
        }

        public static Generic create() {
            return new Generic(false);
        }

        public static Generic createWrapAround() {
            return new Generic(true);
        }
    }

    public static final class Int extends VectorIterator<Integer> {

        public Int(boolean wrapAround) {
            super(wrapAround);
        }

        public static Int create() {
            return new Int(false);
        }
    }

    public static final class Double extends VectorIterator<java.lang.Double> {
        public Double(boolean wrapAround) {
            super(wrapAround);
        }

        public static Double create() {
            return new Double(false);
        }

        public static Double createWrapAround() {
            return new Double(true);
        }
    }

    public static final class Logical extends VectorIterator<Byte> {
        public Logical(boolean wrapAround) {
            super(wrapAround);
        }

        public static Logical create() {
            return new Logical(false);
        }
    }

    public static final class Raw extends VectorIterator<Byte> {
        public Raw(boolean wrapAround) {
            super(wrapAround);
        }

        public static Raw create() {
            return new Raw(false);
        }
    }

    @ValueType
    public static final class IteratorData<StoreT> {
        public int index = 0;
        public final StoreT store;
        public final int length;

        public IteratorData(StoreT store, int length) {
            this.store = store;
            this.length = length;
        }

        public boolean hasNativeMirror() {
            return NativeDataAccess.isNativeMirror(store);
        }
    }
}
