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
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.array.read.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.control.ForNodeFactory.LengthNodeGen;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.RContext.Engine.ParseException;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

public final class ForNode extends AbstractLoopNode {

    @Child private WriteVariableNode writeLengthNode;
    @Child private WriteVariableNode writeIndexNode;
    @Child private WriteVariableNode writeRangeNode;
    @Child private LoopNode loopNode;

    protected ForNode(WriteVariableNode cvar, RNode range, RNode body) {
        String indexName = AnonymousFrameVariable.create("FOR_INDEX");
        String rangeName = AnonymousFrameVariable.create("FOR_RANGE");
        String lengthName = AnonymousFrameVariable.create("FOR_LENGTH");

        this.writeIndexNode = WriteVariableNode.create(indexName, null, false, false);
        this.writeRangeNode = WriteVariableNode.create(rangeName, range, false, false);
        this.writeLengthNode = WriteVariableNode.create(lengthName, LengthNodeGen.create(ReadVariableNode.create(rangeName, false)), false, false);
        this.loopNode = Truffle.getRuntime().createLoopNode(new ForRepeatingNode(cvar, body, indexName, lengthName, rangeName));
    }

    public static ForNode create(WriteVariableNode cvar, RNode range, RNode body) {
        return new ForNode(cvar, range, body);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        writeIndexNode.execute(frame, 1);
        writeRangeNode.execute(frame);
        writeLengthNode.execute(frame);
        loopNode.executeLoop(frame);
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
    public boolean isSyntax() {
        return true;
    }

    @Override
    public void deparse(State state) {
        state.append("for (");
        getCvar().deparse(state);
        state.append(" in ");
        getRange().deparse(state);
        state.append(") ");
        state.writeOpenCurlyNLIncIndent();
        getBody().deparse(state);
        state.decIndentWriteCloseCurly();
    }

    @Override
    public RNode substitute(REnvironment env) {
        // TODO check type of cvar.substitute
        return create((WriteVariableNode) getCvar().substitute(env), getRange().substitute(env), getBody().substitute(env));
    }

    private ForRepeatingNode getForRepeatingNode() {
        return (ForRepeatingNode) loopNode.getRepeatingNode();
    }

    private static final class ForRepeatingNode extends Node implements RepeatingNode {

        private static final Source ACCESS_ARRAY_SOURCE = Source.asPseudoFile("x[[i]]", "<lfor_array_access>");

        private final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();
        private final BranchProfile breakBlock = BranchProfile.create();
        private final BranchProfile nextBlock = BranchProfile.create();

        @Child private WriteVariableNode writeElementNode;
        @Child private RNode body;

        @Child private ReadVariableNode readIndexNode;
        @Child private ReadVariableNode readLengthNode;
        @Child private WriteVariableNode writeIndexNode;
        @Child private RNode loadElement;

        public ForRepeatingNode(WriteVariableNode writeElementNode, RNode body, String indexName, String lengthName, String rangeName) {
            this.writeElementNode = writeElementNode;
            this.body = body;

            this.readIndexNode = ReadVariableNode.create(indexName, false);
            this.readLengthNode = ReadVariableNode.create(lengthName, false);
            this.writeIndexNode = WriteVariableNode.create(indexName, null, false, false);
            this.loadElement = createIndexedLoad(indexName, rangeName);
        }

        private static RNode createIndexedLoad(String indexName, String rangeName) {
            AccessArrayNode indexNode;
            try {
                indexNode = (AccessArrayNode) ((RLanguage) RContext.getEngine().parse(ACCESS_ARRAY_SOURCE).getDataAt(0)).getRep();
            } catch (ParseException ex) {
                throw RInternalError.shouldNotReachHere();
            }
            REnvironment env = RDataFactory.createNewEnv("dummy");
            env.safePut("i", RDataFactory.createLanguage(ReadVariableNode.create(indexName, false)));
            env.safePut("x", RDataFactory.createLanguage(ReadVariableNode.create(rangeName, false)));
            return indexNode.substitute(env);
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
    }

    @NodeChild("operand")
    protected abstract static class LengthNode extends RNode {

        @Override
        public final Object execute(VirtualFrame frame) {
            return executeInteger(frame);
        }

        @Override
        public abstract int executeInteger(VirtualFrame frame);

        public abstract int executeInteger(VirtualFrame frame, Object value);

        @Specialization
        @SuppressWarnings("unused")
        protected int getLength(RNull operand) {
            return 0;
        }

        @Specialization
        @SuppressWarnings("unused")
        protected int getLength(int operand) {
            return 1;
        }

        @Specialization
        @SuppressWarnings("unused")
        protected int getLength(double operand) {
            return 1;
        }

        @Specialization
        protected int getLength(VirtualFrame frame, RExpression operand, @Cached("createRecursive()") LengthNode recursiveLength) {
            return recursiveLength.executeInteger(frame, operand.getList());
        }

        @Specialization
        protected int getLength(RAbstractContainer operand) {
            return operand.getLength();
        }

        public static LengthNode createRecursive() {
            return LengthNodeGen.create(null);
        }
    }
}
