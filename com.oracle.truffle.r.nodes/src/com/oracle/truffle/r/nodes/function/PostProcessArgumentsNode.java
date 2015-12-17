/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.profiles.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.nodes.*;

/**
 * Encapsulates the nodes that decrement reference count incremented when the argument node is
 * unwrapped.
 */
public final class PostProcessArgumentsNode extends RNode {

    @Children protected final ReadVariableNode[] sequence;
    @Child private PostProcessArgumentsNode nextOptPostProccessArgNode;
    @CompilationFinal public int transArgsBitSet;
    // the first time this node is cloned (via FunctionDefinitionNode) it's from the trufflerizer -
    // in this case we should not create the node chain chain in the clone operation (below) at the
    // reference count meta data needs to be associated with this node and not its clone
    @CompilationFinal private boolean createClone;
    private final ConditionProfile isRefCountUpdateable = ConditionProfile.createBinaryProfile();

    private PostProcessArgumentsNode(ReadVariableNode[] sequence) {
        this.sequence = sequence;
        this.createClone = false;
        this.transArgsBitSet = 0;
    }

    public static PostProcessArgumentsNode create(int length) {
        int maxLength = Math.min(length, ArgumentStatePush.MAX_COUNTED_ARGS);
        ReadVariableNode[] argReadNodes = new ReadVariableNode[maxLength];
        for (int i = 0; i < maxLength; i++) {
            argReadNodes[i] = ReadVariableNode.createForRefCount(Integer.valueOf(1 << i));
        }
        return new PostProcessArgumentsNode(argReadNodes);
    }

    public int getLength() {
        return sequence.length;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        if (transArgsBitSet > 0) {
            for (int i = 0; i < sequence.length; i++) {
                int mask = 1 << i;
                if ((transArgsBitSet & mask) == mask) {
                    RShareable s = (RShareable) sequence[i].execute(frame);
                    if (s == null) {
                        // it happens rarely in compiled code, but if it does happen, stop
                        // decrementing reference counts
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        transArgsBitSet = ArgumentStatePush.INVALID_INDEX;
                        break;
                    }
                    if (isRefCountUpdateable.profile(!s.isSharedPermanent())) {
                        s.decRefCount();
                    }
                }
            }
        }
        return RNull.instance;
    }

    @Override
    public Node deepCopy() {
        CompilerAsserts.neverPartOfCompilation();
        if (createClone) {
            PostProcessArgumentsNode copy = (PostProcessArgumentsNode) super.deepCopy();
            nextOptPostProccessArgNode = insert(copy);
            return copy;
        } else {
            this.createClone = true;
            return this;
        }
    }

    PostProcessArgumentsNode getActualNode() {
        CompilerAsserts.neverPartOfCompilation();
        if (nextOptPostProccessArgNode == null) {
            return this;
        } else {
            PostProcessArgumentsNode nextNode = nextOptPostProccessArgNode.getActualNode();
            // shorten up the lookup chain
            nextOptPostProccessArgNode = insert(nextNode);
            return nextNode;
        }
    }

}
