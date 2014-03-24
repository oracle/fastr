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
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(value = "UseMethod")
public abstract class UseMethod extends RBuiltinNode {
    /*
     * TODO: If more than two parameters are passed to UseMethod the extra parameters are ignored
     * and a warning is generated.
     */
    private static final Object[] PARAMETER_NAMES = new Object[]{"generic", "object"};
    @Child protected DispatchedCallNode dispatchedCallNode;
    protected String lastGenericName;

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getArguments() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @Specialization
    public Object useMethod(VirtualFrame frame, String generic, RAbstractVector arg) {
        return useMethodHelper(frame, generic, arg.getClassHierarchy());
    }

    /*
     * If only one argument is passed to UseMethod, the first argument of enclosing function is used
     * to resolve the generic.
     */
    @Specialization
    public Object useMethod(VirtualFrame frame, String generic, @SuppressWarnings("unused") RMissing arg) {
        RArguments args = frame.getArguments(RArguments.class);
        if (args == null || args.getLength() == 0 || args.getArgument(0) == null) {
            throw RError.getUnknownFunctionUseMethod(getEncapsulatingSourceSection(), generic, RNull.instance.toString());
        }
        Object enclosingArg = args.getArgument(0);
        if (enclosingArg instanceof Byte) {
            return useMethod(frame, generic, (byte) enclosingArg);
        }
        if (enclosingArg instanceof String) {
            return useMethod(frame, generic, (String) enclosingArg);
        }
        if (enclosingArg instanceof Integer) {
            return useMethod(frame, generic, (int) enclosingArg);
        }
        if (enclosingArg instanceof Double) {
            return useMethod(frame, generic, (double) enclosingArg);
        }
        return useMethod(frame, generic, (RAbstractVector) enclosingArg);
    }

    @Specialization
    public Object useMethod(VirtualFrame frame, String generic, @SuppressWarnings("unused") byte arg) {
        return useMethodHelper(frame, generic, RRuntime.TYPE_LOGICAL);
    }

    @Specialization
    public Object useMethod(VirtualFrame frame, String generic, @SuppressWarnings("unused") String arg) {
        return useMethodHelper(frame, generic, RRuntime.TYPE_CHARACTER);
    }

    @Specialization
    public Object useMethod(VirtualFrame frame, String generic, @SuppressWarnings("unused") int arg) {
        return useMethodHelper(frame, generic, RRuntime.CLASS_INTEGER);
    }

    @Specialization
    public Object useMethod(VirtualFrame frame, String generic, @SuppressWarnings("unused") double arg) {
        return useMethodHelper(frame, generic, RRuntime.CLASS_DOUBLE);
    }

    @Specialization
    public Object useMethod(@SuppressWarnings("unused") VirtualFrame frame, @SuppressWarnings("unused") Object generic, @SuppressWarnings("unused") Object arg) {
        throw RError.getNonStringGeneric(getEncapsulatingSourceSection());
    }

    private Object useMethodHelper(VirtualFrame frame, String generic, String className) {
        return useMethodHelper(frame, generic, RDataFactory.createStringVector(className));
    }

    private Object useMethodHelper(VirtualFrame frame, String generic, String[] classNames) {
        return useMethodHelper(frame, generic, RDataFactory.createStringVector(classNames, true));
    }

    private Object useMethodHelper(VirtualFrame frame, String generic, RStringVector classNames) {
        if (dispatchedCallNode == null || !lastGenericName.equals(generic)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            DispatchedCallNode dcn = DispatchedCallNode.create(generic, RRuntime.USE_METHOD);
            dispatchedCallNode = dispatchedCallNode == null ? insert(dcn) : dispatchedCallNode.replace(dcn);
            lastGenericName = generic;
        }
        throw new ReturnException(dispatchedCallNode.execute(frame, classNames));
    }
}
