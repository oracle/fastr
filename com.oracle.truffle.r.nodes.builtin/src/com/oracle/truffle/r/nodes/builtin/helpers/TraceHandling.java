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

import java.io.FileWriter;
import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import com.oracle.truffle.r.nodes.instrumentation.RInstrumentation;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RMissing;

/**
 * Handles everything related to the R {@code .PrimTrace and .fastr.trace} functionality.
 */
public class TraceHandling {

    public static void enableTrace(RFunction func) {
        @SuppressWarnings("unchecked")
        EventBinding<TraceEventListener> binding = (EventBinding<TraceEventListener>) RContext.getInstance().stateInstrumentation.getTraceBinding(RInstrumentation.getSourceSection(func));
        if (binding != null) {
            // only one
            binding.dispose();
        }
        attachPrimitiveTraceHandler(func);
    }

    public static void disableTrace(RFunction func) {
        @SuppressWarnings("unchecked")
        EventBinding<TraceEventListener> binding = (EventBinding<TraceEventListener>) RContext.getInstance().stateInstrumentation.getTraceBinding(RInstrumentation.getSourceSection(func));
        if (binding != null) {
            binding.dispose();
            RContext.getInstance().stateInstrumentation.putTraceBinding(RInstrumentation.getSourceSection(func), null);
        }
    }

    public static void setTracingState(boolean state) {
        EventBinding<?>[] listeners = RContext.getInstance().stateInstrumentation.getTraceBindings();
        for (int i = 0; i < listeners.length; i++) {
            @SuppressWarnings("unchecked")
            EventBinding<TraceEventListener> binding = (EventBinding<TraceEventListener>) listeners[i];
            if (state) {
                binding.getElement().enable();
            } else {
                binding.getElement().disable();
            }
        }
    }

    public static void traceAllFunctions() {
        if (FastROptions.TraceCalls.getBooleanValue()) {
            PrimitiveFunctionEntryEventListener fser = new PrimitiveFunctionEntryEventListener();
            SourceSectionFilter.Builder builder = SourceSectionFilter.newBuilder();
            builder.tagIs(StandardTags.RootTag.class);
            SourceSectionFilter filter = builder.build();
            RInstrumentation.getInstrumenter().attachListener(filter, fser);
            setOutputHandler();
        }
    }

    public static boolean enableStatementTrace(RFunction func, RLanguage tracer, @SuppressWarnings("unused") Object exit, Object at, boolean print) {
        @SuppressWarnings("unchecked")
        EventBinding<TraceEventListener> binding = (EventBinding<TraceEventListener>) RContext.getInstance().stateInstrumentation.getTraceBinding(RInstrumentation.getSourceSection(func));
        if (binding != null) {
            // only one allowed
            binding.dispose();
        }
        if (at == RMissing.instance) {
            // simple case
            TracerFunctionEntryEventListener listener = new TracerFunctionEntryEventListener(tracer, print);
            binding = RInstrumentation.getInstrumenter().attachListener(RInstrumentation.createFunctionStartFilter(func).build(), listener);
            setOutputHandler();
            RContext.getInstance().stateInstrumentation.putTraceBinding(RInstrumentation.getSourceSection(func), binding);
        }
        return false;
    }

    private static void attachPrimitiveTraceHandler(RFunction func) {
        PrimitiveFunctionEntryEventListener fser = new PrimitiveFunctionEntryEventListener();
        EventBinding<TraceEventListener> binding = RInstrumentation.getInstrumenter().attachListener(RInstrumentation.createFunctionStartFilter(func).build(), fser);
        setOutputHandler();
        RContext.getInstance().stateInstrumentation.putTraceBinding(RInstrumentation.getSourceSection(func), binding);
    }

    private abstract static class TraceEventListener implements ExecutionEventListener {

        @CompilationFinal private boolean disabled;
        CyclicAssumption disabledUnchangedAssumption = new CyclicAssumption("trace event disabled state unchanged");

        protected TraceEventListener() {
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

        protected String getCallSource(VirtualFrame frame) {
            RCaller caller = RArguments.getCall(frame);
            String callString;
            if (caller != null && caller.isValidCaller()) {
                callString = getCallerSource(caller);
            } else {
                callString = "<no source>";
            }
            return callString;
        }

        @TruffleBoundary
        private static String getCallerSource(RCaller caller) {
            return RContext.getRRuntimeASTAccess().getCallerSource(caller);
        }
    }

    /**
     * Event listener for the simple case where no tracer is provided and the output is canned
     * (.PrimTrace).
     */
    private static class PrimitiveFunctionEntryEventListener extends TraceEventListener {
        @Override
        public void onEnter(EventContext context, VirtualFrame frame) {
            if (!disabled()) {
                int depth = RArguments.getDepth(frame);
                try {
                    for (int i = 0; i < depth - 1; i++) {
                        outputHandler.writeString(" ", false);
                    }
                    String callString = getCallSource(frame);
                    outputHandler.writeString("trace: " + callString, true);
                } catch (IOException ex) {
                    throw RError.ioError(RError.SHOW_CALLER2, ex);
                }
            }
        }

        @Override
        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }

        @Override
        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
        }
    }

    /**
     * Event listener for the case where user provided an expression to be evaluated on function
     * entry.
     */
    private static class TracerFunctionEntryEventListener extends TraceEventListener {
        private final RLanguage tracer;
        private final boolean print;

        TracerFunctionEntryEventListener(RLanguage tracer, boolean print) {
            this.tracer = tracer;
            this.print = print;
        }

        @Override
        public void onEnter(EventContext context, VirtualFrame frame) {
            if (!disabled()) {
                if (print) {
                    try {
                        String callString = getCallSource(frame);
                        outputHandler.writeString("Tracing " + callString + " on entry", true);
                    } catch (IOException ex) {
                        throw RError.ioError(RError.SHOW_CALLER2, ex);
                    }
                }
                RContext.getEngine().eval(tracer, frame.materialize());
            }
        }

        @Override
        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }

        @Override
        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
        }
    }

    @SuppressWarnings("unused")
    private static class TraceStatementEventReceiver extends TraceEventListener {

        @Override
        public void onEnter(EventContext context, VirtualFrame frame) {
            if (!disabled()) {
                //
            }
        }

        @Override
        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            if (!disabled()) {
                //
            }
        }

        @Override
        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
            if (!disabled()) {
                //
            }
        }
    }

    private abstract static class OutputHandler {
        abstract void writeString(String s, boolean nl) throws IOException;
    }

    private static class StdoutOutputHandler extends OutputHandler {

        @Override
        @TruffleBoundary
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
                Utils.rSuicide("failed to open 'fastr_tracecalls.log'" + e.getMessage());
            }
        }

        @Override
        @TruffleBoundary
        void writeString(String s, boolean nl) throws IOException {
            fileWriter.append(s);
            if (nl) {
                fileWriter.append('\n');
            }
            fileWriter.flush();
        }
    }

    private static void setOutputHandler() {
        if (outputHandler == null) {
            outputHandler = FastROptions.TraceCallsToFile.getBooleanValue() ? new FileOutputHandler() : new StdoutOutputHandler();
        }
    }

    private static OutputHandler outputHandler;

}
