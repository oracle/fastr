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

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.CallInlineCacheNode;
import com.oracle.truffle.r.nodes.CallInlineCacheNodeGen;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctionsFactory.GetNodeGen;
import com.oracle.truffle.r.nodes.function.ArgumentMatcher;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNodeGen;
import com.oracle.truffle.r.nodes.function.EvaluatedArguments;
import com.oracle.truffle.r.nodes.function.GroupDispatchNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseCheckHelperNode;
import com.oracle.truffle.r.nodes.function.RCallNode.RootCallNode;
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode;
import com.oracle.truffle.r.nodes.function.S3FunctionLookupNode.Result;
import com.oracle.truffle.r.nodes.function.signature.GetCallerFrameNode;
import com.oracle.truffle.r.nodes.function.signature.RArgumentsNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RDispatch;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.InternalRSyntaxNodeChildren;
import com.oracle.truffle.r.runtime.nodes.RNode;

// TODO Implement properly, this is a simple implementation that works when the environment doesn't matter
@RBuiltin(name = "do.call", kind = INTERNAL, parameterNames = {"what", "args", "envir"})
public abstract class DoCall extends RBuiltinNode implements InternalRSyntaxNodeChildren {

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
                CompilerDirectives.transferToInterpreterAndInvalidate();
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
                /* This child is not being used in a syntax context so remove tags */
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
        Object[] callArgs = argsNode.execute(func, caller, callerFrame, RArguments.getDepth(frame) + 1, RArguments.getPromiseFrame(frame), reorderedArgs.getArguments(), reorderedArgs.getSignature(),
                        null);
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
