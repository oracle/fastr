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
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.complexValue;
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
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;

@RBuiltin(name = "cumprod", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = MATH_GROUP_GENERIC, behavior = PURE)
public abstract class CumProd extends RBuiltinNode.Arg1 {

    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();
    @Child private BinaryArithmetic mul = BinaryArithmetic.MULTIPLY.createOperation();

    static {
        Casts casts = new Casts(CumProd.class);
        casts.arg("x").allowNull().mustBe(missingValue().not(), RError.Message.ARGUMENT_EMPTY, 0, "cumsum", 1).mapIf(complexValue().not(), asDoubleVector(true, false, false));
    }

    @Specialization
    protected double cumrpod(double arg) {
        return arg;
    }

    @Specialization
    protected RDoubleVector cumNull(@SuppressWarnings("unused") RNull x) {
        return RDataFactory.createEmptyDoubleVector();
    }

    @Specialization(guards = "xAccess.supports(x)")
    protected RDoubleVector cumprodDouble(RAbstractDoubleVector x,
                    @Cached("x.access()") VectorAccess xAccess) {
        try (SequentialIterator iter = xAccess.access(x)) {
            double[] array = new double[xAccess.getLength(iter)];
            double prev = 1;
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
                prev = mul.op(prev, value);
                assert !RRuntime.isNA(prev) : "double multiplication should not introduce NAs";
                array[iter.getIndex()] = prev;
            }
            return RDataFactory.createDoubleVector(array, xAccess.na.neverSeenNA(), getNamesNode.getNames(x));
        }
    }

    @Specialization(replaces = "cumprodDouble")
    protected RDoubleVector cumprodDoubleGeneric(RAbstractDoubleVector x) {
        return cumprodDouble(x, x.slowPathAccess());
    }

    @Specialization(guards = "xAccess.supports(x)")
    protected RComplexVector cumprodComplex(RAbstractComplexVector x,
                    @Cached("x.access()") VectorAccess xAccess) {
        try (SequentialIterator iter = xAccess.access(x)) {
            double[] array = new double[xAccess.getLength(iter) * 2];
            RComplex prev = RDataFactory.createComplex(1, 0);
            while (xAccess.next(iter)) {
                double real = xAccess.getComplexR(iter);
                double imag = xAccess.getComplexI(iter);
                if (xAccess.na.check(real, imag)) {
                    Arrays.fill(array, 2 * iter.getIndex(), array.length, RRuntime.DOUBLE_NA);
                    break;
                }
                prev = mul.op(prev.getRealPart(), prev.getImaginaryPart(), real, imag);
                assert !RRuntime.isNA(prev) : "complex multiplication should not introduce NAs";
                array[iter.getIndex() * 2] = prev.getRealPart();
                array[iter.getIndex() * 2 + 1] = prev.getImaginaryPart();
            }
            return RDataFactory.createComplexVector(array, xAccess.na.neverSeenNA(), getNamesNode.getNames(x));
        }
    }

    @Specialization(replaces = "cumprodComplex")
    protected RComplexVector cumprodComplexGeneric(RAbstractComplexVector x) {
        return cumprodComplex(x, x.slowPathAccess());
    }
}
