/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.RCallNode.ExplicitArgs;
import com.oracle.truffle.r.nodes.function.RFunctionEvalNodesFactory.FunctionInfoFactoryNodeGen;
import com.oracle.truffle.r.nodes.function.RFunctionEvalNodesFactory.FunctionInfoNodeGen;
import com.oracle.truffle.r.nodes.function.opt.ShareObjectNode;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.Closure;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPairListLibrary;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseState;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public class RFunctionEvalNodes {

    private static final String TRYCATCH = "tryCatch";
    private static final String EVALQ = "evalq";

    /**
     * The representation of a function call.
     */
    public static final class FunctionInfo {
        public final RFunction function;
        public final String name;
        public final RPairList argList;
        public final REnvironment env;
        public final boolean noArgs;

        FunctionInfo(RFunction function, String name, RPairList argList, REnvironment env) {
            this.function = function;
            this.name = name;
            this.argList = argList != null ? argList : RDataFactory.createPairList();
            this.env = env;
            this.noArgs = argList == null;
        }

        public RArgsValuesAndNames prepareArguments(MaterializedFrame currentFrame, MaterializedFrame promiseEvalFrame, BranchProfile symbolArgProfile, BranchProfile pairListArgProfile,
                        RPairListLibrary plLib, PromiseHelperNode promiseHelper, ShareObjectNode sharedObjectNode) {
            if (noArgs) {
                return new RArgsValuesAndNames(new Object[0], ArgumentsSignature.empty(0));
            }

            int len = 0;
            for (@SuppressWarnings("unused")
            RPairList item : argList) {
                len++;
            }

            Object[] args = new Object[len];
            Object[] names = new Object[len];
            int i = 0;
            RBuiltinDescriptor rBuiltin = function.getRBuiltin();
            boolean isFieldAccess = rBuiltin != null ? rBuiltin.isFieldAccess() : false;
            RArgsValuesAndNames varArgs = null;
            for (RPairList plt : argList) {
                Object a = plLib.car(plt);

                if (a instanceof RSymbol) {
                    symbolArgProfile.enter();

                    String symName = ((RSymbol) a).getName();

                    if (isFieldAccess && i > 0) {
                        args[i] = symName;
                        isFieldAccess = false;
                    } else {
                        RSyntaxNode lookupSyntaxNode = createLookupNode(a);
                        Closure closure = Closure.createPromiseClosure(wrapArgNode(i, lookupSyntaxNode));
                        if (ArgumentsSignature.VARARG_NAME.equals(symName)) {
                            if (varArgs == null) {
                                RPromise promise = RDataFactory.createPromise(PromiseState.Supplied, closure, promiseEvalFrame);
                                varArgs = (RArgsValuesAndNames) promiseHelper.evaluate(currentFrame, promise);
                            }
                            args[i] = varArgs;
                        } else {
                            args[i] = RDataFactory.createPromise(PromiseState.Supplied, closure, promiseEvalFrame);
                        }

                    }

                } else if (a instanceof RPairList) {
                    pairListArgProfile.enter();
                    RPairList aPL = (RPairList) a;
                    aPL = RDataFactory.createPairList(plLib.car(aPL), plLib.cdr(aPL), plLib.getTag(aPL), SEXPTYPE.LANGSXP);
                    args[i] = createPromise(promiseEvalFrame, aPL, ((RAttributable) a).getAttributes(), i);
                } else {
                    args[i] = sharedObjectNode.execute(a);
                }

                Object ptag = plLib.getTag(plt);
                if (args[i] == varArgs && varArgs != null) {
                    names[i] = varArgs.getSignature();
                } else if (RPairList.isNull(ptag)) {
                    names[i] = null;
                } else if (ptag instanceof RSymbol) {
                    names[i] = ((RSymbol) ptag).getName();
                } else {
                    names[i] = RRuntime.asString(ptag);
                    assert names[i] != null : "unexpected type of tag in RPairList";
                }
                i++;
            }

            Object[] flattenedArgs = flattenArgs(args);
            return new RArgsValuesAndNames(flattenedArgs, ArgumentsSignature.get(flattenNames(names, flattenedArgs.length)));
        }

        @TruffleBoundary
        private static Closure createClosure(int i, RPairList aPL) {
            RSyntaxNode syntaxNode = aPL.createNode();
            Closure closure = Closure.createPromiseClosure(wrapArgNode(i, syntaxNode));
            return closure;
        }

        private static Object[] flattenArgs(Object[] args) {
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

        private static String[] flattenNames(Object[] names, int len) {
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
        private static RNode wrapArgNode(int i, RSyntaxNode syntaxNode) {
            return WrapArgumentNode.create(syntaxNode.asRNode(), i);
        }

        @TruffleBoundary
        private static RPromise createPromise(MaterializedFrame promiseEvalFrame, RPairList aPL, DynamicObject attributes, int i) {
            if (attributes != null) {
                RAttributable.copyAttributes(aPL, attributes);
            }
            Closure closure = createClosure(i, aPL);
            return RDataFactory.createPromise(PromiseState.Supplied, closure, promiseEvalFrame);
        }

        @TruffleBoundary
        private static RSyntaxNode createLookupNode(Object a) {
            RSyntaxNode lookupSyntaxNode = RContext.getASTBuilder().lookup(RSyntaxNode.LAZY_DEPARSE, ((RSymbol) a).getName(), false);
            return lookupSyntaxNode;
        }

    }

    /**
     * Creates a {@code FunctionInfo} instance for a recognized function call. The function can be
     * represented either by a {@link RFunction function} object or by a {@link RSymbol symbol}.
     */
    public abstract static class FunctionInfoFactoryNode extends Node {

        protected static final int CACHE_SIZE = DSLConfig.getCacheSize(100);
        protected static final int SYM_CACHE_SIZE = DSLConfig.getCacheSize(3);

        static FunctionInfoFactoryNode create() {
            return FunctionInfoFactoryNodeGen.create();
        }

        abstract FunctionInfo execute(Object fun, Object argList, REnvironment env);

        @Specialization(limit = "SYM_CACHE_SIZE", guards = {"cachedFunName.equals(funSym.getName())", "env.getFrame().getFrameDescriptor() == cachedFrameDesc"})
        FunctionInfo createFunctionInfoFromSymbolCached(@SuppressWarnings("unused") RSymbol funSym, RPairList argList, REnvironment env,
                        @SuppressWarnings("unused") @Cached("funSym.getName()") String cachedFunName,
                        @SuppressWarnings("unused") @Cached("env.getFrame().getFrameDescriptor()") FrameDescriptor cachedFrameDesc,
                        @Cached("createFunctionLookup(cachedFunName)") ReadVariableNode readFunNode,
                        @Cached("createClassProfile()") ValueProfile frameProfile,
                        @Cached("createClassProfile()") ValueProfile frameAccessProfile,
                        @Cached("create()") BranchProfile ignoredProfile) {
            Object fun = readFunNode.execute(frameProfile.profile(env.getFrame(frameAccessProfile)));
            if (!(fun instanceof RFunction) || ((RFunction) fun).isBuiltin()) {
                // if (true) {
                ignoredProfile.enter();
                return null; // TODO: Try to handle builtins too
            }
            return new FunctionInfo((RFunction) fun, funSym.getName(), argList, env);
        }

        @Specialization(replaces = "createFunctionInfoFromSymbolCached")
        FunctionInfo createFunctionInfoFromSymbolUncached(RSymbol funSym, RPairList argList, REnvironment env,
                        @Cached("createClassProfile()") ValueProfile frameProfile,
                        @Cached("createClassProfile()") ValueProfile frameAccessProfile,
                        @Cached("create()") BranchProfile ignoredProfile) {
            Object fun = ReadVariableNode.lookupFunction(funSym.getName(), frameProfile.profile(env.getFrame(frameAccessProfile)));
            if (!(fun instanceof RFunction) || ((RFunction) fun).isBuiltin()) {
                // if (true) {
                ignoredProfile.enter();
                return null; // TODO: Try to handle builtins too
            }
            return new FunctionInfo((RFunction) fun, funSym.getName(), argList, env);
        }

        @Specialization
        FunctionInfo createFunctionInfo(RFunction fun, @SuppressWarnings("unused") RNull argList, REnvironment env) {
            return new FunctionInfo(fun, fun.getName(), null, env);
        }

        @Specialization
        FunctionInfo createFunctionInfo(RFunction fun, RPairList argList, REnvironment env) {
            return new FunctionInfo(fun, fun.getName(), argList, env);
        }

        @SuppressWarnings("unused")
        @Fallback
        FunctionInfo fallback(Object fun, Object argList, REnvironment env) {
            return null;
        }

    }

    /**
     * Extracts information about a function call in an expression. Currently, two situations are
     * recognized: a simple function call and a function call wrapped in the try-catch envelope.
     */
    public abstract static class FunctionInfoNode extends Node {

        public abstract FunctionInfo execute(RPairList expr, REnvironment env);

        static FunctionInfoNode create() {
            return FunctionInfoNodeGen.create();
        }

        @Specialization(guards = "isSimpleCall(expr, plLib)")
        FunctionInfo handleSimpleCall(RPairList expr, REnvironment env,
                        @Cached("create()") FunctionInfoFactoryNode funInfoFactory,
                        @CachedLibrary(limit = "1") RPairListLibrary plLib) {
            return funInfoFactory.execute(plLib.car(expr), plLib.cdr(expr), env);
        }

        @Specialization(guards = "isTryCatchWrappedCall(expr, plLib)")
        FunctionInfo handleTryCatchWrappedCall(RPairList expr, @SuppressWarnings("unused") REnvironment env,
                        @CachedLibrary(limit = "1") RPairListLibrary plLib,
                        @Cached("create()") BranchProfile branchProfile1,
                        @Cached("create()") BranchProfile branchProfile2,
                        @Cached("create()") BranchProfile branchProfile3,
                        @Cached("create()") BranchProfile branchProfile4,
                        @Cached("create()") FunctionInfoFactoryNode funInfoFactory) {
            Object pp = plLib.cdr(expr);
            pp = plLib.car(pp);
            if (pp instanceof RPairList) {
                branchProfile1.enter();
                Object p = plLib.car(pp);
                if (p instanceof RSymbol && EVALQ.equals(((RSymbol) p).getName())) {
                    branchProfile2.enter();
                    pp = plLib.cdr(pp);
                    if (pp instanceof RPairList) {
                        branchProfile3.enter();
                        REnvironment evalEnv = (REnvironment) plLib.car(plLib.cdr(pp));
                        pp = plLib.car(pp);
                        if (pp instanceof RPairList) {
                            branchProfile4.enter();
                            p = plLib.car(pp);
                            Object argList = plLib.cdr(pp);
                            return funInfoFactory.execute(p, argList instanceof RPairList ? (RPairList) argList : null, evalEnv);
                        }
                    }
                }
            }
            return null;
        }

        @SuppressWarnings("unused")
        @Fallback
        FunctionInfo handleOther(RPairList expr, REnvironment env) {
            return null;
        }

        boolean isSimpleCall(RPairList expr, RPairListLibrary plLib) {
            Object fn = plLib.car(expr);
            return fn instanceof RFunction || (fn instanceof RSymbol && !TRYCATCH.equals(((RSymbol) fn).getName()));
        }

        boolean isTryCatchWrappedCall(RPairList expr, RPairListLibrary plLib) {
            Object p = plLib.car(expr);
            return p instanceof RSymbol && TRYCATCH.equals(((RSymbol) p).getName());
        }

    }

    /**
     * The root node for the call target called by {@code FunctionEvalCallNode}. It evaluates the
     * function call using {@code RCallBaseNode}.
     */
    public static final class FunctionEvalRootNode extends RootNode {

        @Child RCallBaseNode callNode;
        @Child LocalReadVariableNode funReader;

        protected FunctionEvalRootNode(RFrameSlot argsIdentifier, RFrameSlot funIdentifier) {
            super(RContext.getInstance().getLanguage());
            this.callNode = RCallNode.createExplicitCall(argsIdentifier);
            this.funReader = LocalReadVariableNode.create(funIdentifier, false);
            Truffle.getRuntime().createCallTarget(this);
        }

        @Override
        public SourceSection getSourceSection() {
            return RSyntaxNode.INTERNAL;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            MaterializedFrame evalFrame = (MaterializedFrame) frame.getArguments()[0];
            RFunction fun = (RFunction) funReader.execute(evalFrame);
            return callNode.execute(evalFrame, fun);
        }

    }

    /**
     * This is a helper intermediary node that is used by {@code FunctionEvalNode} to insert a new
     * frame on the Truffle stack.
     */
    public static final class FunctionEvalCallNode extends Node {

        private final RFrameSlot argsIdentifier = RFrameSlot.createTemp(true);
        private final RFrameSlot funIdentifier = RFrameSlot.createTemp(true);
        @CompilationFinal private FrameSlot argsFrameSlot;
        @CompilationFinal private FrameSlot funFrameSlot;

        private final FunctionEvalRootNode funEvalRootNode = new FunctionEvalRootNode(argsIdentifier, funIdentifier);
        @Child private DirectCallNode directCallNode = DirectCallNode.create(funEvalRootNode.getCallTarget());

        public Object execute(VirtualFrame evalFrame, RFunction function, RArgsValuesAndNames args, RCaller explicitCaller, Object callerFrame) {
            if (argsFrameSlot == null || funFrameSlot == null) {
                assert funFrameSlot == null;
                CompilerDirectives.transferToInterpreterAndInvalidate();
                synchronized (FrameSlotChangeMonitor.class) {
                    argsFrameSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(evalFrame.getFrameDescriptor(), argsIdentifier, FrameSlotKind.Object);
                    funFrameSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(evalFrame.getFrameDescriptor(), funIdentifier, FrameSlotKind.Object);
                }
            }
            try {
                // the two slots are used to pass the explicit args and the called function to the
                // FunctionEvalRootNode
                FrameSlotChangeMonitor.setObject(evalFrame, argsFrameSlot, new ExplicitArgs(args, explicitCaller, callerFrame));
                FrameSlotChangeMonitor.setObject(evalFrame, funFrameSlot, function);
                return directCallNode.call(evalFrame);
            } finally {
                FrameSlotChangeMonitor.setObject(evalFrame, argsFrameSlot, null);
                FrameSlotChangeMonitor.setObject(evalFrame, funFrameSlot, null);
            }
        }

    }

    public static final class SlowPathFunctionEvalCallNode extends TruffleBoundaryNode {
        @Child private FunctionEvalCallNode slowPathCallNode;

        @TruffleBoundary
        public Object execute(MaterializedFrame evalFrame, Object callerFrame, RCaller caller, RFunction func, RArgsValuesAndNames args) {
            slowPathCallNode = insert(new FunctionEvalCallNode());
            return slowPathCallNode.execute(evalFrame, func, args, caller, callerFrame);
        }
    }

}
