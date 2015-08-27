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

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * The following methods must be implemented by all implementors of {@link RSyntaxNode}. However,
 * they should not, in general, be called, unless it is statically known that the node is an
 * {@link RSyntaxNode}. Instead the generic methods on {@link RBaseNode} should be called.
 *
 */
public interface RSyntaxNodeSPI {
    /**
     * Support for the {@code deparse} builtin function. The source representation should be
     * appended to the {@link State}.
     */
    void deparseImpl(State state);

    /**
     * Support for the {@code substitute} builtin function. Assert: {this.isSyntax() == true}. N.B.
     * this method only needs to operate on pristine (uninitialized) ASTs. The AST is cloned prior
     * to the substitution; therefore there is no need to create a new node if it can be determined
     * that no changes were made.
     */
    RSyntaxNode substituteImpl(REnvironment env);

    /**
     * Support for serializing closures. The relevant methods in {@link State} should be called to
     * create the virtual pairlist for this node,
     */
    void serializeImpl(RSerialize.State state);

    /* Methods to support access on an RLanguage object, e.g. length(quote(f(a,b,c)))) */

    /**
     * Return the "length" of this node in the R sense.
     */
    default int getRlengthImpl() {
        throw RInternalError.unimplemented("getRlengthImpl");
    }

    /**
     * Return a value (usually an {@link RLanguage} instance) for the element of this node at
     * {@code index}.
     */
    default Object getRelementImpl(@SuppressWarnings("unused") int index) {
        throw RInternalError.unimplemented("getRelementImpl");
    }

    /**
     * Return {@code true} iff this node "equals" {@code other} in the R sense. (Used for
     * {@code identical} builtin).
     */
    default boolean getRequalsImpl(@SuppressWarnings("unused") RSyntaxNode other) {
        throw RInternalError.unimplemented("getRequalsImpl");
    }

}
