/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.Instrument;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.ProbeNode;
import com.oracle.truffle.api.instrument.ProbeNode.WrapperNode;
import com.oracle.truffle.api.instrument.StandardSyntaxTag;
import com.oracle.truffle.api.instrument.SyntaxTag;
import com.oracle.truffle.api.instrument.SyntaxTagTrap;
import com.oracle.truffle.api.instrument.TruffleEventReceiver;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.control.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import com.oracle.truffle.r.nodes.function.FunctionBodyNode;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.function.FunctionStatementsNode;
import com.oracle.truffle.r.nodes.function.FunctionUID;
import com.oracle.truffle.r.nodes.instrument.RInstrument;
import com.oracle.truffle.r.nodes.instrument.RSyntaxTag;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.RFunction;

import java.util.*;

/**
 * The implementation of the R debug functions.
 *
 * When a function is enabled for debugging a set of {@link DebugEventReceiver}s are created and
 * associated with {@link Instrument}s and attached to key nodes in the AST body associated with the
 * {@link FunctionDefinitionNode} corresponding to the {@link RFunction} instance.
 *
 * Two different receiver classes are defined:
 * <ul>
 * <li>{@link FunctionStatementsEventReceiver}: attaches to {@link FunctionStatementsNode} and
 * handles the special behavior on entry/exit</li>
 * <li>{@link StatementEventReceiver}: attaches to all {@link StandardSyntaxTag#STATEMENT} nodes and
 * handles "n" and "s" browser commands</li>
 * <li>{@link LoopStatementEventReceiver}: attaches to {@link LoopNode} instances and handles
 * special "f" command behavior.
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
 * {@link LoopNode}. On a "f" every receiver <b>except</b> the one associated with that loop is
 * disabled. On return from the loop, everything is re-enabled.
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

    private static Probe findStartMethodProbe(FunctionDefinitionNode fdn) {
        return RInstrument.findSingleProbe(fdn.getUID(), StandardSyntaxTag.START_METHOD);
    }

    private static Probe attachDebugHandler(FunctionDefinitionNode fdn, Object text, Object condition, boolean once) {
        Probe probe = findStartMethodProbe(fdn);
        if (probe == null) {
            return null;
        }
        FunctionStatementsEventReceiver fser = new FunctionStatementsEventReceiver(fdn, text, condition, once);
        probe.attach(fser.getInstrument());
        attachToStatementNodes(fser);
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

    private static void attachToStatementNodes(FunctionStatementsEventReceiver functionStatementsEventReceiver) {
        functionStatementsEventReceiver.getFunctionDefinitionNode().getBody().accept(new NodeVisitor() {
            public boolean visit(Node node) {
                if (node instanceof ProbeNode.WrapperNode) {
                    ProbeNode.WrapperNode wrapper = (ProbeNode.WrapperNode) node;
                    Probe probe = wrapper.getProbe();
                    if (probe.isTaggedAs(StandardSyntaxTag.STATEMENT)) {
                        Node child = wrapper.getChild();
                        if (child instanceof LoopNode) {
                            probe.attach(functionStatementsEventReceiver.getLoopStatementInstrument(wrapper));
                        } else {
                            probe.attach(functionStatementsEventReceiver.getStatementInstrument());
                        }
                    }
                }
                return true;
            }
        });
    }

    private abstract static class DebugEventReceiver implements TruffleEventReceiver {

        protected final Object text;
        protected final Object condition;
        protected final FunctionDefinitionNode functionDefinitionNode;
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
        public void returnVoid(Node node, VirtualFrame frame) {
            if (!disabled()) {
                throw RInternalError.shouldNotReachHere();
            }
        }

        @Override
        public void returnExceptional(Node node, VirtualFrame frame, Exception exception) {
        }

        boolean disabled() {
            return disabled;
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

        protected void browserInteract(Node node, VirtualFrame frame) {
            Browser.ExitMode exitMode = Browser.interact(frame.materialize());
            switch (exitMode) {
                case NEXT:
                    break;
                case STEP:
                    if (this instanceof StatementEventReceiver) {
                        StepIntoTagTrap.setTrap();
                    }
                    break;
                case CONTINUE:
                    // Have to disable
                    doContinue();
                    StepIntoTagTrap.clearTrap();
                    break;
                case FINISH:
                    // If in loop, continue to loop end, else act like CONTINUE
                    LoopNode loopNode = inLoop(node);
                    if (loopNode != null) {
                        // Have to disable just the body of the loop
                        FunctionStatementsEventReceiver fser = receiverMap.get(functionDefinitionNode.getUID());
                        fser.setFinishing(loopNode);
                    } else {
                        doContinue();
                    }
                    StepIntoTagTrap.clearTrap();
            }
        }

        private void doContinue() {
            FunctionStatementsEventReceiver fser = receiverMap.get(functionDefinitionNode.getUID());
            fser.setContinuing();
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

        private final List<Instrument> instruments = new ArrayList<>();
        private final StatementEventReceiver statementReceiver;
        ArrayList<LoopStatementEventReceiver> loopStatementReceivers = new ArrayList<>();

        private boolean once;
        private boolean continuing;

        FunctionStatementsEventReceiver(FunctionDefinitionNode functionDefinitionNode, Object text, Object condition, boolean once) {
            super(functionDefinitionNode, text, condition);
            receiverMap.put(functionDefinitionNode.getUID(), this);
            instruments.add(Instrument.create(this));
            statementReceiver = new StatementEventReceiver(functionDefinitionNode, text, condition);
            this.once = once;
        }

        Instrument getInstrument() {
            return instruments.get(0);
        }

        Instrument getStatementInstrument() {
            Instrument instrument = Instrument.create(statementReceiver);
            instruments.add(instrument);
            return instrument;
        }

        Instrument getLoopStatementInstrument(WrapperNode loopNodeWrapper) {
            LoopStatementEventReceiver lser = new LoopStatementEventReceiver(functionDefinitionNode, text, condition, loopNodeWrapper, this);
            loopStatementReceivers.add(lser);
            Instrument instrument = Instrument.create(lser);
            instruments.add(instrument);
            return instrument;
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

        void setFinishing(LoopNode loopNode) {
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
            for (LoopStatementEventReceiver lser : loopStatementReceivers) {
                lser.enable();
            }
            statementReceiver.enable();
        }

        @Override
        public void enter(Node node, VirtualFrame frame) {
            if (!disabled()) {
                RContext.getInstance().getConsoleHandler().print("debugging in: ");
                printCall(frame);
                FunctionDefinitionNode fdn = (FunctionDefinitionNode) RArguments.getFunction(frame).getRootNode();
                boolean brace = fdn.hasBraces();
                if (brace) {
                    printNode(node, brace);
                    browserInteract(node, frame);
                }
            }
        }

        @Override
        public void returnValue(Node node, VirtualFrame frame, Object result) {
            if (!disabled()) {
                returnCleanup(frame);
            }
        }

        @Override
        public void returnExceptional(Node node, VirtualFrame frame, Exception exception) {
            if (!disabled()) {
                returnCleanup(frame);
            }
        }

        private void returnCleanup(VirtualFrame frame) {
            RContext.getInstance().getConsoleHandler().print("exiting from: ");
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
            RContext.getInstance().getConsoleHandler().println(RArguments.safeGetCallSourceString(frame));
        }

    }

    private static void printNode(Node node, boolean curly) {
        RContext.ConsoleHandler consoleHandler = RContext.getInstance().getConsoleHandler();
        RDeparse.State state = RDeparse.State.createPrintableState();
        ((RNode) node).deparse(state);
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
        public void enter(Node node, VirtualFrame frame) {
            if (!disabled()) {
                // in case we did a step into that never called a function
                StepIntoTagTrap.clearTrap();
                printNode(node, false);
                browserInteract(node, frame);
            }
        }

        @Override
        public void returnValue(Node node, VirtualFrame frame, Object result) {
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
        public void enter(Node node, VirtualFrame frame) {
            if (!disabled()) {
                super.enter(node, frame);
            }
        }

        WrapperNode getLoopNodeWrapper() {
            return loopNodeWrapper;
        }

        void setFinishing() {
            finishing = true;
        }

        @Override
        public void returnExceptional(Node node, VirtualFrame frame, Exception exception) {
            if (!disabled()) {
                returnCleanup();
            }
        }

        @Override
        public void returnValue(Node node, VirtualFrame frame, Object result) {
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

    private static LoopNode inLoop(final Node nodeArg) {
        Node node = nodeArg;
        while (!(node instanceof RootNode)) {
            node = node.getParent();
            if (node instanceof LoopNode) {
                return (LoopNode) node;
            }
        }
        return null;
    }

    /**
     * Global trap for step into.
     */
    private static class StepIntoTagTrap extends SyntaxTagTrap {

        private static final StepIntoTagTrap fastRSyntaxTagTrap = new StepIntoTagTrap(RSyntaxTag.FUNCTION_BODY);

        private static boolean set;

        public StepIntoTagTrap(SyntaxTag tag) {
            super(tag);
        }

        @Override
        public void tagTrappedAt(Node node, MaterializedFrame frame) {
            FunctionBodyNode functionBodyNode = (FunctionBodyNode) node;
            FunctionDefinitionNode fdn = (FunctionDefinitionNode) functionBodyNode.getRootNode();
            ensureSingleStep(fdn);
            clearTrap();
            // next stop will be the START_METHOD node
        }

        static void setTrap() {
            Probe.setTagTrap(fastRSyntaxTagTrap);
            set = true;
        }

        static void clearTrap() {
            if (set) {
                Probe.clearTagTrap();
                set = false;
            }
        }

    }

}
