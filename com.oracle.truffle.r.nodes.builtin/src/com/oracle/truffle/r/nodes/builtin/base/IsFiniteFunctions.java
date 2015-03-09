/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

public class IsFiniteFunctions {

    public abstract static class Adapter extends RBuiltinNode {
        protected interface IsFinCall {
            boolean call(double x);
        }

        protected interface IsComplexFinCall {
            boolean call(RComplex x);
        }

        @Specialization
        public RLogicalVector doNull(@SuppressWarnings("unused") RNull x) {
            return RDataFactory.createEmptyLogicalVector();
        }

        private static RLogicalVector doAllFalse(int length) {
            byte[] data = new byte[length];
            for (int i = 0; i < data.length; i++) {
                data[i] = RRuntime.LOGICAL_FALSE;
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        }

        @Specialization
        public RLogicalVector doString(RAbstractStringVector x) {
            return doAllFalse(x.getLength());
        }

        @Specialization
        public RLogicalVector doRaw(RAbstractRawVector x) {
            return doAllFalse(x.getLength());
        }

        @Specialization
        public RLogicalVector doLogical(RAbstractLogicalVector x) {
            // TODO is.infinite
            byte[] data = new byte[x.getLength()];
            for (int i = 0; i < data.length; i++) {
                data[i] = RRuntime.asLogical(!RRuntime.isNA(x.getDataAt(i)));
            }
            return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);

        }

        @Child private TypeofNode typeofNode;

        @Fallback
        protected Object doIsFiniteOther(Object x) {
            controlVisibility();
            if (typeofNode == null) {
                typeofNode = insert(TypeofNodeGen.create(null));
            }
            String type = typeofNode.execute(x).getName();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.DEFAULT_METHOD_NOT_IMPLEMENTED_FOR_TYPE, type);
        }

        protected RLogicalVector doFun(RAbstractVector x, IsFinCall fun) {
            boolean isDouble = x instanceof RDoubleVector;
            RDoubleVector dx = isDouble ? (RDoubleVector) x : null;
            RIntVector ix = !isDouble ? (RIntVector) x : null;
            byte[] b = new byte[x.getLength()];
            for (int i = 0; i < b.length; i++) {
                b[i] = RRuntime.asLogical(fun.call(isDouble ? dx.getDataAt(i) : ix.getDataAt(i)));
            }
            return RDataFactory.createLogicalVector(b, RDataFactory.COMPLETE_VECTOR);
        }

        protected RLogicalVector doFun(RAbstractComplexVector x, IsComplexFinCall fun) {
            byte[] b = new byte[x.getLength()];
            for (int i = 0; i < b.length; i++) {
                b[i] = RRuntime.asLogical(fun.call(x.getDataAt(i)));
            }
            return RDataFactory.createLogicalVector(b, RDataFactory.COMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = "is.finite", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsFinite extends Adapter {

        @Specialization
        protected RLogicalVector doIsFinite(RAbstractDoubleVector vec) {
            controlVisibility();
            return doFun(vec, RRuntime::isFinite);
        }

        @Specialization
        protected RLogicalVector doIsFinite(RAbstractIntVector vec) {
            controlVisibility();
            return doFun(vec, RRuntime::isFinite);
        }

        private static boolean bothFinite(RComplex complex) {
            return RRuntime.isFinite(complex.getRealPart()) && RRuntime.isFinite(complex.getImaginaryPart());
        }

        @Specialization
        protected RLogicalVector doIsFinite(RAbstractComplexVector vec) {
            controlVisibility();
            return doFun(vec, IsFinite::bothFinite);
        }

    }

    @RBuiltin(name = "is.infinite", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsInfinite extends Adapter {

        @Specialization
        protected RLogicalVector doIsInfinite(RAbstractDoubleVector vec) {
            controlVisibility();
            return doFun(vec, Double::isInfinite);
        }

        @Specialization
        protected RLogicalVector doIsInfinite(RAbstractIntVector vec) {
            controlVisibility();
            return doFun(vec, Double::isInfinite);
        }

        private static boolean eitherInfinite(RComplex complex) {
            return Double.isFinite(complex.getRealPart()) || Double.isFinite(complex.getImaginaryPart());
        }

        @Specialization
        protected RLogicalVector doIsInfinite(RAbstractComplexVector vec) {
            controlVisibility();
            return doFun(vec, IsInfinite::eitherInfinite);
        }

    }

    @RBuiltin(name = "is.nan", kind = PRIMITIVE, parameterNames = {"x"})
    public abstract static class IsNan extends Adapter {

        @Specialization
        protected RLogicalVector doIsNan(RAbstractDoubleVector vec) {
            controlVisibility();
            return doFun(vec, Double::isNaN);
        }

        @Specialization
        protected RLogicalVector doIsNan(RAbstractIntVector vec) {
            controlVisibility();
            return doFun(vec, Double::isNaN);
        }

        private static boolean eitherNaN(RComplex complex) {
            return Double.isNaN(complex.getRealPart()) || Double.isNaN(complex.getImaginaryPart());
        }

        @Specialization
        protected RLogicalVector doIsNan(RAbstractComplexVector vec) {
            controlVisibility();
            return doFun(vec, IsNan::eitherNaN);
        }

    }

}
