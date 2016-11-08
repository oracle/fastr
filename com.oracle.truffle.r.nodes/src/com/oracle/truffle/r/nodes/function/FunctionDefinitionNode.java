/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
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
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxFunction;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public final class FunctionDefinitionNode extends RRootNode implements RSyntaxNode, RSyntaxFunction {

    @Child private RNode body; // typed as RNode to avoid custom instrument wrapper
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
    private boolean instrumented = false;
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

    @CompilationFinal private BranchProfile invalidateFrameSlotProfile;
    // S3/S4 slots
    @Child private FrameSlotNode dotGenericSlot;
    @Child private FrameSlotNode dotMethodSlot;
    // S3 slots
    @Child private FrameSlotNode dotClassSlot;
    @Child private FrameSlotNode dotGenericCallEnvSlot;
    @Child private FrameSlotNode dotGenericCallDefSlot;
    @Child private FrameSlotNode dotGroupSlot;
    // S4 slots
    @Child private FrameSlotNode dotDefinedSlot;
    @Child private FrameSlotNode dotTargetSlot;
    @Child private FrameSlotNode dotMethodsSlot;

    @Child private SetVisibilityNode visibility = SetVisibilityNode.create();

    @Child private PostProcessArgumentsNode argPostProcess;

    private final boolean needsSplitting;

    @CompilationFinal private boolean containsDispatch;

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
        super(null, formals, frameDesc, RASTBuilder.createFunctionFastPath(body, formals.getSignature()));
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
        NodeCountFilter findAlwaysSplitInternal = node -> {
            if (node instanceof RCallNode) {
                RCallNode internalCall = (RCallNode) node;

                if (internalCall.getFunctionNode() instanceof ReadVariableNode) {
                    ReadVariableNode readInternal = (ReadVariableNode) internalCall.getFunctionNode();

                    /*
                     * TODO This is a hack to make sapply split lapply. We need to find better ways
                     * to do this. If a function uses lapply anywhere as name then it gets split.
                     * This could get exploited.
                     */
                    RBuiltinDescriptor directBuiltin = RContext.lookupBuiltinDescriptor(readInternal.getIdentifier());
                    if (directBuiltin != null && directBuiltin.isSplitCaller()) {
                        return true;
                    }

                    if (readInternal.getIdentifier().equals(".Internal")) {
                        Node internalFunctionArgument = RASTUtils.unwrap(internalCall.getArguments().getArguments()[0]);
                        if (internalFunctionArgument instanceof RCallNode) {
                            RCallNode innerCall = (RCallNode) internalFunctionArgument;
                            if (innerCall.getFunctionNode() instanceof ReadVariableNode) {
                                ReadVariableNode readInnerCall = (ReadVariableNode) innerCall.getFunctionNode();
                                RBuiltinDescriptor builtin = RContext.lookupBuiltinDescriptor(readInnerCall.getIdentifier());
                                if (builtin != null && builtin.isSplitCaller()) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        };
        return NodeUtil.countNodes(this, findAlwaysSplitInternal) > 0;
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
        /*
         * It might be possible to only record this iff a handler is installed, by using the
         * RArguments array.
         */
        Object handlerStack = RErrorHandling.getHandlerStack();
        Object restartStack = RErrorHandling.getRestartStack();
        boolean runOnExitHandlers = true;
        try {
            verifyEnclosingAssumptions(frame);
            setupDispatchSlots(frame);
            saveArguments.execute(frame);
            Object result = body.execute(frame);
            normalExit.enter();
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
            visibility.executeEndOfFunction(frame);
            if (argPostProcess != null) {
                resetArgs.enter();
                argPostProcess.execute(frame);
            }
            if (runOnExitHandlers) {
                RErrorHandling.restoreStacks(handlerStack, restartStack);
                if (onExitProfile.profile(onExitSlot.hasValue(frame))) {
                    if (onExitExpressionCache == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        onExitExpressionCache = insert(InlineCacheNode.createExpression(3));
                    }
                    ArrayList<Object> current = getCurrentOnExitList(frame, onExitSlot.executeFrameSlot(frame));
                    /*
                     * We do not need to preserve visibility, since visibility.executeEndOfFunction
                     * was already called.
                     */
                    for (Object expr : current) {
                        if (!(expr instanceof RNode)) {
                            RInternalError.shouldNotReachHere("unexpected type for on.exit entry");
                        }
                        RNode node = (RNode) expr;
                        onExitExpressionCache.execute(frame, node);
                    }
                }
            }
        }
    }

    private void setupDispatchSlots(VirtualFrame frame) {
        DispatchArgs dispatchArgs = RArguments.getDispatchArgs(frame);
        if (dispatchArgs == null) {
            return;
        }
        if (dotGenericSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            assert invalidateFrameSlotProfile == null && dotMethodSlot == null;
            invalidateFrameSlotProfile = BranchProfile.create();
            FrameDescriptor frameDescriptor = frame.getFrameDescriptor();

            dotGenericSlot = insert(FrameSlotNode.createInitialized(frameDescriptor, RRuntime.R_DOT_GENERIC, true));
            dotMethodSlot = insert(FrameSlotNode.createInitialized(frameDescriptor, RRuntime.R_DOT_METHOD, true));
        }
        FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotGenericSlot.executeFrameSlot(frame), dispatchArgs.generic, false, invalidateFrameSlotProfile);
        FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotMethodSlot.executeFrameSlot(frame), dispatchArgs.method, false, invalidateFrameSlotProfile);

        if (dispatchArgs instanceof S3Args) {
            S3Args s3Args = (S3Args) dispatchArgs;
            if (dotClassSlot == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                assert dotGenericCallEnvSlot == null && dotGenericCallDefSlot == null && dotGroupSlot == null;
                invalidateFrameSlotProfile = BranchProfile.create();
                FrameDescriptor frameDescriptor = frame.getFrameDescriptor();

                dotClassSlot = insert(FrameSlotNode.createInitialized(frameDescriptor, RRuntime.R_DOT_CLASS, true));
                dotGenericCallEnvSlot = insert(FrameSlotNode.createInitialized(frameDescriptor, RRuntime.R_DOT_GENERIC_CALL_ENV, true));
                dotGenericCallDefSlot = insert(FrameSlotNode.createInitialized(frameDescriptor, RRuntime.R_DOT_GENERIC_DEF_ENV, true));
                dotGroupSlot = insert(FrameSlotNode.createInitialized(frameDescriptor, RRuntime.R_DOT_GROUP, true));
            }
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotClassSlot.executeFrameSlot(frame), s3Args.clazz, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotGenericCallEnvSlot.executeFrameSlot(frame), s3Args.callEnv, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotGenericCallDefSlot.executeFrameSlot(frame), s3Args.defEnv, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotGroupSlot.executeFrameSlot(frame), s3Args.group, false, invalidateFrameSlotProfile);
        } else {
            S4Args s4Args = (S4Args) dispatchArgs;
            if (dotDefinedSlot == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                assert dotTargetSlot == null && dotMethodsSlot == null;
                invalidateFrameSlotProfile = BranchProfile.create();
                FrameDescriptor frameDescriptor = frame.getFrameDescriptor();

                dotDefinedSlot = insert(FrameSlotNode.createInitialized(frameDescriptor, RRuntime.R_DOT_DEFINED, true));
                dotTargetSlot = insert(FrameSlotNode.createInitialized(frameDescriptor, RRuntime.R_DOT_TARGET, true));
                dotMethodsSlot = insert(FrameSlotNode.createInitialized(frameDescriptor, RRuntime.R_DOT_METHODS, true));
            }
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotDefinedSlot.executeFrameSlot(frame), s4Args.defined, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotTargetSlot.executeFrameSlot(frame), s4Args.target, false, invalidateFrameSlotProfile);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, dotMethodsSlot.executeFrameSlot(frame), s4Args.methods, false, invalidateFrameSlotProfile);
        }
    }

    @SuppressWarnings("unchecked")
    private static ArrayList<Object> getCurrentOnExitList(VirtualFrame frame, FrameSlot slot) {
        try {
            return (ArrayList<Object>) frame.getObject(slot);
        } catch (FrameSlotTypeException e) {
            throw RInternalError.shouldNotReachHere();
        }
    }

    @Override
    public String getName() {
        return name == null ? "<no source>" : name;
    }

    @Override
    public String toString() {
        return getName();
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getInstrumented() {
        return instrumented;
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
}
