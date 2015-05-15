/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctionsFactory.GetNodeGen;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.function.signature.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

// TODO Implement properly, this is a simple implementation that works when the environment doesn't matter
@RBuiltin(name = "do.call", kind = INTERNAL, parameterNames = {"what", "args", "envir"})
public abstract class DoCall extends RBuiltinNode {

    @Child private CallInlineCacheNode callCache = CallInlineCacheNodeGen.create();
    @Child private GetFunctions.Get getNode;
    @Child private GroupDispatchNode groupDispatch;
    @Child private GetCallerFrameNode getCallerFrame;

    @Child private PromiseHelperNode promiseHelper = new PromiseHelperNode();
    @CompilationFinal private boolean needsCallerFrame;

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();
    private final BranchProfile errorProfile = BranchProfile.create();
    private final BranchProfile containsRLanguageProfile = BranchProfile.create();
    private final BranchProfile containsRSymbolProfile = BranchProfile.create();

    @Specialization
    protected Object doDoCall(VirtualFrame frame, Object what, RList argsAsList, REnvironment env) {
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
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.MUST_BE_STRING_OR_FUNCTION, "what");
        }

        Object[] argValues = argsAsList.getDataCopy();
        RStringVector n = argsAsList.getNames(attrProfiles);
        String[] argNames = n == null ? new String[argValues.length] : n.getDataNonShared();
        ArgumentsSignature signature = ArgumentsSignature.get(argNames);
        RBuiltinDescriptor builtin = func.getRBuiltin();
        MaterializedFrame callerFrame = null;
        for (int i = 0; i < argValues.length; i++) {
            Object arg = argValues[i];
            if (arg instanceof RLanguage) {
                containsRLanguageProfile.enter();
                callerFrame = getCallerFrame(frame, callerFrame);
                RLanguage lang = (RLanguage) arg;
                argValues[i] = createArgPromise(callerFrame, NodeUtil.cloneNode(((RNode) lang.getRep())));
            } else if (arg instanceof RSymbol) {
                containsRSymbolProfile.enter();
                RSymbol symbol = (RSymbol) arg;
                if (symbol.getName().isEmpty()) {
                    argValues[i] = RMissing.instance;
                } else {
                    callerFrame = getCallerFrame(frame, callerFrame);
                    argValues[i] = createArgPromise(callerFrame, RASTUtils.createReadVariableNode(symbol.getName()));
                }
            }
        }
        if (func.isBuiltin()) {
            if (builtin.getGroup() != null) {
                if (groupDispatch == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    groupDispatch = insert(GroupDispatchNode.create(builtin.getName(), null, func, getSourceSection()));
                }
                for (int i = 0; i < argValues.length; i++) {
                    Object arg = argValues[i];
                    if (arg instanceof RPromise) {
                        argValues[i] = promiseHelper.evaluate(frame, (RPromise) arg);
                    }
                }
                return groupDispatch.executeDynamic(frame, new RArgsValuesAndNames(argValues, signature), builtin.getName(), builtin.getGroup(), func);
            }
        }
        EvaluatedArguments evaledArgs = EvaluatedArguments.create(argValues, signature);
        EvaluatedArguments reorderedArgs = ArgumentMatcher.matchArgumentsEvaluated(func, evaledArgs, getEncapsulatingSourceSection(), false);
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
        Object[] callArgs = RArguments.create(RArguments.getContext(frame), func, callCache.getSourceSection(), callerFrame, RArguments.getDepth(frame) + 1, reorderedArgs.getArguments(),
                        reorderedArgs.getSignature());
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
