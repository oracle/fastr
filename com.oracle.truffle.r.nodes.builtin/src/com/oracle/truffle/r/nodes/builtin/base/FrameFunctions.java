/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.SUBSTITUTE;

import java.util.ArrayList;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.FrameFunctionsFactory.SysFrameNodeGen;
import com.oracle.truffle.r.nodes.function.ArgumentMatcher;
import com.oracle.truffle.r.nodes.function.CallArgumentsNode;
import com.oracle.truffle.r.nodes.function.GetCallerFrameNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseDeoptimizeFrameNode;
import com.oracle.truffle.r.nodes.function.PromiseNode;
import com.oracle.truffle.r.nodes.function.PromiseNode.VarArgNode;
import com.oracle.truffle.r.nodes.function.PromiseNode.VarArgsPromiseNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.HasSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * sys.R. See
 * <a href="https://stat.ethz.ch/R-manual/R-devel/library/base/html/sys.parent.html">here</a>. N.B.
 * The frame for the sys functions themselves is not counted in the R spec. Frames are numbered 0,
 * 1, .. starting from .GlobalEnv. Non-negative arguments are frame numbers, negative arguments are
 * relative to the current frame.
 */
public class FrameFunctions {

    public static final class FrameHelper extends RBaseNode {

        private final ConditionProfile currentFrameProfile = ConditionProfile.createBinaryProfile();

        /**
         * Determine the frame access mode of a subclass. The rule of thumb is that subclasses that
         * only use the frame internally should not materialize it, i.e., they should use
         * {@link FrameAccess#READ_ONLY} or {@link FrameAccess#READ_WRITE}.
         */
        private final FrameAccess access;

        public FrameHelper(FrameAccess access) {
            this.access = access;
        }

        protected Frame getFrame(VirtualFrame frame, int n) {
            int actualFrame = decodeFrameNumber(RArguments.getCall(frame), n);
            return RInternalError.guaranteeNonNull(getNumberedFrame(frame, actualFrame));
        }

        protected RCaller getCall(RCaller currentCall, int n) {
            int actualFrame = decodeFrameNumber(currentCall, n);
            RCaller call = currentCall;
            while (call != null) {
                while (call.isPromise()) {
                    call = call.getPromiseOriginalCall();
                }
                if (call.getDepth() == actualFrame) {
                    return call;
                }
                call = call.getParent();
            }
            throw RInternalError.shouldNotReachHere();
        }

        /**
         * Handles n > 0 and n < 0 and errors relating to stack depth.
         */
        private int decodeFrameNumber(RCaller currentCall, int n) {
            RCaller call = currentCall;
            call = call.getParent(); // skip the .Internal function
            while (call.isPromise()) {
                call = call.getParent();
            }
            int depth = call.getDepth();
            if (n > 0) {
                if (n > depth) {
                    throw error(RError.Message.NOT_THAT_MANY_FRAMES);
                }
                return n;
            } else {
                if (-n > depth) {
                    throw error(RError.Message.NOT_THAT_MANY_FRAMES);
                }
                return depth + n;
            }
        }

        private static final int ITERATE_LEVELS = 2;

        @ExplodeLoop
        protected Frame getNumberedFrame(VirtualFrame frame, int actualFrame) {
            if (currentFrameProfile.profile(RArguments.getDepth(frame) == actualFrame)) {
                return frame;
            } else {
                if (RArguments.getDepth(frame) - actualFrame <= ITERATE_LEVELS) {
                    Frame current = frame;
                    for (int i = 0; i < ITERATE_LEVELS; i++) {
                        current = current == null ? null : RArguments.getCallerFrame(current);
                        if (current != null && RArguments.getDepth(current) == actualFrame) {
                            return current;
                        }
                    }
                    notifyRCallNodes(actualFrame, RArguments.getCall(frame));
                }
                return Utils.getStackFrame(access, actualFrame);
            }
        }

