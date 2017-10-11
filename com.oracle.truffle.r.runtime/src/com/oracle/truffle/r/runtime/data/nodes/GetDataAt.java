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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDoubleSequence;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.GetDataAtFactory.DoubleNodeGen;
import com.oracle.truffle.r.runtime.data.nodes.GetDataAtFactory.IntNodeGen;

/**
 * Contains nodes implementing fast-path versions of e.g.
 * {@link RAbstractIntVector#getDataAt(Object, int)}. The first parameter 'store' should be
 * retrieved for given vector using {@link GetDataStore} node. Store object must be used only for
 * accessing the data of the vector for which it was created. The reason for having a store object
 * is to avoid field load (field of the RVector object) on every data access.
 */
public abstract class GetDataAt extends Node {

    public abstract Object getAsObject(RAbstractVector vector, Object store, int index);

    @ImportStatic(NativeDataAccess.class)
    public abstract static class Int extends GetDataAt {

        public static Int create() {
            return IntNodeGen.create();
        }

        @Override
        public Object getAsObject(RAbstractVector vector, Object store, int index) {
            return get((RAbstractIntVector) vector, store, index);
        }

        public final int get(RAbstractIntVector vector, Object store, int index) {
            return execute(vector, store, index);
        }

        public abstract int execute(RAbstractIntVector vector, Object store, int index);

        protected int doRVector(RIntVector vector, int[] store, int index) {
            return store[index];
        }

        @Specialization(guards = "isNativeMirror(store)")
        protected int doRVector(RIntVector vector, Object store, int index) {
            return NativeDataAccess.getIntNativeMirrorData(store, index);
        }

        @Specialization
        protected int doSequence(RIntSequence sequence, Object store, int index) {
            return sequence.getStart() + index * sequence.getStride();
        }

        // This accounts for other vector types, like closures
        @Specialization(guards = {"isGenericVector(vector)", "cachedClass == vector.getClass()"})
        protected int doDoubleClosure(RAbstractIntVector vector, Object store, int index,
                        @Cached("vector.getClass()") Class<? extends RAbstractIntVector> cachedClass) {
            return cachedClass.cast(vector).getDataAt(store, index);
        }

        @Fallback
        protected int doDoubleClosure(RAbstractIntVector vector, Object store, int index) {
            return vector.getDataAt(store, index);
        }

        protected static boolean isGenericVector(RAbstractIntVector vector) {
            return !(vector instanceof RIntVector) && !(vector instanceof RIntSequence);
        }
    }

    @ImportStatic(NativeDataAccess.class)
    public abstract static class Double extends GetDataAt {

        public static Double create() {
            return DoubleNodeGen.create();
        }

        @Override
        public Object getAsObject(RAbstractVector vector, Object store, int index) {
            return get((RAbstractDoubleVector) vector, store, index);
        }

        public final double get(RAbstractDoubleVector vector, Object store, int index) {
            return execute(vector, store, index);
        }

        public abstract double execute(RAbstractDoubleVector vector, Object store, int index);

        @Specialization(guards = "isNativeMirror(store)")
        protected double doRVector(RDoubleVector vector, Object store, int index) {
            return NativeDataAccess.getDoubleNativeMirrorData(store, index);
        }

        @Specialization
        protected double doRVector(RDoubleVector vector, double[] store, int index) {
            return store[index];
        }

        @Specialization
        protected double doSequence(RDoubleSequence sequence, Object store, int index) {
            return sequence.getStart() + index * sequence.getStride();
        }

        @Specialization(guards = {"isGenericVector(vector)", "cachedClass == vector.getClass()"}, limit = "3")
        protected double doDoubleClosure(RAbstractDoubleVector vector, Object store, int index,
                        @Cached("vector.getClass()") Class<?> cachedClass) {
            return ((RAbstractDoubleVector) cachedClass.cast(vector)).getDataAt(store, index);
        }

        @Fallback
        protected double doDoubleClosure(RAbstractDoubleVector vector, Object store, int index) {
            return vector.getDataAt(store, index);
        }

        protected static boolean isGenericVector(RAbstractDoubleVector vector) {
            return !(vector instanceof RDoubleVector) && !(vector instanceof RDoubleSequence);
        }
    }

    @ImportStatic(NativeDataAccess.class)
    public abstract static class Logical extends GetDataAt {

        public static Logical create() {
            return GetDataAtFactory.LogicalNodeGen.create();
        }

        @Override
        public Object getAsObject(RAbstractVector vector, Object store, int index) {
            return get((RAbstractLogicalVector) vector, store, index);
        }

        public final byte get(RAbstractLogicalVector vector, Object store, int index) {
            return execute(vector, store, index);
        }

        public abstract byte execute(RAbstractLogicalVector vector, Object store, int index);

        protected byte doRVector(RLogicalVector vector, byte[] store, int index) {
            return store[index];
        }

        @Specialization(guards = "isNativeMirror(store)")
        protected byte doRVector(RLogicalVector vector, Object store, int index) {
            return NativeDataAccess.getLogicalNativeMirrorData(store, index);
        }

