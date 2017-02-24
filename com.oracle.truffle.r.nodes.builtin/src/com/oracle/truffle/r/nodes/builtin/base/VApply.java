/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.emptyList;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notLogicalNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetNamesAttributeNode;
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
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
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
import com.oracle.truffle.r.runtime.ops.na.NACheck;

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
@RBuiltin(name = "vapply", kind = INTERNAL, parameterNames = {"X", "FUN", "FUN.VALUE", "USE.NAMES"}, splitCaller = true, behavior = COMPLEX)
public abstract class VApply extends RBuiltinNode {

    private final ConditionProfile useNamesProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile dimsProfile = ConditionProfile.createBinaryProfile();
    private final NACheck naCheck = NACheck.create();

    @Child private LapplyInternalNode doApply = LapplyInternalNodeGen.create();

    @Child private CastComplexNode castComplex;
    @Child private CastDoubleNode castDouble;
    @Child private CastIntegerNode castInteger;
    @Child private CastLogicalNode castLogical;
    @Child private CastStringNode castString;
    @Child private SetDimAttributeNode setDimNode;
    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();
    @Child private SetNamesAttributeNode setNamesNode = SetNamesAttributeNode.create();

    static {
        // @formatter:off
        Casts casts = new Casts(VApply.class);
        casts.arg("X").mapNull(emptyList());
        casts.arg("FUN").mustBe(instanceOf(RFunction.class), RError.SHOW_CALLER, RError.Message.APPLY_NON_FUNCTION);
        casts.arg("FUN.VALUE").defaultError(RError.SHOW_CALLER, RError.Message.MUST_BE_VECTOR, "FUN.VALUE").mustBe(abstractVectorValue()).asVector(true);
        casts.arg("USE.NAMES").defaultError(RError.SHOW_CALLER, RError.Message.INVALID_VALUE, "USE.NAMES").
                mustBe(numericValue()).asLogicalVector().findFirst().mustBe(notLogicalNA()).map(toBoolean());
        // @formatter:on
    }

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
            castString = insert(CastStringNodeGen.create(true, preserveAllAttr, preserveAllAttr));
        }
        return castString.execute(operand);
    }

    @Specialization
    protected Object vapply(VirtualFrame frame, RAbstractVector vec, RFunction fun, RAbstractVector funValue, boolean useNames) {
        return delegateToLapply(frame, vec, fun, funValue, useNames);
    }

    @Specialization(guards = "isNotAbstractVector(obj)")
    protected Object vapplyNonVector(VirtualFrame frame, Object obj, RFunction fun, RAbstractVector funValue, boolean useNames) {
        // wrap the single value into a list and use normal vapply algorithm
        return vapply(frame, RDataFactory.createList(new Object[]{obj}), fun, funValue, useNames);
    }

    static boolean isNotAbstractVector(Object obj) {
        return !(obj instanceof RAbstractVector);
    }

    private RVector<?> delegateToLapply(VirtualFrame frame, RAbstractVector vec, RFunction fun, RAbstractVector funValueVec, boolean useNames) {
        /*
         * The implementation is complicated by the existence of scalar length 1 vectors (e.g.
         * Integer) and concrete length 1 vectors (e.g. RIntVector), as either form can occur in
         * both funValueArg and the result of the doApply. At a slight performance cost this code
         * works exclusively in terms of concrete vectors.
         */
        int funValueVecLen = funValueVec.getLength();

        RVector<?> vecMat = vec.materialize();
        Object[] applyResult = doApply.execute(frame, vecMat, fun);

        RVector<?> result;
        boolean applyResultZeroLength = applyResult.length == 0;

        naCheck.enable(true);
        // TODO check funValueLen against length of result
        if (funValueVec instanceof RAbstractIntVector) {
            int[] data = applyResultZeroLength ? new int[0] : convertIntVector(applyResult, funValueVecLen);
            result = RDataFactory.createIntVector(data, naCheck.neverSeenNA());
        } else if (funValueVec instanceof RAbstractDoubleVector) {
            double[] data = applyResultZeroLength ? new double[0] : convertDoubleVector(applyResult, funValueVecLen);
            result = RDataFactory.createDoubleVector(data, naCheck.neverSeenNA());
        } else if (funValueVec instanceof RAbstractLogicalVector) {
            byte[] data = applyResultZeroLength ? new byte[0] : convertLogicalVector(applyResult, funValueVecLen);
            result = RDataFactory.createLogicalVector(data, naCheck.neverSeenNA());
        } else if (funValueVec instanceof RAbstractStringVector) {
            String[] data = applyResultZeroLength ? new String[0] : convertStringVector(applyResult, funValueVecLen);
            result = RDataFactory.createStringVector(data, naCheck.neverSeenNA());
        } else if (funValueVec instanceof RAbstractComplexVector) {
            double[] data = applyResultZeroLength ? new double[1] : convertComplexVector(applyResult, funValueVecLen);
            result = RDataFactory.createComplexVector(data, naCheck.neverSeenNA());
        } else {
            throw RInternalError.shouldNotReachHere();
        }

        if (dimsProfile.profile(funValueVecLen > 1)) {
            if (setDimNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setDimNode = insert(SetDimAttributeNode.create());
            }
            setDimNode.setDimensions(result, new int[]{funValueVecLen, applyResult.length});
        }

        // TODO: handle names in case of matrices
        if (useNamesProfile.profile(useNames)) {
            RStringVector names = getNamesNode.getNames(vecMat);
            RStringVector newNames = null;
            if (names != null) {
                newNames = names;
            } else if (vecMat instanceof RStringVector) {
                newNames = (RStringVector) vecMat.copy();
            }
            if (newNames != null) {
                setNamesNode.setNames(result, newNames);
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
                double val = v.getDataAt(j);
                naCheck.check(val);
                newArray[ind++] = val;
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
                int val = v.getDataAt(j);
                naCheck.check(val);
                newArray[ind++] = val;
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
                byte val = v.getDataAt(j);
                naCheck.check(val);
                newArray[ind++] = val;
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
                String val = v.getDataAt(j);
                naCheck.check(val);
                newArray[ind++] = val;
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
                naCheck.check(val);
                newArray[ind++] = val.getRealPart();
                newArray[ind++] = val.getImaginaryPart();
            }
        }
        return newArray;
    }
}
