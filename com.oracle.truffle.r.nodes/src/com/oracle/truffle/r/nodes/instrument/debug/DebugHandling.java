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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.Instrument;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.ProbeNode;
import com.oracle.truffle.api.instrument.StandardSyntaxTag;
import com.oracle.truffle.api.instrument.TruffleEventReceiver;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;
import com.oracle.truffle.r.nodes.RNode;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.function.FunctionStatementsNode;
import com.oracle.truffle.r.nodes.instrument.RInstrument;
import com.oracle.truffle.r.runtime.BrowserQuitException;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RContext;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RFunction;
import java.util.*;

/**
 * The implementation of the R debug functions.
 *
 * When a function is enabled for debugging a set of {@link DebugEventReceivers} are created
 * and associated with {@link Instrument}s and attached to key nodes in the AST body associated with the
 * {@link FunctionDefinitionNode} corresponding to the {@link RFunction} instance.
 *
 * Two different receiver classes are defined:
 * <ul>
 * <li>{@link FunctionBodyEventReceiver}: attaches to {@link FunctionBodyNode} and handles the special behavior on entry/exit</li>
 * <li>{@link StatementEventReceiver}: attaches to all {@link STATEMENT} nodes and handles "n" and "s" browser commands</li>
 * </ul>
 * <p>
 * Step Into is slightly tricky because, at the point the command is issued, we do not know what function the call
 * will resolve to. This is finessed by a callback from the {@code execute} method of {@link FunctionDefinitionNode},
 * which acts as if {@code debugonce} had been called by the user (unless debug was already enabled in which case there
 * is nothing to do).
 * <p>
 * Currently, {@code debugonce} and {@code undebug} are handled by disabling the receiver behavior.
 * For full-speed execution after such calls, the instruments should probably be disposed instead.
 */
public class DebugHandling {

    /**
     * Records all functions that have debug receivers installed.
     */
    private static final WeakHashMap<UUID, FunctionBodyEventReceiver> handlerMap = new WeakHashMap<>();

    /**
     * Attach the DebugHandling instrument to the FunctionStatementsNode and all
     * syntactic nodes.
     */
    @SuppressWarnings("unused")
    public static boolean enableDebug(RFunction func, Object text, Object condition, boolean once) {
        FunctionDefinitionNode fdn = (FunctionDefinitionNode) func.getRootNode();
        FunctionBodyEventReceiver fbr = handlerMap.get(fdn.getUUID());
        if (fbr == null) {
            Probe probe = attachDebugHandler(fdn, text, condition, once);
            return probe != null;
        } else {
            fbr.enable(once);
            return true;
        }
     }

    @TruffleBoundary
    public static void checkStep(VirtualFrame frame, FunctionDefinitionNode fdn) {
        Frame callerFrame = Utils.getCallerFrame(frame, FrameInstance.FrameAccess.READ_ONLY);
        if (callerFrame != null && RArguments.getStepping(callerFrame)) {
            ensureSingleStep(fdn);
        }
    }

    public static boolean undebug(RFunction func) {
        FunctionBodyEventReceiver fbr = handlerMap.get(((FunctionDefinitionNode) func.getRootNode()).getUUID());
        if (fbr == null) {
           return false;
        } else {
            fbr.disable();
            return true;
        }
    }

    public static boolean isDebugged(RFunction func) {
        FunctionBodyEventReceiver fbr = handlerMap.get(((FunctionDefinitionNode) func.getRootNode()).getUUID());
        return fbr != null && !fbr.disabled();
    }

    private static Probe findStartMethodProbe(RFunction func) {
        return findStartMethodProbe((FunctionDefinitionNode) func.getRootNode());
    }

    private static Probe findStartMethodProbe(FunctionDefinitionNode fdn) {
        return RInstrument.findSingleProbe(fdn.getUUID(), StandardSyntaxTag.START_METHOD);
    }

    private static Probe attachDebugHandler(FunctionDefinitionNode fdn, Object text, Object condition, boolean once) {
        Probe probe = findStartMethodProbe(fdn);
        if (probe == null) {
            return null;
        }
        FunctionBodyEventReceiver functionBodyEventReceiver = new FunctionBodyEventReceiver(fdn, text, condition, once);
        probe.attach(functionBodyEventReceiver.getInstrument());
        attachToStatementNodes(functionBodyEventReceiver);
        return probe;
    }

