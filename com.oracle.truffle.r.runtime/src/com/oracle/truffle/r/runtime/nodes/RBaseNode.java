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
package com.oracle.truffle.r.runtime.nodes;

import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * This class should be used as the superclass for all instances of nodes in the FastR AST in
 * preference to {@link Node}. Its basic function is to provide access to the {@link RSyntaxNode}
 * after replace transformations have taken place that replace the {@link RSyntaxNode} with a
 * subclass of this class. The mechanism for locating the original {@link RSyntaxNode} is
 * subclass-specific, specified by the {@link #getRSyntaxNode()} method, the implementation of which
 * defaults to a hierarchical search, since many replacement strategies fit that structure.
 *
 * It also overrides the implementations of {@link #getSourceSection()},
 * {@link #assignSourceSection}, {@link #clearSourceSection()} and
 * {@link #getEncapsulatingSourceSection()} to enforce the FastR invariant that <b>only</b>nodes
 * that implement {@link #getRSyntaxNode()} should have a {@link SourceSection} attribute.
 *
 * Is it ever acceptable to subclass {@link Node} directly? The answer is yes, with the following
 * caveats:
 * <ul>
 * <li>The code in the subclass does not invoke methods in the {@link RError} class <b>or</b>takes
 * the responsibility to locate the appropriate {@link RBaseNode} to pass</li>
 * <li>An instance of the subclass is never used to {@link #replace} an instance of
 * {@link RBaseNode}.</li>
 * </ul>
 */
public abstract class RBaseNode extends Node {
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
        RSyntaxNode node = checkReplacementChild();
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
            if (RContext.getRRuntimeASTAccess().isReplacementNode(this)) {
                return super.getSourceSection();
            }
            RSyntaxNode node = checkReplacementChild();
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
        RSyntaxNode node = checkReplacementChild();
        if (node != null) {
            return node.getSourceSection();
        }
        return super.getEncapsulatingSourceSection();
    }

    /**
     * This is rather nasty, but then that applies to {@code ReplacementNode} in general. Since a
     * auto-generated child of a {@code ReplacementNode} may have a {@link SourceSection}, we might
     * return it using the normal logic, but that would be wrong, we really need to return the the
     * {@link SourceSection} of the {@code ReplacementNode} itself. This is a case where we can't
     * use {@link #getEncapsulatingSourceSection} as a workaround (unless we created a completely
     * parallel set of node classes) because the {@code ReplacementNode} child nodes are just
     * standard {@link RSyntaxNode}s.
     *
     * @return {@code null} if not a child of a {@code ReplacementNode}, otherwise the
     *         {@code ReplacementNode}.
     */
    private RSyntaxNode checkReplacementChild() {
        Node node = this;
        while (node != null) {
            if (RContext.getRRuntimeASTAccess().isReplacementNode(node)) {
                return (RSyntaxNode) node;
            }
            node = node.getParent();
        }
        return null;
    }

}
