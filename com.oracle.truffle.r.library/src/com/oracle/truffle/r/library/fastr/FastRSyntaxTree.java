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
package com.oracle.truffle.r.library.fastr;

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.instrumentation.RSyntaxTags;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxFunction;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNodeVisitor;
import com.oracle.truffle.r.runtime.nodes.RSyntaxVisitor;

/**
 * Prints the "syntax" tree to the standard output connection. Three "modes" are implemented:
 * <ol>
 * <li><b>node</b>: Use the Truffle {@link NodeVisitor}, which will visit every node in the tree.
 * Only nodes that return {@code true} to {@link RSyntaxNode#isSyntax()} are processed. N.B. This
 * will reach nodes that implement {@link RSyntaxNode} but are used in {@link RSyntaxNode#INTERNAL}
 * mode</li>
 * <li><b>syntaxnode</b>: Use the {@link RSyntaxNodeVisitor}. The main difference from mode
 * {@code node} is that the children of non-syntax nodes are not visited at all.</li>
 * <li><b<syntaxelement</b>: Use the {@link RSyntaxVisitor} to visit the "logical" syntax tree.</li>
 * </ol>
 *
 */
public abstract class FastRSyntaxTree extends RExternalBuiltinNode.Arg4 {
    @Specialization
    @TruffleBoundary
    protected RNull printTree(RFunction function, byte printSourceLogical, RAbstractStringVector visitMode, byte printTagsLogical) {
        boolean printSource = RRuntime.fromLogical(printSourceLogical);
        boolean printTags = RRuntime.fromLogical(printTagsLogical);
        FunctionDefinitionNode root = (FunctionDefinitionNode) function.getTarget().getRootNode();
        switch (visitMode.getDataAt(0)) {
            case "node":
                root.accept(new NodeVisitor() {

                    @Override
                    public boolean visit(Node node) {
                        if (RBaseNode.isRSyntaxNode(node)) {
                            writeString(node.getClass().getSimpleName(), false);
                            SourceSection ss = node.getSourceSection();
                            processSourceSection(ss, printSource, printTags);
                            printnl();
                        }
                        return true;
                    }

                });
                break;

            case "syntaxnode":
                RSyntaxNode.accept(root, 0, new RSyntaxNodeVisitor() {

                    @Override
                    public boolean visit(RSyntaxNode node, int depth) {
                        printIndent(depth);
                        writeString(node.getClass().getSimpleName(), false);
                        processRSyntaxNode(node, printSource, printTags);
                        printnl();
                        return true;
                    }
                }, true);
                break;

            case "syntaxelement":
                RSyntaxVisitor<Void> visitor = new RSyntaxVisitor<Void>() {
                    private int depth;

                    @Override
                    protected Void visit(RSyntaxCall element) {
                        printIndent(depth);
                        writeString(element.getClass().getSimpleName(), false);
                        processSourceSection(element.getSourceSection(), printSource, printTags);
                        printnl();
                        RSyntaxElement lhs = element.getSyntaxLHS();
                        RSyntaxElement[] arguments = element.getSyntaxArguments();
                        accept(lhs);
                        for (RSyntaxElement arg : arguments) {
                            depth++;
                            accept(arg);
                            depth--;
                        }
                        return null;
                    }

                    @Override
                    protected Void visit(RSyntaxConstant element) {
                        printIndent(depth);
                        writeString(element.getClass().getSimpleName(), false);
                        processSourceSection(element.getSourceSection(), printSource, printTags);
                        printnl();
                        return null;
                    }

                    @Override
                    protected Void visit(RSyntaxLookup element) {
                        printIndent(depth);
                        writeString("RSyntaxLookup # ", false);
                        writeString(element.getIdentifier(), true);
                        return null;
                    }

                    @Override
                    protected Void visit(RSyntaxFunction element) {
                        printIndent(depth);
                        writeString(element.getClass().getSimpleName(), false);
                        processSourceSection(element.getSourceSection(), printSource, printTags);
                        printnl();
                        depth++;
                        accept(element.getSyntaxBody());
                        depth--;
                        return null;
                    }

                };
                visitor.accept(root.getBody());
                break;

        }
        return RNull.instance;
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object fallback(Object a1, Object a2, Object a3, Object a4) {
        throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
    }

    private static void printIndent(int depth) {
        for (int i = 0; i < depth; i++) {
            writeString("  ", false);
        }
    }

    private static void processRSyntaxNode(RSyntaxNode node, boolean printSource, boolean printTags) {
        SourceSection ss = node.getSourceSection();
        if (ss == null) {
            writeString(" *** null source section", false);
        } else {
            if (printSource) {
                printSourceCode(ss);
            }
            if (printTags) {
                if (node instanceof RNode) {
                    printTags(node.asRNode());
                }
            }
        }
    }

    private static void processSourceSection(SourceSection ss, boolean printSource, @SuppressWarnings("unused") boolean printTags) {
        // All syntax nodes should have source sections
        if (ss == null) {
            writeString(" *** null source section", false);
        } else {
            if (printSource) {
                printSourceCode(ss);
            }
        }
    }

    private static void printSourceCode(SourceSection ss) {
        String code = ss.getCode();
        if (code.length() > 40) {
            code = code.substring(0, 40) + " ....";
        }
        code = code.replace("\n", "\\n ");
        writeString(" # ", false);
        writeString(code.length() == 0 ? "<EMPTY>" : code, false);
    }

    private static void printTags(RNode node) {
        writeString(" # tags [ ", false);
        for (int i = 0; i < RSyntaxTags.ALL_TAGS.length; i++) {
            Class<?> tag = RSyntaxTags.ALL_TAGS[i];
            if (node.isTaggedWith(tag)) {
                writeString(tag.getSimpleName(), false);
                writeString(" ", false);
            }
        }
        writeString("]", false);
    }

    private static void printnl() {
        writeString("", true);
    }

    private static void writeString(String msg, boolean nl) {
        try {
            StdConnections.getStdout().writeString(msg, nl);
        } catch (IOException ex) {
            throw RError.error(RError.NO_CALLER, RError.Message.GENERIC, ex.getMessage() == null ? ex : ex.getMessage());
        }
    }
}
