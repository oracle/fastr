/*
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2012, The R Core Team
 * Copyright (c) 2004, The R Foundation
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.complexValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.runtime.RDispatch.MATH_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.UnaryCopyAttributesNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.primitive.BinaryMapFunctionNode;
import com.oracle.truffle.r.nodes.primitive.BinaryMapNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nmath.GammaFunctions;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public class BaseGammaFunctions {

    public abstract static class GammaBase extends RBuiltinNode.Arg1 {

        private final NACheck naValCheck = NACheck.create();

        protected static void casts(Casts casts) {
            casts.arg("x").defaultError(RError.Message.NON_NUMERIC_MATH).mustBe(complexValue().not(), RError.Message.UNIMPLEMENTED_COMPLEX_FUN).mustBe(numericValue()).asDoubleVector();
        }

        @Specialization
        protected RDoubleVector digamma(RAbstractDoubleVector x,
                        @Cached("create()") UnaryCopyAttributesNode copyAttributesNode) {
            naValCheck.enable(x);
            double[] result = new double[x.getLength()];
            boolean warnNaN = false;
            for (int i = 0; i < x.getLength(); i++) {
                double xv = x.getDataAt(i);
                double val;
                if (naValCheck.checkNAorNaN(xv)) {
                    if (naValCheck.check(xv)) {
                        val = RRuntime.DOUBLE_NA;
                    } else {
                        val = Double.NaN;
                    }
                } else {
                    val = scalarFunction(xv);
                    if (Double.isNaN(val)) {
                        warnNaN = true;
                    }
                }
                result[i] = val;
            }
            if (warnNaN) {
                warning(RError.Message.NAN_PRODUCED);
            }
            RDoubleVector resultVector = RDataFactory.createDoubleVector(result, naValCheck.neverSeenNA());
            copyAttributesNode.execute(resultVector, x);
            return resultVector;
        }

        protected double scalarFunction(@SuppressWarnings("unused") double xv) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @RBuiltin(name = "gamma", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
    public abstract static class Gamma extends GammaBase {

        static {
            casts(new Casts(Gamma.class));
        }

        @Override
        protected double scalarFunction(double xv) {
            return GammaFunctions.gammafn(xv);
        }
    }

    @RBuiltin(name = "trigamma", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
    public abstract static class TriGamma extends GammaBase {
        static {
            casts(new Casts(TriGamma.class));
        }

        @Override
        protected double scalarFunction(double xv) {
            return GammaFunctions.trigamma(xv);
        }
    }

    @RBuiltin(name = "lgamma", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
    public abstract static class Lgamma extends GammaBase {

        static {
            casts(new Casts(Lgamma.class));
        }

        @Override
        protected double scalarFunction(double xv) {
            return GammaFunctions.lgammafn(xv);
        }
    }

    @RBuiltin(name = "digamma", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
    public abstract static class DiGamma extends GammaBase {

        static {
            casts(new Casts(DiGamma.class));
        }

        @Override
        protected double scalarFunction(double xv) {
            return GammaFunctions.digamma(xv);
        }
    }

    @RBuiltin(name = "tetragamma", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
    public abstract static class TetraGamma extends GammaBase {

        static {
            casts(new Casts(TetraGamma.class));
        }

        @Override
        protected double scalarFunction(double xv) {
            return GammaFunctions.tetragamma(xv);
        }
    }

    @RBuiltin(name = "pentagamma", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
    public abstract static class PentaGamma extends GammaBase {

        static {
            casts(new Casts(PentaGamma.class));
        }

        @Override
        protected double scalarFunction(double xv) {
            return GammaFunctions.pentagamma(xv);
        }
    }

    @RBuiltin(name = "psigamma", kind = INTERNAL, parameterNames = {"x", "deriv"}, behavior = PURE)
    public abstract static class PsiGamma extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(PsiGamma.class);
            casts.arg(0).defaultError(RError.Message.NON_NUMERIC_MATH).mustBe(complexValue().not(), RError.Message.UNIMPLEMENTED_COMPLEX_FUN).mustBe(numericValue()).asDoubleVector();
            casts.arg(1).defaultError(RError.Message.NON_NUMERIC_MATH).mustBe(complexValue().not(), RError.Message.UNIMPLEMENTED_COMPLEX_FUN).mustBe(numericValue()).asDoubleVector();
        }

        @Specialization(guards = "binaryMapNode.isSupported(x, deriv)")
        protected RAbstractDoubleVector psiGammaFast(RAbstractDoubleVector x, RAbstractDoubleVector deriv,
                        @Cached("createFastCached(x, deriv)") BinaryMapNode binaryMapNode) {
            return (RAbstractDoubleVector) binaryMapNode.apply(x, deriv);
        }

        @Specialization(replaces = "psiGammaFast")
        protected RAbstractDoubleVector psiGammaGeneric(RAbstractDoubleVector x, RAbstractDoubleVector deriv,
                        @Cached("createGeneric(x, deriv)") BinaryMapNode binaryMapNode) {
            return (RAbstractDoubleVector) binaryMapNode.apply(x, deriv);
        }

        protected BinaryMapNode createFastCached(RAbstractDoubleVector x, RAbstractDoubleVector deriv) {
            return createCached(x, deriv, false);
        }

        protected BinaryMapNode createGeneric(RAbstractDoubleVector x, RAbstractDoubleVector deriv) {
            return createCached(x, deriv, true);
        }

        protected BinaryMapNode createCached(RAbstractDoubleVector x, RAbstractDoubleVector deriv, boolean isGeneric) {
            return BinaryMapNode.create(new PsiGammaFunction(), x, deriv, RType.Double, RType.Double, false, isGeneric);
        }

    }

    static final class PsiGammaFunction extends BinaryMapFunctionNode {

        @Override
        public double applyDouble(double x, double deriv) {
            return GammaFunctions.psigamma(x, deriv);
        }

        @Override
        public boolean mayFoldConstantTime(Class<? extends RAbstractVector> left, Class<? extends RAbstractVector> right) {
            return false;
        }

        @Override
        public RAbstractVector tryFoldConstantTime(RAbstractVector left, int leftLength, RAbstractVector right, int rightLength) {
            return null;
        }

    }

}
