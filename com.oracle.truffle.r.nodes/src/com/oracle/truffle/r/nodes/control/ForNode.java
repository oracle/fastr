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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode.Mode;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.AnonymousFrameVariable;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.interop.ForeignArray2R;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

@ImportStatic({RRuntime.class, ForeignArray2R.class, Message.class})
@NodeChild(value = "range", type = RNode.class)
public abstract class ForNode extends AbstractLoopNode implements RSyntaxNode, RSyntaxCall {

    @Child public RNode body;
    @Child private SetVisibilityNode visibility;

    private final RSyntaxLookup var;

    public ForNode(SourceSection src, RSyntaxLookup operator, RSyntaxLookup var, RNode body) {
        super(src, operator);
        this.var = var;
        this.body = body;
    }

    protected abstract RNode getRange();

    @Specialization(guards = "!isForeignObject(range)")
    protected Object iterate(VirtualFrame frame, Object range,
                    @Cached("createIndexName()") String indexName,
                    @Cached("createRangeName()") String rangeName,
                    @Cached("createLengthName()") String lengthName,
                    @Cached("createWriteVariable(indexName)") WriteVariableNode writeIndexNode,
                    @Cached("createWriteVariable(rangeName)") WriteVariableNode writeRangeNode,
                    @Cached("createWriteVariable(lengthName)") WriteVariableNode writeLengthNode,
                    @Cached("create()") RLengthNode length,
                    @Cached("createForIndexLoopNode(indexName, lengthName, rangeName)") LoopNode l) {
        writeIndexNode.execute(frame, 1);
        writeRangeNode.execute(frame, range);
        writeLengthNode.execute(frame, length.executeInteger(range));

        l.executeLoop(frame);

        return RNull.instance;
    }

    @Specialization(guards = "isForeignArray(range, hasSize)")
    protected Object iterateForeignArray(VirtualFrame frame, Object range,
                    @Cached("createIndexName()") String indexName,
                    @Cached("createRangeName()") String rangeName,
                    @Cached("createLengthName()") String lengthName,
                    @Cached("createWriteVariable(indexName)") WriteVariableNode writeIndexNode,
                    @Cached("createWriteVariable(rangeName)") WriteVariableNode writeRangeNode,
                    @Cached("createWriteVariable(lengthName)") WriteVariableNode writeLengthNode,
                    @Cached("create()") RLengthNode length,
                    @Cached("createForIndexLoopNode(indexName, lengthName, rangeName)") LoopNode l,
                    @Cached("HAS_SIZE.createNode()") Node hasSize) {
        return iterate(frame, range, indexName, rangeName, lengthName, writeIndexNode, writeRangeNode, writeLengthNode, length, l);
    }

    @Specialization(guards = "isJavaIterable(range)")
    protected Object iterateForeignArray(VirtualFrame frame, Object range,
                    @Cached("createIteratorName()") String iteratorName,
                    @Cached("createWriteVariable(iteratorName)") WriteVariableNode writeIteratorNode,
                    @Cached("createForIterableLoopNode(iteratorName)") LoopNode l,
                    @Cached("READ.createNode()") Node readNode,
                    @Cached("createExecute(0).createNode()") Node executeNode) {

        TruffleObject iterator = getIterator((TruffleObject) range, readNode, executeNode);
        writeIteratorNode.execute(frame, iterator);

        l.executeLoop(frame);

        return RNull.instance;
    }

    @Specialization(guards = {"isForeignObject(range)", "!isForeignArray(range, hasSizeNode)", "!isJavaIterable(range)"})
    protected Object iterateKeys(VirtualFrame frame, Object range,
                    @Cached("createIndexName()") String indexName,
                    @Cached("createPositionName()") String positionName,
                    @Cached("createRangeName()") String rangeName,
                    @Cached("createKeysName()") String keysName,
                    @Cached("createLengthName()") String lengthName,
                    @Cached("createWriteVariable(indexName)") WriteVariableNode writeIndexNode,
                    @Cached("createWriteVariable(rangeName)") WriteVariableNode writeRangeNode,
                    @Cached("createWriteVariable(keysName)") WriteVariableNode writeKeysNode,
                    @Cached("createWriteVariable(lengthName)") WriteVariableNode writeLengthNode,
                    @Cached("createForKeysLoopNode(indexName, positionName, lengthName, rangeName, keysName)") LoopNode l,
                    @Cached("KEYS.createNode()") Node keysNode,
                    @Cached("HAS_SIZE.createNode()") Node hasSizeNode,
                    @Cached("GET_SIZE.createNode()") Node sizeNode) {
        try {
            TruffleObject keys = ForeignAccess.sendKeys(keysNode, (TruffleObject) range);
            writeIndexNode.execute(frame, 1);
            writeRangeNode.execute(frame, range);
            writeKeysNode.execute(frame, keys);
            writeLengthNode.execute(frame, getKeysLength(keys, sizeNode));

            l.executeLoop(frame);
        } catch (UnsupportedMessageException ex) {

        }
        return RNull.instance;
    }

