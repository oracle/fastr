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
import com.oracle.truffle.r.runtime.*;

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
        if (body.getSourceSection() == null) {
            // Can't instrument AST (bodies) without a SourceSection
            if (FastROptions.debugMatches("RASTProberNoSource")) {
                RDeparse.State state = RDeparse.State.createPrintableState();
                fdn.deparse(state);
                System.out.printf("No source sections for %s, can't instrument%n", fdn);
                System.out.println(state.toString());
            }
            return;
        }
        RInstrument.registerFunctionDefinition(fdn);
        FunctionStatementsNode stmts = body.getStatements();
        FunctionUID uid = fdn.getUID();
        body.probe().tagAs(RSyntaxTag.FUNCTION_BODY, uid);
        stmts.probe().tagAs(START_METHOD, uid);
        TaggingNodeVisitor visitor = new TaggingNodeVisitor(uid);
        if (FastROptions.debugMatches("RASTProberTag")) {
            System.out.printf("Tagging function %s%n", uid);
        }
        RSyntaxNode.accept(body, 0, visitor);
    }

    public abstract static class StatementVisitor implements RSyntaxNodeVisitor {
        protected final FunctionUID uid;

        StatementVisitor(FunctionUID uid) {
            this.uid = uid;
        }

        @Override
        public boolean visit(RSyntaxNode node, int depth) {
            if (node instanceof BlockNode) {
                BlockNode sequenceNode = (BlockNode) node;
                RNode[] block = sequenceNode.getSequence();
                for (int i = 0; i < block.length; i++) {
                    RSyntaxNode n = RSyntaxNode.cast(block[i].unwrap());
                    if (n.getSourceSection() != null) {
                        if (!callback(n)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        protected abstract boolean callback(RSyntaxNode node);

    }

    public static class TaggingNodeVisitor extends StatementVisitor {

        TaggingNodeVisitor(FunctionUID uid) {
            super(uid);
        }

        @Override
        public boolean visit(RSyntaxNode node, int depth) {
            super.visit(node, depth);
            if (node instanceof RCallNode) {
                if (node.getSourceSection() != null) {
                    tagNode(node, CALL);
                }
            }
            return true;
        }

        private void tagNode(RSyntaxNode node, StandardSyntaxTag tag) {
            RInstrument.NodeId nodeId = new RInstrument.NodeId(uid, node);
            node.asRNode().probe().tagAs(tag, new RInstrument.NodeId(uid, node));
            if (FastROptions.debugMatches("RASTProberTag")) {
                System.out.printf("Tagged %s as %s: %s%n", node.getClass().getSimpleName(), tag, nodeId.toString());
            }

        }

        @Override
        protected boolean callback(RSyntaxNode node) {
            tagNode(node, STATEMENT);
            return true;
        }

    }

}
