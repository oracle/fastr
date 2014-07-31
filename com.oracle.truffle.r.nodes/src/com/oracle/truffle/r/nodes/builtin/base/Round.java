/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin(name = "round", kind = PRIMITIVE, parameterNames = {"x", "digits"})
public abstract class Round extends RBuiltinNode {

    @Child protected UnaryArithmetic roundOp = UnaryArithmetic.ROUND.create();

    private final NACheck check = NACheck.create();

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(0)};
    }

    @CreateCast("arguments")
    protected RNode[] castStatusArgument(RNode[] arguments) {
        // digits argument is at index 1
        arguments[1] = CastIntegerNodeFactory.create(arguments[1], true, false, false);
        return arguments;
    }

    protected static boolean hasDigits(@SuppressWarnings("unused") Object x, int digits) {
        return digits != 0;
    }

    @Specialization(order = 1)
    public int round(int x, @SuppressWarnings("unused") int digits) {
        controlVisibility();
        return roundOp.op(x);
    }

    @Specialization(order = 20, guards = "!hasDigits")
    public double round(double x, @SuppressWarnings("unused") int digits) {
        controlVisibility();
        return roundOp.op(x);
    }

    @Specialization(order = 21, guards = "hasDigits")
    public double roundDigits(double x, int digits) {
        controlVisibility();
        return roundOp.opd(x, digits);
    }

    @Specialization(order = 22, guards = "!hasDigits")
    public RDoubleVector round(RDoubleVector x, int digits) {
        controlVisibility();
        double[] result = new double[x.getLength()];
        check.enable(x);
        for (int i = 0; i < x.getLength(); ++i) {
            result[i] = round(x.getDataAt(i), digits);
            check.check(result[i]);
        }
        return RDataFactory.createDoubleVector(result, check.neverSeenNA());
    }

    @Specialization(order = 23, guards = "hasDigits")
    public RDoubleVector roundDigits(RDoubleVector x, int digits) {
        controlVisibility();
        double[] result = new double[x.getLength()];
        check.enable(x);
        for (int i = 0; i < x.getLength(); ++i) {
            result[i] = roundDigits(x.getDataAt(i), digits);
            check.check(result[i]);
        }
        return RDataFactory.createDoubleVector(result, check.neverSeenNA());
    }

    @Specialization(order = 30, guards = "!hasDigits")
    public RComplex round(RComplex x, @SuppressWarnings("unused") int digits) {
        controlVisibility();
        return roundOp.op(x.getRealPart(), x.getImaginaryPart());
    }

    @Specialization(order = 31, guards = "hasDigits")
    public RComplex roundDigits(RComplex x, int digits) {
        controlVisibility();
        return roundOp.opd(x.getRealPart(), x.getImaginaryPart(), digits);
    }

    @Specialization(order = 32, guards = "!hasDigits")
    public RComplexVector round(RComplexVector x, int digits) {
        controlVisibility();
        double[] result = new double[x.getLength() << 1];
        check.enable(x);
        for (int i = 0; i < x.getLength(); ++i) {
            RComplex z = x.getDataAt(i);
            RComplex r = round(z, digits);
            result[2 * i] = r.getRealPart();
            result[2 * i + 1] = r.getImaginaryPart();
            check.check(r);
        }
        return RDataFactory.createComplexVector(result, check.neverSeenNA());
    }

    @Specialization(order = 33, guards = "hasDigits")
    public RComplexVector roundDigits(RComplexVector x, int digits) {
        controlVisibility();
        double[] result = new double[x.getLength() << 1];
        check.enable(x);
        for (int i = 0; i < x.getLength(); ++i) {
            RComplex z = x.getDataAt(i);
            RComplex r = roundDigits(z, digits);
            result[2 * i] = r.getRealPart();
            result[2 * i + 1] = r.getImaginaryPart();
            check.check(r);
        }
        return RDataFactory.createComplexVector(result, check.neverSeenNA());
    }

}
