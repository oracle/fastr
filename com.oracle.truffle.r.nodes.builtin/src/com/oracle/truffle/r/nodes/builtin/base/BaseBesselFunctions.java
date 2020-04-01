/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.nmath.BesselFunctions;

public class BaseBesselFunctions {

    @RBuiltin(name = "besselI", kind = INTERNAL, parameterNames = {"x", "nu", "expo"}, behavior = PURE)
    public abstract static class BesselI extends RBuiltinNode.Arg3 {

        static {
            Casts casts = new Casts(BesselI.class);
            casts.arg(0).mustBe(numericValue()).asDoubleVector();
            casts.arg(1).mustBe(numericValue()).asDoubleVector();
            casts.arg(2).mustBe(numericValue()).asDoubleVector();
        }

        @Specialization(guards = {"xAccess.supports(x)", "nuAccess.supports(nu)", "expoAccess.supports(expo)"})
        protected RDoubleVector doFast(RDoubleVector x, RDoubleVector nu, RDoubleVector expo,
                        @Cached("x.access()") VectorAccess xAccess,
                        @Cached("nu.access()") VectorAccess nuAccess,
                        @Cached("expo.access()") VectorAccess expoAccess,
                        @Cached("create()") VectorFactory factory) {
            SequentialIterator xIter = xAccess.access(x);
            SequentialIterator nuIter = nuAccess.access(nu);
            SequentialIterator expoIter = expoAccess.access(expo);
            int xLen = xAccess.getLength(xIter);
            int nuLen = nuAccess.getLength(nuIter);
            int expoLen = expoAccess.getLength(expoIter);
            int n = (xLen != 0 && nuLen != 0 && expoLen != 0) ? Math.max(Math.max(xLen, nuLen), expoLen) : 0;
            double[] result = new double[n];
            for (int i = 0; i < n; i++) {
                xAccess.nextWithWrap(xIter);
                nuAccess.nextWithWrap(nuIter);
                expoAccess.nextWithWrap(expoIter);
                double xElem = xAccess.getDouble(xIter);
                double nuElem = nuAccess.getDouble(nuIter);
                double expoElem = expoAccess.getDouble(expoIter);
                result[i] = (xAccess.na.check(xElem) || nuAccess.na.check(nuElem) || expoAccess.na.check(expoElem))
                                ? RRuntime.DOUBLE_NA
                                : BesselFunctions.bessel_i(xElem, nuElem, expoElem);
            }
            return factory.createDoubleVector(result, xAccess.na.neverSeenNA() && nuAccess.na.neverSeenNA() && expoAccess.na.neverSeenNA());
        }

        @Specialization(replaces = "doFast")
        protected RDoubleVector doGeneric(RDoubleVector x, RDoubleVector nu, RDoubleVector expo,
                        @Cached("create()") VectorFactory factory) {
            return doFast(x, nu, expo, x.slowPathAccess(), nu.slowPathAccess(), expo.slowPathAccess(), factory);
        }

    }

