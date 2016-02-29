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
package com.oracle.truffle.r.nodes.instrument.trace;

import java.io.FileWriter;
import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.StandardInstrumentListener;
import com.oracle.truffle.api.instrument.StandardSyntaxTag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.function.FunctionStatementsNode;
import com.oracle.truffle.r.nodes.instrument.RInstrument;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.FunctionUID;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Handles everything related to the R {@code trace} function. Also used by
 * {@link FastROptions#TraceCalls} option.
 *
 * Output normally goes to the connection associated with the standard output, which can be
 * redirected via the {@code sink} R function. However, for use with {@link FastROptions#TraceCalls}
 * ,it may also be explicitly directed to the file {@code fastr_tracecalls.log}.
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

    public static void setTracingState(boolean state) {
        Object[] listeners = RContext.getInstance().stateTraceHandling.getListeners();
        for (int i = 0; i < listeners.length; i++) {
            TraceFunctionEventReceiver tl = (TraceFunctionEventReceiver) listeners[i];
            if (state) {
                tl.enable();
            } else {
                tl.disable();
            }
        }
    }

    @SuppressWarnings("unused")
    public static boolean enableStatementTrace(RFunction func, RSyntaxNode tracer) {
        return false;
    }

    public static Probe attachTraceHandler(FunctionUID uid) {
        Probe probe = RInstrument.findSingleProbe(uid, StandardSyntaxTag.START_METHOD);
        if (probe == null) {
            return null;
        }
        TraceFunctionEventReceiver fser = new TraceFunctionEventReceiver();
        RInstrument.getInstrumenter().attach(probe, fser, "trace");
        RContext.getInstance().stateTraceHandling.put(uid, fser);
        if (outputHandler == null) {
            setOutputHandler();
        }
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

    private abstract static class OutputHandler {
        abstract void writeString(String s, boolean nl) throws IOException;
    }

    private static class StdoutOutputHandler extends OutputHandler {

        @Override
        void writeString(String s, boolean nl) throws IOException {
            StdConnections.getStdout().writeString(s, nl);
        }

    }

    private static class FileOutputHandler extends OutputHandler {
        private FileWriter fileWriter;

        FileOutputHandler() {
            try {
                fileWriter = new FileWriter("fastr_tracecalls.log");
            } catch (IOException e) {
                Utils.fatalError("failed to open 'fastr_tracecalls.log'" + e.getMessage());
            }
        }

        @Override
        void writeString(String s, boolean nl) throws IOException {
            fileWriter.append(s);
            if (nl) {
                fileWriter.append('\n');
            }
            fileWriter.flush();
        }
    }

    private static void setOutputHandler() {
        outputHandler = FastROptions.TraceCallsToFile.getBooleanValue() ? new FileOutputHandler() : new StdoutOutputHandler();
    }

    private static OutputHandler outputHandler;

    private static class TraceFunctionEventReceiver extends TraceEventReceiver {

        TraceFunctionEventReceiver() {
        }

        public void onEnter(Probe probe, Node node, VirtualFrame frame) {
            if (!disabled()) {
                @SuppressWarnings("unused")
                FunctionStatementsNode fsn = (FunctionStatementsNode) node;
                int depth = RArguments.getDepth(frame);
                try {
                    for (int i = 0; i < depth; i++) {
                        outputHandler.writeString(" ", false);
                    }
                    RCaller caller = RArguments.getCall(frame);
                    String callString;
                    if (caller != null) {
                        callString = RContext.getRRuntimeASTAccess().getCallerSource(caller);
                    } else {
                        callString = "<no source>";
                    }
                    outputHandler.writeString("trace: " + callString, true);
                } catch (IOException ex) {
                    throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, ex.getMessage());
                }
            }
        }

        @Override
        public void onReturnExceptional(Probe probe, Node node, VirtualFrame frame, Throwable exception) {
        }

        @Override
        public void onReturnValue(Probe probe, Node node, VirtualFrame frame, Object result) {
        }

    }

    @SuppressWarnings("unused")
    private static class TraceStatementEventReceiver extends TraceEventReceiver {

        public void onEnter(Probe probe, Node node, VirtualFrame vFrame) {
            if (!disabled()) {
                //
            }

        }

        public void onReturnValue(Probe probe, Node node, VirtualFrame vFrame, Object result) {
            if (!disabled()) {
                //
            }

        }

        public void onReturnExceptional(Probe probe, Node node, VirtualFrame vFrame, Throwable exception) {
            if (!disabled()) {
                //
            }

        }
    }

}
