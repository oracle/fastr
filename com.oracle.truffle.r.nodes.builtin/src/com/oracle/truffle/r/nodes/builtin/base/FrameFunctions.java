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

import java.util.ArrayList;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.PromiseEvalFrameDebug;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.ArgumentMatcher;
import com.oracle.truffle.r.nodes.function.CallArgumentsNode;
import com.oracle.truffle.r.nodes.function.GroupDispatchNode;
import com.oracle.truffle.r.nodes.function.MatchedArguments;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode.PromiseDeoptimizeFrameNode;
import com.oracle.truffle.r.nodes.function.PromiseNode;
import com.oracle.truffle.r.nodes.function.PromiseNode.VarArgNode;
import com.oracle.truffle.r.nodes.function.PromiseNode.VarArgsPromiseNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.function.UnrolledVariadicArguments;
import com.oracle.truffle.r.nodes.function.signature.FrameDepthNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RArguments.S3Args;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
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
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.nodes.IdenticalVisitor;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * sys.R. See <a
 * href="https://stat.ethz.ch/R-manual/R-devel/library/base/html/sys.parent.html">here</a>. N.B. The
 * frame for the sys functions themselves is not counted in the R spec. Frames are numbered 0, 1, ..
 * starting from .GlobalEnv. Non-negative arguments are frame numbers, negative arguments are
 * relative to the current frame.
 */
public class FrameFunctions {

    public abstract static class FrameDepthHelper extends RBuiltinNode {
        @Child private FrameDepthNode frameDepthNode;

        protected int getEffectiveDepth(VirtualFrame frame) {
            if (frameDepthNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                frameDepthNode = new FrameDepthNode();
            }
            int depth = frameDepthNode.execute(frame);
            return depth;
        }

        @TruffleBoundary
        private static boolean matchPromise(RCaller call, RSyntaxNode promiseNode) {
            if (call == null) {
                return false;
            }
            RSyntaxNode callNode = RASTUtils.unwrap(call.getRep()).asRSyntaxNode();
            return new IdenticalVisitor().accept(promiseNode, callNode);

        }

    }

    public abstract static class FrameHelper extends FrameDepthHelper {

        private final ConditionProfile currentFrameProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile caller1Profile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile caller2Profile = ConditionProfile.createBinaryProfile();
        protected final BranchProfile errorProfile = BranchProfile.create();

        /**
         * Determine the frame access mode of a subclass. The rule of thumb is that subclasses that
         * only use the frame internally should not materialize it, i.e., they should use
         * {@link FrameAccess#READ_ONLY} or {@link FrameAccess#READ_WRITE}.
         */
        protected abstract FrameAccess frameAccess();

        /**
         * Handles n > 0 and n < 0 and errors relating to stack depth.
         */
        protected Frame getFrame(VirtualFrame frame, int n) {
            int actualFrame;
            int depth;
            if (n > 0) {
                depth = RArguments.getDepth(frame);
                if (n > depth) {
                    errorProfile.enter();
                    throw RError.error(this, RError.Message.NOT_THAT_MANY_FRAMES);
                }
                actualFrame = n;
            } else {
                /*
                 * Negative frame depths require special treatment during promise evaluation. TODO
                 * should previous arm really be n >= 0? Spec says non-negative values are frame
                 * numbers and negative numbers are relative to top frame.
                 */
                depth = getEffectiveDepth(frame);
                actualFrame = depth + n - 1;
            }
            Frame result = getNumberedFrame(frame, actualFrame);
            if (result == null) {
                errorProfile.enter();
                PromiseEvalFrameDebug.dumpStack("getFrame");
                throw RError.error(this, RError.Message.NOT_THAT_MANY_FRAMES);
            }
            return result;
        }

