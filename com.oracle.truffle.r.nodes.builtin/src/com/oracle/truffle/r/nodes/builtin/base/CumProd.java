/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asIntegerVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.chain;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.complexValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.mapIf;
import static com.oracle.truffle.r.runtime.RDispatch.MATH_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "cumprod", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
public abstract class CumProd extends RBuiltinNode {

    private final NACheck na = NACheck.create();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Child private BinaryArithmetic mul = BinaryArithmetic.MULTIPLY.create();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("x").allowNull().mapIf(integerValue().or(logicalValue()), asIntegerVector(), chain(mapIf(complexValue().not(), asDoubleVector())).end());
    }

    @Specialization
    protected int cumprod(int arg) {
        return arg;
    }

    @Specialization
    protected double cumrpod(double arg) {
        return arg;
    }

    @Specialization
    protected RDoubleVector cumNull(@SuppressWarnings("unused") RNull rnull) {
        return RDataFactory.createEmptyDoubleVector();
    }

    @Specialization
    protected RIntVector cumprod(RAbstractIntVector arg) {
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
        return RDataFactory.createIntVector(array, !na.neverSeenNA(), arg.getNames(attrProfiles));
    }

    @Specialization
    protected RDoubleVector cumprod(RAbstractDoubleVector arg) {
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
        return RDataFactory.createDoubleVector(array, !na.neverSeenNA(), arg.getNames(attrProfiles));
    }

    @Specialization
    protected RComplexVector cumprod(RAbstractComplexVector arg) {
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
        return RDataFactory.createComplexVector(array, !na.neverSeenNA(), arg.getNames(attrProfiles));
    }
}
