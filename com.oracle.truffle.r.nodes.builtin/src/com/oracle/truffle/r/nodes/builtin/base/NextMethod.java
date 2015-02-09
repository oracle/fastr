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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "NextMethod", kind = SUBSTITUTE, parameterNames = {"generic", "object", "..."})
// TODO INTERNAL
public abstract class NextMethod extends RBuiltinNode {

    @Child private DispatchedCallNode dispatchedCallNode;
    @Child private ReadVariableNode rvnClass;
    @Child private ReadVariableNode rvnGeneric;
    @Child private PromiseHelperNode promiseHelper = new PromiseHelperNode();
    @CompilationFinal private String lastGenericName;

    private final BranchProfile errorProfile = BranchProfile.create();

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RNull.instance), ConstantNode.create(RNull.instance), ConstantNode.create(RMissing.instance)};
    }

    protected Object nextMethod(VirtualFrame frame, String genericMethod, @SuppressWarnings("unused") Object obj, Object[] args, String[] argNames) {
        controlVisibility();
        final RStringVector type = readType(frame);
        final String genericName = genericMethod == null ? readGenericName(frame, genericMethod) : genericMethod;
        if (genericName == null) {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.GEN_FUNCTION_NOT_SPECIFIED);
        }
        if (dispatchedCallNode == null || !lastGenericName.equals(genericName)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            RFunction enclosingFunction = RArguments.getFunction(frame);
            String enclosingFunctionName = null;
            if (!RArguments.hasS3Args(frame)) {
                enclosingFunctionName = enclosingFunction.getRootNode().toString();
            }
            DispatchedCallNode dcn = DispatchedCallNode.create(genericName, enclosingFunctionName, RRuntime.NEXT_METHOD, args, argNames);
            dispatchedCallNode = dispatchedCallNode == null ? insert(dcn) : dispatchedCallNode.replace(dcn);
            lastGenericName = genericName;
        }
        return dispatchedCallNode.execute(frame, type);
    }

    @Specialization
    protected Object nextMethod(VirtualFrame frame, @SuppressWarnings("unused") RNull generic, @SuppressWarnings("unused") RNull obj, RArgsValuesAndNames args) {
        controlVisibility();
        return nextMethod(frame, null, null, args.getValues(), args.getNames());
    }

    @Specialization
    protected Object nextMethod(VirtualFrame frame, String generic, Object obj, RArgsValuesAndNames args) {
        controlVisibility();
        return nextMethod(frame, generic, obj, args.getValues(), args.getNames());
    }

    private RStringVector getAlternateClassHr(VirtualFrame frame) {
        if (RArguments.getArgumentsLength(frame) == 0 || RArguments.getArgument(frame, 0) == null ||
                        (!(RArguments.getArgument(frame, 0) instanceof RAbstractVector) && !(RArguments.getArgument(frame, 0) instanceof RPromise))) {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.OBJECT_NOT_SPECIFIED);
        }
        Object arg = RArguments.getArgument(frame, 0);
        if (arg instanceof RPromise) {
            arg = promiseHelper.evaluate(frame, (RPromise) arg);
        }
        RAbstractContainer enclosingArg = (RAbstractContainer) arg;
        if (!enclosingArg.isObject()) {
            errorProfile.enter();
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
