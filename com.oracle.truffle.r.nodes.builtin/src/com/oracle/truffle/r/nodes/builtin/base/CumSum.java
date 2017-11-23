/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;

@RBuiltin(name = "cumsum", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
public abstract class CumSum extends RBuiltinNode.Arg1 {

    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();
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
        return RDataFactory.createComplexVector(new double[0], true, getNamesNode.getNames(x));
    }

    @Specialization(guards = "x.getLength()==0")
    protected RAbstractVector cumEmpty(RAbstractDoubleVector x) {
        return RDataFactory.createDoubleVector(new double[0], true, getNamesNode.getNames(x));
    }

    @Specialization(guards = "x.getLength()==0")
    protected RAbstractVector cumEmpty(RAbstractIntVector x) {
        return RDataFactory.createIntVector(new int[0], true, getNamesNode.getNames(x));
    }

    @Specialization(guards = "xAccess.supports(x)")
    protected RIntVector cumsumInt(RAbstractIntVector x,
                    @Cached("x.access()") VectorAccess xAccess) {
        try (SequentialIterator iter = xAccess.access(x)) {
            int[] array = new int[xAccess.getLength(iter)];
            int prev = 0;
            while (xAccess.next(iter)) {
                int value = xAccess.getInt(iter);
                if (xAccess.na.check(value)) {
                    Arrays.fill(array, iter.getIndex(), array.length, RRuntime.INT_NA);
                    break;
                }
                prev = add.op(prev, value);
                // integer addition can introduce NAs
                if (add.introducesNA() && RRuntime.isNA(prev)) {
                    Arrays.fill(array, iter.getIndex(), array.length, RRuntime.INT_NA);
                    break;
                }
                array[iter.getIndex()] = prev;
            }
            return RDataFactory.createIntVector(array, xAccess.na.neverSeenNA() && !add.introducesNA(), getNamesNode.getNames(x));
        }
    }

    @Specialization(replaces = "cumsumInt")
    protected RIntVector cumsumIntGeneric(RAbstractIntVector x) {
        return cumsumInt(x, x.slowPathAccess());
    }

    @Specialization(guards = "xAccess.supports(x)")
    protected RDoubleVector cumsumDouble(RAbstractDoubleVector x,
                    @Cached("x.access()") VectorAccess xAccess) {
        try (SequentialIterator iter = xAccess.access(x)) {
            double[] array = new double[xAccess.getLength(iter)];
            double prev = 0;
            while (xAccess.next(iter)) {
                double value = xAccess.getDouble(iter);
                if (xAccess.na.check(value)) {
                    Arrays.fill(array, iter.getIndex(), array.length, RRuntime.DOUBLE_NA);
                    break;
                }
                if (xAccess.na.checkNAorNaN(value)) {
                    Arrays.fill(array, iter.getIndex(), array.length, Double.NaN);
                    break;
                }
                prev = add.op(prev, value);
                assert !RRuntime.isNA(prev) : "double addition should not introduce NAs";
                array[iter.getIndex()] = prev;
            }
            return RDataFactory.createDoubleVector(array, xAccess.na.neverSeenNA(), getNamesNode.getNames(x));
        }
    }

    @Specialization(replaces = "cumsumDouble")
    protected RDoubleVector cumsumDoubleGeneric(RAbstractDoubleVector x) {
        return cumsumDouble(x, x.slowPathAccess());
    }

    @Specialization(guards = "xAccess.supports(x)")
    protected RComplexVector cumsumComplex(RAbstractComplexVector x,
                    @Cached("x.access()") VectorAccess xAccess) {
        try (SequentialIterator iter = xAccess.access(x)) {
            double[] array = new double[xAccess.getLength(iter) * 2];
            RComplex prev = RDataFactory.createComplex(0, 0);
            while (xAccess.next(iter)) {
                double real = xAccess.getComplexR(iter);
                double imag = xAccess.getComplexI(iter);
                if (xAccess.na.check(real, imag)) {
                    Arrays.fill(array, 2 * iter.getIndex(), array.length, RRuntime.DOUBLE_NA);
                    break;
                }
                prev = add.op(prev.getRealPart(), prev.getImaginaryPart(), real, imag);
                assert !RRuntime.isNA(prev) : "complex addition should not introduce NAs";
                array[iter.getIndex() * 2] = prev.getRealPart();
                array[iter.getIndex() * 2 + 1] = prev.getImaginaryPart();
            }
            return RDataFactory.createComplexVector(array, xAccess.na.neverSeenNA(), getNamesNode.getNames(x));
        }
    }

    @Specialization(replaces = "cumsumComplex")
    protected RComplexVector cumsumComplexGeneric(RAbstractComplexVector x) {
        return cumsumComplex(x, x.slowPathAccess());
    }
}
