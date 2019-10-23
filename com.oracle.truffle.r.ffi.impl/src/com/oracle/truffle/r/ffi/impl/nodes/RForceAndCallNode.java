/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nodes;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.RCallerHelper;
import com.oracle.truffle.r.nodes.function.opt.eval.AbstractCallInfoEvalNode;
import com.oracle.truffle.r.nodes.function.opt.eval.ArgValueSupplierNode;
import com.oracle.truffle.r.nodes.function.opt.eval.CallInfo.ArgumentBuilderState;
import com.oracle.truffle.r.nodes.function.opt.eval.CallInfoEvalRootNode.FastPathDirectCallerNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPairListLibrary;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.env.REnvironment;

public abstract class RForceAndCallNode extends AbstractCallInfoEvalNode {

    public static RForceAndCallNode create() {
        return RForceAndCallNodeGen.create();
    }

    public abstract Object executeObject(Object e, Object f, int n, Object env);

    static final class CachedFunctionCall {
        final WeakReference<RootNode> rootRef;
        final WeakReference<FrameDescriptor> fdRef;

        CachedFunctionCall(RFunction fun, REnvironment env, ValueProfile accessProfile) {
            this.rootRef = new WeakReference<>(fun.getRootNode());
            this.fdRef = new WeakReference<>(env.getFrame(accessProfile).getFrameDescriptor());
        }

        boolean isCompatible(RFunction fun, REnvironment env, ValueProfile accessProfile) {
            return rootRef != null && fdRef != null && this.rootRef.get() == fun.getRootNode() && fdRef.get() == env.getFrame(accessProfile).getFrameDescriptor();
        }
    }

    @Specialization(guards = "cachedFunCall.isCompatible(fun, env, accessProfile)", limit = "CACHE_SIZE")
    @ExplodeLoop
    Object forceAndCallCached(Object e, RFunction fun, int n, REnvironment env,
                    @Cached("createClassProfile()") ValueProfile accessProfile,
                    @SuppressWarnings("unused") @Cached("new(fun, env, accessProfile)") CachedFunctionCall cachedFunCall,
                    @CachedLibrary(limit = "1") RPairListLibrary plLib,
                    @Cached("new()") PromiseHelperNode promiseHelper,
                    @Cached("createArgValueSupplierNodes(MAX_ARITY, true)") ArgValueSupplierNode[] argValueSupplierNodes,
                    @Cached("new()") FastPathDirectCallerNode callNode,
                    @Cached("createBinaryProfile()") ConditionProfile varArgsProfile,
                    @Cached BranchProfile isBuiltinProfile) {
        Object el = plLib.cdr(e);
        List<Object> argValues = new LinkedList<>();
        RBuiltinDescriptor rBuiltin = fun.getRBuiltin();
        final ArgumentBuilderState argBuilderState = new ArgumentBuilderState(rBuiltin != null ? rBuiltin.isFieldAccess() : false);
        MaterializedFrame promiseEvalFrame = env.getFrame(accessProfile);
        for (int i = 0; i < argValueSupplierNodes.length; i++) {
            assert el instanceof RPairList;
            Object arg = plLib.car(el);

            Object argVal = argValueSupplierNodes[i].execute(arg, i, argBuilderState, promiseEvalFrame, promiseEvalFrame, promiseHelper);

            argValues.add(argVal);
            el = plLib.cdr(el);
            if (el == RNull.instance) {
                break;
            }
        }

        final RArgsValuesAndNames argsAndNames;
        if (varArgsProfile.profile(argBuilderState.varArgs == null)) {
            argsAndNames = new RArgsValuesAndNames(argValues.toArray(), ArgumentsSignature.empty(argValues.size()));
        } else {
            argsAndNames = createArgsAndNames(argValues, argBuilderState.varArgs);
        }

        if (!fun.isBuiltin()) {
            isBuiltinProfile.enter();
            flattenFirstArgs(env.getFrame(), n, argsAndNames, promiseHelper);
        }

        RCaller rCaller = RCaller.create(env.getFrame(), RCallerHelper.createFromArguments(fun, argsAndNames));
        return callNode.execute(promiseEvalFrame, fun, argsAndNames, rCaller, promiseEvalFrame);
    }

