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

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.utilities.CyclicAssumption;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * Helper for tracing memory copying events, as used by the {@code tracemem} bultin. All
 * implementors of {@link RAbstractVector} are expected to report to {@link MemoryCopyTracer} and
 * others can listen to them through {@link Listener} interface. Use method
 * {@link #setTracingState(boolean)} to enable/disable the tracing.
 */
public final class MemoryCopyTracer {
    private static Deque<Listener> listeners = new ConcurrentLinkedDeque<>();
    @CompilationFinal private static boolean enabled;

    private static final CyclicAssumption noMemoryCopyTracingAssumption = new CyclicAssumption("data copying");

    private MemoryCopyTracer() {
        // only static methods
    }

    /**
     * Adds a listener of memory copying events.
     */
    public static void addListener(Listener listener) {
        listeners.addLast(listener);
    }

    /**
     * After calling this method memory related events will be reported to the listener. This
     * invalidates global assumption and should be used with caution.
     */
    public static void setTracingState(boolean newState) {
        if (enabled != newState) {
            noMemoryCopyTracingAssumption.invalidate();
            enabled = newState;
        }
    }

    /**
     * Reports copy event to the listener. If there are no traced objects, this should turn into
     * no-op.
     */
    public static void reportCopying(RAbstractVector source, RAbstractVector dest) {
        if (enabled) {
            for (Listener listener : listeners) {
                listener.reportCopying(source, dest);
            }
        }
    }

    public interface Listener {
        void reportCopying(RAbstractVector source, RAbstractVector dest);
    }
}
