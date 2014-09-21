/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

public abstract class PMinMax extends RBuiltinNode {

    @Child private CastToVectorNode castVector;
    @Child private CastIntegerNode castInteger;
    @Child private CastDoubleNode castDouble;
    @Child private PrecedenceNode precedenceNode = PrecedenceNodeFactory.create(null, null);
    private final ReduceSemantics semantics;
    private final NACheck na = NACheck.create();
    final ConditionProfile lengthProfile = ConditionProfile.createBinaryProfile();

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    private final BinaryArithmetic op;

    public PMinMax(BinaryArithmetic op, ReduceSemantics semantics) {
        this.op = op;
        this.semantics = semantics;
    }

    public PMinMax(PMinMax other) {
        this.op = other.op;
        this.semantics = other.semantics;
    }

    private RAbstractVector castVector(VirtualFrame frame, Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeFactory.create(null, true, true, true, false));
        }
        return ((RAbstractVector) castVector.executeObject(frame, value)).materialize();
    }

    private CastNode getIntegerCastNode() {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInteger = insert(CastIntegerNodeFactory.create(null, true, true, true));
        }
        return castInteger;
    }

    private CastNode getDoubleCastNode() {
        if (castDouble == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castDouble = insert(CastDoubleNodeFactory.create(null, true, true, true));
        }
        return castDouble;
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

    @Specialization(guards = "isIntegerPrecedence")
    protected RIntVector pMinMaxInt(VirtualFrame frame, byte naRm, RArgsValuesAndNames args) {
        int maxLength = convertToVectorAndEnableNACheck(frame, args, getIntegerCastNode());
        if (lengthProfile.profile(maxLength == 0)) {
            return RDataFactory.createEmptyIntVector();
        } else {
            int[] data = new int[maxLength];
            Object[] argValues = args.getValues();
            boolean warningAdded = false;
            for (int i = 0; i < maxLength; i++) {
                int result = semantics.getIntStart();
                for (int j = 0; j < args.length(); j++) {
                    RAbstractIntVector vec = (RAbstractIntVector) argValues[j];
                    if (vec.getLength() < maxLength && !warningAdded) {
                        RError.warning(RError.Message.ARG_RECYCYLED);
                        warningAdded = true;
                    }
                    int v = vec.getDataAt(i % vec.getLength());
                    if (na.check(v)) {
                        if (naRm == RRuntime.LOGICAL_TRUE) {
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
            return RDataFactory.createIntVector(data, na.neverSeenNA() || naRm == RRuntime.LOGICAL_TRUE);
        }
    }

    @Specialization(guards = "isLogicalPrecedence")
    protected RIntVector pMinMaxLogical(VirtualFrame frame, byte naRm, RArgsValuesAndNames args) {
        return pMinMaxInt(frame, naRm, args);
    }

    @Specialization(guards = "isDoublePrecedence")
    protected RDoubleVector pMinMaxDouble(VirtualFrame frame, byte naRm, RArgsValuesAndNames args) {
        int maxLength = convertToVectorAndEnableNACheck(frame, args, getDoubleCastNode());
        if (lengthProfile.profile(maxLength == 0)) {
            return RDataFactory.createEmptyDoubleVector();
        } else {
            double[] data = new double[maxLength];
            Object[] argValues = args.getValues();
            boolean warningAdded = false;
            for (int i = 0; i < maxLength; i++) {
                double result = semantics.getDoubleStart();
                for (int j = 0; j < args.length(); j++) {
                    RAbstractDoubleVector vec = (RAbstractDoubleVector) argValues[j];
                    if (vec.getLength() < maxLength && !warningAdded) {
                        RError.warning(RError.Message.ARG_RECYCYLED);
                        warningAdded = true;
                    }
                    double v = vec.getDataAt(i % vec.getLength());
                    if (na.check(v)) {
                        if (naRm == RRuntime.LOGICAL_TRUE) {
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
            return RDataFactory.createDoubleVector(data, na.neverSeenNA() || naRm == RRuntime.LOGICAL_TRUE);
        }
    }

    // TODO implement support for strings

    @SuppressWarnings("unused")
    @Specialization(guards = "isComplexPrecedence")
    protected RComplexVector pMinMaxComplex(byte naRm, RArgsValuesAndNames args) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_INPUT_TYPE);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isRawPrecedence")
    protected RRawVector pMinMaxRaw(byte naRm, RArgsValuesAndNames args) {
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_INPUT_TYPE);
    }

    @RBuiltin(name = "pmax", kind = INTERNAL, parameterNames = {"na.rm", "..."})
    public abstract static class PMax extends PMinMax {

        public PMax() {
            super(BinaryArithmetic.MAX.create(), new ReduceSemantics(RRuntime.INT_MIN_VALUE, Double.NEGATIVE_INFINITY, false, RError.Message.NO_NONMISSING_MAX, false, true));
        }

    }

    @RBuiltin(name = "pmin", kind = INTERNAL, parameterNames = {"na.rm", "..."})
    public abstract static class PMin extends PMinMax {

        public PMin() {
            super(BinaryArithmetic.MIN.create(), new ReduceSemantics(RRuntime.INT_MAX_VALUE, Double.POSITIVE_INFINITY, false, RError.Message.NO_NONMISSING_MIN, false, true));
        }

    }

    protected boolean isIntegerPrecedence(VirtualFrame frame, @SuppressWarnings("unused") byte naRm, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.INT_PRECEDENCE;
    }

    protected boolean isLogicalPrecedence(VirtualFrame frame, @SuppressWarnings("unused") byte naRm, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.LOGICAL_PRECEDENCE;
    }

    protected boolean isDoublePrecedence(VirtualFrame frame, @SuppressWarnings("unused") byte naRm, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.DOUBLE_PRECEDENCE;
    }

    protected boolean isStringPrecedence(VirtualFrame frame, @SuppressWarnings("unused") byte naRm, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.STRING_PRECEDENCE;
    }

    protected boolean isComplexPrecedence(VirtualFrame frame, @SuppressWarnings("unused") byte naRm, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.COMPLEX_PRECEDENCE;
    }

    protected boolean isRawPrecedence(VirtualFrame frame, @SuppressWarnings("unused") byte naRm, RArgsValuesAndNames args) {
        return precedence(frame, args) == PrecedenceNode.RAW_PRECEDENCE;
    }

    private int precedence(VirtualFrame frame, RArgsValuesAndNames args) {
        int precedence = -1;
        Object[] array = args.getValues();
        for (int i = 0; i < array.length; i++) {
            precedence = Math.max(precedence, precedenceNode.executeInteger(frame, array[i], RRuntime.LOGICAL_FALSE));
        }
        return precedence;
    }

}
