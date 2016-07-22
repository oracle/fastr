/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * Helper for tracing memory related events. All implementors of {@link RAbstractVector} are
 * expected to report to {@link MemoryTracer} and othes can listen to them through {@link Listener}
 * interface. Use method {@link #reportEvents()} to start the tracing.
 */
public final class MemoryTracer {
    private static Listener listener;
    private static final Assumption noMemoryTracingAssumption = Truffle.getRuntime().createAssumption();

    private MemoryTracer() {
        // only static methods
    }

    /**
     * Sets the listener of memory tracing events. For the time being there can only be one
     * listener. This can be extended to an array should we need more listeners.
     */
    public static void setListener(Listener newListener) {
        listener = newListener;
    }

    /**
     * After calling this method memory related events will be reported to the listener. This
     * invalidates global assumption and should be used with caution.
     */
    public static void reportEvents() {
        noMemoryTracingAssumption.invalidate();
    }

    /**
     * Reports copy event to the listener. If there are no traced objects, this should turn into
     * no-op.
     */
    public static void reportCopying(RAbstractVector source, RAbstractVector dest) {
        if (!noMemoryTracingAssumption.isValid() && listener != null) {
            listener.reportCopying(source, dest);
        }
    }

    public interface Listener {
        void reportCopying(RAbstractVector source, RAbstractVector dest);
    }
}
