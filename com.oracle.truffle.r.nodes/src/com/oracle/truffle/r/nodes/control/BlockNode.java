/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.control;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.IntValueProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.context.FastROptions;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * A {@link BlockNode} represents a sequence of statements created by "{ ... }" in source code.
 */
public final class BlockNode extends AbstractBlockNode {

    private static final TruffleLogger LOGGER = RLogger.getLogger(BlockNode.class.getName());

    @Children private final RNode[] sequence;
    @Child private SetVisibilityNode visibility;

    private BlockNode(SourceSection src, RSyntaxLookup operator, RNode[] sequence) {
        super(src, operator);
        this.sequence = sequence;
    }

    public RNode[] getSequence() {
        return sequence;
    }

    public static AbstractBlockNode create(SourceSection src, RSyntaxLookup operator, RNode[] sequence) {
        int blockSizeLimit = RContext.getInstance().getOption(FastROptions.BlockSizeLimit);
        if (blockSizeLimit > 0) {
            int blockSequenceSizeLimit = RContext.getInstance().getOption(FastROptions.BlockSequenceSizeLimit);
            int[] sizes = isHugeBlock(src, sequence, blockSequenceSizeLimit, blockSizeLimit);
            if (sizes != null) {
                return createHugeBlockNode(src, operator, sequence, sizes, blockSizeLimit);
            }
        }
        return new BlockNode(src, operator, sequence);
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
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
    @ExplodeLoop
    public Object visibleExecute(VirtualFrame frame) {
        if (sequence.length == 0) {
            if (visibility == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                visibility = insert(SetVisibilityNode.create());
            }
            visibility.execute(frame, true);
            return RNull.instance;
        }
        for (int i = 0; i < sequence.length - 1; i++) {
            sequence[i].voidExecute(frame);
        }
        return sequence[sequence.length - 1].visibleExecute(frame);
    }

    @Override
    public RSyntaxNode[] getSyntaxArguments() {
        return RASTUtils.asSyntaxNodes(sequence);
    }

    @Override
    public ArgumentsSignature getSyntaxSignature() {
        return ArgumentsSignature.empty(sequence.length);
    }

    private static int[] isHugeBlock(SourceSection src, RNode[] sequence, int blockSequenceSizeLimit, int blockSizeLimit) {
        if (sequence.length <= blockSequenceSizeLimit) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.log(Level.FINER, "{0} sequence.length < blockSequenceSizeLimit : {1} < {2}", new Object[]{classNameAndHash(src), sequence.length, blockSequenceSizeLimit});
            }
            return null;
        }
        CompilerAsserts.neverPartOfCompilation();
        NodeCounter counter = new NodeCounter();

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.log(Level.FINER, "{0} sequence:", classNameAndHash(src));
        }

        int[] sizes = new int[sequence.length];
        int size = 0;
        long t = System.currentTimeMillis();
        for (int i = 0; i < sizes.length; i++) {
            Node ch = sequence[i];
            ch.accept(counter);
            sizes[i] = counter.size;
            size += sizes[i];
            counter.size = 0;

            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.log(Level.FINER, "  {0}, size {1}", new Object[]{classNameAndHash(ch), sizes[i]});
            }
        }
        if (size >= blockSizeLimit) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "sequence for {0} is huge: sequence size {1}, nodes size {2}, eval took {3} millis",
                                new Object[]{classNameAndHash(src), sequence.length, size, (System.currentTimeMillis() - t)});
            }
            return sizes;
        }
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "{0} sequence size {1}, nodes size {2}, eval took {3} millis",
                            new Object[]{classNameAndHash(src), sequence.length, size, (System.currentTimeMillis() - t)});
        }
        return null;
    }

    private static HugeBlockNode createHugeBlockNode(SourceSection src, RSyntaxLookup operator, RNode[] sequence, int[] sizes, int blockSizeLimitOption) {
        assert sizes.length == sequence.length;
        long t = System.currentTimeMillis();
        try {
            CompilerAsserts.neverPartOfCompilation();
            List<HugeBlockRootNode> rootNodes = new ArrayList<>(sequence.length);

            int cumSize = 0;
            int idxFrom = 0;
            int blockIdx = 0;
            for (int i = 0; i < sequence.length; i++) {
                if (cumSize + sizes[i] > blockSizeLimitOption || i == sequence.length - 1) {
                    final RNode[] subSequence = new RNode[i - idxFrom + 1];
                    for (int j = 0; j < subSequence.length; j++) {
                        subSequence[j] = sequence[idxFrom + j];
                    }
                    HugeBlockRootNode blockNode;
                    if (i < sequence.length - 1) {
                        blockNode = new VoidRootNode(src, ":BLOCK" + blockIdx++, subSequence);
                        blockNode.adoptChildren();
                        rootNodes.add(blockNode);
                    } else {
                        blockNode = new BlockRootNode(src, ":BLOCK" + blockIdx++, subSequence);
                        blockNode.adoptChildren();
                        rootNodes.add(blockNode);
                    }

                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "  sub-block: {0}, sequence size {1}, node size {2}", new Object[]{blockNode.toString(), subSequence.length, cumSize + sizes[i]});
                    }

                    idxFrom = i + 1;
                    cumSize = 0;
                } else {
                    cumSize += sizes[i];
                }
            }
            // the original sequence isn't held like @Children in HugeBlockNode, and used only to
            // compute getSyntaxArguments/Signature.
            return new HugeBlockNode(src, operator, sequence, rootNodes.toArray(new HugeBlockRootNode[rootNodes.size()]));
        } finally {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "{0} creating huge block node took {1} millis", new Object[]{classNameAndHash(src), (System.currentTimeMillis() - t)});
            }
        }
    }

    public static final class HugeBlockNode extends AbstractBlockNode {
        private static final int VOID_EXECUTE = 0;
        private static final int EXECUTE = 1;
        private static final int VISIBLE_EXECUTE = 2;

        @Child private SetVisibilityNode visibility;
        @Children protected final DirectCallNode[] calls;
        private final RNode[] originalSequence;
        private final HugeBlockRootNode[] rootNodes;
        @CompilationFinal private boolean firstExecution;

        private HugeBlockNode(SourceSection src, RSyntaxLookup operator, RNode[] sequence, HugeBlockRootNode[] rootNodes) {
            super(src, operator);
            this.originalSequence = sequence;
            this.rootNodes = rootNodes;
            // this.callTargets = new CallTarget[rootNodes.length];
            this.calls = new DirectCallNode[rootNodes.length];
            for (int i = 0; i < rootNodes.length; i++) {
                HugeBlockRootNode rootNode = rootNodes[i];
                calls[i] = Truffle.getRuntime().createDirectCallNode(Truffle.getRuntime().createCallTarget(rootNode));
            }
        }

        @Override
        @ExplodeLoop
        public Object execute(VirtualFrame frame) {
            if (!firstExecution) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                firstExecution = true;
                setOriginalRootNode();
            }

            if (calls.length == 0) {
                return RNull.instance;
            }
            MaterializedFrame materializedFrame = frame.materialize();
            for (int i = 0; i < calls.length - 1; i++) {
                calls[i].call(materializedFrame, VOID_EXECUTE);
            }
            return calls[calls.length - 1].call(materializedFrame, EXECUTE);
        }

        @Override
        @ExplodeLoop
        public void voidExecute(VirtualFrame frame) {
            if (!firstExecution) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                firstExecution = true;
                setOriginalRootNode();
            }

            MaterializedFrame materializedFrame = frame.materialize();
            for (int i = 0; i < calls.length; i++) {
                calls[i].call(materializedFrame, VOID_EXECUTE);
            }
        }

        @Override
        @ExplodeLoop
        public Object visibleExecute(VirtualFrame frame) {
            if (!firstExecution) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                firstExecution = true;
                setOriginalRootNode();
            }
            MaterializedFrame materializedFrame = frame.materialize();
            if (calls.length == 0) {
                if (visibility == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    visibility = insert(SetVisibilityNode.create());
                }
                visibility.execute(materializedFrame, true);
                return RNull.instance;
            }
            for (int i = 0; i < calls.length - 1; i++) {
                calls[i].call(materializedFrame, VOID_EXECUTE);
            }
            return calls[calls.length - 1].call(materializedFrame, VISIBLE_EXECUTE);
        }

        @Override
        public RSyntaxNode[] getSyntaxArguments() {
            return RASTUtils.asSyntaxNodes(originalSequence);
        }

        @Override
        public ArgumentsSignature getSyntaxSignature() {
            return ArgumentsSignature.empty(originalSequence.length);
        }

        private void setOriginalRootNode() {
            RootNode fdn = getRootNode();
            for (HugeBlockRootNode brn : rootNodes) {
                String oldName = null;
                if (LOGGER.isLoggable(Level.FINE)) {
                    oldName = brn.toString();
                }
                brn.setOriginalRootNode(fdn);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "  setting fdn {0} for {1} -> {2}", new Object[]{fdn, oldName, brn});
                }
            }
        }
    }

    public abstract static class HugeBlockRootNode extends RootNode {

        @Children protected RNode[] nodes;

        private final SourceSection src;
        private final String nameSuffix;
        @CompilationFinal private RootNode originalRoot;

        protected HugeBlockRootNode(SourceSection src, String nameSuffix, RNode[] nodes) {
            super(RContext.getInstance().getLanguage());
            this.nameSuffix = nameSuffix;
            this.src = src;
            this.nodes = nodes;
        }

        @Override
        public SourceSection getSourceSection() {
            return src;
        }

        protected MaterializedFrame materializeFrame(VirtualFrame frame) {
            return (MaterializedFrame) frame.getArguments()[0];
        }

        private void setOriginalRootNode(RootNode fdn) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.originalRoot = fdn;
        }

        public RootNode getOriginalRootNode() {
            return originalRoot;
        }

        @Override
        public String getName() {
            return (originalRoot != null ? originalRoot.getRootNode().getName() : getClass().getSimpleName()) + nameSuffix;
        }

        @Override
        public String toString() {
            return getName() + "@" + Integer.toHexString(hashCode());
        }

        @ExplodeLoop
        protected Object voidExecute(VirtualFrame frame) {
            MaterializedFrame materializedFrame = materializeFrame(frame);
            for (int i = 0; i < nodes.length; i++) {
                nodes[i].voidExecute(materializedFrame);
            }
            return null;
        }

    }

    private static final class VoidRootNode extends HugeBlockRootNode {
        private VoidRootNode(SourceSection src, String name, RNode[] nodes) {
            super(src, name, nodes);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return voidExecute(frame);
        }
    }

    private static final class BlockRootNode extends HugeBlockRootNode {
        final IntValueProfile executeKindProfile = IntValueProfile.createIdentityProfile();

        private BlockRootNode(SourceSection src, String name, RNode[] nodes) {
            super(src, name, nodes);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            int executeKind = executeKindProfile.profile((int) frame.getArguments()[1]);
            switch (executeKind) {
                case HugeBlockNode.VOID_EXECUTE:
                    return voidExecute(frame);
                case HugeBlockNode.EXECUTE:
                    return exec(frame);
                case HugeBlockNode.VISIBLE_EXECUTE:
                    return visibleExec(frame);
                default:
                    throw RInternalError.shouldNotReachHere();
            }
        }

        @ExplodeLoop
        private Object exec(VirtualFrame frame) {
            if (nodes.length == 0) {
                return RNull.instance;
            }
            MaterializedFrame materializedFrame = materializeFrame(frame);
            for (int i = 0; i < nodes.length - 1; i++) {
                nodes[i].voidExecute(materializedFrame);
            }
            return nodes[nodes.length - 1].execute(materializedFrame);
        }

        @ExplodeLoop
        public Object visibleExec(VirtualFrame frame) {
            if (nodes.length == 0) {
                return RNull.instance;
            }
            MaterializedFrame materializedFrame = materializeFrame(frame);
            for (int i = 0; i < nodes.length - 1; i++) {
                nodes[i].voidExecute(materializedFrame);
            }
            return nodes[nodes.length - 1].visibleExecute(materializedFrame);
        }
    }

    private static final class NodeCounter implements NodeVisitor {
        public int size;

        @Override
        public boolean visit(Node node) {
            if (!node.getCost().isTrivial()) {
                size++;
            }
            if (node instanceof ReplacementDispatchNode) {
                size += 2;
            }
            return true;
        }
    }

    private static String classNameAndHash(Object o) {
        return o.getClass().getSimpleName() + "@" + Integer.toHexString(o.hashCode());
    }
}
