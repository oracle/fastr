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

/**
 * Manage the creation/activation of handlers or performance analysis.
 */
public class RPerfAnalysis {

    public interface Handler {
        void initialize();

        String getName();

        void report();

    }

    public static void initialize() {
        // Currently we use a system property to enable the perf analysis
        String prop = System.getProperty("fastr.perf");
        if (prop == null) {
            return;
        }
        boolean all = prop.length() == 0;
        String[] names = all ? null : prop.split(",");
        try {
            for (Class<? extends Handler> perfClass : RPerfClasses.CLASSES) {
                Handler handler = perfClass.newInstance();
                String name = handler.getName();
                if (all || enabled(names, name)) {
                    handlers.add(handler);
                    handler.initialize();
                }
            }
        } catch (Exception ex) {
            Utils.fail("failed to instantiate RPerf class: " + ex);
        }
    }

    private static boolean enabled(String[] names, String name) {
        for (String hname : names) {
            if (hname.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean reporting;

    public static void report() {
        if (reporting) {
            // some crash in a reporter caused a recursive entry
            return;
        }
        reporting = true;
        for (Handler handler : handlers) {
            handler.report();
        }
    }

    private static ArrayList<Handler> handlers = new ArrayList<>();

    public static void registerHandler(Handler handler) {
        handlers.add(handler);
    }
}
