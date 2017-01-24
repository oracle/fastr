/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.complexValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.runtime.RDispatch.MATH_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNodeGen;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.ops.UnaryArithmeticFactory;

@RBuiltin(name = "trunc", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
public abstract class Trunc extends RBuiltinNode {

    private static final UnaryArithmeticFactory TRUNC = TruncArithmetic::new;

    @Child private BoxPrimitiveNode boxPrimitive = BoxPrimitiveNodeGen.create();
    @Child private UnaryArithmeticNode trunc = UnaryArithmeticNodeGen.create(TRUNC, RError.Message.NON_NUMERIC_MATH, RType.Double);

    @Override
    protected void createCasts(CastBuilder casts) {
        //@formatter:off
        casts.arg("x").
            defaultError(this, RError.Message.NON_NUMERIC_MATH).
            mustNotBeNull().
            mustBe(complexValue().not(), RError.Message.UNIMPLEMENTED_COMPLEX_FUN).
            mustBe(numericValue()).
            asDoubleVector(true, true, true);
        //@formatter:on
    }

    @Specialization
    protected Object trunc(Object value) {
        return trunc.execute(boxPrimitive.execute(value));
    }

    private static final class TruncArithmetic extends Round.RoundArithmetic {

        @Override
        public double op(double op) {
            if (op > 0) {
                return Math.floor(op);
            } else {
                return Math.ceil(op);
            }
        }
    }
}
