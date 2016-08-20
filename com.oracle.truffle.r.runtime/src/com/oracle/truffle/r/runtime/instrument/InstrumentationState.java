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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.runtime.RCleanUp;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.tools.Profiler;

/**
 * Collects together all the context-specific state related to profiling, instrumentation.
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

    /**
     * The {@link Instrumenter} associated with this {@link RContext}. Never {@code null}.
     */
    private final Instrumenter instrumenter;

    /**
     * The {@link Profiler}, if any, associated with this {@link RContext}.
     */
    private Profiler profiler;

    private TracememContext tracememContext;

    Map<String, RprofState> rprofStates = new ConcurrentHashMap<>(7);

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
     * The {@link BrowserState} state, if any, associated with this {@link RContext}.
     */
    private BrowserState browserState;

    /**
     * Whether debugging is globally disabled in this {@link RContext}. Used to (temporarily)
     * disable all debugging across calls that are used internally in the implementation.
     *
     */
    private boolean debugGloballyDisabled;

    public abstract static class RprofState implements CleanupHandler {
        private PrintStream out;

        protected RprofState() {
            RCleanUp.registerCleanupHandler(this);
        }

        /**
         * Return current output or {@code null} if not profiling.
         */
        public PrintStream out() {
            return out;
        }

        public void setOut(PrintStream out) {
            this.out = out;
        }

        public void closeAndResetOut() {
            out.flush();
            out.close();
            out = null;
        }
    }

    public static class BrowserState {
        public static final class HelperState {
            // docs state that "text" is a string but in reality it can be anything
            public final Object text;
            public final Object condition;

            public HelperState(Object text, Object condition) {
                this.text = text;
                this.condition = condition;
            }
        }

        private boolean inBrowser;
        private String lastEmptyLineCommand = "n";
        private ArrayList<HelperState> helperStateList = new ArrayList<>();

        public void setInBrowser(boolean state) {
            this.inBrowser = state;
        }

        public boolean inBrowser() {
            return inBrowser;
        }

        public void setLastEmptyLineCommand(String s) {
            lastEmptyLineCommand = s;
        }

        public String lastEmptyLineCommand() {
            return lastEmptyLineCommand;
        }

        public ArrayList<HelperState> helperStateList() {
            return helperStateList;
        }
    }

    /**
     * An interface that can be used to register an action to be taken when the system shuts down as
     * part of the {@code R_Cleanup}.
     *
     */
    public interface CleanupHandler {
        void cleanup(int status);
    }

    private InstrumentationState(Instrumenter instrumenter) {
        this.instrumenter = instrumenter;
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

    public Profiler getProfiler() {
        if (profiler == null) {
            PolyglotEngine vm = RContext.getInstance().getVM();
            profiler = Profiler.find(vm);
        }
        return profiler;
    }

    public Instrumenter getInstrumenter() {
        return instrumenter;
    }

    public RprofState getRprofState(String name) {
        RprofState state = rprofStates.get(name);
        return state;
    }

    public void setRprofState(String name, RprofState state) {
        rprofStates.put(name, state);
    }

    public TracememContext getTracemem() {
        if (tracememContext == null) {
            tracememContext = new TracememContext();
        }
        return tracememContext;
    }

    public BrowserState getBrowserState() {
        if (browserState == null) {
            browserState = new BrowserState();
        }
        return browserState;
    }

    public boolean setDebugGloballyDisabled(boolean state) {
        boolean current = debugGloballyDisabled;
        this.debugGloballyDisabled = state;
        return current;
    }

    public boolean debugGloballyDisabled() {
        return debugGloballyDisabled;
    }

    public static InstrumentationState newContext(@SuppressWarnings("unused") RContext context, Instrumenter instrumenter) {
        return new InstrumentationState(instrumenter);
    }
}
