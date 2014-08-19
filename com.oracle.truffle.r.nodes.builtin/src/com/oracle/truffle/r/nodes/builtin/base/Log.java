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
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "log", kind = PRIMITIVE, parameterNames = {"x", "base"})
public abstract class Log extends RBuiltinNode {

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(Math.E)};
    }

    @CreateCast("arguments")
    protected static RNode[] castStatusArgument(RNode[] arguments) {
        // base argument is at index 1, and double
        arguments[1] = CastDoubleNodeFactory.create(arguments[1], true, false, false);
        return arguments;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected RNull log(VirtualFrame frame, RNull x, RNull base) {
        controlVisibility();
        throw RError.error(frame, this.getEncapsulatingSourceSection(), RError.Message.NON_NUMERIC_ARGUMENT_FUNCTION);
    }

    @Specialization
    protected double log(int x, double base) {
        controlVisibility();
        return logb(x, base);
    }

    @Specialization
    protected double log(double x, double base) {
        controlVisibility();
        return logb(x, base);
    }

    @Specialization
    protected RDoubleVector log(RIntVector vector, double base) {
        controlVisibility();
        double[] resultVector = new double[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            int inputValue = vector.getDataAt(i);
            double result = RRuntime.DOUBLE_NA;
            if (RRuntime.isComplete(inputValue)) {
                result = logb(inputValue, base);
            }
            resultVector[i] = result;
        }
        return RDataFactory.createDoubleVector(resultVector, vector.isComplete());
    }

    @Specialization
    protected RDoubleVector log(RDoubleVector vector, double base) {
        controlVisibility();
        double[] doubleVector = new double[vector.getLength()];
        for (int i = 0; i < vector.getLength(); i++) {
            double value = vector.getDataAt(i);
            if (RRuntime.isComplete(value)) {
                value = logb(value, base);
            }
            doubleVector[i] = value;
        }
        return RDataFactory.createDoubleVector(doubleVector, vector.isComplete());
    }

    protected static double logb(double x, double base) {
        return Math.log(x) / Math.log(base);
    }
}
