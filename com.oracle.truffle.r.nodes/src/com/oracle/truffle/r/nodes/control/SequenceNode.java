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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.env.REnvironment;

public class SequenceNode extends RNode {

    @Children protected final RNode[] sequence;

    public SequenceNode(RNode[] sequence) {
        this.sequence = sequence;
    }

    public SequenceNode(SourceSection src, RNode[] sequence) {
        this(sequence);
        assignSourceSection(src);
    }

    /**
     * Similar to {@link #ensureSequence} but for subclasses, where we have to extract any existing
     * array.
     *
     * @param node
     */
    protected SequenceNode(RNode node) {
        this(convert(node));
    }

    public RNode[] getSequence() {
        return sequence;
    }

    /**
     * Ensures that {@code node} is a {@link SequenceNode} by converting any other node to a single
     * length sequence.
     */
    public static RNode ensureSequence(RNode node) {
        if (node == null || node instanceof SequenceNode) {
            return node;
        } else {
            return new SequenceNode(new RNode[]{node});
        }
    }

    private static RNode[] convert(RNode node) {
        if (node instanceof SequenceNode) {
            return ((SequenceNode) node).sequence;
        } else {
            return new RNode[]{node};
        }
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

    @TruffleBoundary
    @Override
    public void deparse(State state) {
        for (int i = 0; i < sequence.length; i++) {
            state.mark();
            sequence[i].deparse(state);
            if (state.changed()) {
                // not all nodes will produce output
                state.writeline();
                state.mark(); // in case last
            }
        }
    }

    @TruffleBoundary
    @Override
    public RNode substitute(REnvironment env) {
        RNode[] sequenceSubs = new RNode[sequence.length];
        for (int i = 0; i < sequence.length; i++) {
            sequenceSubs[i] = sequence[i].substitute(env);
        }
        return new SequenceNode(sequenceSubs);
    }

}