    @Specialization(replaces = "forceAndCallCached")
    Object forceAndCall(Object e, RFunction fun, int n, REnvironment env,
                    @Cached("createClassProfile()") ValueProfile accessProfile,
                    @CachedLibrary(limit = "1") RPairListLibrary plLib,
                    @Cached("new()") PromiseHelperNode promiseHelper,
                    @Cached("create(false)") ArgValueSupplierNode argValueSupplier,
                    @Cached("createBinaryProfile()") ConditionProfile varArgsProfile,
                    @Cached BranchProfile isBuiltinProfile) {
        Object el = plLib.cdr(e);
        List<Object> argValues = new LinkedList<>();
        int i = 0;
        RBuiltinDescriptor rBuiltin = fun.getRBuiltin();
        final ArgumentBuilderState argBuilderState = new ArgumentBuilderState(rBuiltin != null ? rBuiltin.isFieldAccess() : false);
        MaterializedFrame promiseEvalFrame = env.getFrame(accessProfile);
        while (el != RNull.instance) {
            assert el instanceof RPairList;
            Object arg = plLib.car(el);

            Object argVal = argValueSupplier.execute(arg, i, argBuilderState, promiseEvalFrame, promiseEvalFrame, promiseHelper);

            argValues.add(argVal);
            el = plLib.cdr(el);
            i++;
        }

        final RArgsValuesAndNames argsAndNames;
        if (varArgsProfile.profile(argBuilderState.varArgs == null)) {
            argsAndNames = new RArgsValuesAndNames(argValues.toArray(), ArgumentsSignature.empty(argValues.size()));
        } else {
            argsAndNames = createArgsAndNames(argValues, argBuilderState.varArgs);
        }

        if (!fun.isBuiltin()) {
            isBuiltinProfile.enter();
            flattenFirstArgs(env.getFrame(), n, argsAndNames, promiseHelper);
        }

        RCaller rCaller = RCaller.create(env.getFrame(), RCallerHelper.createFromArguments(fun, argsAndNames));
        return RContext.getEngine().evalFunction(fun, env.getFrame(), rCaller, false, argsAndNames.getSignature(), argsAndNames.getArguments());
    }

    private static RArgsValuesAndNames createArgsAndNames(List<Object> argValues, RArgsValuesAndNames dotArgs) {
        final RArgsValuesAndNames argsAndNames;
        Object[] argValuesEx = new Object[argValues.size() + dotArgs.getLength() - 1];
        String[] argNamesEx = new String[argValues.size() + dotArgs.getLength() - 1];
        System.arraycopy(argValues.toArray(), 0, argValuesEx, 0, argValues.size() - 1);
        System.arraycopy(dotArgs.getArguments(), 0, argValuesEx, argValues.size() - 1, dotArgs.getLength());
        final String[] argNames = dotArgs.getSignature().getNames();
        if (argNames != null) {
            System.arraycopy(argNames, 0, argNamesEx, argValues.size() - 1, dotArgs.getLength());
        }
        argsAndNames = new RArgsValuesAndNames(argValuesEx, ArgumentsSignature.get(argNamesEx));
        return argsAndNames;
    }

    private void flattenFirstArgs(VirtualFrame frame, int n, RArgsValuesAndNames args, PromiseHelperNode promiseHelper) {
        // In GnuR there appears to be no error checks on n > args.length
        if (args.getLength() < n) {
            CompilerDirectives.transferToInterpreter();
            throw RError.nyi(this, "forceAndCall with insufficient arguments");
        }
        for (int i = 0; i < n; i++) {
            Object arg = args.getArgument(i);
            if (arg instanceof RArgsValuesAndNames) {
                CompilerDirectives.transferToInterpreter();
                throw RError.nyi(this, "forceAndCall trying to force varargs");
            }
            if (arg instanceof RPromise) {
                promiseHelper.evaluate(frame, (RPromise) arg);
            }
        }
    }
}
