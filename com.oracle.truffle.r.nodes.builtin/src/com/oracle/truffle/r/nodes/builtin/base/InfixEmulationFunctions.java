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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.array.*;
import com.oracle.truffle.r.nodes.access.array.ArrayPositionCast.*;
import com.oracle.truffle.r.nodes.access.array.ArrayPositionCastNodeGen.*;
import com.oracle.truffle.r.nodes.access.array.read.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.InfixEmulationFunctionsFactory.PromiseEvaluatorNodeGen;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Work-around builtins for infix operators that FastR (currently) does not define as functions.
 * These definitions create the illusion that the definitions exist, even if they are not actually
 * bound to anything useful.
 *
 * One important reason that these must exist as {@link RBuiltin}s is that they occur when deparsing
 * packages and the deparse logic depends on them being found as builtins. See {@link RDeparse}.
 *
 * N.B. These could be implemented by delegating to the equivalent nodes, e.g.
 * {@link AccessArrayNode}.
 */
public class InfixEmulationFunctions {

    public abstract static class ErrorAdapter extends RBuiltinNode {
        protected RError nyi() throws RError {
            throw RError.nyi(getEncapsulatingSourceSection(), "");
        }
    }

    private static class AccessPositions extends PositionsArrayConversionNodeAdapter {
        @Children protected final MultiDimPosConverterNode[] multiDimOperatorConverters;
        private final int length;

        public AccessPositions(ArrayPositionCast[] elements, OperatorConverterNode[] operatorConverters, MultiDimPosConverterNode[] multiDimOperatorConverters) {
            super(elements, operatorConverters);
            this.multiDimOperatorConverters = multiDimOperatorConverters;
            assert elements.length == operatorConverters.length && (multiDimOperatorConverters == null || elements.length == multiDimOperatorConverters.length);
            this.length = elements.length;
        }

        public int getLength() {
            return length;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            RInternalError.shouldNotReachHere();
            return null;
        }

        @ExplodeLoop
        public Object execute(VirtualFrame frame, Object vector, Object[] pos, byte exact, Object[] newPositions) {
            for (int i = 0; i < length; i++) {
                newPositions[i] = elements[i].executeArg(frame, vector, operatorConverters[i].executeConvert(frame, vector, pos[i], exact));
                if (multiDimOperatorConverters != null) {
                    newPositions[i] = multiDimOperatorConverters[i].executeConvert(frame, vector, newPositions[i]);
                }
            }
            if (elements.length == 1) {
                return newPositions[0];
            } else {
                return newPositions;
            }
        }

        public static AccessPositions create(ArrayPositionCast[] castPositions, OperatorConverterNode[] operatorConverters, MultiDimPosConverterNode[] multiDimOperatorConverters) {
            return new AccessPositions(castPositions, operatorConverters, multiDimOperatorConverters);
        }

    }

    @NodeChild(value = "op")
    protected abstract static class PromiseEvaluator extends RNode {

        protected abstract Object execute(VirtualFrame frame, Object op);

        @Child private PromiseHelperNode promiseHelper = new PromiseHelperNode();
        @Child private PromiseEvaluator evalRecursive;

        protected Object evalRecursive(VirtualFrame frame, Object op) {
            if (evalRecursive == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                evalRecursive = insert(PromiseEvaluatorNodeGen.create(null));
            }
            return evalRecursive.execute(frame, op);
        }

        @Specialization
        protected Object eval(VirtualFrame frame, RPromise p) {
            return promiseHelper.evaluate(frame, p);
        }

        @Specialization
        protected RAbstractVector eval(RAbstractVector op) {
            return op;
        }

        @ExplodeLoop
        @Specialization(guards = "!argsEmpty")
        protected RArgsValuesAndNames eval(VirtualFrame frame, RArgsValuesAndNames args) {
            Object[] values = args.getValues();
            for (int i = 0; i < values.length; i++) {
                values[i] = evalRecursive(frame, values[i]);
            }
            return args;
        }

        @Specialization(guards = "argsEmpty")
        protected RArgsValuesAndNames evalEmpty(RArgsValuesAndNames args) {
            return args;
        }

        @Fallback
        protected Object eval(Object op) {
            return op;
        }

