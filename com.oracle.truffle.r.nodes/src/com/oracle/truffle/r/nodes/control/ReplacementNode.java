/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.UpdateFieldNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.array.write.UpdateArrayHelperNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.RSymbol;

/**
 * Holds the sequence of nodes created for R's replacement assignment. Allows custom deparse and
 * debug handling.
 */
public class ReplacementNode extends RNode {

    @Children protected final RNode[] sequence;

    public ReplacementNode(SourceSection src, RNode[] seq) {
        this.sequence = seq;
        assignSourceSection(src);
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        Object lastResult = null;
        for (int i = 0; i < sequence.length; i++) {
            lastResult = sequence[i].execute(frame);
        }
        return lastResult;
    }

    @Override
    public boolean isSyntax() {
        return true;
    }

    @Override
    public void deparse(RDeparse.State state) {
        WriteVariableNode valueStoreNode = (WriteVariableNode) sequence[0];
        /*
         * The rhs of valueStoreNode is the rhs of our result. sequence[1] is the copy of the object
         * being updated. sequence[2] is the interesting part and varies depending on the type of
         * update.
         */
        WriteVariableNode tmpAssignNode = (WriteVariableNode) sequence[1];
        WriteVariableNode updateNode = null;
        RNode s2 = sequence[2];
        if (s2 instanceof WriteVariableNode) {
            updateNode = (WriteVariableNode) s2;
        } else if (s2 instanceof ReplacementNode) {
            // FIXME this does not produce the right result
            ReplacementNode rn = (ReplacementNode) s2;
            updateNode = (WriteVariableNode) rn.sequence[2];
        } else {
            RInternalError.shouldNotReachHere();
        }
        RNode updateNodeRhs = (RNode) RASTUtils.unwrap(updateNode.getRhs());
        if (updateNodeRhs instanceof UpdateFieldNode) {
            UpdateFieldNode ufn = (UpdateFieldNode) updateNodeRhs;
            tmpAssignNode.getRhs().deparse(state);
            state.append('$');
            state.append(ufn.getField());
            state.append(" <- ");
            valueStoreNode.getRhs().deparse(state);
        } else if (updateNodeRhs instanceof RCallNode) {
            // E.g `class<-`
            RSymbol funcSymbol = (RSymbol) RASTUtils.findFunctionName(updateNodeRhs, false);
            String funcName = funcSymbol.getName();
            funcName = funcName.substring(0, funcName.length() - 2);
            state.append(funcName);
            state.append('(');
            tmpAssignNode.getRhs().deparse(state);
            state.append(") <- ");
            valueStoreNode.getRhs().deparse(state);
        } else if (updateNodeRhs instanceof UpdateArrayHelperNode) {
            UpdateArrayHelperNode uan = (UpdateArrayHelperNode) updateNodeRhs;
            tmpAssignNode.getRhs().deparse(state);
            uan.deparse(state);
            state.append(" <- ");
            valueStoreNode.getRhs().deparse(state);
        } else {
            RInternalError.shouldNotReachHere();
        }
    }
}
