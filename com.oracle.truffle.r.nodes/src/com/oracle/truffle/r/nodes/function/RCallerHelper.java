/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.function;

import java.util.function.Supplier;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.signature.VarArgsHelper;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.VirtualEvalFrame;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RAttributableNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * It's a helper for constructing {@link RCaller} representations (placed here due to inter-package
 * dependencies).
 */
public final class RCallerHelper {

    private RCallerHelper() {
        // container class
    }

    /**
     * This function can be used to construct a proper RCaller instance whenever a name of the
     * function called by the CallMatcherNode is available (e.g. when CallMatcherNode is used by
     * S3/S4 dispatch), and that can then be used to retrieve correct syntax nodes.
     *
     * @param arguments array with arguments and corresponding names. This method strips any
     *            {@code RMissing} arguments and unrolls all varargs within the arguments array.
     */
    public static Supplier<RSyntaxElement> createFromArguments(RFunction function, RArgsValuesAndNames arguments) {
        return createFromArgumentsInternal(function, arguments);
    }

    public static Supplier<RSyntaxElement> createFromArguments(RFunction function, RArgsValuesAndNames arguments, DynamicObject attributes) {
        return createFromArgumentsInternal(function, arguments, attributes);
    }

    /**
     * @see #createFromArguments(RFunction, RArgsValuesAndNames)
     */
    public static Supplier<RSyntaxElement> createFromArguments(String function, RArgsValuesAndNames arguments) {
        return createFromArgumentsInternal(function, arguments);
    }

    public static Supplier<RSyntaxElement> createFromArguments(String function, RArgsValuesAndNames arguments, DynamicObject attributes) {
        return createFromArgumentsInternal(function, arguments, attributes);
    }

    private static Supplier<RSyntaxElement> createFromArgumentsInternal(final Object function, final RArgsValuesAndNames arguments) {
        return createFromArgumentsInternal(function, arguments, null);
    }

    private static Supplier<RSyntaxElement> createFromArgumentsInternal(final Object function, final RArgsValuesAndNames arguments, DynamicObject attributes) {
        return new Supplier<RSyntaxElement>() {

            RSyntaxElement syntaxNode = null;

            @Override
            public RSyntaxElement get() {
                if (syntaxNode == null) {
                    int length = 0;
                    for (int i = 0; i < arguments.getLength(); i++) {
                        Object arg = arguments.getArgument(i);
                        if (arg instanceof RArgsValuesAndNames) {
                            length += ((RArgsValuesAndNames) arg).getLength();
                        } else if (arguments.getArgument(i) != RMissing.instance) {
                            length++;
                        }
                    }

                    RSyntaxNode[] syntaxArguments = new RSyntaxNode[length];
                    String[] signature = new String[length];
                    int index = 0;
                    for (int i = 0; i < arguments.getLength(); i++) {
                        Object arg = arguments.getArgument(i);
                        if (arg instanceof RArgsValuesAndNames) {
                            RArgsValuesAndNames varargs = (RArgsValuesAndNames) arg;
                            for (int j = 0; j < varargs.getLength(); j++) {
                                syntaxArguments[index] = getArgumentNode(varargs.getArgument(j));
                                signature[index] = varargs.getSignature().getName(j);
                                index++;
                            }
                        } else if (arg != RMissing.instance) {
                            syntaxArguments[index] = getArgumentNode(arg);
                            signature[index] = arguments.getSignature().getName(i);
                            index++;
                        }
                    }
                    Object replacedFunction = function instanceof String ? ReadVariableNode.wrap(RSyntaxNode.LAZY_DEPARSE, ReadVariableNode.createFunctionLookup((String) function)) : function;
                    syntaxNode = RASTUtils.createCall(replacedFunction, true, ArgumentsSignature.get(signature), syntaxArguments);
                    if (attributes != null) {
                        RAttributableNode syntaxNodeAttr = (RAttributableNode) syntaxNode;
                        syntaxNodeAttr.setAttributes(attributes);
                    }
                }
                return syntaxNode;
            }
        };
    }

    private static RSyntaxNode getArgumentNode(Object arg) {
        if (arg instanceof RPromise) {
            RPromise p = (RPromise) arg;
            return p.getRep().asRSyntaxNode();
        } else if (!(arg instanceof RMissing)) {
            return ConstantNode.create(arg);
        }
        return null;
    }

