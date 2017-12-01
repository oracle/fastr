/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asIntegerVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.UnaryCopyAttributesNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;

/**
 * Base package builtins beta and lbeta.
 */
public class BetaFunctions {

    @RBuiltin(name = "lbeta", kind = INTERNAL, parameterNames = {"a", "b"}, behavior = PURE)
    public abstract static class LBeta extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(LBeta.class);
            casts.arg(0).mustBe(numericValue(), Message.NON_NUMERIC_MATH).mapIf(logicalValue(), asIntegerVector());
            casts.arg(1).mustBe(numericValue(), Message.NON_NUMERIC_MATH).mapIf(logicalValue(), asIntegerVector());
        }

        @Child private UnaryCopyAttributesNode copyAttrs = UnaryCopyAttributesNode.create();

        @Specialization(guards = {"aAccess.supports(a)", "bAccess.supports(b)"})
        protected RAbstractDoubleVector doVectors(RAbstractVector a, RAbstractVector b,
                        @Cached("a.access()") VectorAccess aAccess,
                        @Cached("b.access()") VectorAccess bAccess,
                        @Cached("create()") VectorFactory factory) {
            try (SequentialIterator aIter = aAccess.access(a); SequentialIterator bIter = bAccess.access(b)) {
                int resultLen = Math.max(aAccess.getLength(aIter), bAccess.getLength(bIter));
                double[] result = new double[resultLen];
                for (int i = 0; i < resultLen; i++) {
                    aAccess.nextWithWrap(aIter);
                    bAccess.nextWithWrap(bIter);
                    result[i] = lbeta(aAccess.getDouble(aIter), bAccess.getDouble(bIter));
                }
                RDoubleVector resultVector = factory.createDoubleVector(result, a.isComplete() && b.isComplete());
                if (resultLen == aAccess.getLength(aIter)) {
                    copyAttrs.execute(resultVector, a);
                } else {
                    copyAttrs.execute(resultVector, b);
                }
                return resultVector;
            }
        }

        @Specialization(replaces = "doVectors")
        protected RAbstractDoubleVector doVectorsGeneric(RAbstractVector a, RAbstractVector b,
                        @Cached("create()") VectorFactory factory) {
            return doVectors(a, b, a.slowPathAccess(), b.slowPathAccess(), factory);
        }

        private double lbeta(double a, double b) {
            if (RRuntime.isNA(a) || RRuntime.isNA(b)) {
                return RRuntime.DOUBLE_NA;
            }
            return com.oracle.truffle.r.runtime.nmath.LBeta.lbeta(a, b);
        }

    }

}
