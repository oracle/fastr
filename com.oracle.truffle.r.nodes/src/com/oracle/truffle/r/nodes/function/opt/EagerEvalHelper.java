/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
