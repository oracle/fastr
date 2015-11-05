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
package com.oracle.truffle.r.nodes.instrument;

import java.util.*;

import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;

/**
 * Basic support for adding entry/exit counters to nodes. A counter must be identified with some
 * unique value that enables it to be retrieved.
 *
 */
public class REntryCounters {

    private static HashMap<Object, Basic> counterMap = new HashMap<>();
    public static final String INFO = "R node entry counter";

    public static class Basic implements SimpleInstrumentListener {

        protected int enterCount;
        protected int exitCount;

        public Basic(Object tag) {
            counterMap.put(tag, this);
        }

        @Override
        public void onEnter(Probe probe) {
            enterCount++;
        }

        private void returnAny(@SuppressWarnings("unused") Probe probe) {
            exitCount++;
        }

        @Override
        public void onReturnVoid(Probe probe) {
            returnAny(probe);
        }

        @Override
        public void onReturnValue(Probe probe, Object result) {
            returnAny(probe);
        }

        @Override
        public void onReturnExceptional(Probe probe, Throwable exception) {
            returnAny(probe);
        }

        public int getEnterCount() {
            return enterCount;
        }

        public int getExitCount() {
            return exitCount;
        }
    }

    /**
     * A counter that is specialized for function entry, tagged with the {@link FunctionUID}.
     */
    public static class Function extends Basic {
        public static final String INFO = "R function entry counter";

        static {
            RPerfStats.register(new PerfHandler());
        }

        private static class PerfHandler implements RPerfStats.Handler {
            private static class FunctionCount implements Comparable<FunctionCount> {
                int count;
                String name;

                FunctionCount(int count, String name) {
                    this.count = count;
                    this.name = name;
                }

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

            public void initialize(String optionText) {
            }

            public String getName() {
                return NAME;
            }

            /**
             * R's anonymous function definitions don't help with reporting. We make an attempt to
             * locate a function name in the global/package environments.
             */
            public void report() {
                RPerfStats.out().println("R Function Entry Counts");
                ArrayList<FunctionCount> results = new ArrayList<>();
                for (Map.Entry<Object, Basic> entry : counterMap.entrySet()) {
                    if (entry.getValue() instanceof Function) {
                        FunctionUID uid = (FunctionUID) entry.getKey();
                        RInstrument.FunctionIdentification fdi = RInstrument.getFunctionIdentification(uid);
                        int count = entry.getValue().getEnterCount();
                        if (count > 0) {
                            results.add(new FunctionCount(count, fdi.name));
                        }
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

        public Function(FunctionUID uuid) {
            super(uuid);
        }

        public static boolean enabled() {
            return RPerfStats.enabled(PerfHandler.NAME);
        }

    }

    /**
     * Return the counter tagged with {@code tag}, or {@code null} if not found.
     */
    public static REntryCounters.Basic findCounter(Object tag) {
        return counterMap.get(tag);
    }

}