        @TruffleBoundary
        private static void notifyRCallNodes(int actualFrame, RCaller caller) {
            RCaller currentCaller = caller;
            for (int i = 0; i < ITERATE_LEVELS; i++) {
                if (currentCaller == null || currentCaller.getDepth() <= actualFrame) {
                    break;
                }
                if (currentCaller.isValidCaller() && !currentCaller.isPromise() && currentCaller.getSyntaxNode() instanceof RCallNode) {
                    ((RCallNode) currentCaller.getSyntaxNode()).setNeedsCallerFrame();
                }
                currentCaller = currentCaller.getParent();
            }
        }
    }

    @RBuiltin(name = "sys.call", kind = INTERNAL, parameterNames = {"which"}, behavior = COMPLEX)
    public abstract static class SysCall extends RBuiltinNode.Arg1 {

        @Child private FrameHelper helper = new FrameHelper(FrameAccess.READ_ONLY);

        static {
            Casts casts = new Casts(SysCall.class);
            casts.arg("which").asIntegerVector().findFirst();
        }

        @Specialization
        protected Object sysCall(VirtualFrame frame, int which) {
            /*
             * sys.call preserves provided names but does not create them, unlike match.call.
             */
            return createCall(RArguments.getCall(frame), which);
        }

        @TruffleBoundary
        private Object createCall(RCaller currentCall, int which) {
            RCaller call = helper.getCall(currentCall, which);
            assert !call.isPromise();
            if (call == null || !call.isValidCaller()) {
                return RNull.instance;
            }
            return RContext.getRRuntimeASTAccess().getSyntaxCaller(call);
        }
    }

    /**
     * Generate a call object in which all of the arguments are fully qualified. Unlike
     * {@code sys.call}, named arguments are re-ordered to match the order in the signature. Plus,
     * unlike, {@code sys.call}, the {@code call} argument can be provided by the caller. "..." is a
     * significant complication for two reasons:
     * <ol>
     * <li>If {@code expand.dots} is {@code false} the "..." args are wrapped in a {@code pairlist}
     * </li>
     * <li>One of the args might itself be "..." in which case the values have to be retrieved from
     * the environment associated with caller of the function containing {@code match.call}.</li>
     * </ol>
     * In summary, although the simple cases are indeed simple, there are many possible variants
     * using "..." that make the code a lot more complex that it seems it ought to be.
     */
    @RBuiltin(name = "match.call", kind = INTERNAL, parameterNames = {"definition", "call", "expand.dots", "envir"}, behavior = COMPLEX)
    public abstract static class MatchCall extends RBuiltinNode.Arg4 {

        @Child private FrameHelper helper = new FrameHelper(FrameAccess.READ_ONLY);

        static {
            Casts casts = new Casts(MatchCall.class);
            casts.arg("definition").mustBe(RFunction.class);
            casts.arg("call").mustBe(RLanguage.class);
            casts.arg("expand.dots").asLogicalVector().findFirst();
            casts.arg("envir").mustBe(REnvironment.class, Message.MUST_BE_ENVIRON);
        }

        @Specialization
        protected RLanguage matchCall(RFunction definition, Object callObj, byte expandDotsL, REnvironment env) {
            /*
             * definition==null in the standard (default) case, in which case we get the RFunction
             * from the calling frame
             */
            RLanguage call = checkCall(callObj);
            if (expandDotsL == RRuntime.LOGICAL_NA) {
                throw error(RError.Message.INVALID_ARGUMENT, "expand.dots");
            }
            boolean expandDots = RRuntime.fromLogical(expandDotsL);

            return doMatchCall(env.getFrame(), definition, call, expandDots);
        }

