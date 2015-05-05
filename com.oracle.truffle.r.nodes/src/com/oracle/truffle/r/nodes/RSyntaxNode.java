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
package com.oracle.truffle.r.nodes;

import com.oracle.truffle.api.instrument.ProbeNode.WrapperNode;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * An interface that identifies an AST node as being part of the syntactic structure of the
 * (original) AST. Essentially a syntax node is defined as one produced by the parser and before any
 * evaluation takes place. I.e. a visit to every node in such a tree should have
 * {@code node instanceof RSyntaxNode == true}. A syntax node (necessarily) includes <i>backbone</i>
 * nodes that, while carrying no actual syntactic information, are used to connect one syntax node
 * to another. It is possible to detect backbone nodes using the {@link #isBackbone} method.
 *
 * Currently FastR deviates from this ideal in that the parsing process or, more accurately, the
 * conversion of the AST produced by the parser into a Truffle AST effects some transformations that
 * introduce nodes that do not correspond to the original syntax. The most notable such
 * transformation is seen in the {@code ReplacementNode} that implements a lowering of the so-called
 * <i>replacement</i> logic into a sequence of lower-level nodes that nonetheless implement this
 * interface.
 */
public interface RSyntaxNode {
    /**
     * A convenience method that captures the fact that, while the notion of a syntax node is
     * described in this interface, in practice all {@link RSyntaxNode} instances are also
     * {@link RNode}s. At runtime this method should be inlined away.
     */
    default RNode asRNode() {
        return (RNode) this;
    }

    /**
     * Denotes that this node is part of the "backbone" of the AST, but carries no useful syntactic
     * information. A classic case is a {@link WrapperNode} inserted for instrumentation purposes.
     */
    default boolean isBackbone() {
        return false;
    }

    /**
     * A placeholder. Eventually this method will appear on a Truffle super-interface and will
     * disappear from the {@link Node} class.
     */
    default SourceSection getSourceSection() {
        return (asRNode().getSourceSection());
    }

    /**
     * Support for the {@code deparse} builtin function.
     */
    default void deparse(@SuppressWarnings("unused") State state) {
        throw RInternalError.unimplemented("deparse not implemented in " + getClass());
    }

    /**
     * Support for the {@code substitute} builtin function. Assert: {this.isSyntax() == true}. N.B.
     * this method only needs to operate on pristine (uninitialized) ASTs. The AST is cloned prior
     * to the substitution; therefore there is no need to create a new node if it can be determined
     * that no changes were made.
     */
    default RSyntaxNode substitute(@SuppressWarnings("unused") REnvironment env) {
        throw RInternalError.unimplemented("substitute not implemented in " + getClass());
    }

    /**
     * Support for serializing closures.
     */
    default void serialize(@SuppressWarnings("unused") RSerialize.State state) {
        throw RInternalError.unimplemented("serialize not implemented in " + getClass());
    }

    static RSyntaxNode cast(RNode node) {
        return (RSyntaxNode) node;
    }

    /**
     * Traverses the entire tree but only invokes the {@code visit} method for nodes that return
     * {@code true} to {instance RSyntaxNode}. Similar therefore to {@code Node#accept}. The
     * assumption is that the syntax is properly connected so that there is no need to visit the
     * children of non-syntax nodes. We visit but do not call {@code nodeVisitor} on nodes that
     * return {@code true} to {@link #isBackbone()}.
     *
     * N.B. A {@link ReplacementNode} is a very special case. Its children are {@link RSyntaxNode}s,
     * but we do not want to visit them at all. Hopefully this node will be retired eventually.
     */
    static void accept(Node node, int depth, RSyntaxNodeVisitor nodeVisitor) {
        boolean visitChildren = true;
        if (node instanceof RSyntaxNode) {
            RSyntaxNode syntaxNode = (RSyntaxNode) node;
            if (!syntaxNode.isBackbone()) {
                visitChildren = nodeVisitor.visit(syntaxNode, depth);
            }
            if (!(node instanceof ReplacementNode)) {
                if (visitChildren) {
                    for (Node child : node.getChildren()) {
                        if (child != null && child instanceof RSyntaxNode) {
                            accept(child, depth + 1, nodeVisitor);
                        }
                    }
                }
            }
        }
    }
}
