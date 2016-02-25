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

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.nodes.*;

/**
 * A {@link BlockNode} represents a sequence of statements such as the body of a {@code while} loop.
 *
 */
public class BlockNode extends SequenceNode implements RSyntaxNode, RSyntaxCall, VisibilityController {
    public static final RNode[] EMPTY_BLOCK = new RNode[0];
    private SourceSection sourceSection;

    public BlockNode(SourceSection src, RNode[] sequence) {
        super(sequence);
        this.sourceSection = src;
    }

    /**
     * A convenience method where {@code node} may or may not be a a {@link SequenceNode} already.
     */
    public BlockNode(SourceSection src, RSyntaxNode node) {
        this(src, convert(node));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        controlVisibility();
        return super.execute(frame);
    }

    /**
     * Ensures that {@code node} is a {@link BlockNode}.
     */
    public static RSyntaxNode ensureBlock(RSyntaxNode node) {
        if (node == null || node instanceof BlockNode) {
            return node;
        } else {
            return new BlockNode(node.getSourceSection(), new RNode[]{node.asRNode()});
        }
    }

    private static RNode[] convert(RSyntaxNode node) {
        if (node instanceof BlockNode) {
            return ((SequenceNode) node).sequence;
        } else {
            return new RNode[]{node.asRNode()};
        }
    }

    @TruffleBoundary
    @Override
    public void deparseImpl(RDeparse.State state) {
        state.startNodeDeparse(this);
        // empty deparses as {}
        if (sequence.length != 1 || RASTUtils.hasBraces(this)) {
            state.writeOpenCurlyNLIncIndent();
        }
        for (int i = 0; i < sequence.length; i++) {
            state.mark();
            sequence[i].deparse(state);
            if (state.changed()) {
                // not all nodes will produce output
                state.writeline();
                state.mark(); // in case last
            }
        }
        if (sequence.length != 1 || RASTUtils.hasBraces(this)) {
            state.decIndentWriteCloseCurly();
        }
        state.endNodeDeparse(this);
    }

    @Override
    public void serializeImpl(RSerialize.State state) {
        /*
         * In GnuR there are no empty statement sequences, because "{" is really a function in R, so
         * it is represented as a LANGSXP with symbol "{" and a NULL cdr, representing the empty
         * sequence. This is an unpleasant special case in FastR that we can only detect by
         * re-examining the original source.
         *
         * A sequence of length 1, i.e. a single statement, is represented as itself, e.g. a SYMSXP
         * for "x" or a LANGSXP for a function call. Otherwise, the representation is a LISTSXP
         * pairlist, where the car is the statement and the cdr is either NILSXP or a LISTSXP for
         * the next statement. Typically the statement (car) is itself a LANGSXP pairlist but it
         * might be a simple value, e.g. SYMSXP.
         */
        if (sequence.length == 0) {
            state.setNull();
        } else {
            for (int i = 0; i < sequence.length; i++) {
                state.serializeNodeSetCar(sequence[i]);
                if (i != sequence.length - 1) {
                    state.openPairList();
                }
            }
            state.linkPairList(sequence.length);
        }
    }

    @TruffleBoundary
    @Override
    public RSyntaxNode substituteImpl(REnvironment env) {
        RNode[] sequenceSubs = new RNode[sequence.length];
        for (int i = 0; i < sequence.length; i++) {
            sequenceSubs[i] = sequence[i].substitute(env).asRNode();
        }
        return new BlockNode(null, sequenceSubs);
    }

    @Override
    public void allNamesImpl(RAllNames.State state) {
        if (sequence.length != 1 || RASTUtils.hasBraces(this)) {
            state.addName("{");
        }
        for (int i = 0; i < sequence.length; i++) {
            sequence[i].allNames(state);
        }
    }

    public int getRlengthImpl() {
        /*
         * We can't get this completely compatible with GnuR without knowing if the source had a "{"
         * or not. However, semantically what really matters is that if the length is > 1, there
         * *must* have been a "{", so we fabricate it. Furthermore, if length==1, then we must
         * delegate to the underlying node
         */
        int len = getSequence().length;
        if (len == 1) {
            return getSequence()[0].asRSyntaxNode().getRlengthImpl();
        } else {
            return len + 1;
        }
    }

    @Override
    public Object getRelementImpl(int index) {
        /* See related comments in getRlengthImpl. */
        RNode[] seq = getSequence();
        if (seq.length > 1) {
            switch (index) {
                case 0:
                    return RDataFactory.createSymbol("{");
                default:
                    return RASTUtils.createLanguageElement(seq[index - 1]);
            }
        } else {
            return getSequence()[0].asRSyntaxNode().getRelementImpl(index);
        }
    }

    @Override
    public boolean getRequalsImpl(RSyntaxNode other) {
        if (!(other instanceof BlockNode)) {
            return false;
        }
        BlockNode otherBlock = (BlockNode) other;
        if (getRlengthImpl() != otherBlock.getRlengthImpl()) {
            return false;
        }
        RNode[] seq = getSequence();
        RNode[] otherSeq = otherBlock.getSequence();
        for (int i = 0; i < sequence.length; i++) {
            RSyntaxNode e = seq[i].asRSyntaxNode();
            RSyntaxNode eOther = otherSeq[i].asRSyntaxNode();
            if (!e.getRequalsImpl(eOther)) {
                return false;
            }
        }
        return true;
    }

    public void setSourceSection(SourceSection sourceSection) {
        this.sourceSection = sourceSection;
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSection;
    }

    @Override
    public void unsetSourceSection() {
        sourceSection = null;
    }

    public RSyntaxElement getSyntaxLHS() {
        return RSyntaxLookup.createDummyLookup(getSourceSection(), "{", true);
    }

    public RSyntaxNode[] getSyntaxArguments() {
        return RASTUtils.asSyntaxNodes(sequence);
    }

    public ArgumentsSignature getSyntaxSignature() {
        return ArgumentsSignature.empty(sequence.length);
    }
}