        @TruffleBoundary
        private static RLanguage doMatchCall(MaterializedFrame cframe, RFunction definition, RLanguage call, boolean expandDots) {
            /*
             * We have to ensure that all parameters are named, in the correct order, and deal with
             * "...". This process has a lot in common with MatchArguments, which we use as a
             * starting point
             */
            RCallNode callNode = (RCallNode) RASTUtils.unwrap(call.getRep());
            CallArgumentsNode callArgs = callNode.createArguments(null, false, false);
            ArgumentsSignature inputVarArgSignature = callArgs.containsVarArgsSymbol() ? CallArgumentsNode.getVarargsAndNames(cframe).getSignature() : null;
            RNode[] matchedArgNodes = ArgumentMatcher.matchArguments((RRootNode) definition.getRootNode(), callArgs, inputVarArgSignature, null, null, true).getArguments();
            ArgumentsSignature sig = ((HasSignature) definition.getRootNode()).getSignature();
            // expand any varargs
            ArrayList<RNode> nodes = new ArrayList<>();
            ArrayList<String> names = new ArrayList<>();

            FrameSlot varArgSlot = cframe.getFrameDescriptor().findFrameSlot(ArgumentsSignature.VARARG_NAME);
            RArgsValuesAndNames varArgParameter = varArgSlot == null ? null : (RArgsValuesAndNames) FrameSlotChangeMonitor.getValue(varArgSlot, cframe);

            for (int i = 0; i < sig.getLength(); i++) {
                RNode arg = matchedArgNodes[i];
                arg = checkForVarArgNode(varArgParameter, arg);

                if (isMissing(arg)) {
                    // nothing to do
                } else if (arg instanceof VarArgsPromiseNode) {
                    VarArgsPromiseNode vararg = (VarArgsPromiseNode) arg;

                    ArgumentsSignature varArgSignature = vararg.getSignature();
                    RPromise.Closure[] closures = vararg.getClosures();

                    RNode[] varArgNodes = new RNode[varArgSignature.getLength()];
                    for (int i2 = 0; i2 < varArgNodes.length; i2++) {
                        Closure cl = closures[i2];
                        RNode n = (RNode) RASTUtils.unwrap(cl.getExpr());
                        n = checkForVarArgNode(varArgParameter, n);

                        if (n instanceof PromiseNode) {
                            varArgNodes[i2] = ((PromiseNode) n).getPromiseExpr().asRNode();
                        } else {
                            varArgNodes[i2] = n;
                        }
                    }

                    if (expandDots) {
                        for (int i2 = 0; i2 < varArgNodes.length; i2++) {
                            nodes.add(varArgNodes[i2]);
                            names.add(varArgSignature.getName(i2));
                        }
                    } else {
                        // GnuR appears to create a pairlist rather than a list
                        RPairList head = RDataFactory.createPairList();
                        head.setType(SEXPTYPE.LISTSXP);
                        RPairList pl = head;
                        RPairList prev = null;
                        for (int i2 = 0; i2 < varArgNodes.length; i2++) {
                            RNode n = varArgNodes[i2];
                            Object listValue = RASTUtils.createLanguageElement(n.asRSyntaxNode());
                            pl.setCar(listValue);
                            if (varArgSignature.getName(i2) != null) {
                                pl.setTag(RDataFactory.createSymbolInterned(varArgSignature.getName(i2)));
                            }
                            if (prev != null) {
                                prev.setCdr(pl);
                            }
                            prev = pl;
                            if (i2 != varArgSignature.getLength() - 1) {
                                pl = RDataFactory.createPairList();
                                pl.setType(SEXPTYPE.LISTSXP);
                            }
                        }
                        nodes.add(ConstantNode.create(head));
                        names.add(ArgumentsSignature.VARARG_NAME);
                    }
                } else if (arg instanceof PromiseNode) {
                    nodes.add(((PromiseNode) arg).getPromiseExpr().asRNode());
                    names.add(sig.getName(i));
                } else {
                    nodes.add(arg);
                    names.add(sig.getName(i));
                }
            }
            sig = ArgumentsSignature.get(names.toArray(new String[names.size()]));
            RSyntaxNode[] newArgs = nodes.toArray(new RSyntaxNode[nodes.size()]);

            RSyntaxNode modCallNode = RASTUtils.createCall(callNode.getFunction(), false, sig, newArgs);
            return RDataFactory.createLanguage(modCallNode.asRNode());
        }

