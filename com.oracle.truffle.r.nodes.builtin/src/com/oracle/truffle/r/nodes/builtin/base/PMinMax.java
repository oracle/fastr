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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RError.NO_CALLER;
import static com.oracle.truffle.r.runtime.RError.SHOW_CALLER;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.PMinMaxNodeGen.MultiElemStringHandlerNodeGen;
import com.oracle.truffle.r.nodes.unary.CastDoubleNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleNodeGen;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.nodes.unary.CastToVectorNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNodeGen;
import com.oracle.truffle.r.nodes.unary.PrecedenceNode;
import com.oracle.truffle.r.nodes.unary.PrecedenceNodeGen;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticReduceNode.ReduceSemantics;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.ops.BinaryArithmetic;
import com.oracle.truffle.r.runtime.ops.BinaryArithmeticFactory;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public abstract class PMinMax extends RBuiltinNode {

    @Child private MultiElemStringHandler stringHandler;
    @Child private CastToVectorNode castVector;
    @Child private CastIntegerNode castInteger;
    @Child private CastDoubleNode castDouble;
    @Child private CastStringNode castString;
    @Child private PrecedenceNode precedenceNode = PrecedenceNodeGen.create();
    private final ReduceSemantics semantics;
    private final BinaryArithmeticFactory factory;
    @Child private BinaryArithmetic op;
    private final NACheck na = NACheck.create();
    private final ConditionProfile lengthProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile naRmProfile = ConditionProfile.createBinaryProfile();

    protected PMinMax(ReduceSemantics semantics, BinaryArithmeticFactory factory) {
        this.semantics = semantics;
        this.factory = factory;
        this.op = factory.createOperation();
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("na.rm").defaultError(SHOW_CALLER, Message.INVALID_VALUE, "na.rm").mustBe(numericValue()).asLogicalVector().findFirst().mustBe(logicalNA().not()).map(toBoolean());
    }

    private byte handleString(Object[] argValues, boolean naRm, int offset, int ind, int maxLength, byte warning, Object data) {
        if (stringHandler == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stringHandler = insert(MultiElemStringHandlerNodeGen.create(semantics, factory, na));
        }
        return stringHandler.executeByte(argValues, naRm, offset, ind, maxLength, warning, data);
    }

    private RAbstractVector castVector(Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeGen.create(false));
        }
        return ((RAbstractVector) castVector.execute(value)).materialize();
    }

    private CastNode getIntegerCastNode() {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInteger = insert(CastIntegerNodeGen.create(true, true, true));
        }
        return castInteger;
    }

    private CastNode getDoubleCastNode() {
        if (castDouble == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castDouble = insert(CastDoubleNodeGen.create(true, true, true));
        }
        return castDouble;
    }

    private CastNode getStringCastNode() {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeGen.create(true, true, true));
        }
        return castString;
    }

    private int convertToVectorAndEnableNACheck(RArgsValuesAndNames args, CastNode castNode) {
        int length = 0;
        Object[] argValues = args.getArguments();
        for (int i = 0; i < args.getLength(); i++) {
            RAbstractVector v = castVector(argValues[i]);
            na.enable(v);
            int vecLength = v.getLength();
            if (vecLength == 0) {
                // we can stop - the result will be empty vector anyway
                return vecLength;
            }
            length = Math.max(length, vecLength);
            argValues[i] = castNode.execute(v);
        }
        return length;
    }

    @Specialization(guards = {"isIntegerPrecedence(args)", "args.getLength() == 0"})
    protected Object pMinMaxNoneVecInt(@SuppressWarnings("unused") boolean naRm, @SuppressWarnings("unused") RArgsValuesAndNames args) {
        return RDataFactory.createEmptyIntVector();
    }

    @Specialization(guards = {"isIntegerPrecedence(args)", "args.getLength() == 1"})
    protected Object pMinMaxOneVecInt(@SuppressWarnings("unused") boolean naRm, RArgsValuesAndNames args) {
        return args.getArgument(0);
    }

    @Specialization(guards = {"isIntegerPrecedence(args)", "args.getLength() > 1"})
    protected RIntVector pMinMaxInt(boolean naRm, RArgsValuesAndNames args) {
        int maxLength = convertToVectorAndEnableNACheck(args, getIntegerCastNode());
        if (lengthProfile.profile(maxLength == 0)) {
            return RDataFactory.createEmptyIntVector();
        } else {
            boolean profiledNaRm = naRmProfile.profile(naRm);
            int[] data = new int[maxLength];
            Object[] argValues = args.getArguments();
            boolean warningAdded = false;
            for (int i = 0; i < maxLength; i++) {
                int result = semantics.getIntStart();
                for (int j = 0; j < argValues.length; j++) {
                    RAbstractIntVector vec = (RAbstractIntVector) argValues[j];
                    na.enable(vec);
                    if (vec.getLength() > 1 && vec.getLength() < maxLength && !warningAdded) {
                        RError.warning(RError.SHOW_CALLER2, RError.Message.ARG_RECYCYLED);
                        warningAdded = true;
                    }
                    int v = vec.getDataAt(i % vec.getLength());
                    if (na.check(v)) {
                        if (profiledNaRm) {
                            continue;
                        } else {
                            result = RRuntime.INT_NA;
                            break;
                        }
                    } else {
                        result = op.op(result, v);
                    }
                }
                data[i] = result;
            }
            return RDataFactory.createIntVector(data, na.neverSeenNA() || profiledNaRm);
        }
    }

    @Specialization(guards = {"isLogicalPrecedence(args)", "args.getLength() == 1"})
    protected Object pMinMaxOneVecLogical(@SuppressWarnings("unused") boolean naRm, RArgsValuesAndNames args) {
        return args.getArgument(0);
    }

    @Specialization(guards = {"isLogicalPrecedence(args)", "args.getLength() != 1"})
    protected RIntVector pMinMaxLogical(boolean naRm, RArgsValuesAndNames args) {
        return pMinMaxInt(naRm, args);
    }

    @Specialization(guards = {"isDoublePrecedence(args)", "args.getLength() == 0"})
    @SuppressWarnings("unused")
    protected Object pMinMaxNoneVecDouble(boolean naRm, RArgsValuesAndNames args) {
        return RDataFactory.createEmptyDoubleVector();
    }

    @Specialization(guards = {"isDoublePrecedence(args)", "args.getLength() == 1"})
    @SuppressWarnings("unused")
    protected Object pMinMaxOneVecDouble(boolean naRm, RArgsValuesAndNames args) {
        return args.getArgument(0);
    }

    @Specialization(guards = {"isDoublePrecedence(args)", "args.getLength() ==2"})
    protected RDoubleVector pMinMaxTwoDouble(boolean naRm, RArgsValuesAndNames args, //
                    @Cached("create()") NACheck naCheckX, //
                    @Cached("create()") NACheck naCheckY, //
                    @Cached("create()") CastDoubleNode castX, //
                    @Cached("create()") CastDoubleNode castY, //
                    @Cached("create()") CastToVectorNode castVectorX, //
                    @Cached("create()") CastToVectorNode castVectorY) {
        Object[] argValues = args.getArguments();
        RAbstractDoubleVector x = (RAbstractDoubleVector) castVectorX.execute(castX.execute(argValues[0]));
        RAbstractDoubleVector y = (RAbstractDoubleVector) castVectorY.execute(castY.execute(argValues[1]));
        int xLength = x.getLength();
        int yLength = y.getLength();
        int maxLength = Math.max(xLength, yLength);
        if (lengthProfile.profile(xLength == 0 || yLength == 0)) {
            return RDataFactory.createEmptyDoubleVector();
        } else {
            naCheckX.enable(x);
            naCheckY.enable(y);
            if ((xLength > 1 && xLength < maxLength) || (yLength > 1 && yLength < maxLength)) {
                RError.warning(RError.SHOW_CALLER2, RError.Message.ARG_RECYCYLED);
            }
            boolean profiledNaRm = naRmProfile.profile(naRm);
            double[] data = new double[maxLength];
            int xOffset = 0;
            int yOffset = 0;
            for (int i = 0; i < maxLength; i++, xOffset++, yOffset++) {
                if (xOffset == xLength) {
                    xOffset = 0;
                }
                if (yOffset == yLength) {
                    yOffset = 0;
                }
                double xValue = x.getDataAt(xOffset);
                double yValue = y.getDataAt(yOffset);
                double result;
                if (naCheckX.check(xValue)) {
                    result = profiledNaRm ? yValue : RRuntime.DOUBLE_NA;
                } else if (naCheckY.check(yValue)) {
                    result = profiledNaRm ? xValue : RRuntime.DOUBLE_NA;
                } else {
                    result = op.op(xValue, yValue);
                }
                data[i] = result;
            }
            return RDataFactory.createDoubleVector(data, (naCheckX.neverSeenNA() && naCheckY.neverSeenNA()) || profiledNaRm);
        }
    }

    @Specialization(guards = {"isDoublePrecedence(args)", "args.getLength() > 2"})
    protected RDoubleVector pMinMaxDouble(boolean naRm, RArgsValuesAndNames args) {
        int maxLength = convertToVectorAndEnableNACheck(args, getDoubleCastNode());
        if (lengthProfile.profile(maxLength == 0)) {
            return RDataFactory.createEmptyDoubleVector();
        } else {
            Object[] argValues = args.getArguments();
            boolean warningAdded = false;
            for (int j = 0; j < argValues.length; j++) {
                RAbstractDoubleVector vec = (RAbstractDoubleVector) argValues[j];
                na.enable(vec);
                if (vec.getLength() > 1 && vec.getLength() < maxLength && !warningAdded) {
                    RError.warning(RError.SHOW_CALLER2, RError.Message.ARG_RECYCYLED);
                    warningAdded = true;
                }
            }
            boolean profiledNaRm = naRmProfile.profile(naRm);
            double[] data = new double[maxLength];
            for (int i = 0; i < maxLength; i++) {
                double result = semantics.getDoubleStart();
                for (int j = 0; j < argValues.length; j++) {
                    RAbstractDoubleVector vec = (RAbstractDoubleVector) argValues[j];
                    double v = vec.getDataAt(i % vec.getLength());
                    if (na.check(v)) {
                        if (profiledNaRm) {
                            continue;
                        } else {
                            result = RRuntime.DOUBLE_NA;
                            break;
                        }
                    } else {
                        result = op.op(result, v);
                    }
                }
                data[i] = result;
            }
            return RDataFactory.createDoubleVector(data, na.neverSeenNA() || profiledNaRm);
        }
    }

    @Specialization(guards = {"isStringPrecedence(args)", "args.getLength() == 1"})
    @SuppressWarnings("unused")
    protected Object pMinMaxOneVecString(boolean naRm, RArgsValuesAndNames args) {
        return args.getArgument(0);
    }

    @Specialization(guards = {"isStringPrecedence(args)", "args.getLength() != 1"})
    protected RStringVector pMinMaxString(boolean naRm, RArgsValuesAndNames args) {
        int maxLength = convertToVectorAndEnableNACheck(args, getStringCastNode());
        if (lengthProfile.profile(maxLength == 0)) {
            return RDataFactory.createEmptyStringVector();
        } else {
            boolean profiledNaRm = naRmProfile.profile(naRm);
            String[] data = new String[maxLength];
            Object[] argValues = args.getArguments();
            byte warningAdded = RRuntime.LOGICAL_FALSE;
            for (int i = 0; i < maxLength; i++) {
                warningAdded = handleString(argValues, naRm, 0, i, maxLength, warningAdded, data);
            }
            return RDataFactory.createStringVector(data, na.neverSeenNA() || profiledNaRm);
        }
    }

    @SuppressWarnings("unused")
    @Fallback
    protected RRawVector pMinMaxRaw(Object naRm, Object args) {
        throw RError.error(NO_CALLER, RError.Message.INVALID_INPUT_TYPE);
    }

    @RBuiltin(name = "pmax", kind = INTERNAL, parameterNames = {"na.rm", "..."}, behavior = PURE)
    public abstract static class PMax extends PMinMax {

        public PMax() {
            super(new ReduceSemantics(RRuntime.INT_MIN_VALUE, Double.NEGATIVE_INFINITY, false, RError.Message.NO_NONMISSING_MAX, RError.Message.NO_NONMISSING_MAX_NA, false, true),
                            BinaryArithmetic.MAX);
        }
    }

    @RBuiltin(name = "pmin", kind = INTERNAL, parameterNames = {"na.rm", "..."}, behavior = PURE)
    public abstract static class PMin extends PMinMax {

        public PMin() {
            super(new ReduceSemantics(RRuntime.INT_MAX_VALUE, Double.POSITIVE_INFINITY, false, RError.Message.NO_NONMISSING_MIN, RError.Message.NO_NONMISSING_MIN_NA, false, true),
                            BinaryArithmetic.MIN);
        }
    }

    protected boolean isIntegerPrecedence(RArgsValuesAndNames args) {
        return precedence(args) == PrecedenceNode.INT_PRECEDENCE;
    }

    protected boolean isLogicalPrecedence(RArgsValuesAndNames args) {
        return precedence(args) == PrecedenceNode.LOGICAL_PRECEDENCE;
    }

    protected boolean isDoublePrecedence(RArgsValuesAndNames args) {
        return precedence(args) == PrecedenceNode.DOUBLE_PRECEDENCE;
    }

    protected boolean isStringPrecedence(RArgsValuesAndNames args) {
        return precedence(args) == PrecedenceNode.STRING_PRECEDENCE;
    }

    private int precedence(RArgsValuesAndNames args) {
        int precedence = -1;
        Object[] array = args.getArguments();
        for (int i = 0; i < array.length; i++) {
            precedence = Math.max(precedence, precedenceNode.executeInteger(array[i], false));
        }
        return precedence;
    }

    protected abstract static class MultiElemStringHandler extends RBaseNode {

        public abstract byte executeByte(Object[] argValues, boolean naRm, int offset, int ind, int maxLength, byte warning, Object data);

        @Child private MultiElemStringHandler recursiveStringHandler;
        private final ReduceSemantics semantics;
        private final BinaryArithmeticFactory factory;
        @Child private BinaryArithmetic op;
        private final NACheck na;
        private final ConditionProfile naRmProfile = ConditionProfile.createBinaryProfile();

        protected MultiElemStringHandler(ReduceSemantics semantics, BinaryArithmeticFactory factory, NACheck na) {
            this.semantics = semantics;
            this.factory = factory;
            this.op = factory.createOperation();
            this.na = na;
        }

        private byte handleString(Object[] argValues, boolean naRm, int offset, int ind, int maxLength, byte warning, Object data) {
            if (recursiveStringHandler == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                recursiveStringHandler = insert(MultiElemStringHandlerNodeGen.create(semantics, factory, na));
            }
            return recursiveStringHandler.executeByte(argValues, naRm, offset, ind, maxLength, warning, data);
        }

        @Specialization
        protected byte doStringVectorMultiElem(Object[] argValues, boolean naRm, int offset, int ind, int maxLength, byte warning, Object d) {
            String[] data = (String[]) d;
            byte warningAdded = warning;
            RAbstractStringVector vec = (RAbstractStringVector) argValues[offset];
            if (vec.getLength() > 1 && vec.getLength() < maxLength && warningAdded == RRuntime.LOGICAL_FALSE) {
                RError.warning(RError.SHOW_CALLER2, RError.Message.ARG_RECYCYLED);
                warningAdded = RRuntime.LOGICAL_TRUE;
            }
            String result = vec.getDataAt(ind % vec.getLength());
            na.enable(result);
            if (naRmProfile.profile(naRm)) {
                if (na.check(result)) {
                    // the following is meant to eliminate leading NA-s
                    if (offset == argValues.length - 1) {
                        // last element - all other are NAs
                        data[ind] = semantics.getStringStart();
                    } else {
                        return handleString(argValues, naRm, offset + 1, ind, maxLength, warningAdded, data);
                    }
                    return warningAdded;
                }
            } else {
                if (na.check(result)) {
                    data[ind] = result;
                    return warningAdded;
                }
            }
            // when we reach here, it means that we have already seen one non-NA element
            assert !RRuntime.isNA(result);
            for (int i = offset + 1; i < argValues.length; i++) {
                vec = (RAbstractStringVector) argValues[i];
                if (vec.getLength() > 1 && vec.getLength() < maxLength && warningAdded == RRuntime.LOGICAL_FALSE) {
                    RError.warning(this, RError.Message.ARG_RECYCYLED);
                    warningAdded = RRuntime.LOGICAL_TRUE;
                }

                String current = vec.getDataAt(ind % vec.getLength());
                na.enable(current);
                if (na.check(current)) {
                    if (naRmProfile.profile(naRm)) {
                        // skip NA-s
                        continue;
                    } else {
                        data[ind] = RRuntime.STRING_NA;
                        return warningAdded;
                    }
                } else {
                    result = op.op(result, current);
                }
            }
            data[ind] = result;
            return warningAdded;
        }
    }
}
