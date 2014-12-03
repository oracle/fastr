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

import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.Probe.ProbeListener;
import com.oracle.truffle.api.instrument.StandardSyntaxTag;
import com.oracle.truffle.api.instrument.SyntaxTag;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.nodes.function.FunctionUID;
import com.oracle.truffle.r.nodes.instrument.trace.*;
import com.oracle.truffle.r.options.FastROptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles the initialization of the instrumentation system.
 */
public class RInstrument {

    private static Map<FunctionUID, ArrayList<Probe>> probeMap = new HashMap<>();

    private static class RProbeListener implements ProbeListener {

        @Override
        public void startASTProbing(Source source) {
            System.console();
        }

        @Override
        public void newProbeInserted(Probe probe) {
        }

        @Override
        public void probeTaggedAs(Probe probe, SyntaxTag tag, Object tagValue) {
            if (tag == RSyntaxTag.FUNCTION_BODY) {
                putProbe((FunctionUID) tagValue, probe);
                if (FastROptions.AddFunctionCounters.getValue()) {
                    probe.attach(new REntryCounters.Function((FunctionUID) tagValue).instrument);
                }
            } else if (tag == StandardSyntaxTag.START_METHOD) {
                putProbe((FunctionUID) tagValue, probe);
                if (FastROptions.TraceCalls.getValue()) {
                    TraceHandling.attachTraceHandler((FunctionUID) tagValue);
                }
            }
        }

        @Override
        public void endASTProbing(Source source) {
        }

    }

    private static void putProbe(FunctionUID uid, Probe probe) {
        ArrayList<Probe> list = probeMap.get(uid);
        if (list == null) {
            list = new ArrayList<>();
            probeMap.put(uid, list);
        }
        list.add(probe);
    }

    public static void initialize() {
        Probe.registerASTProber(RASTDebugProber.getRASTProber());
        Probe.addProbeListener(new RProbeListener());
    }

    public static Probe findSingleProbe(FunctionUID uid, SyntaxTag tag) {
        ArrayList<Probe> list = probeMap.get(uid);
        if (list != null) {
            for (Probe probe : list) {
                if (probe.isTaggedAs(tag)) {
                    return probe;
                }
            }
        }
        return null;
    }
}
