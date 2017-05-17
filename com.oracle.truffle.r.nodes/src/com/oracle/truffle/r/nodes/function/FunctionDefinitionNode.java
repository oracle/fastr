/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.NodeUtil.NodeCountFilter;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.InlineCacheNode;
import com.oracle.truffle.r.nodes.RASTBuilder;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.access.FrameSlotNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinFactory;
import com.oracle.truffle.r.nodes.control.BreakException;
import com.oracle.truffle.r.nodes.control.NextException;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.ExitException;
import com.oracle.truffle.r.runtime.JumpToTopLevelException;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RArguments.DispatchArgs;
import com.oracle.truffle.r.runtime.RArguments.S3Args;
import com.oracle.truffle.r.runtime.RArguments.S4Args;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RErrorHandling;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.Utils.DebugExitException;
import com.oracle.truffle.r.runtime.builtins.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxFunction;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxVisitor;

public final class FunctionDefinitionNode extends RRootNode implements RSyntaxNode, RSyntaxFunction {

    private final FormalArguments formalArguments;

    @Child private RNode body;

    /**
     * This exists for debugging purposes. It is set initially when the function is defined to
     * either:
     * <ul>
     * <li>The name of the variable that the function definition is assigned to, e.g,
     * {@code f <- function}.
     * <li>The first several characters of the function definition for anonymous functions.
     * </ul>
     * It can be updated later by calling {@link #setName}, which is useful for functions lazily
     * loaded from packages, where at the point of definition any assignee variable is unknown.
     */
    private String name;
    private SourceSection sourceSectionR;
    private final SourceSection[] argSourceSections;

    @Child private RNode saveArguments;
    @Child private FrameSlotNode onExitSlot;
    @Child private InlineCacheNode onExitExpressionCache;
    private final ConditionProfile onExitProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile resetArgs = BranchProfile.create();
    private final BranchProfile normalExit = BranchProfile.create();
    private final BranchProfile breakProfile = BranchProfile.create();
    private final BranchProfile nextProfile = BranchProfile.create();

    @Child private SetVisibilityNode visibility = SetVisibilityNode.create();

    @Child private PostProcessArgumentsNode argPostProcess;

    @Child private SetupS3ArgsNode setupS3Args;
    @Child private SetupS4ArgsNode setupS4Args;

    private final boolean needsSplitting;

    @CompilationFinal private boolean containsDispatch;

    private final Assumption noHandlerStackSlot = Truffle.getRuntime().createAssumption();
    private final Assumption noRestartStackSlot = Truffle.getRuntime().createAssumption();
    @CompilationFinal private FrameSlot handlerStackSlot;
    @CompilationFinal private FrameSlot restartStackSlot;

    /**
     * Profiling for catching {@link ReturnException}s.
     */
    private final ConditionProfile returnTopLevelProfile = ConditionProfile.createBinaryProfile();

    public static FunctionDefinitionNode create(SourceSection src, FrameDescriptor frameDesc, SourceSection[] argSourceSections, SaveArgumentsNode saveArguments, RSyntaxNode body,
                    FormalArguments formals, String name, PostProcessArgumentsNode argPostProcess) {
        return new FunctionDefinitionNode(src, frameDesc, argSourceSections, saveArguments, body, formals, name, argPostProcess);
    }

    private FunctionDefinitionNode(SourceSection src, FrameDescriptor frameDesc, SourceSection[] argSourceSections, RNode saveArguments, RSyntaxNode body, FormalArguments formals,
                    String name, PostProcessArgumentsNode argPostProcess) {
        super(frameDesc, RASTBuilder.createFunctionFastPath(body, formals.getSignature()));
        this.formalArguments = formals;
        this.argSourceSections = argSourceSections;
        assert FrameSlotChangeMonitor.isValidFrameDescriptor(frameDesc);
        assert src != null;
        this.sourceSectionR = src;
        this.saveArguments = saveArguments;
        this.body = body.asRNode();
        this.name = name;
        this.onExitSlot = FrameSlotNode.createInitialized(frameDesc, RFrameSlot.OnExit, false);
        this.needsSplitting = needsAnyBuiltinSplitting();
        this.containsDispatch = containsAnyDispatch(body);
        this.argPostProcess = argPostProcess;
    }

