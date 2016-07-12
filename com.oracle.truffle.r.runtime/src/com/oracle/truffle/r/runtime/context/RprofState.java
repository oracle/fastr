/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.context;

import java.io.PrintWriter;

import com.oracle.truffle.api.instrumentation.ExecutionEventListener;

public class RprofState implements RContext.ContextState {
    private PrintWriter out;
    private Thread profileThread;
    private ExecutionEventListener statementListener;
    private long intervalInMillis;
    private boolean lineProfiling;

    public static RprofState newContext(@SuppressWarnings("unused") RContext context) {
        return new RprofState();
    }

    public void initialize(PrintWriter outA, Thread profileThreadA, ExecutionEventListener statementListenerA, long intervalInMillisA,
                    boolean lineProfilingA) {
        this.out = outA;
        this.profileThread = profileThreadA;
        this.statementListener = statementListenerA;
        this.intervalInMillis = intervalInMillisA;
        this.lineProfiling = lineProfilingA;
    }

    public boolean lineProfiling() {
        return lineProfiling;
    }

    public PrintWriter out() {
        return out;
    }

    public long intervalInMillis() {
        return intervalInMillis;
    }

    public ExecutionEventListener statementListener() {
        return statementListener;
    }

    public Thread profileThread() {
        return profileThread;
    }

}
