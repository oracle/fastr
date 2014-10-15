package com.oracle.truffle.r.nodes;

import com.oracle.truffle.r.runtime.RDeparse.State;

/**
 * An adapter than can be subclassed that does nothing on {@code deparse}.
 */
public abstract class RNoDeparseNode extends RNode {
    @Override
    public void deparse(State state) {
        // do nothing
    }
}
