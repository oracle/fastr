/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RError.Message.ARGUMENT_NOT_ENVIRONMENT;
import static com.oracle.truffle.r.runtime.RError.Message.ARGUMENT_NOT_FUNCTION;
import static com.oracle.truffle.r.runtime.RError.Message.UNKNOWN_OBJECT;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
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
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.ffi.impl.nodes.RfEvalNodeGen.FunctionEvalNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.RfEvalNodeGen.FunctionInfoFactoryNodeGen;
import com.oracle.truffle.r.ffi.impl.nodes.RfEvalNodeGen.FunctionInfoNodeGen;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.RCallBaseNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.function.RCallNode.ExplicitArgs;
import com.oracle.truffle.r.nodes.function.RCallerHelper;
import com.oracle.truffle.r.nodes.function.opt.ShareObjectNode;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.VirtualEvalFrame;
import com.oracle.truffle.r.runtime.builtins.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.Closure;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPairListLibrary;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseState;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.ActiveBinding;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;
import com.oracle.truffle.r.runtime.ffi.RFFIContext;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public abstract class RfEvalNode extends FFIUpCallNode.Arg2 {

    @Child private PromiseHelperNode promiseHelper;
    private final ConditionProfile envIsNullProfile = ConditionProfile.createBinaryProfile();

    protected RfEvalNode() {
    }

    public static RfEvalNode create() {
        return RfEvalNodeGen.create();
    }

    @TruffleBoundary
    private static RCaller createCall(REnvironment env) {
        Frame frame = getCurrentRFrameFastPath();
        if (frame == null) {
            // Is it necessary?
            // Warning: Utils.getActualCurrentFrame causes deopt
            Utils.getActualCurrentFrame();
        }
        final MaterializedFrame envFrame = env.getFrame();
        RCaller originalCaller = RArguments.getCall(envFrame);
        if (!RCaller.isValidCaller(originalCaller) && env instanceof REnvironment.NewEnv) {
            // Try to find the valid original caller stored in the original frame of a
            // VirtualEvalFrame that is the same as envFrame
            RCaller validOrigCaller = Utils.iterateRFrames(FrameAccess.READ_ONLY, (f) -> {
                if (f instanceof VirtualEvalFrame && ((VirtualEvalFrame) f).getOriginalFrame() == envFrame) {
                    return RArguments.getCall(f);
                } else {
                    return null;
                }
            });
            if (validOrigCaller != null) {
                originalCaller = validOrigCaller;
            }
        }
        RCaller currentCaller = RArguments.getCall(frame);
        if (env == REnvironment.globalEnv(RContext.getInstance())) {
            return RCaller.createForPromise(originalCaller, currentCaller);
        } else {
            return RCaller.createForPromise(originalCaller, env, currentCaller);
        }
    }

    @Specialization
    Object handlePromise(RPromise expr, @SuppressWarnings("unused") RNull nulLEnv) {
        return getPromiseHelper().visibleEvaluate(getCurrentRFrame(), expr);
    }

    @Specialization
    Object handlePromise(RPromise expr, REnvironment env, @Cached("createBinaryProfile()") ConditionProfile isEvaluatedProfile) {
        Object value = expr.getRawValue();
        if (isEvaluatedProfile.profile(value != null)) {
            return value;
        }

        return handlePromise(expr, env);
    }

    @TruffleBoundary
    Object handlePromise(RPromise expr, REnvironment env) {
        return getPromiseHelper().visibleEvaluate(env.getFrame(), expr);
    }

    @Specialization
    @TruffleBoundary
    Object handleExpression(RExpression expr, Object envArg) {
        REnvironment env = getEnv(envArg);
        return RContext.getEngine().eval(expr, env, createCall(env));
    }

    /**
     * The representation of a function call.
     */
    static final class FunctionInfo {
        final RFunction function;
        final RPairList argList;
        final REnvironment env;
        final boolean noArgs;

        FunctionInfo(RFunction function, RPairList argList, REnvironment env) {
            this.function = function;
            this.argList = argList != null ? argList : RDataFactory.createPairList();
            this.env = env;
            this.noArgs = argList == null;
        }

        RArgsValuesAndNames prepareArguments(MaterializedFrame promiseEvalFrame, BranchProfile symbolArgProfile, BranchProfile pairListArgProfile, BranchProfile namedArgsProfile,
                        RPairListLibrary plLib) {
            if (noArgs) {
                return new RArgsValuesAndNames(new Object[0], ArgumentsSignature.empty(0));
            }

            int len = 0;
            boolean named = false;
            for (RPairList item : argList) {
                named = named || !item.isNullTag();
                len++;
            }
            Object[] args = new Object[len];
            String[] names = named ? new String[len] : null;
            int i = 0;
            RBuiltinDescriptor rBuiltin = function.getRBuiltin();
            boolean isFieldAccess = rBuiltin != null ? rBuiltin.isFieldAccess() : false;
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
                        Closure closure = Closure.createPromiseClosure(lookupSyntaxNode.asRNode());
                        args[i] = RDataFactory.createPromise(PromiseState.Supplied, closure, promiseEvalFrame);
                    }

                } else if (a instanceof RPairList) {
                    pairListArgProfile.enter();
                    RPairList aPL = (RPairList) a;
                    aPL = RDataFactory.createPairList(plLib.car(aPL), plLib.cdr(aPL), plLib.getTag(aPL), SEXPTYPE.LANGSXP);
                    args[i] = createPromise(promiseEvalFrame, aPL, ((RAttributable) a).getAttributes());
                } else {
                    args[i] = a;
                }

                if (named) {
                    namedArgsProfile.enter();
                    Object ptag = plLib.getTag(plt);
                    if (RPairList.isNull(ptag)) {
                        names[i] = RRuntime.NAMES_ATTR_EMPTY_VALUE;
                    } else if (ptag instanceof RSymbol) {
                        names[i] = ((RSymbol) ptag).getName();
                    } else {
                        names[i] = RRuntime.asString(ptag);
                        assert names[i] != null : "unexpected type of tag in RPairList";
                    }
                }
                i++;
            }

            return new RArgsValuesAndNames(args, named ? ArgumentsSignature.get(names) : ArgumentsSignature.empty(len));
        }

        @TruffleBoundary
        private static RPromise createPromise(MaterializedFrame promiseEvalFrame, RPairList aPL, DynamicObject attributes) {
            if (attributes != null) {
                RAttributable.copyAttributes(aPL, attributes);
            }
            Closure closure = aPL.getClosure();
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
    abstract static class FunctionInfoFactoryNode extends Node {

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
            if ("{".equals(funSym.getName()) || "if".equals(funSym.getName())) {
                ignoredProfile.enter();
                return null; // TODO: Try to handle the block and "if" too
            }
            return new FunctionInfo((RFunction) fun, argList, env);
        }

        @Specialization(replaces = "createFunctionInfoFromSymbolCached")
        FunctionInfo createFunctionInfoFromSymbolUncached(RSymbol funSym, RPairList argList, REnvironment env,
                        @Cached("createClassProfile()") ValueProfile frameProfile,
                        @Cached("createClassProfile()") ValueProfile frameAccessProfile,
                        @Cached("create()") BranchProfile ignoredProfile) {
            Object fun = ReadVariableNode.lookupFunction(funSym.getName(), frameProfile.profile(env.getFrame(frameAccessProfile)));
            if ("{".equals(funSym.getName()) || "if".equals(funSym.getName())) {
                ignoredProfile.enter();
                return null; // TODO: Try to handle the block and "if" too
            }
            return new FunctionInfo((RFunction) fun, argList, env);
        }

        @Specialization
        FunctionInfo createFunctionInfo(RFunction fun, @SuppressWarnings("unused") RNull argList, REnvironment env) {
            return new FunctionInfo(fun, null, env);
        }

        @Specialization
        FunctionInfo createFunctionInfo(RFunction fun, RPairList argList, REnvironment env) {
            return new FunctionInfo(fun, argList, env);
        }

        @SuppressWarnings("unused")
        @Fallback
        FunctionInfo fallback(Object fun, Object argList, REnvironment env) {
            return null;
        }

    }

    private static final String tryCatch = "tryCatch";
    private static final String evalq = "evalq";

    /**
     * Extracts information about a function call in an expression. Currently, two situations are
     * recognized: a simple function call and a function call wrapped in the try-catch envelope.
     */
    abstract static class FunctionInfoNode extends Node {

        abstract FunctionInfo execute(RPairList expr, REnvironment env);

        static FunctionInfoNode create() {
            return FunctionInfoNodeGen.create();
        }

        @Specialization(guards = "isSimpleCall(expr)")
        FunctionInfo handleSimpleCall(RPairList expr, REnvironment env,
                        @Cached("create()") FunctionInfoFactoryNode funInfoFactory) {
            return funInfoFactory.execute(expr.car(), expr.cdr(), env);
        }

        @Specialization(guards = "isTryCatchWrappedCall(expr)")
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
                if (p instanceof RSymbol && evalq.equals(((RSymbol) p).getName())) {
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

        boolean isSimpleCall(RPairList expr) {
            return expr.car() instanceof RFunction;
        }

        boolean isTryCatchWrappedCall(RPairList expr) {
            Object p = expr.car();
            return p instanceof RSymbol && tryCatch.equals(((RSymbol) p).getName());
        }

    }

    /**
     * The root node for the call target called by {@code FunctionEvalCallNode}. It evaluates the
     * function call using {@code RCallBaseNode}.
     */
    static final class FunctionEvalRootNode extends RootNode {

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
    static final class FunctionEvalCallNode extends Node {

        private final RFrameSlot argsIdentifier = RFrameSlot.createTemp(true);
        private final RFrameSlot funIdentifier = RFrameSlot.createTemp(true);
        @CompilationFinal private FrameSlot argsFrameSlot;
        @CompilationFinal private FrameSlot funFrameSlot;

        private final FunctionEvalRootNode funEvalRootNode = new FunctionEvalRootNode(argsIdentifier, funIdentifier);
        @Child private DirectCallNode directCallNode = DirectCallNode.create(funEvalRootNode.getCallTarget());

        public Object execute(VirtualFrame evalFrame, RFunction function, RArgsValuesAndNames args, RCaller explicitCaller, Object callerFrame) {
            if (argsFrameSlot == null) {
                assert funFrameSlot == null;
                CompilerDirectives.transferToInterpreterAndInvalidate();
                argsFrameSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(evalFrame.getFrameDescriptor(), argsIdentifier, FrameSlotKind.Object);
                funFrameSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(evalFrame.getFrameDescriptor(), funIdentifier, FrameSlotKind.Object);
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

    static final class SlowPathFunctionEvalCallNode extends TruffleBoundaryNode {
        @Child private FunctionEvalCallNode slowPathCallNode;

        @TruffleBoundary
        public Object execute(MaterializedFrame evalFrame, Object callerFrame, RCaller caller, RFunction func, RArgsValuesAndNames args) {
            slowPathCallNode = insert(new FunctionEvalCallNode());
            return slowPathCallNode.execute(evalFrame, func, args, caller, callerFrame);
        }
    }

    /**
     * Evaluates the function call defined in {@code FunctionInfo} in the fast path.
     */
    abstract static class FunctionEvalNode extends Node {

        protected static final int CACHE_SIZE = DSLConfig.getCacheSize(100);

        private final ValueProfile frameProfile = ValueProfile.createClassProfile();
        private final ValueProfile frameAccessProfile = ValueProfile.createClassProfile();

        abstract Object execute(Object downcallFrame, FunctionInfo functionInfo);

        @Specialization(limit = "CACHE_SIZE", guards = {"functionInfo.env.getFrame().getFrameDescriptor() == cachedDesc"})
        Object evalFastPath(Object downcallFrame, FunctionInfo functionInfo,
                        @SuppressWarnings("unused") @Cached("functionInfo.env.getFrame().getFrameDescriptor()") FrameDescriptor cachedDesc,
                        @Cached("new()") FunctionEvalCallNode callNode,
                        @CachedLibrary(limit = "1") RPairListLibrary plLib,
                        @Cached("create()") BranchProfile symbolArgProfile,
                        @Cached("create()") BranchProfile pairListArgProfile,
                        @Cached("create()") BranchProfile namedArgsProfile) {
            MaterializedFrame materializedFrame = (MaterializedFrame) downcallFrame;
            MaterializedFrame promiseFrame = frameProfile.profile(functionInfo.env.getFrame(frameAccessProfile)).materialize();
            MaterializedFrame evalFrame = getEvalFrame(materializedFrame, promiseFrame);
            RArgsValuesAndNames args = functionInfo.prepareArguments(evalFrame, symbolArgProfile, pairListArgProfile, namedArgsProfile, plLib);
            RCaller caller = RCallerHelper.getExplicitCaller(materializedFrame, promiseFrame, functionInfo.env, null, functionInfo.function, args);
            RArguments.setCall(evalFrame, caller);

            Object resultValue = callNode.execute(evalFrame, functionInfo.function, args, caller, materializedFrame);
            // setVisibility ???
            return resultValue;
        }

        @Specialization(replaces = "evalFastPath")
        Object evalSlowPath(Object downcallFrame, FunctionInfo functionInfo,
                        @Cached("new()") SlowPathFunctionEvalCallNode slowPathCallNode,
                        @CachedLibrary(limit = "1") RPairListLibrary plLib,
                        @Cached("create()") BranchProfile symbolArgProfile,
                        @Cached("create()") BranchProfile pairListArgProfile,
                        @Cached("create()") BranchProfile namedArgsProfile) {
            MaterializedFrame materializedFrame = (MaterializedFrame) downcallFrame;
            MaterializedFrame promiseFrame = frameProfile.profile(functionInfo.env.getFrame(frameAccessProfile)).materialize();
            MaterializedFrame evalFrame = getEvalFrame(materializedFrame, promiseFrame);
            RArgsValuesAndNames args = functionInfo.prepareArguments(evalFrame, symbolArgProfile, pairListArgProfile, namedArgsProfile, plLib);
            RCaller caller = RCallerHelper.getExplicitCaller(materializedFrame, promiseFrame, functionInfo.env, null, functionInfo.function, args);
            RArguments.setCall(evalFrame, caller);

            Object resultValue = slowPathCallNode.execute(evalFrame, materializedFrame, caller, functionInfo.function, args);
            // setVisibility ???
            return resultValue;
        }

        private static MaterializedFrame getEvalFrame(VirtualFrame currentFrame, MaterializedFrame envFrame) {
            return VirtualEvalFrame.create(envFrame, RArguments.getFunction(currentFrame), currentFrame, null);
        }

    }

    @Child private FunctionEvalNode functionEvalNode;

    /**
     * This specialization attempts to retrieve a function call from the expression and evaluate it
     * in the fast path. Otherwise it falls back into the default slow path expression evaluation.
     * It uses {@code FunctionInfoNode} to retrieve the function call from the expression and
     * {@code FunctionEvalNode} to evaluate the function call in the fast path.
     */
    @Specialization(guards = "expr.isLanguage()")
    Object handleLanguage(RPairList expr, Object envArg,
                    @Cached("create()") BranchProfile noDowncallFrameFunProfile,
                    @Cached("create()") BranchProfile nullFunProfile,
                    @Cached("create()") FunctionInfoNode funInfoNode,
                    @Cached("create()") ShareObjectNode updateRefCountNode) {
        MaterializedFrame downcallFrame = getCurrentRFrameFastPath();
        if (downcallFrame == null) {
            noDowncallFrameFunProfile.enter();
            return handleLanguageDefault(expr, envArg);
        }

        REnvironment env = getEnv(envArg);
        FunctionInfo functionInfo = funInfoNode.execute(expr, env);
        if (functionInfo == null) {
            nullFunProfile.enter();
            return handleLanguageDefault(expr, envArg);
        }

        if (functionEvalNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            functionEvalNode = insert(FunctionEvalNodeGen.create());
        }

        try {
            Object res = functionEvalNode.execute(downcallFrame, functionInfo);
            return updateRefCountNode.execute(res);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    private static MaterializedFrame getCurrentRFrameFastPath() {
        RFFIContext context = RContext.getInstance().getStateRFFI();
        return context.rffiContextState.currentDowncallFrame;
    }

    @TruffleBoundary
    private static MaterializedFrame getCurrentRFrameSlowPath() {
        return Utils.getActualCurrentFrame().materialize();
    }

    private static MaterializedFrame getCurrentRFrame() {
        MaterializedFrame frame = getCurrentRFrameFastPath();
        return frame != null ? frame : getCurrentRFrameSlowPath();
    }

    @TruffleBoundary
    private Object handleLanguageDefault(RPairList expr, Object envArg) {
        REnvironment env = getEnv(envArg);
        return RContext.getEngine().eval(expr, env, createCall(env));
    }

    @Specialization
    @TruffleBoundary
    Object handleSymbol(RSymbol expr, Object envArg,
                    @Cached("createClassProfile()") ValueProfile accessProfile) {
        Object result = ReadVariableNode.lookupAny(expr.getName(), getEnv(envArg).getFrame(accessProfile), false);
        if (result == null) {
            throw RError.error(RError.NO_CALLER, UNKNOWN_OBJECT, expr.getName());
        }
        if (result instanceof ActiveBinding) {
            result = ((ActiveBinding) result).readValue();
        }
        return result;
    }

    @Specialization(guards = "!l.isLanguage()")
    Object handlePairList(RPairList l, Object envArg,
                    @Cached("createBinaryProfile()") ConditionProfile isPromiseProfile,
                    @Cached("createBinaryProfile()") ConditionProfile noArgsProfile) {
        REnvironment env = getEnv(envArg);
        Object car = l.car();
        RFunction f = null;
        if (isPromiseProfile.profile(car instanceof RPromise)) {
            car = getPromiseHelper().visibleEvaluate(null, (RPromise) car);
        }

        if (car instanceof RFunction) {
            f = (RFunction) car;
        } else if (car instanceof RSymbol) {
            f = ReadVariableNode.lookupFunction(((RSymbol) car).getName(), env.getFrame());
        }

        if (f == null) {
            throw RError.error(RError.NO_CALLER, ARGUMENT_NOT_FUNCTION);
        }

        Object args = l.cdr();
        if (noArgsProfile.profile(args == RNull.instance)) {
            return evalFunction(f, env, null);
        } else {
            RList argsList = ((RPairList) args).toRList();
            return evalFunction(f, env, ArgumentsSignature.fromNamesAttribute(argsList.getNames()), argsList.getDataTemp());
        }
    }

    @TruffleBoundary
    private static Object evalFunction(RFunction f, REnvironment env, ArgumentsSignature argsNames, Object... args) {
        return RContext.getEngine().evalFunction(f, env == REnvironment.globalEnv() ? null : env.getFrame(), createCall(env), true, argsNames, args);
    }

    @Fallback
    Object handleOthers(Object expr, Object env) {
        if (env instanceof REnvironment) {
            return expr;
        } else {
            CompilerDirectives.transferToInterpreter();
            throw RError.error(RError.NO_CALLER, ARGUMENT_NOT_ENVIRONMENT);
        }
    }

    private REnvironment getEnv(Object envArg) {
        if (envIsNullProfile.profile(envArg == RNull.instance)) {
            return REnvironment.globalEnv(RContext.getInstance());
        } else if (envArg instanceof REnvironment) {
            REnvironment env = (REnvironment) envArg;
            if (env == REnvironment.emptyEnv()) {
                return RContext.getInstance().stateREnvironment.getEmptyDummy();
            }
            return env;
        }
        CompilerDirectives.transferToInterpreter();
        throw RError.error(RError.NO_CALLER, ARGUMENT_NOT_ENVIRONMENT);
    }

    private PromiseHelperNode getPromiseHelper() {
        if (promiseHelper == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            promiseHelper = insert(new PromiseHelperNode());
        }
        return promiseHelper;
    }
}
