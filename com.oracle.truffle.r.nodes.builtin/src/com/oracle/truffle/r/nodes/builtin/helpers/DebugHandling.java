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
package com.oracle.truffle.r.nodes.builtin.helpers;

import java.io.IOException;
import java.util.ArrayList;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import com.oracle.truffle.r.nodes.control.AbstractLoopNode;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.instrumentation.RInstrumentation;
import com.oracle.truffle.r.nodes.instrumentation.RSyntaxTags;
import com.oracle.truffle.r.runtime.JumpToTopLevelException;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.context.ConsoleHandler;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxFunction;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxVisitor;

/**
 * The implementation of the R debug functions.
 *
 * When a function is enabled for debugging a set of {@link DebugEventListener}s are created and
 * attached to key nodes in the AST body associated with the {@link FunctionDefinitionNode}
 * corresponding to the {@link RFunction} instance.
 *
 * Three different listener classes are defined:
 * <ul>
 * <li>{@link FunctionStatementsEventListener}: attaches to function bodies and handles the special
 * behavior on entry/exit</li>
 * <li>{@link StatementEventListener}: attaches to all {@code StandardTags.StatementTag} nodes and
 * handles "n" and "s" browser commands</li>
 * <li>{@link LoopStatementEventListener}: attaches to {@link AbstractLoopNode} instances and
 * handles special "f" command behavior.
 * </ul>
 * <p>
 * Step Into is slightly tricky because, at the point the command is issued, we do not know what
 * function the call will resolve to. Nor do we know whether the function that is entered has itself
 * been the subject of debug (and not been undebug'ed). In any event we have to force it into debug
 * mode and reset it's state on return. This is handled as follows:
 * <ol>
 * <li>On a step-into, attach a {@link StepIntoInstrumentListener} with a filter that matches all
 * functions and the {@code StandardTags.RootTag} tag</li>
 * <li>On entry to that listener instrument/enable the function we have entered (if necessary) for
 * one-time (unless already)</li>
 * <li>Dispose the {@link StepIntoInstrumentListener} and continue, which will then stop at the
 * function body.</li>
 * <li>On return from the function, reset the debug state if necessary.
 * </ol>
 * <p>
 * When invoked from within a loop The "f" command continues the loop body without entry and the
 * re-enables entry. This is handled by creating a {@link LoopStatementEventListener} per
 * {@link AbstractLoopNode}. On a "f" every listener <b>except</b> the one associated with that loop
 * is disabled. On return from the loop, everything is re-enabled.
 * <p>
 * Currently, {@code debugonce} and {@code undebug} are handled by disabling the listener behavior.
 * Any change in enabled state is managed by an {@link Assumption} which will invalidate the code of
 * the listener. In the case where events are disabled there should be no compilation overhead from
 * the listeners. Alternatively we could dispose all the listeners and revert to initial state.
 */
public class DebugHandling {

    /**
     * Attach the DebugHandling instrument to the FunctionStatementsNode and all syntactic nodes.
     *
     * @param implicit TODO
     */
    public static boolean enableDebug(RFunction func, Object text, Object condition, boolean once, boolean implicit) {
        FunctionStatementsEventListener fbr = getFunctionStatementsEventListener(func);
        if (fbr == null) {
            attachDebugHandler(func, text, condition, once, implicit);
        } else {
            fbr.enable();
        }
        return true;
    }

    public static boolean undebug(RFunction func) {
        FunctionStatementsEventListener fbr = getFunctionStatementsEventListener(func);
        if (fbr == null) {
            return false;
        } else {
            fbr.disable();
            return true;
        }
    }

    public static boolean isDebugged(RFunction func) {
        FunctionStatementsEventListener fser = getFunctionStatementsEventListener(func);
        return fser != null && !fser.disabled();
    }

    private static FunctionStatementsEventListener getFunctionStatementsEventListener(RFunction func) {
        return (FunctionStatementsEventListener) RContext.getInstance().stateInstrumentation.getDebugListener(RInstrumentation.getSourceSection(func));
    }

    private static FunctionStatementsEventListener getFunctionStatementsEventListener(FunctionDefinitionNode fdn) {
        return (FunctionStatementsEventListener) RContext.getInstance().stateInstrumentation.getDebugListener(fdn.getSourceSection());
    }

    private static void attachDebugHandler(RFunction func, Object text, Object condition, boolean once, boolean implicit) {
        attachDebugHandler(RInstrumentation.getFunctionDefinitionNode(func), text, condition, once, implicit);
    }

