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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "c", kind = PRIMITIVE, parameterNames = {"..."})
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

    public abstract Object executeCombine(VirtualFrame frame, Object value);

    private RAbstractVector castVector(VirtualFrame frame, Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeFactory.create(null, false, false, false, false));
        }
        RVector resultVector = ((RAbstractVector) castVector.executeObject(frame, value)).materialize();
        // need to copy if vector is shared in case the same variable is used in combine, e.g. :
        // x <- 1:2 ; names(x) <- c("A",NA) ; c(x,test=x)
        if (resultVector.isShared()) {
            resultVector = resultVector.copy();
        }
        return resultVector;
    }

    protected Combine() {
        this.foldOperation = new CombineFoldOperationNode();
        this.precedenceNode = PrecedenceNodeFactory.create(null, null);
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
    protected RNull pass(RMissing vector) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    protected RNull pass(RNull vector) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    protected RIntVector pass(RIntVector vector) {
        controlVisibility();
        RIntVector result = (RIntVector) vector.copyDropAttributes();
        result.copyNamesFrom(vector);
        return result;
    }

    @Specialization
    protected RDoubleVector pass(RDoubleVector vector) {
        controlVisibility();
        RDoubleVector result = (RDoubleVector) vector.copyDropAttributes();
        result.copyNamesFrom(vector);
        return result;
    }

    @Specialization
    protected RComplexVector pass(RComplexVector vector) {
        controlVisibility();
        RComplexVector result = (RComplexVector) vector.copyDropAttributes();
        result.copyNamesFrom(vector);
        return result;
    }

    @Specialization
    protected RStringVector pass(RStringVector vector) {
        controlVisibility();
        RStringVector result = (RStringVector) vector.copyDropAttributes();
        result.copyNamesFrom(vector);
        return result;
    }

    @Specialization
    protected RRawVector pass(RRawVector vector) {
        controlVisibility();
        RRawVector result = (RRawVector) vector.copyDropAttributes();
        result.copyNamesFrom(vector);
        return result;
    }

    @Specialization
    protected RLogicalVector pass(RLogicalVector vector) {
        controlVisibility();
        RLogicalVector result = (RLogicalVector) vector.copyDropAttributes();
        result.copyNamesFrom(vector);
        return result;
    }

    @Specialization
    protected RIntVector pass(RIntSequence vector) {
        controlVisibility();
        RIntVector result = (RIntVector) vector.copyDropAttributes();
        result.copyNamesFrom(vector);
        return result;
    }

    @Specialization
    protected RDoubleVector pass(RDoubleSequence vector) {
        controlVisibility();
        RDoubleVector result = (RDoubleVector) vector.copyDropAttributes();
        result.copyNamesFrom(vector);
        return result;
    }

    @Specialization
    protected RList pass(RList list) {
        controlVisibility();
        RList result = (RList) list.copyDropAttributes();
        result.copyNamesFrom(list);
        return result;
    }

    @Specialization(guards = "noArgNames")
    protected int pass(int value) {
        controlVisibility();
        return value;
    }

    @Specialization(guards = "hasArgNames")
    protected RIntVector passArgs(int value) {
        controlVisibility();
        return RDataFactory.createIntVector(new int[]{value}, true, RDataFactory.createStringVector(getSuppliedArgsNames(), true));
    }

    @Specialization(guards = "noArgNames")
    protected double pass(double value) {
        controlVisibility();
        return value;
    }

    @Specialization(guards = "hasArgNames")
    protected RDoubleVector passArgs(double value) {
        controlVisibility();
        return RDataFactory.createDoubleVector(new double[]{value}, true, RDataFactory.createStringVector(getSuppliedArgsNames(), true));
    }

    @Specialization(guards = "noArgNames")
    protected byte pass(byte value) {
        controlVisibility();
        return value;
    }

    @Specialization(guards = "hasArgNames")
    protected RLogicalVector passArgs(byte value) {
        controlVisibility();
        return RDataFactory.createLogicalVector(new byte[]{value}, true, RDataFactory.createStringVector(getSuppliedArgsNames(), true));
    }

    @Specialization(guards = "noArgNames")
    protected String pass(String value) {
        controlVisibility();
        return value;
    }

    @Specialization(guards = "hasArgNames")
    protected RStringVector passArgs(String value) {
        controlVisibility();
        return RDataFactory.createStringVector(new String[]{value}, true, RDataFactory.createStringVector(getSuppliedArgsNames(), true));
    }

    @Specialization(guards = "noArgNames")
    protected RRaw pass(RRaw value) {
        controlVisibility();
        return value;
    }

    @Specialization(guards = "hasArgNames")
    protected RRawVector passArgs(RRaw value) {
        controlVisibility();
        return RDataFactory.createRawVector(new byte[]{value.getValue()}, RDataFactory.createStringVector(getSuppliedArgsNames(), true));
    }

    @Specialization(guards = "noArgNames")
    protected RComplex pass(RComplex value) {
        controlVisibility();
        return value;
    }

    @Specialization(guards = "hasArgNames")
    protected RComplexVector passArgs(RComplex value) {
        controlVisibility();
        return RDataFactory.createComplexVector(new double[]{value.getRealPart(), value.getImaginaryPart()}, true, RDataFactory.createStringVector(getSuppliedArgsNames(), true));
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
            precedence = Math.max(precedence, precedenceNode.executeInteger(frame, array[i], RRuntime.LOGICAL_FALSE));
        }
        return precedence;
    }

    private Object castComplex(VirtualFrame frame, Object operand) {
        if (castComplex == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castComplex = insert(CastComplexNodeFactory.create(null, true, false, false));
        }
        return castComplex.executeCast(frame, operand);
    }

    private Object castDouble(VirtualFrame frame, Object operand) {
        if (castDouble == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castDouble = insert(CastDoubleNodeFactory.create(null, true, false, false));
        }
        return castDouble.executeCast(frame, operand);
    }

    private Object castInteger(VirtualFrame frame, Object operand) {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInteger = insert(CastIntegerNodeFactory.create(null, true, false, false));
        }
        return castInteger.executeCast(frame, operand);
    }

    private Object castLogical(VirtualFrame frame, Object operand) {
        if (castLogical == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castLogical = insert(CastLogicalNodeFactory.create(null, true, false, false));
        }
        return castLogical.executeCast(frame, operand);
    }

    private Object castString(VirtualFrame frame, Object operand) {
        if (castString == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castString = insert(CastStringNodeFactory.create(null, false, true, false, false));
        }
        return castString.executeCast(frame, operand);
    }

    private Object castRaw(VirtualFrame frame, Object operand) {
        if (castRaw == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castRaw = insert(CastRawNodeFactory.create(null, true, false, false));
        }
        return castRaw.executeCast(frame, operand);
    }

    private Object castList(VirtualFrame frame, Object operand) {
        if (castList == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castList = insert(CastListNodeFactory.create(null, true, false, false));
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

    @Specialization(guards = "isNullPrecedence")
    @ExplodeLoop
    protected RNull allNull(VirtualFrame frame, Object[] array) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization(guards = {"isLogicalPrecedence", "noArgNames"})
    @ExplodeLoop
    protected Object allLogical(VirtualFrame frame, Object[] array) {
        controlVisibility();
        Object current = castLogical(frame, array[0]);
        for (int i = 1; i < array.length; i++) {
            Object other = castLogical(frame, array[i]);
            current = foldOperation.executeLogical(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isLogicalPrecedence", "hasArgNames"})
    @ExplodeLoop
    protected Object allLogicalArgs(VirtualFrame frame, Object[] array) {
        controlVisibility();
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castLogical(frame, namesMerge(currentVector, getSuppliedArgsNames()[0]));
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castLogical(frame, namesMerge(otherVector, getSuppliedArgsNames()[i]));
            current = foldOperation.executeLogical(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isIntegerPrecedence", "noArgNames"})
    @ExplodeLoop
    protected Object allInt(VirtualFrame frame, Object[] array) {
        controlVisibility();
        Object current = castInteger(frame, array[0]);
        for (int i = 1; i < array.length; i++) {
            Object other = castInteger(frame, array[i]);
            current = foldOperation.executeInteger(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isIntegerPrecedence", "hasArgNames"})
    @ExplodeLoop
    protected Object allIntArgs(VirtualFrame frame, Object[] array) {
        controlVisibility();
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castInteger(frame, namesMerge(currentVector, getSuppliedArgsNames()[0]));
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castInteger(frame, namesMerge(otherVector, getSuppliedArgsNames()[i]));
            current = foldOperation.executeInteger(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isDoublePrecedence", "noArgNames"})
    @ExplodeLoop
    protected Object allDouble(VirtualFrame frame, Object[] array) {
        controlVisibility();
        Object current = castDouble(frame, array[0]);
        for (int i = 1; i < array.length; i++) {
            Object other = castDouble(frame, array[i]);
            current = foldOperation.executeDouble(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isDoublePrecedence", "hasArgNames"})
    @ExplodeLoop
    protected Object allDoubleArgs(VirtualFrame frame, Object[] array) {
        controlVisibility();
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castDouble(frame, namesMerge(currentVector, getSuppliedArgsNames()[0]));
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castDouble(frame, namesMerge(otherVector, getSuppliedArgsNames()[i]));
            current = foldOperation.executeDouble(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isComplexPrecedence", "noArgNames"})
    @ExplodeLoop
    protected Object allComplex(VirtualFrame frame, Object[] array) {
        controlVisibility();
        Object current = castComplex(frame, array[0]);
        for (int i = 1; i < array.length; i++) {
            Object other = castComplex(frame, array[i]);
            current = foldOperation.executeComplex(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isComplexPrecedence", "hasArgNames"})
    @ExplodeLoop
    protected Object allComplexArgs(VirtualFrame frame, Object[] array) {
        controlVisibility();
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castComplex(frame, namesMerge(currentVector, getSuppliedArgsNames()[0]));
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castComplex(frame, namesMerge(otherVector, getSuppliedArgsNames()[i]));
            current = foldOperation.executeComplex(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isStringPrecedence", "noArgNames"})
    @ExplodeLoop
    protected Object allString(VirtualFrame frame, Object[] array) {
        controlVisibility();
        Object current = castString(frame, array[0]);
        for (int i = 1; i < array.length; i++) {
            Object other = castString(frame, array[i]);
            current = foldOperation.executeString(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isStringPrecedence", "hasArgNames"})
    @ExplodeLoop
    protected Object allStringArgs(VirtualFrame frame, Object[] array) {
        controlVisibility();
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castString(frame, namesMerge(currentVector, getSuppliedArgsNames()[0]));
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castString(frame, namesMerge(otherVector, getSuppliedArgsNames()[i]));
            current = foldOperation.executeString(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isRawPrecedence", "noArgNames"})
    @ExplodeLoop
    protected Object allRaw(VirtualFrame frame, Object[] array) {
        controlVisibility();
        Object current = castRaw(frame, array[0]);
        for (int i = 1; i < array.length; i++) {
            Object other = castRaw(frame, array[i]);
            current = foldOperation.executeRaw(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isRawPrecedence", "hasArgNames"})
    @ExplodeLoop
    protected Object allRawArgs(VirtualFrame frame, Object[] array) {
        controlVisibility();
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castRaw(frame, namesMerge(currentVector, getSuppliedArgsNames()[0]));
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castRaw(frame, namesMerge(otherVector, getSuppliedArgsNames()[i]));
            current = foldOperation.executeRaw(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isListPrecedence", "noArgNames"})
    @ExplodeLoop
    protected Object list(VirtualFrame frame, Object[] array) {
        controlVisibility();
        RList current = RDataFactory.createList();
        for (int i = 0; i < array.length; ++i) {
            Object other = castList(frame, array[i]);
            current = (RList) foldOperation.executeList(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isListPrecedence", "hasArgNames"})
    protected Object listArgs(VirtualFrame frame, Object[] array) {
        controlVisibility();
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castList(frame, namesMerge(currentVector, getSuppliedArgsNames()[0]));
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castList(frame, namesMerge(otherVector, getSuppliedArgsNames()[i]));
            current = foldOperation.executeList(frame, current, other);
        }
        return current;
    }

    public boolean hasArgNames() {
        return getSuppliedArgsNames() != null;
    }

    public boolean noArgNames() {
        return !hasArgNames();
    }
}
