/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.Arrays;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.TypeofNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public class IsFiniteFunctions {

    public abstract static class Adapter extends RBuiltinNode {

        @Child private GetDimAttributeNode getDims = GetDimAttributeNode.create();
        @Child private GetNamesAttributeNode getNames = GetNamesAttributeNode.create();

        @FunctionalInterface
        protected interface ComplexPredicate {
            boolean test(RComplex x);
        }

        @FunctionalInterface
        protected interface LogicalPredicate {
            boolean test(byte x);
        }

        @Specialization
        public RLogicalVector doNull(@SuppressWarnings("unused") RNull x) {
            return RDataFactory.createEmptyLogicalVector();
        }

        @Specialization
        public RLogicalVector doString(RAbstractStringVector x) {
            return doFunConstant(x, RRuntime.LOGICAL_FALSE);
        }

        @Specialization
        public RLogicalVector doRaw(RAbstractRawVector x) {
            return doFunConstant(x, RRuntime.LOGICAL_FALSE);
        }

        @Fallback
        @TruffleBoundary
        protected Object doIsFiniteOther(Object x) {
            throw RError.error(this, RError.Message.DEFAULT_METHOD_NOT_IMPLEMENTED_FOR_TYPE, TypeofNode.getTypeof(x).getName());
        }

        protected RLogicalVector doFunConstant(RAbstractVector x, byte value) {
            byte[] b = new byte[x.getLength()];
            Arrays.fill(b, value);
            return RDataFactory.createLogicalVector(b, RDataFactory.COMPLETE_VECTOR, getDims.getDimensions(x), getNames.getNames(x));
        }

        protected RLogicalVector doFunDouble(RAbstractDoubleVector x, DoublePredicate fun) {
            byte[] b = new byte[x.getLength()];
            for (int i = 0; i < b.length; i++) {
                b[i] = RRuntime.asLogical(fun.test(x.getDataAt(i)));
            }
            return RDataFactory.createLogicalVector(b, RDataFactory.COMPLETE_VECTOR, getDims.getDimensions(x), getNames.getNames(x));
        }

        protected RLogicalVector doFunLogical(RAbstractLogicalVector x, LogicalPredicate fun) {
            byte[] b = new byte[x.getLength()];
            for (int i = 0; i < b.length; i++) {
                b[i] = RRuntime.asLogical(fun.test(x.getDataAt(i)));
            }
            return RDataFactory.createLogicalVector(b, RDataFactory.COMPLETE_VECTOR, getDims.getDimensions(x), getNames.getNames(x));
        }

        protected RLogicalVector doFunInt(RAbstractIntVector x, IntPredicate fun) {
            byte[] b = new byte[x.getLength()];
            for (int i = 0; i < b.length; i++) {
                b[i] = RRuntime.asLogical(fun.test(x.getDataAt(i)));
            }
            return RDataFactory.createLogicalVector(b, RDataFactory.COMPLETE_VECTOR, getDims.getDimensions(x), getNames.getNames(x));
        }

        protected RLogicalVector doFunComplex(RAbstractComplexVector x, ComplexPredicate fun) {
            byte[] b = new byte[x.getLength()];
            for (int i = 0; i < b.length; i++) {
                b[i] = RRuntime.asLogical(fun.test(x.getDataAt(i)));
            }
            return RDataFactory.createLogicalVector(b, RDataFactory.COMPLETE_VECTOR, getDims.getDimensions(x), getNames.getNames(x));
        }
    }

    @RBuiltin(name = "is.finite", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsFinite extends Adapter {

        static {
            Casts.noCasts(IsFinite.class);
        }

        @Specialization
        protected RLogicalVector doIsFinite(RAbstractDoubleVector vec) {
            return doFunDouble(vec, RRuntime::isFinite);
        }

        @Specialization(guards = "vec.isComplete()")
        protected RLogicalVector doComplete(RAbstractIntVector vec) {
            return doFunConstant(vec, RRuntime.LOGICAL_TRUE);
        }

        @Specialization(replaces = "doComplete")
        protected RLogicalVector doIsFinite(RAbstractIntVector vec) {
            return doFunInt(vec, value -> !RRuntime.isNA(value));
        }

        @Specialization(guards = "vec.isComplete()")
        protected RLogicalVector doComplete(RAbstractLogicalVector vec) {
            return doFunConstant(vec, RRuntime.LOGICAL_TRUE);
        }

        @Specialization(replaces = "doComplete")
        protected RLogicalVector doIsFinite(RAbstractLogicalVector vec) {
            return doFunLogical(vec, value -> !RRuntime.isNA(value));
        }

        @Specialization
        protected RLogicalVector doIsFinite(RAbstractComplexVector vec) {
            return doFunComplex(vec, value -> RRuntime.isFinite(value.getRealPart()) && RRuntime.isFinite(value.getImaginaryPart()));
        }
    }

    @RBuiltin(name = "is.infinite", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsInfinite extends Adapter {

        static {
            Casts.noCasts(IsInfinite.class);
        }

        @Specialization
        protected RLogicalVector doIsInfinite(RAbstractDoubleVector vec) {
            return doFunDouble(vec, Double::isInfinite);
        }

        @Specialization
        protected RLogicalVector doComplete(RAbstractIntVector vec) {
            return doFunConstant(vec, RRuntime.LOGICAL_FALSE);
        }

        @Specialization
        protected RLogicalVector doComplete(RAbstractLogicalVector vec) {
            return doFunConstant(vec, RRuntime.LOGICAL_FALSE);
        }

        @Specialization
        protected RLogicalVector doIsInfinite(RAbstractComplexVector vec) {
            return doFunComplex(vec, value -> Double.isInfinite(value.getRealPart()) || Double.isInfinite(value.getImaginaryPart()));
        }
    }

    @RBuiltin(name = "is.nan", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
    public abstract static class IsNaN extends Adapter {

        static {
            Casts.noCasts(IsNaN.class);
        }

        private static boolean isNaN(double value) {
            return Double.isNaN(value) && !RRuntime.isNA(value);
        }

        @Specialization
        protected RLogicalVector doIsNan(RAbstractDoubleVector vec) {
            return doFunDouble(vec, IsNaN::isNaN);
        }

        @Specialization
        protected RLogicalVector doIsNan(RAbstractIntVector vec) {
            return doFunConstant(vec, RRuntime.LOGICAL_FALSE);
        }

        @Specialization
        protected RLogicalVector doIsNan(RAbstractLogicalVector vec) {
            return doFunConstant(vec, RRuntime.LOGICAL_FALSE);
        }

        @Specialization
        protected RLogicalVector doIsNan(RAbstractComplexVector vec) {
            return doFunComplex(vec, value -> isNaN(value.getRealPart()) || isNaN(value.getImaginaryPart()));
        }
    }
}
