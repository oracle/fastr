/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RDispatch.INTERNAL_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.InitDimsNamesDimNamesNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.TypeofNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class IsFiniteFunctions {

    @ImportStatic(RRuntime.class)
    public abstract static class Adapter extends RBuiltinNode.Arg1 {

        @Child private InitDimsNamesDimNamesNode initDimsNamesDimNames = InitDimsNamesDimNamesNode.create();
        private final Predicates predicates;

        protected Adapter(Predicates predicates) {
            this.predicates = predicates;
        }

        abstract static class Predicates {
            public abstract boolean test(double x);

            public abstract boolean test(RComplex x);

            public boolean test(@SuppressWarnings("unused") int x) {
                throw RInternalError.shouldNotReachHere();
            }

            public boolean test(@SuppressWarnings("unused") byte x) {
                throw RInternalError.shouldNotReachHere();
            }
        }

        @Specialization
        public RLogicalVector doNull(@SuppressWarnings("unused") RNull x) {
            return RDataFactory.createEmptyLogicalVector();
        }

        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        public RLogicalVector doString(RStringVector x,
                        @CachedLibrary("x.getData()") VectorDataLibrary dataLib) {
            return doFunConstant(dataLib, x.getData(), x, RRuntime.LOGICAL_FALSE);
        }

        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        public RLogicalVector doRaw(RRawVector x,
                        @CachedLibrary("x.getData()") VectorDataLibrary dataLib) {
            return doFunConstant(dataLib, x.getData(), x, RRuntime.LOGICAL_FALSE);
        }

        @Specialization(guards = "isForeignObject(obj)")
        @TruffleBoundary
        protected byte doIsForeign(@SuppressWarnings("unused") TruffleObject obj) {
            throw error(RError.Message.DEFAULT_METHOD_NOT_IMPLEMENTED_FOR_TYPE, "polyglot.value");
        }

        @Fallback
        @TruffleBoundary
        protected Object doIsFiniteOther(Object x) {
            throw error(RError.Message.DEFAULT_METHOD_NOT_IMPLEMENTED_FOR_TYPE, TypeofNode.getTypeof(x).getName());
        }

        protected RLogicalVector doFunConstant(VectorDataLibrary dataLib, Object xData, RAbstractVector x, byte value) {
            byte[] b = new byte[dataLib.getLength(xData)];
            Arrays.fill(b, value);
            RLogicalVector result = RDataFactory.createLogicalVector(b, RDataFactory.COMPLETE_VECTOR);
            initDimsNamesDimNames.initAttributes(result, x);
            return result;
        }

        protected RLogicalVector doFunDouble(VectorDataLibrary xDataLib, Object xData, RDoubleVector x) {
            byte[] b = new byte[xDataLib.getLength(xData)];
            for (int i = 0; i < b.length; i++) {
                b[i] = RRuntime.asLogical(predicates.test(xDataLib.getDoubleAt(xData, i)));
            }
            RLogicalVector result = RDataFactory.createLogicalVector(b, RDataFactory.COMPLETE_VECTOR);
            initDimsNamesDimNames.initAttributes(result, x);
            return result;
        }

        protected RLogicalVector doFunLogical(VectorDataLibrary xDataLib, Object xData, RLogicalVector x) {
            byte[] b = new byte[xDataLib.getLength(xData)];
            for (int i = 0; i < b.length; i++) {
                b[i] = RRuntime.asLogical(predicates.test(xDataLib.getLogicalAt(xData, i)));
            }
            RLogicalVector result = RDataFactory.createLogicalVector(b, RDataFactory.COMPLETE_VECTOR);
            initDimsNamesDimNames.initAttributes(result, x);
            return result;
        }

        protected RLogicalVector doFunInt(VectorDataLibrary xDataLib, Object xData, RIntVector x) {
            byte[] b = new byte[xDataLib.getLength(xData)];
            for (int i = 0; i < b.length; i++) {
                b[i] = RRuntime.asLogical(predicates.test(xDataLib.getIntAt(xData, i)));
            }
            RLogicalVector result = RDataFactory.createLogicalVector(b, RDataFactory.COMPLETE_VECTOR);
            initDimsNamesDimNames.initAttributes(result, x);
            return result;
        }

        protected RLogicalVector doFunComplex(VectorDataLibrary xDataLib, Object xData, RComplexVector x) {
            byte[] b = new byte[xDataLib.getLength(xData)];
            for (int i = 0; i < b.length; i++) {
                b[i] = RRuntime.asLogical(predicates.test(xDataLib.getComplexAt(xData, i)));
            }
            RLogicalVector result = RDataFactory.createLogicalVector(b, RDataFactory.COMPLETE_VECTOR);
            initDimsNamesDimNames.initAttributes(result, x);
            return result;
        }
    }

    @RBuiltin(name = "is.finite", kind = PRIMITIVE, dispatch = INTERNAL_GENERIC, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsFinite extends Adapter {

        static {
            Casts.noCasts(IsFinite.class);
        }

        private static final class FinitePredicates extends Predicates {
            private static final FinitePredicates INSTANCE = new FinitePredicates();

            @Override
            public boolean test(double x) {
                return RRuntime.isFinite(x);
            }

            @Override
            public boolean test(int x) {
                return !RRuntime.isNA(x);
            }

            @Override
            public boolean test(byte x) {
                return !RRuntime.isNA(x);
            }

            @Override
            public boolean test(RComplex x) {
                return RRuntime.isFinite(x.getRealPart()) && RRuntime.isFinite(x.getImaginaryPart());
            }
        }

        public IsFinite() {
            super(FinitePredicates.INSTANCE);
        }

        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        protected RLogicalVector doIsFinite(RDoubleVector vec,
                        @CachedLibrary("vec.getData()") VectorDataLibrary dataLib) {
            return doFunDouble(dataLib, vec.getData(), vec);
        }

        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        protected RLogicalVector doIsFinite(RIntVector vec,
                        @Cached("createBinaryProfile()") ConditionProfile isCompleteProfile,
                        @CachedLibrary("vec.getData()") VectorDataLibrary dataLib) {
            final Object vecData = vec.getData();
            if (isCompleteProfile.profile(dataLib.isComplete(vecData))) {
                return doFunConstant(dataLib, vecData, vec, RRuntime.LOGICAL_TRUE);
            } else {
                return doFunInt(dataLib, vecData, vec);
            }
        }

        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        protected RLogicalVector doIsFinite(RLogicalVector vec,
                        @Cached("createBinaryProfile()") ConditionProfile isCompleteProfile,
                        @CachedLibrary("vec.getData()") VectorDataLibrary dataLib) {
            final Object vecData = vec.getData();
            if (isCompleteProfile.profile(dataLib.isComplete(vecData))) {
                return doFunConstant(dataLib, vecData, vec, RRuntime.LOGICAL_TRUE);
            } else {
                return doFunLogical(dataLib, vec.getData(), vec);
            }
        }

        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        protected RLogicalVector doIsFinite(RComplexVector vec,
                        @CachedLibrary("vec.getData()") VectorDataLibrary dataLib) {
            return doFunComplex(dataLib, vec.getData(), vec);
        }
    }

    @RBuiltin(name = "is.infinite", kind = PRIMITIVE, dispatch = INTERNAL_GENERIC, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsInfinite extends Adapter {

        static {
            Casts.noCasts(IsInfinite.class);
        }

        private static final class InfinitePredicates extends Predicates {
            private static final InfinitePredicates INSTANCE = new InfinitePredicates();

            @Override
            public boolean test(double x) {
                return Double.isInfinite(x);
            }

            @Override
            public boolean test(RComplex x) {
                return Double.isInfinite(x.getRealPart()) || Double.isInfinite(x.getImaginaryPart());
            }
        }

        public IsInfinite() {
            super(InfinitePredicates.INSTANCE);
        }

        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        protected RLogicalVector doIsInfinite(RDoubleVector vec,
                        @CachedLibrary("vec.getData()") VectorDataLibrary dataLib) {
            return doFunDouble(dataLib, vec.getData(), vec);
        }

        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        protected RLogicalVector doComplete(RIntVector vec,
                        @CachedLibrary("vec.getData()") VectorDataLibrary dataLib) {
            return doFunConstant(dataLib, vec.getData(), vec, RRuntime.LOGICAL_FALSE);
        }

        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        protected RLogicalVector doComplete(RLogicalVector vec,
                        @CachedLibrary("vec.getData()") VectorDataLibrary dataLib) {
            return doFunConstant(dataLib, vec.getData(), vec, RRuntime.LOGICAL_FALSE);
        }

        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        protected RLogicalVector doIsInfinite(RComplexVector vec,
                        @CachedLibrary("vec.getData()") VectorDataLibrary dataLib) {
            return doFunComplex(dataLib, vec.getData(), vec);
        }
    }

    @RBuiltin(name = "is.nan", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = INTERNAL_GENERIC, behavior = PURE)
    public abstract static class IsNaN extends Adapter {

        static {
            Casts.noCasts(IsNaN.class);
        }

        private static final class NaNPredicates extends Predicates {
            private static final NaNPredicates INSTANCE = new NaNPredicates();

            @Override
            public boolean test(double x) {
                return Double.isNaN(x) && !RRuntime.isNA(x);
            }

            @Override
            public boolean test(RComplex x) {
                return test(x.getRealPart()) || test(x.getImaginaryPart());
            }
        }

        public IsNaN() {
            super(NaNPredicates.INSTANCE);
        }

        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        protected RLogicalVector doIsNan(RDoubleVector vec,
                        @CachedLibrary("vec.getData()") VectorDataLibrary dataLib) {
            return doFunDouble(dataLib, vec.getData(), vec);
        }

        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        protected RLogicalVector doIsNan(RIntVector vec,
                        @CachedLibrary("vec.getData()") VectorDataLibrary dataLib) {
            return doFunConstant(dataLib, vec.getData(), vec, RRuntime.LOGICAL_FALSE);
        }

        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        protected RLogicalVector doIsNan(RLogicalVector vec,
                        @CachedLibrary("vec.getData()") VectorDataLibrary dataLib) {
            return doFunConstant(dataLib, vec.getData(), vec, RRuntime.LOGICAL_FALSE);
        }

        @Specialization(limit = "getTypedVectorDataLibraryCacheSize()")
        protected RLogicalVector doIsNan(RComplexVector vec,
                        @CachedLibrary("vec.getData()") VectorDataLibrary dataLib) {
            return doFunComplex(dataLib, vec.getData(), vec);
        }
    }
}
