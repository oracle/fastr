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
package com.oracle.truffle.r.nodes.instrumentation;

import java.util.Arrays;
import java.util.Map;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.runtime.RPerfStats;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.tools.Profiler;
import com.oracle.truffle.tools.Profiler.Counter;
import com.oracle.truffle.tools.Profiler.Counter.TimeKind;

/**
 * Interface to the Truffle {@link Profiler}.
 */
public class RFunctionProfiler {
    static {
        RPerfStats.register(new PerfHandler());
    }

    static boolean enabled() {
        return RPerfStats.enabled(PerfHandler.NAME);
    }

    /**
     * This is called on startup to support {@link RPerfStats}.
     */
    static void installTimers(RContext context) {
        if (enabled()) {
            enableTiming(context, true, true);
        }
    }

    private static Profiler getProfiler(RContext context) {
        PolyglotEngine vm = context.getVM();
        Profiler profiler = Profiler.find(vm);
        return profiler;
    }

    private static Profiler getProfiler() {
        PolyglotEngine vm = RContext.getInstance().getVM();
        Profiler profiler = Profiler.find(vm);
        return profiler;
    }

    private static void enableTiming(RContext context, @SuppressWarnings("unused") boolean counting, boolean timing) {
        Profiler profiler = getProfiler(context);
        context.getInstrumentationState().setProfiler(profiler);
        profiler.setTiming(timing);
        profiler.setCollecting(true);
    }

    /**
     * (Interactively) installs a timer for a specific function. Currently the {@link Profiler} does
     * not support profiling limited to specific functions so this effectively enables everything.
     * If {@code func} is {@code null} profile all functions. In principle profiling can be
     * restricted to entry counting and timing but currently counting is always on.
     *
     */
    public static void installTimer(@SuppressWarnings("unused") RFunction func, boolean counting, boolean timing) {
        enableTiming(RContext.getInstance(), counting, timing);
    }

    public static Counter getCounter(RFunction func) {
        Profiler profiler = getProfiler();
        if (profiler.isCollecting()) {
            String funcName = func.getTarget().getRootNode().getName();
            Map<SourceSection, Counter> counters = profiler.getCounters();
            for (Counter counter : counters.values()) {
                if (counter.getName().equals(funcName)) {
                    return counter;
                }
            }
        }
        return null;
    }

    public static void reset() {
        Profiler profiler = getProfiler();
        profiler.clearData();
        profiler.setCollecting(false);
    }

    public static void clear() {
        Profiler profiler = getProfiler();
        profiler.clearData();
    }

    public static Counter[] getCounters() {
        Profiler profiler = getProfiler();
        if (profiler.isCollecting()) {
            Map<SourceSection, Counter> counters = profiler.getCounters();
            Counter[] result = new Counter[counters.size()];
            counters.values().toArray(result);
            return result;
        } else {
            return null;
        }
    }

    public static boolean isTiming() {
        Profiler profiler = getProfiler();
        return profiler.isTiming();
    }

    private static class PerfHandler implements RPerfStats.Handler {
        static final String NAME = "timer";
        @SuppressWarnings("unused") private boolean stmts;
        private int threshold;

        @Override
        public void initialize(String optionText) {
            if (optionText.length() > 0) {
                String[] subOptions = optionText.split(":");
                for (String subOption : subOptions) {
                    if (subOption.equals("stmts")) {
                        Utils.warn("statement timing is not implemented");
                        stmts = true;
                    } else if (subOption.startsWith("threshold")) {
                        threshold = Integer.parseInt(subOption.substring(subOption.indexOf('=') + 1)) * 1000;
                    }
                }
            }
        }

        @Override
        public String getName() {
            return NAME;
        }

        private static class SortableCounter implements Comparable<SortableCounter> {
            private Counter counter;

            SortableCounter(Counter counter) {
                this.counter = counter;
            }

            @Override
            public int compareTo(SortableCounter other) {
                long myTime = counter.getSelfTime(TimeKind.INTERPRETED_AND_COMPILED);
                long otherTime = other.counter.getSelfTime(TimeKind.INTERPRETED_AND_COMPILED);
                return myTime < otherTime ? 1 : (myTime > otherTime ? -1 : 0);
            }

        }

        /**
         * Report the statement timing information at the end of the run. The report is per function
         * Functions that consumed less time than requested threshold (default 0) are not included
         * in the report. The report is sorted by cumulative time.
         */
        @Override
        public void report() {
            Profiler profiler = RContext.getInstance().getInstrumentationState().getProfiler();
            // profiler.printHistograms(RPerfStats.out());
            Map<SourceSection, Counter> counters = profiler.getCounters();
            long totalTime = 0;
            SortableCounter[] sortedCounters = new SortableCounter[counters.size()];
            int i = 0;
            for (Counter counter : counters.values()) {
                totalTime += counter.getSelfTime(TimeKind.INTERPRETED_AND_COMPILED);
                sortedCounters[i++] = new SortableCounter(counter);
            }
            Arrays.sort(sortedCounters);
            for (SortableCounter scounter : sortedCounters) {
                long time = scounter.counter.getSelfTime(TimeKind.INTERPRETED_AND_COMPILED);
                if (time > 0 && time > threshold) {
                    SourceSection ss = scounter.counter.getSourceSection();
                    Source source = ss.getSource();
                    RPerfStats.out().println("==========");
                    double thisPercent = percent(time, totalTime);
                    RPerfStats.out().printf("%d ms (%.2f%%): %s, %s%n", time, thisPercent, scounter.counter.getName(), RSource.getOrigin(source));
                }
            }
            System.console();
        }

        private static double percent(long a, long b) {
            return ((double) a * 100) / b;
        }

    }
}
