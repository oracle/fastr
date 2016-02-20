/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.SimpleInstrumentListener;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.instrument.RInstrument.FunctionIdentification;
import com.oracle.truffle.r.nodes.instrument.RInstrument.NodeId;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.FunctionUID;
import com.oracle.truffle.r.runtime.RPerfStats;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Basic support for adding as timer to a node. A timer must be identified with some unique value
 * that enables it to be retrieved.
 *
 * The instrument records the cumulative time spent executing this node during the process execution
 * using {@link System#nanoTime()}.
 *
 */

public class RNodeTimer {
    private static HashMap<Object, Basic> timerMap = new HashMap<>();

    public static class Basic implements SimpleInstrumentListener {

        public static final String INFO = "R node timer";

        protected long enterTime;
        protected long cumulativeTime;

        public Basic(RInstrument.NodeId tag) {
            timerMap.put(tag, this);
        }

        @Override
        public void onEnter(Probe probe) {
            enterTime = System.nanoTime();
        }

        private void returnAny(@SuppressWarnings("unused") Probe probe) {
            cumulativeTime += System.nanoTime() - enterTime;
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

        public long getTime() {
            return cumulativeTime;
        }

    }

    public static class Statement extends Basic {
        public Statement(RInstrument.NodeId tag) {
            super(tag);
        }

        static {
            RPerfStats.register(new PerfHandler());
        }

        private static class TimingData implements Comparable<TimingData> {
            long time;
            FunctionUID functionUID;

            TimingData(FunctionUID functionUID) {
                this.functionUID = functionUID;
            }

            void addTime(long t) {
                this.time += t;
            }

            public int compareTo(TimingData o) {
                if (time < o.time) {
                    return 1;
                } else if (time > o.time) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }

        private static class PerfHandler implements RPerfStats.Handler {
            static final String NAME = "timing";
            private boolean stmts;
            private int threshold;

            public void initialize(String optionText) {
                if (optionText.length() > 0) {
                    String[] subOptions = optionText.split(":");
                    for (String subOption : subOptions) {
                        if (subOption.equals("stmts")) {
                            stmts = true;
                        } else if (subOption.startsWith("threshold")) {
                            threshold = Integer.parseInt(subOption.substring(subOption.indexOf('=') + 1)) * 1000;
                        }
                    }
                }
            }

            public String getName() {
                return NAME;
            }

            /**
             * Report the statement timing information at the end of the run. The report is per
             * function {@link FunctionUID}, which uniquely defines a function in the face of call
             * target splitting. Functions that consumed less time than requested threshold (default
             * 0) are not included in the report. The report is sorted by cumulative time.
             */
            public void report() {
                Map<FunctionUID, TimingData> functionMap = new TreeMap<>();

                for (Map.Entry<Object, Basic> entry : timerMap.entrySet()) {
                    if (entry.getValue() instanceof Statement) {
                        NodeId nodeId = (NodeId) entry.getKey();
                        TimingData timingData = functionMap.get(nodeId.uid);
                        if (timingData == null) {
                            timingData = new TimingData(nodeId.uid);
                            functionMap.put(nodeId.uid, timingData);
                        }
                        timingData.addTime(millis(entry.getValue().cumulativeTime));
                    }
                }

                Collection<TimingData> values = functionMap.values();
                TimingData[] sortedData = new TimingData[values.size()];
                values.toArray(sortedData);
                Arrays.sort(sortedData);
                long totalTime = 0;
                for (TimingData t : sortedData) {
                    totalTime += t.time;
                }

                for (TimingData t : sortedData) {
                    if (t.time > 0) {
                        if (t.time > threshold) {
                            FunctionIdentification fdi = RInstrument.getFunctionIdentification(t.functionUID);
                            RPerfStats.out().println("==========");
                            RPerfStats.out().printf("%d ms (%.2f%%): %s, %s%n", t.time, percent(t.time, totalTime), fdi.name, fdi.origin);
                            if (stmts) {
                                SourceSection ss = fdi.node.getSourceSection();
                                if (ss == null) {
                                    // wrapper
                                    ss = fdi.node.getBody().getSourceSection();
                                    if (ss == null) {
                                        RPerfStats.out().println("no source available");
                                    }
                                } else {
                                    long[] time = createLineTimes(fdi);
                                    int startLine = ss.getStartLine();
                                    int lastLine = ss.getEndLine();
                                    for (int i = startLine; i <= lastLine; i++) {
                                        RPerfStats.out().printf("%8dms: %s%n", time[i], fdi.source.getCode(i));
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }

        private static double percent(long a, long b) {
            return ((double) a * 100) / b;
        }

        private static class LineTimesNodeVisitor extends RASTProber.StatementVisitor {
            private final long[] times;

            LineTimesNodeVisitor(FunctionUID uid, long[] time) {
                super(uid);
                this.times = time;
            }

            @Override
            protected boolean callback(RSyntaxNode node) {
                SourceSection ss = node.getSourceSection();
                if (ss != null) {
                    NodeId nodeId = new NodeId(uid, node);
                    Statement stmt = (Statement) timerMap.get(nodeId);
                    if (stmt != null) {
                        assert ss.getStartLine() != 0;
                        long stmtTime = millis(stmt.cumulativeTime);
                        times[0] += stmtTime;
                        times[ss.getStartLine()] += stmtTime;
                    } else {
                        /*
                         * This happens because default arguments are not visited during the AST
                         * probe walk.
                         */
                        if (FastROptions.debugMatches("nodetimer")) {
                            System.out.printf("Failed to find map entry: %s%n", nodeId);
                            System.out.printf("Map entries for function: %s%n", uid);
                            for (Map.Entry<Object, Basic> entry : timerMap.entrySet()) {
                                NodeId nid = (NodeId) entry.getKey();
                                if (nid.uid.equals(uid)) {
                                    System.out.printf("%d%n", nid.charIndex);
                                }
                            }
                        }
                    }
                }
                return true;
            }
        }

        private static long millis(long nanos) {
            return nanos / 1000;
        }

        private static long[] createLineTimes(FunctionIdentification fdi) {
            /*
             * Although only those lines occupied by the function will actually have entries in the
             * array, addressing is easier if we allocate an array that is as long as the entire
             * source. Since there is never a line 0, we use that to compute the total.
             */
            final long[] times = new long[fdi.source.getLineCount() + 1];
            RSyntaxNode.accept(fdi.node.getBody(), 0, new LineTimesNodeVisitor(fdi.node.getUID(), times), false);
            return times;
        }

        public static boolean enabled() {
            return RPerfStats.enabled(PerfHandler.NAME);
        }

        /**
         * Return the timer tagged with {@code tag}, or {@code null} if not found.
         */
        public static RNodeTimer.Basic findTimer(Object tag) {
            return timerMap.get(tag);
        }
    }

}
