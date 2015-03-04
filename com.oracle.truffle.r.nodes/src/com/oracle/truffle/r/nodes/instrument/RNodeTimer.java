/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.instrument.impl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;

/**
 * Basic support for adding as timer to a node. A timer must be identified with some unique value
 * that enables it to be retrieved.
 *
 * The {@link Basic#instrument} field is used to attach the timer to a {@link Probe}.
 *
 */

public class RNodeTimer {
    private static WeakHashMap<Object, Basic> timerMap = new WeakHashMap<>();

    public static class Basic {

        protected long enterTime;
        protected long cumulativeTime;
        public final Instrument instrument;

        public Basic(Object tag) {
            instrument = Instrument.create(new SimpleEventReceiver() {

                @Override
                public void enter(Node node, VirtualFrame frame) {
                    enterTime = System.nanoTime();
                }

                @Override
                public void returnAny(Node node, VirtualFrame frame) {
                    cumulativeTime += System.nanoTime() - enterTime;
                }
            }, "R node timer");

            timerMap.put(tag, this);
        }

        public long getTime() {
            return cumulativeTime;
        }

    }

    public static class Statement extends Basic {
        /**
         * Tag is the {@link RNode}.
         */
        public Statement(Object tag) {
            super(tag);
        }

        static {
            RPerfAnalysis.register(new PerfHandler());
        }

        private static class StatementData implements Comparable<StatementData> {
            long time;
            RNode node;

            StatementData(long time, RNode node) {
                this.time = time;
                this.node = node;
            }

            public int compareTo(StatementData o) {
                if (time < o.time) {
                    return 1;
                } else if (time > o.time) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }

        private static class PerfHandler implements RPerfAnalysis.Handler {
            static final String NAME = "timing";

            public void initialize() {
            }

            public String getName() {
                return NAME;
            }

            private static final long THRESHOLD = 0; // 1000000000L;

            public void report() {
                ArrayList<StatementData> data = new ArrayList<>();
                for (Map.Entry<Object, Basic> entry : timerMap.entrySet()) {
                    if (entry.getValue() instanceof Statement) {
                        if (entry.getValue().cumulativeTime > THRESHOLD) {
                            data.add(new StatementData(entry.getValue().cumulativeTime, (RNode) entry.getKey()));
                        }
                    }
                }
                StatementData[] sortedData = new StatementData[data.size()];
                data.toArray(sortedData);
                Arrays.sort(sortedData);
                for (StatementData sd : sortedData) {
                    System.out.printf("%10d: %s%n", sd.time, safeGetCode(sd.node));
                }
            }

            private static String safeGetCode(Node node) {
                SourceSection ss = node.getSourceSection();
                if (ss != null) {
                    return ss.getCode();
                } else {
                    FunctionDefinitionNode rootNode = (FunctionDefinitionNode) node.getRootNode();
                    RDeparse.State state = RDeparse.State.createPrintableState();
                    rootNode.deparse(state);
                    return state.toString();
                }
            }

        }

        public static boolean enabled() {
            return RPerfAnalysis.enabled(PerfHandler.NAME);
        }

        /**
         * Return the timer tagged with {@code tag}, or {@code null} if not found.
         */
        public static RNodeTimer.Basic findTimer(Object tag) {
            return timerMap.get(tag);
        }
    }

}
