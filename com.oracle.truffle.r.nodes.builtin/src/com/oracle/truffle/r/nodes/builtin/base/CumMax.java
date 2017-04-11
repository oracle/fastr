/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asIntegerVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.complexValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.missingValue;
import static com.oracle.truffle.r.runtime.RDispatch.MATH_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "cummax", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
public abstract class CumMax extends RBuiltinNode.Arg1 {

    private final NACheck na = NACheck.create();
    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

    static {
        Casts casts = new Casts(CumMax.class);
        casts.arg("x").allowNull().mustBe(missingValue().not(), RError.Message.ARGUMENT_EMPTY, 0, "cumsum", 1).mustBe(complexValue().not(), RError.Message.CUMMAX_UNDEFINED_FOR_COMPLEX).mapIf(
                        integerValue().or(logicalValue()), asIntegerVector(true, false, false), asDoubleVector(true, false, false));
    }

    @Specialization
    protected double cummax(double arg) {
        return arg;
    }

    @Specialization
    protected int cummax(int arg) {
        return arg;
    }

    @Specialization
    protected RDoubleVector cumNull(@SuppressWarnings("unused") RNull rnull) {
        return RDataFactory.createEmptyDoubleVector();
    }

    @Specialization(guards = "emptyVec.getLength()==0")
    protected RAbstractVector cumEmpty(RAbstractDoubleVector emptyVec) {
        return RDataFactory.createDoubleVector(new double[0], true, emptyVec.getNames());
    }

    @Specialization(guards = "emptyVec.getLength()==0")
    protected RAbstractVector cumEmpty(RAbstractIntVector emptyVec) {
        return RDataFactory.createIntVector(new int[0], true, emptyVec.getNames());
    }

    @Specialization
    protected RAbstractIntVector cummaxIntSequence(RIntSequence v,
                    @Cached("createBinaryProfile()") ConditionProfile negativeStrideProfile) {
        if (negativeStrideProfile.profile(v.getStride() < 0)) {
            // all numbers are smaller than the first one
            return RDataFactory.createIntSequence(v.getStart(), 0, v.getLength());
        } else {
            return v;
        }
    }

    @Specialization
    protected RDoubleVector cummax(RAbstractDoubleVector v) {
        double[] cmaxV = new double[v.getLength()];
        na.enable(v);
        double max = v.getDataAt(0);
        cmaxV[0] = max;
        na.check(max);
        for (int i = 1; i < v.getLength(); i++) {
            double value = v.getDataAt(i);
            if (na.check(value)) {
                Arrays.fill(cmaxV, i, cmaxV.length, RRuntime.DOUBLE_NA);
                break;
            }
            if (na.checkNAorNaN(value)) {
                Arrays.fill(cmaxV, i, cmaxV.length, Double.NaN);
                break;
            }
            if (value > max) {
                max = value;
            }
            cmaxV[i] = max;
        }
        return RDataFactory.createDoubleVector(cmaxV, na.neverSeenNA(), getNamesNode.getNames(v));
    }

    @Specialization(replaces = "cummaxIntSequence")
    protected RIntVector cummax(RAbstractIntVector v) {
        int[] cmaxV = new int[v.getLength()];
        na.enable(v);
        int max = v.getDataAt(0);
        na.check(max);
        cmaxV[0] = max;
        for (int i = 1; i < v.getLength(); i++) {
            int value = v.getDataAt(i);
            if (na.check(value)) {
                Arrays.fill(cmaxV, i, cmaxV.length, RRuntime.INT_NA);
                break;
            }
            if (value > max) {
                max = value;
            }
            cmaxV[i] = max;
        }
        return RDataFactory.createIntVector(cmaxV, na.neverSeenNA(), getNamesNode.getNames(v));
    }
}