        protected Frame getNumberedFrame(VirtualFrame frame, int actualFrame, boolean skipPromiseEvalFrames) {
            int depth = RArguments.getDepth(frame);
            if (currentFrameProfile.profile(actualFrame == depth)) {
                return frame;
            } else {
                MaterializedFrame caller1 = RArguments.getCallerFrame(frame);
                if (caller1Profile.profile(caller1 != null)) {
                    if (RArguments.getDepth(caller1) == actualFrame) {
                        return caller1;
                    }
                    MaterializedFrame caller2 = RArguments.getCallerFrame(caller1);
                    if (caller2Profile.profile(caller2 != null)) {
                        if (RArguments.getDepth(caller2) == actualFrame) {
                            return caller2;
                        }
                    }
                }
                return Utils.getStackFrame(frameAccess(), actualFrame, skipPromiseEvalFrames);
            }
        }

        protected Frame getNumberedFrame(VirtualFrame frame, int actualFrame) {
            return getNumberedFrame(frame, actualFrame, false);

        }

    }

    @RBuiltin(name = "sys.call", kind = INTERNAL, parameterNames = {"which"})
    public abstract static class SysCall extends FrameHelper {

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.READ_ONLY;
        }

        @Specialization
        protected Object sysCall(VirtualFrame frame, int which) {
            /*
             * sys.call preserves provided names but does not create them, unlike match.call.
             */
            controlVisibility();
            Frame cframe = getFrame(frame, which);
            if (RArguments.getFunction(cframe) == null) {
                return RNull.instance;
            }
            return createCall(cframe);
        }

        @Specialization
        protected Object sysCall(VirtualFrame frame, double which) {
            return sysCall(frame, (int) which);
        }

