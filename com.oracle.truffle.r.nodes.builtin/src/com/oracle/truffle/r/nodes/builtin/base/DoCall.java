/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.profiles.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctionsFactory.GetNodeGen;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseCheckHelperNode;
import com.oracle.truffle.r.nodes.function.RCallNode.RootCallNode;
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode.Result;
import com.oracle.truffle.r.nodes.function.signature.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.nodes.*;

// TODO Implement properly, this is a simple implementation that works when the environment doesn't matter
@RBuiltin(name = "do.call", kind = INTERNAL, parameterNames = {"what", "args", "envir"})
public abstract class DoCall extends RBuiltinNode {

    @Child private CallInlineCacheNode callCache = CallInlineCacheNodeGen.create();
    @Child private GetFunctions.Get getNode;
    @Child private GroupDispatchNode groupDispatch;
    @Child private GetCallerFrameNode getCallerFrame;

    @Child private PromiseHelperNode promiseHelper = new PromiseHelperNode();
    @CompilationFinal private boolean needsCallerFrame;

    @Child private S3FunctionLookupNode dispatchLookup;
    @Child private PromiseCheckHelperNode hierarchyPromiseHelper;
    @Child private ClassHierarchyNode classHierarchyNode;
    @Child private RootCallNode internalDispatchCall;

    @Child private RArgumentsNode argsNode = RArgumentsNode.create();

    private final RCaller caller = RDataFactory.createCaller(this);
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();
    private final BranchProfile errorProfile = BranchProfile.create();
    private final BranchProfile containsRLanguageProfile = BranchProfile.create();
    private final BranchProfile containsRSymbolProfile = BranchProfile.create();

    @Specialization
    protected Object doDoCall(VirtualFrame frame, Object what, RList argsAsList, REnvironment env) {
        /*
         * Step 1: handle the variants of "what" (could be done in extra specializations) and assign
         * "func".
         */
        RFunction func;
        if (what instanceof RFunction) {
            func = (RFunction) what;
        } else if (what instanceof String || (what instanceof RAbstractStringVector && ((RAbstractStringVector) what).getLength() == 1)) {
            if (getNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getNode = insert(GetNodeGen.create(new RNode[4], null, null));
            }
            func = (RFunction) getNode.execute(frame, what, env, RType.Function.getName(), RRuntime.LOGICAL_TRUE);
        } else {
            errorProfile.enter();
            throw RError.error(this, RError.Message.MUST_BE_STRING_OR_FUNCTION, "what");
        }

        /*
         * Step 2: To re-create the illusion of a normal call, turn the values in argsAsList into
         * promises.
         */
        Object[] argValues = argsAsList.getDataCopy();
        RStringVector n = argsAsList.getNames(attrProfiles);
        String[] argNames = n == null ? new String[argValues.length] : n.getDataNonShared();
        ArgumentsSignature signature = ArgumentsSignature.get(argNames);
        MaterializedFrame callerFrame = null;
        for (int i = 0; i < argValues.length; i++) {
            Object arg = argValues[i];
            if (arg instanceof RLanguage) {
                containsRLanguageProfile.enter();
                callerFrame = getCallerFrame(frame, callerFrame);
                RLanguage lang = (RLanguage) arg;
                argValues[i] = createArgPromise(callerFrame, RASTUtils.cloneNode((lang.getRep())));
            } else if (arg instanceof RSymbol) {
                containsRSymbolProfile.enter();
                RSymbol symbol = (RSymbol) arg;
                if (symbol.getName().isEmpty()) {
                    argValues[i] = REmpty.instance;
                } else {
                    callerFrame = getCallerFrame(frame, callerFrame);
                    argValues[i] = createArgPromise(callerFrame, RASTUtils.createReadVariableNode(symbol.getName()));
                }
            }
        }

        /* Step 3: Perform the actual evaluation, checking for builtin special cases. */
        return executeCall(frame, func, argValues, signature, callerFrame);
    }

    /* This exists solely for forceAndCall builtin (R3.2.3) */
    @Specialization
    protected Object doDoCall(VirtualFrame frame, RFunction func, RArgsValuesAndNames args, @SuppressWarnings("unused") RNull env) {
        return executeCall(frame, func, args.getArguments(), args.getSignature(), null);
    }

