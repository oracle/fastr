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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.WriteVariableNode.Mode;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.gnur.*;
import com.oracle.truffle.r.runtime.nodes.*;

public final class ForNode extends AbstractLoopNode implements VisibilityController, RSyntaxNode {

    @Child private WriteVariableNode writeLengthNode;
    @Child private WriteVariableNode writeIndexNode;
    @Child private WriteVariableNode writeRangeNode;
    @Child private LoopNode loopNode;

    protected ForNode(WriteVariableNode cvar, RNode range, RNode body) {
        String indexName = AnonymousFrameVariable.create("FOR_INDEX");
        String rangeName = AnonymousFrameVariable.create("FOR_RANGE");
        String lengthName = AnonymousFrameVariable.create("FOR_LENGTH");

        this.writeIndexNode = WriteVariableNode.createAnonymous(indexName, null, Mode.REGULAR);
        this.writeRangeNode = WriteVariableNode.createAnonymous(rangeName, range, Mode.REGULAR);
        this.writeLengthNode = WriteVariableNode.createAnonymous(lengthName, RLengthNodeGen.create(ReadVariableNode.create(rangeName, false)), Mode.REGULAR);
        this.loopNode = Truffle.getRuntime().createLoopNode(new ForRepeatingNode(this, cvar, body, indexName, lengthName, rangeName));
    }

    public static ForNode create(WriteVariableNode cvar, RSyntaxNode range, RSyntaxNode body) {
        return new ForNode(cvar, range.asRNode(), body.asRNode());
    }

    @Override
    public Object execute(VirtualFrame frame) {
        writeIndexNode.execute(frame, 1);
        writeRangeNode.execute(frame);
        writeLengthNode.execute(frame);
        loopNode.executeLoop(frame);
        forceVisibility(false);
        return RNull.instance;
    }

    public WriteVariableNode getCvar() {
        return getForRepeatingNode().writeElementNode;
    }

    public RNode getRange() {
        return writeRangeNode.getRhs();
    }

    public RNode getBody() {
        return getForRepeatingNode().body;
    }

    @Override
    public void deparseImpl(RDeparse.State state) {
        state.startNodeDeparse(this);
        state.append("for (");
        getCvar().deparse(state);
        state.append(" in ");
        getRange().deparse(state);
        state.append(") ");
        state.writeOpenCurlyNLIncIndent();
        getBody().deparse(state);
        state.decIndentWriteCloseCurly();
        state.endNodeDeparse(this);
    }

    @Override
    public void serializeImpl(RSerialize.State state) {
        state.setAsBuiltin("for");
        state.openPairList(SEXPTYPE.LISTSXP);
        // variable
        state.serializeNodeSetCar(getCvar());
        // range
        state.openPairList(SEXPTYPE.LISTSXP);
        state.serializeNodeSetCar(getRange());
        // body
        state.openPairList(SEXPTYPE.LISTSXP);
        state.openBrace();
        state.serializeNodeSetCdr(getBody(), SEXPTYPE.LISTSXP);
        state.closeBrace();
        state.linkPairList(3);
        state.setCdr(state.closePairList());
    }

    @Override
    public RSyntaxNode substituteImpl(REnvironment env) {
        return create((WriteVariableNode) getCvar().substitute(env), getRange().substitute(env), getBody().substitute(env));
    }

    private ForRepeatingNode getForRepeatingNode() {
        return (ForRepeatingNode) loopNode.getRepeatingNode();
    }

    private static final class ForRepeatingNode extends RBaseNode implements RepeatingNode {

        private static final Source ACCESS_ARRAY_SOURCE = Source.fromText("x[[i]]", "<lfor_array_access>");

        private final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();
        private final BranchProfile breakBlock = BranchProfile.create();
        private final BranchProfile nextBlock = BranchProfile.create();

        @Child private WriteVariableNode writeElementNode;
        @Child private RNode body;

        @Child private ReadVariableNode readIndexNode;
        @Child private ReadVariableNode readLengthNode;
        @Child private WriteVariableNode writeIndexNode;
        @Child private RNode loadElement;

        // used as RSyntaxNode
        private final ForNode forNode;

        public ForRepeatingNode(ForNode forNode, WriteVariableNode cvar, RNode body, String indexName, String lengthName, String rangeName) {
            this.forNode = forNode;
            this.writeElementNode = cvar;
            this.body = body;

            this.readIndexNode = ReadVariableNode.createAnonymous(indexName);
            this.readLengthNode = ReadVariableNode.createAnonymous(lengthName);
            this.writeIndexNode = WriteVariableNode.createAnonymous(indexName, null, Mode.REGULAR);
            this.loadElement = createIndexedLoad(indexName, rangeName);
            // pre-initialize the profile so that loop exits to not deoptimize
            conditionProfile.profile(false);
        }

        private static RNode createIndexedLoad(String indexName, String rangeName) {
            RCallNode indexNode;
            try {
                indexNode = (RCallNode) ((RLanguage) RContext.getEngine().parse(ACCESS_ARRAY_SOURCE).getDataAt(0)).getRep();
            } catch (ParseException ex) {
                throw RInternalError.shouldNotReachHere();
            }
            REnvironment env = RDataFactory.createInternalEnv();
            env.safePut("i", RDataFactory.createLanguage(ReadVariableNode.createAnonymous(indexName)));
            env.safePut("x", RDataFactory.createLanguage(ReadVariableNode.createAnonymous(rangeName)));
            return indexNode.substitute(env).asRNode();
        }

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
                    writeElementNode.execute(frame, loadElement.execute(frame));
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
            } finally {
                writeIndexNode.execute(frame, index + 1);
            }
        }

        @Override
        protected RSyntaxNode getRSyntaxNode() {
            return forNode;
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
            if (sourceSection != null) {
                startLine = sourceSection.getStartLine();
            }
            return String.format("for-<%s:%d>", function, startLine);
        }
    }
}