    protected String createIndexName() {
        return AnonymousFrameVariable.create("FOR_INDEX");
    }

    protected String createPositionName() {
        return AnonymousFrameVariable.create("FOR_POSITION");
    }

    protected String createRangeName() {
        return AnonymousFrameVariable.create("FOR_RANGE");
    }

    protected String createLengthName() {
        return AnonymousFrameVariable.create("FOR_LENGTH");
    }

    protected String createIteratorName() {
        return AnonymousFrameVariable.create("FOR_ITERATOR");
    }

    protected String createKeysName() {
        return AnonymousFrameVariable.create("FOR_KEYS");
    }

    protected String createFrameVariable(String name) {
        return AnonymousFrameVariable.create(name);
    }

    protected WriteVariableNode createWriteVariable(String name) {
        return WriteVariableNode.createAnonymous(name, Mode.REGULAR, null);
    }

    protected LoopNode createForIterableLoopNode(String iteratorName) {
        return createLoopNode(new ForIterableRepeatingNode(this, var.getIdentifier(), RASTUtils.cloneNode(body), iteratorName));
    }

    protected LoopNode createForKeysLoopNode(String indexName, String positionName, String lengthName, String rangeName, String keysName) {
        return createLoopNode(new ForKeysRepeatingNode(this, var.getIdentifier(), RASTUtils.cloneNode(body), indexName, positionName, lengthName, rangeName, keysName));
    }

    protected LoopNode createForIndexLoopNode(String indexName, String lengthName, String rangeName) {
        return createLoopNode(new ForIndexRepeatingNode(this, var.getIdentifier(), RASTUtils.cloneNode(body), indexName, lengthName, rangeName));
    }

    private static LoopNode createLoopNode(AbstractRepeatingNode n) {
        return Truffle.getRuntime().createLoopNode(n);
    }

