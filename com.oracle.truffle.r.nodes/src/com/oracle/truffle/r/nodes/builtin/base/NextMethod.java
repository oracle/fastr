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
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.RBuiltin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(value = "NextMethod", lastParameterKind = LastParameterKind.VAR_ARGS_SPECIALIZE)
public abstract class NextMethod extends RBuiltinNode {

    private static final Object[] PARAMETER_NAMES = new Object[]{"generic", "object", "..."};
    @Child protected DispatchedCallNode dispatchedCallNode;
    @Child protected ReadVariableNode rvnClass;
    @Child protected ReadVariableNode rvnGeneric;
    protected String lastGenericName;

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getArguments() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @Specialization(order = 0)
    public Object nextMethod(VirtualFrame frame, String genericMethod, @SuppressWarnings("unused") Object obj, Object[] args) {
        controlVisibility();
        final RStringVector type = readType(frame);
        final String genericName = readGenericName(frame, genericMethod);
        if (genericName == null) {
            throw RError.getUnspecifiedGenFunction(getEncapsulatingSourceSection());
        }
        if (dispatchedCallNode == null || !lastGenericName.equals(genericName)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            DispatchedCallNode dcn = DispatchedCallNode.create(genericName, RRuntime.NEXT_METHOD, args);
            dispatchedCallNode = dispatchedCallNode == null ? insert(dcn) : dispatchedCallNode.replace(dcn);
            lastGenericName = genericName;
        }

        return dispatchedCallNode.execute(frame, type);
    }

    @Specialization(order = 10)
    public Object nextMethod(VirtualFrame frame, @SuppressWarnings("unused") RMissing generic, @SuppressWarnings("unused") RMissing obj, @SuppressWarnings("unused") RMissing args) {
        controlVisibility();
        return nextMethod(frame, null, null, new Object[0]);
    }

    @Specialization(order = 11)
    public Object nextMethod(VirtualFrame frame, String generic, Object obj, @SuppressWarnings("unused") RMissing args) {
        controlVisibility();
        return nextMethod(frame, generic, obj, new Object[0]);
    }

    private RStringVector getAlternateClassHr(VirtualFrame frame) {
        RArguments enclArgs = frame.getArguments(RArguments.class);
        if (enclArgs == null || enclArgs.getLength() == 0 || enclArgs.getArgument(0) == null || !(enclArgs.getArgument(0) instanceof RAbstractVector)) {
            throw RError.getObjectNotSpecified(getEncapsulatingSourceSection());
        }
        RAbstractVector enclosingArg = (RAbstractVector) enclArgs.getArgument(0);
        if (!enclosingArg.isObject()) {
            throw RError.getObjectNotSpecified(getEncapsulatingSourceSection());
        }
        return enclosingArg.getClassHierarchy();
    }

    private String readGenericName(VirtualFrame frame, final String genericMethod) {
        if (rvnGeneric == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            rvnGeneric = insert(ReadVariableNode.create(RRuntime.RDotGeneric, false));
        }
        try {
            return rvnGeneric.executeString(frame);
        } catch (UnexpectedResultException e) {
            return genericMethod;
        } catch (RError e) {
            return genericMethod;
        }
    }

    private RStringVector readType(VirtualFrame frame) {
        if (rvnClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            rvnClass = insert(ReadVariableNode.create(RRuntime.RDotClass, false));
        }
        try {
            return rvnClass.executeRStringVector(frame);
        } catch (UnexpectedResultException e) {
            return getAlternateClassHr(frame);
        } catch (RError e) {
            return getAlternateClassHr(frame);
        }
    }

}
