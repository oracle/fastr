/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.truffle.r.nodes.instrument;

import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.Probe.ProbeListener;
import com.oracle.truffle.api.instrument.StandardSyntaxTag;
import com.oracle.truffle.api.instrument.SyntaxTag;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.nodes.function.FunctionUID;
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
