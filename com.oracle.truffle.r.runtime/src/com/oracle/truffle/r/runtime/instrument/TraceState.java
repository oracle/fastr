/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.instrument;

import java.util.WeakHashMap;

import com.oracle.truffle.r.runtime.FunctionUID;
import com.oracle.truffle.r.runtime.context.RContext;

/**
 * The tracingState is a global variable in R, so we store it (and the associated listener objects)
 * in the {@link RContext}. To finesse the temporary existence of two instrumentation frameworks the
 * receiver is typed as {@link Object}.
 *
 */
public class TraceState {
    public static class ContextStateImpl implements RContext.ContextState {

        /**
         * Records all functions that have trace listeners installed.
         */
        private final WeakHashMap<FunctionUID, Object> listenerMap = new WeakHashMap<>();
        private boolean tracingState = true;

        public void put(FunctionUID functionUID, Object listener) {
            listenerMap.put(functionUID, listener);
        }

        public Object get(FunctionUID functionUID) {
            return listenerMap.get(functionUID);
        }

        public boolean setTracingState(boolean state) {
            boolean prev = tracingState;
            tracingState = state;
            return prev;
        }

        public boolean getTracingState() {
            return tracingState;
        }

        public Object[] getListeners() {
            Object[] result = new Object[listenerMap.size()];
            listenerMap.values().toArray(result);
            return result;
        }
    }

    public static ContextStateImpl newContext(@SuppressWarnings("unused") RContext context) {
        return new ContextStateImpl();
    }

}
