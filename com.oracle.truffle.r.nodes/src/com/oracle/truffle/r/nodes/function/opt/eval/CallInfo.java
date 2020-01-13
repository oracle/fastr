/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function.opt.eval;

import java.lang.ref.WeakReference;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.WrapArgumentNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.data.Closure;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPairListLibrary;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * A representation of a function call.
 */
public final class CallInfo {

    public enum EvalMode {
        /**
         * Evaluate in the slow path
         */
        SLOW,
        /**
         * Evaluate in the fast path
         */
        FAST
    }

    public final RFunction function;
    public final String name;
    public final RPairList argList;
    public final REnvironment env;
    public final boolean noArgs;
    public final int argsLen;
    public final EvalMode evalMode;

    public static final class CachedCallInfo {

        private final WeakReference<FrameDescriptor> fdRef;
        private final int argsLen;

        CachedCallInfo(FrameDescriptor fd, int argsLen) {
            this.fdRef = new WeakReference<>(fd);
            this.argsLen = argsLen;
        }

        public boolean isCompatible(CallInfo other, ValueProfile otherEnvClassProfile) {
            FrameDescriptor fd = fdRef.get();
            return fd != null && otherEnvClassProfile.profile(other.env.getFrame()).getFrameDescriptor() == fd && other.argsLen == this.argsLen;
        }

    }

    CallInfo(RFunction function, String name, RPairList argList, REnvironment env, RPairListLibrary plLib) {
        this.function = function;
        this.name = name;
        this.argList = argList != null ? argList : RDataFactory.createPairList();
        this.env = env;
        this.noArgs = argList == null;

        int len = 0;
        if (!noArgs) {
            len = plLib.getLength(argList);
        }
        this.argsLen = len;

        this.evalMode = function.isBuiltin() ? EvalMode.SLOW : EvalMode.FAST;
    }

    public CachedCallInfo getCachedCallInfo() {
        return new CachedCallInfo(env.getFrame().getFrameDescriptor(), argsLen);
    }

    @ExplodeLoop
    public RArgsValuesAndNames prepareArgumentsExploded(MaterializedFrame currentFrame, MaterializedFrame promiseEvalFrame, RPairListLibrary plLib,
                    PromiseHelperNode promiseHelper, ArgValueSupplierNode[] argValSupplierNodes) {
        if (noArgs) {
            return new RArgsValuesAndNames(new Object[0], ArgumentsSignature.empty(0));
        }

        Object[] args = new Object[argsLen];
        Object[] names = new Object[argsLen];
        RBuiltinDescriptor rBuiltin = function.getRBuiltin();
        ArgumentBuilderState argBuilderState = new ArgumentBuilderState(rBuiltin != null ? rBuiltin.isFieldAccess() : false);
        Object next = argList;
        for (int i = 0; i < argValSupplierNodes.length && i < argsLen; i++) {
            RPairList curr = (RPairList) next;
            ArgValueSupplierNode argValSupplierNode = argValSupplierNodes[i];
            handleArgument(currentFrame, promiseEvalFrame, plLib, promiseHelper, args, names, argBuilderState, curr, i, argValSupplierNode);
            next = plLib.cdr(curr);
        }

        Object[] flattenedArgs = flattenArgs(args);
        return new RArgsValuesAndNames(flattenedArgs, ArgumentsSignature.get(flattenNames(names, flattenedArgs.length)));
    }

