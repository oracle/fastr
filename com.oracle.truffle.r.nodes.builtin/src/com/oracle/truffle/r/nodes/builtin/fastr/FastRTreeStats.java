/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@RBuiltin(name = ".fastr.treestats", kind = RBuiltinKind.PRIMITIVE, parameterNames = {"obj"})
public abstract class FastRTreeStats extends RBuiltinNode {

    private static final RStringVector COLNAMES = RDataFactory.createStringVector(new String[]{"Total", "Syntax", "Non-Syntax"}, RDataFactory.COMPLETE_VECTOR);

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance};
    }

    @Specialization
    @TruffleBoundary
    protected RList treeStats(RFunction function) {
        SyntaxNodeCount snc = doWalk(function, null);
        ArrayList<SyntaxNodeCount> sncList = new ArrayList<>(1);
        sncList.add(snc);
        return createResult(sncList);
    }

    @Specialization
    @TruffleBoundary
    protected RList treeStats(REnvironment env) {
        RStringVector bindings = env.ls(true, null, true);
        ArrayList<SyntaxNodeCount> sncList = new ArrayList<>();
        for (int i = 0; i < bindings.getLength(); i++) {
            String binding = bindings.getDataAt(i);
            Object value = env.get(binding);
            if (value instanceof RPromise) {
                value = PromiseHelperNode.evaluateSlowPath(null, (RPromise) value);

            }
            if (value instanceof RFunction) {
                RFunction func = (RFunction) value;
                if (!func.isBuiltin()) {
                    sncList.add(doWalk((RFunction) value, binding));
                }
            }
        }
        return createResult(sncList);
    }

    @Specialization
    @TruffleBoundary
    protected RList treeStats(@SuppressWarnings("unused") RNull function) {
        String[] searchPath = REnvironment.searchPath();
        Object[] listData = new Object[searchPath.length];
        for (int i = 0; i < searchPath.length; i++) {
            String pkg = searchPath[i];
            listData[i] = treeStats(REnvironment.lookupOnSearchPath(pkg));
        }
        return RDataFactory.createList(listData, RDataFactory.createStringVector(searchPath, RDataFactory.COMPLETE_VECTOR));
    }

    @TruffleBoundary
    private static SyntaxNodeCount doWalk(RFunction function, String knownName) {
        FunctionDefinitionNode root = (FunctionDefinitionNode) function.getTarget().getRootNode();
        SyntaxNodeCount syntaxNodeCount = new SyntaxNodeCount(root, knownName);
        root.accept(syntaxNodeCount);
        return syntaxNodeCount;
    }

    private static RList createResult(ArrayList<SyntaxNodeCount> functionCounts) {
        Object[] listData = new Object[functionCounts.size()];
        String[] names = new String[functionCounts.size()];
        for (int i = 0; i < listData.length; i++) {
            SyntaxNodeCount snc = functionCounts.get(i);
            listData[i] = RDataFactory.createIntVector(new int[]{snc.total(), snc.syntaxNodeCount, snc.nonSyntaxNodeCount}, RDataFactory.COMPLETE_VECTOR, COLNAMES);
            names[i] = snc.name();
        }
        return RDataFactory.createList(listData, RDataFactory.createStringVector(names, RDataFactory.COMPLETE_VECTOR));
    }

    private static final class SyntaxNodeCount implements NodeVisitor {
        private final FunctionDefinitionNode fdn;
        private final String knownName;
        private int nonSyntaxNodeCount;
        private int syntaxNodeCount;

        SyntaxNodeCount(FunctionDefinitionNode fdn, String knownName) {
            this.fdn = fdn;
            this.knownName = knownName;
        }

        @Override
        public boolean visit(Node node) {
            if (RBaseNode.isRSyntaxNode(node)) {
                syntaxNodeCount++;
            } else {
                nonSyntaxNodeCount++;
            }
            return true;
        }

        private int total() {
            return syntaxNodeCount + nonSyntaxNodeCount;
        }

        private String name() {
            if (knownName != null) {
                return knownName;
            } else {
                return fdn.toString();
            }
        }
    }
}
