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
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RSource;

/**
 * An interface that identifies an AST node as being part of the syntactic structure of the
 * (original) AST. Essentially a syntax node is defined as one produced by the parser and before any
 * evaluation takes place. I.e. a visit to every node in such a tree should have
 * {@code node instanceof RSyntaxNode == true}.
 *
 * Currently FastR deviates from this ideal in that the parsing process or, more accurately, the
 * conversion of the AST produced by the parser into a Truffle AST effects some transformations that
 * introduce nodes that do not correspond to the original syntax. The most notable such
 * transformation is seen in the {@code ReplacementNode} that implements a lowering of the so-called
 * <i>replacement</i> logic into a sequence of lower-level nodes that nonetheless implement this
 * interface.
 *
 * A more subtle issue is the general use of syntax nodes in rewrites of the AST where the nodes do
 * not correspond to nodes in the original source. Examples are anonymous variables created as a
 * {code ReadVariableNode}. Evidently such nodes, and the larger structures that contain them,
 * cannot be given meaningful {@link SourceSection} information. Ideally such nodes would be
 * refactored in such a way that such nodes did not implement this interface. However, as a
 * workaround, the {@link #isSyntax} method can be overridden in such nodes, using some contextual
 * information, to return {@code false}.
 *
 * It is an invariant that every RSyntaxNode has a non-null {@link SourceSection}, but to handle the
 * cases alluded to above, several {@link SourceSection} values are defined in this interface, e.g.
 * {@link #SOURCE_UNAVAILABLE}, that can be used instead of {@code null} and identify the situation.
 * One particular case is {@link #LAZY_DEPARSE} which indicates that a valid {@link SourceSection}
 * can be produced for the associated node, but it is computed lazily, when requested.
 */
public interface RSyntaxNode extends RSyntaxElement {

    /**
     * A convenience method that captures the fact that, while the notion of a syntax node is
     * described in this interface, in practice all {@link RSyntaxNode} instances are also
     * {@link RNode}s. At runtime this method should be inlined away.
     */
    default RNode asRNode() {
        return (RNode) this;
    }

    /**
     * Similar but handles {@code FunctionDefinitionNode}.
     */
    default Node asNode() {
        return (Node) this;
    }

    /**
     * If overridden to return {@code false}, denotes a node that is being used in a non-syntactic
     * situation.
     */
    default boolean isSyntax() {
        return true;
    }

    /**
     * Indicates the case where a node that should have a valid {@link SourceSection} but for reason
     * does not have. Ideally never required.
     */
    SourceSection SOURCE_UNAVAILABLE = RSource.createUnknown("unavailable");

    /**
     * Indicates a node that was created as part of an AST transformation related to the internal
     * execution process. This should never be used for a node that could manifest to the R
     * programmer.
     */
    SourceSection INTERNAL = RSource.createUnknown("internal");

    /**
     * Indicates that this {@link SourceSection} can be created on demand if required.
     */
    SourceSection LAZY_DEPARSE = RSource.createUnknown("lazy deparse");

}
