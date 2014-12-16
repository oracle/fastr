/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

import java.util.*;

import com.oracle.truffle.r.options.*;

/**
 * Manage the creation/activation of handlers or performance analysis. Enabled by the
 * {@link FastROptions#PerfStats} option.
 *
 * The handlers are all registered statically from the class wanting to participate. The handles are
 * enabled selectively at runtime based on the command line option. An enabled handler gets a call
 * to its {@link Handler#initialize()} method so that it can enable its perf-mode behavior.
 */
public class RPerfAnalysis {

    public interface Handler {
        void initialize();

        String getName();

        void report();
    }

    public static class Histogram {
        private final long[] hist;
        private int maxSize = -1;

        public Histogram(int buckets) {
            hist = new long[buckets + 1];
        }

        public void inc(int size) {
            if (size > maxSize) {
                maxSize = size;
            }
            hist[effectiveBucket(size)]++;
        }

        public void dec(int size) {
            hist[effectiveBucket(size)]--;
        }

        public int numBuckets() {
            return hist.length - 1;
        }

        public long getMaxSize() {
            return maxSize;
        }

        public int effectiveBucket(int size) {
            if (size > hist.length - 1) {
                return hist.length - 1;
            } else {
                return size;
            }
        }

        public long getTotalCount() {
            long totalCount = 0;
            for (int i = 0; i < hist.length; i++) {
                totalCount += hist[i];
            }
            return totalCount;
        }

        public void report() {
            long maxCount = -1;
            for (int i = 0; i < hist.length; i++) {
                if (hist[i] > maxCount) {
                    maxCount = hist[i];
                }
            }
            int fieldWidth = Long.toString(maxCount).length() + 1;
            if (fieldWidth < 10) {
                fieldWidth = 10;
            }
            String fieldWidthString = Integer.toString(fieldWidth);
            String sFormat = "%-" + fieldWidthString + "s";
            String dFormat = "%-" + fieldWidthString + "d";
            System.out.printf(sFormat, "Size");
            for (int i = 0; i < hist.length - 1; i++) {
                System.out.printf(dFormat, i);
            }
            System.out.printf(sFormat, "> " + (hist.length - 1));
            System.out.println();
            System.out.printf(sFormat, "Count");
            for (int i = 0; i < hist.length - 1; i++) {
                System.out.printf(dFormat, hist[i]);
            }
            System.out.printf(dFormat, hist[hist.length - 1]);
            System.out.println();
        }

    }

    private static final ArrayList<Handler> handlers = new ArrayList<>();

    public static void register(Handler handler) {
        handlers.add(handler);
        if (enabled(handler.getName())) {
            handler.initialize();
        }
    }

    private static boolean enabled(String name) {
        return FastROptions.matchesElement(name, FastROptions.PerfStats);
    }

    private static boolean reporting;

    /**
     * Called just before FastR exits.
     */
    public static void report() {
        if (reporting) {
            // some crash in a reporter caused a recursive entry
            return;
        }
        reporting = true;
        for (Handler handler : handlers) {
            if (enabled(handler.getName())) {
                handler.report();
            }
        }
    }
}
