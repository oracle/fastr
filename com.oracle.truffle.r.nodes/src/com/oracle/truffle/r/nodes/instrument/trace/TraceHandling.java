/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.instrument.trace;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.instrument.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Handles everything related to the R {@code trace} function.
 */
public class TraceHandling {

    public static boolean enableTrace(RFunction func) {
        FunctionDefinitionNode fdn = (FunctionDefinitionNode) func.getRootNode();
        TraceFunctionEventReceiver fbr = (TraceFunctionEventReceiver) RContext.getInstance().stateTraceHandling.get(fdn.getUID());
        if (fbr == null) {
            Probe probe = attachTraceHandler(fdn.getUID());
            return probe != null;
        } else {
            fbr.enable();
            return true;
        }

    }

    public static boolean disableTrace(RFunction func) {
        FunctionDefinitionNode fdn = (FunctionDefinitionNode) func.getRootNode();
        TraceFunctionEventReceiver fbr = (TraceFunctionEventReceiver) RContext.getInstance().stateTraceHandling.get(fdn.getUID());
        if (fbr == null) {
            return false;
        } else {
            fbr.disable();
            return true;
        }
    }

    public static Probe attachTraceHandler(FunctionUID uid) {
        Probe probe = RInstrument.findSingleProbe(uid, StandardSyntaxTag.START_METHOD);
        if (probe == null) {
            return null;
        }
        TraceFunctionEventReceiver fser = new TraceFunctionEventReceiver();
        RInstrument.getInstrumenter().attach(probe, fser, "trace");
        RContext.getInstance().stateTraceHandling.put(uid, fser);
        return probe;
    }

    private abstract static class TraceEventReceiver implements StandardInstrumentListener {

        @CompilationFinal private boolean disabled;
        CyclicAssumption disabledUnchangedAssumption = new CyclicAssumption("trace event disabled state unchanged");

        protected TraceEventReceiver() {
        }

        @Override
        public void onReturnVoid(Probe probe, Node node, VirtualFrame frame) {
            if (!disabled()) {
                throw RInternalError.shouldNotReachHere();
            }
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

    }

    private static class TraceFunctionEventReceiver extends TraceEventReceiver {
        private static final int INDENT = 2;
        private static int indent;

        TraceFunctionEventReceiver() {
        }

        public void onEnter(Probe probe, Node node, VirtualFrame frame) {
            if (!disabled()) {
                @SuppressWarnings("unused")
                FunctionStatementsNode fsn = (FunctionStatementsNode) node;
                for (int i = 0; i < indent; i++) {
                    RContext.getInstance().getConsoleHandler().print(" ");
                }
                String callString = RContext.getRRuntimeASTAccess().getCallerSource(RArguments.getCall(frame));
                RContext.getInstance().getConsoleHandler().printf("trace: %s%n", callString);
                indent += INDENT;
            }
        }

        @Override
        public void onReturnExceptional(Probe probe, Node node, VirtualFrame frame, Exception exception) {
            if (!disabled()) {
                indent -= INDENT;
            }
        }

        @Override
        public void onReturnValue(Probe probe, Node node, VirtualFrame frame, Object result) {
            if (!disabled()) {
                indent -= INDENT;
            }
        }

    }

}
