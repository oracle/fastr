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
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin(name = "sqrt", kind = PRIMITIVE, parameterNames = {"x"})
public abstract class Sqrt extends RBuiltinNode {

    private final NACheck na = NACheck.create();

    @Specialization
    public double sqrt(double x) {
        controlVisibility();
        na.enable(x);
        if (na.check(x)) {
            return RRuntime.DOUBLE_NA;
        }
        return Math.sqrt(x);
    }

    @Specialization
    protected double sqrt(int x) {
        controlVisibility();
        na.enable(x);
        if (na.check(x)) {
            return RRuntime.DOUBLE_NA;
        }
        return Math.sqrt(x);
    }

    @Specialization
    protected double sqrt(byte x) {
        controlVisibility();
        // sqrt for logical values: TRUE -> 1, FALSE -> 0, NA -> NA
        na.enable(x);
        if (na.check(x)) {
            return RRuntime.DOUBLE_NA;
        }
        return x;
    }

    @Specialization
    protected RDoubleVector sqrt(RIntSequence xs) {
        controlVisibility();
        double[] res = new double[xs.getLength()];
        int current = xs.getStart();
        for (int i = 0; i < xs.getLength(); ++i) {
            res[i] = Math.sqrt(current);
            current += xs.getStride();
        }
        RDoubleVector result = RDataFactory.createDoubleVector(res, na.neverSeenNA(), xs.getDimensions(), xs.getNames());
        result.copyRegAttributesFrom(xs);
        return result;
    }

    @Specialization
    protected RDoubleVector sqrt(RDoubleVector xs) {
        controlVisibility();
        double[] res = new double[xs.getLength()];
        na.enable(xs);
        for (int i = 0; i < xs.getLength(); ++i) {
            res[i] = na.check(xs.getDataAt(i)) ? RRuntime.DOUBLE_NA : Math.sqrt(xs.getDataAt(i));
        }
        RDoubleVector result = RDataFactory.createDoubleVector(res, na.neverSeenNA(), xs.getDimensions(), xs.getNames());
        result.copyRegAttributesFrom(xs);
        return result;
    }

}
