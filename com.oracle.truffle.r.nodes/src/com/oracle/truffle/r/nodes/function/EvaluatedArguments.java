package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;

/**
 * Simple container class for holding 'evaluated' arguments ({@link #getEvaluatedArgs()}) which are
 * ready to be pushed into {@link RArguments}. Objects of this class are created by
 * {@link MatchedArgumentsNode}!
 *
 * @see #getNames()
 */
public class EvaluatedArguments extends Arguments<RNode> {

    EvaluatedArguments(RNode[] evaluatedArgs, String[] names) {
        super(evaluatedArgs, names);
    }

    /**
     * @return The argument array that contains the evaluated arguments
     * @see EvaluatedArguments
     */
    public RNode[] getEvaluatedArgs() {
        return arguments;
    }

    /**
     * @return The names of the arguments that where supplied for the function, in the order the
     *         function call specifies (NOT formal order)
     * @see EvaluatedArguments
     */
    public String[] getNames() {
        return names;
    }
}
