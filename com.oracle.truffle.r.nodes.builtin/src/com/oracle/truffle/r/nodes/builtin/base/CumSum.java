/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asIntegerVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.chain;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.complexValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.mapIf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.missingValue;
import static com.oracle.truffle.r.runtime.RDispatch.MATH_GROUP_GENERIC;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.ExtractNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary;
import com.oracle.truffle.r.runtime.data.VectorDataLibrary.SeqIterator;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.WarningInfo;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "cumsum", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
public abstract class CumSum extends RBuiltinNode.Arg1 {

    @Child private ExtractNamesAttributeNode extractNamesNode = ExtractNamesAttributeNode.create();
    @Child private BinaryArithmetic add = BinaryArithmetic.ADD.createOperation();

    static {
        Casts casts = new Casts(CumSum.class);
        casts.arg("x").allowNull().mustBe(missingValue().not(), RError.Message.ARGUMENT_EMPTY, 0, "cumsum", 1).mapIf(integerValue().or(logicalValue()), asIntegerVector(true, false, false),
                        chain(mapIf(complexValue().not(), asDoubleVector(true, false, false))).end());
    }

    @Specialization
    protected double cumsum(double arg) {
        return arg;
    }

    @Specialization
    protected int cumsum(int arg) {
        return arg;
    }

    @Specialization
    protected RDoubleVector cumNull(@SuppressWarnings("unused") RNull x) {
        return RDataFactory.createEmptyDoubleVector();
    }

    @Specialization(guards = "x.getLength()==0")
    protected RAbstractVector cumEmpty(RAbstractComplexVector x) {
        return RDataFactory.createComplexVector(new double[0], true, extractNamesNode.execute(x));
    }

    @Specialization(guards = "x.getLength()==0")
    protected RAbstractVector cumEmpty(RDoubleVector x) {
        return RDataFactory.createDoubleVector(new double[0], true, extractNamesNode.execute(x));
    }

    @Specialization(guards = "x.getLength()==0")
    protected RAbstractVector cumEmpty(RIntVector x) {
        return RDataFactory.createIntVector(new int[0], true, extractNamesNode.execute(x));
    }

    @Specialization(limit = "getVectorAccessCacheSize()")
    protected RIntVector cumsumInt(RIntVector x,
                    @Cached NACheck naCheck,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib,
                    @Cached() BranchProfile hasWarningsBranchProfile) {
        Object xData = x.getData();
        naCheck.enable(xDataLib, xData);
        SeqIterator iter = xDataLib.iterator(xData);
        int[] array = new int[iter.getLength()];
        int prev = 0;
        WarningInfo warningInfo = new WarningInfo();
        while (xDataLib.next(xData, iter)) {
            int value = xDataLib.getNextInt(xData, iter);
            if (naCheck.check(value)) {
                Arrays.fill(array, iter.getIndex(), array.length, RRuntime.INT_NA);
                break;
            }
            prev = add.op(warningInfo, prev, value);
            // integer addition can introduce NAs
            if (add.introducesNA() && RRuntime.isNA(prev)) {
                Arrays.fill(array, iter.getIndex(), array.length, RRuntime.INT_NA);
                break;
            }
            array[iter.getIndex()] = prev;
        }
        if (warningInfo.hasIntergerOverflow()) {
            hasWarningsBranchProfile.enter();
            RError.warning(RError.NO_CALLER, Message.INTEGER_OVERFLOW_USE_NUMERIC, "cumsum", "cumsum");
        }
        return RDataFactory.createIntVector(array, naCheck.neverSeenNA() && !add.introducesNA(), extractNamesNode.execute(x));
    }

    @Specialization(limit = "getVectorAccessCacheSize()")
    protected RDoubleVector cumsumDouble(RDoubleVector x,
                    @Cached NACheck naCheck,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib) {
        Object xData = x.getData();
        naCheck.enable(xDataLib, xData);
        SeqIterator iter = xDataLib.iterator(xData);
        double[] array = new double[iter.getLength()];
        double prev = 0;
        while (xDataLib.next(xData, iter)) {
            double value = xDataLib.getNextDouble(xData, iter);
            if (naCheck.check(value)) {
                Arrays.fill(array, iter.getIndex(), array.length, RRuntime.DOUBLE_NA);
                break;
            }
            if (naCheck.checkNAorNaN(value)) {
                Arrays.fill(array, iter.getIndex(), array.length, Double.NaN);
                break;
            }
            prev = add.op(prev, value);
            assert !RRuntime.isNA(prev) : "double addition should not introduce NAs";
            array[iter.getIndex()] = prev;
        }
        return RDataFactory.createDoubleVector(array, naCheck.neverSeenNA(), extractNamesNode.execute(x));
    }

    @Specialization(limit = "getVectorAccessCacheSize()")
    protected RComplexVector cumsumComplex(RAbstractComplexVector x,
                    @Cached NACheck naCheck,
                    @CachedLibrary("x.getData()") VectorDataLibrary xDataLib) {
        Object xData = x.getData();
        naCheck.enable(xDataLib, xData);
        SeqIterator iter = xDataLib.iterator(xData);
        double[] array = new double[iter.getLength() * 2];
        RComplex prev = RComplex.valueOf(0, 0);
        while (xDataLib.next(xData, iter)) {
            RComplex curr = xDataLib.getNextComplex(xData, iter);
            double real = curr.getRealPart();
            double imag = curr.getImaginaryPart();
            if (naCheck.check(real, imag)) {
                Arrays.fill(array, 2 * iter.getIndex(), array.length, RRuntime.DOUBLE_NA);
                break;
            }
            prev = add.op(prev.getRealPart(), prev.getImaginaryPart(), real, imag);
            assert !RRuntime.isNA(prev) : "complex addition should not introduce NAs";
            array[iter.getIndex() * 2] = prev.getRealPart();
            array[iter.getIndex() * 2 + 1] = prev.getImaginaryPart();
        }
        return RDataFactory.createComplexVector(array, naCheck.neverSeenNA(), extractNamesNode.execute(x));
    }
}