    @TruffleBoundary
    private static FunctionStatementsEventListener attachDebugHandler(FunctionDefinitionNode fdn, Object text, Object condition, boolean once, boolean implicit) {
        FunctionStatementsEventListener fser = new FunctionStatementsEventListener(fdn, text, condition, once, implicit);
        // First attach the main listener on the START_FUNCTION
        Instrumenter instrumenter = RInstrumentation.getInstrumenter();
        SourceSectionFilter.Builder functionBuilder = RInstrumentation.createFunctionFilter(fdn, StandardTags.RootTag.class);
        instrumenter.attachListener(functionBuilder.build(), fser);
        // Next attach statement handler to all STATEMENTs except LOOPs
        SourceSectionFilter.Builder statementBuilder = RInstrumentation.createFunctionStatementFilter(fdn);
        statementBuilder.tagIsNot(RSyntaxTags.LoopTag.class);
        instrumenter.attachListener(statementBuilder.build(), fser.getStatementListener());
        // Finally attach loop listeners to all loop nodes
        SourceSectionFilter.Builder loopBuilder = RInstrumentation.createFunctionFilter(fdn, RSyntaxTags.LoopTag.class);
        new RSyntaxVisitor<Void>() {

            @Override
            protected Void visit(RSyntaxCall element) {
                if (element instanceof AbstractLoopNode) {
                    instrumenter.attachListener(loopBuilder.build(), fser.getLoopStatementReceiver((AbstractLoopNode) element));
                }
                accept(element.getSyntaxLHS());
                for (RSyntaxElement arg : element.getSyntaxArguments()) {
                    accept(arg);
                }
                return null;
            }

            @Override
            protected Void visit(RSyntaxConstant element) {
                return null;
            }

            @Override
            protected Void visit(RSyntaxLookup element) {
                return null;
            }

            @Override
            protected Void visit(RSyntaxFunction element) {
                accept(element.getSyntaxBody());
                return null;
            }
        }.accept(fdn);
        return fser;
    }

    private static FunctionStatementsEventListener ensureSingleStep(FunctionDefinitionNode fdn) {
        FunctionStatementsEventListener fser = getFunctionStatementsEventListener(fdn);
        if (fser == null) {
            // attach a "once" listener
            fser = attachDebugHandler(fdn, null, null, true, true);
        } else {
            if (fser.disabled()) {
                fser.enable();
                // record initial state was disabled for undo
                fser.enabledForStepInto = true;
            }
        }
        return fser;
    }

    private abstract static class DebugEventListener implements ExecutionEventListener {

        protected final Object text;
        protected final Object condition;
        protected final FunctionDefinitionNode functionDefinitionNode;
        protected EventBinding<StepIntoInstrumentListener> stepIntoInstrument;
        @CompilationFinal private boolean disabled;
        CyclicAssumption disabledUnchangedAssumption = new CyclicAssumption("debug event disabled state unchanged");

        @Child private BrowserInteractNode browserInteractNode = BrowserInteractNodeGen.create();

        protected DebugEventListener(FunctionDefinitionNode functionDefinitionNode, Object text, Object condition) {
            this.text = text;
            this.condition = condition;
            this.functionDefinitionNode = functionDefinitionNode;
        }

        @Override
        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }

        boolean disabled() {
            return disabled || RContext.getInstance().stateInstrumentation.debugGloballyDisabled();
        }

        void disable() {
            setDisabledState(true);
        }

        void enable() {
            setDisabledState(false);
        }

        private void setDisabledState(boolean newState) {
            if (newState != disabled) {
                disabledUnchangedAssumption.invalidate();
                disabled = newState;
            }
        }

