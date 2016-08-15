/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.*;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributes;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.RAttributes.AttributeTracer.Change;
import com.oracle.truffle.r.runtime.instrument.InstrumentationState.RprofState;
import com.oracle.truffle.tools.Profiler;
import com.oracle.truffle.tools.Profiler.Counter.TimeKind;

public class FastRStats {

    private abstract static class CastHelper extends RBuiltinNode {
        protected void filename(CastBuilder casts) {
            casts.arg("filename").mustBe(stringValue().or(nullValue())).asStringVector();
        }

        protected void append(CastBuilder casts) {
            casts.arg("append").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).notNA().map(toBoolean());
        }
    }

    @RBuiltin(name = ".fastr.prof.attr", visibility = OFF, kind = PRIMITIVE, parameterNames = {"filename", "append"}, behavior = COMPLEX)
    public abstract static class FastRProfAttr extends CastHelper implements RAttributes.AttributeTracer.Listener {
        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{"Rprofattr.out", RRuntime.LOGICAL_FALSE};
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            filename(casts);
            append(casts);
        }

        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected RNull profAttr(RNull filenameVec, boolean append) {
            endProfiling();
            return RNull.instance;
        }

        @Specialization
        @TruffleBoundary
        protected RNull profAttr(RAbstractStringVector filenameVec, boolean append) {
            if (filenameVec.getLength() == 0) {
                // disable
                endProfiling();
            } else {
                // enable after ending any previous session
                State state = State.get();
                try {
                    PrintStream out = new PrintStream(new FileOutputStream(filenameVec.getDataAt(0), append));
                    state.setOut(out);
                    RAttributes.AttributeTracer.addListener(this);
                    RAttributes.AttributeTracer.setTracingState(true);
                } catch (IOException ex) {
                    throw RError.error(this, RError.Message.GENERIC, String.format("Rprofmem: cannot open profile file '%s'", filenameVec.getDataAt(0)));
                }

            }
            return RNull.instance;
        }

        protected void endProfiling() {
            State state = State.get();
            if (state.out() != null) {
                state.cleanup(0);
            }
        }

        @Override
        public void reportAttributeChange(Change change, RAttributes attrs, Object value) {
            State rprofattrState = State.get();
            rprofattrState.out().printf("%s,%d,", change.name(), System.identityHashCode(attrs));
            switch (change) {
                case CREATE:
                    rprofattrState.out().print("NA");
                    break;
                case GROW:
                    rprofattrState.out().printf("%d", (int) value);
                    break;
                default:
                    rprofattrState.out().printf("%s", (String) value);
                    break;
            }
            rprofattrState.out().println();
        }

        private static class State extends RprofState {
            private static State get() {
                State state = (State) RContext.getInstance().stateInstrumentation.getRprofState("attr");
                if (state == null) {
                    state = new State();
                    RContext.getInstance().stateInstrumentation.setRprofState("attr", state);
                }
                return state;
            }

            @Override
            public void cleanup(int status) {
                RAttributes.AttributeTracer.setTracingState(false);
                closeAndResetOut();
            }

        }

    }

    @RBuiltin(name = ".fastr.stats.typecounts", visibility = OFF, kind = PRIMITIVE, parameterNames = {"filename", "append"}, behavior = COMPLEX)
    public abstract static class FastRProfTypecounts extends CastHelper implements RDataFactory.Listener {
        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{"Rproftypecounts.out", RRuntime.LOGICAL_FALSE};
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            filename(casts);
            append(casts);
        }

        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected RNull profTypecounts(RNull filenameVec, boolean append) {
            endProfiling();
            return RNull.instance;
        }

        @Specialization
        @TruffleBoundary
        protected RNull profTypecounts(RAbstractStringVector filenameVec, boolean append) {
            if (filenameVec.getLength() == 0) {
                // disable
                endProfiling();
            } else {
                // enable after ending any previous session
                State state = State.get();
                try {
                    PrintStream out = new PrintStream(new FileOutputStream(filenameVec.getDataAt(0), append));
                    state.setOut(out);
                    RDataFactory.addListener(this);
                    RDataFactory.setTracingState(true);
                } catch (IOException ex) {
                    throw RError.error(this, RError.Message.GENERIC, String.format("Rprofmem: cannot open profile file '%s'", filenameVec.getDataAt(0)));
                }

            }
            return RNull.instance;
        }

        protected void endProfiling() {
            State state = State.get();
            if (state.out() != null) {
                RDataFactory.setTracingState(false);
                state.cleanup(0);
            }
        }

        @Override
        public void reportAllocation(RTypedValue data) {
            Class<? extends RTypedValue> klass = data.getClass();
            boolean isVector = (data instanceof RAbstractVector);
            State state = State.get();
            Map<Class<? extends RTypedValue>, SortedMap<Integer, State.Counter>> typecountsMap = state.getTypecountsMap();
            SortedMap<Integer, State.Counter> countsMap = typecountsMap.get(klass);
            if (countsMap == null) {
                countsMap = new TreeMap<>();
                typecountsMap.put(klass, countsMap);
            }
            int length;
            if (isVector) {
                RAbstractVector vector = (RAbstractVector) data;
                length = vector.getLength();
            } else {
                length = 1;
            }
            State.Counter count = countsMap.get(length);
            if (count == null) {
                count = new State.Counter();
                countsMap.put(length, count);
            }
            count.incCount();
        }

        private static class State extends RprofState {
            public static class Counter {
                private int count;

                public void incCount() {
                    count++;
                }

                public int getCount() {
                    return count;
                }

                @Override
                public String toString() {
                    return Integer.toString(count);
                }

            }

            private Map<Class<? extends RTypedValue>, SortedMap<Integer, Counter>> typecountsMap;

            private static State get() {
                State state = (State) RContext.getInstance().stateInstrumentation.getRprofState("typecounts");
                if (state == null) {
                    state = new State();
                    RContext.getInstance().stateInstrumentation.setRprofState("typecounts", state);
                }
                return state;
            }

            private Map<Class<? extends RTypedValue>, SortedMap<Integer, Counter>> getTypecountsMap() {
                if (typecountsMap == null) {
                    typecountsMap = new HashMap<>();
                }
                return typecountsMap;
            }

            @Override
            public void cleanup(int status) {
                for (Map.Entry<Class<? extends RTypedValue>, SortedMap<Integer, Counter>> entry : getTypecountsMap().entrySet()) {
                    SortedMap<Integer, Counter> countsMap = entry.getValue();
                    int totalCount = 0;
                    for (Counter counter : countsMap.values()) {
                        totalCount += counter.getCount();
                    }
                    out().printf("%s,%d", entry.getKey().getSimpleName(), totalCount);
                    for (Map.Entry<Integer, Counter> countsEntry : countsMap.entrySet()) {
                        out().printf(",%d:%d", countsEntry.getKey(), countsEntry.getValue().getCount());
                    }
                    out().println();
                }
                closeAndResetOut();
            }
        }

    }

    @RBuiltin(name = ".fastr.stats.funcounts", visibility = OFF, kind = PRIMITIVE, parameterNames = {"filename", "append", "timing", "threshold", "histograms"}, behavior = COMPLEX)
    public abstract static class FastRProfFuncounts extends CastHelper {
        @Override
        public Object[] getDefaultParameterValues() {
            return new Object[]{"Rproffuncounts.out", RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_FALSE, 0, RRuntime.LOGICAL_FALSE};
        }

        @Override
        protected void createCasts(CastBuilder casts) {
            filename(casts);
            append(casts);
            casts.arg("timing").asLogicalVector().findFirst().notNA().map(toBoolean());
            casts.arg("threshold").asIntegerVector().findFirst().notNA();
            casts.arg("histograms").asLogicalVector().findFirst().notNA().map(toBoolean());
        }

        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected RNull profFuncounts(RNull filenameVec, boolean append, boolean timing, int threshold, boolean histograms) {
            endProfiling();
            return RNull.instance;
        }

        @Specialization
        @TruffleBoundary
        protected RNull profFuncounts(RAbstractStringVector filenameVec, boolean append, boolean timing, int threshold, boolean histograms) {
            if (filenameVec.getLength() == 0) {
                // disable
                endProfiling();
            } else {
                // enable after ending any previous session
                State state = State.get();
                try {
                    PrintStream out = new PrintStream(new FileOutputStream(filenameVec.getDataAt(0), append));
                    state.initialize(out, threshold, timing, histograms);
                    Profiler profiler = RContext.getInstance().stateInstrumentation.getProfiler();
                    profiler.setCollecting(true);
                    profiler.setTiming(timing);
                } catch (IOException ex) {
                    throw RError.error(this, RError.Message.GENERIC, String.format("Rprofmem: cannot open profile file '%s'", filenameVec.getDataAt(0)));
                }

            }
            return RNull.instance;
        }

        protected void endProfiling() {
            State state = State.get();
            if (state.out() != null) {
                state.cleanup(0);
            }
        }

        private static class State extends RprofState {
            private int threshold;
            private boolean timing;
            private boolean histograms;

            private static State get() {
                State state = (State) RContext.getInstance().stateInstrumentation.getRprofState("funcounts");
                if (state == null) {
                    state = new State();
                    RContext.getInstance().stateInstrumentation.setRprofState("funcounts", state);
                }
                return state;
            }

            private void initialize(PrintStream outA, int thresholdA, boolean timingA, boolean histogramsA) {
                this.setOut(outA);
                this.threshold = thresholdA;
                this.timing = timingA;
                this.histograms = histogramsA;
            }

            @Override
            public void cleanup(int status) {
                Profiler profiler = RContext.getInstance().getInstrumentationState().getProfiler();
                if (histograms) {
                    profiler.printHistograms(out());
                    return;
                }
                /*
                 * Report the statement counts/timing information at the end of the run. The report
                 * is per function. If timing is on, output is sorted by time, otherwise by
                 * invocation count. When timing, functions that consumed less time than requested
                 * threshold (default 0) are not included in the report.
                 */
                Map<SourceSection, Profiler.Counter> counters = profiler.getCounters();
                long totalTime = 0;
                SortableCounter[] sortedCounters = new SortableCounter[counters.size()];
                int i = 0;
                for (Profiler.Counter counter : counters.values()) {
                    sortedCounters[i++] = new SortableCounter(counter, timing);
                    if (timing) {
                        totalTime += counter.getSelfTime(TimeKind.INTERPRETED_AND_COMPILED);
                    }
                }
                Arrays.sort(sortedCounters);
                for (SortableCounter scounter : sortedCounters) {
                    long time = scounter.counter.getSelfTime(TimeKind.INTERPRETED_AND_COMPILED);
                    long invocations = scounter.counter.getInvocations(TimeKind.INTERPRETED_AND_COMPILED);
                    boolean include = timing ? time > 0 && time > threshold : invocations > 0;
                    if (include) {
                        SourceSection ss = scounter.counter.getSourceSection();
                        Source source = ss.getSource();
                        out().println("==========");
                        out().printf("calls %d", invocations);
                        if (timing) {
                            double thisPercent = percent(time, totalTime);
                            out().printf(", time %d ms (%.2f%%)", time, thisPercent);
                        }
                        out().printf(": %s, %s%n", scounter.counter.getName(), RSource.getOrigin(source));
                    }
                }
                profiler.clearData();
                closeAndResetOut();
            }

            private static double percent(long a, long b) {
                return ((double) a * 100) / b;
            }

        }

        private static final class SortableCounter implements Comparable<SortableCounter> {
            private boolean timing;
            private Profiler.Counter counter;

            private SortableCounter(Profiler.Counter counter, boolean timing) {
                this.counter = counter;
                this.timing = timing;
            }

            @Override
            public int compareTo(SortableCounter other) {
                if (timing) {
                    long myTime = counter.getSelfTime(TimeKind.INTERPRETED_AND_COMPILED);
                    long otherTime = other.counter.getSelfTime(TimeKind.INTERPRETED_AND_COMPILED);
                    return myTime < otherTime ? 1 : (myTime > otherTime ? -1 : 0);
                } else {
                    long myInvocations = counter.getInvocations(TimeKind.INTERPRETED_AND_COMPILED);
                    long otherInvocations = other.counter.getInvocations(TimeKind.INTERPRETED_AND_COMPILED);
                    return myInvocations < otherInvocations ? 1 : (myInvocations > otherInvocations ? -1 : 0);
                }
            }

        }

    }
}