    @Override
    public FormalArguments getFormalArguments() {
        return formalArguments;
    }

    @Override
    public ArgumentsSignature getSignature() {
        return formalArguments.getSignature();
    }

    @Override
    public RootCallTarget duplicateWithNewFrameDescriptor() {
        RCodeBuilder<RSyntaxNode> builder = RContext.getASTBuilder();

        List<RCodeBuilder.Argument<RSyntaxNode>> args = new ArrayList<>();
        for (int i = 0; i < getFormalArguments().getLength(); i++) {
            RNode value = getFormalArguments().getArgument(i);
            SourceSection source = argSourceSections == null ? getSourceSection() : argSourceSections[i];
            args.add(RCodeBuilder.argument(source, getFormalArguments().getSignature().getName(i), value == null ? null : builder.process(value.asRSyntaxNode())));
        }
        RootCallTarget callTarget = RContext.getASTBuilder().rootFunction(getSourceSection(), args, builder.process(getBody()), name);
        return callTarget;
    }

    private static boolean containsAnyDispatch(RSyntaxNode body) {
        NodeCountFilter dispatchingMethodsFilter = node -> {
            if (node instanceof ReadVariableNode) {
                ReadVariableNode rvn = (ReadVariableNode) node;
                String identifier = rvn.getIdentifier();
                // TODO: can we also do this for S4 new?
                return rvn.getMode() == RType.Function && ("UseMethod".equals(identifier) || "standardGeneric".equals(identifier));
            }
            return false;
        };
        return NodeUtil.countNodes(body.asRNode(), dispatchingMethodsFilter) > 0;
    }

    @Override
    public boolean containsDispatch() {
        return containsDispatch;
    }

    @Override
    public void setContainsDispatch(boolean containsDispatch) {
        this.containsDispatch = containsDispatch;
    }

