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
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.function.DispatchedCallNode.DispatchType;
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
    @CompilationFinal private String cachedGeneric;

    private final BranchProfile errorProfile = BranchProfile.create();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RNull.instance, RNull.instance, RArgsValuesAndNames.EMPTY};
    }

    protected Object nextMethod(VirtualFrame frame, String generic, @SuppressWarnings("unused") Object obj, Object[] args, ArgumentsSignature signature) {
        controlVisibility();
        RStringVector type = readType(frame);

        if (dispatchedCallNode == null || (cachedGeneric != generic && !cachedGeneric.equals(generic))) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            cachedGeneric = generic;
            RFunction enclosingFunction = RArguments.getFunction(frame);
            String enclosingFunctionName = null;
            if (!RArguments.hasS3Args(frame)) {
                enclosingFunctionName = enclosingFunction.getRootNode().toString();
            }
            DispatchedCallNode newDispatched = DispatchedCallNode.create(generic.intern(), enclosingFunctionName, DispatchType.NextMethod, args, signature);
            if (dispatchedCallNode == null) {
                dispatchedCallNode = insert(newDispatched);
            } else {
                /*
                 * The generic name may have changed. This is very unlikely, and therefore
                 * implemented very inefficiently. Output a warning in case this really happens.
                 */
                RError.performanceWarning("non-constant generic parameter in NextMethod");
                dispatchedCallNode.replace(newDispatched);
            }
        }
        return dispatchedCallNode.execute(frame, type);
    }

    @Specialization
    protected Object nextMethod(VirtualFrame frame, @SuppressWarnings("unused") RNull generic, Object obj, RArgsValuesAndNames args) {
        controlVisibility();
        String genericName = RArguments.getS3Generic(frame);
        if (genericName == null || genericName.isEmpty()) {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.GEN_FUNCTION_NOT_SPECIFIED);
        }
        return nextMethod(frame, genericName, obj, args.getValues(), args.getSignature());
    }

    @Specialization
    protected Object nextMethod(VirtualFrame frame, String generic, Object obj, RArgsValuesAndNames args) {
        controlVisibility();
        return nextMethod(frame, generic, obj, args.getValues(), args.getSignature());
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
        if (!enclosingArg.isObject(attrProfiles)) {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.OBJECT_NOT_SPECIFIED);
        }
        return enclosingArg.getClassHierarchy();
    }

    private RStringVector readType(VirtualFrame frame) {
        final RStringVector storedClass = RArguments.getS3Class(frame);
        if (storedClass == null) {
            return getAlternateClassHr(frame);
        }
        return storedClass;
    }
}
