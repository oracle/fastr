/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.nodes.unary.UnaryArithmeticReduceNode.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.*;
import com.oracle.truffle.r.runtime.ops.na.*;
import com.oracle.truffle.r.nodes.builtin.base.PMinMaxFactory.MultiElemStringHandlerNodeGen;

public abstract class PMinMax extends RBuiltinNode {

    @Child private MultiElemStringHandler stringHandler;
    @Child private CastToVectorNode castVector;
    @Child private CastIntegerNode castInteger;
    @Child private CastDoubleNode castDouble;
    @Child private CastStringNode castString;
    @Child private PrecedenceNode precedenceNode = PrecedenceNodeGen.create(null, null);
    private final ReduceSemantics semantics;
    private final BinaryArithmeticFactory factory;
    @Child private BinaryArithmetic op;
    private final NACheck na = NACheck.create();
    private final ConditionProfile lengthProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile naRmProfile = ConditionProfile.createBinaryProfile();

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    public PMinMax(ReduceSemantics semantics, BinaryArithmeticFactory factory) {
        this.semantics = semantics;
        this.factory = factory;
        this.op = factory.create();
    }

    public PMinMax(PMinMax other) {
        this(other.semantics, other.factory);
    }

    private byte handleString(VirtualFrame frame, Object[] argValues, byte naRm, int offset, int ind, int maxLength, byte warning, Object data) {
        if (stringHandler == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stringHandler = insert(MultiElemStringHandlerNodeGen.create(semantics, factory, na, null, null, null, null, null, null, null));
        }
        return stringHandler.executeByte(frame, argValues, naRm, offset, ind, maxLength, warning, data);
    }

