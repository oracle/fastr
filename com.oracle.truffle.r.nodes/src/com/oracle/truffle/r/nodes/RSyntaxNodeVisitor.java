/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/**
 * Visitor for the syntax nodes in the tree, defined by {@link RSyntaxNode#isSyntax()} returning
 * {@code true}. This interface is intended for the Truffle instrumentation layer.
 */
public interface RSyntaxNodeVisitor {
    /**
     * This visitor method is called for every node in the tree for which
     * {@link RSyntaxNode#isSyntax()} returns {@code true}. Its return value determines if the
     * children of this node should be excluded in the iteration.
     *
     * N.B. The visit order for child nodes is non-deterministic. The only guarantee is that every
     * "syntactic" child will be visited.
     *
     * @param node the node that is currently visited
     * @param depth the current depth in the tree
     * @return {@code true} if the children should be visited too, {@code false} otherwise
     */
    boolean visit(RSyntaxNode node, int depth);

}
