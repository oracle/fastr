/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * A {@link BlockNode} represents a sequence of statements created by "{ ... }" in source code.
 */
public final class BlockNode extends OperatorNode {

    public static final RNode[] EMPTY_BLOCK = new RNode[0];

    @Children protected final RNode[] sequence;
    @Child private SetVisibilityNode visibility = SetVisibilityNode.create();

    public BlockNode(SourceSection src, RSyntaxLookup operator, RNode[] sequence) {
        super(src, operator);
        this.sequence = sequence;
    }

    public RNode[] getSequence() {
        return sequence;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        visibility.execute(frame, true);
        if (sequence.length == 0) {
            return RNull.instance;
        }
        for (int i = 0; i < sequence.length - 1; i++) {
            sequence[i].voidExecute(frame);
        }
        return sequence[sequence.length - 1].execute(frame);
    }

    @Override
    @ExplodeLoop
    public void voidExecute(VirtualFrame frame) {
        for (int i = 0; i < sequence.length; i++) {
            sequence[i].voidExecute(frame);
        }
    }

    @Override
    public RSyntaxNode[] getSyntaxArguments() {
        return RASTUtils.asSyntaxNodes(sequence);
    }

    @Override
    public ArgumentsSignature getSyntaxSignature() {
        return ArgumentsSignature.empty(sequence.length);
    }
}