    public RArgsValuesAndNames prepareArguments(MaterializedFrame currentFrame, MaterializedFrame promiseEvalFrame, RPairListLibrary plLib,
                    PromiseHelperNode promiseHelper, ArgValueSupplierNode argValSupplierNode) {
        if (noArgs) {
            return new RArgsValuesAndNames(new Object[0], ArgumentsSignature.empty(0));
        }

        Object[] args = new Object[argsLen];
        Object[] names = new Object[argsLen];
        RBuiltinDescriptor rBuiltin = function.getRBuiltin();
        ArgumentBuilderState argBuilderState = new ArgumentBuilderState(rBuiltin != null ? rBuiltin.isFieldAccess() : false);
        Object next = argList;
        for (int i = 0; i < argsLen; i++) {
            RPairList curr = (RPairList) next;
            handleArgument(currentFrame, promiseEvalFrame, plLib, promiseHelper, args, names, argBuilderState, curr, i, argValSupplierNode);
            next = plLib.cdr(curr);
        }

        Object[] flattenedArgs = flattenArgs(args);
        return new RArgsValuesAndNames(flattenedArgs, ArgumentsSignature.get(flattenNames(names, flattenedArgs.length)));
    }

    private static void handleArgument(MaterializedFrame currentFrame, MaterializedFrame promiseEvalFrame, RPairListLibrary plLib, PromiseHelperNode promiseHelper, Object[] args, Object[] names,
                    ArgumentBuilderState argBuilderState, RPairList plt, int i, ArgValueSupplierNode argBuilderNode) {
        Object a = plLib.car(plt);
        args[i] = argBuilderNode.execute(a, i, argBuilderState, currentFrame, promiseEvalFrame, promiseHelper);

        Object ptag = plLib.getTag(plt);
        if (args[i] == argBuilderState.varArgs && argBuilderState.varArgs != null) {
            names[i] = argBuilderState.varArgs.getSignature();
        } else if (RRuntime.isNull(ptag)) {
            names[i] = null;
        } else if (ptag instanceof RSymbol) {
            names[i] = ((RSymbol) ptag).getName();
        } else {
            names[i] = RRuntime.asString(ptag);
            assert names[i] != null : "unexpected type of tag in RPairList";
        }
    }

    public static final class ArgumentBuilderState {
        public RArgsValuesAndNames varArgs;
        public RPromise varArgsPromise;
        final boolean isFieldAccess;

        public ArgumentBuilderState(boolean isFieldAccess) {
            this.isFieldAccess = isFieldAccess;
        }
    }

    @TruffleBoundary
    private static Closure createClosure(int i, RPairList aPL) {
        RSyntaxNode syntaxNode = aPL.createNode();
        Closure closure = Closure.createPromiseClosure(wrapArgNode(i, syntaxNode));
        return closure;
    }

    public static Object[] flattenArgs(Object[] args) {
        int len = 0;
        for (Object arg : args) {
            if (arg instanceof RArgsValuesAndNames) {
                len += ((RArgsValuesAndNames) arg).getLength();
            } else {
                len++;
            }
        }

        Object[] flattened = new Object[len];
        int i = 0;
        for (Object arg : args) {
            if (arg instanceof RArgsValuesAndNames) {
                RArgsValuesAndNames varArgs = (RArgsValuesAndNames) arg;
                for (Object varArg : varArgs.getArguments()) {
                    flattened[i++] = varArg;
                }
            } else {
                flattened[i++] = arg;
            }
        }

        return flattened;
    }

    public static String[] flattenNames(Object[] names, int len) {
        String[] flattened = new String[len];
        int i = 0;
        for (Object name : names) {
            if (name instanceof ArgumentsSignature) {
                ArgumentsSignature varArgSig = (ArgumentsSignature) name;
                for (int j = 0; j < varArgSig.getLength(); j++) {
                    flattened[i++] = varArgSig.getName(j);
                }
            } else {
                flattened[i++] = (String) name;
            }
        }

        return flattened;
    }

    @TruffleBoundary
    static RNode wrapArgNode(int i, RSyntaxNode syntaxNode) {
        return WrapArgumentNode.create(syntaxNode.asRNode(), i);
    }

    @TruffleBoundary
    static Closure createPromiseClosure(RPairList aPL, DynamicObject attributes, int i) {
        if (attributes != null) {
            RAttributable.copyAttributes(aPL, attributes);
        }
        Closure closure = createClosure(i, aPL);
        return closure;
    }

}
