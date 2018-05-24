/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.ffi;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.context.RContext;

public class RFFILog {
    /**
     * Set this to {@code true} when it is not possible to set {@link FastROptions}.
     */
    private static boolean alwaysTrace;
    /**
     * Is set by initialization and caches whether we are tracing.
     */
    @CompilationFinal public static boolean traceEnabled;

    public static boolean traceInitialized;

    /**
     * Always trace to a file because stdout is problematic for embedded mode.
     */
    private static final String TRACEFILE = "fastr_trace_nativecalls";
    private static PrintWriter traceStream;

    /**
     * Handles the initialization of the RFFI downcalls/upcall tracing implementation.
     */
    public static synchronized void initializeTracing() {
        if (!traceInitialized) {
            traceInitialized = true;
            traceEnabled = alwaysTrace || FastROptions.TraceNativeCalls.getBooleanValue();
            if (traceEnabled) {
                if (traceStream == null) {
                    initTraceStream();
                }
            }
        }
    }

    public static boolean traceEnabled() {
        return traceEnabled;
    }

    public static synchronized void write(String value) {
        assert traceEnabled : "check traceEnabled() before calling RFFILog.write";
        traceStream.write(value);
        traceStream.append(" [ctx:").append("" + System.identityHashCode(RContext.getInstance()));
        traceStream.append(",thread:").append("" + Thread.currentThread().getId()).append(']');
        traceStream.write('\n');
        traceStream.flush();
    }

    public static synchronized void printf(String fmt, Object... args) {
        assert traceEnabled : "check traceEnabled() before calling RFFILog.printf";
        traceStream.printf(fmt, args);
        traceStream.write('\n');
        traceStream.flush();
    }

    public static synchronized void printStackTrace() {
        assert traceEnabled : "check traceEnabled() before calling RFFILog.printStackTrace";
        new RuntimeException().printStackTrace(traceStream);
    }

    private static void initTraceStream() {
        Path tracePath = Utils.getLogPath(TRACEFILE);
        if (tracePath != null) {
            try {
                traceStream = new PrintWriter(tracePath.toString());
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
                System.exit(1);
            }
        } else {
            System.err.println("Cannot write trace log file (tried current working directory, user home directory, FastR home directory).");
            System.exit(1);
        }
    }
}