    private static void ensureSingleStep(FunctionDefinitionNode fdn) {
        FunctionBodyEventReceiver fbr = handlerMap.get(fdn.getUUID());
        if (fbr == null) {
            attachDebugHandler(fdn, null, null, true);
        } else {
            if (fbr.disabled()) {
                fbr.enable(true);
            }
        }
    }

    private static void attachToStatementNodes(FunctionBodyEventReceiver functionBodyEventReceiver) {
        functionBodyEventReceiver.getFunctionDefinitionNode().getBody().accept(new NodeVisitor() {
            public boolean visit(Node node) {
                if (node instanceof ProbeNode.WrapperNode) {
                    ProbeNode.WrapperNode wrapper = (ProbeNode.WrapperNode) node;
                    Probe probe = wrapper.getProbe();
                    if (probe.isTaggedAs(StandardSyntaxTag.STATEMENT)) {
                        probe.attach(functionBodyEventReceiver.getStatementInstrument());
                    }
                }
                return true;
            }
        });
    }

    private abstract static class DebugEventReceiver implements TruffleEventReceiver {

        @SuppressWarnings("unused")
        protected final Object text;
        @SuppressWarnings("unused")
        protected final Object condition;
        private boolean disabled;
        protected final FunctionDefinitionNode functionDefinitionNode;

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
            throw RInternalError.shouldNotReachHere();
        }

        @Override
        public void returnExceptional(Node node, VirtualFrame frame, Exception exception) {
            if (exception instanceof BrowserQuitException) {
                // cleanup?
            }
        }

        boolean disabled() {
            return disabled;
        }

        void disable() {
            disabled = true;
        }

        void enable(boolean once) {
            disabled = false;
        }

        protected static void printNode(Node node, boolean curly) {
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

    }

    /**
     * This handles function entry and exit. We try to emulate GnuR behavior but
     * since FastR does not (yet) handle <@code {</code> correctly, it is a bit
     * heuristic. In particular, if a function is defined using <@code {
     * }</code>, GnuR stops at the <@code {</code> and then "steps over" the
     * <@code {</code> to the first statement, otherwise it just stops at the
     * first statement.
     */
    private static class FunctionBodyEventReceiver extends DebugEventReceiver {

        private final List<Instrument> instruments = new ArrayList<>();
        private final StatementEventReceiver statementReceiver;

        private boolean once;

        FunctionBodyEventReceiver(FunctionDefinitionNode functionDefinitionNode, Object text, Object condition, boolean once) {
            super(functionDefinitionNode, text, condition);
            handlerMap.put(functionDefinitionNode.getUUID(), this);
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

        void disable() {
            super.disable();
            statementReceiver.disable();
        }

        void enable(boolean once) {
            super.enable(once);
            statementReceiver.disable();
        }

        @Override
        public void enter(Node node, VirtualFrame frame) {
            if (disabled()) {
                return;
            }
            FunctionStatementsNode fsn = (FunctionStatementsNode) node;

            RContext.getInstance().getConsoleHandler().print("debugging in: ");
            printCall(frame);
            FunctionDefinitionNode fdn = (FunctionDefinitionNode) RArguments.getFunction(frame).getRootNode();
            boolean curly = fdn.toString().trim().endsWith("}");
            if (curly) {
                printNode(node, curly);
                Browser.interact(frame.materialize());
            }
        }

        @Override
        public void returnValue(Node node, VirtualFrame frame, Object result) {
            if (disabled()) {
                return;
            }
            RContext.getInstance().getConsoleHandler().print("exiting from: ");
            printCall(frame);
            if (once) {
                disable();
            }
        }

        private void printCall(VirtualFrame frame) {
            RContext.getInstance().getConsoleHandler().println(RArguments.getCallSourceSection(frame).getCode());
        }

    }

    private static class StatementEventReceiver extends DebugEventReceiver {

        StatementEventReceiver(FunctionDefinitionNode functionDefinitionNode, Object text, Object condition) {
            super(functionDefinitionNode, text, condition);
        }

        @Override
        public void enter(Node node, VirtualFrame frame) {
            if (disabled()) {
                return;
            }
            printNode(node, false);
            Browser.ExitMode exitMode = Browser.interact(frame.materialize());
            switch (exitMode) {
                case NEXT:
                    break;
                case STEP:
                    RArguments.setStepping(frame, true);
                    break;
                case CONTINUE:
                    break;
            }
        }

        @Override
        public void returnValue(Node node, VirtualFrame frame, Object result) {
            if (disabled()) {
                return;
            }
            RArguments.setStepping(frame, false);
        }

    }

}
