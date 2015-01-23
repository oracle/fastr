/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.utilities.ConditionProfile;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseCheckHelperNode;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * The {@code switch} builtin. When called directly, the "..." arguments are not evaluated before
 * the call, as the semantics requires that only the matched case is evaluated. However, if called
 * indirectly, e.g., by {@do.call}, the arguments will have been evaluated, regardless of the match,
 * so we have to be prepared for both evaluated and unevaluated args, which is encapsulated in
 * {@link PromiseCheckHelperNode}.
 *
 */
@RBuiltin(name = "switch", kind = PRIMITIVE, parameterNames = {"EXPR", "..."}, nonEvalArgs = {1})
public abstract class Switch extends RBuiltinNode {
    @Child private CastIntegerNode castIntNode;
    @Child private PromiseCheckHelperNode promiseHelper = new PromiseCheckHelperNode();

    private final BranchProfile suppliedArgNameIsEmpty = BranchProfile.create();
    private final BranchProfile suppliedArgNameIsNull = BranchProfile.create();
    private final BranchProfile matchedArgIsMissing = BranchProfile.create();
    private final ConditionProfile currentDefaultProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile returnValueProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile notIntType = BranchProfile.create();

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @Specialization(guards = "isLengthOne")
    @TruffleBoundary
    protected Object doSwitch(VirtualFrame frame, RAbstractStringVector x, RArgsValuesAndNames optionalArgs) {
        controlVisibility();
        return prepareResult(doSwitchString(frame, x, optionalArgs));
    }

    @TruffleBoundary
    protected Object doSwitchString(VirtualFrame frame, RAbstractStringVector x, RArgsValuesAndNames optionalArgs) {
        controlVisibility();
        Object[] optionalArgValues = optionalArgs.getValues();
        final String xStr = x.getDataAt(0);
        final String[] names = optionalArgs.getNames();
        for (int i = 0; i < names.length; ++i) {
            final String suppliedArgName = names[i];
            if (suppliedArgName == null) {
                continue;
            } else if (suppliedArgName.length() == 0) {
                suppliedArgNameIsEmpty.enter();
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.ZERO_LENGTH_VARIABLE);
            } else if (xStr.equals(suppliedArgName)) {
                // match, evaluate the associated arg
                Object optionalArgValue = promiseHelper.checkEvaluate(frame, optionalArgValues[i]);
                if (RMissingHelper.isMissing(optionalArgValue)) {
                    matchedArgIsMissing.enter();

                    // Fall-through: If the matched value is missing, take the next non-missing
                    for (int j = i + 1; j < optionalArgValues.length; j++) {
                        Object val = promiseHelper.checkEvaluate(frame, optionalArgValues[j]);
                        if (!RMissingHelper.isMissing(val)) {
                            return val;
                        }
                    }

                    // No non-missing value: invisible null
                    return null;
                } else {
                    // Default: Matched name has a value
                    return optionalArgValue;
                }
            }
        }
        // We didn't find a match, so check for default(s)
        Object currentDefault = null;
        for (int i = 0; i < names.length; ++i) {
            final String suppliedArgName = names[i];
            if (suppliedArgName == null) {
                suppliedArgNameIsNull.enter();
                Object optionalArg = optionalArgValues[i];
                if (currentDefaultProfile.profile(currentDefault != null)) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.DUPLICATE_SWITCH_DEFAULT, deparseDefault(currentDefault), deparseDefault(optionalArg));
                } else {
                    currentDefault = optionalArg;
                }
            }
        }
        if (returnValueProfile.profile(currentDefault != null)) {
            return promiseHelper.checkEvaluate(frame, currentDefault);
        } else {
            return null;
        }
    }

    private static String deparseDefault(Object arg) {
        if (arg instanceof RPromise) {
            // We do not want to evaluate the promise,just display the rep
            RPromise p = (RPromise) arg;
            RNode node = (RNode) p.getRep();
            State state = State.createPrintableState();
            node.deparse(state);
            return state.toString();
        } else {
            return RDeparse.deparseForPrint(arg);
        }
    }

    @Specialization
    protected Object doSwitch(VirtualFrame frame, int x, RArgsValuesAndNames optionalArgs) {
        return prepareResult(doSwitchInt(frame, x, optionalArgs));
    }

    @Specialization
    protected Object doSwitch(VirtualFrame frame, Object x, RArgsValuesAndNames optionalArgs) {
        if (castIntNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castIntNode = insert(CastIntegerNodeGen.create(null, false, false, false));
        }
        Object objIndex = castIntNode.executeCast(frame, x);
        if (!(objIndex instanceof Integer)) {
            notIntType.enter();
            return null;
        }
        return prepareResult(doSwitchInt(frame, (int) objIndex, optionalArgs));
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object doSwitch(RMissing x, RMissing optionalArgs) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.EXPR_MISSING);
    }

    private Object doSwitchInt(VirtualFrame frame, int index, RArgsValuesAndNames optionalArgs) {
        Object[] optionalArgValues = optionalArgs.getValues();
        if (index >= 1 && index <= optionalArgValues.length) {
            Object value = promiseHelper.checkEvaluate(frame, optionalArgValues[index - 1]);
            if (value != null) {
                return value;
            }
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NO_ALTERNATIVE_IN_SWITCH);
        }
        return null;
    }

    protected boolean isLengthOne(RAbstractStringVector x) {
        return x.getLength() == 1;
    }

    private Object prepareResult(Object value) {
        if (returnValueProfile.profile(value != null)) {
            forceVisibility(true);
            return value;
        } else {
            forceVisibility(false);
            return RNull.instance;
        }
    }

}
