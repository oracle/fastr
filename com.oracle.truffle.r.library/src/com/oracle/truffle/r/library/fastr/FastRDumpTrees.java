/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeClass;
import com.oracle.truffle.api.nodes.NodeFieldAccessor;
import com.oracle.truffle.api.nodes.NodeFieldAccessor.NodeFieldKind;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;

/**
 * Dump Truffle trees to a listening IGV instance, if any. If igvDump == FALSE, dumps tree to
 * .dot-file in the cwd
 */
public abstract class FastRDumpTrees extends RExternalBuiltinNode.Arg3 {

    private static final int FUNCTION_LENGTH_LIMIT = 40;
    private static final String DOT_TREE_FILE_NAME = "tree.dot";

    @Specialization
    protected RNull dump(RFunction function, byte igvDump, byte verbose) {

        RootNode root = function.getTarget().getRootNode();
        if (igvDump == RRuntime.LOGICAL_FALSE) {
            // Use .dot dump instead
            writeDotTreeToFile(root, verbose == RRuntime.LOGICAL_TRUE);
            // User gets no feedback about the output file; should 'debug.deump' be visible?
            return RNull.instance;
        }

        String source = ((RRootNode) root).getSourceCode();
        Utils.dumpFunction("dump: " + (source.length() <= FUNCTION_LENGTH_LIMIT ? source : source.substring(0, FUNCTION_LENGTH_LIMIT) + "..."), function);
        return RNull.instance;
    }

    @SuppressWarnings("unused")
    @Fallback
    protected Object fallback(Object a1, Object a2, Object a3) {
        throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
    }

