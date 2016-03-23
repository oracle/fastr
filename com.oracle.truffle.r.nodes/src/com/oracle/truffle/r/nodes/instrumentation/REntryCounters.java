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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.instrumentation.RInstrumentation.FunctionIdentification;
import com.oracle.truffle.r.runtime.FunctionUID;
import com.oracle.truffle.r.runtime.RPerfStats;
import com.oracle.truffle.r.runtime.data.RFunction;

/**
 * Basic support for adding entry/exit counters to nodes. The {@link SourceSection} attribute is is
 * used to retrieve the counter associated with a node.
 *
 */
public class REntryCounters {
    public static final class Counter {
        private final Object ident;
        private int enterCount;
        private int exitCount;

        Counter(Object ident) {
            this.ident = ident;
        }

        public int getEnterCount() {
            return enterCount;
        }

        public int getExitCount() {
            return exitCount;
        }

        public Object getIdent() {
            return ident;
        }
    }

    /**
     * Listener that is independent of the kind of node and specific instance being counted.
     */
    private abstract static class BasicListener implements ExecutionEventListener {

        private HashMap<SourceSection, Counter> counterMap = new HashMap<>();

        private Counter getCounter(EventContext context) {
            SourceSection ss = context.getInstrumentedSourceSection();
            Counter counter = counterMap.get(ss);
            if (counter == null) {
                Object obj = counterCreated(context);
                counter = new Counter(obj);
                counterMap.put(ss, counter);
            }
            return counter;
        }

        protected Counter getCounter(SourceSection sourceSection) {
            Counter counter = counterMap.get(sourceSection);
            assert counter != null;
            return counter;
        }

        protected Map<SourceSection, Counter> getCounterMap() {
            return counterMap;
        }

        @Override
        public void onEnter(EventContext context, VirtualFrame frame) {
            getCounter(context).enterCount++;
        }

        @Override
        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            getCounter(context).exitCount++;
        }

        @Override
        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
            getCounter(context).exitCount++;
        }

        protected abstract Object counterCreated(EventContext context);
    }

    /**
     * A counter that is specialized for function entry, tagged with the {@link FunctionUID}.
     */
    public static class FunctionListener extends BasicListener {
        private static final FunctionListener singleton = new FunctionListener();

        static void installCounters() {
            if (enabled()) {
                SourceSectionFilter.Builder builder = SourceSectionFilter.newBuilder();
                builder.tagIs(RSyntaxTags.START_FUNCTION);
                SourceSectionFilter filter = builder.build();
                RInstrumentation.getInstrumenter().attachListener(filter, singleton);
            }
        }

        public static void installCounter(RFunction func) {
            RInstrumentation.getInstrumenter().attachListener(RInstrumentation.createFunctionStartFilter(func).build(), singleton);
        }

        public static Counter findCounter(RFunction func) {
            FunctionDefinitionNode fdn = (FunctionDefinitionNode) func.getRootNode();
            return singleton.getCounter(fdn.getBody().getSourceSection());
        }

        @Override
        protected FunctionUID counterCreated(EventContext context) {
            FunctionDefinitionNode fdn = (FunctionDefinitionNode) context.getInstrumentedNode().getRootNode();
            return fdn.getUID();
        }

        static {
            RPerfStats.register(new PerfHandler());
        }

        static boolean enabled() {
            return RPerfStats.enabled(PerfHandler.NAME);
        }

        private static class PerfHandler implements RPerfStats.Handler {
            private static class FunctionCount implements Comparable<FunctionCount> {
                int count;
                String name;

                FunctionCount(int count, String name) {
                    this.count = count;
                    this.name = name;
                }

                @Override
                public int compareTo(FunctionCount o) {
                    if (count < o.count) {
                        return 1;
                    } else if (count > o.count) {
                        return -1;
                    } else {
                        return name.compareTo(o.name);
                    }
                }
            }

            static final String NAME = "functioncounts";

            @Override
            public void initialize(String optionText) {
            }

            @Override
            public String getName() {
                return NAME;
            }

            /**
             * R's anonymous function definitions don't help with reporting. We make an attempt to
             * locate a function name in the global/package environments.
             */
            @Override
            public void report() {
                RPerfStats.out().println("R Function Entry Counts");
                ArrayList<FunctionCount> results = new ArrayList<>();
                for (Map.Entry<SourceSection, Counter> entry : FunctionListener.singleton.getCounterMap().entrySet()) {
                    Counter counter = entry.getValue();
                    FunctionIdentification fdi = RInstrumentation.getFunctionIdentification((FunctionUID) counter.getIdent());
                    int count = counter.getEnterCount();
                    if (count > 0) {
                        results.add(new FunctionCount(count, fdi.name));
                    }
                }
                FunctionCount[] sortedCounts = new FunctionCount[results.size()];
                results.toArray(sortedCounts);
                Arrays.sort(sortedCounts);
                for (FunctionCount functionCount : sortedCounts) {
                    RPerfStats.out().printf("%6d: %s%n", functionCount.count, functionCount.name);
                }
            }
        }
    }
}
