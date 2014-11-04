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
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.Instrument;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.ProbeNode.WrapperNode;
import com.oracle.truffle.api.instrument.StandardSyntaxTag;
import com.oracle.truffle.api.instrument.TruffleEventReceiver;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;
import com.oracle.truffle.r.nodes.RNode;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.function.FunctionStatementsNode;
import com.oracle.truffle.r.nodes.instrument.RInstrument;
import com.oracle.truffle.r.nodes.instrument.RSyntaxTag;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.ConsoleHandler;
import com.oracle.truffle.r.runtime.data.*;

public class DebugFunctions {

    protected abstract static class ErrorAdapter extends RInvisibleBuiltinNode {

        protected RError arg1Closure() throws RError {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.ARG_MUST_BE_CLOSURE);
        }
    }

    @RBuiltin(name = "debug", kind = RBuiltinKind.INTERNAL, parameterNames = {"fun", "text", "condition"})
    public abstract static class Debug extends ErrorAdapter {

        @SuppressWarnings("unused")
        @Fallback
        @TruffleBoundary
        protected Object doDebug(Object fun, Object text, Object condition) {
            throw arg1Closure();
        }

        @Specialization
        @TruffleBoundary
        protected RNull doDebug(RFunction fun, Object text, Object condition) {
            controlVisibility();
            // GnuR does not generate an error for builtins, but debug (obviously) has no effect
            if (!fun.isBuiltin()) {
                if (!enableDebug(fun, text, condition, false)) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.GENERIC, "failed to attach debug handler (not instrumented?)");
                }
            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = "debugonce", kind = RBuiltinKind.INTERNAL, parameterNames = {"fun", "text", "condition"})
    public abstract static class DebugOnce extends ErrorAdapter {

        @SuppressWarnings("unused")
        @Fallback
        @TruffleBoundary
        protected Object doDebug(Object fun, Object text, Object condition) {
            throw arg1Closure();
        }

        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected RNull debugonce(RFunction fun, Object text, Object condition) {
            // TODO implement
            controlVisibility();
            return RNull.instance;
        }
    }

    @RBuiltin(name = "undebug", kind = RBuiltinKind.INTERNAL, parameterNames = {"fun"})
    public abstract static class UnDebug extends ErrorAdapter {

        @Fallback
        @TruffleBoundary
        protected Object doDebug(@SuppressWarnings("unused") Object fun) {
            throw arg1Closure();
        }

        @Specialization
        @TruffleBoundary
        protected RNull undebug(RFunction func) {
            controlVisibility();
            Probe probe = findStartMethodProbe(func);
            if (probe != null && probe.isTaggedAs(RSyntaxTag.DEBUGGED)) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NOT_DEBUGGED);
            } else {
                // TDOD
            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = "isdebugged", kind = RBuiltinKind.INTERNAL, parameterNames = {"fun"})
    public abstract static class IsDebugged extends ErrorAdapter {

        @Fallback
        @TruffleBoundary
        protected Object doDebug(@SuppressWarnings("unused") Object fun) {
            throw arg1Closure();
        }

        @Specialization
        @TruffleBoundary
        protected byte isDebugged(RFunction func) {
            controlVisibility();
            Probe probe = findStartMethodProbe(func);
            if (probe != null) {
                return RRuntime.asLogical(probe.isTaggedAs(RSyntaxTag.DEBUGGED));
            }
            return RRuntime.LOGICAL_FALSE;
        }
    }

    private static Probe findStartMethodProbe(RFunction func) {
        FunctionDefinitionNode fdn = (FunctionDefinitionNode) func.getRootNode();
        return RInstrument.findSingleProbe(fdn.getUUID(), StandardSyntaxTag.START_METHOD);
    }

    /**
     * Attach the DebugHandler instrument to the FunctionStatementsNode and all syntactic nodes.
     */
    @SuppressWarnings("unused")
    private static boolean enableDebug(RFunction func, Object text, Object condition, boolean once) {
        Probe probe = findStartMethodProbe(func);
        if (probe == null) {
            return false;
        }
        probe.attach(DebugHandler.create());
        attachToStatementNodes((FunctionDefinitionNode) func.getRootNode());
        probe.tagAs(RSyntaxTag.DEBUGGED, null);
        return true;
    }

    private static void attachToStatementNodes(FunctionDefinitionNode fdn) {
        fdn.getBody().accept(new NodeVisitor() {
            public boolean visit(Node node) {
                if (node instanceof WrapperNode) {
                    WrapperNode wrapper = (WrapperNode) node;
                    Probe probe = wrapper.getProbe();
                    if (probe.isTaggedAs(StandardSyntaxTag.STATEMENT)) {
                        probe.attach(DebugHandler.create());
                    }
                }
                return true;
            }
        });
    }

    private static final class DebugHandler {

        @SuppressWarnings("unused") private Object text;
        @SuppressWarnings("unused") private Object condition;
        public final Instrument instrument;

        static Instrument create() {
            return (new DebugHandler()).instrument;
        }

        private DebugHandler() {
            instrument = Instrument.create(new TruffleEventReceiver() {

                @Override
                public void enter(Node node, VirtualFrame frame) {
                    RDeparse.State state = RDeparse.State.createPrintableState();
                    ((RNode) node).deparse(state);

                    boolean curly = false;
                    ConsoleHandler ch = RContext.getInstance().getConsoleHandler();
                    if (node instanceof FunctionStatementsNode) {
                        ch.println("debugging in: TBD");
                        curly = true;
                    }
                    ch.print("debug: ");
                    if (curly) {
                        ch.println("{");
                    }
                    ch.print(state.toString());
                    if (curly) {
                        ch.print("}");
                    }
                    ch.print("\n");

                    BrowserFunctions.Browser.doBrowser(frame.materialize());
                }

                @Override
                public void returnVoid(Node node, VirtualFrame frame) {
                    System.console();
                }

                @Override
                public void returnValue(Node node, VirtualFrame frame, Object result) {
                    System.console();
                }

                @Override
                public void returnExceptional(Node node, VirtualFrame frame, Exception exception) {
                    System.console();
                }
            }, "R debug handler");

        }

    }

}
