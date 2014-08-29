package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.r.nodes.*;

/**
 * @author TODO Gero, add comment!
 *
 */
public interface UnmatchedArguments {
    RNode[] getArguments();

    String[] getNames();
}
