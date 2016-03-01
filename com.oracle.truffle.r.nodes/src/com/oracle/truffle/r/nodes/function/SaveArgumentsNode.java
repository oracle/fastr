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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * Encapsulates the nodes that save the incoming function arguments into the frame. Functionally a
 * pass-through, but provides structure that assists instrumentation. This <b>always</b> exists even
 * if the function has no formal arguments.
 *
 * TODO This used to forward the substitute method even though it is not part of the "syntax".
 * However, it is likely that without it, substitute on an entire function will not execute
 * correctly.
 */
@NodeInfo(cost = NodeCost.NONE)
public class SaveArgumentsNode extends RNode {

    public static final SaveArgumentsNode NO_ARGS = new SaveArgumentsNode(RNode.EMTPY_RNODE_ARRAY);

    @Children private final RNode[] sequence;

    public SaveArgumentsNode(RNode[] sequence) {
        this.sequence = sequence;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        for (RNode node : sequence) {
            node.execute(frame);
        }
        return RNull.instance;
    }

}
