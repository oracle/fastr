/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.instrument.debug.*;
import com.oracle.truffle.r.nodes.instrument.trace.*;
import com.oracle.truffle.r.options.FastROptions;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles the initialization of the instrumentation system which sets up various instruments
 * depending on command line options.
 *
 */
public class RInstrument {

    /**
     * Collects together all the {@linkProbe} instances for a given function.
     */
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
                if (REntryCounters.Function.enabled()) {
                    probe.attach(new REntryCounters.Function((FunctionUID) tagValue).instrument);
                }
            } else if (tag == StandardSyntaxTag.START_METHOD) {
                putProbe((FunctionUID) tagValue, probe);
                if (FastROptions.TraceCalls.getValue()) {
                    TraceHandling.attachTraceHandler((FunctionUID) tagValue);
                }
            } else if (tag == StandardSyntaxTag.STATEMENT) {
                if (RNodeTimer.Statement.enabled()) {
                    probe.attach(new RNodeTimer.Statement(tagValue).instrument);
                }
            }
        }

        @Override
        public void endASTProbing(Source source) {
        }

    }

    /**
     * Controls whether ASTs are instrumented after parse. The default value controlled by
     * {@link FastROptions#Instrument}.
     */
    private static boolean instrumentingEnabled;

    /**
     * The function names that were requested to be used in implicit {@code debug(f)} calls, when
     * those functions are defined.
     */
    private static String[] debugFunctionNames;

    private static void putProbe(FunctionUID uid, Probe probe) {
        ArrayList<Probe> list = probeMap.get(uid);
        if (list == null) {
            list = new ArrayList<>();
            probeMap.put(uid, list);
        }
        list.add(probe);
    }

    /**
     * Initialize the instrumentation system. {@link RASTProber} is registered to tag interesting
     * nodes. {@link RProbeListener} is added to (optionally) add probes to nodes tagged by
     * {@link RASTProber}.
     *
     * As a convenience we force {@link #instrumentingEnabled} on if those {@code RPerfStats}
     * features that need it are also enabled.
     */
    public static void initialize() {
        // @formatter:off
        instrumentingEnabled = FastROptions.Instrument.getValue() || FastROptions.TraceCalls.getValue() || FastROptions.Rdebug.getValue() != null ||
                        REntryCounters.Function.enabled() || RNodeTimer.Statement.enabled();
        // @formatter:on
        if (instrumentingEnabled) {
            Probe.registerASTProber(RASTProber.getRASTProber());
            Probe.addProbeListener(new RProbeListener());
        }
        String rdebugValue = FastROptions.Rdebug.getValue();
        if (rdebugValue != null) {
            debugFunctionNames = rdebugValue.split(",");
        }
    }

    public static boolean instrumentingEnabled() {
        return instrumentingEnabled;
    }

    public static void checkDebugRequested(String name, RFunction func) {
        if (debugFunctionNames != null) {
            for (String debugFunctionName : debugFunctionNames) {
                if (debugFunctionName.equals(name)) {
                    DebugHandling.enableDebug(func, "", RNull.instance, false);
                }
            }
        }
    }

    /**
     * Returns the {@link Probe} with the given tag for the given function, or {@code null} if not
     * found.
     */
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

    /**
     * Finds a {@link FunctionDefinitionNode} that has the given {@code uid}. Owing to splitting,
     * this is not necessarily unique.
     */
    public static FunctionDefinitionNode getFunctionDefinitionNode(FunctionUID uid) {
        for (RootCallTarget target : Truffle.getRuntime().getCallTargets()) {
            RootNode rootNode = target.getRootNode();
            if (rootNode instanceof FunctionDefinitionNode) {
                FunctionDefinitionNode fdn = (FunctionDefinitionNode) rootNode;
                FunctionUID fdnuid = fdn.getUID();
                if (fdnuid != null && fdnuid.compareTo(uid) == 0) {
                    return fdn;
                }
            }
        }
        return null;
    }

    private static Map<FunctionUID, String> functionMap;

    /**
     * Attempts to locare a name for an (assumed) builtin or global function. Returns {@code null}
     * if not found.
     */
    public static String findFunctionName(FunctionUID uid) {
        if (functionMap == null) {
            initFunctionMap();
        }
        return functionMap.get(uid);
    }

    private static void initFunctionMap() {
        functionMap = new HashMap<>();
        REnvironment env = REnvironment.globalEnv();
        while (env != REnvironment.emptyEnv()) {
            // This is rather inefficient, but doesn't matter
            RStringVector names = env.ls(true, null);
            for (int i = 0; i < names.getLength(); i++) {
                String name = names.getDataAt(i);
                Object val = env.get(name);
                if (val instanceof RFunction) {
                    RFunction func = (RFunction) val;
                    FunctionDefinitionNode fdn = (FunctionDefinitionNode) func.getRootNode();
                    if (fdn.getUID() != null) {
                        functionMap.put(fdn.getUID(), name);
                    }
                }
            }
            env = env.getParent();
        }
    }

}
