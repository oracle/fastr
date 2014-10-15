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
    public static void accept(Node node, int depth, RSyntaxNodeVisitor nodeVisitor) {
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
