/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.utilities.CyclicAssumption;

public final class AttributeTracer {
    private static Deque<AttributeTracer.Listener> listeners = new ConcurrentLinkedDeque<>();
    @CompilationFinal static boolean enabled;

    private static final CyclicAssumption noAttributeTracingAssumption = new CyclicAssumption("data copying");

    private AttributeTracer() {
        // only static methods
    }

    /**
     * Adds a listener of attribute events.
     */
    public static void addListener(AttributeTracer.Listener listener) {
        listeners.addLast(listener);
    }

    /**
     * After calling this method attribute related events will be reported to the listener. This
     * invalidates global assumption and should be used with caution.
     */
    public static void setTracingState(boolean newState) {
        if (enabled != newState) {
            noAttributeTracingAssumption.invalidate();
            enabled = newState;
        }
    }

    public enum Change {
        CREATE,
        ADD,
        UPDATE,
        REMOVE,
        GROW
    }

    public static void reportAttributeChange(AttributeTracer.Change change, DynamicObject attrs, Object value) {
        if (enabled) {
            for (AttributeTracer.Listener listener : listeners) {
                listener.reportAttributeChange(change, attrs, value);
            }
        }
    }

    public interface Listener {
        /**
         * Reports attribute events to the listener. If there are no traced objects, this should
         * turn into no-op. {@code valuer} depends on the value of {@code change}. For
         * {@code CREATE} it is {@code null}, for {@code ADD,UPDATE, REMOVE} it is the
         * {@code String} name of the attribute and for {@code GROW} it is the (new) size.
         */
        void reportAttributeChange(AttributeTracer.Change change, DynamicObject attrs, Object value);
    }
}
