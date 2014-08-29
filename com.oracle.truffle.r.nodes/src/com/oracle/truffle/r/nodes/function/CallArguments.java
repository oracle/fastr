package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.r.nodes.*;

/**
 * @author TODO Gero, add comment!
 *
 */
public interface CallArguments {
    RNode[] getArguments();

    String[] getNames();
}