        private static RNode checkForVarArgNode(RArgsValuesAndNames varArgParameter, RNode arg) {
            if (arg instanceof VarArgNode) {
                Object argument = varArgParameter.getArgument(((VarArgNode) arg).getIndex());
                if (argument instanceof RPromise) {
                    RNode unwrapped = (RNode) RASTUtils.unwrap(((RPromise) argument).getRep());
                    return unwrapped instanceof ConstantNode ? unwrapped : ReadVariableNode.create(createVarArgName((VarArgNode) arg));
                } else {
                    return ConstantNode.create(argument);
                }
            }
            return arg;
        }

        private static boolean isMissing(RNode node) {
            return ConstantNode.isMissing(node) || isEmptyVarArg(node);
        }

        private static boolean isEmptyVarArg(RNode node) {
            if (node instanceof ConstantNode && ((ConstantNode) node).getValue() instanceof RArgsValuesAndNames) {
                RArgsValuesAndNames rvn = (RArgsValuesAndNames) ((ConstantNode) node).getValue();
                return rvn.getLength() == 0;
            }
            return false;
        }

        private static String createVarArgName(VarArgNode varArgNode) {
            CompilerAsserts.neverPartOfCompilation(); // for string concatenation and interning
            int vn = varArgNode.getIndex() + 1;
            String varArgSymbol = (vn < 10 ? ".." : ".") + vn;
            return varArgSymbol;
        }

        @Specialization
        @SuppressWarnings("unused")
        protected RLanguage matchCall(Object definition, Object call, Object expandDots, Object envir) {
            throw error(RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
        }

        private RLanguage checkCall(Object obj) throws RError {
            Object callObj = obj;
            while (callObj instanceof RExpression) {
                callObj = ((RExpression) callObj).getDataAt(0);
            }
            if (callObj instanceof RLanguage) {
                RLanguage call = (RLanguage) callObj;
                RNode node = (RNode) RASTUtils.unwrap(call.getRep());
                if (node instanceof RCallNode) {
                    return call;
                }
            }
            throw error(RError.Message.INVALID_ARGUMENT, "call");
        }
    }

    @RBuiltin(name = "sys.nframe", kind = INTERNAL, parameterNames = {}, behavior = COMPLEX)
    public abstract static class SysNFrame extends RBuiltinNode.Arg0 {

        private final BranchProfile isPromiseCurrentProfile = BranchProfile.create();
        private final BranchProfile isPromiseResultProfile = BranchProfile.create();

        @Specialization
        protected int sysNFrame(VirtualFrame frame) {
            RCaller call = RArguments.getCall(frame);
            while (call.isPromise()) {
                isPromiseCurrentProfile.enter();
                call = call.getParent();
            }
            call = call.getParent();
            while (call.isPromise()) {
                isPromiseResultProfile.enter();
                call = call.getParent();
            }
            return call.getDepth();
        }
    }

    @RBuiltin(name = "sys.frame", kind = INTERNAL, parameterNames = {"which"}, behavior = COMPLEX)
    public abstract static class SysFrame extends RBuiltinNode.Arg1 {

        @Child private FrameHelper helper = new FrameHelper(FrameAccess.MATERIALIZE);
        @Child private PromiseDeoptimizeFrameNode deoptFrameNode = new PromiseDeoptimizeFrameNode();

        private final ConditionProfile zeroProfile = ConditionProfile.createBinaryProfile();

        public abstract REnvironment executeInt(VirtualFrame frame, int which);

        public static SysFrame create() {
            return SysFrameNodeGen.create();
        }

        static {
            Casts casts = new Casts(SysFrame.class);
            casts.arg("which").asIntegerVector().findFirst();
        }

        @Specialization
        protected REnvironment sysFrame(VirtualFrame frame, int which) {
            REnvironment result;
            if (zeroProfile.profile(which == 0)) {
                result = REnvironment.globalEnv();
            } else {
                Frame callerFrame = helper.getFrame(frame, which);
                result = REnvironment.frameToEnvironment(callerFrame.materialize());
            }

            // Deoptimize every promise which is now in this frame, as it might leave it's stack
            deoptFrameNode.deoptimizeFrame(result.getFrame());
            return result;
        }
    }

