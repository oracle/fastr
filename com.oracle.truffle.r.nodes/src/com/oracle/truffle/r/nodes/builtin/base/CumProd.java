/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;

import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin(name = "cumprod", kind = PRIMITIVE)
public abstract class CumProd extends RBuiltinNode {

    private final NACheck na = NACheck.create();

    @Child private BinaryArithmetic mul = BinaryArithmetic.MULTIPLY.create();

    @Specialization
    public int cumprod(int arg) {
        controlVisibility();
        return arg;
    }

    @Specialization
    public double cumrpod(double arg) {
        controlVisibility();
        return arg;
    }

    @Specialization
    public int cumprod(byte arg) {
        controlVisibility();
        na.enable(arg);
        if (na.check(arg)) {
            return RRuntime.INT_NA;
        }
        return arg;
    }

    @Specialization
    public RIntVector cumprod(RAbstractIntVector arg) {
        controlVisibility();
        int[] array = new int[arg.getLength()];
        na.enable(arg);
        int prev = 1;
        int i;
        for (i = 0; i < arg.getLength(); i++) {
            if (na.check(arg.getDataAt(i))) {
                break;
            }
            prev = mul.op(prev, arg.getDataAt(i));
            if (na.check(prev)) {
                break;
            }
            array[i] = prev;
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(array, i, array.length, RRuntime.INT_NA);
        }
        return RDataFactory.createIntVector(array, !na.neverSeenNA(), arg.getNames());
    }

    @Specialization
    public RDoubleVector cumprod(RDoubleVector arg) {
        controlVisibility();
        double[] array = new double[arg.getLength()];
        na.enable(arg);
        double prev = 1;
        int i;
        for (i = 0; i < arg.getLength(); i++) {
            if (na.check(arg.getDataAt(i))) {
                break;
            }
            prev = mul.op(prev, arg.getDataAt(i));
            if (na.check(prev)) {
                break;
            }
            array[i] = prev;
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(array, i, array.length, RRuntime.DOUBLE_NA);
        }
        return RDataFactory.createDoubleVector(array, !na.neverSeenNA(), arg.getNames());
    }

    @Specialization
    public RIntVector cumprod(RLogicalVector arg) {
        controlVisibility();
        int[] array = new int[arg.getLength()];
        na.enable(arg);
        int prev = 1;
        int i;
        for (i = 0; i < arg.getLength(); i++) {
            if (na.check(arg.getDataAt(i))) {
                break;
            }
            prev = mul.op(prev, arg.getDataAt(i));
            if (na.check(prev)) {
                break;
            }
            array[i] = prev;
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(array, i, array.length, RRuntime.INT_NA);
        }
        return RDataFactory.createIntVector(array, !na.neverSeenNA(), arg.getNames());
    }

    @Specialization
    public RDoubleVector cumprod(RStringVector arg) {
        controlVisibility();
        double[] array = new double[arg.getLength()];
        na.enable(arg);
        double prev = 1;
        int i;
        for (i = 0; i < arg.getLength(); i++) {
            prev = mul.op(prev, na.convertStringToDouble(arg.getDataAt(i)));
            if (na.check(arg.getDataAt(i))) {
                break;
            }
            array[i] = prev;
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(array, i, array.length, RRuntime.DOUBLE_NA);
        }
        return RDataFactory.createDoubleVector(array, !na.neverSeenNA(), arg.getNames());
    }

    @Specialization
    public RComplexVector cumprod(RComplexVector arg) {
        controlVisibility();
        double[] array = new double[arg.getLength() * 2];
        na.enable(arg);
        RComplex prev = RDataFactory.createComplex(1, 0);
        int i;
        for (i = 0; i < arg.getLength(); i++) {
            if (na.check(arg.getDataAt(i))) {
                break;
            }
            prev = mul.op(prev.getRealPart(), prev.getImaginaryPart(), arg.getDataAt(i).getRealPart(), arg.getDataAt(i).getImaginaryPart());
            if (na.check(prev)) {
                break;
            }
            array[i * 2] = prev.getRealPart();
            array[i * 2 + 1] = prev.getImaginaryPart();
        }
        if (!na.neverSeenNA()) {
            Arrays.fill(array, 2 * i, array.length, RRuntime.DOUBLE_NA);
        }
        return RDataFactory.createComplexVector(array, !na.neverSeenNA(), arg.getNames());
    }
}