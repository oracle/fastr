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
package com.oracle.truffle.r.runtime;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * Manage the creation/activation of handlers or performance analysis. Enabled by the
 * {@code FastROptions.Option.PerfStats} option.
 *
 * The handlers are all registered statically from the class wanting to participate. The handlers
 * are enabled selectively at runtime based on the command line option. An enabled handler gets a
 * call to its {@link Handler#initialize(String)} method so that it can enable its perf-mode
 * behavior.
 */
public class RPerfStats {

    public interface Handler {
        /**
         * Called on startup if enabled to initialize any necessary state.
         *
         * @param optionText any text after the handler name on the command line
         */
        void initialize(String optionText);

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
            out().printf(sFormat, "Size");
            for (int i = 0; i < hist.length - 1; i++) {
                out().printf(dFormat, i);
            }
            out().printf(sFormat, "> " + (hist.length - 1));
            out().println();
            out().printf(sFormat, "Count");
            for (int i = 0; i < hist.length - 1; i++) {
                out().printf(dFormat, hist[i]);
            }
            out().printf(dFormat, hist[hist.length - 1]);
            out().println();
        }
    }

    private static final ArrayList<Handler> handlers = new ArrayList<>();
    private static boolean initialized;
    private static PrintStream out = System.out;

    public static PrintStream out() {
        return out;
    }

    /**
     * Register a {@link Handler}. This should be done in a {@code static} block so that, in an AOT
     * VM, all handlers are included in the image. N.B. Owing to dynamic class loading in a standard
     * VM, this may be called after {@link RPerfStats#initialize}, so we may have to invoke
     * {@code handler.initialize} from here.
     */
    public static void register(Handler handler) {
        handlers.add(handler);
        if (initialized) {
            String optionText = getOptionText(handler.getName());
            if (optionText != null) {
                handler.initialize(optionText);
            }
        }
    }

    /**
     * Called by the engine startup sequence to initialize all registered handlers.
     */
    public static void initialize() {
        for (Handler handler : handlers) {
            String optionText = getOptionText(handler.getName());
            if (optionText != null) {
                handler.initialize(optionText);
            }
        }
        initialized = true;
    }

    private static String getOptionText(String name) {
        String optionValue = getPerfStatsOption(name);
        if (optionValue != null) {
            if (optionValue.length() > 0) {
                optionValue = optionValue.substring(name.length());
            }
        }
        return optionValue;
    }

    private static String getPerfStatsOption(String name) {
        return FastROptions.matchesElement(name, FastROptions.PerfStats.getStringValue());
    }

    public static boolean enabled(String name) {
        return getPerfStatsOption(name) != null;
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
        String file = FastROptions.PerfStatsFile.getStringValue();
        if (file != null) {
            try {
                out = new PrintStream(new FileOutputStream(file));
            } catch (IOException ex) {
                System.err.print("PerfStats: can't open " + file + " for output, using stdout");
            }
        }
        boolean headerOutput = false;
        for (Handler handler : handlers) {
            if (enabled(handler.getName())) {
                if (!headerOutput) {
                    out().println("RPerfStats Reports");
                    headerOutput = true;
                }
                handler.report();
            }
        }
    }
}
