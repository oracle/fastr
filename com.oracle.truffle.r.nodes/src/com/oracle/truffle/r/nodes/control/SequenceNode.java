/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.*;

public final class SequenceNode extends RNode {

    @Children private final RNode[] sequence;

    public SequenceNode(RNode[] sequence) {
        this.sequence = sequence;
    }

    public SequenceNode(SourceSection src, RNode[] sequence) {
        this(sequence);
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
    @SlowPath
    public void deparse(State state) {
        int i = 0;
        while (i < sequence.length) {
            state.mark();
            if (tmpAssignDeparse(state, i)) {
                i += 2;
            } else {
                sequence[i].deparse(state);
            }
            i++;
            if (state.changed()) {
                // not all nodes will produce output
                state.writeline();
            }
        }
    }

    /**
     * This is a workaround for the eager transformation that introduces {@code *tmp*} in the
     * initial AST.
     */
    private boolean tmpAssignDeparse(State state, int i) {
        RNode node1 = sequence[i];
        if (node1 instanceof WriteVariableNode.HasName) {
            WriteVariableNode.HasName node1Name = (WriteVariableNode.HasName) node1;
            if (node1Name.getName().startsWith("java.lang.Object")) {
                // @formatter:off
                /*
                 * Ok, here we go. The sequence is:
                 *   java.lang.Object@N <- rhs
                 *   *tmp* <- vec
                 *   vec$foo <- *tmp*foo <- java.lang.Object@N
                 *
                 * which we want to deparse as:
                 *   vec$foo <- rhs
                 */
                // @formatter:on
                WriteVariableNode node2 = (WriteVariableNode) sequence[i + 1];
                WriteVariableNode node3 = (WriteVariableNode) sequence[i + 2];
                state.append(((WriteVariableNode.HasName) node3).getName());
                RNode node3Rhs = node3.getRhs();
                if (node3Rhs instanceof UpdateFieldNode) {
                    UpdateFieldNode ufn = (UpdateFieldNode) node3Rhs;
                    state.append('$');
                    state.append(ufn.getField());
                } else if (node3Rhs instanceof RCallNode) {
                    node3Rhs.deparse(state);
                } else {
                    RInternalError.shouldNotReachHere();
                }
                state.append(" <- ");
                ((WriteVariableNode) node1).getRhs().deparse(state);
                return true;
            }
        }
        return false;
    }

}
