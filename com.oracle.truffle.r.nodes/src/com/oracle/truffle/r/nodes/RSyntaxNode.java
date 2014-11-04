/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes;

import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.*;

/**
 * An interface that identifies an AST node as being part of the syntactic structure of the
 * (original) AST. This, perhaps unfortunately, is not a static property of the node class. For
 * example, some {@link WriteVariableNode} instances are created that do not correspond to syntax.
 */
public interface RSyntaxNode {
    /**
     * Support for the {@code deparse} builtin function. N.B. {@link #isSyntax()} does not have to
     * return {@code true} for a node to override this method. Whether to override is an
     * implementation convenience.
     */
    default void deparse(@SuppressWarnings("unused") State state) {
        throw RInternalError.unimplemented();
    }

    /**
     * Support for the {@code substitute} builtin function. Assert: {this.isSyntax() == true}.
     */
    default RNode substitute(@SuppressWarnings("unused") REnvironment env) {
        throw RInternalError.unimplemented();
    }

    /**
     * Returns {@code true} if and only if this node is part of the syntactic view of the AST.
     * Perhaps surprisingly the majority of nodes are not syntactic, hence the {@code false}
     * default.
     */
    default boolean isSyntax() {
        return false;
    }

    /**
     * Traverses the entire tree but only invokes the {@code visit} method for nodes that return
     * {@code true} to {@link RSyntaxNode#isSyntax()}.
     */
    static void accept(Node node, int depth, RSyntaxNodeVisitor nodeVisitor) {
        boolean visitChildren = true;
        if (node instanceof RSyntaxNode) {
            RSyntaxNode syntaxNode = (RSyntaxNode) node;
            if (syntaxNode.isSyntax()) {
                visitChildren = nodeVisitor.visit(syntaxNode, depth);
            }
        }
        if (visitChildren) {
            for (Node child : node.getChildren()) {
                if (child != null) {
                    accept(child, depth + 1, nodeVisitor);
                }
            }
        }
    }
}
