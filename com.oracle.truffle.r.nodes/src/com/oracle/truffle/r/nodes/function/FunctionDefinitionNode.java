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
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.access.FrameSlotNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.control.BreakException;
import com.oracle.truffle.r.nodes.control.NextException;
import com.oracle.truffle.r.nodes.instrumentation.RInstrumentation;
import com.oracle.truffle.r.nodes.instrumentation.RSyntaxTags;
import com.oracle.truffle.r.runtime.BrowserQuitException;
import com.oracle.truffle.r.runtime.FunctionUID;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RArguments.DispatchArgs;
import com.oracle.truffle.r.runtime.RArguments.S3Args;
import com.oracle.truffle.r.runtime.RArguments.S4Args;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RErrorHandling;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.Utils.DebugExitException;
import com.oracle.truffle.r.runtime.WithFunctionUID;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;
import com.oracle.truffle.r.runtime.instrument.FunctionUIDFactory;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public final class FunctionDefinitionNode extends RRootNode implements RSyntaxNode, WithFunctionUID {

    @Child private RNode body; // typed as RNode to avoid custom instrument wrapper
    /**
     * This exists for debugging purposes. It is set initially when the function is defined to
     * either:
     * <ul>
     * <li>The name of the variable that the function definition is assigned to, e.g,
     * {@code f <- function}.
     * <li>The first several characters of the function definition for anonymous functions.
     * </ul>
     * It can be updated later by calling {@link #setDescription}, which is useful for functions
     * lazily loaded from packages, where at the point of definition any assignee variable is
     * unknown.
     */
    private String description;
    private FunctionUID uuid;
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

    @Child private PostProcessArgumentsNode argPostProcess;

    private final boolean needsSplitting;

    private final boolean containsDispatch;

    /**
     * Profiling for catching {@link ReturnException}s.
     */
    private final BranchProfile returnProfile = BranchProfile.create();

    public static FunctionDefinitionNode create(SourceSection src, FrameDescriptor frameDesc, SourceSection[] argSourceSections, SaveArgumentsNode saveArguments, RSyntaxNode body,
                    FormalArguments formals, String description,
                    PostProcessArgumentsNode argPostProcess) {
        return new FunctionDefinitionNode(src, frameDesc, argSourceSections, saveArguments, body, formals, description, argPostProcess, FunctionUIDFactory.get().createUID());
    }

    private FunctionDefinitionNode(SourceSection src, FrameDescriptor frameDesc, SourceSection[] argSourceSections, RNode saveArguments, RSyntaxNode body, FormalArguments formals,
                    String description, PostProcessArgumentsNode argPostProcess, FunctionUID uuid) {
        super(null, formals, frameDesc);
        this.argSourceSections = argSourceSections;
        assert FrameSlotChangeMonitor.isValidFrameDescriptor(frameDesc);
        assert src != null;
        this.sourceSectionR = src;
        this.saveArguments = saveArguments;
        this.body = body.asRNode();
        SourceSection sns = body.getSourceSection();
        if (!sns.hasTag(RSyntaxTags.START_FUNCTION)) {
            body.setSourceSection(RSyntaxTags.addTags(sns, RSyntaxTags.START_FUNCTION));
        }
        this.description = description;
        this.onExitSlot = FrameSlotNode.createInitialized(frameDesc, RFrameSlot.OnExit, false);
        this.uuid = uuid;
        this.needsSplitting = needsAnyBuiltinSplitting();
        this.containsDispatch = containsAnyDispatch(body);
        this.argPostProcess = argPostProcess;
        RInstrumentation.registerFunctionDefinition(this);
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
        RootCallTarget callTarget = RContext.getASTBuilder().rootFunction(getSourceSection(), args, builder.process(getBody()), description);
        ((FunctionDefinitionNode) callTarget.getRootNode()).uuid = uuid;
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

    public boolean containsDispatch() {
        return containsDispatch;
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

    @Override
    public FunctionUID getUID() {
        return uuid;
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
            returnProfile.enter();
            int depth = ex.getDepth();
            if (depth != -1 && RArguments.getDepth(frame) != depth) {
                throw ex;
            } else {
                return ex.getResult();
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
        } catch (DebugExitException | BrowserQuitException e) {
            /*
             * These relate to the debugging support. exitHandlers must be suppressed and the
             * exceptions must pass through unchanged; they are not errors
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
                RErrorHandling.restoreStacks(handlerStack, restartStack);
                if (onExitProfile.profile(onExitSlot.hasValue(frame))) {
                    if (onExitExpressionCache == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        onExitExpressionCache = insert(InlineCacheNode.createExpression(3));
                    }
                    ArrayList<Object> current = getCurrentOnExitList(frame, onExitSlot.executeFrameSlot(frame));
                    // Preserve the visibility state as may be changed by the on.exit
                    boolean isVisible = RContext.getInstance().isVisible();
                    try {
                        for (Object expr : current) {
                            if (!(expr instanceof RNode)) {
                                RInternalError.shouldNotReachHere("unexpected type for on.exit entry");
                            }
                            RNode node = (RNode) expr;
                            onExitExpressionCache.execute(frame, node);
                        }
                    } finally {
                        RContext.getInstance().setVisible(isVisible);
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
    public String toString() {
        return description == null ? "<no source>" : description;
    }

    /*
     * TODO Decide whether we really care about the braces/no-braces issue for deparse and
     * serialize, since we do not distinguish them in other nodes at the present time.
     */

    @Override
    public void deparseImpl(RDeparse.State state) {
        // TODO linebreaks
        state.startNodeDeparse(this);
        state.append("function (");
        FormalArguments formals = getFormalArguments();
        int formalsLength = formals.getSignature().getLength();
        for (int i = 0; i < formalsLength; i++) {
            RNode defaultArg = formals.getDefaultArgument(i);
            state.append(formals.getSignature().getName(i));
            if (defaultArg != null) {
                state.append(" = ");
                defaultArg.deparse(state);
            }
            if (i != formalsLength - 1) {
                state.append(", ");
            }
        }
        state.append(") ");
        state.writeline();
        body.deparse(state);
        state.endNodeDeparse(this);
    }

    @Override
    public RSyntaxNode substituteImpl(REnvironment env) {
        FrameDescriptor frameDesc = new FrameDescriptor();

        FrameSlotChangeMonitor.initializeFunctionFrameDescriptor("<substituted function>", frameDesc);
        return new FunctionDefinitionNode(RSyntaxNode.EAGER_DEPARSE, frameDesc, null, saveArguments, body.substitute(env), getFormalArguments(), null,
                        argPostProcess, FunctionUIDFactory.get().createUID());
    }

    /**
     * Serialize a function. On entry {@code state} has an active pairlist, whose {@code tag} is the
     * enclosing {@link REnvironment}. The {@code car} must be set to the pairlist representing the
     * formal arguments (or {@link RNull} if none) and the {@code cdr} to the pairlist representing
     * the body. Each formal argument is represented as a pairlist:
     * <ul>
     * <li>{@code tag}: RSymbol(name)</li>
     * <li>{@code car}: Missing or default value</li>
     * <li>{@code cdr}: if last formal then RNull else pairlist for next argument.
     * </ul>
     * N.B. The body is never empty as the syntax "{}" has a value, however if the body is a simple
     * expression, e.g. {@code function(x) x}, the body is not represented as a pairlist, just a
     * SYMSXP, which is handled transparently in {@code RSerialize.State.closePairList()}.
     *
     */
    @Override
    public void serializeImpl(RSerialize.State state) {
        serializeFormals(state);
        serializeBody(state);
    }

    /**
     * Also called by {@link FunctionExpressionNode}.
     */
    public void serializeBody(RSerialize.State state) {
        state.openPairList();
        body.serialize(state);
        state.setCdr(state.closePairList());
    }

    /**
     * Also called by {@link FunctionExpressionNode}.
     */
    public void serializeFormals(RSerialize.State state) {
        FormalArguments formals = getFormalArguments();
        int formalsLength = formals.getSignature().getLength();
        if (formalsLength > 0) {
            for (int i = 0; i < formalsLength; i++) {
                RNode defaultArg = formals.getDefaultArgument(i);
                state.openPairList();
                state.setTagAsSymbol(formals.getSignature().getName(i));
                if (defaultArg != null) {
                    state.serializeNodeSetCar(defaultArg);
                } else {
                    state.setCarMissing();
                }
            }
            state.linkPairList(formalsLength);
            state.setCar(state.closePairList());
        } else {
            state.setCar(RNull.instance);
        }
    }

    public void setDescription(String name) {
        this.description = name;
    }

    public boolean getInstrumented() {
        return instrumented;
    }

    @Override
    public void setSourceSection(SourceSection sourceSection) {
        assert sourceSection != null;
        this.sourceSectionR = sourceSection;
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSectionR;
    }
}
