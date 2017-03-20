/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.r.nodes.unary.ConditionalMapNode.PipelineReturnException;

@NodeInfo(cost = NodeCost.NONE)
public final class ChainedCastNode extends CastNode {

    @FunctionalInterface
    public interface CastNodeFactory {
        CastNode create();
    }

    @Child private CastNode firstCast;
    @Child private CastNode secondCast;

    private final boolean isFirstNode;

    public ChainedCastNode(CastNode firstCast, CastNode secondCast, boolean isFirstNode) {
        this.firstCast = firstCast;
        this.secondCast = secondCast;
        this.isFirstNode = isFirstNode;
    }

    @Override
    public Object execute(Object value) {
        if (isFirstNode) {
            try {
                return secondCast.execute(firstCast.execute(value));
            } catch (PipelineReturnException ex) {
                return ex.getResult();
            }
        } else {
            return secondCast.execute(firstCast.execute(value));
        }
    }

    public CastNode getFirstCast() {
        return firstCast;
    }

    public CastNode getSecondCast() {
        return secondCast;
    }
}
