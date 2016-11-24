/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.nodes.access.AccessArgumentNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.PromiseNode;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxConstant;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;

/**
 * Provides small helper function for eager evaluation of arguments for the use in
 * {@link PromiseNode} and {@link AccessArgumentNode}.
 */
public class EagerEvalHelper {

    /**
     * @return Whether to use optimizations for constants
     */
    public static boolean optConsts() {
        return FastROptions.EagerEval.getBooleanValue() || FastROptions.EagerEvalConstants.getBooleanValue();
    }

    /**
     * @return Whether to use optimizations for single variables
     */
    public static boolean optVars() {
        return FastROptions.EagerEval.getBooleanValue() || FastROptions.EagerEvalVariables.getBooleanValue();
    }

    /**
     * @return Whether to use optimizations for single variables
     */
    public static boolean optDefault() {
        return FastROptions.EagerEval.getBooleanValue() || FastROptions.EagerEvalDefault.getBooleanValue();
    }

    /**
     * @return Whether to use optimizations for arbitrary expressions
     */
    public static boolean optExprs() {
        return FastROptions.EagerEval.getBooleanValue() || FastROptions.EagerEvalExpressions.getBooleanValue();
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
        if (expr instanceof RSyntaxCall) {
            RSyntaxCall call = (RSyntaxCall) expr;
            if (call.getSyntaxLHS() instanceof RSyntaxLookup) {
                String functionName = ((RSyntaxLookup) call.getSyntaxLHS()).getIdentifier();
                switch (functionName) {
                    case "character":
                        if (call.getSyntaxArguments().length == 0) {
                            return RDataFactory.createEmptyStringVector();
                        } else if (call.getSyntaxArguments().length == 1) {
                            RSyntaxElement argument = call.getSyntaxArguments()[0];
                            Integer value = RSyntaxConstant.asIntConstant(argument, true);
                            if (value != null) {
                                return ShareObjectNode.share(RDataFactory.createStringVector(value));
                            }
                        }
                        break;
                }
            }
        } else if (expr instanceof RSyntaxConstant) {
            return ((RSyntaxConstant) expr).getValue();
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
     */
    private static boolean isVariableArgument(RBaseNode expr) {
        // Do NOT try to optimize anything that might force a Promise, as this might be arbitrary
        // complex (time and space)!
        return expr instanceof ReadVariableNode && !((ReadVariableNode) expr).isForceForTypeCheck();
    }

    private static boolean isCheapExpressionArgument(@SuppressWarnings("unused") RNode expr) {
        // TODO Implement cheap eagerness analysis
        return false;
    }
}
