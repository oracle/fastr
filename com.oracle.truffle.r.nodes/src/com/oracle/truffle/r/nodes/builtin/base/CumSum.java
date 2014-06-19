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

import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin(name = "cumsum", kind = PRIMITIVE)
public abstract class CumSum extends RBuiltinNode {

    private final NACheck na = NACheck.create();

    @Child private BinaryArithmetic add = BinaryArithmetic.ADD.create();

    @Specialization
    public double cumsum(double arg) {
        controlVisibility();
        return arg;
    }

    @Specialization
    public int cumsum(int arg) {
        controlVisibility();
        return arg;
    }

    @Specialization
    public int cumsum(byte arg) {
        controlVisibility();
        na.enable(arg);
        if (na.check(arg)) {
            return RRuntime.INT_NA;
        }
        return arg;
    }

    @Specialization
    public RIntVector cumsum(RIntSequence arg) {
        controlVisibility();
        int[] res = new int[arg.getLength()];
        int current = arg.getStart();
        int prev = 0;
        int i;
        na.enable(true);
        for (i = 0; i < arg.getLength(); ++i) {
            prev = add.op(prev, current);
            if (na.check(prev)) {
                break;
            }
            current += arg.getStride();
            res[i] = prev;
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(res, i, res.length, RRuntime.INT_NA);
        }
        return RDataFactory.createIntVector(res, RDataFactory.COMPLETE_VECTOR, arg.getNames());
    }

    @Specialization
    public RDoubleVector cumsum(RDoubleVector arg) {
        controlVisibility();
        double[] res = new double[arg.getLength()];
        double prev = 0.0;
        na.enable(arg);
        int i;
        for (i = 0; i < arg.getLength(); ++i) {
            prev = add.op(prev, arg.getDataAt(i));
            if (na.check(arg.getDataAt(i))) {
                break;
            }
            res[i] = prev;
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(res, i, res.length, RRuntime.DOUBLE_NA);
        }
        return RDataFactory.createDoubleVector(res, na.neverSeenNA(), arg.getNames());
    }

    @Specialization
    public RIntVector cumsum(RIntVector arg) {
        controlVisibility();
        int[] res = new int[arg.getLength()];
        int prev = 0;
        int i;
        na.enable(true);
        for (i = 0; i < arg.getLength(); ++i) {
            if (na.check(arg.getDataAt(i))) {
                break;
            }
            prev = add.op(prev, arg.getDataAt(i));
            if (na.check(prev)) {
                break;
            }
            res[i] = prev;
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(res, i, res.length, RRuntime.INT_NA);
        }
        return RDataFactory.createIntVector(res, na.neverSeenNA(), arg.getNames());
    }

    @Specialization
    public RIntVector cumsum(RLogicalVector arg) {
        controlVisibility();
        int[] res = new int[arg.getLength()];
        int prev = 0;
        int i;
        na.enable(true);
        for (i = 0; i < arg.getLength(); ++i) {
            prev = add.op(prev, arg.getDataAt(i));
            if (na.check(arg.getDataAt(i))) {
                break;
            }
            res[i] = prev;
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(res, i, res.length, RRuntime.INT_NA);
        }
        return RDataFactory.createIntVector(res, na.neverSeenNA(), arg.getNames());
    }

    @Specialization
    public RDoubleVector cumsum(RStringVector arg) {
        controlVisibility();
        double[] res = new double[arg.getLength()];
        double prev = 0.0;
        na.enable(arg);
        int i;
        for (i = 0; i < arg.getLength(); ++i) {
            double value = na.convertStringToDouble(arg.getDataAt(i));
            prev = add.op(prev, value);
            if (na.check(arg.getDataAt(i))) {
                break;
            }
            res[i] = prev;
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(res, i, res.length, RRuntime.DOUBLE_NA);
        }
        return RDataFactory.createDoubleVector(res, na.neverSeenNA(), arg.getNames());
    }

    @Specialization
    public RComplexVector cumsum(RComplexVector arg) {
        controlVisibility();
        double[] res = new double[arg.getLength() * 2];
        RComplex prev = RDataFactory.createComplex(0.0, 0.0);
        int i;
        na.enable(true);
        for (i = 0; i < arg.getLength(); ++i) {
            prev = add.op(prev.getRealPart(), prev.getImaginaryPart(), arg.getDataAt(i).getRealPart(), arg.getDataAt(i).getImaginaryPart());
            if (na.check(arg.getDataAt(i))) {
                break;
            }
            res[2 * i] = prev.getRealPart();
            res[2 * i + 1] = prev.getImaginaryPart();
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(res, 2 * i, res.length, RRuntime.DOUBLE_NA);
        }
        return RDataFactory.createComplexVector(res, na.neverSeenNA(), arg.getNames());
    }

}