    @RBuiltin(name = "sys.frames", kind = INTERNAL, parameterNames = {}, behavior = COMPLEX)
    public abstract static class SysFrames extends RBuiltinNode.Arg0 {

        @Child private FrameHelper helper = new FrameHelper(FrameAccess.MATERIALIZE);
        @Child private PromiseDeoptimizeFrameNode deoptFrameNode = new PromiseDeoptimizeFrameNode();

        @Specialization
        protected Object sysFrames(VirtualFrame frame) {
            int depth = RArguments.getDepth(frame);
            if (depth == 1) {
                return RNull.instance;
            } else {
                RPairList result = RDataFactory.createPairList();
                RPairList next = result;
                for (int i = 1; i < depth; i++) {
                    MaterializedFrame mf = helper.getNumberedFrame(frame, i).materialize();
                    deoptFrameNode.deoptimizeFrame(mf);
                    next.setCar(REnvironment.frameToEnvironment(mf));
                    if (i != depth - 1) {
                        RPairList pl = RDataFactory.createPairList();
                        next.setCdr(pl);
                        next = pl;
                    } else {
                        next.setCdr(RNull.instance);
                    }
                }
                return result;
            }
        }
    }

    @RBuiltin(name = "sys.calls", kind = INTERNAL, parameterNames = {}, behavior = COMPLEX)
    public abstract static class SysCalls extends RBuiltinNode.Arg0 {

        @Specialization
        protected Object sysCalls(VirtualFrame frame) {
            RCaller call = RArguments.getCall(frame);
            while (call.isPromise()) {
                // isPromiseCurrentProfile.enter();
                call = call.getParent();
            }
            call = call.getParent();
            while (call.isPromise()) {
                // isPromiseResultProfile.enter();
                call = call.getParent();
            }
            int depth = call.getDepth();
            if (depth == 0) {
                return RNull.instance;
            } else {
                Object result = Utils.iterateRFrames(FrameAccess.READ_ONLY, new Function<Frame, Object>() {
                    Object result = RNull.instance;

                    @Override
                    public Object apply(Frame f) {
                        RCaller currentCall = RArguments.getCall(f);
                        if (!currentCall.isPromise() && currentCall.getDepth() <= depth) {
                            result = RDataFactory.createPairList(createCall(currentCall), result);
                        }
                        return RArguments.getDepth(f) == 1 ? result : null;
                    }
                });
                return result;
            }
        }

        @TruffleBoundary
        private static Object createCall(RCaller call) {
            assert call != null;
            return RContext.getRRuntimeASTAccess().getSyntaxCaller(call);
        }
    }

    @RBuiltin(name = "sys.parent", kind = INTERNAL, parameterNames = {"n"}, behavior = COMPLEX)
    public abstract static class SysParent extends RBuiltinNode.Arg1 {

        private final BranchProfile nullCallerProfile = BranchProfile.create();
        private final BranchProfile promiseProfile = BranchProfile.create();
        private final BranchProfile nonNullCallerProfile = BranchProfile.create();

        static {
            Casts casts = new Casts(SysParent.class);
            casts.arg("n").asIntegerVector().findFirst();
        }

        @Specialization
        protected int sysParent(VirtualFrame frame, int n) {
            RCaller call = RArguments.getCall(frame);
            for (int i = 0; i < n + 1; i++) {
                call = call.getParent();
                if (call == null) {
                    nullCallerProfile.enter();
                    return 0;
                }
                while (call.isPromise()) {
                    promiseProfile.enter();
                    call = call.getParent();
                }
            }
            nonNullCallerProfile.enter();
            return call.getDepth();
        }
    }

    @RBuiltin(name = "sys.function", kind = INTERNAL, parameterNames = {"which"}, splitCaller = true, alwaysSplit = true, behavior = COMPLEX)
    public abstract static class SysFunction extends RBuiltinNode.Arg1 {

        @Child private FrameHelper helper = new FrameHelper(FrameAccess.READ_ONLY);

        public abstract Object executeObject(VirtualFrame frame, int which);