    /*
     * Output of dot-files representing the tree
     */
    public static String writeDotTreeToFile(Node root, boolean verbose) {
        File dotFile = new File(DOT_TREE_FILE_NAME);
        try {
            // Open output file
            if (!dotFile.exists()) {
                dotFile.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(dotFile);

            // Let visitor handle the tree
            DotTreePrinter printer = new DotTreePrinter(fos, verbose);
            printer.printTreeGraph(root);
            printer.finish();
        } catch (IOException err) {
            return err.getMessage();
        }

        return "Tree successfully written to '" + dotFile.getAbsolutePath() + "'.\nVisualize using '$ dot -Tpng tree.dot', for example.";
    }

    /**
     * Written based on {@link NodeUtil#printTree(OutputStream, Node)}.
     */
    private static class DotTreePrinter {

        private static final int INVALID_ID = -1;
        private static final String ROW_START = "<TR><TD>";
        private static final String ROW_END = "</TD></TR>";
        private static final String FIELD_SOURCE_SECTION = "sourceSection";

        private final PrintWriter pw;
        private final boolean verbose;

        private int lastID = 0;

        DotTreePrinter(OutputStream os, boolean verbose) {
            this.pw = new PrintWriter(os);
            this.verbose = verbose;
        }

        @TruffleBoundary
        public void printTreeGraph(Node node) {
            pw.println("digraph debug_tree {");
            pw.println("rankdir = TB");
            pw.println("node [shape=plaintext]");

            printNode(node);

            pw.println("}");
        }

        /**
         * Prints nodes as dot nodes with HTML-like formatting based on table.
         *
         * @param node
         * @return The dot node id
         */
        protected int printNode(Node node) {
            if (node == null) {
                return INVALID_ID;
            }

            int nodeId = newId();

            // Start printing the current node
            pw.print(id(nodeId));
            pw.println(" [");

            // The structure of dots plain text shapes is defined by their labels
            // Nodes are drawn as tables, one NodeField per row
            StringBuilder label = new StringBuilder("label = <<TABLE>");
            label.append(ROW_START).append("<B>").append(nodeName(node)).append("</B>").append(ROW_END);

            NodeFieldAccessor[] fields = NodeClass.get(node.getClass()).getFields();

            // DATA fields are omitted when not told to be verbose
            if (verbose) {
                // Simple DATA fields are added as name/value pairs
                for (NodeFieldAccessor field : fields) {
                    if (field.getKind() != NodeFieldKind.DATA) {
                        continue;
                    }

                    label.append(ROW_START);
                    // Name..
                    label.append(field.getName());
                    label.append(" = ");

                    // ..and value
                    Object value = field.loadValue(node);
                    String valStr = escapeHTMLCharacters(String.valueOf(value));
                    label.append(valStr);
                    label.append(ROW_END);
                }
            } else {
                // If not being verbose, print at least "sourceSection"s content
                for (NodeFieldAccessor field : fields) {
                    if (FIELD_SOURCE_SECTION.equals(field.getName())) {
                        Object value = field.loadValue(node);
                        if (value == null) {
                            // If null just skip
                            break;
                        }

                        label.append(ROW_START);

                        String valStr = escapeHTMLCharacters(String.valueOf(value));
                        label.append(valStr);

                        label.append(ROW_END);
                        break;
                    }
                }
            }

            // Child and children, however, get their name and port inside the record.
            int localPortId = 0;
            for (NodeFieldAccessor field : fields) {
                if (field.getKind() != NodeFieldKind.CHILD && field.getKind() != NodeFieldKind.CHILDREN) {
                    continue;
                }

                // If this child's value is not set: Don't bother displaying it
                Object value = field.loadValue(node);
                if (!verbose && value == null) {
                    continue;
                }

                // Start a new row and cell with a port name
                label.append("<TR><TD PORT=\"").append(childPort(localPortId++)).append("\">");

                label.append(field.getName());
                if (field.getKind() == NodeFieldKind.CHILDREN) {
                    label.append("*");
                }

                // Child/children may be null, too
                if (value == null) {
                    label.append(" = null");
                }

                label.append(ROW_END);
            }

            // [Finish current node]
            label.append("</TABLE>>");
            pw.println(label.toString());
            pw.println("];");

            // Finally, print the actual children and their edges
            printNodeChildren(node, nodeId);

            return nodeId;
        }

        /**
         * Replaces the characters ", &, <, > by their corresponding HTML escape sequence.
         *
         * @param unescapedStr
         * @return String with characters escaped
         */
        protected static String escapeHTMLCharacters(String unescapedStr) {
            String escapedStr = unescapedStr.replace("\"", "&quot;");
            escapedStr = escapedStr.replace("&", "&amp;");
            escapedStr = escapedStr.replace("<", "&lt;");
            escapedStr = escapedStr.replace(">", "&gt;");
            return escapedStr;
        }

        protected void printNodeChildren(Node node, int nodeId) {
            NodeFieldAccessor[] fields = NodeClass.get(node.getClass()).getFields();
            int localPortId = 0;
            for (NodeFieldAccessor field : fields) {
                if (field.getKind() != NodeFieldKind.CHILD && field.getKind() != NodeFieldKind.CHILDREN) {
                    continue;
                }

                Object value = field.loadValue(node);
                if (field.getKind() == NodeFieldKind.CHILD) {
                    int childNodeId = printNode((Node) value);
                    createEdge(node, nodeId, localPortId, childNodeId);
                } else if (field.getKind() == NodeFieldKind.CHILDREN) {
                    for (Node child : (Node[]) value) {
                        int childNodeId = printNode(child);
                        createEdge(node, nodeId, localPortId, childNodeId);
                    }
                }
                localPortId++;
            }
        }

        protected void createEdge(@SuppressWarnings("unused") Node node, int nodeId, int localChildId, int childNodeId) {
            if (nodeId == INVALID_ID || childNodeId == INVALID_ID) {
                // In case one of them is null: No edge needed
                return;
            }
            pw.append(id(nodeId)).append(":").append(childPort(localChildId));
            pw.append(" -> ");
            pw.append(id(childNodeId));
            pw.append(";\n");
        }

        protected static String nodeName(Node node) {
            return node.getClass().getSimpleName();
        }

        private static String childPort(int localPortId) {
            return "c" + localPortId;
        }

        private static String id(int id) {
            return "id" + id;
        }

        private int newId() {
            return ++lastID;
        }

        /**
         * Shuts down the {@link PrintWriter}.
         */
        public void finish() {
            this.pw.println();
            this.pw.flush();
            this.pw.close();
        }
    }
}
