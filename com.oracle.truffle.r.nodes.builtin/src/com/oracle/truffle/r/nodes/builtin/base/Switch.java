/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
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
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "switch", kind = PRIMITIVE, parameterNames = {"EXPR", "..."})
public abstract class Switch extends RBuiltinNode {
    @Child private CastIntegerNode castIntNode;

    private final BranchProfile suppliedArgNameIsNull = BranchProfile.create();
    private final BranchProfile matchedArgIsMissing = BranchProfile.create();
    private final ConditionProfile currentDefaultValueProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile returnValueProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile notIntType = BranchProfile.create();

    private boolean isVisible = true;

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    public boolean getVisibility() {
        return this.isVisible;
    }

    @Specialization(guards = "isLengthOne")
    @TruffleBoundary
    protected Object doSwitch(RAbstractStringVector x, RArgsValuesAndNames optionalArgs) {
        controlVisibility();
        Object[] optionalArgValues = optionalArgs.getValues();
        Object currentDefaultValue = null;
        final String xStr = x.getDataAt(0);
        final String[] names = optionalArgs.getNames();
        for (int i = 0; i < names.length; ++i) {
            final String suppliedArgName = names[i];
            final Object optionalArgValue = optionalArgValues[i];
            if (xStr.equals(suppliedArgName) && optionalArgValue != null) {
                if (RMissingHelper.isMissing(optionalArgValue)) {
                    matchedArgIsMissing.enter();

                    // Fall-through: If the matched value is missing, take the next non-missing
                    for (int j = i + 1; j < optionalArgValues.length; j++) {
                        Object val = optionalArgValues[j];
                        if (!RMissingHelper.isMissing(val)) {
                            return returnNonNull(val);
                        }
                    }

                    // No non-missing value: invisible null
                    return returnNull();
                } else {
                    // Default: Matched name has a value
                    return returnNonNull(optionalArgValue);
                }
            }
            if (suppliedArgName == null) {
                suppliedArgNameIsNull.enter();
                if (currentDefaultValueProfile.profile(currentDefaultValue != null)) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.DUPLICATE_SWITCH_DEFAULT, currentDefaultValue.toString(), optionalArgValue.toString());
                } else {
                    currentDefaultValue = optionalArgValue;
                }
            }
        }
        if (returnValueProfile.profile(currentDefaultValue != null)) {
            return returnNonNull(currentDefaultValue);
        } else {
            return returnNull();
        }
    }

    @Specialization
    protected Object doSwitch(int x, RArgsValuesAndNames optionalArgs) {
        return doSwitchInt(x, optionalArgs);
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
            return returnNull();
        }
        return doSwitchInt((int) objIndex, optionalArgs);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object doSwitch(RMissing x, RMissing optionalArgs) {
        CompilerDirectives.transferToInterpreter();
        throw RError.error(getEncapsulatingSourceSection(), RError.Message.EXPR_MISSING);
    }

    @TruffleBoundary
    private Object doSwitchInt(int index, RArgsValuesAndNames optionalArgs) {
        Object[] optionalArgValues = optionalArgs.getValues();
        if (index >= 1 && index <= optionalArgValues.length) {
            Object value = optionalArgValues[index - 1];
            if (value != null) {
                return returnNonNull(value);
            }
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NO_ALTERNATIVE_IN_SWITCH);
        }
        return returnNull();
    }

    protected boolean isLengthOne(RAbstractStringVector x) {
        return x.getLength() == 1;
    }

    private Object returnNull() {
        switchVisibilityTo(false);
        return RNull.instance;
    }

    private Object returnNonNull(Object value) {
        switchVisibilityTo(true);
        return value;
    }

    private void switchVisibilityTo(boolean isVisibleArg) {
        this.isVisible = isVisibleArg;
        controlVisibility();
    }
}
