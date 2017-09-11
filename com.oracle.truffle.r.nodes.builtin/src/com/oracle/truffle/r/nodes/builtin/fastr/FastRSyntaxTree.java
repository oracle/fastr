/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.instrumentation.RSyntaxTags;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxFunction;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxVisitor;

/**
 * Prints the "syntax" tree to the standard output connection. Three "modes" are implemented:
 * <ol>
 * <li><b>node</b>: Use the Truffle {@link NodeVisitor}, which will visit every node in the tree.
 * Only nodes that return {@code true} to {@link RSyntaxNode#isSyntax()} are processed. N.B. This
 * will reach nodes that implement {@link RSyntaxNode} but are used in {@link RSyntaxNode#INTERNAL}
 * mode</li>
 * <li><b>syntaxelement</b>: Use the {@link RSyntaxVisitor} to visit the "logical" syntax tree.</li>
 * </ol>
 *
 */
@RBuiltin(name = ".fastr.syntaxtree", visibility = OFF, kind = PRIMITIVE, parameterNames = {"func", "visitMode", "printSource", "printTags"}, behavior = IO)
public abstract class FastRSyntaxTree extends RBuiltinNode.Arg4 {

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, "syntaxelement", RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_FALSE};
    }

    static {
        Casts casts = new Casts(FastRSyntaxTree.class);
        casts.arg("func").mustBe(instanceOf(RFunction.class));
        casts.arg("visitMode").asStringVector().findFirst();
        casts.arg("printSource").asLogicalVector().findFirst().map(toBoolean());
        casts.arg("printTags").asLogicalVector().findFirst().map(toBoolean());
    }

    @Specialization
    @TruffleBoundary
    protected RNull printTree(RFunction function, String visitMode, boolean printSource, boolean printTags) {
        FunctionDefinitionNode root = (FunctionDefinitionNode) function.getTarget().getRootNode();
        switch (visitMode) {
            case "node":
                root.accept(new NodeVisitor() {

                    @Override
                    public boolean visit(Node node) {
                        int depth = nodeDepth(node);
                        printIndent(depth);
                        writeString(node.getClass().getSimpleName(), false);
                        processNode(node, printSource, printTags);
                        printnl();
                        return true;
                    }
                });
                break;

            case "syntaxelement":
                RSyntaxVisitor<Void> visitor = new RSyntaxVisitor<Void>() {
                    private int depth;

                    @Override
                    protected Void visit(RSyntaxCall element) {
                        printIndent(depth);
                        RSyntaxElement lhs = element.getSyntaxLHS();
                        if (lhs instanceof RSyntaxLookup) {
                            writeString(element.getClass().getSimpleName() + " " + ((RSyntaxLookup) lhs).getIdentifier(), false);
                        } else {
                            writeString(element.getClass().getSimpleName(), false);
                        }
                        processSourceSection(element.getSourceSection(), printSource);
                        printnl();
                        RSyntaxElement[] arguments = element.getSyntaxArguments();
                        if (!(lhs instanceof RSyntaxLookup)) {
                            accept(lhs);
                        }
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
                        processSourceSection(element.getSourceSection(), printSource);
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
                        processSourceSection(element.getSourceSection(), printSource);
                        printnl();
                        depth++;
                        accept(element.getSyntaxBody());
                        depth--;
                        return null;
                    }
                };
                visitor.accept(root.getBody());
                break;

            default:
                throw error(RError.Message.INVALID_ARGUMENT, "visitMode");

        }
        return RNull.instance;
    }

    private static int nodeDepth(Node node) {
        int result = 0;
        Node parent = node.getParent();
        while (parent != null) {
            result++;
            parent = parent.getParent();
        }
        return result;
    }

    private static void printIndent(int depth) {
        for (int i = 0; i < depth; i++) {
            writeString("  ", false);
        }
    }

    private static void processRSyntaxNode(RSyntaxNode node, boolean printSource, boolean printTags) {
        SourceSection ss = node.getSourceSection();
        processSourceSection(ss, printSource);
        if (printTags) {
            if (node instanceof RNode) {
                printTags(node.asRNode());
            }
        }
    }

    private static void processNode(Node node, boolean printSource, boolean printTags) {
        if (node instanceof RSyntaxNode) {
            processRSyntaxNode((RSyntaxNode) node, printSource, printTags);
        }
    }

    private static void processSourceSection(SourceSection ss, boolean printSource) {
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
        CharSequence codeText = ss.getCharacters();
        String code;
        if (codeText.length() > 40) {
            code = codeText.subSequence(0, 40) + " ....";
        } else {
            code = codeText.toString();
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
