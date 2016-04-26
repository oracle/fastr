/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.Lapply.LapplyInternalNode;
import com.oracle.truffle.r.nodes.builtin.base.LapplyNodeGen.LapplyInternalNodeGen;
import com.oracle.truffle.r.nodes.unary.CastComplexNode;
import com.oracle.truffle.r.nodes.unary.CastComplexNodeGen;
import com.oracle.truffle.r.nodes.unary.CastDoubleNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleNodeGen;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.nodes.unary.CastLogicalNode;
import com.oracle.truffle.r.nodes.unary.CastLogicalNodeGen;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * The {@code vapply} builtin. The closure definition for {@code vapply} is
 * {@code function(X, FUN, FUN.VALUE, ...,  USE.NAMES = TRUE)}. The {@code .Internal} call in
 * {@code sapply.R} is {@code .Internal(vapply(X, FUN, FUN.VALUE, USE.NAMES))}. I.e., the "..." is
 * not passed even though, for correct operation, any extra arguments must be passed to {@code FUN}.
 * In order for FastR to use the same version of {@code sapply.R} as GnuR, we have to define the
 * specialization signature without the "...", which means we have to fish the optional arguments
 * out of the frame for the closure.
 *
 * TODO Set dimnames on result if necessary.
 */
@RBuiltin(name = "vapply", kind = INTERNAL, parameterNames = {"X", "FUN", "FUN.VALUE", "USE.NAMES"}, splitCaller = true)
public abstract class VApply extends RBuiltinNode {

    private final ValueProfile funValueProfile = ValueProfile.createClassProfile();
    private final ConditionProfile useNamesProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile dimsProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile errorProfile = BranchProfile.create();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Child private LapplyInternalNode doApply = LapplyInternalNodeGen.create();

    @Child private CastComplexNode castComplex;
    @Child private CastDoubleNode castDouble;
    @Child private CastIntegerNode castInteger;
    @Child private CastLogicalNode castLogical;
    @Child private CastStringNode castString;

    private Object castComplex(Object operand, boolean preserveAllAttr) {
        if (castComplex == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castComplex = insert(CastComplexNodeGen.create(true, preserveAllAttr, preserveAllAttr));
        }
        return castComplex.execute(operand);
    }

    private Object castDouble(Object operand, boolean preserveAllAttr) {
        if (castDouble == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castDouble = insert(CastDoubleNodeGen.create(true, preserveAllAttr, preserveAllAttr));
        }
        return castDouble.execute(operand);
    }

