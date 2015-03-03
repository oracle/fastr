/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.nodes.Node.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.binary.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.CombineFactory.UnwrapExpressionNodeGen;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.function.DispatchedCallNode.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "c", kind = PRIMITIVE, parameterNames = {"..."})
@GenerateNodeFactory
@SuppressWarnings("unused")
public abstract class Combine extends RPrecedenceBuiltinNode {

    @Child private CastToVectorNode castVector;

    @Child private FoldOperationNode foldOperation;

    @Child private Combine combineRecursive;

    @Child private UnwrapExpression unwrapExpression;

    @Child private DispatchedCallNode dcn;

    private final ConditionProfile noAttributesAndNamesProfile = ConditionProfile.createBinaryProfile();

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    public abstract Object executeCombine(VirtualFrame frame, Object value);

    private RAbstractVector castVector(VirtualFrame frame, Object value) {
        if (castVector == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castVector = insert(CastToVectorNodeGen.create(null, false, false, false, false));
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
    }

    protected Combine(Combine previousNode) {
        this.foldOperation = previousNode.foldOperation;
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

    private RVector passVector(RVector vector) {
        controlVisibility();
        if (noAttributesAndNamesProfile.profile(vector.getAttributes() == null && vector.getNames(attrProfiles) == null && vector.getDimNames() == null)) {
            return vector;
        } else {
            RVector result = vector.copyDropAttributes();
            result.copyNamesFrom(attrProfiles, vector);
            return result;
        }
    }

    @Specialization
    protected RIntVector pass(RIntVector vector) {
        return (RIntVector) passVector(vector);
    }

    @Specialization
    protected RDoubleVector pass(RDoubleVector vector) {
        return (RDoubleVector) passVector(vector);
    }

    @Specialization
    protected RComplexVector pass(RComplexVector vector) {
        return (RComplexVector) passVector(vector);
    }

    @Specialization
    protected RStringVector pass(RStringVector vector) {
        return (RStringVector) passVector(vector);
    }

    @Specialization
    protected RRawVector pass(RRawVector vector) {
        return (RRawVector) passVector(vector);
    }

    @Specialization
    protected RLogicalVector pass(RLogicalVector vector) {
        return (RLogicalVector) passVector(vector);
    }

    @Specialization(guards = "!isNumericVersion")
    protected RList pass(RList list) {
        return (RList) passVector(list);
    }

    private static final ArgumentsSignature SIGNATURE = ArgumentsSignature.get(new String[]{"..."});

    @Specialization(guards = "isNumericVersion")
    /**
     * A temporary specific hack for internal generic dispatch on "numeric_version" objects
     * which are {@link Rlist}s.
     */
    protected RList passNumericVersion(VirtualFrame frame, RList list) {
        if (dcn == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            dcn = insert(DispatchedCallNode.create("c", DispatchType.UseMethod, SIGNATURE));
        }
        return (RList) dcn.executeInternal(frame, list.getClassHierarchy(), new Object[]{list});
    }

    public boolean isNumericVersion(RList list) {
        RStringVector klass = list.getClassAttr(attrProfiles);
        if (klass != null) {
            for (int i = 0; i < klass.getLength(); i++) {
                if (klass.getDataAt(i).equals("numeric_version")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Specialization
    protected RIntSequence pass(RIntSequence sequence) {
        controlVisibility();
        // sequences are immutable anyway
        return sequence;
    }

    @Specialization
    protected RDoubleSequence pass(RDoubleSequence sequence) {
        controlVisibility();
        // sequences are immutable anyway
        return sequence;
    }

    @Specialization
    protected RExpression pass(RExpression expr) {
        controlVisibility();
        return expr;
    }

    @Specialization(guards = "noArgNames")
    protected int pass(int value) {
        controlVisibility();
        return value;
    }

    @Specialization(guards = "hasArgNames")
    protected RIntVector passArgs(int value) {
        controlVisibility();
        return RDataFactory.createIntVector(new int[]{value}, true, getSuppliedSignature().createVector());
    }

    @Specialization(guards = "noArgNames")
    protected double pass(double value) {
        controlVisibility();
        return value;
    }

    @Specialization(guards = "hasArgNames")
    protected RDoubleVector passArgs(double value) {
        controlVisibility();
        return RDataFactory.createDoubleVector(new double[]{value}, true, getSuppliedSignature().createVector());
    }

    @Specialization(guards = "noArgNames")
    protected byte pass(byte value) {
        controlVisibility();
        return value;
    }

    @Specialization(guards = "hasArgNames")
    protected RLogicalVector passArgs(byte value) {
        controlVisibility();
        return RDataFactory.createLogicalVector(new byte[]{value}, true, getSuppliedSignature().createVector());
    }

    @Specialization(guards = "noArgNames")
    protected String pass(String value) {
        controlVisibility();
        return value;
    }

    @Specialization(guards = "hasArgNames")
    protected RStringVector passArgs(String value) {
        controlVisibility();
        return RDataFactory.createStringVector(new String[]{value}, true, getSuppliedSignature().createVector());
    }

    @Specialization(guards = "noArgNames")
    protected RRaw pass(RRaw value) {
        controlVisibility();
        return value;
    }

    @Specialization(guards = "hasArgNames")
    protected RRawVector passArgs(RRaw value) {
        controlVisibility();
        return RDataFactory.createRawVector(new byte[]{value.getValue()}, getSuppliedSignature().createVector());
    }

    @Specialization(guards = "noArgNames")
    protected RComplex pass(RComplex value) {
        controlVisibility();
        return value;
    }

    @Specialization(guards = "hasArgNames")
    protected RComplexVector passArgs(RComplex value) {
        controlVisibility();
        return RDataFactory.createComplexVector(new double[]{value.getRealPart(), value.getImaginaryPart()}, true, getSuppliedSignature().createVector());
    }

    public RAbstractVector namesMerge(RAbstractVector vector, String name) {
        RStringVector orgNamesObject = vector.getNames(attrProfiles);
        if ((orgNamesObject == null && name == null) || vector.getLength() == 0) {
            return vector;
        }
        if (orgNamesObject == null) {
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
            RStringVector orgNames = orgNamesObject;
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
    protected RNull allNull(VirtualFrame frame, RArgsValuesAndNames args) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization(guards = {"!isNullPrecedence", "oneElement"})
    protected Object allOneElem(VirtualFrame frame, RArgsValuesAndNames args) {
        controlVisibility();
        if (combineRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            combineRecursive = insert(CombineFactory.create(new RNode[1], getBuiltin(), getSuppliedSignature()));
        }
        return combineRecursive.executeCombine(frame, args.getValues()[0]);
    }

    @Specialization(guards = {"isLogicalPrecedence", "noArgNames", "!oneElement"})
    @ExplodeLoop
    protected Object allLogical(VirtualFrame frame, RArgsValuesAndNames args) {
        controlVisibility();
        Object[] array = args.getValues();
        Object current = castLogical(frame, array[0], false);
        for (int i = 1; i < array.length; i++) {
            Object other = castLogical(frame, array[i], false);
            current = foldOperation.executeLogical(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isLogicalPrecedence", "hasArgNames", "!oneElement"})
    @ExplodeLoop
    protected Object allLogicalArgs(VirtualFrame frame, RArgsValuesAndNames args) {
        controlVisibility();
        Object[] array = args.getValues();
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castLogical(frame, namesMerge(currentVector, getSuppliedSignature().getName(0)), false);
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castLogical(frame, namesMerge(otherVector, getSuppliedSignature().getName(i)), false);
            current = foldOperation.executeLogical(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isIntegerPrecedence", "noArgNames", "!oneElement"})
    @ExplodeLoop
    protected Object allInt(VirtualFrame frame, RArgsValuesAndNames args) {
        controlVisibility();
        Object[] array = args.getValues();
        Object current = castInteger(frame, array[0], false);
        for (int i = 1; i < array.length; i++) {
            Object other = castInteger(frame, array[i], false);
            current = foldOperation.executeInteger(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isIntegerPrecedence", "hasArgNames", "!oneElement"})
    @ExplodeLoop
    protected Object allIntArgs(VirtualFrame frame, RArgsValuesAndNames args) {
        controlVisibility();
        Object[] array = args.getValues();
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castInteger(frame, namesMerge(currentVector, getSuppliedSignature().getName(0)), false);
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castInteger(frame, namesMerge(otherVector, getSuppliedSignature().getName(i)), false);
            current = foldOperation.executeInteger(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isDoublePrecedence", "noArgNames", "!oneElement"})
    @ExplodeLoop
    protected Object allDouble(VirtualFrame frame, RArgsValuesAndNames args) {
        controlVisibility();
        Object[] array = args.getValues();
        Object current = castDouble(frame, array[0], false);
        for (int i = 1; i < array.length; i++) {
            Object other = castDouble(frame, array[i], false);
            current = foldOperation.executeDouble(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isDoublePrecedence", "hasArgNames", "!oneElement"})
    @ExplodeLoop
    protected Object allDoubleArgs(VirtualFrame frame, RArgsValuesAndNames args) {
        controlVisibility();
        Object[] array = args.getValues();
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castDouble(frame, namesMerge(currentVector, getSuppliedSignature().getName(0)), false);
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castDouble(frame, namesMerge(otherVector, getSuppliedSignature().getName(i)), false);
            current = foldOperation.executeDouble(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isComplexPrecedence", "noArgNames", "!oneElement"})
    @ExplodeLoop
    protected Object allComplex(VirtualFrame frame, RArgsValuesAndNames args) {
        controlVisibility();
        Object[] array = args.getValues();
        Object current = castComplex(frame, array[0], false);
        for (int i = 1; i < array.length; i++) {
            Object other = castComplex(frame, array[i], false);
            current = foldOperation.executeComplex(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isComplexPrecedence", "hasArgNames", "!oneElement"})
    @ExplodeLoop
    protected Object allComplexArgs(VirtualFrame frame, RArgsValuesAndNames args) {
        controlVisibility();
        Object[] array = args.getValues();
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castComplex(frame, namesMerge(currentVector, getSuppliedSignature().getName(0)), false);
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castComplex(frame, namesMerge(otherVector, getSuppliedSignature().getName(i)), false);
            current = foldOperation.executeComplex(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isStringPrecedence", "noArgNames", "!oneElement"})
    @ExplodeLoop
    protected Object allString(VirtualFrame frame, RArgsValuesAndNames args) {
        controlVisibility();
        Object[] array = args.getValues();
        Object current = castString(frame, array[0], false);
        for (int i = 1; i < array.length; i++) {
            Object other = castString(frame, array[i], false);
            current = foldOperation.executeString(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isStringPrecedence", "hasArgNames", "!oneElement"})
    @ExplodeLoop
    protected Object allStringArgs(VirtualFrame frame, RArgsValuesAndNames args) {
        controlVisibility();
        Object[] array = args.getValues();
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castString(frame, namesMerge(currentVector, getSuppliedSignature().getName(0)), false);
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castString(frame, namesMerge(otherVector, getSuppliedSignature().getName(i)), false);
            current = foldOperation.executeString(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isRawPrecedence", "noArgNames", "!oneElement"})
    @ExplodeLoop
    protected Object allRaw(VirtualFrame frame, RArgsValuesAndNames args) {
        controlVisibility();
        Object[] array = args.getValues();
        Object current = castRaw(frame, array[0], false);
        for (int i = 1; i < array.length; i++) {
            Object other = castRaw(frame, array[i], false);
            current = foldOperation.executeRaw(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isRawPrecedence", "hasArgNames", "!oneElement"})
    @ExplodeLoop
    protected Object allRawArgs(VirtualFrame frame, RArgsValuesAndNames args) {
        controlVisibility();
        Object[] array = args.getValues();
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castRaw(frame, namesMerge(currentVector, getSuppliedSignature().getName(0)), false);
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castRaw(frame, namesMerge(otherVector, getSuppliedSignature().getName(i)), false);
            current = foldOperation.executeRaw(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isListPrecedence", "noArgNames", "!oneElement"})
    @ExplodeLoop
    protected Object list(VirtualFrame frame, RArgsValuesAndNames args) {
        controlVisibility();
        Object[] array = args.getValues();
        RList current = RDataFactory.createList();
        for (int i = 0; i < array.length; ++i) {
            Object other = castList(frame, array[i], false);
            current = (RList) foldOperation.executeList(frame, current, other);
        }
        return current;
    }

    @Specialization(guards = {"isListPrecedence", "hasArgNames", "!oneElement"})
    protected Object listArgs(VirtualFrame frame, RArgsValuesAndNames args) {
        controlVisibility();
        Object[] array = args.getValues();
        RAbstractVector currentVector = castVector(frame, array[0]);
        Object current = castList(frame, namesMerge(currentVector, getSuppliedSignature().getName(0)), false);
        for (int i = 1; i < array.length; i++) {
            RAbstractVector otherVector = castVector(frame, array[i]);
            Object other = castList(frame, namesMerge(otherVector, getSuppliedSignature().getName(i)), false);
            current = foldOperation.executeList(frame, current, other);
        }
        return current;
    }

    private Object unwrapExpression(VirtualFrame frame, Object operand) {
        if (unwrapExpression == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            unwrapExpression = insert(UnwrapExpressionNodeGen.create(null));
        }
        return unwrapExpression.executeObject(frame, operand);
    }

    @Specialization(guards = {"isExprPrecedence", "noArgNames", "!oneElement"})
    @ExplodeLoop
    protected Object expr(VirtualFrame frame, RArgsValuesAndNames args) {
        controlVisibility();
        Object[] array = args.getValues();
        RList current = RDataFactory.createList();
        for (int i = 0; i < array.length; ++i) {
            Object op = unwrapExpression(frame, array[i]);
            Object other = castList(frame, op, false);
            current = (RList) foldOperation.executeList(frame, current, other);
        }
        return RDataFactory.createExpression(current);
    }

    @Specialization(guards = {"isExprPrecedence", "hasArgNames", "!oneElement"})
    protected Object exprArgs(VirtualFrame frame, RArgsValuesAndNames args) {
        controlVisibility();
        Object[] array = args.getValues();
        Object op = unwrapExpression(frame, array[0]);
        RAbstractVector currentVector = castVector(frame, op);
        Object current = castList(frame, namesMerge(currentVector, getSuppliedSignature().getName(0)), false);
        for (int i = 1; i < array.length; i++) {
            op = unwrapExpression(frame, array[i]);
            RAbstractVector otherVector = castVector(frame, op);
            Object other = castList(frame, namesMerge(otherVector, getSuppliedSignature().getName(i)), false);
            current = foldOperation.executeList(frame, current, other);
        }
        return RDataFactory.createExpression((RList) current);
    }

    protected boolean oneElement(RArgsValuesAndNames args) {
        return args.length() == 1;
    }

    protected boolean hasArgNames() {
        return getSuppliedSignature().getNonNullCount() > 0;
    }

    protected boolean noArgNames() {
        return !hasArgNames();
    }

    @NodeChild("operand")
    protected abstract static class UnwrapExpression extends RNode {

        public abstract Object executeObject(VirtualFrame frame, Object operand);

        @Specialization
        protected RNull unwrap(RNull operand) {
            return operand;
        }

        @Specialization
        protected RList unwrap(RExpression operand) {
            return operand.getList();
        }

        @Specialization
        protected RAbstractVector unwrap(RAbstractVector operand) {
            return operand;
        }

        @Specialization
        protected RDataFrame unwrap(RDataFrame operand) {
            return operand;
        }

        @Specialization
        protected RLanguage unwrap(RLanguage operand) {
            return operand;
        }

    }
}
