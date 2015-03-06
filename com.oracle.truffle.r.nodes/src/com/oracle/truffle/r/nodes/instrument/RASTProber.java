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

import static com.oracle.truffle.api.instrument.StandardSyntaxTag.*;

import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.instrument.ProbeNode.WrapperNode;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.options.FastROptions;

/**
 * A visitor which traverses a completely parsed R AST (presumed not yet executed) and attaches
 * {@linkplain Probe Probes} at some of them for use by the instrumentation framework.
 *
 * Syntax nodes in {@link SequenceNode}s are tagged with {@link StandardSyntaxTag#STATEMENT}.
 * {@link FunctionStatementsNode}s are tagged with {@link StandardSyntaxTag#START_METHOD} which
 * allows the debugger to pause after the arguments have been saved in the frame.
 *
 * N.B. The calls to {@code probe()} insert a {@link WrapperNode} as the parent of the associated
 * node.
 */
public final class RASTProber implements ASTProber {

    private static final RASTProber singleton = new RASTProber();

    private RASTProber() {
    }

    public static RASTProber getRASTProber() {
        return singleton;
    }

    public void probeAST(Node node) {
        FunctionBodyNode body = (FunctionBodyNode) node;
        FunctionDefinitionNode fdn = (FunctionDefinitionNode) body.getParent();
        if (fdn.getSourceSection() == null) {
            // Can't instrument ASTs without a SourceSection
            return;
        }
        RInstrument.registerFunctionDefinition(fdn);
        FunctionStatementsNode stmts = body.getStatements();
        FunctionUID uid = fdn.getUID();
        body.probe().tagAs(RSyntaxTag.FUNCTION_BODY, uid);
        stmts.probe().tagAs(START_METHOD, uid);
        TagSyntaxNodeVisitor visitor = new TagSyntaxNodeVisitor(uid);
        if (FastROptions.debugMatches("astprober")) {
            System.out.printf("Tagging function %s%n", uid);
        }
        body.accept(visitor);
    }

    public abstract static class SyntaxNodeVisitor implements NodeVisitor {
        final FunctionUID uid;

        SyntaxNodeVisitor(FunctionUID uid) {
            this.uid = uid;
        }

        @Override
        public boolean visit(Node node) {
            if (node instanceof SequenceNode) {
                SequenceNode sequenceNode = (SequenceNode) node;
                RNode[] sequence = sequenceNode.getSequence();
                for (int i = 0; i < sequence.length; i++) {
                    RNode n = sequence[i].unwrap();
                    if (n.isSyntax() && n.getSourceSection() != null) {
                        if (!callback(n)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        protected abstract boolean callback(RNode node);

    }

    private static class TagSyntaxNodeVisitor extends SyntaxNodeVisitor {

        TagSyntaxNodeVisitor(FunctionUID uid) {
            super(uid);
        }

        @Override
        protected boolean callback(RNode node) {
            RInstrument.NodeId nodeId = new RInstrument.NodeId(uid, node);
            node.probe().tagAs(STATEMENT, new RInstrument.NodeId(uid, node));
            if (FastROptions.debugMatches("astprober")) {
                System.out.printf("Tagged %s as STATEMENT: %s%n", node.toString(), nodeId.toString());
            }
            return true;
        }

    }

}
