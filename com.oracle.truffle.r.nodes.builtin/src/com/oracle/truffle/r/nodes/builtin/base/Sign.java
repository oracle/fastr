/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNodeGen;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticNode;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticNodeGen;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.ops.UnaryArithmetic;

@RBuiltin(name = "sign", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
public abstract class Sign extends RBuiltinNode {

    @Child private BoxPrimitiveNode boxPrimitive = BoxPrimitiveNodeGen.create();
    @Child private UnaryArithmeticNode signNode = UnaryArithmeticNodeGen.create(SignArithmetic::new, RType.Logical,
                        RError.Message.ARGUMENTS_PASSED_0_1, new Object[]{getRBuiltin().name()});

    @Specialization
    protected Object sign(Object x) {
        return signNode.execute(boxPrimitive.execute(x));
    }

    static final class SignArithmetic extends UnaryArithmetic {

            @Override
            public int op(byte op) {
                return op == RRuntime.LOGICAL_TRUE ? 1 : 0;
            }

            @Override
            public int op(int op) {
                return op == 0 ? 0 : (op > 0 ? 1 : -1);
            }

            @Override
            public double op(double op) {
                return Math.signum(op);
            }

            @Override
            public RComplex op(double re, double im) {
                throw new UnsupportedOperationException();
            }
    }
}
