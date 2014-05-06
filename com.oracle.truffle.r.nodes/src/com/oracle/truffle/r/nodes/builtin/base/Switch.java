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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.RBuiltin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(value = "switch", lastParameterKind = LastParameterKind.VAR_ARGS_SPECIALIZE)
@NodeField(name = "argNames", type = String[].class)
public abstract class Switch extends RBuiltinNode {
    private static final Object[] PARAMETER_NAMES = new Object[]{"EXPR", "..."};

    @Child protected CastIntegerNode castIntNode;

    public abstract String[] getArgNames();

    private boolean isVisible = true;

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    public boolean getVisibility() {
        return this.isVisible;
    }

    @Specialization(order = 1)
    public Object doSwitch(String x, Object[] optionalArgs) {
        controlVisibility();
        Object currentDefaultValue = null;
        final String[] argNames = this.getArgNames();
        for (int i = 1; i < argNames.length; ++i) {
            final String argName = argNames[i];
            final Object value = optionalArgs[i - 1];
            if (x.equals(argName)) {
                if (value != null) {
                    return returnNonNull(value);
                }
            }
            if (argName == null) {
                if (currentDefaultValue != null) {
                    throw RError.getDuplicateSwitchDefaults(getEncapsulatingSourceSection(), currentDefaultValue.toString(), value.toString());
                }
                currentDefaultValue = value;
            }
        }
        if (currentDefaultValue != null) {
            return returnNonNull(currentDefaultValue);
        } else {
            return returnNull();
        }
    }

    @Specialization(order = 0)
    public Object doSwitch(RStringVector x, Object[] optionalArgs) {
        if (x.getLength() != 1) {
            throw RError.getExprNotLengthOne(getEncapsulatingSourceSection());
        }
        return doSwitch(x.getDataAt(0), optionalArgs);
    }

    @Specialization(order = 2)
    public Object doSwitch(int x, Object[] optionalArgs) {
        return doSwitchInt(x, optionalArgs);
    }

    @Specialization(order = 3)
    public Object doSwitch(VirtualFrame frame, Object x, Object[] optionalArgs) {
        if (castIntNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castIntNode = insert(CastIntegerNodeFactory.create(null, false, false, false));
        }
        Object objIndex = castIntNode.executeCast(frame, x);
        if (!(objIndex instanceof Integer)) {
            return returnNull();
        }
        int index = (Integer) objIndex;
        return doSwitchInt(index, optionalArgs);
    }

    private Object doSwitchInt(int index, Object[] optionalArgs) {
        if (index >= 1 && index <= optionalArgs.length) {
            Object value = optionalArgs[index - 1];
            if (value != null) {
                return returnNonNull(value);
            }
            throw RError.getNoAlertnativeInSwitch(getEncapsulatingSourceSection());
        }
        return returnNull();
    }

    @SuppressWarnings("unused")
    @Specialization(order = 5)
    public Object doSwitch(VirtualFrame frame, RMissing x, RMissing optionalArgs) {
        throw RError.getExprMissing(getEncapsulatingSourceSection());
    }

    private Object returnNull() {
        this.isVisible = false;
        controlVisibility();
        return RNull.instance;
    }

    private Object returnNonNull(Object value) {
        this.isVisible = true;
        controlVisibility();
        return value;
    }
}
