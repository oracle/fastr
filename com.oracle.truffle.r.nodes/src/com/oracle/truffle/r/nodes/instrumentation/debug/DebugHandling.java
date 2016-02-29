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
package com.oracle.truffle.r.nodes.instrumentation.debug;

import java.io.IOException;
import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.instrumentation.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.instrumentation.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.instrument.Browser;
import com.oracle.truffle.r.runtime.nodes.*;

/**
 * The implementation of the R debug functions.
 *
 * When a function is enabled for debugging a set of {@link DebugEventListener}s are created and
 * attached to key nodes in the AST body associated with the {@link FunctionDefinitionNode}
 * corresponding to the {@link RFunction} instance.
 *
 * Three different listener classes are defined:
 * <ul>
 * <li>{@link FunctionStatementsEventListener}: attaches to {@link FunctionStatementsNode} and
 * handles the special behavior on entry/exit</li>
 * <li>{@link StatementEventListener}: attaches to all {@link RSyntaxTags#STATEMENT} nodes and
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
 * functions and the {@link RSyntaxTags#ENTER_FUNCTION} tag</li>
 * <li>On entry to that listener instrument/enable the function we have entered (if necessary) for
 * one-time (unless already)</li>
 * <li>Dispose the {@link StepIntoInstrumentListener} and continue, which will then stop at the
 * {@link FunctionStatementsNode}.</li>
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
     * Records all functions that have debug listeners installed.
     */
    private static final WeakHashMap<FunctionUID, FunctionStatementsEventListener> listenerMap = new WeakHashMap<>();

    /**
     * This flag is used to (temporarily) disable all debugging across calls that are used
     * internally in the implementation.
     */
    private static boolean globalDisable;

    /**
     * Attach the DebugHandling instrument to the FunctionStatementsNode and all syntactic nodes.
     */
    public static boolean enableDebug(RFunction func, Object text, Object condition, boolean once) {
        FunctionDefinitionNode fdn = (FunctionDefinitionNode) func.getRootNode();
        FunctionStatementsEventListener fbr = listenerMap.get(fdn.getUID());
        if (fbr == null) {
            attachDebugHandler(func, text, condition, once);
        } else {
            fbr.enable();
        }
        return true;
    }

    public static boolean undebug(RFunction func) {
        FunctionStatementsEventListener fbr = listenerMap.get(((FunctionDefinitionNode) func.getRootNode()).getUID());
        if (fbr == null) {
            return false;
        } else {
            fbr.disable();
            return true;
        }
    }

    public static boolean isDebugged(RFunction func) {
        FunctionStatementsEventListener fser = listenerMap.get(((FunctionDefinitionNode) func.getRootNode()).getUID());
        return fser != null && !fser.disabled();
    }

    /**
     * Disables/enables debugging globally. Intended to be used for short period, typically while
     * executing functions used internally by the implementation.
     *
     * @param disable {@code true} to disable, {@code false} to enable.
     * @return the current value (default {@code false}.
     */
    public static boolean globalDisable(boolean disable) {
        boolean current = globalDisable;
        globalDisable = disable;
        return current;
    }

    private static void attachDebugHandler(RFunction func, Object text, Object condition, boolean once) {
        attachDebugHandler(RInstrumentation.getFunctionDefinitionNode(func), text, condition, once);
    }

    private static FunctionStatementsEventListener attachDebugHandler(FunctionDefinitionNode fdn, Object text, Object condition, boolean once) {
        FunctionStatementsEventListener fser = new FunctionStatementsEventListener(fdn, text, condition, once);
        // First attach the main listener on the START_FUNCTION
        Instrumenter instrumenter = RInstrumentation.getInstrumenter();
        SourceSectionFilter.Builder functionBuilder = RInstrumentation.createFunctionFilter(fdn, RSyntaxTags.START_FUNCTION);
        instrumenter.attachListener(functionBuilder.build(), fser);
        // Next attach statement handler to all STATEMENTs except LOOPs
        SourceSectionFilter.Builder statementBuilder = RInstrumentation.createFunctionStatementFilter(fdn);
        statementBuilder.tagIsNot(RSyntaxTags.LOOP);
        instrumenter.attachListener(statementBuilder.build(), fser.getStatementListener());
        // Finally attach loop listeners to all loop nodes
        SourceSectionFilter.Builder loopBuilder = RInstrumentation.createFunctionFilter(fdn, RSyntaxTags.LOOP);
        RSyntaxNode.accept(fdn, 0, new RSyntaxNodeVisitor() {

            public boolean visit(RSyntaxNode node, int depth) {
                SourceSection ss = node.getSourceSection();
                if (ss.hasTag(RSyntaxTags.LOOP)) {
                    instrumenter.attachListener(loopBuilder.build(), fser.getLoopStatementReceiver(node));
                }
                return true;
            }

        }, false);
        return fser;
    }

    private static void ensureSingleStep(FunctionDefinitionNode fdn) {
        FunctionStatementsEventListener fser = listenerMap.get(fdn.getUID());
        if (fser == null) {
            // attach a "once" listener
            fser = attachDebugHandler(fdn, null, null, true);
        } else {
            if (fser.disabled()) {
                fser.enable();
                // record initial state was disabled for undo
                fser.enabledForStepInto = true;
            }
        }
    }

    private abstract static class DebugEventListener implements ExecutionEventListener {

        protected final Object text;
        protected final Object condition;
        protected final FunctionDefinitionNode functionDefinitionNode;
        protected EventBinding<StepIntoInstrumentListener> stepIntoInstrument;
        @CompilationFinal private boolean disabled;
        CyclicAssumption disabledUnchangedAssumption = new CyclicAssumption("debug event disabled state unchanged");

        protected DebugEventListener(FunctionDefinitionNode functionDefinitionNode, Object text, Object condition) {
            this.text = text;
            this.condition = condition;
            this.functionDefinitionNode = functionDefinitionNode;
        }

        @Override
        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }

        boolean disabled() {
            return disabled || globalDisable;
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

        protected static void print(String msg, boolean nl) {
            try {
                StdConnections.getStdout().writeString(msg, nl);
            } catch (IOException ex) {
                throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, ex.getMessage());
            }
        }

        protected void browserInteract(Node node, VirtualFrame frame) {
            Browser.ExitMode exitMode = Browser.interact(frame.materialize());
            switch (exitMode) {
                case NEXT:
                    break;
                case STEP:
                    if (this instanceof StatementEventListener) {
                        /*
                         * We have no idea what function will be called but we have to stop there
                         * whatever so we create a filter for all START_METHOD tags with a
                         * StepIntoInstrumentListener which will then get undone when it is entered,
                         * so hopefully only the one function will actually get instrumented - but
                         * will everything get invalidated?
                         */
                        stepIntoInstrument = RInstrumentation.getInstrumenter().attachListener(SourceSectionFilter.newBuilder().tagIs(RSyntaxTags.ENTER_FUNCTION).build(),
                                        new StepIntoInstrumentListener(this));
                    }
                    break;
                case CONTINUE:
                    // Have to disable
                    doContinue();
                    clearStepInstrument();
                    break;
                case FINISH:
                    // If in loop, continue to loop end, else act like CONTINUE
                    AbstractLoopNode loopNode = inLoop(node);
                    if (loopNode != null) {
                        // Have to disable just the body of the loop
                        FunctionStatementsEventListener fser = listenerMap.get(functionDefinitionNode.getUID());
                        fser.setFinishing(loopNode);
                    } else {
                        doContinue();
                    }
                    clearStepInstrument();
            }
        }

        private void doContinue() {
            FunctionStatementsEventListener fser = listenerMap.get(functionDefinitionNode.getUID());
            fser.setContinuing();
        }

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
         * Records whether a permanent handler was (temporarily) enabled for a step-into.
         *
         */
        private boolean enabledForStepInto;
        private boolean continuing;

        FunctionStatementsEventListener(FunctionDefinitionNode functionDefinitionNode, Object text, Object condition, boolean once) {
            super(functionDefinitionNode, text, condition);
            listenerMap.put(functionDefinitionNode.getUID(), this);
            statementListener = new StatementEventListener(functionDefinitionNode, text, condition);
            this.once = once;
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
            for (LoopStatementEventListener lser : loopStatementListeners) {
                lser.enable();
            }
            statementListener.enable();
        }

        @Override
        public void onEnter(EventContext context, VirtualFrame frame) {
            if (!disabled()) {
                print("debugging in: ", false);
                printCall(frame);
                FunctionDefinitionNode fdn = (FunctionDefinitionNode) RArguments.getFunction(frame).getRootNode();
                boolean brace = fdn.hasBraces();
                if (brace) {
                    printNode(context.getInstrumentedNode(), brace);
                    browserInteract(context.getInstrumentedNode(), frame);
                }
            }
        }

        @Override
        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            if (!disabled()) {
                returnCleanup(frame);
            }
        }

        @Override
        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
            if (!disabled()) {
                returnCleanup(frame);
            }
        }

        private void returnCleanup(VirtualFrame frame) {
            print("exiting from: ", false);
            printCall(frame);
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

    private static void printNode(Node node, boolean curly) {
        ConsoleHandler consoleHandler = RContext.getInstance().getConsoleHandler();
        RDeparse.State state = RDeparse.State.createPrintableState();
        ((RBaseNode) node).deparse(state);
        consoleHandler.print("debug: ");
        if (curly) {
            consoleHandler.println("{");
        }
        consoleHandler.print(state.toString());
        if (curly) {
            consoleHandler.print("}");
        }
        consoleHandler.print("\n");
    }

    private static class StatementEventListener extends DebugEventListener {

        StatementEventListener(FunctionDefinitionNode functionDefinitionNode, Object text, Object condition) {
            super(functionDefinitionNode, text, condition);
        }

        @Override
        public void onEnter(EventContext context, VirtualFrame frame) {
            if (!disabled()) {
                // in case we did a step into that never called a function
                clearStepInstrument();
                printNode(context.getInstrumentedNode(), false);
                browserInteract(context.getInstrumentedNode(), frame);
            }
        }

        @Override
        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
        }

    }

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
            if (!disabled()) {
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
            if (!disabled()) {
                returnCleanup();
            }
        }

        @Override
        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            if (!disabled()) {
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
        private DebugEventListener debugEventReceiver;

        StepIntoInstrumentListener(DebugEventListener debugEventReceiver) {
            this.debugEventReceiver = debugEventReceiver;
        }

        @Override
        public void onEnter(EventContext context, VirtualFrame frame) {
            if (!globalDisable) {
                FunctionBodyNode functionBodyNode = (FunctionBodyNode) context.getInstrumentedNode();
                FunctionDefinitionNode fdn = (FunctionDefinitionNode) functionBodyNode.getRootNode();
                ensureSingleStep(fdn);
                debugEventReceiver.clearStepInstrument();
                // next stop will be the FunctionStatementsNode
            }
        }

        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
        }

        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }

    }

}
