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
package com.oracle.truffle.r.nodes.instrument.debug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.WeakHashMap;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.Instrumenter;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.ProbeInstrument;
import com.oracle.truffle.api.instrument.StandardBeforeInstrumentListener;
import com.oracle.truffle.api.instrument.StandardInstrumentListener;
import com.oracle.truffle.api.instrument.StandardSyntaxTag;
import com.oracle.truffle.api.instrument.TagInstrument;
import com.oracle.truffle.api.instrument.WrapperNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import com.oracle.truffle.r.nodes.control.AbstractLoopNode;
import com.oracle.truffle.r.nodes.function.FunctionBodyNode;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.function.FunctionStatementsNode;
import com.oracle.truffle.r.nodes.instrument.RInstrument;
import com.oracle.truffle.r.nodes.instrument.RSyntaxTag;
import com.oracle.truffle.r.runtime.FunctionUID;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.context.ConsoleHandler;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.instrument.Browser;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * The implementation of the R debug functions.
 *
 * When a function is enabled for debugging a set of {@link DebugEventReceiver}s are created and
 * associated with {@link ProbeInstrument}s and attached to key nodes in the AST body associated
 * with the {@link FunctionDefinitionNode} corresponding to the {@link RFunction} instance.
 *
 * Two different receiver classes are defined:
 * <ul>
 * <li>{@link FunctionStatementsEventReceiver}: attaches to {@link FunctionStatementsNode} and
 * handles the special behavior on entry/exit</li>
 * <li>{@link StatementEventReceiver}: attaches to all {@link StandardSyntaxTag#STATEMENT} nodes and
 * handles "n" and "s" browser commands</li>
 * <li>{@link LoopStatementEventReceiver}: attaches to {@link AbstractLoopNode} instances and
 * handles special "f" command behavior.
 * </ul>
 * <p>
 * Step Into is slightly tricky because, at the point the command is issued, we do not know what
 * function the call will resolve to. There are two solutions to this:
 * <ul>
 * <li></li>Use the global trap mechanism of the instrumentation framework to force entry to
 * <b>any</b> function. This is enabled on a step into command and immediately disabled on taking
 * the trap (cf hardware single step).
 * <li></li>A callback from the {@code execute} method of {@link FunctionDefinitionNode}, which acts
 * as if {@code debugonce} had been called by the user (unless debug was already enabled in which
 * case there is nothing to do). This has been prototyped but it is not clear it provides sufficient
 * value for the added complexity..
 * <p>
 * When invoked from within a loop The "f" command continues the loop body without entry and the
 * re-enables entry. This is handled by creating a {@link LoopStatementEventReceiver} per
 * {@link AbstractLoopNode}. On a "f" every receiver <b>except</b> the one associated with that loop
 * is disabled. On return from the loop, everything is re-enabled.
 * <p>
 * Currently, {@code debugonce} and {@code undebug} are handled by disabling the receiver behavior.
 * Any change in enabled state is managed by an {@link Assumption} which will invalidate the code of
 * the receiver. In the case where events are disabled there should be no compilation overhead from
 * the receivers.
 */
public class DebugHandling {

    /**
     * Records all functions that have debug receivers installed.
     */
    private static final WeakHashMap<FunctionUID, FunctionStatementsEventReceiver> receiverMap = new WeakHashMap<>();

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
        FunctionStatementsEventReceiver fbr = receiverMap.get(fdn.getUID());
        if (fbr == null) {
            Probe probe = attachDebugHandler(fdn, text, condition, once);
            return probe != null;
        } else {
            fbr.enable();
            return true;
        }
    }

    public static boolean undebug(RFunction func) {
        FunctionStatementsEventReceiver fbr = receiverMap.get(((FunctionDefinitionNode) func.getRootNode()).getUID());
        if (fbr == null) {
            return false;
        } else {
            fbr.disable();
            return true;
        }
    }

    public static boolean isDebugged(RFunction func) {
        FunctionStatementsEventReceiver fser = receiverMap.get(((FunctionDefinitionNode) func.getRootNode()).getUID());
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

    private static Probe findStartMethodProbe(FunctionDefinitionNode fdn) {
        return RInstrument.findSingleProbe(fdn.getUID(), StandardSyntaxTag.START_METHOD);
    }

    private static Probe attachDebugHandler(FunctionDefinitionNode fdn, Object text, Object condition, boolean once) {
        Probe probe = findStartMethodProbe(fdn);
        if (probe == null) {
            return null;
        }
        FunctionStatementsEventReceiver fser = new FunctionStatementsEventReceiver(fdn, text, condition, once);
        Instrumenter instrumenter = RInstrument.getInstrumenter();
        instrumenter.attach(probe, fser, "debug");
        attachToStatementNodes(fser, instrumenter);
        return probe;
    }

    private static void ensureSingleStep(FunctionDefinitionNode fdn) {
        FunctionStatementsEventReceiver fbr = receiverMap.get(fdn.getUID());
        if (fbr == null) {
            attachDebugHandler(fdn, null, null, true);
        } else {
            if (fbr.disabled()) {
                fbr.enable();
            }
        }
    }

    private static void attachToStatementNodes(FunctionStatementsEventReceiver functionStatementsEventReceiver, Instrumenter instrumenter) {
        functionStatementsEventReceiver.getFunctionDefinitionNode().getBody().asNode().accept(new NodeVisitor() {
            public boolean visit(Node node) {
                if (node instanceof WrapperNode) {
                    WrapperNode wrapper = (WrapperNode) node;
                    Probe probe = wrapper.getProbe();
                    if (probe.isTaggedAs(StandardSyntaxTag.STATEMENT)) {
                        Node child = wrapper.getChild();
                        if (child instanceof AbstractLoopNode) {
                            instrumenter.attach(probe, functionStatementsEventReceiver.getLoopStatementReceiver(wrapper), "debug:loop");
                        } else {
                            instrumenter.attach(probe, functionStatementsEventReceiver.getStatementReceiver(), "debug:stmt");
                        }
                    }
                }
                return true;
            }
        });
    }

    private abstract static class DebugEventReceiver implements StandardInstrumentListener {

        protected final Object text;
        protected final Object condition;
        protected final FunctionDefinitionNode functionDefinitionNode;
        protected TagInstrument stepIntoInstrument;
        @CompilationFinal private boolean disabled;
        CyclicAssumption disabledUnchangedAssumption = new CyclicAssumption("debug event disabled state unchanged");

        protected DebugEventReceiver(FunctionDefinitionNode functionDefinitionNode, Object text, Object condition) {
            this.text = text;
            this.condition = condition;
            this.functionDefinitionNode = functionDefinitionNode;
        }

        FunctionDefinitionNode getFunctionDefinitionNode() {
            return functionDefinitionNode;
        }

        @Override
        public void onReturnVoid(Probe probe, Node node, VirtualFrame frame) {
            if (!disabled()) {
                throw RInternalError.shouldNotReachHere();
            }
        }

        @Override
        public void onReturnExceptional(Probe probe, Node node, VirtualFrame frame, Throwable exception) {
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
                    if (this instanceof StatementEventReceiver) {
                        stepIntoInstrument = RInstrument.getInstrumenter().attach(RSyntaxTag.FUNCTION_BODY, new StepIntoInstrumentListener(this), "step");
                    }
                    break;
                case CONTINUE:
                    // Have to disable
                    doContinue();
                    clearTrap();
                    break;
                case FINISH:
                    // If in loop, continue to loop end, else act like CONTINUE
                    AbstractLoopNode loopNode = inLoop(node);
                    if (loopNode != null) {
                        // Have to disable just the body of the loop
                        FunctionStatementsEventReceiver fser = receiverMap.get(functionDefinitionNode.getUID());
                        fser.setFinishing(loopNode);
                    } else {
                        doContinue();
                    }
                    clearTrap();
            }
        }

        private void doContinue() {
            FunctionStatementsEventReceiver fser = receiverMap.get(functionDefinitionNode.getUID());
            fser.setContinuing();
        }

        protected void clearTrap() {
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
    private static class FunctionStatementsEventReceiver extends DebugEventReceiver {

        private final StatementEventReceiver statementReceiver;
        ArrayList<LoopStatementEventReceiver> loopStatementReceivers = new ArrayList<>();

        private boolean once;
        private boolean continuing;

        FunctionStatementsEventReceiver(FunctionDefinitionNode functionDefinitionNode, Object text, Object condition, boolean once) {
            super(functionDefinitionNode, text, condition);
            receiverMap.put(functionDefinitionNode.getUID(), this);
            statementReceiver = new StatementEventReceiver(functionDefinitionNode, text, condition);
            this.once = once;
        }

        StatementEventReceiver getStatementReceiver() {
            return statementReceiver;
        }

        LoopStatementEventReceiver getLoopStatementReceiver(WrapperNode loopNodeWrapper) {
            LoopStatementEventReceiver lser = new LoopStatementEventReceiver(functionDefinitionNode, text, condition, loopNodeWrapper, this);
            loopStatementReceivers.add(lser);
            return lser;
        }

        @Override
        void disable() {
            super.disable();
            statementReceiver.disable();
            for (LoopStatementEventReceiver lser : loopStatementReceivers) {
                lser.disable();
            }
        }

        @Override
        void enable() {
            super.enable();
            enableChildren();
        }

        void enableChildren() {
            statementReceiver.enable();
            for (LoopStatementEventReceiver lser : loopStatementReceivers) {
                lser.enable();
            }
        }

        void setContinuing() {
            continuing = true;
            statementReceiver.disable();
            for (LoopStatementEventReceiver lser : loopStatementReceivers) {
                lser.disable();
            }
        }

        void setFinishing(AbstractLoopNode loopNode) {
            // Disable every statement receiver except that for loopNode
            WrapperNode loopNodeWrapper = (WrapperNode) loopNode.getParent();
            for (LoopStatementEventReceiver lser : loopStatementReceivers) {
                if (lser.getLoopNodeWrapper() == loopNodeWrapper) {
                    lser.setFinishing();
                } else {
                    lser.disable();
                }
            }
            statementReceiver.disable();
        }

        void endFinishing() {
            enableChildren();
        }

        @Override
        public void onEnter(Probe probe, Node node, VirtualFrame frame) {
            if (!disabled()) {
                print("debugging in: ", false);
                printCall(frame);
                FunctionDefinitionNode fdn = (FunctionDefinitionNode) RArguments.getFunction(frame).getRootNode();
                /*
                 * If this is a recursive call, then returnCleanup will not have happened, so we
                 * enable our child listeners unconditionally. TODO It is possible that the enabled
                 * state should be stacked to match the call stack in the recursive case.
                 */
                enableChildren();
                boolean brace = fdn.hasBraces();
                if (brace) {
                    printNode(node, brace);
                    browserInteract(node, frame);
                }
            }
        }

        @Override
        public void onReturnValue(Probe probe, Node node, VirtualFrame frame, Object result) {
            if (!disabled()) {
                returnCleanup(frame);
            }
        }

        @Override
        public void onReturnExceptional(Probe probe, Node node, VirtualFrame frame, Throwable exception) {
            if (!disabled()) {
                returnCleanup(frame);
            }
        }

        private void returnCleanup(VirtualFrame frame) {
            print("exiting from: ", false);
            printCall(frame);
            if (once) {
                disable();
            } else if (continuing) {
                statementReceiver.enable();
                for (LoopStatementEventReceiver lser : loopStatementReceivers) {
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

    private static class StatementEventReceiver extends DebugEventReceiver {

        StatementEventReceiver(FunctionDefinitionNode functionDefinitionNode, Object text, Object condition) {
            super(functionDefinitionNode, text, condition);
        }

        @Override
        public void onEnter(Probe probe, Node node, VirtualFrame frame) {
            if (!disabled()) {
                // in case we did a step into that never called a function
                clearTrap();
                printNode(node, false);
                browserInteract(node, frame);
            }
        }

        @Override
        public void onReturnValue(Probe probe, Node node, VirtualFrame frame, Object result) {
        }

    }

    private static class LoopStatementEventReceiver extends StatementEventReceiver {

        private boolean finishing;
        /**
         * The wrapper for the loop node is stable whereas the loop node itself will be replaced
         * with a specialized node.
         */
        private final WrapperNode loopNodeWrapper;
        private final FunctionStatementsEventReceiver fser;

        LoopStatementEventReceiver(FunctionDefinitionNode functionDefinitionNode, Object text, Object condition, WrapperNode loopNodeWrapper, FunctionStatementsEventReceiver fser) {
            super(functionDefinitionNode, text, condition);
            this.loopNodeWrapper = loopNodeWrapper;
            this.fser = fser;
        }

        @Override
        public void onEnter(Probe probe, Node node, VirtualFrame frame) {
            if (!disabled()) {
                super.onEnter(probe, node, frame);
            }
        }

        WrapperNode getLoopNodeWrapper() {
            return loopNodeWrapper;
        }

        void setFinishing() {
            finishing = true;
        }

        @Override
        public void onReturnExceptional(Probe probe, Node node, VirtualFrame frame, Throwable exception) {
            if (!disabled()) {
                returnCleanup();
            }
        }

        @Override
        public void onReturnValue(Probe probe, Node node, VirtualFrame frame, Object result) {
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
    private static class StepIntoInstrumentListener implements StandardBeforeInstrumentListener {
        private DebugEventReceiver debugEventReceiver;

        StepIntoInstrumentListener(DebugEventReceiver debugEventReceiver) {
            this.debugEventReceiver = debugEventReceiver;
        }

        @Override
        public void onEnter(Probe probe, Node node, VirtualFrame frame) {
            if (!globalDisable) {
                FunctionBodyNode functionBodyNode = (FunctionBodyNode) node;
                FunctionDefinitionNode fdn = (FunctionDefinitionNode) functionBodyNode.getRootNode();
                ensureSingleStep(fdn);
                debugEventReceiver.clearTrap();
                // next stop will be the START_METHOD node
            }
        }

    }

}
