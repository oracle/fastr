package com.oracle.truffle.r.nodes.profile;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.*;

/**
 * Base class for nodes that are solely executed behind a {@link TruffleBoundary} to ensure that
 * these replaces or insertions below this node do not trigger invalidation of compiled code.
 *
 * TODO this is a candidate for Truffle standardization in the future.
 */
public abstract class TruffleBoundaryNode extends Node implements ReplaceObserver {

    @Override
    public final boolean nodeReplaced(Node oldNode, Node newNode, CharSequence reason) {
        return true;
    }

}