    private boolean needsAnyBuiltinSplitting() {
        RSyntaxVisitor<Boolean> visitor = new RSyntaxVisitor<Boolean>() {

            @Override
            protected Boolean visit(RSyntaxCall element) {
                RSyntaxElement lhs = element.getSyntaxLHS();
                RSyntaxElement[] arguments = element.getSyntaxArguments();
                if (lhs instanceof RSyntaxLookup) {
                    String function = ((RSyntaxLookup) lhs).getIdentifier();
                    /*
                     * TODO This is a hack to make sapply split lapply. We need to find better ways
                     * to do this. If a function uses lapply anywhere as name then it gets split.
                     * This could get exploited.
                     */
                    RBuiltinDescriptor directBuiltin = RContext.lookupBuiltinDescriptor(function);
                    if (directBuiltin != null && directBuiltin.isSplitCaller()) {
                        return true;
                    }
                }
                for (RSyntaxElement arg : arguments) {
                    if (arg != null && accept(arg)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            protected Boolean visit(RSyntaxConstant element) {
                return false;
            }

            @Override
            protected Boolean visit(RSyntaxLookup element) {
                return false;
            }

            @Override
            protected Boolean visit(RSyntaxFunction element) {
                return false;
            }
        };
        return visitor.accept(getSyntaxBody());
    }

    @Override
    public boolean needsSplitting() {
        return needsSplitting;
    }

    public RSyntaxNode getBody() {
        return body.asRSyntaxNode();
    }

    public PostProcessArgumentsNode getArgPostProcess() {
        return argPostProcess;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        boolean runOnExitHandlers = true;
        try {
            verifyEnclosingAssumptions(frame);
            setupDispatchSlots(frame);
            saveArguments.execute(frame);
            Object result = body.visibleExecute(frame);
            normalExit.enter();
            if (CompilerDirectives.inInterpreter() && result == null) {
                throw RInternalError.shouldNotReachHere("invalid null in result of " + this);
            }
            return result;
        } catch (ReturnException ex) {
            if (returnTopLevelProfile.profile(ex.getTarget() == RArguments.getCall(frame))) {
                return ex.getResult();
            } else {
                throw ex;
            }
        } catch (BreakException e) {
            breakProfile.enter();
            throw e;
        } catch (NextException e) {
            nextProfile.enter();
            throw e;
        } catch (RError e) {
            CompilerDirectives.transferToInterpreter();
            SetVisibilityNode.executeSlowPath(frame, false);
            throw e;
        } catch (DebugExitException | JumpToTopLevelException | ExitException | ThreadDeath e) {
            /*
             * These relate to debugging support and various other reasons for returning to the top
             * level. exitHandlers must be suppressed and the exceptions must pass through
             * unchanged; they are not errors
             */
            CompilerDirectives.transferToInterpreter();
            runOnExitHandlers = false;
            throw e;
        } catch (Throwable e) {
            CompilerDirectives.transferToInterpreter();
            runOnExitHandlers = false;
            throw e instanceof RInternalError ? (RInternalError) e : new RInternalError(e, e.toString());
        } finally {
            /*
             * Although a user function may throw an exception from an onExit handler, all
             * evaluations are wrapped in an anonymous function (see REngine.makeCallTarget) that
             * has no exit handlers (by fiat), so any exceptions from onExits handlers will be
             * caught above.
             */
            if (argPostProcess != null) {
                resetArgs.enter();
                argPostProcess.execute(frame);
            }
            if (runOnExitHandlers) {
                visibility.executeEndOfFunction(frame);
                if (!noHandlerStackSlot.isValid()) {
                    FrameSlot slot = getHandlerFrameSlot(frame);
                    if (frame.isObject(slot)) {
                        try {
                            RErrorHandling.restoreHandlerStack(frame.getObject(slot));
                        } catch (FrameSlotTypeException e) {
                            throw RInternalError.shouldNotReachHere();
                        }
                    }
                }
                if (!noRestartStackSlot.isValid()) {
                    FrameSlot slot = getRestartFrameSlot(frame);
                    if (frame.isObject(slot)) {
                        try {
                            RErrorHandling.restoreRestartStack(frame.getObject(slot));
                        } catch (FrameSlotTypeException e) {
                            throw RInternalError.shouldNotReachHere();
                        }
                    }
                }

                if (onExitProfile.profile(onExitSlot.hasValue(frame))) {
                    if (onExitExpressionCache == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        onExitExpressionCache = insert(InlineCacheNode.createExpression(3));
                    }
                    RPairList current = getCurrentOnExitList(frame, onExitSlot.executeFrameSlot(frame));
                    /*
                     * We do not need to preserve visibility, since visibility.executeEndOfFunction
                     * was already called.
                     */
                    try {
                        for (RPairList expr : current) {
                            if (expr.car() != RNull.instance) {
                                if (!(expr.car() instanceof RNode)) {
                                    RInternalError.shouldNotReachHere("unexpected type for on.exit entry: " + expr.car());
                                }
                                onExitExpressionCache.execute(frame, expr.car());
                            }
                        }
                    } catch (ReturnException ex) {
                        if (returnTopLevelProfile.profile(ex.getTarget() == RArguments.getCall(frame))) {
                            return ex.getResult();
                        } else {
                            throw ex;
                        }
                    }
                }
            }
        }
    }

    private abstract static class SetupDispatchNode extends Node {
        // S3/S4 slots
        private final FrameSlot dotGenericSlot;
        private final FrameSlot dotMethodSlot;

        final BranchProfile invalidateFrameSlotProfile = BranchProfile.create();

        SetupDispatchNode(FrameDescriptor frameDescriptor) {
            dotGenericSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frameDescriptor, RRuntime.R_DOT_GENERIC, FrameSlotKind.Object);
            dotMethodSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frameDescriptor, RRuntime.R_DOT_METHOD, FrameSlotKind.Object);
        }

        void execute(VirtualFrame frame, DispatchArgs args) {
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotGenericSlot, args.generic, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotMethodSlot, args.method, false, invalidateFrameSlotProfile);
        }
    }

    private static final class SetupS3ArgsNode extends SetupDispatchNode {
        // S3 slots
        private final FrameSlot dotClassSlot;
        private final FrameSlot dotGenericCallEnvSlot;
        private final FrameSlot dotGenericCallDefSlot;
        private final FrameSlot dotGroupSlot;

