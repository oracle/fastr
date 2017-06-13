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
package com.oracle.truffle.r.nodes.control;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableFactory.WrapperNode;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode.Mode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.AnonymousFrameVariable;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public final class ForNode extends AbstractLoopNode implements RSyntaxNode, RSyntaxCall {

    @Child private WriteVariableNode writeLengthNode;
    @Child private WriteVariableNode writeIndexNode;
    @Child private WriteVariableNode writeRangeNode;
    @Child private LoopNode loopNode;
    @Child private SetVisibilityNode visibility = SetVisibilityNode.create();

    private final RSyntaxLookup var;

    public ForNode(SourceSection src, RSyntaxLookup operator, RSyntaxLookup var, RNode range, RNode body) {
        super(src, operator);
        this.var = var;
        String indexName = AnonymousFrameVariable.create("FOR_INDEX");
        String rangeName = AnonymousFrameVariable.create("FOR_RANGE");
        String lengthName = AnonymousFrameVariable.create("FOR_LENGTH");

        this.writeIndexNode = WriteVariableNode.createAnonymous(indexName, Mode.REGULAR, null);
        this.writeRangeNode = WriteVariableNode.createAnonymous(rangeName, Mode.REGULAR, range);
        this.writeLengthNode = WriteVariableNode.createAnonymous(lengthName, Mode.REGULAR, RLengthNodeGen.create(ReadVariableNode.create(rangeName)));
        this.loopNode = Truffle.getRuntime().createLoopNode(new ForRepeatingNode(this, var.getIdentifier(), body, indexName, lengthName, rangeName));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        writeIndexNode.execute(frame, 1);
        writeRangeNode.execute(frame);
        writeLengthNode.execute(frame);
        loopNode.executeLoop(frame);
        visibility.execute(frame, false);
        return RNull.instance;
    }

    private static final class ForRepeatingNode extends Node implements RepeatingNode {

        private final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();
        private final BranchProfile breakBlock = BranchProfile.create();
        private final BranchProfile nextBlock = BranchProfile.create();

        @Child private WriteVariableNode writeElementNode;
        @Child private RNode body;

        @Child private ReadVariableNode readIndexNode;
        @Child private ReadVariableNode readLengthNode;
        @Child private WriteVariableNode writeIndexNode;

        // only used for toString
        private final ForNode forNode;

        ForRepeatingNode(ForNode forNode, String var, RNode body, String indexName, String lengthName, String rangeName) {
            this.forNode = forNode;
            this.writeElementNode = WriteVariableNode.createAnonymous(var, Mode.REGULAR, createIndexedLoad(indexName, rangeName), false);
            this.body = body;

            this.readIndexNode = ReadVariableNode.create(indexName);
            this.readLengthNode = ReadVariableNode.create(lengthName);
            this.writeIndexNode = WriteVariableNode.createAnonymous(indexName, Mode.REGULAR, null);
            // pre-initialize the profile so that loop exits to not deoptimize
            conditionProfile.profile(false);
        }

        private static RNode createIndexedLoad(String indexName, String rangeName) {
            RCodeBuilder<RSyntaxNode> builder = RContext.getASTBuilder();
            RSyntaxNode receiver = builder.lookup(RSyntaxNode.INTERNAL, rangeName, false);
            RSyntaxNode index = builder.lookup(RSyntaxNode.INTERNAL, indexName, false);
            RSyntaxNode access = builder.lookup(RSyntaxNode.INTERNAL, "[[", true);
            return builder.call(RSyntaxNode.INTERNAL, access, receiver, index).asRNode();
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            int length;
            int index;
            try {
                length = readLengthNode.executeInteger(frame);
                index = readIndexNode.executeInteger(frame);
            } catch (UnexpectedResultException e1) {
                throw new AssertionError("For index must be Integer.");
            }
            try {
                if (conditionProfile.profile(index <= length)) {
                    writeElementNode.voidExecute(frame);
                    body.voidExecute(frame);
                    return true;
                } else {
                    return false;
                }
            } catch (BreakException e) {
                breakBlock.enter();
                return false;
            } catch (NextException e) {
                nextBlock.enter();
                return true;
            } finally {
                writeIndexNode.execute(frame, index + 1);
            }
        }

        @Override
        public String toString() {
            return forNode.toString();
        }
    }

    @Override
    public RSyntaxElement[] getSyntaxArguments() {
        ForRepeatingNode repeatingNode = (ForRepeatingNode) loopNode.getRepeatingNode();
        return new RSyntaxElement[]{var, writeRangeNode.getRhs().asRSyntaxNode(), repeatingNode.body.asRSyntaxNode()};
    }

    @Override
    public ArgumentsSignature getSyntaxSignature() {
        return ArgumentsSignature.empty(3);
    }

    /**
     * Tests if the provided node is a loop-body node (also considering wrappers).
     */
    public static boolean isLoopBody(Node n) {
        Node parent = n.getParent();
        if (parent instanceof WrapperNode) {
            Node grandparent = parent.getParent();
            return grandparent instanceof ForRepeatingNode && ((ForRepeatingNode) grandparent).body == parent;
        }
        return parent instanceof ForRepeatingNode && ((ForRepeatingNode) parent).body == n;
    }
}
