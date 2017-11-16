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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte0;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRaw;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.SequentialIterator;

@RBuiltin(name = "rep_len", kind = INTERNAL, parameterNames = {"x", "length.out"}, behavior = PURE)
public abstract class RepeatLength extends RBuiltinNode.Arg2 {

    static {
        Casts casts = new Casts(RepeatLength.class);
        casts.arg("x").allowNull().mustBe(abstractVectorValue().or(instanceOf(RExpression.class)), RError.Message.ATTEMPT_TO_REPLICATE_NO_VECTOR);
        casts.arg("length.out").defaultError(RError.Message.INVALID_VALUE, "length.out").mustNotBeNull().asIntegerVector().mustBe(singleElement()).findFirst().mustNotBeNA().mustBe(gte0());
    }

    @Specialization
    @SuppressWarnings("unused")
    protected RNull repLen(RNull value, int length) {
        if (length != 0) {
            RError.error(RError.SHOW_CALLER, RError.Message.CANNOT_REPLICATE_NULL);
        }
        return RNull.instance;
    }

    //
    // Specialization for single values
    //
    @Specialization
    protected RRawVector repLen(RRaw value, int length) {
        byte[] array = new byte[length];
        Arrays.fill(array, value.getValue());
        return RDataFactory.createRawVector(array);
    }

    @Specialization
    protected RIntVector repLen(int value, int length) {
        int[] array = new int[length];
        Arrays.fill(array, value);
        return RDataFactory.createIntVector(array, !RRuntime.isNA(value));
    }

    @Specialization
    protected RDoubleVector repLen(double value, int length) {
        double[] array = new double[length];
        Arrays.fill(array, value);
        return RDataFactory.createDoubleVector(array, !RRuntime.isNA(value));
    }

    @Specialization
    protected RStringVector repLen(String value, int length) {
        String[] array = new String[length];
        Arrays.fill(array, value);
        return RDataFactory.createStringVector(array, !RRuntime.isNA(value));
    }

    @Specialization
    protected RComplexVector repLen(RComplex value, int length) {
        int complexLength = length * 2;
        double[] array = new double[complexLength];
        for (int i = 0; i < complexLength; i += 2) {
            array[i] = value.getRealPart();
            array[i + 1] = value.getImaginaryPart();
        }
        return RDataFactory.createComplexVector(array, !value.isNA());
    }

    @Specialization
    protected RLogicalVector repLen(byte value, int length) {
        byte[] array = new byte[length];
        Arrays.fill(array, value);
        return RDataFactory.createLogicalVector(array, value != RRuntime.LOGICAL_NA);
    }

    @Specialization(guards = "xAccess.supports(x)")
    protected RAbstractVector repLenCached(RAbstractVector x, int length,
                    @Cached("x.access()") VectorAccess xAccess,
                    @Cached("createNew(xAccess.getType())") VectorAccess resultAccess,
                    @Cached("createBinaryProfile()") ConditionProfile emptyProfile,
                    @Cached("create()") VectorFactory factory) {
        try (SequentialIterator xIter = xAccess.access(x)) {
            if (emptyProfile.profile(xAccess.getLength(xIter) == 0)) {
                return factory.createVector(xAccess.getType(), length, true);
            }
            RAbstractVector result = factory.createVector(xAccess.getType(), length, false);
            try (SequentialIterator resultIter = resultAccess.access(result)) {
                while (resultAccess.next(resultIter)) {
                    xAccess.nextWithWrap(xIter);
                    resultAccess.setFromSameType(resultIter, xAccess, xIter);
                }
            }
            result.setComplete(x.isComplete());
            return result;
        }
    }

    @Specialization(replaces = "repLenCached")
    protected RAbstractVector repLenGeneric(RAbstractVector x, int length,
                    @Cached("createBinaryProfile()") ConditionProfile emptyProfile,
                    @Cached("create()") VectorFactory factory) {
        return repLenCached(x, length, x.slowPathAccess(), VectorAccess.createSlowPathNew(x.getRType()), emptyProfile, factory);
    }

}