        @TruffleBoundary
        protected static void print(String msg, boolean nl) {
            try {
                StdConnections.getStdout().writeString(msg, nl);
            } catch (IOException ex) {
                throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, ex.getMessage());
            }
        }

        protected void browserInteract(Node node, VirtualFrame frame) {
            int exitMode = (int) browserInteractNode.execute(frame);
            switch (exitMode) {
                case BrowserInteractNode.NEXT:
                    break;
                case BrowserInteractNode.STEP:
                    if (this instanceof StatementEventListener) {
                        /*
                         * We have no idea what function will be called but we have to stop there
                         * whatever so we create a filter for all START_METHOD tags with a
                         * StepIntoInstrumentListener which will then get undone when it is entered,
                         * so hopefully only the one function will actually get instrumented - but
                         * will everything get invalidated?
                         */
                        attachStepInto();
                    }
                    break;
                case BrowserInteractNode.CONTINUE:
                    // Have to disable
                    doContinue();
                    clearStepInstrument();
                    break;
                case BrowserInteractNode.FINISH:
                    // If in loop, continue to loop end, else act like CONTINUE
                    AbstractLoopNode loopNode = inLoop(node);
                    if (loopNode != null) {
                        // Have to disable just the body of the loop
                        FunctionStatementsEventListener fser = getFunctionStatementsEventListener(functionDefinitionNode);
                        fser.setFinishing(loopNode);
                    } else {
                        doContinue();
                    }
                    clearStepInstrument();
            }
        }

        @TruffleBoundary
        private void attachStepInto() {
            FunctionStatementsEventListener parentListener = getFunctionStatementsEventListener(functionDefinitionNode);
            parentListener.stepIntoInstrument = RInstrumentation.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(StandardTags.RootTag.class).build(),
                            new StepIntoInstrumentListener(parentListener));

        }

        private void doContinue() {
            FunctionStatementsEventListener fser = getFunctionStatementsEventListener(functionDefinitionNode);
            fser.setContinuing();
        }

        @TruffleBoundary
        protected void clearStepInstrument() {
            if (stepIntoInstrument != null) {
                stepIntoInstrument.dispose();
                stepIntoInstrument = null;
            }
        }
    }

    /**
     * This handles function entry and exit. We try to emulate GnuR behavior but since FastR does
     * not (yet) handle <@code {</code> correctly, it is a bit heuristic. In particular, if a
     * function is defined using <@code { }</code>, GnuR stops at the <@code {</code> and then
     * "steps over" the <@code {</code> to the first statement, otherwise it just stops at the first
     * statement.
     */
    private static class FunctionStatementsEventListener extends DebugEventListener {

        private final StatementEventListener statementListener;
        ArrayList<LoopStatementEventListener> loopStatementListeners = new ArrayList<>();

        /**
         * Denotes the {@code debugOnce} function, debugging disabled after one execution, or a
         * handler established temporarily for step-into.
         */
        private final boolean once;
        /**
         * Denotes that this was installed by an explicit call to {@code browser()} on an otherwise
         * undebugged function. {@code assert once == true}.
         */
        private final boolean implicit;
        /**
         * Records whether a permanent handler was (temporarily) enabled for a step-into.
         *
         */
        private boolean enabledForStepInto;
        private boolean continuing;

        FunctionStatementsEventListener(FunctionDefinitionNode functionDefinitionNode, Object text, Object condition, boolean once, boolean implicit) {
            super(functionDefinitionNode, text, condition);
            RContext.getInstance().stateInstrumentation.putDebugListener(functionDefinitionNode.getSourceSection(), this);
            statementListener = new StatementEventListener(functionDefinitionNode, text, condition);
            this.once = once;
            this.implicit = implicit;
        }

        StatementEventListener getStatementListener() {
            return statementListener;
        }

        LoopStatementEventListener getLoopStatementReceiver(RSyntaxNode loopNode) {
            LoopStatementEventListener lser = new LoopStatementEventListener(functionDefinitionNode, text, condition, loopNode, this);
            loopStatementListeners.add(lser);
            return lser;
        }

        @Override
        void disable() {
            super.disable();
            statementListener.disable();
            for (LoopStatementEventListener lser : loopStatementListeners) {
                lser.disable();
            }
        }

        @Override
        void enable() {
            super.enable();
            enableChildren();
        }

        void enableChildren() {
            statementListener.enable();
            for (LoopStatementEventListener lser : loopStatementListeners) {
                lser.enable();
            }
        }

        void setContinuing() {
            continuing = true;
            statementListener.disable();
            for (LoopStatementEventListener lser : loopStatementListeners) {
                lser.disable();
            }
        }

        void setFinishing(AbstractLoopNode loopNode) {
            // Disable every statement listener except that for loopNode
            for (LoopStatementEventListener lser : loopStatementListeners) {
                if (lser.getLoopNode() == loopNode) {
                    lser.setFinishing();
                } else {
                    lser.disable();
                }
            }
            statementListener.disable();
        }

        void endFinishing() {
            enableChildren();
        }

        @Override
        public void onEnter(EventContext context, VirtualFrame frame) {
            if (!disabled()) {
                CompilerDirectives.transferToInterpreter();
                print("debugging in: ", false);
                printCall(frame);
                /*
                 * If this is a recursive call, then returnCleanup will not have happened, so we
                 * enable our child listeners unconditionally. TODO It is possible that the enabled
                 * state should be stacked to match the call stack in the recursive case.
                 */
                enableChildren();
                printNode(context.getInstrumentedNode(), true);
                browserInteract(context.getInstrumentedNode(), frame);
            }
        }

        @Override
        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            if (!disabled()) {
                CompilerDirectives.transferToInterpreter();
                returnCleanup(frame, false);
            }
        }

        @Override
        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
            if (!disabled()) {
                CompilerDirectives.transferToInterpreter();
                returnCleanup(frame, exception instanceof JumpToTopLevelException);
            }
        }

        private void returnCleanup(VirtualFrame frame, boolean jumpToTopLevel) {
            if (!implicit && !once && !jumpToTopLevel) {
                print("exiting from: ", false);
                printCall(frame);
            }
            if (once || enabledForStepInto) {
                disable();
            } else if (continuing) {
                statementListener.enable();
                for (LoopStatementEventListener lser : loopStatementListeners) {
                    lser.enable();
                }
                continuing = false;
            }
        }

        private static void printCall(VirtualFrame frame) {
            String callString = RContext.getRRuntimeASTAccess().getCallerSource(RArguments.getCall(frame));
            print(callString, true);
        }
    }

    @TruffleBoundary
    private static void printNode(Node node, boolean startFunction) {
        ConsoleHandler consoleHandler = RContext.getInstance().getConsoleHandler();
        /*
         * N.B. It would seem that GnuR does a deparse that because, e.g., a function that ends with
         * } without a preceding newline prints with one and indentation is standardized.
         */
        RBaseNode rNode = (RBaseNode) node;
        boolean curly = RSyntaxCall.isCallTo((RSyntaxElement) node, "{");

        if (startFunction && !curly) {
            consoleHandler.print("debug: ");
        } else {
            SourceSection source = ((RBaseNode) node).asRSyntaxNode().getSourceSection();
            String path = RSource.getPath(source.getSource());
            if (path == null) {
                path = "";
            }
            consoleHandler.print("debug at " + path + "#" + source.getStartLine() + ": ");
        }
        consoleHandler.print(RDeparse.deparseSyntaxElement(rNode.asRSyntaxNode()));
        consoleHandler.print("\n");
    }

    private static class StatementEventListener extends DebugEventListener {

        StatementEventListener(FunctionDefinitionNode functionDefinitionNode, Object text, Object condition) {
            super(functionDefinitionNode, text, condition);
        }

        @Override
        public void onEnter(EventContext context, VirtualFrame frame) {
            if (!disabled()) {
                CompilerDirectives.transferToInterpreter();
                // in case we did a step into that never called a function
                clearStepInstrument();
                RBaseNode node = (RBaseNode) context.getInstrumentedNode();
                if (node.isTaggedWith(StandardTags.RootTag.class)) {
                    // already handled
                    return;
                }
                printNode(node, false);
                browserInteract(node, frame);
            }
        }

        @Override
        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
        }
    }

    /**
     * Handles the loop header and there is one instance registered for each loop.
     */
    private static class LoopStatementEventListener extends StatementEventListener {

        private boolean finishing;
        /**
         * The wrapper for the loop node is stable whereas the loop node itself will be replaced
         * with a specialized node.
         */
        private final RSyntaxNode loopNode;
        private final FunctionStatementsEventListener fser;

        LoopStatementEventListener(FunctionDefinitionNode functionDefinitionNode, Object text, Object condition, RSyntaxNode loopNode, FunctionStatementsEventListener fser) {
            super(functionDefinitionNode, text, condition);
            this.loopNode = loopNode;
            this.fser = fser;
        }

        @Override
        public void onEnter(EventContext context, VirtualFrame frame) {
            if (!disabled() && context.getInstrumentedNode() == loopNode) {
                super.onEnter(context, frame);
            }
        }

        RSyntaxNode getLoopNode() {
            return loopNode;
        }

        void setFinishing() {
            finishing = true;
        }

        @Override
        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
            if (!disabled() && context.getInstrumentedNode() == loopNode) {
                CompilerDirectives.transferToInterpreter();
                returnCleanup();
            }
        }

        @Override
        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            if (!disabled() && context.getInstrumentedNode() == loopNode) {
                CompilerDirectives.transferToInterpreter();
                returnCleanup();
            }
        }

        private void returnCleanup() {
            if (finishing) {
                finishing = false;
                fser.endFinishing();
            }
        }
    }

    private static AbstractLoopNode inLoop(final Node nodeArg) {
        Node node = nodeArg;
        while (!(node instanceof RootNode)) {
            node = node.getParent();
            if (node instanceof AbstractLoopNode) {
                return (AbstractLoopNode) node;
            }
        }
        return null;
    }

    /**
     * Listener for (transient) step into.
     */
    private static class StepIntoInstrumentListener implements ExecutionEventListener {
        private final FunctionStatementsEventListener functionStatementsEventListener;

        StepIntoInstrumentListener(FunctionStatementsEventListener debugEventReceiver) {
            this.functionStatementsEventListener = debugEventReceiver;
        }

        @Override
        public void onEnter(EventContext context, VirtualFrame frame) {
            if (!RContext.getInstance().stateInstrumentation.debugGloballyDisabled()) {
                CompilerDirectives.transferToInterpreter();
                FunctionDefinitionNode fdn = (FunctionDefinitionNode) context.getInstrumentedNode().getRootNode();
                FunctionStatementsEventListener ensureSingleStep = ensureSingleStep(fdn);

                functionStatementsEventListener.clearStepInstrument();
                ensureSingleStep.onEnter(context, frame);
            }
        }

        @Override
        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
        }

        @Override
        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }
    }
}