    private static TruffleObject getIterator(TruffleObject obj, Node readNode, Node executeNode) {
        assert ForeignArray2R.isJavaIterable(obj);
        try {
            TruffleObject itFun = (TruffleObject) ForeignAccess.sendRead(readNode, obj, "iterator");
            return (TruffleObject) ForeignAccess.sendExecute(executeNode, itFun);
        } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException | ArityException ex) {
            throw RInternalError.shouldNotReachHere(ex, "is java.lang.Iterable but could not access the iterator() function: " + obj);
        }
    }

    private static int getKeysLength(TruffleObject keys, Node sizeNode) {
        try {
            return (int) ForeignAccess.sendGetSize(sizeNode, keys);
        } catch (UnsupportedMessageException ex) {
            throw RInternalError.shouldNotReachHere(ex, "has keys but could not read keys size: " + keys);
        }
    }

    @Override
    public Object visibleExecute(VirtualFrame frame) {
        voidExecute(frame);
        if (visibility == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            visibility = insert(SetVisibilityNode.create());
        }
        visibility.execute(frame, false);
        return RNull.instance;
    }

    private abstract static class AbstractIndexRepeatingNode extends AbstractRepeatingNode {

        private final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();
        private final BranchProfile breakBlock = BranchProfile.create();
        private final BranchProfile nextBlock = BranchProfile.create();

        @Child private WriteVariableNode writeElementNode;
        @Child private WriteVariableNode writeIndexNode;
        @Child private LocalReadVariableNode readIndexNode;
        @Child private LocalReadVariableNode readLengthNode;

        // only used for toString
        private final ForNode forNode;

        AbstractIndexRepeatingNode(ForNode forNode, String var, RNode body, String indexName, String positionName, String lengthName, String rangeName) {
            super(body);
            this.forNode = forNode;
            this.writeElementNode = WriteVariableNode.createAnonymous(var, Mode.REGULAR, createPositionLoad(positionName, rangeName), false);

            this.readIndexNode = LocalReadVariableNode.create(indexName, true);
            this.readLengthNode = LocalReadVariableNode.create(lengthName, true);
            this.writeIndexNode = WriteVariableNode.createAnonymous(indexName, Mode.REGULAR, null);
            // pre-initialize the profile so that loop exits to not deoptimize
            conditionProfile.profile(false);
        }

        private static RNode createPositionLoad(String positionName, String rangeName) {
            RCodeBuilder<RSyntaxNode> builder = RContext.getASTBuilder();
            RSyntaxNode receiver = builder.lookup(RSyntaxNode.INTERNAL, rangeName, false);
            RSyntaxNode position = builder.lookup(RSyntaxNode.INTERNAL, positionName, false);
            RSyntaxNode access = builder.lookup(RSyntaxNode.INTERNAL, "[[", true);
            return builder.call(RSyntaxNode.INTERNAL, access, receiver, position).asRNode();
        }

        protected abstract void writePosition(VirtualFrame frame, int index);

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            int length;
            int index;
            try {
                length = readLengthNode.executeInteger(frame);
                index = readIndexNode.executeInteger(frame);
            } catch (UnexpectedResultException e1) {
                throw RInternalError.shouldNotReachHere("For index must be Integer.");
            }
            try {
                if (conditionProfile.profile(index <= length)) {
                    writePosition(frame, index);
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

    private static final class ForIndexRepeatingNode extends AbstractIndexRepeatingNode {

        ForIndexRepeatingNode(ForNode forNode, String var, RNode body, String indexName, String lengthName, String rangeName) {
            // index used as position
            super(forNode, var, body, indexName, indexName, lengthName, rangeName);
        }

        @Override
        protected void writePosition(VirtualFrame frame, int index) {
            // index already used as position
        }

    }

    private static final class ForKeysRepeatingNode extends AbstractIndexRepeatingNode {

        @Child private Node readForeignNode;
        @Child private LocalReadVariableNode readKeysNode;
        @Child private WriteVariableNode writePositionNode;

        ForKeysRepeatingNode(ForNode forNode, String var, RNode body, String indexName, String positionName, String lengthName, String rangeName, String keysName) {
            super(forNode, var, body, indexName, positionName, lengthName, rangeName);

            this.writePositionNode = WriteVariableNode.createAnonymous(positionName, Mode.REGULAR, null);
            this.readKeysNode = LocalReadVariableNode.create(keysName, true);
            readForeignNode = Message.READ.createNode();
        }

        @Override
        protected void writePosition(VirtualFrame frame, int index) {
            try {
                TruffleObject keys = (TruffleObject) readKeysNode.execute(frame);
                assert keys != null;
                Object position = ForeignAccess.sendRead(readForeignNode, keys, index - 1);
                writePositionNode.execute(frame, position);
            } catch (UnknownIdentifierException | UnsupportedMessageException ex) {
                throw RInternalError.shouldNotReachHere(ex, "could not read foreign key: " + (index - 1));
            }
        }
    }

    private static final class ForIterableRepeatingNode extends AbstractRepeatingNode {

        private final ConditionProfile conditionProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile breakBlock = BranchProfile.create();
        private final BranchProfile nextBlock = BranchProfile.create();

        @Child private WriteVariableNode writeElementNode;
        @Child private LocalReadVariableNode readIteratorNode;

        @Child private Node readForeignNode;
        @Child private Node executeForeignNode;

        // only used for toString
        private final ForNode forNode;

        ForIterableRepeatingNode(ForNode forNode, String var, RNode body, String iteratorName) {
            super(body);
            this.forNode = forNode;
            this.writeElementNode = WriteVariableNode.createAnonymous(var, Mode.REGULAR, createNextLoad(iteratorName), false);

            this.readIteratorNode = LocalReadVariableNode.create(iteratorName, true);

            this.executeForeignNode = Message.createExecute(0).createNode();
            this.readForeignNode = Message.READ.createNode();

            // pre-initialize the profile so that loop exits to not deoptimize
            conditionProfile.profile(false);
        }

        private static RNode createNextLoad(String iteratorName) {
            RCodeBuilder<RSyntaxNode> builder = RContext.getASTBuilder();
            RSyntaxNode receiver = builder.lookup(RSyntaxNode.INTERNAL, iteratorName, false);
            RSyntaxNode next = builder.lookup(RSyntaxNode.INTERNAL, "next", true);
            RSyntaxNode access = builder.lookup(RSyntaxNode.INTERNAL, "$", true);
            RSyntaxNode nextCall = builder.call(RSyntaxNode.INTERNAL, access, receiver, next);
            return builder.call(RSyntaxNode.INTERNAL, nextCall).asRNode();
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            try {
                TruffleObject iterator = (TruffleObject) readIteratorNode.execute(frame);
                assert iterator != null;

                if (conditionProfile.profile(hasNext(iterator))) {
                    writeElementNode.voidExecute(frame);
                    body.voidExecute(frame);
                    return true;
                }
                return false;
            } catch (BreakException e) {
                breakBlock.enter();
                return false;
            } catch (NextException e) {
                nextBlock.enter();
                return true;
            }
        }

        private boolean hasNext(TruffleObject iterator) {
            try {
                TruffleObject hasNextFun = (TruffleObject) ForeignAccess.sendRead(readForeignNode, iterator, "hasNext");
                return (boolean) ForeignAccess.sendExecute(executeForeignNode, hasNextFun);
            } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException | ArityException ex) {
                throw RInternalError.shouldNotReachHere(ex, "Could not retrieve hasNext function");
            }
        }

        @Override
        public String toString() {
            return forNode.toString();
        }

    }

    @Override
    public RSyntaxElement[] getSyntaxArguments() {
        return new RSyntaxElement[]{var, getRange().asRSyntaxNode(), body.asRSyntaxNode()};
    }

    @Override
    public ArgumentsSignature getSyntaxSignature() {
        return ArgumentsSignature.empty(3);
    }
}
