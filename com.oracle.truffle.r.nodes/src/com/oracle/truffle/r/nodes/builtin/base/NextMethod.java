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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RBuiltin.LastParameterKind;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "NextMethod", kind = SUBSTITUTE, lastParameterKind = LastParameterKind.VAR_ARGS_SPECIALIZE)
// TODO INTERNAL
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
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.GEN_FUNCTION_NOT_SPECIFIED);
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
        if (RArguments.getArgumentsLength(frame) == 0 || RArguments.getArgument(frame, 0) == null || !(RArguments.getArgument(frame, 0) instanceof RAbstractVector)) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.OBJECT_NOT_SPECIFIED);
        }
        RAbstractVector enclosingArg = (RAbstractVector) RArguments.getArgument(frame, 0);
        if (!enclosingArg.isObject()) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.OBJECT_NOT_SPECIFIED);
        }
        return enclosingArg.getClassHierarchy();
    }

    private static String readGenericName(VirtualFrame frame, final String genericMethod) {
        final String storedGeneric = RArguments.getS3Generic(frame);
        if (storedGeneric == null || storedGeneric.isEmpty()) {
            return genericMethod;
        }
        return storedGeneric;
    }

    private RStringVector readType(VirtualFrame frame) {
        final RStringVector storedClass = RArguments.getS3Class(frame);
        if (storedClass == null) {
            return getAlternateClassHr(frame);
        }
        return storedClass;
    }

}