        // This accounts for other vector types, like closures
        @Specialization(guards = {"isGenericVector(vector)", "cachedClass == vector.getClass()"})
        protected byte doDoubleClosure(RAbstractLogicalVector vector, Object store, int index,
                        @Cached("vector.getClass()") Class<? extends RAbstractLogicalVector> cachedClass) {
            return cachedClass.cast(vector).getDataAt(store, index);
        }

        @Fallback
        protected byte doDoubleClosure(RAbstractLogicalVector vector, Object store, int index) {
            return vector.getDataAt(store, index);
        }

        protected static boolean isGenericVector(RAbstractLogicalVector vector) {
            return !(vector instanceof RLogicalVector);
        }
    }

    @ImportStatic(NativeDataAccess.class)
    public abstract static class Raw extends GetDataAt {

        public static Raw create() {
            return GetDataAtFactory.RawNodeGen.create();
        }

        @Override
        public Object getAsObject(RAbstractVector vector, Object store, int index) {
            return get((RAbstractRawVector) vector, store, index);
        }

        public final byte get(RAbstractRawVector vector, Object store, int index) {
            return execute(vector, store, index);
        }

        public abstract byte execute(RAbstractRawVector vector, Object store, int index);

        protected byte doRVector(RRawVector vector, byte[] store, int index) {
            return store[index];
        }

        @Specialization(guards = "isNativeMirror(store)")
        protected byte doRVector(RRawVector vector, Object store, int index) {
            return NativeDataAccess.getRawNativeMirrorData(store, index);
        }

        // This accounts for other vector types, like closures
        @Specialization(guards = {"isGenericVector(vector)", "cachedClass == vector.getClass()"})
        protected byte doDoubleClosure(RAbstractRawVector vector, Object store, int index,
                        @Cached("vector.getClass()") Class<? extends RAbstractRawVector> cachedClass) {
            return cachedClass.cast(vector).getRawDataAt(store, index);
        }

        @Fallback
        protected byte doDoubleClosure(RAbstractRawVector vector, Object store, int index) {
            return vector.getRawDataAt(store, index);
        }

        protected static boolean isGenericVector(RAbstractRawVector vector) {
            return !(vector instanceof RRawVector);
        }
    }

    @ImportStatic(NativeDataAccess.class)
    public abstract static class Complex extends GetDataAt {

        public static Complex create() {
            return GetDataAtFactory.ComplexNodeGen.create();
        }

        @Override
        public Object getAsObject(RAbstractVector vector, Object store, int index) {
            return get((RAbstractComplexVector) vector, store, index);
        }

        public final RComplex get(RAbstractComplexVector vector, Object store, int index) {
            return execute(vector, store, index);
        }

        public abstract RComplex execute(RAbstractComplexVector vector, Object store, int index);

        protected RComplex doRVector(RComplexVector vector, double[] store, int index) {
            return RComplex.valueOf(store[index * 2], store[index * 2 + 1]);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isNativeMirror(store)")
        protected RComplex doRVector(RComplexVector vector, Object store, int index) {
            throw RInternalError.unimplemented();
        }

        @Specialization(guards = {"isGenericVector(vector)", "cachedClass == vector.getClass()"})
        protected RComplex doDoubleClosure(RAbstractComplexVector vector, Object store, int index,
                        @Cached("vector.getClass()") Class<? extends RAbstractComplexVector> cachedClass) {
            return cachedClass.cast(vector).getDataAt(store, index);
        }

        @Fallback
        protected RComplex doDoubleClosure(RAbstractComplexVector vector, Object store, int index) {
            return vector.getDataAt(store, index);
        }

        protected static boolean isGenericVector(RAbstractComplexVector vector) {
            return !(vector instanceof RComplexVector);
        }
    }

    @ImportStatic(NativeDataAccess.class)
    public abstract static class String extends GetDataAt {

        public static String create() {
            return GetDataAtFactory.StringNodeGen.create();
        }

        @Override
        public Object getAsObject(RAbstractVector vector, Object store, int index) {
            return get((RAbstractStringVector) vector, store, index);
        }

        public final java.lang.String get(RAbstractStringVector vector, Object store, int index) {
            return execute(vector, store, index);
        }

        public abstract java.lang.String execute(RAbstractStringVector vector, Object store, int index);

        protected java.lang.String doRVector(RStringVector vector, java.lang.String[] store, int index) {
            return store[index];
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isNativeMirror(store)")
        protected java.lang.String doRVector(RStringVector vector, Object store, int index) {
            throw RInternalError.unimplemented();
        }

        @Specialization(guards = {"isGenericVector(vector)", "cachedClass == vector.getClass()"})
        protected java.lang.String doDoubleClosure(RAbstractStringVector vector, Object store, int index,
                        @Cached("vector.getClass()") Class<? extends RAbstractStringVector> cachedClass) {
            return cachedClass.cast(vector).getDataAt(store, index);
        }

        @Fallback
        protected java.lang.String doDoubleClosure(RAbstractStringVector vector, Object store, int index) {
            return vector.getDataAt(store, index);
        }

        protected static boolean isGenericVector(RAbstractStringVector vector) {
            return !(vector instanceof RStringVector);
        }
    }
}
