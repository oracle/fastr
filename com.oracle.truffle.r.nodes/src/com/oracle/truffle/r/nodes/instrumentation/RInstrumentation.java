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

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.instrumentation.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.instrumentation.debug.*;
import com.oracle.truffle.r.nodes.instrumentation.trace.TraceHandling;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.instrument.*;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNodeVisitor;

/**
 * Handles the initialization of the (NEW) instrumentation system which sets up various instruments
 * depending on command line options.
 *
 */
public class RInstrumentation {

    /**
     * Collects together all the relevant data for a function, keyed by the {@link FunctionUID},
     * which is unique.
     */
    private static Map<FunctionUID, FunctionData> functionMap = new HashMap<>();

    /**
     * Created lazily as needed.
     */
    public static class FunctionIdentification {
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

    @CompilationFinal private static Instrumenter instrumenter;

    /**
     * The function names that were requested to be used in implicit {@code debug(f)} calls, when
     * those functions are defined.
     */
    @CompilationFinal private static String[] debugFunctionNames;

    /**
     * Called back from {@link FunctionDefinitionNode} so that we can record the {@link FunctionUID}
     * and use {@code fdn} as the canonical {@link FunctionDefinitionNode}.
     *
     * TODO Remove hack to tag with {@code uidTag} once {@link SourceSectionFilter} provides a
     * builtin way.
     *
     * @param fdn
     */
    public static void registerFunctionDefinition(FunctionDefinitionNode fdn) {
        FunctionUID uid = fdn.getUID();
        FunctionData fd = functionMap.get(uid);
        if (fd != null) {
            // duplicate
            return;
        }
        assert fd == null;
        functionMap.put(uid, new FunctionData(uid, fdn));
        String uidTag = RSyntaxTags.createUidTag(uid);
        RSyntaxNode.accept(fdn, 0, new RSyntaxNodeVisitor() {

            public boolean visit(RSyntaxNode node, int depth) {
                SourceSection ss = node.getSourceSection();
                assert ss != null;
                String[] tags = RSyntaxTags.getTags(ss);
                if (tags != null) {
                    String[] updatedTags = new String[tags.length + 1];
                    System.arraycopy(tags, 0, updatedTags, 0, tags.length);
                    updatedTags[tags.length] = uidTag;
                    node.setSourceSection(ss.withTags(updatedTags));
                }
                return true;
            }

        }, false);
    }

    public static FunctionIdentification getFunctionIdentification(FunctionUID uid) {
        return functionMap.get(uid).getIdentification();
    }

    public static FunctionDefinitionNode getFunctionDefinitionNode(RFunction func) {
        assert !func.isBuiltin();
        return (FunctionDefinitionNode) func.getRootNode();
    }

    /**
     * Create a filter that matches all the statement nodes in {@code func}.
     */
    public static SourceSectionFilter.Builder createFunctionStatementFilter(RFunction func) {
        return createFunctionFilter(func, RSyntaxTags.STATEMENT);
    }

    public static SourceSectionFilter.Builder createFunctionStatementFilter(FunctionDefinitionNode fdn) {
        return createFunctionFilter(fdn, RSyntaxTags.STATEMENT);
    }

    public static SourceSectionFilter.Builder createFunctionFilter(RFunction func, String tag) {
        FunctionDefinitionNode fdn = getFunctionDefinitionNode(func);
        return createFunctionFilter(fdn, tag);
    }

    public static SourceSectionFilter.Builder createFunctionFilter(FunctionDefinitionNode fdn, String tag) {
        /* Filter needs to check for statement tags in the range of the function in the Source */
        SourceSectionFilter.Builder builder = SourceSectionFilter.newBuilder();
        builder.tagIs(tag);
        SourceSection fdns = fdn.getSourceSection();
        builder.indexIn(fdns.getCharIndex(), fdns.getCharLength());
        builder.sourceIs(fdns.getSource());
        // TODO remove when UID tag redundant
        builder.tagIs(RSyntaxTags.createUidTag(fdn.getUID()));
        return builder;

    }

    /**
     * Create a filter that matches the start function node (tag) in {@code func}.
     */
    public static SourceSectionFilter.Builder createFunctionStartFilter(RFunction func) {
        FunctionDefinitionNode fdn = (FunctionDefinitionNode) func.getRootNode();
        SourceSectionFilter.Builder builder = SourceSectionFilter.newBuilder();
        builder.tagIs(RSyntaxTags.START_FUNCTION);
        FunctionStatementsNode fsn = ((FunctionBodyNode) fdn.getBody()).getStatements();
        builder.sourceSectionEquals(fsn.getSourceSection());
        return builder;
    }

    /**
     * Initialize the instrumentation system.
     *
     */
    public static void initialize(Instrumenter instrumenterArg) {
        instrumenter = instrumenterArg;
        if (FastROptions.LoadPkgSourcesIndex.getBooleanValue()) {
            RPackageSource.initialize();
        }
        String rdebugValue = FastROptions.Rdebug.getStringValue();
        if (rdebugValue != null) {
            debugFunctionNames = rdebugValue.split(",");
        }
        REntryCounters.FunctionListener.installCounters();
        RNodeTimer.StatementListener.installTimers();
        TraceHandling.traceAllFunctions();
    }

    public static Instrumenter getInstrumenter() {
        return instrumenter;
    }

    public static void checkDebugRequested(RFunction func) {
        if (debugFunctionNames != null) {
            FunctionDefinitionNode fdn = (FunctionDefinitionNode) func.getRootNode();
            for (String debugFunctionName : debugFunctionNames) {
                if (debugFunctionName.equals(fdn.toString())) {
                    DebugHandling.enableDebug(func, "", RNull.instance, false);
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
