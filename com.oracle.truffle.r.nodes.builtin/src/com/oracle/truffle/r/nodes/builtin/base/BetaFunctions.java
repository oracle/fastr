/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE_ARITHMETIC;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.stats.StatsFunctionsNodes.Function2_1Node;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;

/**
 * Base package builtins beta and lbeta.
 */
public class BetaFunctions {

    @RBuiltin(name = "lbeta", kind = INTERNAL, parameterNames = {"a", "b"}, behavior = PURE_ARITHMETIC)
    public abstract static class LBeta extends RBuiltinNode.Arg2 {
        static {
            Casts casts = new Casts(LBeta.class);
            casts.arg(0).mustBe(numericValue(), Message.NON_NUMERIC_MATH).asDoubleVector(true, true, true);
            casts.arg(1).mustBe(numericValue(), Message.NON_NUMERIC_MATH).asDoubleVector(true, true, true);
        }

        @Specialization
        protected Object doVectors(RAbstractDoubleVector a, RAbstractDoubleVector b,
                        @Cached("createFuncNode()") Function2_1Node funcNode) {
            // Note: we call execute, which skips the casts of the Function2_1Node
            return funcNode.execute(a, b, true);
        }

        protected Function2_1Node createFuncNode() {
            return Function2_1Node.create(com.oracle.truffle.r.runtime.nmath.LBeta.INSTANCE);
        }
    }

    @RBuiltin(name = "beta", kind = INTERNAL, parameterNames = {"a", "b"}, behavior = PURE_ARITHMETIC)
    public abstract static class BetaBuiltin extends RBuiltinNode.Arg2 {
        static {
            Casts casts = new Casts(BetaBuiltin.class);
            casts.arg(0).mustBe(numericValue(), Message.NON_NUMERIC_MATH).asDoubleVector(true, true, true);
            casts.arg(1).mustBe(numericValue(), Message.NON_NUMERIC_MATH).asDoubleVector(true, true, true);
        }

        @Specialization
        protected Object doVectors(RAbstractDoubleVector a, RAbstractDoubleVector b,
                        @Cached("createFuncNode()") Function2_1Node funcNode) {
            // Note: we call execute, which skips the casts of the Function2_1Node
            return funcNode.execute(a, b, true);
        }

        protected Function2_1Node createFuncNode() {
            return Function2_1Node.create(com.oracle.truffle.r.runtime.nmath.Beta.INSTANCE);
        }
    }
}
