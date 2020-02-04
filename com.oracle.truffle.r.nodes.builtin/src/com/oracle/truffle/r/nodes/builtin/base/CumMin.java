/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates
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
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.ExtractNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntSeqVectorData;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "cummin", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
public abstract class CumMin extends RBuiltinNode.Arg1 {

    private final NACheck na = NACheck.create();
    @Child private ExtractNamesAttributeNode extractNamesNode = ExtractNamesAttributeNode.create();

    static {
        Casts casts = new Casts(CumMin.class);
        casts.arg("x").allowNull().mustBe(missingValue().not(), RError.Message.ARGUMENT_EMPTY, 0, "cumsum", 1).mustBe(complexValue().not(), RError.Message.CUMMIN_UNDEFINED_FOR_COMPLEX).mapIf(
                        integerValue().or(logicalValue()), asIntegerVector(true, false, false), asDoubleVector(true, false, false));
    }

    @Specialization
    protected double cummin(double arg) {
        return arg;
    }

    @Specialization
    protected int cummin(int arg) {
        return arg;
    }

    @Specialization
    protected RDoubleVector cumNull(@SuppressWarnings("unused") RNull rnull) {
        return RDataFactory.createEmptyDoubleVector();
    }

    @Specialization(guards = "emptyVec.getLength()==0")
    protected RAbstractVector cumEmpty(RAbstractDoubleVector emptyVec,
                    @Cached("create()") GetNamesAttributeNode getNames) {
        return RDataFactory.createDoubleVector(new double[0], true, getNames.getNames(emptyVec));
    }

    @Specialization(guards = "emptyVec.getLength()==0")
    protected RAbstractVector cumEmpty(RIntVector emptyVec,
                    @Cached("create()") GetNamesAttributeNode getNames) {
        return RDataFactory.createIntVector(new int[0], true, getNames.getNames(emptyVec));
    }

    @Specialization(guards = "v.isSequence()")
    protected RIntVector cumminIntSequence(RIntVector v,
                    @Cached("createBinaryProfile()") ConditionProfile negativeStrideProfile) {
        RIntSeqVectorData seq = (RIntSeqVectorData) v.getData();
        if (negativeStrideProfile.profile(seq.getStride() > 0)) {
            // all numbers are bigger than the first one
            return RDataFactory.createIntSequence(seq.getStart(), 0, v.getLength());
        } else {
            return v;
        }
    }

    @Specialization
    protected RDoubleVector cummin(RAbstractDoubleVector v) {
        double[] cminV = new double[v.getLength()];
        na.enable(v);
        double min = v.getDataAt(0);
        na.check(min);
        cminV[0] = min;
        for (int i = 1; i < v.getLength(); i++) {
            double value = v.getDataAt(i);
            if (na.check(value)) {
                Arrays.fill(cminV, i, cminV.length, RRuntime.DOUBLE_NA);
                break;
            }
            if (na.checkNAorNaN(value)) {
                Arrays.fill(cminV, i, cminV.length, Double.NaN);
                break;
            }
            if (value < min) {
                min = value;
            }
            cminV[i] = min;
        }
        return RDataFactory.createDoubleVector(cminV, na.neverSeenNA(), extractNamesNode.execute(v));
    }

    @Specialization(replaces = "cumminIntSequence")
    protected com.oracle.truffle.r.runtime.data.RIntVector cummin(RIntVector v) {
        int[] cminV = new int[v.getLength()];
        na.enable(v);
        int min = v.getDataAt(0);
        na.check(min);
        cminV[0] = min;
        for (int i = 1; i < v.getLength(); i++) {
            int value = v.getDataAt(i);
            if (na.check(value)) {
                Arrays.fill(cminV, i, cminV.length, RRuntime.INT_NA);
                break;
            }
            if (value < min) {
                min = value;
            }
            cminV[i] = min;
        }
        return RDataFactory.createIntVector(cminV, na.neverSeenNA(), extractNamesNode.execute(v));
    }
}