        static {
            Casts casts = new Casts(SysFunction.class);
            casts.arg("which").asIntegerVector().findFirst();
        }

        @Specialization
        protected Object sysFunction(VirtualFrame frame, int which) {
            // N.B. Despite the spec, n==0 is treated as the current function
            Frame callerFrame = helper.getFrame(frame, which);
            RFunction func = RArguments.getFunction(callerFrame);

            if (func == null) {
                return RNull.instance;
            } else {
                return func;
            }
        }
    }

    @RBuiltin(name = "sys.parents", kind = INTERNAL, parameterNames = {}, behavior = COMPLEX)
    public abstract static class SysParents extends RBuiltinNode.Arg0 {

        @Specialization
        protected RIntVector sysParents(VirtualFrame frame) {
            RCaller call = RArguments.getCall(frame);
            while (call.isPromise()) {
                // isPromiseCurrentProfile.enter();
                call = call.getParent();
            }
            call = call.getParent();
            while (call.isPromise()) {
                // isPromiseResultProfile.enter();
                call = call.getParent();
            }
            int depth = call.getDepth();
            if (depth == 0) {
                return RDataFactory.createEmptyIntVector();
            } else {
                int[] data = Utils.iterateRFrames(FrameAccess.READ_ONLY, new Function<Frame, int[]>() {
                    int[] result = new int[depth];

                    @Override
                    public int[] apply(Frame f) {
                        RCaller currentCall = RArguments.getCall(f);
                        if (!currentCall.isPromise() && currentCall.getDepth() <= depth) {
                            result[currentCall.getDepth() - 1] = currentCall.getParent().getDepth();
                        }
                        return RArguments.getDepth(f) == 1 ? result : null;
                    }
                });
                return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
            }
        }
    }

    /**
     * The environment of the caller of the function that called parent.frame.
     */
    @RBuiltin(name = "parent.frame", kind = SUBSTITUTE, parameterNames = {"n"}, behavior = COMPLEX)
    public abstract static class ParentFrame extends RBuiltinNode.Arg1 {

        @Child private FrameHelper helper = new FrameHelper(FrameAccess.MATERIALIZE);

        private final BranchProfile nullCallerProfile = BranchProfile.create();
        private final BranchProfile promiseProfile = BranchProfile.create();
        private final BranchProfile nonNullCallerProfile = BranchProfile.create();

        public abstract REnvironment execute(VirtualFrame frame, int n);

        static {
            Casts casts = new Casts(ParentFrame.class);
            casts.arg("n").asIntegerVector().findFirst();
        }

        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{1};
        }

        @Specialization(guards = "n == 1")
        protected REnvironment parentFrameDirect(VirtualFrame frame, @SuppressWarnings("unused") int n,
                        @Cached("new()") GetCallerFrameNode getCaller) {
            return REnvironment.frameToEnvironment(getCaller.execute(frame));
        }

        @Specialization(replaces = "parentFrameDirect")
        protected REnvironment parentFrame(VirtualFrame frame, int n) {
            if (n <= 0) {
                throw error(RError.Message.INVALID_VALUE, "n");
            }
            RCaller call = RArguments.getCall(frame);
            while (call.isPromise()) {
                promiseProfile.enter();
                call = call.getParent();
            }
            for (int i = 0; i < n; i++) {
                call = call.getParent();
                if (call == null) {
                    nullCallerProfile.enter();
                    return REnvironment.globalEnv();
                }
                while (call.isPromise()) {
                    promiseProfile.enter();
                    call = call.getParent();
                }
            }
            nonNullCallerProfile.enter();
            // if (RArguments.getDispatchArgs(f) != null && RArguments.getDispatchArgs(f) instanceof
            // S3Args) {
            // /*
            // * Skip the next frame if this frame has dispatch args, and therefore was
            // * called by UseMethod or NextMethod.
            // */
            // parentDepth--;
            // }
            return REnvironment.frameToEnvironment(helper.getNumberedFrame(frame, call.getDepth()).materialize());
        }
    }
}
