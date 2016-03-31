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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.VisibilityController;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSourceSectionNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * A {@link BlockNode} represents a sequence of statements created by "{ ... }" in source code.
 */
public final class BlockNode extends RSourceSectionNode implements RSyntaxNode, RSyntaxCall, VisibilityController {

    public static final RNode[] EMPTY_BLOCK = new RNode[0];

    @Children protected final RNode[] sequence;

    public BlockNode(SourceSection src, RNode[] sequence) {
        super(src);
        this.sequence = sequence;
    }

    public RNode[] getSequence() {
        return sequence;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        controlVisibility();
        Object lastResult = RNull.instance;
        for (int i = 0; i < sequence.length; i++) {
            lastResult = sequence[i].execute(frame);
        }
        return lastResult;
    }

    @TruffleBoundary
    @Override
    public void deparseImpl(RDeparse.State state) {
        state.startNodeDeparse(this);
        // empty deparses as {}
        state.writeOpenCurlyNLIncIndent();
        for (int i = 0; i < sequence.length; i++) {
            state.mark();
            sequence[i].deparse(state);
            if (state.changed()) {
                // not all nodes will produce output
                state.writeline();
                state.mark(); // in case last
            }
        }
        state.decIndentWriteCloseCurly();
        state.endNodeDeparse(this);
    }

    @Override
    public void serializeImpl(RSerialize.State state) {
        state.setAsLangType();
        state.setCarAsSymbol("{");

        for (int i = 0; i < sequence.length; i++) {
            state.openPairList(SEXPTYPE.LISTSXP);
            state.serializeNodeSetCar(sequence[i]);
        }
        state.linkPairList(sequence.length + 1);
    }

    @TruffleBoundary
    @Override
    public RSyntaxNode substituteImpl(REnvironment env) {
        RNode[] sequenceSubs = new RNode[sequence.length];
        for (int i = 0; i < sequence.length; i++) {
            sequenceSubs[i] = sequence[i].substitute(env).asRNode();
        }
        return new BlockNode(RSyntaxNode.EAGER_DEPARSE, sequenceSubs);
    }

    @Override
    public RSyntaxElement getSyntaxLHS() {
        return RSyntaxLookup.createDummyLookup(getSourceSection(), "{", true);
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