    private Object executeCall(VirtualFrame frame, RFunction funcArg, Object[] argValues, ArgumentsSignature signature, MaterializedFrame callerFrameArg) {
        RFunction func = funcArg;
        MaterializedFrame callerFrame = callerFrameArg;
        RBuiltinDescriptor builtin = func.getRBuiltin();
        if (func.isBuiltin() && builtin.getDispatch() == RDispatch.INTERNAL_GENERIC) {
            if (dispatchLookup == null) {
                dispatchLookup = insert(S3FunctionLookupNode.create(true, false));
                classHierarchyNode = insert(ClassHierarchyNodeGen.create(true, true));
                hierarchyPromiseHelper = insert(new PromiseCheckHelperNode());
            }
            RStringVector type = classHierarchyNode.execute(hierarchyPromiseHelper.checkEvaluate(frame, argValues[0]));
            Result result = dispatchLookup.execute(frame, builtin.getName(), type, null, frame.materialize(), null);
            if (result != null) {
                func = result.function;
            }
        }
        if (func.isBuiltin() && builtin.getGroup() != null) {
            if (groupDispatch == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                groupDispatch = insert(GroupDispatchNode.create(builtin.getName(), null, func, getOriginalCall().getSourceSection()));
            }
            for (int i = 0; i < argValues.length; i++) {
                Object arg = argValues[i];
                if (arg instanceof RPromise) {
                    argValues[i] = promiseHelper.evaluate(frame, (RPromise) arg);
                }
            }
            return groupDispatch.executeDynamic(frame, new RArgsValuesAndNames(argValues, signature), builtin.getName().intern(), builtin.getGroup(), func);
        }
        EvaluatedArguments evaledArgs = EvaluatedArguments.create(argValues, signature);
        EvaluatedArguments reorderedArgs = ArgumentMatcher.matchArgumentsEvaluated(func, evaledArgs, this, false);
        if (func.isBuiltin()) {
            Object[] argArray = reorderedArgs.getArguments();
            for (int i = 0; i < argArray.length; i++) {
                Object arg = argArray[i];
                if (builtin.evaluatesArg(i)) {
                    if (arg instanceof RPromise) {
                        argArray[i] = promiseHelper.evaluate(frame, (RPromise) arg);
                    } else if (arg instanceof RArgsValuesAndNames) {
                        Object[] varArgValues = ((RArgsValuesAndNames) arg).getArguments();
                        for (int j = 0; j < varArgValues.length; j++) {
                            if (varArgValues[j] instanceof RPromise) {
                                varArgValues[j] = promiseHelper.evaluate(frame, (RPromise) varArgValues[j]);
                            }
                        }
                    }
                } else {
                    if (!(arg instanceof RPromise) && arg != RMissing.instance && !(arg instanceof RArgsValuesAndNames)) {
                        callerFrame = getCallerFrame(frame, callerFrame);
                        argArray[i] = createArgPromise(callerFrame, ConstantNode.create(arg));
                    }
                }
            }
        }
        if (!needsCallerFrame && func.containsDispatch()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            needsCallerFrame = true;
        }
        callerFrame = needsCallerFrame ? getCallerFrame(frame, callerFrame) : null;
        Object[] callArgs = argsNode.execute(func, caller, callerFrame, RArguments.getDepth(frame) + 1, reorderedArgs.getArguments(), reorderedArgs.getSignature(), null);
        RArguments.setIsIrregular(callArgs, true);
        return callCache.execute(frame, func.getTarget(), callArgs);

    }

    private MaterializedFrame getCallerFrame(VirtualFrame frame, MaterializedFrame callerFrame) {
        if (getCallerFrame == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getCallerFrame = insert(new GetCallerFrameNode());
        }
        if (callerFrame == null) {
            return getCallerFrame.execute(frame);
        } else {
            return callerFrame;
        }
    }

    @TruffleBoundary
    private static RPromise createArgPromise(MaterializedFrame frame, RNode rep) {
        return RDataFactory.createPromise(RPromise.PromiseType.ARG_SUPPLIED, frame, RPromise.Closure.create(rep));
    }
}