    /**
     * This method calculates the signature of the permuted arguments lazily.
     */
    public static Supplier<RSyntaxElement> createFromArguments(String function, long[] preparePermutation, Object[] suppliedArguments, ArgumentsSignature suppliedSignature) {
        return new Supplier<RSyntaxElement>() {

            RSyntaxElement syntaxNode = null;

            @Override
            public RSyntaxElement get() {
                if (syntaxNode == null) {
                    Object[] values = new Object[preparePermutation.length];
                    String[] names = new String[preparePermutation.length];
                    for (int i = 0; i < values.length; i++) {
                        long source = preparePermutation[i];
                        if (!VarArgsHelper.isVarArgsIndex(source)) {
                            values[i] = suppliedArguments[(int) source];
                            names[i] = suppliedSignature.getName((int) source);
                        } else {
                            int varArgsIdx = VarArgsHelper.extractVarArgsIndex(source);
                            int argsIdx = VarArgsHelper.extractVarArgsArgumentIndex(source);
                            RArgsValuesAndNames varargs = (RArgsValuesAndNames) suppliedArguments[varArgsIdx];
                            values[i] = varargs.getArguments()[argsIdx];
                            names[i] = varargs.getSignature().getName(argsIdx);
                        }
                    }
                    RArgsValuesAndNames arguments = new RArgsValuesAndNames(values, ArgumentsSignature.get(names));
                    syntaxNode = createFromArguments(function, arguments).get();
                }
                return syntaxNode;
            }
        };
    }

    public static RCaller getPromiseCallerForExplicitCaller(RContext ctx, VirtualFrame virtualFrame, MaterializedFrame envFrame, REnvironment env) {
        RCaller currentCall = RArguments.getCall(virtualFrame);
        RCaller originalPromiseCaller = REnvironment.globalEnv(ctx) == env ? RCaller.topLevel : currentCall;
        if (env instanceof REnvironment.Function) {
            originalPromiseCaller = RArguments.getCall(envFrame);
        } else if (env instanceof REnvironment.NewEnv) {
            originalPromiseCaller = RArguments.getCall(envFrame);
            if (!RCaller.isValidCaller(originalPromiseCaller)) {
                RCaller validOrigCaller = tryToFindEnvCaller(envFrame);
                if (validOrigCaller != null) {
                    originalPromiseCaller = validOrigCaller;
                }
            }
        }

        // Note: it is OK that there is actually no frame for the "fakePromiseCaller" since
        // artificial frames for promises are anyway ignored when walking the stack. Even in the
        // case of real promises, there may be not actual frame created for their evaluation:
        // see InlineCacheNode.
        RCaller promiseCaller;
        if (env == REnvironment.globalEnv(ctx)) {
            promiseCaller = RCaller.createForPromise(originalPromiseCaller, currentCall, null);
        } else {
            promiseCaller = RCaller.createForPromise(originalPromiseCaller, env, currentCall, null);
        }
        return promiseCaller;
    }

    /**
     * Try to find the valid original caller stored in the original frame of a VirtualEvalFrame that
     * is the same as envFrame. Note: It's a very slow operation due to the frame iteration.
     */
    @TruffleBoundary
    public static RCaller tryToFindEnvCaller(MaterializedFrame envFrame) {
        RCaller validOrigCaller = Utils.iterateRFrames(FrameAccess.READ_ONLY, (f) -> {
            if (f instanceof VirtualEvalFrame && ((VirtualEvalFrame) f).getOriginalFrame() == envFrame) {
                return RArguments.getCall(f);
            } else {
                return null;
            }
        });
        return validOrigCaller;
    }

    public static RCaller getExplicitCaller(VirtualFrame virtualFrame, String funcName, RFunction func, RArgsValuesAndNames args, RCaller promiseParentCaller, DynamicObject attributes) {
        RCaller currentCall = RArguments.getCall(virtualFrame);
        Supplier<RSyntaxElement> callerSyntax;
        if (funcName != null) {
            callerSyntax = RCallerHelper.createFromArguments(funcName, args, attributes);
        } else {
            callerSyntax = RCallerHelper.createFromArguments(func, args, attributes);
        }
        return RCaller.create(currentCall.getDepth() + 1, promiseParentCaller, callerSyntax);
    }

    public static RCaller getExplicitCaller(VirtualFrame virtualFrame, RPairList expr, RCaller promiseParentCaller) {
        RCaller currentCall = RArguments.getCall(virtualFrame);
        Supplier<RSyntaxElement> callerSyntax = new Supplier<RSyntaxElement>() {

            RSyntaxElement syntaxNode = null;

            @Override
            public RSyntaxElement get() {
                if (syntaxNode == null) {
                    syntaxNode = expr.createNode();
                }
                return syntaxNode;
            }
        };
        return RCaller.create(currentCall.getDepth() + 1, promiseParentCaller, callerSyntax);
    }

    /**
     * If the call leads to actual call via
     * {@link com.oracle.truffle.r.nodes.function.call.CallRFunctionNode}, which creates new frame
     * and new set of arguments for it, then for this new arguments we explicitly provide
     * {@link RCaller} that looks like the function was called from the explicitly given environment
     * (it will be its parent call), but at the same time its depth is one above the do.call
     * function that actually invoked it.
     *
     * @see RCaller
     * @see RArguments
     */
    public static RCaller getExplicitCaller(RContext ctx, VirtualFrame virtualFrame, MaterializedFrame envFrame, REnvironment env, String funcName, RFunction func,
                    RArgsValuesAndNames args, DynamicObject attributes) {
        RCaller fakePromiseCaller = getPromiseCallerForExplicitCaller(ctx, virtualFrame, envFrame, env);
        return getExplicitCaller(virtualFrame, funcName, func, args, fakePromiseCaller, attributes);
    }

}
