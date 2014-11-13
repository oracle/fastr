/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
