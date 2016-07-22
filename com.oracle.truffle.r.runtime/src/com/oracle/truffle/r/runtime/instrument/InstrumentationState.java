/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.instrument;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.WeakHashMap;

import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.tools.Profiler;

/**
 * The tracingState is a global variable in R, so we store it (and the associated listener objects)
 * in the {@link RContext}. We also store related {@code debug} state, as that is also context
 * specific.
 *
 */
public final class InstrumentationState implements RContext.ContextState {

    /**
     * Records all functions that have trace listeners installed.
     */
    private final WeakHashMap<SourceSection, EventBinding<?>> traceBindingMap = new WeakHashMap<>();

    private boolean tracingState = true;

    /**
     * Records all functions that have debug listeners installed.
     */
    private final WeakHashMap<SourceSection, ExecutionEventListener> debugListenerMap = new WeakHashMap<>();

    private final Instrumenter instrumenter;

    private Profiler profiler;

    private final RprofState rprofState;

    private final TracememContext tracememContext;

    /**
     * State used by the {@code tracemem} built-in.
     */
    public static final class TracememContext {
        private HashSet<Object> tracedObjects;

        public HashSet<Object> getTracedObjects() {
            if (tracedObjects == null) {
                tracedObjects = new HashSet<>();
            }
            return tracedObjects;
        }
    }

    /**
     * State used by {@code Rprof}.
     *
     */
    public static final class RprofState {
        private PrintWriter out;
        private Thread profileThread;
        private ExecutionEventListener statementListener;
        private long intervalInMillis;
        private boolean lineProfiling;

        public void initialize(PrintWriter outA, Thread profileThreadA, ExecutionEventListener statementListenerA, long intervalInMillisA,
                        boolean lineProfilingA) {
            this.out = outA;
            this.profileThread = profileThreadA;
            this.statementListener = statementListenerA;
            this.intervalInMillis = intervalInMillisA;
            this.lineProfiling = lineProfilingA;
        }

        public boolean lineProfiling() {
            return lineProfiling;
        }

        public PrintWriter out() {
            return out;
        }

        public long intervalInMillis() {
            return intervalInMillis;
        }

        public ExecutionEventListener statementListener() {
            return statementListener;
        }

        public Thread profileThread() {
            return profileThread;
        }

    }

    private InstrumentationState(Instrumenter instrumenter) {
        this.instrumenter = instrumenter;
        this.rprofState = new RprofState();
        this.tracememContext = new TracememContext();
    }

    public void putTraceBinding(SourceSection ss, EventBinding<?> binding) {
        traceBindingMap.put(ss, binding);
    }

    public EventBinding<?> getTraceBinding(SourceSection ss) {
        return traceBindingMap.get(ss);
    }

    public void putDebugListener(SourceSection ss, ExecutionEventListener listener) {
        debugListenerMap.put(ss, listener);
    }

    public EventBinding<?>[] getTraceBindings() {
        EventBinding<?>[] result = new EventBinding<?>[traceBindingMap.size()];
        traceBindingMap.values().toArray(result);
        return result;

    }

    public ExecutionEventListener getDebugListener(SourceSection ss) {
        return debugListenerMap.get(ss);
    }

    public boolean setTracingState(boolean state) {
        boolean prev = tracingState;
        tracingState = state;
        return prev;
    }

    public boolean getTracingState() {
        return tracingState;
    }

    public void setProfiler(Profiler profiler) {
        this.profiler = profiler;
    }

    public Profiler getProfiler() {
        return profiler;
    }

    public Instrumenter getInstrumenter() {
        return instrumenter;
    }

    public RprofState getRprof() {
        return rprofState;
    }

    public TracememContext getTracemem() {
        return tracememContext;
    }

    public static InstrumentationState newContext(@SuppressWarnings("unused") RContext context, Instrumenter instrumenter) {
        return new InstrumentationState(instrumenter);
    }
}
