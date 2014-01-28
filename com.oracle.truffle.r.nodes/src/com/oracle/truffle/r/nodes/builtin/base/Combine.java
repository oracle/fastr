/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.nodes.Node.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.RBuiltin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(value = "c", lastParameterKind = LastParameterKind.VAR_ARGS_SPECIALIZE)
@NodeField(name = "argNames", type = String[].class)
@SuppressWarnings("unused")
public abstract class Combine extends RBuiltinNode {

    @Child private CastComplexNode castComplex;
    @Child private CastDoubleNode castDouble;
    @Child private CastIntegerNode castInteger;
    @Child private CastLogicalNode castLogical;
    @Child private CastStringNode castString;
    @Child private CastRawNode castRaw;
    @Child private CastListNode castList;
    @Child private CastToVectorNode castVector;

    @Child private FoldOperationNode foldOperation;

    @Child private PrecedenceNode precedenceNode;

    private static final Object[] PARAMETER_NAMES = new Object[]{"..."};

    public abstract Object executeCombine(VirtualFrame frame, Object value);

    public abstract String[] getArgNames();

    private RAbstractVector castVector(VirtualFrame frame, Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreter();
            castVector = adoptChild(CastToVectorNodeFactory.create(null));
        }
        return castVector.executeRAbstractVector(frame, value);
    }

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    protected Combine() {
        this.foldOperation = adoptChild(new CombineFoldOperationNode());
        this.precedenceNode = adoptChild(PrecedenceNodeFactory.create(null));
    }

    protected Combine(Combine previousNode) {
        this.foldOperation = previousNode.foldOperation;
        this.precedenceNode = previousNode.precedenceNode;
    }

    public void setFoldOperation(FoldOperationNode foldOperation) {
        this.foldOperation = foldOperation;
    }

    protected static boolean noArgs(Object[] operand) {
        return operand.length == 0;
    }

    @Specialization
    public RNull pass(RMissing vector) {
        return RNull.instance;
    }

    @Specialization
    public RNull pass(RNull vector) {
        return RNull.instance;
    }

    @Specialization
    public RIntVector pass(RIntVector vector) {
        return vector;
    }

    @Specialization
    public RDoubleVector pass(RDoubleVector vector) {
        return vector;
    }

    @Specialization
    public RComplexVector pass(RComplexVector vector) {
        return vector;
    }

    @Specialization
    public RStringVector pass(RStringVector vector) {
        return vector;
    }

    @Specialization
    public RRawVector pass(RRawVector vector) {
        return vector;
    }

    @Specialization
    public RLogicalVector pass(RLogicalVector vector) {
        return vector;
    }

    @Specialization
    public RIntSequence pass(RIntSequence vector) {
        return vector;
    }

    @Specialization
    public RDoubleSequence pass(RDoubleSequence vector) {
        return vector;
    }

    @Specialization
    public RList pass(RList list) {
        return list;
    }

    @Specialization(guards = "noArgNames")
    public int pass(int value) {
        return value;
    }

    @Specialization(guards = "hasArgNames")
    public RIntVector passArgs(int value) {
        return RDataFactory.createIntVector(new int[]{value}, true, RDataFactory.createStringVector(getArgNames(), true));
    }

    @Specialization(guards = "noArgNames")
    public double pass(double value) {
        return value;
    }

    @Specialization(guards = "hasArgNames")
    public RDoubleVector passArgs(double value) {
        return RDataFactory.createDoubleVector(new double[]{value}, true, RDataFactory.createStringVector(getArgNames(), true));
    }

    @Specialization(guards = "noArgNames")
    public byte pass(byte value) {
        return value;
    }

    @Specialization(guards = "hasArgNames")
    public RLogicalVector passArgs(byte value) {
        return RDataFactory.createLogicalVector(new byte[]{value}, true, RDataFactory.createStringVector(getArgNames(), true));
    }

    @Specialization(guards = "noArgNames")
    public String pass(String value) {
        return value;
    }

    @Specialization(guards = "hasArgNames")
    public RStringVector passArgs(String value) {
        return RDataFactory.createStringVector(new String[]{value}, true, RDataFactory.createStringVector(getArgNames(), true));
    }

    @Specialization(guards = "noArgNames")
    public RRaw pass(RRaw value) {
        return value;
    }

    @Specialization(guards = "hasArgNames")
    public RRawVector passArgs(RRaw value) {
        return RDataFactory.createRawVector(new byte[]{value.getValue()}, RDataFactory.createStringVector(getArgNames(), true));
    }

    @Specialization(guards = "noArgNames")
    public RComplex pass(RComplex value) {
        return value;
    }

    @Specialization(guards = "hasArgNames")
    public RComplexVector passArgs(RComplex value) {
        return RDataFactory.createComplexVector(new double[]{value.getRealPart(), value.getImaginaryPart()}, true, RDataFactory.createStringVector(getArgNames(), true));
    }

    protected boolean isIntegerPrecedence(VirtualFrame frame, Object[] array) {
        return precedence(frame, array) == PrecedenceNode.INT_PRECEDENCE;
    }

    protected boolean isLogicalPrecedence(VirtualFrame frame, Object[] array) {
        return precedence(frame, array) == PrecedenceNode.LOGICAL_PRECEDENCE;
    }

    protected boolean isDoublePrecedence(VirtualFrame frame, Object[] array) {
        return precedence(frame, array) == PrecedenceNode.DOUBLE_PRECEDENCE;
    }

    protected boolean isComplexPrecedence(VirtualFrame frame, Object[] array) {
        return precedence(frame, array) == PrecedenceNode.COMPLEX_PRECEDENCE;
    }

    protected boolean isStringPrecedence(VirtualFrame frame, Object[] array) {
        return precedence(frame, array) == PrecedenceNode.STRING_PRECEDENCE;
    }

    protected boolean isRawPrecedence(VirtualFrame frame, Object[] array) {
        return precedence(frame, array) == PrecedenceNode.RAW_PRECEDENCE;
    }

    protected boolean isNullPrecedence(VirtualFrame frame, Object[] array) {
        return precedence(frame, array) == PrecedenceNode.NO_PRECEDENCE;
    }

    protected boolean isListPrecedence(VirtualFrame frame, Object[] array) {
        return precedence(frame, array) == PrecedenceNode.LIST_PRECEDENCE;
    }

    private int precedence(VirtualFrame frame, Object[] array) {
        int precedence = -1;
        for (int i = 0; i < array.length; i++) {
            precedence = Math.max(precedence, precedenceNode.executeInteger(frame, array[i]));
        }
        return precedence;
    }

    private Object castComplex(VirtualFrame frame, Object operand) {
        if (castComplex == null) {
            CompilerDirectives.transferToInterpreter();
            castComplex = adoptChild(CastComplexNodeFactory.create(null));
        }
        return castComplex.executeCast(frame, operand);
    }

    private Object castDouble(VirtualFrame frame, Object operand) {
        if (castDouble == null) {
            CompilerDirectives.transferToInterpreter();
            castDouble = adoptChild(CastDoubleNodeFactory.create(null, true, false));
        }
        return castDouble.executeCast(frame, operand);
    }

    private Object castInteger(VirtualFrame frame, Object operand) {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreter();
            castInteger = adoptChild(CastIntegerNodeFactory.create(null, true, false, getContext()));
        }
        return castInteger.executeCast(frame, operand);
    }

    private Object castLogical(VirtualFrame frame, Object operand) {
        if (castLogical == null) {
            CompilerDirectives.transferToInterpreter();
            castLogical = adoptChild(CastLogicalNodeFactory.create(null, true, false, getContext()));
        }
        return castLogical.executeCast(frame, operand);
    }

    private Object castString(VirtualFrame frame, Object operand) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreter();
            castString = adoptChild(CastStringNodeFactory.create(null, false, true, false));
        }
        return castString.executeCast(frame, operand);
    }

    private Object castRaw(VirtualFrame frame, Object operand) {
        if (castRaw == null) {
            CompilerDirectives.transferToInterpreter();
            castRaw = adoptChild(CastRawNodeFactory.create(null));
        }
        return castRaw.executeCast(frame, operand);
    }

    private Object castList(VirtualFrame frame, Object operand) {
        if (castList == null) {
            CompilerDirectives.transferToInterpreter();
            castList = adoptChild(CastListNodeFactory.create(null));
        }
        return castList.executeCast(frame, operand);
    }

    public static RAbstractVector namesMerge(RAbstractVector vector, String name) {
        Object orgNamesObject = vector.getNames();
        if (((orgNamesObject == null || orgNamesObject == RNull.instance) && name == null) || vector.getLength() == 0) {
            return vector;
        }
        if (orgNamesObject == null || orgNamesObject == RNull.instance) {
            assert (name != null);
            assert (!name.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE));
            RVector v = vector.materialize();
            if (v.getLength() == 1) {
                // single value - just use the name
                v.setNames(RDataFactory.createStringVector(new String[]{name}, true));
            } else {
                // multiple values - prepend name to the index of a given value
                String[] names = new String[v.getLength()];
                for (int i = 0; i < names.length; i++) {
                    names[i] = name + (i + 1);
                }
                v.setNames(RDataFactory.createStringVector(names, true));
            }
            return v;
        } else {
            RStringVector orgNames = (RStringVector) orgNamesObject;
            if (vector.getLength() == 1) {
                // single value
                if (name == null) {
                    // use the same name
                    return vector;
                } else {
                    RVector v = vector.materialize();
                    // prepend name to the original name
                    assert (!name.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE));
                    String orgName = orgNames.getDataAt(0);
                    v.setNames(RDataFactory.createStringVector(new String[]{orgName.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE) ? name : name + "." + orgName}, true));
                    return v;
                }
            } else {
                // multiple values - prepend name to the index of a given value or to the original
                // name
                RVector v = vector.materialize();
                String[] names = new String[v.getLength()];
                for (int i = 0; i < names.length; i++) {
                    if (name == null) {
                        names[i] = orgNames.getDataAt(i);
                    } else {
                        assert (!name.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE));
                        String orgName = orgNames.getDataAt(i);
                        names[i] = orgName.equals(RRuntime.NAMES_ATTR_EMPTY_VALUE) ? name + (i + 1) : name + "." + orgName;
                    }
                }
                v.setNames(RDataFactory.createStringVector(names, true));
                return v;
            }
        }
    }

    @Specialization(order = 1, guards = "isNullPrecedence")
    @ExplodeLoop
    public RNull allNull(VirtualFrame frame, Object[] array) {
        return RNull.instance;
    }

    @Specialization(order = 2, guards = {"isLogicalPrecedence", "noArgNames"})
    @ExplodeLoop
    public Object allLogical(VirtualFrame frame, Object[] array) {
        Object current = castLogical(frame, array[0]);
        for (int i = 1; i < array.length; i++) {
            Object other = castLogical(frame, array[i]);
            current = foldOperation.executeLogical(frame, current, other);
        }
        return current;
    }

    @Specialization(order = 3, guards = {"isLogicalPrecedence", "hasArgNames"})
    @ExplodeLoop
    public Object allLogicalArgs(VirtualFrame frame, Object[] array) {
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castLogical(frame, namesMerge(currentVector, getArgNames()[0]));
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castLogical(frame, namesMerge(otherVector, getArgNames()[i]));
            current = foldOperation.executeLogical(frame, current, other);
        }
        return current;
    }

    @Specialization(order = 4, guards = {"isIntegerPrecedence", "noArgNames"})
    @ExplodeLoop
    public Object allInt(VirtualFrame frame, Object[] array) {
        Object current = castInteger(frame, array[0]);
        for (int i = 1; i < array.length; i++) {
            Object other = castInteger(frame, array[i]);
            current = foldOperation.executeInteger(frame, current, other);
        }
        return current;
    }

    @Specialization(order = 5, guards = {"isIntegerPrecedence", "hasArgNames"})
    @ExplodeLoop
    public Object allIntArgs(VirtualFrame frame, Object[] array) {
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castInteger(frame, namesMerge(currentVector, getArgNames()[0]));
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castInteger(frame, namesMerge(otherVector, getArgNames()[i]));
            current = foldOperation.executeInteger(frame, current, other);
        }
        return current;
    }

    @Specialization(order = 6, guards = {"isDoublePrecedence", "noArgNames"})
    @ExplodeLoop
    public Object allDouble(VirtualFrame frame, Object[] array) {
        Object current = castDouble(frame, array[0]);
        for (int i = 1; i < array.length; i++) {
            Object other = castDouble(frame, array[i]);
            current = foldOperation.executeDouble(frame, current, other);
        }
        return current;
    }

    @Specialization(order = 7, guards = {"isDoublePrecedence", "hasArgNames"})
    @ExplodeLoop
    public Object allDoubleArgs(VirtualFrame frame, Object[] array) {
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castDouble(frame, namesMerge(currentVector, getArgNames()[0]));
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castDouble(frame, namesMerge(otherVector, getArgNames()[i]));
            current = foldOperation.executeDouble(frame, current, other);
        }
        return current;
    }

    @Specialization(order = 8, guards = {"isComplexPrecedence", "noArgNames"})
    @ExplodeLoop
    public Object allComplex(VirtualFrame frame, Object[] array) {
        Object current = castComplex(frame, array[0]);
        for (int i = 1; i < array.length; i++) {
            Object other = castComplex(frame, array[i]);
            current = foldOperation.executeComplex(frame, current, other);
        }
        return current;
    }

    @Specialization(order = 9, guards = {"isComplexPrecedence", "hasArgNames"})
    @ExplodeLoop
    public Object allComplexArgs(VirtualFrame frame, Object[] array) {
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castComplex(frame, namesMerge(currentVector, getArgNames()[0]));
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castComplex(frame, namesMerge(otherVector, getArgNames()[i]));
            current = foldOperation.executeComplex(frame, current, other);
        }
        return current;
    }

    @Specialization(order = 10, guards = {"isStringPrecedence", "noArgNames"})
    @ExplodeLoop
    public Object allString(VirtualFrame frame, Object[] array) {
        Object current = castString(frame, array[0]);
        for (int i = 1; i < array.length; i++) {
            Object other = castString(frame, array[i]);
            current = foldOperation.executeString(frame, current, other);
        }
        return current;
    }

    @Specialization(order = 11, guards = {"isStringPrecedence", "hasArgNames"})
    @ExplodeLoop
    public Object allStringArgs(VirtualFrame frame, Object[] array) {
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castString(frame, namesMerge(currentVector, getArgNames()[0]));
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castString(frame, namesMerge(otherVector, getArgNames()[i]));
            current = foldOperation.executeString(frame, current, other);
        }
        return current;
    }

    @Specialization(order = 12, guards = {"isRawPrecedence", "noArgNames"})
    @ExplodeLoop
    public Object allRaw(VirtualFrame frame, Object[] array) {
        Object current = castRaw(frame, array[0]);
        for (int i = 1; i < array.length; i++) {
            Object other = castRaw(frame, array[i]);
            current = foldOperation.executeRaw(frame, current, other);
        }
        return current;
    }

    @Specialization(order = 13, guards = {"isRawPrecedence", "hasArgNames"})
    @ExplodeLoop
    public Object allRawArgs(VirtualFrame frame, Object[] array) {
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castRaw(frame, namesMerge(currentVector, getArgNames()[0]));
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castRaw(frame, namesMerge(otherVector, getArgNames()[i]));
            current = foldOperation.executeRaw(frame, current, other);
        }
        return current;
    }

    @Specialization(order = 14, guards = {"isListPrecedence", "noArgNames"})
    @ExplodeLoop
    public Object list(VirtualFrame frame, Object[] array) {
        RList current = RDataFactory.createList();
        for (int i = 0; i < array.length; ++i) {
            Object other = castList(frame, array[i]);
            current = (RList) foldOperation.executeList(frame, current, other);
        }
        return current;
    }

    @Specialization(order = 15, guards = {"isListPrecedence", "hasArgNames"})
    public Object listArgs(VirtualFrame frame, Object[] array) {
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castList(frame, namesMerge(currentVector, getArgNames()[0]));
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castList(frame, namesMerge(otherVector, getArgNames()[i]));
            current = foldOperation.executeList(frame, current, other);
        }
        return current;
    }

    public boolean hasArgNames() {
        return getArgNames() != null;
    }

    public boolean noArgNames() {
        return !hasArgNames();
    }
}
