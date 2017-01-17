/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNodeGen;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.ops.UnaryArithmetic;
import com.oracle.truffle.r.runtime.ops.UnaryArithmeticFactory;

public abstract class UnaryArithmeticBuiltinNode extends RBuiltinNode implements UnaryArithmeticFactory {

    @Child private BoxPrimitiveNode boxPrimitive = BoxPrimitiveNodeGen.create();
    @Child private UnaryArithmeticNode unaryNode;

    protected UnaryArithmeticBuiltinNode(RType minPrecedence, RError.Message error, Object[] errorArgs) {
        unaryNode = UnaryArithmeticNodeGen.create(this, minPrecedence, error, errorArgs);
    }

    protected UnaryArithmeticBuiltinNode(RType minPrecedence) {
        unaryNode = UnaryArithmeticNodeGen.create(this, minPrecedence, RError.Message.ARGUMENTS_PASSED, new Object[]{0, "'" + getRBuiltin().name() + "'", 1});
    }

    @Specialization
    public Object calculateUnboxed(Object value) {
        return unaryNode.execute(boxPrimitive.execute(value));
    }

    @Override
    public UnaryArithmetic createOperation() {
        return new UnaryArithmetic() {

            @Override
            public RType calculateResultType(RType argumentType) {
                return UnaryArithmeticBuiltinNode.this.calculateResultType(argumentType);
            }

            @Override
            public double opd(double re, double im) {
                return UnaryArithmeticBuiltinNode.this.opd(re, im);
            }

            @Override
            public int op(byte op) {
                return UnaryArithmeticBuiltinNode.this.op(op);
            }

            @Override
            public int op(int op) {
                return UnaryArithmeticBuiltinNode.this.op(op);
            }

            @Override
            public double op(double op) {
                return UnaryArithmeticBuiltinNode.this.op(op);
            }

            @Override
            public RComplex op(double re, double im) {
                return UnaryArithmeticBuiltinNode.this.op(re, im);
            }
        };
    }

    protected RType calculateResultType(RType argumentType) {
        return argumentType;
    }

    @SuppressWarnings("unused")
    protected int op(byte op) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    protected int op(int op) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    protected double op(double op) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    protected RComplex op(double re, double im) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    protected double opd(double re, double im) {
        throw new UnsupportedOperationException();
    }
}
