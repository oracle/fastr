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
package com.oracle.truffle.r.nodes.instrumentation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.control.BlockNode;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.instrumentation.RInstrumentation.FunctionIdentification;
import com.oracle.truffle.r.runtime.FunctionUID;
import com.oracle.truffle.r.runtime.RPerfStats;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNodeVisitor;

/**
 * Basic support for adding as timer to a node. Currently limited to "statement" timing.
 *
 * The instrument records the cumulative time spent executing this node during the process execution
 * using {@link System#nanoTime()}.
 *
 */
class RNodeTimer {

    private static final class TimeInfo {
        private final Object ident;
        protected long enterTime;
        protected long cumulativeTime;

        TimeInfo(Object ident) {
            this.ident = ident;
        }

        public long getTime() {
            return cumulativeTime;
        }

        public Object getIdent() {
            return ident;
        }
    }

    private abstract static class BasicListener implements ExecutionEventListener {
        private HashMap<SourceSection, TimeInfo> timeInfoMap = new HashMap<>();

        private TimeInfo getTimeInfo(EventContext context) {
            SourceSection ss = context.getInstrumentedSourceSection();
            TimeInfo timeInfo = timeInfoMap.get(ss);
            if (timeInfo == null) {
                Object obj = timeInfoCreated(context);
                timeInfo = new TimeInfo(obj);
                timeInfoMap.put(ss, timeInfo);
            }
            return timeInfo;
        }

        protected TimeInfo getTimeInfo(SourceSection sourceSection) {
            TimeInfo timeInfo = timeInfoMap.get(sourceSection);
            assert timeInfo != null;
            return timeInfo;
        }

        protected Map<SourceSection, TimeInfo> getTimeInfoMap() {
            return timeInfoMap;
        }

        @Override
        public void onEnter(EventContext context, VirtualFrame frame) {
            getTimeInfo(context).enterTime = System.nanoTime();
        }

        @Override
        public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {
            TimeInfo timeInfo = getTimeInfo(context);
            timeInfo.cumulativeTime += System.nanoTime() - timeInfo.enterTime;
        }

        @Override
        public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {
            onReturnValue(context, frame, exception);
        }

        protected abstract Object timeInfoCreated(EventContext context);

    }

    static class StatementListener extends BasicListener {
        private static final StatementListener singleton = new StatementListener();

        static long findTimer(RFunction func) {
            FunctionDefinitionNode fdn = (FunctionDefinitionNode) func.getRootNode();
            FunctionUID uid = fdn.getUID();
            long cumTime = 0;
            for (Map.Entry<SourceSection, TimeInfo> entry : StatementListener.singleton.getTimeInfoMap().entrySet()) {
                TimeInfo timeInfo = entry.getValue();
                Node node = (Node) timeInfo.getIdent();
                FunctionDefinitionNode entryFdn = (FunctionDefinitionNode) node.getRootNode();
                FunctionUID entryUid = entryFdn.getUID();
                if (entryUid.equals(uid)) {
                    // statement in "func"
                    cumTime += timeInfo.cumulativeTime;
                }
            }
            return cumTime;
        }

        static void installTimers() {
            if (enabled()) {
                SourceSectionFilter.Builder builder = SourceSectionFilter.newBuilder();
                builder.tagIs(RSyntaxTags.STATEMENT);
                SourceSectionFilter filter = builder.build();
                RInstrumentation.getInstrumenter().attachListener(filter, singleton);
            }
        }

        static void installTimer(RFunction func) {
            RInstrumentation.getInstrumenter().attachListener(RInstrumentation.createFunctionStatementFilter(func).build(), singleton);
        }

        @Override
        protected Node timeInfoCreated(EventContext context) {
            return context.getInstrumentedNode();
        }

        // PerfStats support

        static {
            RPerfStats.register(new PerfHandler());
        }

        static boolean enabled() {
            return RPerfStats.enabled(PerfHandler.NAME);
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

            @Override
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

            @Override
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

            @Override
            public String getName() {
                return NAME;
            }

            /**
             * Report the statement timing information at the end of the run. The report is per
             * function {@link FunctionUID}, which uniquely defines a function in the face of call
             * target splitting. Functions that consumed less time than requested threshold (default
             * 0) are not included in the report. The report is sorted by cumulative time.
             */
            @Override
            public void report() {
                Map<FunctionUID, TimingData> functionMap = new TreeMap<>();

                for (Map.Entry<SourceSection, TimeInfo> entry : StatementListener.singleton.getTimeInfoMap().entrySet()) {
                    TimeInfo timeInfo = entry.getValue();
                    Node node = (Node) timeInfo.getIdent();
                    if (node.getRootNode() instanceof FunctionDefinitionNode) {
                        FunctionDefinitionNode fdn = (FunctionDefinitionNode) node.getRootNode();
                        FunctionUID uid = fdn.getUID();
                        TimingData timingData = functionMap.get(uid);
                        if (timingData == null) {
                            timingData = new TimingData(uid);
                            functionMap.put(uid, timingData);
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

                RPerfStats.out().printf("Total (user) time %d ms%n", totalTime);
                for (TimingData t : sortedData) {
                    if (t.time > 0) {
                        if (t.time > threshold) {
                            FunctionIdentification fdi = RInstrumentation.getFunctionIdentification(t.functionUID);
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

        private abstract static class StatementVisitor implements RSyntaxNodeVisitor {
            protected final FunctionUID uid;

            StatementVisitor(FunctionUID uid) {
                this.uid = uid;
            }

            @Override
            public boolean visit(RSyntaxNode node, int depth) {
                if (node instanceof BlockNode) {
                    BlockNode sequenceNode = (BlockNode) node;
                    RNode[] block = sequenceNode.getSequence();
                    for (int i = 0; i < block.length; i++) {
                        RSyntaxNode n = block[i].unwrap().asRSyntaxNode();
                        if (!callback(n)) {
                            return false;
                        }
                    }
                }
                return true;
            }

            protected abstract boolean callback(RSyntaxNode node);

        }

        private static class LineTimesNodeVisitor extends StatementVisitor {
            private final long[] times;

            LineTimesNodeVisitor(FunctionUID uid, long[] time) {
                super(uid);
                this.times = time;
            }

            @Override
            protected boolean callback(RSyntaxNode node) {
                SourceSection ss = node.getSourceSection();
                TimeInfo timeInfo = singleton.getTimeInfoMap().get(ss);
                if (timeInfo != null) {
                    assert ss.getStartLine() != 0;
                    long stmtTime = millis(timeInfo.cumulativeTime);
                    times[0] += stmtTime;
                    times[ss.getStartLine()] += stmtTime;
                } else {
                    /*
                     * This happens because default arguments are not visited during the AST probe
                     * walk.
                     */
                }
                return true;
            }
        }

        private static long millis(long nanos) {
            return nanos / 1000000;
        }

        private static long[] createLineTimes(FunctionIdentification fdi) {
            /*
             * Although only those lines occupied by the function will actually have entries in the
             * array, addressing is easier if we allocate an array that is as long as the entire
             * source. Since there is never a line 0, we use that to compute the total.
             */
            final long[] times = new long[fdi.source.getLineCount() + 1];
            RSyntaxNode.accept(fdi.node.getBody().asNode(), 0, new LineTimesNodeVisitor(fdi.node.getUID(), times), false);
            return times;
        }
    }
}
