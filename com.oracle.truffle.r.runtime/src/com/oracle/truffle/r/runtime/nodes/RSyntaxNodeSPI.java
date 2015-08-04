package com.oracle.truffle.r.runtime.nodes;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RDeparse.*;
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

}
