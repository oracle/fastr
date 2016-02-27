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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.QuitException;
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
import com.oracle.truffle.r.nodes.instrument.factory.RInstrumentFactory;
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
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;
import com.oracle.truffle.r.runtime.instrument.FunctionUIDFactory;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public final class FunctionDefinitionNode extends RRootNode implements RSyntaxNode {

    @Child private RNode body; // typed as RNode to avoid custom instrument wrapper
    private final RNode uninitializedBody; // copy for "body" builtin
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

    /**
     * An instance of this node may be called from with the intention to have its execution leave a
     * footprint behind in a specific frame/environment, e.g., during library loading, commands from
     * the shell, or R's {@code eval} and its friends. In that case, {@code substituteFrame} is
     * {@code true}, and the {@link #execute(VirtualFrame)} method must be invoked with one
     * argument, namely the {@link VirtualFrame} to be side-effected. Execution will then proceed in
     * the context of that frame. Note that passing only this one frame argument, strictly spoken,
     * violates the frame layout as set forth in {@link RArguments}. This is for internal use only.
     */
    private final boolean substituteFrame;

    private final boolean needsSplitting;

    private final boolean containsDispatch;

    /**
     * Profiling for catching {@link ReturnException}s.
     */
    private final BranchProfile returnProfile = BranchProfile.create();

    public FunctionDefinitionNode(SourceSection src, FrameDescriptor frameDesc, RNode body, FormalArguments formals, String description, boolean substituteFrame,
                    PostProcessArgumentsNode argPostProcess) {
        this(src, frameDesc, body, formals, description, substituteFrame, false, argPostProcess);
    }

    // TODO skipOnExit: Temporary solution to allow onExit to be switched of; used for
    // REngine.evalPromise
    public FunctionDefinitionNode(SourceSection src, FrameDescriptor frameDesc, RNode body, FormalArguments formals, String description, boolean substituteFrame, boolean skipExit,
                    PostProcessArgumentsNode argPostProcess) {
        this(src, frameDesc, body, formals, description, substituteFrame, skipExit, argPostProcess, FunctionUIDFactory.get().createUID());
    }

    private FunctionDefinitionNode(SourceSection src, FrameDescriptor frameDesc, RNode body, FormalArguments formals, String description, boolean substituteFrame, boolean skipExit,
                    PostProcessArgumentsNode argPostProcess, FunctionUID uuid) {
        super(null, formals, frameDesc);
        assert FrameSlotChangeMonitor.isValidFrameDescriptor(frameDesc);
        assert src != null;
        this.sourceSectionR = src;
        this.body = body;
        this.uninitializedBody = body;
        this.description = description;
        this.substituteFrame = substituteFrame;
        this.onExitSlot = skipExit ? null : FrameSlotNode.createInitialized(frameDesc, RFrameSlot.OnExit, false);
        this.uuid = uuid;
        this.needsSplitting = needsAnyBuiltinSplitting();
        this.containsDispatch = containsAnyDispatch(body);
        this.argPostProcess = argPostProcess;
        RInstrumentFactory.getInstance().registerFunctionDefinitionNode(this);
    }

    @Override
    public RRootNode duplicateWithNewFrameDescriptor() {
        FrameDescriptor frameDesc = new FrameDescriptor();
        FrameSlotChangeMonitor.initializeFunctionFrameDescriptor(description != null && !description.isEmpty() ? description : "<function>", frameDesc);
        FunctionDefinitionNode result = new FunctionDefinitionNode(getSourceSection(), frameDesc, (RNode) body.deepCopy(), getFormalArguments(), description, substituteFrame,
                        false, argPostProcess == null ? null : argPostProcess.deepCopyUnconditional(), uuid);
        return result;
    }

    private static boolean containsAnyDispatch(RNode body) {
        NodeCountFilter dispatchingMethodsFilter = node -> {
            if (node instanceof ReadVariableNode) {
                ReadVariableNode rvn = (ReadVariableNode) node;
                String identifier = rvn.getIdentifier();
                // TODO: can we also do this for S4 new?
                return rvn.getMode() == RType.Function && ("UseMethod".equals(identifier) || "standardGeneric".equals(identifier));
                /* || "NextMethod".equals(identifier) */
            }
            return false;
        };
        return NodeUtil.countNodes(body, dispatchingMethodsFilter) > 0;
    }

    public boolean containsDispatch() {
        return containsDispatch;
    }

    @Override
    public Node deepCopy() {
        FunctionDefinitionNode copy = (FunctionDefinitionNode) super.deepCopy();
        return copy;
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

    public FunctionUID getUID() {
        return uuid;
    }

    public BodyNode getBody() {
        return (BodyNode) RASTUtils.unwrap(body);
    }

    public FunctionBodyNode getUninitializedBody() {
        return (FunctionBodyNode) uninitializedBody;
    }

    public PostProcessArgumentsNode getArgPostProcess() {
        return argPostProcess;
    }

    /**
     * @see #substituteFrame
     */
    @Override
    public Object execute(VirtualFrame frame) {
        VirtualFrame vf = substituteFrame ? new SubstituteVirtualFrame((MaterializedFrame) frame.getArguments()[0]) : frame;
        /*
         * It might be possible to only record this iff a handler is installed, by using the
         * RArguments array.
         */
        Object handlerStack = RErrorHandling.getHandlerStack();
        Object restartStack = RErrorHandling.getRestartStack();
        boolean runOnExitHandlers = true;
        try {
            verifyEnclosingAssumptions(vf);
            setupDispatchSlots(vf);
            Object result = body.execute(vf);
            normalExit.enter();
            return result;
        } catch (ReturnException ex) {
            returnProfile.enter();
            int depth = ex.getDepth();
            if ((depth != -1 && RArguments.getDepth(vf) != depth) || (substituteFrame && this.description == RPromise.CLOSURE_WRAPPER_NAME)) {
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
        } catch (DebugExitException | QuitException | BrowserQuitException e) {
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
                argPostProcess.execute(vf);
            }
            if (runOnExitHandlers) {
                RErrorHandling.restoreStacks(handlerStack, restartStack);
                if (onExitSlot != null && onExitProfile.profile(onExitSlot.hasValue(vf))) {
                    if (onExitExpressionCache == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        onExitExpressionCache = insert(InlineCacheNode.createExpression(3));
                    }
                    ArrayList<Object> current = getCurrentOnExitList(vf, onExitSlot.executeFrameSlot(vf));
                    // Preserve the visibility state as may be changed by the on.exit
                    boolean isVisible = RContext.getInstance().isVisible();
                    try {
                        for (Object expr : current) {
                            if (!(expr instanceof RNode)) {
                                RInternalError.shouldNotReachHere("unexpected type for on.exit entry");
                            }
                            RNode node = (RNode) expr;
                            onExitExpressionCache.execute(vf, node);
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

    @Override
    public boolean isCloningAllowed() {
        return !substituteFrame;
    }

    /*
     * TODO Decide whether we really care about the braces/no-braces issue for deparse and
     * serialize, since we do not distinguish them in other nodes at the present time.
     */

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
        return new FunctionDefinitionNode(RSyntaxNode.EAGER_DEPARSE, frameDesc, body.substitute(env).asRNode(), getFormalArguments(), null, substituteFrame, argPostProcess);
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
        boolean hasBraces = checkOpenBrace(state);

        serializeBody(state);

        checkCloseBrace(state, hasBraces);
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
    public boolean checkOpenBrace(RSerialize.State state) {
        boolean hasBraces = hasBraces();
        if (hasBraces) {
            state.openBrace();
            state.openPairList();
        }
        return hasBraces;
    }

    /**
     * Also called by {@link FunctionExpressionNode}.
     */
    public static void checkCloseBrace(RSerialize.State state, boolean hasBraces) {
        if (hasBraces) {
            if (state.isNull()) {
                // special case of empty body "{ }"
                state.setNull();
            } else {
                state.switchCdrToCar();
            }
            state.setCdr(state.closePairList());
            state.setCdr(state.closePairList());
        }
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

    public void setInstrumented() {
        instrumented = true;
    }

    /**
     * A workaround for not representing left curly brace as a function call. We have to depend on
     * the source section and "parse" the start of the function definition.
     */
    @TruffleBoundary
    public boolean hasBraces() {
        SourceSection src = getSourceSection();
        if (src == null) {
            return true; // statistically probable
        }
        String s = src.getCode();
        int ix = s.indexOf('(') + 1;
        int bdepth = 1;
        while (ix < s.length() && bdepth > 0) {
            char ch = s.charAt(ix);
            if (ch == '(') {
                bdepth++;
            } else if (ch == ')') {
                bdepth--;
            }
            ix++;
        }
        while (ix < s.length()) {
            char ch = s.charAt(ix);
            boolean whitespace = Character.isWhitespace(ch);
            if (!whitespace) {
                return ch == '{';
            }
            ix++;
        }
        return false;
    }

    public void setSourceSection(SourceSection sourceSection) {
        assert sourceSection != null;
        this.sourceSectionR = sourceSection;
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSectionR;
    }

}
