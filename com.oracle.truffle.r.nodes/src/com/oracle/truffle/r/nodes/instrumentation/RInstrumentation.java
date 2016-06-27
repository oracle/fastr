/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.instrumentation;

import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.FunctionUID;
import com.oracle.truffle.r.runtime.RPerfStats;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Handles the initialization of the (NEW) instrumentation system which sets up various instruments
 * depending on command line options.
 *
 */
public class RInstrumentation {

    /**
     * Collects together all the relevant data for a function, keyed by the {@link FunctionUID},
     * which is unique, for {@link RPerfStats} use.
     */
    private static Map<FunctionUID, FunctionData> functionMap;

    /**
     * Created lazily as needed.
     */
    static class FunctionIdentification {
        public final Source source;
        public final String name;
        public final String origin;
        public final FunctionDefinitionNode node;

        FunctionIdentification(Source source, String name, String origin, FunctionDefinitionNode node) {
            this.source = source;
            this.name = name;
            this.origin = origin;
            this.node = node;
        }
    }

    /**
     * Created for every {@link FunctionDefinitionNode}. maybe could be lazier.
     */
    private static class FunctionData {
        private final FunctionUID uid;
        private final FunctionDefinitionNode fdn;
        private FunctionIdentification ident;

        FunctionData(FunctionUID uid, FunctionDefinitionNode fdn) {
            this.uid = uid;
            this.fdn = fdn;
        }

        @SuppressWarnings("deprecation")
        private FunctionIdentification getIdentification() {
            if (ident == null) {
                SourceSection ss = fdn.getSourceSection();
                /*
                 * The default for "name" is the description associated with "fdn". If the function
                 * was parsed from text this will be the variable name the function value was
                 * assigned to, or the first 40 characters of the definition if anonymous.
                 */
                String idName = fdn.toString();
                Source idSource = null;
                String idOrigin = null;
                if (ss.getSource() != null) {
                    idSource = ss.getSource();
                    String sourceName = idSource.getName();
                    idOrigin = sourceName;
                    if (sourceName.startsWith("<package:")) {
                        // try to find the name in the package environments
                        // format of sourceName is "<package"xxx deparse>"
                        String functionName = findFunctionName(uid, sourceName.substring(1, sourceName.lastIndexOf(' ')));
                        if (functionName != null) {
                            idName = functionName;
                        }
                    } else {
                        idOrigin = sourceName;
                    }
                } else {
                    // One of the RSyntaxNode "unavailable"s.
                    idOrigin = idName;
                    idSource = Source.fromText(idName, idName);
                }
                ident = new FunctionIdentification(idSource, idName, idOrigin, fdn);
            }
            return ident;

        }
    }

    /**
     * The function names that were requested to be used in implicit {@code debug(f)} calls, when
     * those functions are defined. Global to all contexts.
     */
    @CompilationFinal private static String[] debugFunctionNames;

    /**
     * Called back from {@link FunctionDefinitionNode} so that we can record the {@link FunctionUID}
     * and use {@code fdn} as the canonical {@link FunctionDefinitionNode}.
     *
     * @param fdn
     */
    public static void registerFunctionDefinition(FunctionDefinitionNode fdn) {
        // For PerfStats we need to record the info on fdn for the report
        if (functionMap != null) {
            FunctionUID uid = fdn.getUID();
            FunctionData fd = functionMap.get(uid);
            if (fd != null) {
                // duplicate
                return;
            }
            assert fd == null;
            functionMap.put(uid, new FunctionData(uid, fdn));
        }
    }

    static FunctionIdentification getFunctionIdentification(FunctionUID uid) {
        return functionMap.get(uid).getIdentification();
    }

    public static FunctionDefinitionNode getFunctionDefinitionNode(RFunction func) {
        assert !func.isBuiltin();
        return (FunctionDefinitionNode) func.getRootNode();
    }

    /**
     * Create a filter that matches all the statement nodes in {@code func}.
     */
    static SourceSectionFilter.Builder createFunctionStatementFilter(RFunction func) {
        return createFunctionFilter(func, StandardTags.StatementTag.class);
    }

    public static SourceSectionFilter.Builder createFunctionStatementFilter(FunctionDefinitionNode fdn) {
        return createFunctionFilter(fdn, StandardTags.StatementTag.class);
    }

    static SourceSectionFilter.Builder createFunctionFilter(RFunction func, Class<?> tag) {
        FunctionDefinitionNode fdn = getFunctionDefinitionNode(func);
        return createFunctionFilter(fdn, tag);
    }