        @TruffleBoundary
        private static RLanguage createCall(Frame cframe) {
            RCaller caller = RArguments.getCall(cframe);
            assert caller != null;
            return RContext.getRRuntimeASTAccess().getSyntaxCaller(caller);
        }
    }

    /**
     * Generate a call object in which all of the arguments are fully qualified. Unlike
     * {@code sys.call}, named arguments are re-ordered to match the order in the signature. Plus,
     * unlike, {@code sys.call}, the {@code call} argument can be provided by the caller. "..." is a
     * significant complication for two reasons:
     * <ol>
     * <li>If {@code expand.dots} is {@code false} the "..." args are wrapped in a {@code pairlist}</li>
     * <li>One of the args might itself be "..." in which case the values have to be retrieved from
     * the environment associated with caller of the function containing {@code match.call}.</li>
     * </ol>
     * In summary, although the simple cases are indeed simple, there are many possible variants
     * using "..." that make the code a lot more complex that it seems it ought to be.
     */
    @RBuiltin(name = "match.call", kind = INTERNAL, parameterNames = {"definition", "call", "expand.dots", "envir"})
    public abstract static class MatchCall extends FrameHelper {

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.READ_ONLY;
        }

        @Specialization
        protected RLanguage matchCall(RFunction definition, Object callObj, byte expandDotsL, REnvironment env) {
            /*
             * definition==null in the standard (default) case, in which case we get the RFunction
             * from the calling frame
             */
            controlVisibility();
            RLanguage call = checkCall(callObj);
            if (expandDotsL == RRuntime.LOGICAL_NA) {
                throw RError.error(this, RError.Message.INVALID_ARGUMENT, "expand.dots");
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
            UnrolledVariadicArguments executeFlatten = callArgs.executeFlatten(cframe);
            MatchedArguments matchedArgs = ArgumentMatcher.matchArguments(definition, executeFlatten, null, true);
            ArgumentsSignature sig = matchedArgs.getSignature();
            RNode[] matchedArgNodes = matchedArgs.getArguments();
            // expand any varargs
            ArrayList<RNode> nodes = new ArrayList<>();
            ArrayList<String> names = new ArrayList<>();

            FrameSlot varArgSlot = cframe.getFrameDescriptor().findFrameSlot(ArgumentsSignature.VARARG_NAME);
            RArgsValuesAndNames varArgParameter = varArgSlot == null ? null : (RArgsValuesAndNames) cframe.getValue(varArgSlot);

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
                            Object listValue;
                            if (n instanceof ConstantNode) {
                                listValue = ((ConstantNode) n).getValue();
                            } else if (n instanceof ReadVariableNode) {
                                String id = ((ReadVariableNode) n).getIdentifier();
                                assert id == id.intern();
                                listValue = RDataFactory.createSymbol(id);
                            } else if (n instanceof VarArgNode) {
                                listValue = createVarArgSymbol((VarArgNode) n);
                            } else {
                                throw RInternalError.shouldNotReachHere("node: " + n + " at " + i2);
                            }
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

            RSyntaxNode modCallNode = RASTUtils.createCall(callNode.getFunctionNode(), false, sig, newArgs);
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

        private static RSymbol createVarArgSymbol(VarArgNode varArgNode) {
            CompilerAsserts.neverPartOfCompilation(); // for string concatenation and interning
            String varArgSymbol = createVarArgName(varArgNode);
            return RDataFactory.createSymbolInterned(varArgSymbol);
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
            controlVisibility();
            throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
        }

        private RLanguage checkCall(Object obj) throws RError {
            Object callObj = obj;
            while (callObj instanceof RExpression) {
                callObj = ((RExpression) callObj).getDataAt(0);
            }
            if (callObj instanceof RLanguage) {
                RLanguage call = (RLanguage) callObj;
                RNode node = (RNode) RASTUtils.unwrap(call.getRep());
                if (node instanceof RCallNode || node instanceof GroupDispatchNode) {
                    return call;
                }
            }
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "call");
        }
    }

    @RBuiltin(name = "sys.nframe", kind = INTERNAL, parameterNames = {})
    public abstract static class SysNFrame extends FrameDepthHelper {
        @Specialization
        protected int sysNFrame(VirtualFrame frame) {
            controlVisibility();
            return getEffectiveDepth(frame) - 1;
        }
    }

    private abstract static class DeoptHelper extends FrameHelper {
        protected final PromiseDeoptimizeFrameNode deoptFrameNode = new PromiseDeoptimizeFrameNode();

    }

    @RBuiltin(name = "sys.frame", kind = INTERNAL, parameterNames = {"which"})
    public abstract static class SysFrame extends DeoptHelper {

        private final ConditionProfile zeroProfile = ConditionProfile.createBinaryProfile();

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.MATERIALIZE;
        }

        @Specialization
        protected REnvironment sysFrame(VirtualFrame frame, int which) {
            controlVisibility();
            REnvironment result;
            if (zeroProfile.profile(which == 0)) {
                result = REnvironment.globalEnv();
            } else {
                Frame callerFrame = getFrame(frame, which);
                result = REnvironment.frameToEnvironment(callerFrame.materialize());
            }

            // Deoptimize every promise which is now in this frame, as it might leave it's stack
            deoptFrameNode.deoptimizeFrame(result.getFrame());
            return result;
        }

        @Specialization
        protected REnvironment sysFrame(VirtualFrame frame, double which) {
            return sysFrame(frame, (int) which);
        }
    }

    @RBuiltin(name = "sys.frames", kind = INTERNAL, parameterNames = {})
    public abstract static class SysFrames extends DeoptHelper {
        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.MATERIALIZE;
        }

        @Specialization
        protected Object sysFrames(VirtualFrame frame) {
            controlVisibility();
            int depth = getEffectiveDepth(frame);
            if (depth == 1) {
                return RNull.instance;
            } else {
                RPairList result = RDataFactory.createPairList();
                RPairList next = result;
                for (int i = 1; i < depth; i++) {
                    MaterializedFrame mf = getNumberedFrame(frame, i).materialize();
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

    @RBuiltin(name = "sys.calls", kind = INTERNAL, parameterNames = {})
    public abstract static class SysCalls extends FrameHelper {

        @CompilationFinal private boolean includeTop;

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.READ_ONLY;
        }

        @Specialization
        protected Object sysCalls(VirtualFrame frame) {
            controlVisibility();
            int depth = getEffectiveDepth(frame);
            if (depth == 1) {
                return RNull.instance;
            } else {
                RPairList result = RDataFactory.createPairList();
                RPairList next = result;
                int rdepth = includeTop ? depth + 1 : depth;
                for (int i = 1; i < rdepth; i++) {
                    /*
                     * Normally, getNumberedFrame, which calls Utils.getStackFrame, can return a
                     * PromiseEvalFrame. In other use cases this is important but here it is a
                     * problem as the function value is wrong. Further down the stack we will find
                     * the real function that caused the promise frame to come into existence.
                     */
                    Frame f = getNumberedFrame(frame, i, true);
                    next.setCar(SysCall.createCall(f));
                    if (i != rdepth - 1) {
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

        /**
         * For debug use, includes the top frame.
         */
        public void setIncludeTop() {
            includeTop = true;
        }

    }

    @RBuiltin(name = "sys.parent", kind = INTERNAL, parameterNames = {"n"})
    public abstract static class SysParent extends FrameDepthHelper {

        @Specialization
        protected int sysParent(VirtualFrame frame, int n) {
            controlVisibility();
            int p = getEffectiveDepth(frame) - n - 1;
            return p < 0 ? 0 : p;
        }

        @Specialization
        protected int sysParent(VirtualFrame frame, double n) {
            return sysParent(frame, (int) n);
        }
    }

    @RBuiltin(name = "sys.function", kind = INTERNAL, parameterNames = {"which"}, splitCaller = true, alwaysSplit = true)
    public abstract static class SysFunction extends FrameHelper {

        public abstract Object executeObject(VirtualFrame frame, int which);

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.READ_ONLY;
        }

        @Specialization
        protected Object sysFunction(VirtualFrame frame, int which) {
            controlVisibility();
            // N.B. Despite the spec, n==0 is treated as the current function
            Frame callerFrame = getFrame(frame, which);
            RFunction func = RArguments.getFunction(callerFrame);
            if (func == null) {
                return RNull.instance;
            } else {
                return func;
            }
        }

        @Specialization
        protected Object sysFunction(VirtualFrame frame, double which) {
            return sysFunction(frame, (int) which);
        }
    }

    @RBuiltin(name = "sys.parents", kind = INTERNAL, parameterNames = {})
    public abstract static class SysParents extends FrameHelper {

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.READ_ONLY;
        }

        @Specialization
        protected RIntVector sysParents(VirtualFrame frame) {
            controlVisibility();
            int d = getEffectiveDepth(frame) - 1;
            int[] data = new int[d];
            for (int i = 0; i < d; i++) {
                data[i] = i;
            }
            return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR);
        }
    }

    /**
     * The environment of the caller of the function that called parent.frame.
     */
    @RBuiltin(name = "parent.frame", kind = INTERNAL, parameterNames = {"n"})
    public abstract static class ParentFrame extends FrameHelper {

        private final ConditionProfile nullProfile = ConditionProfile.createBinaryProfile();

        public abstract Object execute(VirtualFrame frame, int n);

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.firstIntegerWithError(0, Message.INVALID_VALUE, "n");
        }

        @Override
        protected final FrameAccess frameAccess() {
            return FrameAccess.MATERIALIZE;
        }

        @Specialization
        protected REnvironment parentFrame(VirtualFrame frame, int n) {
            controlVisibility();
            if (n <= 0) {
                errorProfile.enter();
                throw RError.error(this, RError.Message.INVALID_VALUE, "n");
            }
            Frame callerFrame = Utils.iterateRFrames(frameAccess(), new Function<Frame, Frame>() {
                int parentDepth = getEffectiveDepth(frame) - n - 1;

                @Override
                public Frame apply(Frame f) {
                    if (RArguments.getDepth(f) == parentDepth) {
                        return f;
                    }
                    if (RArguments.getDispatchArgs(f) != null && RArguments.getDispatchArgs(f) instanceof S3Args) {
                        /*
                         * Skip the next frame if this frame has dispatch args, and therefore was
                         * called by UseMethod or NextMethod.
                         */
                        parentDepth--;
                    }
                    return null;
                }
            });
            if (nullProfile.profile(callerFrame == null)) {
                return REnvironment.globalEnv();
            } else {
                return REnvironment.frameToEnvironment(callerFrame.materialize());
            }
        }

        @Specialization
        protected REnvironment parentFrame(VirtualFrame frame, double n) {
            return parentFrame(frame, (int) n);
        }
    }
}