        protected boolean argsEmpty(RArgsValuesAndNames args) {
            return args.length() == 0;
        }

    }

    public abstract static class AccessArrayBuiltin extends RBuiltinNode {
        @Child private AccessArrayNode accessNode;
        @Child private AccessPositions positions;

        @CreateCast("arguments")
        public RNode[] castArguments(RNode[] arguments) {
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = PromiseEvaluatorNodeGen.create(arguments[i]);
            }
            return arguments;
        }

        @ExplodeLoop
        protected Object access(VirtualFrame frame, Object vector, byte exact, RArgsValuesAndNames inds, Object dropDim, boolean isSubset) {
            if (accessNode == null || positions.getLength() != inds.length()) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                if (accessNode == null) {
                    accessNode = insert(AccessArrayNodeGen.create(isSubset, false, false, false, null, null, null, null, null));
                }
                int len = inds.length();
                ArrayPositionCast[] castPositions = new ArrayPositionCast[len];
                OperatorConverterNode[] operatorConverters = new OperatorConverterNode[len];
                MultiDimPosConverterNode[] multiDimOperatorConverters = inds.length() == 1 ? null : new MultiDimPosConverterNode[len];
                for (int i = 0; i < len; i++) {
                    castPositions[i] = ArrayPositionCastNodeGen.create(i, inds.length(), false, isSubset, ConstantNode.create(RNull.instance) /* dummy */, null);
                    operatorConverters[i] = OperatorConverterNodeGen.create(i, inds.length(), false, isSubset, null, ConstantNode.create(RNull.instance) /* dummy */, null);
                    if (multiDimOperatorConverters != null) {
                        multiDimOperatorConverters[i] = MultiDimPosConverterNodeGen.create(isSubset, null, null);
                    }
                }
                positions = insert(AccessPositions.create(castPositions, operatorConverters, multiDimOperatorConverters));
            }
            Object[] pos = inds.getValues();
            return accessNode.executeAccess(frame, vector, exact, 0, positions.execute(frame, vector, pos, exact, pos), dropDim);
        }

        protected boolean noInd(@SuppressWarnings("unused") RAbstractContainer x, RArgsValuesAndNames inds) {
            return inds.length() == 0;
        }

    }

    public abstract static class AccessArraySubsetBuiltinBase extends AccessArrayBuiltin {

        protected final ConditionProfile multiIndexProfile = ConditionProfile.createBinaryProfile();

        protected Object get(VirtualFrame frame, RAbstractContainer x, RArgsValuesAndNames inds, RAbstractLogicalVector dropVec) {
            return access(frame, x, RRuntime.LOGICAL_FALSE, inds, dropVec, true);
        }

        protected Object get(VirtualFrame frame, RAbstractContainer x, RArgsValuesAndNames inds, @SuppressWarnings("unused") RMissing dropVec) {
            byte drop;
            if (multiIndexProfile.profile(inds.length() > 1)) {
                drop = RRuntime.LOGICAL_TRUE;
            } else {
                drop = RRuntime.LOGICAL_FALSE;
            }
            return access(frame, x, RRuntime.LOGICAL_FALSE, inds, drop, true);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "noInd")
        protected Object getNoInd(RAbstractContainer x, RArgsValuesAndNames inds, Object dropVec) {
            return x;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object get(RAbstractContainer x, RMissing inds, Object dropVec) {
            return x;
        }

    }

    @RBuiltin(name = "[", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x", "...", "drop"})
    public abstract static class AccessArraySubsetBuiltin extends AccessArraySubsetBuiltinBase {

        private static final String NAME = "[";

        @Child private DispatchedCallNode dcn;

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
        }

        @Specialization(guards = {"!noInd", "isObject"})
        protected Object getObj(VirtualFrame frame, RAbstractContainer x, RArgsValuesAndNames inds, RAbstractLogicalVector dropVec) {
            if (dcn == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                dcn = insert(DispatchedCallNode.create(NAME, RRuntime.USE_METHOD, new String[]{"", "", "drop"}));
            }
            try {
                return dcn.executeInternal(frame, x.getClassHierarchy(), new Object[]{x, inds, dropVec});
            } catch (RError e) {
                return access(frame, x, RRuntime.LOGICAL_FALSE, inds, dropVec, true);
            }
        }

        @Override
        @Specialization(guards = {"!noInd", "!isObject"})
        protected Object get(VirtualFrame frame, RAbstractContainer x, RArgsValuesAndNames inds, RAbstractLogicalVector dropVec) {
            return super.get(frame, x, inds, dropVec);
        }

        @Specialization(guards = {"!noInd", "isObject"})
        protected Object getObj(VirtualFrame frame, RAbstractContainer x, RArgsValuesAndNames inds, @SuppressWarnings("unused") RMissing dropVec) {
            byte drop;
            if (multiIndexProfile.profile(inds.length() > 1)) {
                drop = RRuntime.LOGICAL_TRUE;
            } else {
                drop = RRuntime.LOGICAL_FALSE;
            }

            if (dcn == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                dcn = insert(DispatchedCallNode.create(NAME, RRuntime.USE_METHOD, new String[]{"", "", "drop"}));
            }
            try {
                return dcn.executeInternal(frame, x.getClassHierarchy(), new Object[]{x, inds, drop});
            } catch (RError e) {
                return access(frame, x, RRuntime.LOGICAL_FALSE, inds, drop, true);
            }
        }

        @Override
        @Specialization(guards = {"!noInd", "!isObject"})
        protected Object get(VirtualFrame frame, RAbstractContainer x, RArgsValuesAndNames inds, RMissing dropVec) {
            return super.get(frame, x, inds, dropVec);
        }

        @SuppressFBWarnings(value = "ES_COMPARING_STRINGS_WITH_EQ", justification = "generic name is interned in the interpreted code for faster comparison")
        protected boolean isObject(VirtualFrame frame, RAbstractContainer x) {
            return x.isObject() && !(RArguments.hasS3Args(frame) && RArguments.getS3Generic(frame) == NAME);
        }

    }

    @RBuiltin(name = ".subset", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x", "...", "drop"})
    public abstract static class AccessArraySubsetDefaultBuiltin extends AccessArraySubsetBuiltinBase {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
        }

        @Override
        @Specialization(guards = "!noInd")
        protected Object get(VirtualFrame frame, RAbstractContainer x, RArgsValuesAndNames inds, RAbstractLogicalVector dropVec) {
            return super.get(frame, x, inds, dropVec);
        }

        @Override
        @Specialization(guards = "!noInd")
        protected Object get(VirtualFrame frame, RAbstractContainer x, RArgsValuesAndNames inds, RMissing dropVec) {
            return super.get(frame, x, inds, dropVec);
        }
    }

    public abstract static class AccessArraySubscriptBuiltinBase extends AccessArrayBuiltin {

        protected final ConditionProfile emptyExactProfile = ConditionProfile.createBinaryProfile();

        protected Object get(VirtualFrame frame, RAbstractContainer x, RArgsValuesAndNames inds, RAbstractLogicalVector exactVec) {
            byte exact;
            if (emptyExactProfile.profile(exactVec.getLength() == 0)) {
                exact = RRuntime.LOGICAL_FALSE;
            } else {
                exact = exactVec.getDataAt(0);
            }
            return access(frame, x, exact, inds, RRuntime.LOGICAL_TRUE, false);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "noInd")
        protected Object getNoInd(RAbstractContainer x, RArgsValuesAndNames inds, RAbstractLogicalVector exactVec) {
            throw RError.error(RError.Message.NO_INDEX);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object get(RAbstractContainer x, RMissing inds, RAbstractLogicalVector exactVec) {
            throw RError.error(RError.Message.NO_INDEX);
        }
    }

    @RBuiltin(name = "[[", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x", "...", "exact"})
    public abstract static class AccessArraySubscriptBuiltin extends AccessArraySubscriptBuiltinBase {

        private static final String NAME = "[[";

        @Child private DispatchedCallNode dcn;

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_TRUE)};
        }

        @Specialization(guards = {"!noInd", "isObject"})
        protected Object getObj(VirtualFrame frame, RAbstractContainer x, RArgsValuesAndNames inds, RAbstractLogicalVector exactVec) {
            byte exact;
            if (emptyExactProfile.profile(exactVec.getLength() == 0)) {
                exact = RRuntime.LOGICAL_FALSE;
            } else {
                exact = exactVec.getDataAt(0);
            }
            if (dcn == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                dcn = insert(DispatchedCallNode.create(NAME, RRuntime.USE_METHOD, new String[]{"", "", "exact"}));
            }
            try {
                return dcn.executeInternal(frame, x.getClassHierarchy(), new Object[]{x, inds, exactVec});
            } catch (RError e) {
                return access(frame, x, exact, inds, RRuntime.LOGICAL_TRUE, false);
            }
        }

        @Override
        @Specialization(guards = {"!noInd", "!isObject"})
        protected Object get(VirtualFrame frame, RAbstractContainer x, RArgsValuesAndNames inds, RAbstractLogicalVector exactVec) {
            return super.get(frame, x, inds, exactVec);
        }

        @SuppressFBWarnings(value = "ES_COMPARING_STRINGS_WITH_EQ", justification = "generic name is interned in the interpreted code for faster comparison")
        protected boolean isObject(VirtualFrame frame, RAbstractContainer x) {
            return x.isObject() && !(RArguments.hasS3Args(frame) && RArguments.getS3Generic(frame) == NAME);
        }

    }

    @RBuiltin(name = ".subset2", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x", "...", "exact"})
    public abstract static class AccessArraySubscriptDefaultBuiltin extends AccessArraySubscriptBuiltinBase {

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_TRUE)};
        }

        @Override
        @Specialization(guards = "!noInd")
        protected Object get(VirtualFrame frame, RAbstractContainer x, RArgsValuesAndNames inds, RAbstractLogicalVector exactVec) {
            return super.get(frame, x, inds, exactVec);
        }

    }

    @RBuiltin(name = "[<-", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x", "i"})
    public abstract static class UpdateArrayBuiltin extends ErrorAdapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object doIt(Object x, Object i) {
            throw nyi();
        }
    }

    @RBuiltin(name = "[[<-", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x", "i"})
    public abstract static class UpdateArrayNodeSubsetBuiltin extends ErrorAdapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object doIt(Object x, Object i) {
            throw nyi();
        }
    }

    @RBuiltin(name = "<-", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x", "i"})
    public abstract static class AssignBuiltin extends ErrorAdapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object doIt(Object x, Object i) {
            throw nyi();
        }
    }

    @RBuiltin(name = "<<-", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x", "i"})
    public abstract static class AssignOuterBuiltin extends ErrorAdapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object doIt(Object x, Object i) {
            throw nyi();
        }
    }

    @RBuiltin(name = "$", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x", "i"})
    public abstract static class AccessFieldBuiltin extends ErrorAdapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object doIt(Object x, Object i) {
            throw nyi();
        }
    }

    @RBuiltin(name = "$<-", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x", "i"})
    public abstract static class UpdateFieldBuiltin extends ErrorAdapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object doIt(Object x, Object i) {
            throw nyi();
        }
    }

    @RBuiltin(name = "{", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class BraceBuiltin extends ErrorAdapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object doIt(Object x) {
            throw nyi();
        }
    }

    @RBuiltin(name = "(", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class ParenBuiltin extends ErrorAdapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object doIt(Object x) {
            throw nyi();
        }
    }

    @RBuiltin(name = "if", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class IfBuiltin extends ErrorAdapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object doIt(Object x) {
            throw nyi();
        }
    }

    @RBuiltin(name = "while", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class WhileBuiltin extends ErrorAdapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object doIt(Object x) {
            throw nyi();
        }
    }

    @RBuiltin(name = "for", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class ForBuiltin extends ErrorAdapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object doIt(Object x) {
            throw nyi();
        }
    }

    @RBuiltin(name = "break", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class BreakBuiltin extends ErrorAdapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object doIt(Object x) {
            throw nyi();
        }
    }

    @RBuiltin(name = "next", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class NextBuiltin extends ErrorAdapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object doIt(Object x) {
            throw nyi();
        }
    }

    @RBuiltin(name = "function", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"x"})
    public abstract static class FunctionBuiltin extends ErrorAdapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object doIt(Object x) {
            throw nyi();
        }
    }

}
