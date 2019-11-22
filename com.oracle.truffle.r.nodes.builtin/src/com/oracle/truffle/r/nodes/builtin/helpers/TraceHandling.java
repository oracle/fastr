/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.helpers;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import com.oracle.truffle.r.nodes.instrumentation.RInstrumentation;

import static com.oracle.truffle.r.runtime.context.FastROptions.TraceCalls;
import static com.oracle.truffle.r.runtime.context.FastROptions.TraceCallsToFile;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RLogger;
import static com.oracle.truffle.r.runtime.RLogger.LOGGER_FUNCTION_CALLS;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.instrument.RSyntaxTags.FunctionBodyBlockTag;
import com.oracle.truffle.r.runtime.data.RMissing;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Handles everything related to the R {@code .PrimTrace and .fastr.trace} functionality.
 */
public class TraceHandling {

    private static final TruffleLogger LOGGER = RLogger.getLogger(LOGGER_FUNCTION_CALLS);

    @SuppressWarnings("unchecked")
    public static void enableTrace(RFunction func) {
        EventBinding<TraceEventListener> binding = (EventBinding<TraceEventListener>) RContext.getInstance().stateInstrumentation.getTraceBinding(RInstrumentation.getSourceSection(func));
        if (binding != null) {
            // only one
            binding.dispose();
        }
        attachPrimitiveTraceHandler(func);
    }

    @SuppressWarnings("unchecked")
    public static void disableTrace(RFunction func) {
        EventBinding<TraceEventListener> binding = (EventBinding<TraceEventListener>) RContext.getInstance().stateInstrumentation.getTraceBinding(RInstrumentation.getSourceSection(func));
        if (binding != null) {
            binding.dispose();
            RContext.getInstance().stateInstrumentation.putTraceBinding(RInstrumentation.getSourceSection(func), null);
        }
    }

    @SuppressWarnings("unchecked")
    public static void setTracingState(boolean state) {
        EventBinding<?>[] listeners = RContext.getInstance().stateInstrumentation.getTraceBindings();
        for (int i = 0; i < listeners.length; i++) {
            EventBinding<TraceEventListener> binding = (EventBinding<TraceEventListener>) listeners[i];
            if (state) {
                binding.getElement().enable();
            } else {
                binding.getElement().disable();
            }
        }
    }

    public static void traceAllFunctions() {
        if (RContext.getInstance().getOption(TraceCalls)) {
            System.out.println("WARNING: The TraceCalls option was discontinued.\n" +
                            "You can rerun FastR with --log.R.com.oracle.truffle.r.functionCalls.level=FINE");

            if (RContext.getInstance().getOption(TraceCallsToFile)) {
                System.out.println("WARNING: The TraceCallsToFile option was discontinued.\n" +
                                "You can rerun FastR with --log.R.com.oracle.truffle.r.functionCalls.level=FINE --log.file=<yourfile>");
            }
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            PrimitiveFunctionEntryEventListener fser = new PrimitiveFunctionEntryEventListener();
            SourceSectionFilter.Builder builder = SourceSectionFilter.newBuilder();
            builder.tagIs(FunctionBodyBlockTag.class);
            SourceSectionFilter filter = builder.build();
            RInstrumentation.getInstrumenter().attachExecutionEventListener(filter, fser);
            setOutputHandler();
        }
    }

    @SuppressWarnings("unchecked")
    public static boolean enableStatementTrace(RFunction func, RPairList tracer, Object at, boolean print) {
        EventBinding<TraceEventListener> binding = (EventBinding<TraceEventListener>) RContext.getInstance().stateInstrumentation.getTraceBinding(RInstrumentation.getSourceSection(func));
        if (binding != null) {
            // only one allowed
            binding.dispose();
        }
        if (at == RMissing.instance) {
            // simple case
            TracerFunctionEntryEventListener listener = new TracerFunctionEntryEventListener(tracer, print);
            binding = RInstrumentation.getInstrumenter().attachExecutionEventListener(RInstrumentation.createFunctionStartFilter(func).build(), listener);
            setOutputHandler();
            RContext.getInstance().stateInstrumentation.putTraceBinding(RInstrumentation.getSourceSection(func), binding);
        }
        return false;
    }

    private static void attachPrimitiveTraceHandler(RFunction func) {
        PrimitiveFunctionEntryEventListener fser = new PrimitiveFunctionEntryEventListener();
        EventBinding<TraceEventListener> binding = RInstrumentation.getInstrumenter().attachExecutionEventListener(RInstrumentation.createFunctionStartFilter(func).build(), fser);
        setOutputHandler();
        RContext.getInstance().stateInstrumentation.putTraceBinding(RInstrumentation.getSourceSection(func), binding);
    }

    private abstract static class TraceEventListener implements ExecutionEventListener {

        @CompilationFinal private boolean disabled;
        private final CyclicAssumption disabledUnchangedAssumption = new CyclicAssumption("trace event disabled state unchanged");

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
                outputHandler.traceFunction(getCallSource(frame), RArguments.getDepth(frame));
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
        private final RPairList tracer;
        private final boolean print;

        TracerFunctionEntryEventListener(RPairList tracer, boolean print) {
            this.tracer = tracer;
            this.print = print;
        }

        @Override
        public void onEnter(EventContext context, VirtualFrame frame) {
            if (!disabled()) {
                MaterializedFrame materializedFrame = frame.materialize();
                if (print) {
                    printEnter(materializedFrame);
                }
                evalTracer(materializedFrame);
            }
        }

        @TruffleBoundary
        private void evalTracer(MaterializedFrame frame) {
            RContext.getEngine().eval(tracer, frame);
        }

        @TruffleBoundary
        private void printEnter(MaterializedFrame frame) {
            outputHandler.traceFunctionEntry(getCallSource(frame));
        }

        @Override
        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
        }

        @Override
        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
        }
    }

    private abstract static class OutputHandler {

        @TruffleBoundary
        void traceFunction(String callString, int depth) {
            write(getTraceFunctionString(callString, depth));
        }

        @TruffleBoundary
        void traceFunctionEntry(String callString) {
            write(getTraceOnFunctionEntryString(callString));
        }

        protected abstract void write(String s);

        protected abstract String getTraceFunctionString(String callString, int depth);

        protected static String getTraceOnFunctionEntryString(String callString) {
            return new StringBuilder().append("Tracing ").append(callString).append(" on entry").toString();
        }

        protected StringBuilder getIntend(int depth) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < depth - 1; i++) {
                sb.append(" ");
            }
            return sb;
        }

    }

    private static class TraceOutputHandler extends OutputHandler {
        @Override
        @TruffleBoundary
        protected String getTraceFunctionString(String callString, int depth) {
            return getIntend(depth).append("trace: ").append(callString).toString();
        }

        @Override
        @TruffleBoundary
        protected void write(String s) {
            try {
                StdConnections.getStdout().writeString(s, true);
            } catch (IOException ex) {
                throw RError.ioError(RError.SHOW_CALLER2, ex);
            }
        }
    }

    private static class LogOutputHandler extends OutputHandler {
        @Override
        @TruffleBoundary
        protected String getTraceFunctionString(String callString, int depth) {
            return getIntend(depth).append(callString).toString();
        }

        @Override
        @TruffleBoundary
        protected void write(String s) {
            LOGGER.fine(s);
        }
    }

    private static void setOutputHandler() {
        if (outputHandler == null) {
            outputHandler = LOGGER.isLoggable(Level.FINE) ? new LogOutputHandler() : new TraceOutputHandler();
        }
    }

    private static OutputHandler outputHandler;

}
