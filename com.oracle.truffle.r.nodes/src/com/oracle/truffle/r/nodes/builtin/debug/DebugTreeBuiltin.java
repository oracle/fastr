/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.debug;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil.NodeClass;
import com.oracle.truffle.api.nodes.NodeUtil.NodeField;
import com.oracle.truffle.api.nodes.NodeUtil.NodeFieldKind;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "debug.tree", kind = PRIMITIVE)
@RBuiltinComment("Prints the Truffle tree of a function. Use debug.tree(a, TRUE) for more detailed output.")
public abstract class DebugTreeBuiltin extends RBuiltinNode {

    private static final Object[] PARAMETER_NAMES = new Object[]{"function", "verbose", "graphically"};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_FALSE)};
    }

    @Specialization
    public Object printTree(RFunction function, byte verbose, byte graphically) {
        controlVisibility();
        RootNode root = function.getTarget().getRootNode();
        if (graphically == RRuntime.LOGICAL_FALSE) {
            if (verbose == RRuntime.LOGICAL_TRUE) {
                return NodeUtil.printTreeToString(root);
            } else {
                return NodeUtil.printCompactTreeToString(root);
            }
        } else {
            return writeDotTreeToFile(root, verbose == RRuntime.LOGICAL_TRUE ? true : false);
        }
    }

    @Specialization
    public RNull printTree(Object function, @SuppressWarnings("unused") Object verbose, @SuppressWarnings("unused") Object graphically) {
        controlVisibility();
        CompilerDirectives.transferToInterpreter();
        throw RError.getNYI("Not a function value: " + function.toString());
    }

    /*
     * Output of dot-files representing the tree
     */

    private static final String DOT_TREE_FILE_NAME = "tree.dot";

    public String writeDotTreeToFile(Node root, boolean verbose) {
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
     * Heavily based on {@link NodeUtil#printTree(OutputStream, Node)}
     */
    private static class DotTreePrinter {

        private static final int INVALID_ID = -1;
        private static final String ROW_START = "<TR><TD>";
        private static final String ROW_END = "</TD></TR>";

        private final PrintWriter pw;
        private final boolean verbose;

        private int lastID = 0;

        public DotTreePrinter(OutputStream os, boolean verbose) {
            this.pw = new PrintWriter(os);
            this.verbose = verbose;
        }

        public void printTreeGraph(Node node) {
            pw.println("digraph debug_tree {");
            pw.println("rankdir = TB");
            pw.println("node [shape=plaintext]");

            printNode(node);

            pw.println("}");
        }

        protected int printNode(Node node) {
            if (node == null) {
                return INVALID_ID;
            }

            int nodeId = newId();

            // Start printing the current node
            pw.print(id(nodeId));
            pw.println(" [");

            // The structure of record shapes are defined by its labels
            StringBuilder label = new StringBuilder("label = <<TABLE>");
            label.append(ROW_START).append("<B>").append(nodeName(node)).append("</B>").append(ROW_END);

            NodeField[] fields = NodeClass.get(node.getClass()).getFields();
            if (verbose) {
                // Simple data fields are added as name/value pairs
                Arrays.stream(fields).filter(f -> f.getKind() == NodeFieldKind.DATA).forEachOrdered(field -> {
                    Object value = field.loadValue(node);
                    label.append(ROW_START);

                    /* ", &, <, > have to be replaced by their corresponding HTML escape sequence */
                    String valStr = String.valueOf(value);
                    valStr = valStr.replace("\"", "&quot;");
                    valStr = valStr.replace("&", "&amp;");
                    valStr = valStr.replace("<", "&lt;");
                    valStr = valStr.replace(">", "&gt;");

                    label.append(field.getName()).append(" = ").append(valStr);
                    label.append(ROW_END);
                });
            }

            // Child and children, however, get their name and port inside the record.
            Predicate<NodeField> childOrChildren = new Predicate<NodeField>() {
                public boolean test(NodeField field) {
                    return field.getKind() == NodeFieldKind.CHILD || field.getKind() == NodeFieldKind.CHILDREN;
                }
            };
            Stream<NodeField> childrenFields = Arrays.stream(fields).filter(childOrChildren);
            childrenFields.forEachOrdered(new Consumer<NodeField>() {
                private int localChildId = 0;

                public void accept(NodeField field) {
                    Object value = field.loadValue(node);
                    // If this child is not set: Don't bother displaying it
                    if (!verbose && value == null) {
                        return;
                    }

                    // Start a new row and cell with a port name
                    label.append("<TR><TD PORT=\"").append(childPort(localChildId++)).append("\">");

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
            });

            // [Finish current node]
            label.append("</TABLE>>");
            pw.println(label.toString());
            pw.println("];");

            // Finally, print the actual children and their edges
            childrenFields = Arrays.stream(fields).filter(childOrChildren);
            printNodeChildren(node, nodeId, childrenFields);

            return nodeId;
        }

        protected void printNodeChildren(Node node, int nodeId, Stream<NodeField> childrenFields) {
            childrenFields.forEachOrdered(new Consumer<NodeField>() {
                private int localChildId = 0;

                public void accept(NodeField field) {
                    Object value = field.loadValue(node);
                    if (field.getKind() == NodeFieldKind.CHILD) {
                        int childNodeId = printNode((Node) value);
                        createEdge(node, nodeId, localChildId, childNodeId);
                    } else if (field.getKind() == NodeFieldKind.CHILDREN) {
                        for (Node child : (Node[]) value) {
                            int childNodeId = printNode(child);
                            createEdge(node, nodeId, localChildId, childNodeId);
                        }
                    }
                    localChildId++;
                }
            });
        }

        private void createEdge(@SuppressWarnings("unused") Node node, int nodeId, int localChildId, int childNodeId) {
            if (nodeId == INVALID_ID || childNodeId == INVALID_ID) {
                return;
            }
            pw.append(id(nodeId)).append(":").append(childPort(localChildId));
            pw.append(" -> ");
            pw.append(id(childNodeId));
            pw.append(";\n");
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

        public void finish() {
            this.pw.println();
            this.pw.flush();
            this.pw.close();
        }

        protected static String nodeName(Node node) {
            return node.getClass().getSimpleName();
        }
    }
}