    @RBuiltin(name = "besselJ", kind = INTERNAL, parameterNames = {"x", "nu"}, behavior = PURE)
    public abstract static class BesselJ extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(BesselJ.class);
            casts.arg(0).mustBe(numericValue()).asDoubleVector();
            casts.arg(1).mustBe(numericValue()).asDoubleVector();
        }

        @Specialization(guards = {"xAccess.supports(x)", "nuAccess.supports(nu)"})
        protected RDoubleVector doFast(RDoubleVector x, RDoubleVector nu,
                        @Cached("x.access()") VectorAccess xAccess,
                        @Cached("nu.access()") VectorAccess nuAccess,
                        @Cached("create()") VectorFactory factory) {
            SequentialIterator xIter = xAccess.access(x);
            SequentialIterator nuIter = nuAccess.access(nu);
            int xLen = xAccess.getLength(xIter);
            int nuLen = nuAccess.getLength(nuIter);
            int n = (xLen != 0 && nuLen != 0) ? Math.max(xLen, nuLen) : 0;
            double[] result = new double[n];
            for (int i = 0; i < n; i++) {
                xAccess.nextWithWrap(xIter);
                nuAccess.nextWithWrap(nuIter);
                double xElem = xAccess.getDouble(xIter);
                double nuElem = nuAccess.getDouble(nuIter);
                result[i] = (xAccess.na.check(xElem) || nuAccess.na.check(nuElem))
                                ? RRuntime.DOUBLE_NA
                                : BesselFunctions.bessel_j(xElem, nuElem);
            }
            return factory.createDoubleVector(result, xAccess.na.neverSeenNA() && nuAccess.na.neverSeenNA());
        }

        @Specialization(replaces = "doFast")
        protected RDoubleVector doGeneric(RDoubleVector x, RDoubleVector nu,
                        @Cached("create()") VectorFactory factory) {
            return doFast(x, nu, x.slowPathAccess(), nu.slowPathAccess(), factory);
        }

    }

    @RBuiltin(name = "besselK", kind = INTERNAL, parameterNames = {"x", "nu", "expo"}, behavior = PURE)
    public abstract static class BesselK extends RBuiltinNode.Arg3 {

        static {
            Casts casts = new Casts(BesselK.class);
            casts.arg(0).mustBe(numericValue()).asDoubleVector();
            casts.arg(1).mustBe(numericValue()).asDoubleVector();
            casts.arg(2).mustBe(numericValue()).asDoubleVector();
        }

        @Specialization(guards = {"xAccess.supports(x)", "nuAccess.supports(nu)", "expoAccess.supports(expo)"})
        protected RDoubleVector doFast(RDoubleVector x, RDoubleVector nu, RDoubleVector expo,
                        @Cached("x.access()") VectorAccess xAccess,
                        @Cached("nu.access()") VectorAccess nuAccess,
                        @Cached("expo.access()") VectorAccess expoAccess,
                        @Cached("create()") VectorFactory factory) {
            SequentialIterator xIter = xAccess.access(x);
            SequentialIterator nuIter = nuAccess.access(nu);
            SequentialIterator expoIter = expoAccess.access(expo);
            int xLen = xAccess.getLength(xIter);
            int nuLen = nuAccess.getLength(nuIter);
            int expoLen = expoAccess.getLength(expoIter);
            int n = (xLen != 0 && nuLen != 0 && expoLen != 0) ? Math.max(Math.max(xLen, nuLen), expoLen) : 0;
            double[] result = new double[n];
            for (int i = 0; i < n; i++) {
                xAccess.nextWithWrap(xIter);
                nuAccess.nextWithWrap(nuIter);
                expoAccess.nextWithWrap(expoIter);
                double xElem = xAccess.getDouble(xIter);
                double nuElem = nuAccess.getDouble(nuIter);
                double expoElem = expoAccess.getDouble(expoIter);
                result[i] = (xAccess.na.check(xElem) || nuAccess.na.check(nuElem) || expoAccess.na.check(expoElem))
                                ? RRuntime.DOUBLE_NA
                                : BesselFunctions.bessel_k(xElem, nuElem, expoElem);
            }
            return factory.createDoubleVector(result, xAccess.na.neverSeenNA() && nuAccess.na.neverSeenNA() && expoAccess.na.neverSeenNA());
        }

        @Specialization(replaces = "doFast")
        protected RDoubleVector doGeneric(RDoubleVector x, RDoubleVector nu, RDoubleVector expo,
                        @Cached("create()") VectorFactory factory) {
            return doFast(x, nu, expo, x.slowPathAccess(), nu.slowPathAccess(), expo.slowPathAccess(), factory);
        }

    }

    @RBuiltin(name = "besselY", kind = INTERNAL, parameterNames = {"x", "nu"}, behavior = PURE)
    public abstract static class BesselY extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(BesselY.class);
            casts.arg(0).mustBe(numericValue()).asDoubleVector();
            casts.arg(1).mustBe(numericValue()).asDoubleVector();
        }

        @Specialization(guards = {"xAccess.supports(x)", "nuAccess.supports(nu)"})
        protected RDoubleVector doFast(RDoubleVector x, RDoubleVector nu,
                        @Cached("x.access()") VectorAccess xAccess,
                        @Cached("nu.access()") VectorAccess nuAccess,
                        @Cached("create()") VectorFactory factory) {
            SequentialIterator xIter = xAccess.access(x);
            SequentialIterator nuIter = nuAccess.access(nu);
            int xLen = xAccess.getLength(xIter);
            int nuLen = nuAccess.getLength(nuIter);
            int n = (xLen != 0 && nuLen != 0) ? Math.max(xLen, nuLen) : 0;
            double[] result = new double[n];
            for (int i = 0; i < n; i++) {
                xAccess.nextWithWrap(xIter);
                nuAccess.nextWithWrap(nuIter);
                double xElem = xAccess.getDouble(xIter);
                double nuElem = nuAccess.getDouble(nuIter);
                result[i] = (xAccess.na.check(xElem) || nuAccess.na.check(nuElem))
                                ? RRuntime.DOUBLE_NA
                                : BesselFunctions.bessel_y(xElem, nuElem);
            }
            return factory.createDoubleVector(result, xAccess.na.neverSeenNA() && nuAccess.na.neverSeenNA());
        }

        @Specialization(replaces = "doFast")
        protected RDoubleVector doGeneric(RDoubleVector x, RDoubleVector nu,
                        @Cached("create()") VectorFactory factory) {
            return doFast(x, nu, x.slowPathAccess(), nu.slowPathAccess(), factory);
        }

    }

}
