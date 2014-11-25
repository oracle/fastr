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
package com.oracle.truffle.r.nodes.instrument;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.Instrument;
import com.oracle.truffle.api.instrument.impl.SimpleEventReceiver;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.function.FunctionUID;
import java.util.WeakHashMap;

public class REntryCounters {

    private static WeakHashMap<Object, Basic> counterMap = new WeakHashMap<>();

    public static class Basic {

        protected int enterCount;
        protected int exitCount;
        public final Instrument instrument;

        public Basic(Object tag) {
            instrument = Instrument.create(new SimpleEventReceiver() {

                @Override
                public void enter(Node node, VirtualFrame frame) {
                    enterCount++;
                }

                @Override
                public void returnAny(Node node, VirtualFrame frame) {
                    exitCount++;
                }
            }, "R node entry counter");

            counterMap.put(tag, this);
        }

        public int getEnterCount() {
            return enterCount;
        }

        public int getExitCount() {
            return exitCount;
        }
    }

    public static class Function extends Basic {

        public Function(FunctionUID uuid) {
            super(uuid);
        }

    }

    public static REntryCounters.Basic findCounter(Object tag) {
        return counterMap.get(tag);
    }

}
