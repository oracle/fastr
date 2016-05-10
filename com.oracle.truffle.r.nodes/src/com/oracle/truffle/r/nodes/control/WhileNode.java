/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.unary.ConvertBooleanNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public final class WhileNode extends AbstractLoopNode implements RSyntaxNode, RSyntaxCall {

    @Child private LoopNode loop;

    /**
     * Also used for {@code repeat}, with a {@code TRUE} condition.
     */
    private final boolean isRepeat;

    private WhileNode(SourceSection src, RSyntaxNode condition, RSyntaxNode body, boolean isRepeat) {
        super(src);
        this.loop = Truffle.getRuntime().createLoopNode(new WhileRepeatingNode(this, ConvertBooleanNode.create(condition), body.asRNode()));
        this.isRepeat = isRepeat;
    }

    public static WhileNode create(SourceSection src, RSyntaxNode condition, RSyntaxNode body, boolean isRepeat) {
        WhileNode result = new WhileNode(src, condition, body, isRepeat);
        return result;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        loop.executeLoop(frame);
        RContext.getInstance().setVisible(false);
        return RNull.instance;
    }

    public ConvertBooleanNode getCondition() {
        return getRepeatingNode().getCondition();
    }

    public RNode getBody() {
        return getRepeatingNode().getBody();
    }

    private WhileRepeatingNode getRepeatingNode() {
        return (WhileRepeatingNode) loop.getRepeatingNode();
    }

    public boolean isRepeat() {
        return isRepeat;
    }

    @Override
    public void serializeImpl(RSerialize.State state) {
        state.setAsBuiltin(isRepeat ? "repeat" : "while");
        if (!isRepeat) {
            state.openPairList(SEXPTYPE.LISTSXP);
            // condition
            state.serializeNodeSetCar(getCondition());
        }
        state.openPairList(SEXPTYPE.LISTSXP);
        state.serializeNodeSetCar(getBody());
        state.linkPairList(isRepeat ? 1 : 2);
        state.setCdr(state.closePairList());
    }

    @Override
    public RSyntaxNode substituteImpl(REnvironment env) {
        return create(RSyntaxNode.EAGER_DEPARSE, getCondition().substitute(env), getBody().substitute(env), isRepeat);
    }

    private static final class WhileRepeatingNode extends RBaseNode implements RepeatingNode {

        @Child private ConvertBooleanNode condition;
        @Child private RNode body;

        private final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();
        private final BranchProfile breakBlock = BranchProfile.create();
        private final BranchProfile nextBlock = BranchProfile.create();

        // used as RSyntaxNode
        private final WhileNode whileNode;

        WhileRepeatingNode(WhileNode whileNode, ConvertBooleanNode condition, RNode body) {
            this.whileNode = whileNode;
            this.condition = condition;
            this.body = body;
            // pre-initialize the profile so that loop exits to not deoptimize
            conditionProfile.profile(false);
        }

        public RNode getBody() {
            return body;
        }

        public ConvertBooleanNode getCondition() {
            return condition;
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            try {
                if (conditionProfile.profile(condition.executeByte(frame) == RRuntime.LOGICAL_TRUE)) {
                    body.execute(frame);
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
            }
        }

        @Override
        protected RSyntaxNode getRSyntaxNode() {
            return whileNode;
        }

        @Override
        public String toString() {
            RootNode rootNode = getRootNode();
            String function = "?";
            if (rootNode instanceof RRootNode) {
                function = rootNode.toString();
            }
            SourceSection sourceSection = getRSyntaxNode().getSourceSection();
            int startLine = -1;
            String shortDescription = "?";
            if (sourceSection != null) {
                startLine = sourceSection.getStartLine();
                shortDescription = sourceSection.getSource() == null ? shortDescription : sourceSection.getSource().getShortName();
            }
            return String.format("while loop at %s<%s:%d>", function, shortDescription, startLine);
        }
    }

    @Override
    public RSyntaxElement getSyntaxLHS() {
        return RSyntaxLookup.createDummyLookup(getSourceSection(), isRepeat ? "repeat" : "while", true);
    }

    @Override
    public RSyntaxElement[] getSyntaxArguments() {
        WhileRepeatingNode repeatingNode = (WhileRepeatingNode) loop.getRepeatingNode();
        if (isRepeat) {
            return new RSyntaxElement[]{repeatingNode.body.asRSyntaxNode()};
        } else {
            return new RSyntaxElement[]{repeatingNode.condition.asRSyntaxNode(), repeatingNode.body.asRSyntaxNode()};
        }
    }

    @Override
    public ArgumentsSignature getSyntaxSignature() {
        return ArgumentsSignature.empty(isRepeat ? 1 : 2);
    }
}
