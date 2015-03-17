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
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctionsFactory.GetFactory;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

// TODO Implement properly, this is a simple implementation that works when the environment doesn't matter
@RBuiltin(name = "do.call", kind = INTERNAL, parameterNames = {"name", "args", "env"})
public abstract class DoCall extends RBuiltinNode {

    @Child private CallInlineCacheNode callCache = CallInlineCacheNode.create(3);
    @Child private GetFunctions.Get getNode;

    @Child private PromiseHelperNode promiseHelper = new PromiseHelperNode();
    @CompilationFinal private boolean needsCallerFrame;

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Specialization(guards = "fname.getLength() == 1")
    protected Object doDoCall(VirtualFrame frame, RAbstractStringVector fname, RList argsAsList, REnvironment env) {
        if (getNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getNode = insert(GetFactory.create(new RNode[4], getBuiltin(), getSuppliedSignature()));
        }
        RFunction func = (RFunction) getNode.execute(frame, fname, env, RType.Function.getName(), RRuntime.LOGICAL_TRUE);
        return doDoCall(frame, func, argsAsList, env);
    }

    @Specialization
    protected Object doDoCall(VirtualFrame frame, RFunction func, RList argsAsList, @SuppressWarnings("unused") REnvironment env) {
        Object[] argValues = argsAsList.getDataNonShared();
        RStringVector n = argsAsList.getNames(attrProfiles);
        String[] argNames = n == null ? new String[argValues.length] : n.getDataNonShared();
        ArgumentsSignature signature = ArgumentsSignature.get(argNames);
        EvaluatedArguments evaledArgs = EvaluatedArguments.create(argValues, signature);
        EvaluatedArguments reorderedArgs = ArgumentMatcher.matchArgumentsEvaluated(func, evaledArgs, getEncapsulatingSourceSection(), false);
        if (func.isBuiltin()) {
            RBuiltinRootNode builtinNode = (RBuiltinRootNode) func.getRootNode();
            Object[] argArray = reorderedArgs.getEvaluatedArgs();
            for (int i = 0; i < argArray.length; i++) {
                Object arg = argArray[i];
                if (builtinNode.evaluatesArg(i)) {
                    if (arg instanceof RPromise) {
                        argArray[i] = promiseHelper.evaluate(frame, (RPromise) arg);
                    } else if (arg instanceof RLanguage) {
                        /*
                         * This is necessary for, e.g., do.call("+", list(quote(a), 2)). But is it
                         * always ok to unconditionally evaluate an RLanguage arg? Are there any
                         * builtins that takes RLanguage values? Also not clear what env to evaluate
                         * in. Can't be frame as, e.g., do.call closure itself defines "quote" as a
                         * logical!
                         */
                        argArray[i] = promiseHelper.evaluate(frame, createArgPromise(REnvironment.globalEnv().getFrame(), arg));
                    }
                } else {
                    if (!(arg instanceof RPromise)) {
                        // TODO there are some builtins that take unevaluated ..., e.g. switch
                        if (!(arg instanceof RArgsValuesAndNames)) {
                            argArray[i] = createArgPromise(frame.materialize(), arg);
                        }
                    }
                }
            }
        } else {
            /*
             * Create promises for all arguments. This is very simplistic, it creates a promise for
             * everything, even constants. Why is this important? E.g. "do.call(f, list(quote(y)))".
             * The "quote(y)" has been evaluated with result RSymbol, but if "f" accesses the
             * argument, it must try to resolve "y" and not just return the RSymbol value.
             */
            Object[] argArray = reorderedArgs.getEvaluatedArgs();
            for (int i = 0; i < argArray.length; i++) {
                Object arg = argArray[i];
                if (!(arg instanceof RPromise)) {
                    // TODO RArgsValuesAndNames
                    if (!(arg instanceof RArgsValuesAndNames)) {
                        argArray[i] = createArgPromise(frame.materialize(), arg);
                    }
                }
            }

        }
        if (!needsCallerFrame && func.containsDispatch()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            needsCallerFrame = true;
        }
        MaterializedFrame callerFrame = needsCallerFrame ? frame.materialize() : null;
        Object[] callArgs = RArguments.create(func, callCache.getSourceSection(), callerFrame, RArguments.getDepth(frame) + 1, reorderedArgs.getEvaluatedArgs(), reorderedArgs.getSignature());
        RArguments.setIsIrregular(callArgs, true);
        return callCache.execute(frame, func.getTarget(), callArgs);
    }

    @TruffleBoundary
    private static RPromise createArgPromise(MaterializedFrame frame, Object arg) {
        return RDataFactory.createPromise(RPromise.EvalPolicy.PROMISED, RPromise.PromiseType.ARG_SUPPLIED, frame, RPromise.Closure.create(RASTUtils.createNodeForValue(arg)));
    }

}
