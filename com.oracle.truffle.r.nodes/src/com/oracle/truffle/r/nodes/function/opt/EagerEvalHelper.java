package com.oracle.truffle.r.nodes.function.opt;

import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.ConstantNode.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.options.*;

public class EagerEvalHelper {

    public static RNode unfold(Object argObj) {
        RNode arg = (RNode) argObj;
        if (arg instanceof WrapArgumentNode) {
            return ((WrapArgumentNode) arg).getOperand();
        }
        return arg;
    }

    /**
     * This methods checks if an argument is a {@link ConstantNode}. Thanks to "..." unrolling, this
     * does not need to handle "..." as special case (which might result in a
     * {@link ConstantMissingNode} if empty).
     *
     * @param expr
     * @return Whether the given {@link RNode} is a {@link ConstantNode}
     */
    public static boolean isConstantArgument(RNode expr) {
        return expr instanceof ConstantNode;
    }

    /**
     * @param expr
     * @return Whether the given {@link RNode} is a {@link ReadVariableNode}
     *
     * @see FastROptions#EagerEvalVariables
     */
    public static boolean isVariableArgument(RNode expr) {
        // Do NOT try to optimize anything that might force a Promise, as this might be arbitrary
        // complex (time and space)!
        return expr instanceof ReadVariableNode && !((ReadVariableNode) expr).getForcePromise();
    }
}