        SetupS3ArgsNode(FrameDescriptor frameDescriptor) {
            super(frameDescriptor);
            dotClassSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frameDescriptor, RRuntime.R_DOT_CLASS, FrameSlotKind.Object);
            dotGenericCallEnvSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frameDescriptor, RRuntime.R_DOT_GENERIC_CALL_ENV, FrameSlotKind.Object);
            dotGenericCallDefSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frameDescriptor, RRuntime.R_DOT_GENERIC_DEF_ENV, FrameSlotKind.Object);
            dotGroupSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frameDescriptor, RRuntime.R_DOT_GROUP, FrameSlotKind.Object);
        }

        void execute(VirtualFrame frame, S3Args args) {
            super.execute(frame, args);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotClassSlot, args.clazz, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotGenericCallEnvSlot, args.callEnv, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotGenericCallDefSlot, args.defEnv, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotGroupSlot, args.group, false, invalidateFrameSlotProfile);
        }
    }

    private static final class SetupS4ArgsNode extends SetupDispatchNode {
        // S4 slots
        private final FrameSlot dotDefinedSlot;
        private final FrameSlot dotTargetSlot;
        private final FrameSlot dotMethodsSlot;

        SetupS4ArgsNode(FrameDescriptor frameDescriptor) {
            super(frameDescriptor);
            dotDefinedSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frameDescriptor, RRuntime.R_DOT_DEFINED, FrameSlotKind.Object);
            dotTargetSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frameDescriptor, RRuntime.R_DOT_TARGET, FrameSlotKind.Object);
            dotMethodsSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frameDescriptor, RRuntime.R_DOT_METHODS, FrameSlotKind.Object);
        }

        void execute(VirtualFrame frame, S4Args args) {
            super.execute(frame, args);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotDefinedSlot, args.defined, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotTargetSlot, args.target, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotMethodsSlot, args.methods, false, invalidateFrameSlotProfile);
        }
    }

    private void setupDispatchSlots(VirtualFrame frame) {
        DispatchArgs dispatchArgs = RArguments.getDispatchArgs(frame);
        if (dispatchArgs == null) {
            return;
        }
        if (dispatchArgs instanceof S3Args) {
            S3Args s3Args = (S3Args) dispatchArgs;
            if (setupS3Args == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setupS3Args = insert(new SetupS3ArgsNode(frame.getFrameDescriptor()));
            }
            setupS3Args.execute(frame, s3Args);
        } else {
            S4Args s4Args = (S4Args) dispatchArgs;
            if (setupS4Args == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                setupS4Args = insert(new SetupS4ArgsNode(frame.getFrameDescriptor()));
            }
            setupS4Args.execute(frame, s4Args);
        }
    }

    private static RPairList getCurrentOnExitList(VirtualFrame frame, FrameSlot slot) {
        try {
            return (RPairList) FrameSlotChangeMonitor.getObject(slot, frame);
        } catch (FrameSlotTypeException e) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @Override
    public String getName() {
        return name == null ? "<no source>" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public RBuiltinFactory getBuiltin() {
        return null;
    }

    @Override
    public void setSourceSection(SourceSection sourceSection) {
        assert sourceSection != null;
        this.sourceSectionR = sourceSection;
    }

    @Override
    public SourceSection getLazySourceSection() {
        return sourceSectionR;
    }

    @Override
    public SourceSection getSourceSection() {
        RDeparse.ensureSourceSection(this);
        return sourceSectionR;
    }

    @Override
    public ArgumentsSignature getSyntaxSignature() {
        return getFormalArguments().getSignature();
    }

    @Override
    public RSyntaxElement[] getSyntaxArgumentDefaults() {
        return RASTUtils.asSyntaxNodes(getFormalArguments().getArguments());
    }

    @Override
    public RSyntaxElement getSyntaxBody() {
        return getBody();
    }

    @Override
    public String getSyntaxDebugName() {
        return name;
    }

    public FrameSlot getRestartFrameSlot(VirtualFrame frame) {
        if (noRestartStackSlot.isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            noRestartStackSlot.invalidate();
        }
        if (restartStackSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            restartStackSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frame.getFrameDescriptor(), RFrameSlot.RestartStack, FrameSlotKind.Object);
        }
        return restartStackSlot;
    }

    public FrameSlot getHandlerFrameSlot(VirtualFrame frame) {
        if (noHandlerStackSlot.isValid()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            noHandlerStackSlot.invalidate();
        }
        if (handlerStackSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            handlerStackSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frame.getFrameDescriptor(), RFrameSlot.HandlerStack, FrameSlotKind.Object);
        }
        return handlerStackSlot;
    }
}
