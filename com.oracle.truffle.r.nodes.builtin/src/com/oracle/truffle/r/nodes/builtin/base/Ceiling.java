/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.*;
import static com.oracle.truffle.r.runtime.RDispatch.MATH_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.ops.UnaryArithmeticFactory;

@RBuiltin(name = "ceiling", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
public abstract class Ceiling extends UnaryArithmeticBuiltinNode {

    public static final UnaryArithmeticFactory CEILING = FloorNodeGen.create(null);

    public Ceiling() {
        super(RType.Double, RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION, null);
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("x").mustNotBeNull(this, RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION).mustBe(complexValue().not(), RError.Message.UNIMPLEMENTED_COMPLEX_FUN).asDoubleVector();
    }

    @Override
    public int op(byte op) {
        return op;
    }

    @Override
    public int op(int op) {
        return op;
    }

    @Override
    public double op(double op) {
        return Math.ceil(op);
    }

    @Override
    protected double opd(double re, double im) {
        return op(re);
    }

    @Override
    public RComplex op(double re, double im) {
        return RDataFactory.createComplex(op(re), op(im));
    }

    @Specialization
    @Override
    public Object calculateUnboxed(Object op) {
        return super.calculateUnboxed(op);
    }

}
