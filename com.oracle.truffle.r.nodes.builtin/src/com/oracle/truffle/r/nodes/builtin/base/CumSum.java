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

import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RDispatch;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "cumsum", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = RDispatch.MATH_GROUP_GENERIC)
public abstract class CumSum extends RBuiltinNode {

    private final NACheck na = NACheck.create();

    @Child private BinaryArithmetic add = BinaryArithmetic.ADD.create();

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Specialization
    protected double cumsum(double arg) {
        return arg;
    }

    @Specialization
    protected int cumsum(int arg) {
        return arg;
    }

    @Specialization
    protected int cumsum(byte arg) {
        na.enable(arg);
        if (na.check(arg)) {
            return RRuntime.INT_NA;
        }
        return arg;
    }

    @Specialization
    protected RIntVector cumsum(RIntSequence arg) {
        int[] res = new int[arg.getLength()];
        int current = arg.getStart();
        int prev = 0;
        int i;
        na.enable(true);
        for (i = 0; i < arg.getLength(); i++) {
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
        return RDataFactory.createIntVector(res, na.neverSeenNA(), arg.getNames(attrProfiles));
    }

    @Specialization
    protected RDoubleVector cumsum(RAbstractDoubleVector arg) {
        double[] res = new double[arg.getLength()];
        double prev = 0.0;
        na.enable(true);
        int i;
        for (i = 0; i < arg.getLength(); i++) {
            prev = add.op(prev, arg.getDataAt(i));
            if (na.check(arg.getDataAt(i))) {
                break;
            }
            res[i] = prev;
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(res, i, res.length, RRuntime.DOUBLE_NA);
        }
        return RDataFactory.createDoubleVector(res, na.neverSeenNA(), arg.getNames(attrProfiles));
    }

    @Specialization
    protected RIntVector cumsum(RAbstractIntVector arg) {
        int[] res = new int[arg.getLength()];
        int prev = 0;
        int i;
        na.enable(true);
        for (i = 0; i < arg.getLength(); i++) {
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
        return RDataFactory.createIntVector(res, na.neverSeenNA(), arg.getNames(attrProfiles));
    }

    @Specialization
    protected RIntVector cumsum(RAbstractLogicalVector arg) {
        int[] res = new int[arg.getLength()];
        int prev = 0;
        int i;
        na.enable(true);
        for (i = 0; i < arg.getLength(); i++) {
            prev = add.op(prev, arg.getDataAt(i));
            if (na.check(arg.getDataAt(i))) {
                break;
            }
            res[i] = prev;
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(res, i, res.length, RRuntime.INT_NA);
        }
        return RDataFactory.createIntVector(res, na.neverSeenNA(), arg.getNames(attrProfiles));
    }

    @Specialization
    protected RDoubleVector cumsum(RAbstractStringVector arg) {
        double[] res = new double[arg.getLength()];
        double prev = 0.0;
        na.enable(true);
        int i;
        for (i = 0; i < arg.getLength(); i++) {
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
        return RDataFactory.createDoubleVector(res, na.neverSeenNA(), arg.getNames(attrProfiles));
    }

    @Specialization
    protected RComplexVector cumsum(RAbstractComplexVector arg) {
        double[] res = new double[arg.getLength() * 2];
        RComplex prev = RDataFactory.createComplex(0.0, 0.0);
        int i;
        na.enable(true);
        for (i = 0; i < arg.getLength(); i++) {
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
        return RDataFactory.createComplexVector(res, na.neverSeenNA(), arg.getNames(attrProfiles));
    }
}
