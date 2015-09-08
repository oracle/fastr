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
package com.oracle.truffle.r.nodes.function.opt;

import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode.ReadKind;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.nodes.*;

/**
 * Provides small helper function for eager evaluation of arguments for the use in
 * {@link PromiseNode} and {@link AccessArgumentNode}.
 */
public class EagerEvalHelper {

    /**
     * @return Whether to use optimizations for constants
     */
    public static boolean optConsts() {
        return FastROptions.EagerEval || FastROptions.EagerEvalConstants;
    }

    /**
     * @return Whether to use optimizations for single variables
     */
    public static boolean optVars() {
        return FastROptions.EagerEval || FastROptions.EagerEvalVariables;
    }

    /**
     * @return Whether to use optimizations for single variables
     */
    public static boolean optDefault() {
        return FastROptions.EagerEval || FastROptions.EagerEvalDefault;
    }

    /**
     * @return Whether to use optimizations for arbitrary expressions
     */
    public static boolean optExprs() {
        return FastROptions.EagerEval || FastROptions.EagerEvalExpressions;
    }

    /**
     * This methods checks if an argument is a {@link ConstantNode}. Thanks to "..." unrolling, this
     * does not need to handle "..." as special case (which might result in a {@link ConstantNode}
     * of RMissing.instance if empty).
     *
     * @param expr
     * @return Whether the given {@link RNode} is a {@link ConstantNode}
     */
    public static Object getOptimizableConstant(RNode expr) {
        if (!optConsts()) {
            return null;
        }
        if (expr instanceof RCallNode) {
            RCallNode call = (RCallNode) expr;
            if (call.getFunctionNode() instanceof ReadVariableNode) {
                String functionName = ((ReadVariableNode) call.getFunctionNode()).getIdentifier();
                switch (functionName) {
                    case "character":
                        if (call.getArgumentCount() == 0) {
                            return RDataFactory.createEmptyStringVector();
                        } else if (call.getArgumentCount() == 1) {
                            RSyntaxNode argument = call.getArgument(0);
                            Integer value = ConstantNode.asIntConstant(argument, true);
                            if (value != null) {
                                RStringVector vector = RDataFactory.createStringVector(value);
                                ArgumentStatePush.transitionStateSlowPath(vector);
                                return vector;
                            }
                        }
                        break;
                }
            }
        } else if (expr instanceof ConstantNode) {
            return ((ConstantNode) expr).getValue();
        }
        return null;
    }

    public static boolean isOptimizableVariable(RNode expr) {
        return optVars() && isVariableArgument(expr);
    }

    public static boolean isOptimizableDefault(RNode expr) {
        return optDefault() && isVariableArgument(expr);
    }

    public static boolean isOptimizableExpression(RNode expr) {
        return optExprs() && isCheapExpressionArgument(expr);
    }

    /**
     * @return Whether the given {@link RNode} is a {@link ReadVariableNode}
     *
     * @see FastROptions#EagerEvalVariables
     */
    public static boolean isVariableArgument(RBaseNode expr) {
        // Do NOT try to optimize anything that might force a Promise, as this might be arbitrary
        // complex (time and space)!
        return expr instanceof ReadVariableNode && ((ReadVariableNode) expr).getKind() != ReadKind.Forced;
    }

    private static boolean isCheapExpressionArgument(@SuppressWarnings("unused") RNode expr) {
        // TODO Implement cheap eagerness analysis
        return false;
    }
}
