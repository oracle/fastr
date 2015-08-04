package com.oracle.truffle.r.nodes;

import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.control.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * This is an adapter class, "Syntax Adapter" that is used to enforce some invariants regarding the
 * use of {@link SourceSection} attributes and provide a mechanism to locate the {@link RSyntaxNode}
 * for an {@link RSyntaxNodeAdapter}.
 *
 * This class also defines the generic support methods for {@code deparse}, {@code substitute} and
 * {@code serialize}. The implementations use {@link #getRSyntaxNode()} to locate the correct
 * {@link RSyntaxNode} and then invoke the corresponding {@code xxxImpl} method. The intention being
 * that the AST may be rewritten in arbitrary ways but it is always possible to locate the
 * {@link RSyntaxNode} that a rewritten node derives from.
 */
public abstract class RSyntaxNodeAdapter extends Node {
    /**
     * Handles the discovery of the {@link RSyntaxNode} that this node is derived from.
     */
    public RSyntaxNode asRSyntaxNode() {
        if (this instanceof RSyntaxNode) {
            return (RSyntaxNode) this;
        } else {
            return getRSyntaxNode();
        }
    }

    /**
     * Many nodes organize themselves in such a way that the relevant {@link RSyntaxNode} can be
     * found by following the parent chain, which is therefore the default implementation.
     */
    protected RSyntaxNode getRSyntaxNode() {
        ReplacementNode node = checkReplacementChild();
        if (node != null) {
            return node;
        }
        Node current = this;
        while (current != null) {
            if (current instanceof RSyntaxNode) {
                return (RSyntaxNode) current;
            }
            current = current.getParent();
        }
        throw RInternalError.shouldNotReachHere("getRSyntaxNode");
    }

    public void deparse(State state) {
        RSyntaxNode syntaxNode = getRSyntaxNode();
        syntaxNode.deparseImpl(state);

    }

    public RSyntaxNode substitute(REnvironment env) {
        RSyntaxNode syntaxNode = getRSyntaxNode();
        return syntaxNode.substituteImpl(env);
    }

    public void serialize(RSerialize.State state) {
        RSyntaxNode syntaxNode = getRSyntaxNode();
        syntaxNode.serializeImpl(state);
    }

    @Override
    public SourceSection getSourceSection() {
        if (this instanceof RSyntaxNode) {
            if (this instanceof ReplacementNode) {
                return super.getSourceSection();
            }
            ReplacementNode node = checkReplacementChild();
            if (node != null) {
                return node.getSourceSection();
            }
            return super.getSourceSection();
        } else {
            throw RInternalError.shouldNotReachHere("getSourceSection on non-syntax node");
        }
    }

    @Override
    public void assignSourceSection(SourceSection section) {
        if (this instanceof RSyntaxNode) {
            super.assignSourceSection(section);
        } else {
            throw RInternalError.shouldNotReachHere("assignSourceSection on non-syntax node");
        }
    }

    @Override
    public void clearSourceSection() {
        if (this instanceof RSyntaxNode) {
            super.clearSourceSection();
        } else {
            /*
             * Eventually this should be an error but currently "substitute" walks the entire tree
             * calling this method.
             */
            super.clearSourceSection();
        }
    }

    @Override
    /**
     * Returns the {@link SourceSection} for this node, by locating the associated {@link RSyntaxNode}.
     * I.e., this method must be stable in the face of AST transformations.
     *
     * N.B. This default implementation may be incorrect unless this node is always a child of the
     * original {@link RSyntaxNode}. In cases where the structure is more complex, e.g. an inline
     * cache, the node should override {@link #getEncapsulatingSourceSection()} with a node-specific
     * implementation. There are basically three approaches:
     * <ol>
     * <li>Store the {@link SourceSection} as field in the node and override this method to return it. This may seem odd since currently
     * every {@link Node} can store a {@link SourceSection}, but that could change, plus it is necessary owing to the check in {@link #assignSourceSection}</li>
     * <li>Store the original {@link RSyntaxNode} and override this method to call its {@link getSourceSection}</li>
     * <li>Follow the node-specific data structure to locate the original {@link RSyntaxNode} and call its {@link getSourceSection}</li>
     * </ol>
     */
    public SourceSection getEncapsulatingSourceSection() {
        ReplacementNode node = checkReplacementChild();
        if (node != null) {
            return node.getSourceSection();
        }
        return super.getEncapsulatingSourceSection();
    }

    /**
     * This is rather nasty, but then that applies to {@link ReplacementNode} in general. Since a
     * auto-generated child of a {@link ReplacementNode} may have a {@link SourceSection}, we might
     * return it using the normal logic, but that would be wrong, we really need to return the the
     * {@link SourceSection} of the {@link ReplacementNode} itself. This is a case where we can't
     * use {@link #getEncapsulatingSourceSection} as a workaround (unless we created a completely
     * parallel set of node classes) because the {@code ReplacementNode} child nodes are just
     * standard {@link RSyntaxNode}s.
     *
     * @return {@code null} if not a child of a {@link ReplacementNode}, otherwise the
     *         {@link ReplacementNode}.
     */
    private ReplacementNode checkReplacementChild() {
        Node node = this;
        while (node != null) {
            if (node instanceof ReplacementNode) {
                return (ReplacementNode) node;
            }
            node = node.getParent();
        }
        return null;
    }

}
