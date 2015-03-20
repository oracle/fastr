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
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.function.DispatchedCallNode.DispatchType;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseCheckHelperNode;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "UseMethod", kind = PRIMITIVE, parameterNames = {"generic", "object"})
public abstract class UseMethod extends RBuiltinNode {

    /*
     * TODO: If more than two parameters are passed to UseMethod the extra parameters are ignored
     * and a warning is generated.
     */

    @Child private DispatchedCallNode dispatchedCallNode;
    @Child protected ClassHierarchyNode classHierarchyNode = ClassHierarchyNodeGen.create(null);
    @Child private PromiseCheckHelperNode promiseCheckHelper;

    @CompilationFinal private String cachedGeneric;
    private final BranchProfile errorProfile = BranchProfile.create();
    private final ConditionProfile argMissingProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile argsValueAndNamesProfile = ConditionProfile.createBinaryProfile();

    @Specialization
    protected Object execute(VirtualFrame frame, String generic, Object arg) {
        controlVisibility();
        if (dispatchedCallNode == null || (cachedGeneric != generic && !cachedGeneric.equals(generic))) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            cachedGeneric = generic;
            DispatchedCallNode newDispatched = DispatchedCallNode.create(generic.intern(), DispatchType.UseMethod, getSuppliedSignature());
            if (dispatchedCallNode == null) {
                dispatchedCallNode = insert(newDispatched);
            } else {
                /*
                 * The generic name may have changed. This is very unlikely, and therefore
                 * implemented very inefficiently. Output a warning in case this really happens.
                 */
                RError.performanceWarning("non-constant generic parameter in UseMethod");
                dispatchedCallNode.replace(newDispatched);
            }
        }

        Object dispatchedObject;
        if (argMissingProfile.profile(arg == RMissing.instance)) {
            // For S3Dispatch, we have to evaluate the the first argument
            dispatchedObject = getEnclosingArg(frame);
        } else {
            dispatchedObject = arg;
        }
        throw new ReturnException(dispatchedCallNode.execute(frame, classHierarchyNode.execute(frame, dispatchedObject)), null);
    }

    /**
     * Get the first (logical) argument in the frame, and handle {@link RPromise}s and
     * {@link RArgsValuesAndNames}.
     */
    private Object getEnclosingArg(VirtualFrame frame) {
        if (RArguments.getArgumentsLength(frame) == 0 || RArguments.getArgument(frame, 0) == null) {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNKNOWN_FUNCTION_USE_METHOD, cachedGeneric, RRuntime.toString(RNull.instance));
        }
        Object enclosingArg = RArguments.getArgument(frame, 0);
        if (argsValueAndNamesProfile.profile(enclosingArg instanceof RArgsValuesAndNames)) {
            // The GnuR "1. argument" might be hidden inside a "..."! Unwrap for proper dispatch
            RArgsValuesAndNames varArgs = (RArgsValuesAndNames) enclosingArg;
            if (varArgs.length() == 0) {
                errorProfile.enter();
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.UNKNOWN_FUNCTION_USE_METHOD, cachedGeneric, RRuntime.toString(RNull.instance));
            }
            enclosingArg = varArgs.getValues()[0];
        }
        if (promiseCheckHelper == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            promiseCheckHelper = insert(new PromiseCheckHelperNode());
        }
        return promiseCheckHelper.checkEvaluate(frame, enclosingArg);
    }
}