    public static SourceSectionFilter.Builder createFunctionFilter(FunctionDefinitionNode fdn, Class<?> tag) {
        /* Filter needs to check for statement tags in the range of the function in the Source */
        SourceSectionFilter.Builder builder = SourceSectionFilter.newBuilder();
        builder.tagIs(tag);
        SourceSection fdns = fdn.getSourceSection();
        builder.indexIn(fdns.getCharIndex(), fdns.getCharLength());
        builder.sourceIs(fdns.getSource());
        builder.rootSourceSectionEquals(fdns);
        return builder;

    }

    /**
     * Create a filter that matches the start function node (tag) in {@code func}.
     */
    public static SourceSectionFilter.Builder createFunctionStartFilter(RFunction func) {
        FunctionDefinitionNode fdn = (FunctionDefinitionNode) func.getRootNode();
        SourceSectionFilter.Builder builder = SourceSectionFilter.newBuilder();
        builder.tagIs(StandardTags.RootTag.class);
        builder.sourceSectionEquals(fdn.getBody().getSourceSection());
        return builder;
    }

    /**
     * Activate the instrumentation system for {@code context}. Currently this simply checks for the
     * global (command-line) options for tracing and timing. They are applied to every context.
     */
    public static void activate(@SuppressWarnings("unused") RContext context) {
        String rdebugValue = FastROptions.Rdebug.getStringValue();
        if (rdebugValue != null) {
            debugFunctionNames = rdebugValue.split(",");
        }
        if (REntryCounters.FunctionListener.enabled() || RNodeTimer.StatementListener.enabled()) {
            functionMap = new HashMap<>();
            REntryCounters.FunctionListener.installCounters();
            RNodeTimer.StatementListener.installTimers();
        }
        // Check for function tracing
        RContext.getRRuntimeASTAccess().traceAllFunctions();
    }

    public static Instrumenter getInstrumenter() {
        return RContext.getInstance().getInstrumenter();
    }

    public static void checkDebugRequested(RFunction func) {
        if (debugFunctionNames != null) {
            FunctionDefinitionNode fdn = (FunctionDefinitionNode) func.getRootNode();
            for (String debugFunctionName : debugFunctionNames) {
                if (debugFunctionName.equals(fdn.toString())) {
                    RContext.getRRuntimeASTAccess().enableDebug(func);
                }
            }
        }
    }

    private static Map<FunctionUID, String> functionNameMap;

    /**
     * Attempts to locate a name for an (assumed) builtin or global function. Returns {@code null}
     * if not found.
     */
    private static String findFunctionName(FunctionUID uid, String packageName) {
        if (functionNameMap == null) {
            functionNameMap = new HashMap<>();
        }
        String name = functionNameMap.get(uid);
        if (name == null) {
            name = findFunctionInPackage(uid, packageName);
        }
        return name;
    }

    /**
     * Try to find the function identified by uid in the given package. N.B. If we have the uid, the
     * promise identifying the lazily loaded function must have been evaluated! So there is no need
     * to evaluate any promises. N.B. For packages, we must use the namespace env as that contains
     * public and private functions.
     */
    private static String findFunctionInPackage(FunctionUID uid, String packageName) {
        if (packageName == null) {
            return findFunctionInEnv(uid, REnvironment.globalEnv());
        }
        REnvironment env = REnvironment.lookupOnSearchPath(packageName);
        env = env.getPackageNamespaceEnv();
        return findFunctionInEnv(uid, env);
    }

    private static String findFunctionInEnv(FunctionUID uid, REnvironment env) {
        // This is rather inefficient, but it doesn't matter
        RStringVector names = env.ls(true, null, false);
        for (int i = 0; i < names.getLength(); i++) {
            String name = names.getDataAt(i);
            Object val = env.get(name);
            if (val instanceof RPromise) {
                RPromise prVal = (RPromise) val;
                if (prVal.isEvaluated()) {
                    val = prVal.getValue();
                } else {
                    continue;
                }
            }
            if (val instanceof RFunction) {
                RFunction func = (RFunction) val;
                RootNode rootNode = func.getRootNode();
                if (rootNode instanceof FunctionDefinitionNode) {
                    FunctionDefinitionNode fdn = (FunctionDefinitionNode) rootNode;
                    if (fdn.getUID().equals(uid)) {
                        functionNameMap.put(fdn.getUID(), name);
                        return name;
                    }
                }
            }
        }
        // Most likely a nested function, which is ok
        // because they are not lazy and so have names from the parser.
        return null;
    }
}
