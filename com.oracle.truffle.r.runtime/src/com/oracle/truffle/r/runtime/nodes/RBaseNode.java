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
package com.oracle.truffle.r.runtime.nodes;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.env.REnvironment;

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
 * that implement {@link RSyntaxNode} should have a {@link SourceSection} attribute (typically
 * subclasses of {@link RSourceSectionNode}).
 *
 * Is it ever acceptable to subclass {@link Node} directly? The answer is yes, with the following
 * caveats:
 * <ul>
 * <li>The code in the subclass does not invoke methods in the {@link RError} class <b>or</b>takes
 * the responsibility to locate the appropriate {@link RBaseNode} to pass (
 * {@link RError#error(Node, RError.Message)} can be used find the base node hierarchically)</li>
 * <li>An instance of the subclass is never used to {@link #replace} an instance of
 * {@link RBaseNode}.</li>
 * </ul>
 *
 */
public abstract class RBaseNode extends Node {

    /**
     * Since {@link RSyntaxNode}s are sometimes used (for convenience) in non-syntax contexts, this
     * function also checks the {@link RSyntaxNode#isSyntax()} method. This method should always be
     * used in preference to {@code instanceof RSyntaxNode}.
     */
    public boolean isRSyntaxNode() {
        return this instanceof RSyntaxNode && ((RSyntaxNode) this).isSyntax();
    }

    /**
     * Convenience method for working with {@link Node}, e.g. in {@link NodeVisitor}.
     */
    public static boolean isRSyntaxNode(Node node) {
        return node instanceof RSyntaxNode && ((RSyntaxNode) node).isSyntax();
    }

    /**
     * Handles the discovery of the {@link RSyntaxNode} that this node is derived from.
     */
    public RSyntaxNode asRSyntaxNode() {
        if (isRSyntaxNode()) {
            return (RSyntaxNode) this;
        } else {
            return getRSyntaxNode();
        }
    }

    /**
     * See comment on {@link #checkGetRSyntaxNode()}.
     */
    public RSyntaxNode checkasRSyntaxNode() {
        if (isRSyntaxNode()) {
            return (RSyntaxNode) this;
        } else {
            return checkGetRSyntaxNode();
        }
    }

    /**
     * Many nodes organize themselves in such a way that the relevant {@link RSyntaxNode} can be
     * found by following the parent chain, which is therefore the default implementation.
     */
    protected RSyntaxNode getRSyntaxNode() {
        RSyntaxNode result = checkGetRSyntaxNode();
        RInternalError.guarantee(result != null, "getRSyntaxNode");
        return result;
    }

    /**
     * If every {@link RBaseNode} subclass either overrides {@link #getRSyntaxNode()} or works
     * correctly with the default hierarchical implementation, this method would be redundant.
     * However, currently that is not always the case, so this method can be used by defensive code.
     * It returns {@code null} if the {@link RSyntaxNode} cannot be located.
     */
    private RSyntaxNode checkGetRSyntaxNode() {
        Node current = this;
        while (current != null) {
            if (current instanceof RSyntaxNode && ((RSyntaxNode) current).isSyntax()) {
                return (RSyntaxNode) current;
            }
            current = current.getParent();
        }
        return null;
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
        /*
         * All the RSyntaxNode implementors (should) override this method, but it may be called on
         * any Node by the Truffle instrumentation machinery, in which case we return null.
         */
        if (this instanceof RSyntaxNode) {
            throw RInternalError.shouldNotReachHere("getSourceSection in RBaseNode");
        } else {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public void assignSourceSection(SourceSection section) {
        throw RInternalError.shouldNotReachHere("assignSourceSection in RBaseNode");
    }

    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public void clearSourceSection() {
        throw RInternalError.shouldNotReachHere("clearSourceSection in RBaseNode");
    }

    @Override
    /**
     * Returns the {@link SourceSection} for this node, by locating the associated
     * {@link RSyntaxNode}. We do not want any code in FastR calling this method as it is subsumed
     * by {@link #getRSyntaxNode}. However, tools code may call it, so we simply delegate the call.
     */
    public SourceSection getEncapsulatingSourceSection() {
        return getRSyntaxNode().getSourceSection();
    }
}