    private RAbstractVector castVector(VirtualFrame frame, Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeGen.create(null, true, true, true, false));
        }
        return ((RAbstractVector) castVector.executeObject(frame, value)).materialize();
    }

    private CastNode getIntegerCastNode() {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInteger = insert(CastIntegerNodeGen.create(null, true, true, true));
        }
        return castInteger;
    }

    private CastNode getDoubleCastNode() {
        if (castDouble == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castDouble = insert(CastDoubleNodeGen.create(null, true, true, true));
        }
        return castDouble;
    }

    private CastNode getStringCastNode() {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeGen.create(null, true, true, true, false));
        }
        return castString;
    }

    private int convertToVectorAndEnableNACheck(VirtualFrame frame, RArgsValuesAndNames args, CastNode castNode) {
        int length = 0;
        Object[] argValues = args.getValues();
        for (int i = 0; i < args.length(); i++) {
            RAbstractVector v = castVector(frame, argValues[i]);
            na.enable(v);
            int vecLength = v.getLength();
            if (vecLength == 0) {
                // we can stop - the result will be empty vector anyway
                return vecLength;
            }
            length = Math.max(length, vecLength);
            argValues[i] = castNode.executeCast(frame, v);
        }
        return length;
    }

    @Specialization(guards = {"isIntegerPrecedence(frame, args)", "oneVector(args)"})
    protected Object pMinMaxOneVecInt(@SuppressWarnings("unused") VirtualFrame frame, @SuppressWarnings("unused") byte naRm, RArgsValuesAndNames args) {
        return args.getValues()[0];
    }

    @Specialization(guards = {"isIntegerPrecedence(frame, args)", "!oneVector(args)"})
    protected RIntVector pMinMaxInt(VirtualFrame frame, byte naRm, RArgsValuesAndNames args) {
        int maxLength = convertToVectorAndEnableNACheck(frame, args, getIntegerCastNode());
        if (lengthProfile.profile(maxLength == 0)) {
            return RDataFactory.createEmptyIntVector();
        } else {
            boolean profiledNaRm = naRmProfile.profile(naRm == RRuntime.LOGICAL_TRUE);
            int[] data = new int[maxLength];
            Object[] argValues = args.getValues();
            boolean warningAdded = false;
            for (int i = 0; i < maxLength; i++) {
                int result = semantics.getIntStart();
                for (int j = 0; j < argValues.length; j++) {
                    RAbstractIntVector vec = (RAbstractIntVector) argValues[j];
                    na.enable(vec);
                    if (vec.getLength() > 1 && vec.getLength() < maxLength && !warningAdded) {
                        RError.warning(RError.Message.ARG_RECYCYLED);
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

    @Specialization(guards = {"isLogicalPrecedence(frame, args)", "oneVector(args)"})
    protected Object pMinMaxOneVecLogical(@SuppressWarnings("unused") VirtualFrame frame, @SuppressWarnings("unused") byte naRm, RArgsValuesAndNames args) {
        return args.getValues()[0];
    }

    @Specialization(guards = {"isLogicalPrecedence(frame, args)", "!oneVector(args)"})
    protected RIntVector pMinMaxLogical(VirtualFrame frame, byte naRm, RArgsValuesAndNames args) {
        return pMinMaxInt(frame, naRm, args);
    }

    @Specialization(guards = {"isDoublePrecedence(frame, args)", "oneVector(args)"})
    @SuppressWarnings("unused")
    protected Object pMinMaxOneVecDouble(VirtualFrame frame, byte naRm, RArgsValuesAndNames args) {
        return args.getValues()[0];
    }

    @Specialization(guards = {"isDoublePrecedence(frame, args)", "!oneVector(args)"})
    protected RDoubleVector pMinMaxDouble(VirtualFrame frame, byte naRm, RArgsValuesAndNames args) {
        int maxLength = convertToVectorAndEnableNACheck(frame, args, getDoubleCastNode());
        if (lengthProfile.profile(maxLength == 0)) {
            return RDataFactory.createEmptyDoubleVector();
        } else {
            boolean profiledNaRm = naRmProfile.profile(naRm == RRuntime.LOGICAL_TRUE);
            double[] data = new double[maxLength];
            Object[] argValues = args.getValues();
            boolean warningAdded = false;
            for (int i = 0; i < maxLength; i++) {
                double result = semantics.getDoubleStart();
                for (int j = 0; j < argValues.length; j++) {
                    RAbstractDoubleVector vec = (RAbstractDoubleVector) argValues[j];
                    na.enable(vec);
                    if (vec.getLength() > 1 && vec.getLength() < maxLength && !warningAdded) {
                        RError.warning(RError.Message.ARG_RECYCYLED);
                        warningAdded = true;
                    }
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

    @Specialization(guards = {"isStringPrecedence(frame, args)", "oneVector(args)"})
    @SuppressWarnings("unused")
    protected Object pMinMaxOneVecString(VirtualFrame frame, byte naRm, RArgsValuesAndNames args) {
        return args.getValues()[0];
    }

    @Specialization(guards = {"isStringPrecedence(frame, args)", "!oneVector(args)"})
    protected RStringVector pMinMaxString(VirtualFrame frame, byte naRm, RArgsValuesAndNames args) {
        int maxLength = convertToVectorAndEnableNACheck(frame, args, getStringCastNode());
        if (lengthProfile.profile(maxLength == 0)) {
            return RDataFactory.createEmptyStringVector();
        } else {
            boolean profiledNaRm = naRmProfile.profile(naRm == RRuntime.LOGICAL_TRUE);
            String[] data = new String[maxLength];
            Object[] argValues = args.getValues();
            byte warningAdded = RRuntime.LOGICAL_FALSE;
            for (int i = 0; i < maxLength; i++) {
                warningAdded = handleString(frame, argValues, naRm, 0, i, maxLength, warningAdded, data);
            }
            return RDataFactory.createStringVector(data, na.neverSeenNA() || profiledNaRm);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isComplexPrecedence(frame, args)")
    protected RComplexVector pMinMaxComplex(VirtualFrame frame, byte naRm, RArgsValuesAndNames args) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_INPUT_TYPE);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isRawPrecedence(frame, args)")
    protected RRawVector pMinMaxRaw(VirtualFrame frame, byte naRm, RArgsValuesAndNames args) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_INPUT_TYPE);
    }

    @RBuiltin(name = "pmax", kind = INTERNAL, parameterNames = {"na.rm", "..."})
    public abstract static class PMax extends PMinMax {

        public PMax() {
            super(new ReduceSemantics(RRuntime.INT_MIN_VALUE, Double.NEGATIVE_INFINITY, false, RError.Message.NO_NONMISSING_MAX, false, true), BinaryArithmetic.MAX);
        }

    }

    @RBuiltin(name = "pmin", kind = INTERNAL, parameterNames = {"na.rm", "..."})
    public abstract static class PMin extends PMinMax {

        public PMin() {
            super(new ReduceSemantics(RRuntime.INT_MAX_VALUE, Double.POSITIVE_INFINITY, false, RError.Message.NO_NONMISSING_MIN, false, true), BinaryArithmetic.MIN);
        }

    }

    protected boolean isIntegerPrecedence(VirtualFrame frame, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.INT_PRECEDENCE;
    }

    protected boolean isLogicalPrecedence(VirtualFrame frame, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.LOGICAL_PRECEDENCE;
    }

    protected boolean isDoublePrecedence(VirtualFrame frame, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.DOUBLE_PRECEDENCE;
    }

    protected boolean isStringPrecedence(VirtualFrame frame, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.STRING_PRECEDENCE;
    }

    protected boolean isComplexPrecedence(VirtualFrame frame, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.COMPLEX_PRECEDENCE;
    }

    protected boolean isRawPrecedence(VirtualFrame frame, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.RAW_PRECEDENCE;
    }

    protected boolean oneVector(RArgsValuesAndNames args) {
        return args.length() == 1;
    }

    private int precedence(VirtualFrame frame, RArgsValuesAndNames args) {
        int precedence = -1;
        Object[] array = args.getValues();
        for (int i = 0; i < array.length; i++) {
            precedence = Math.max(precedence, precedenceNode.executeInteger(frame, array[i], RRuntime.LOGICAL_FALSE));
        }
        return precedence;
    }

    @NodeChildren({@NodeChild("argValues"), @NodeChild("naRm"), @NodeChild("offset"), @NodeChild("ind"), @NodeChild("maxLength"), @NodeChild("warning"), @NodeChild("data")})
    protected abstract static class MultiElemStringHandler extends RNode {

        public abstract byte executeByte(VirtualFrame frame, Object[] argValues, byte naRm, int offset, int ind, int maxLength, byte warning, Object data);

        @Child private MultiElemStringHandler recursiveStringHandler;
        private final ReduceSemantics semantics;
        private final BinaryArithmeticFactory factory;
        @Child private BinaryArithmetic op;
        private final NACheck na;
        private final ConditionProfile naRmProfile = ConditionProfile.createBinaryProfile();

        public MultiElemStringHandler(ReduceSemantics semantics, BinaryArithmeticFactory factory, NACheck na) {
            this.semantics = semantics;
            this.factory = factory;
            this.op = factory.create();
            this.na = na;
        }

        public MultiElemStringHandler(MultiElemStringHandler other) {
            this(other.semantics, other.factory, other.na);
        }

        private byte handleString(VirtualFrame frame, Object[] argValues, byte naRm, int offset, int ind, int maxLength, byte warning, Object data) {
            if (recursiveStringHandler == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                recursiveStringHandler = insert(MultiElemStringHandlerNodeGen.create(semantics, factory, na, null, null, null, null, null, null, null));
            }
            return recursiveStringHandler.executeByte(frame, argValues, naRm, offset, ind, maxLength, warning, data);
        }

        @Specialization
        protected byte doStringVectorMultiElem(VirtualFrame frame, Object[] argValues, byte naRm, int offset, int ind, int maxLength, byte warning, Object d) {
            String[] data = (String[]) d;
            byte warningAdded = warning;
            RAbstractStringVector vec = (RAbstractStringVector) argValues[offset];
            if (vec.getLength() > 1 && vec.getLength() < maxLength && warningAdded == RRuntime.LOGICAL_FALSE) {
                RError.warning(RError.Message.ARG_RECYCYLED);
                warningAdded = RRuntime.LOGICAL_TRUE;
            }
            String result = vec.getDataAt(ind % vec.getLength());
            na.enable(result);
            if (naRmProfile.profile(naRm == RRuntime.LOGICAL_TRUE)) {
                if (na.check(result)) {
                    // the following is meant to eliminate leading NA-s
                    if (offset == argValues.length - 1) {
                        // last element - all other are NAs
                        data[ind] = semantics.getStringStart();
                    } else {
                        return handleString(frame, argValues, naRm, offset + 1, ind, maxLength, warningAdded, data);
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
            for (int i = offset + 1; i < argValues.length; ++i) {
                vec = (RAbstractStringVector) argValues[i];
                if (vec.getLength() > 1 && vec.getLength() < maxLength && warningAdded == RRuntime.LOGICAL_FALSE) {
                    RError.warning(RError.Message.ARG_RECYCYLED);
                    warningAdded = RRuntime.LOGICAL_TRUE;
                }

                String current = vec.getDataAt(ind % vec.getLength());
                na.enable(current);
                if (na.check(current)) {
                    if (naRmProfile.profile(naRm == RRuntime.LOGICAL_TRUE)) {
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