    private Object castInteger(Object operand, boolean preserveAllAttr) {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInteger = insert(CastIntegerNodeGen.create(true, preserveAllAttr, preserveAllAttr));
        }
        return castInteger.execute(operand);
    }

    private Object castLogical(Object operand, boolean preserveAllAttr) {
        if (castLogical == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castLogical = insert(CastLogicalNodeGen.create(true, preserveAllAttr, preserveAllAttr));
        }
        return castLogical.execute(operand);
    }

    private Object castString(Object operand, boolean preserveAllAttr) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeGen.create(false, true, preserveAllAttr, preserveAllAttr));
        }
        return castString.execute(operand);
    }

    @Specialization
    protected Object vapply(VirtualFrame frame, RAbstractVector vec, RFunction fun, Object funValue, byte useNames) {
        RArgsValuesAndNames optionalArgs = (RArgsValuesAndNames) RArguments.getArgument(frame, 3);
        RVector result = delegateToLapply(frame, vec, fun, funValue, useNames, optionalArgs);
        // set here else it gets overridden by the iterator evaluation
        controlVisibility();
        return result;
    }

    private RVector delegateToLapply(VirtualFrame frame, RAbstractVector vec, RFunction fun, Object funValueArg, byte useNames, RArgsValuesAndNames optionalArgs) {
        /*
         * The implementation is complicated by the existence of scalar length 1 vectors (e.g.
         * Integer) and concrete length 1 vectors (e.g. RIntVector), as either form can occur in
         * both funValueArg and the result of the doApply. At a slight performance cost this code
         * works exclusively in terms of concrete vectors.
         */
        Object funValueObj = RRuntime.asAbstractVector(funValueArg);
        if (!(funValueObj instanceof RAbstractVector)) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.MUST_BE_VECTOR, "FUN.VALUE");
        }
        RAbstractVector funValueVec = funValueProfile.profile((RAbstractVector) funValueObj);
        int funValueVecLen = funValueVec.getLength();

        RVector vecMat = vec.materialize();
        Object[] applyResult = doApply.execute(frame, vecMat, fun, optionalArgs);

        RVector result = null;
        boolean applyResultZeroLength = applyResult.length == 0;

        // TODO check funValueLen against length of result
        if (funValueVec instanceof RAbstractIntVector) {
            int[] data = applyResultZeroLength ? new int[0] : convertIntVector(applyResult, funValueVecLen);
            result = RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        } else if (funValueVec instanceof RAbstractDoubleVector) {
            double[] data = applyResultZeroLength ? new double[0] : convertDoubleVector(applyResult, funValueVecLen);
            result = RDataFactory.createDoubleVector(data, RDataFactory.COMPLETE_VECTOR);
        } else if (funValueVec instanceof RAbstractLogicalVector) {
            byte[] data = applyResultZeroLength ? new byte[0] : convertLogicalVector(applyResult, funValueVecLen);
            result = RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        } else if (funValueVec instanceof RAbstractStringVector) {
            String[] data = applyResultZeroLength ? new String[0] : convertStringVector(applyResult, funValueVecLen);
            result = RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
        } else if (funValueVec instanceof RAbstractComplexVector) {
            double[] data = applyResultZeroLength ? new double[1] : convertComplexVector(applyResult, funValueVecLen);
            result = RDataFactory.createComplexVector(data, RDataFactory.COMPLETE_VECTOR);
        } else {
            throw RInternalError.shouldNotReachHere();
        }

        if (dimsProfile.profile(funValueVecLen > 1)) {
            result.setDimensions(new int[]{funValueVecLen, applyResult.length});
        }

        // TODO: handle names in case of matrices
        if (useNamesProfile.profile(RRuntime.fromLogical(useNames))) {
            RStringVector names = vecMat.getNames(attrProfiles);
            RStringVector newNames = null;
            if (names != null) {
                newNames = names;
            } else if (vecMat instanceof RStringVector) {
                newNames = (RStringVector) vecMat.copy();
            }
            if (newNames != null) {
                result.setNames(newNames);
            }
        }
        return result;
    }

    private double[] convertDoubleVector(Object[] values, int len) {
        double[] newArray = new double[values.length * len];
        int ind = 0;
        for (int i = 0; i < values.length; i++) {
            RAbstractDoubleVector v = (RAbstractDoubleVector) RRuntime.asAbstractVector(castDouble(values[i], false));
            for (int j = 0; j < v.getLength(); j++) {
                newArray[ind++] = v.getDataAt(j);
            }
        }
        return newArray;
    }

    private int[] convertIntVector(Object[] values, int len) {
        int[] newArray = new int[values.length * len];
        int ind = 0;
        for (int i = 0; i < values.length; i++) {
            RAbstractIntVector v = (RAbstractIntVector) RRuntime.asAbstractVector(castInteger(values[i], false));
            for (int j = 0; j < v.getLength(); j++) {
                newArray[ind++] = v.getDataAt(j);
            }
        }
        return newArray;
    }

    private byte[] convertLogicalVector(Object[] values, int len) {
        byte[] newArray = new byte[values.length * len];
        int ind = 0;
        for (int i = 0; i < values.length; i++) {
            RAbstractLogicalVector v = (RAbstractLogicalVector) RRuntime.asAbstractVector(castLogical(values[i], false));
            for (int j = 0; j < v.getLength(); j++) {
                newArray[ind++] = v.getDataAt(j);
            }
        }
        return newArray;
    }

    private String[] convertStringVector(Object[] values, int len) {
        String[] newArray = new String[values.length * len];
        int ind = 0;
        for (int i = 0; i < values.length; i++) {
            RAbstractStringVector v = (RAbstractStringVector) RRuntime.asAbstractVector(castString(values[i], false));
            for (int j = 0; j < v.getLength(); j++) {
                newArray[ind++] = v.getDataAt(j);
            }
        }
        return newArray;
    }

    private double[] convertComplexVector(Object[] values, int len) {
        double[] newArray = new double[values.length * len * 2];
        int ind = 0;
        for (int i = 0; i < values.length; i++) {
            RAbstractComplexVector v = (RAbstractComplexVector) RRuntime.asAbstractVector(castComplex(values[i], false));
            for (int j = 0; j < v.getLength(); j++) {
                RComplex val = v.getDataAt(j);
                newArray[ind++] = val.getRealPart();
                newArray[ind++] = val.getImaginaryPart();
            }
        }
        return newArray;
    }
}
