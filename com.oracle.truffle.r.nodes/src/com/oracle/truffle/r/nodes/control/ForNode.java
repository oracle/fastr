/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode.Mode;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.AnonymousFrameVariable;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.interop.ConvertForeignObjectNode;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

@ImportStatic({RRuntime.class, ConvertForeignObjectNode.class})
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
                    @Cached("createIndexName()") @SuppressWarnings("unused") String indexName,
                    @Cached("createRangeName()") @SuppressWarnings("unused") String rangeName,
                    @Cached("createLengthName()") @SuppressWarnings("unused") String lengthName,
                    @Cached("createWriteVariable(indexName)") WriteVariableNode writeIndexNode,
                    @Cached("createWriteVariable(rangeName)") WriteVariableNode writeRangeNode,
                    @Cached("createWriteVariable(lengthName)") WriteVariableNode writeLengthNode,
                    @Cached("createWriteInitialElementNode()") WriteVariableNode writeInitialElementNode,
                    @Cached("create()") RLengthNode length,
                    @Cached("createForIndexLoopNode(indexName, lengthName, rangeName)") LoopNode l) {
        writeIndexNode.execute(frame, 1);
        writeRangeNode.execute(frame, range);
        writeLengthNode.execute(frame, length.executeInteger(range));
        writeInitialElementNode.execute(frame);

        l.execute(frame);

        return RNull.instance;
    }

    @Specialization(guards = "isForeignArray(range, rangeInterop)", limit = "getInteropLibraryCacheSize()")
    protected Object iterateForeignArray(VirtualFrame frame, Object range,
                    @Cached("createIndexName()") String indexName,
                    @Cached("createRangeName()") String rangeName,
                    @Cached("createLengthName()") String lengthName,
                    @Cached("createWriteVariable(indexName)") WriteVariableNode writeIndexNode,
                    @Cached("createWriteVariable(rangeName)") WriteVariableNode writeRangeNode,
                    @Cached("createWriteVariable(lengthName)") WriteVariableNode writeLengthNode,
                    @Cached("createWriteInitialElementNode()") WriteVariableNode writeInitialElementNode,
                    @Cached("create()") RLengthNode length,
                    @Cached("createForIndexLoopNode(indexName, lengthName, rangeName)") LoopNode l,
                    @CachedLibrary("range") @SuppressWarnings("unused") InteropLibrary rangeInterop) {
        return iterate(frame, range, indexName, rangeName, lengthName, writeIndexNode, writeRangeNode, writeLengthNode, writeInitialElementNode, length, l);
    }

    @Specialization(guards = {"isForeignObject(range)", "!isForeignArray(range, rangeInterop)", "rangeInterop.hasMembers(range)"}, limit = "getInteropLibraryCacheSize()")
    protected Object iterateMembers(VirtualFrame frame, Object range,
                    @Cached("createIndexName()") @SuppressWarnings("unused") String indexName,
                    @Cached("createPositionName()") @SuppressWarnings("unused") String positionName,
                    @Cached("createRangeName()") @SuppressWarnings("unused") String rangeName,
                    @Cached("createMembersName()") @SuppressWarnings("unused") String membersName,
                    @Cached("createLengthName()") @SuppressWarnings("unused") String lengthName,
                    @Cached("createWriteVariable(indexName)") WriteVariableNode writeIndexNode,
                    @Cached("createWriteVariable(rangeName)") WriteVariableNode writeRangeNode,
                    @Cached("createWriteVariable(membersName)") WriteVariableNode writeMembersNode,
                    @Cached("createWriteVariable(lengthName)") WriteVariableNode writeLengthNode,
                    @Cached("createWriteInitialElementNode()") WriteVariableNode writeInitialElementNode,
                    @CachedLibrary("range") InteropLibrary rangeInterop,
                    @CachedLibrary(limit = "getInteropLibraryCacheSize()") InteropLibrary membersInterop,
                    @Cached("createForMembersLoopNode(indexName, positionName, lengthName, rangeName, membersName)") LoopNode l) {
        try {
            writeIndexNode.execute(frame, 1);
            writeRangeNode.execute(frame, range);
            Object members = rangeInterop.getMembers(range);
            writeMembersNode.execute(frame, members);
            writeLengthNode.execute(frame, RRuntime.getForeignArraySize(members, membersInterop));
            writeInitialElementNode.execute(frame);
        } catch (UnsupportedMessageException ex) {
            throw RInternalError.shouldNotReachHere();
        }
        l.execute(frame);
        return RNull.instance;
    }

    @Specialization(guards = {"isForeignObject(range)", "!isForeignArray(range, rangeInterop)", "!rangeInterop.hasMembers(range)"}, limit = "getInteropLibraryCacheSize()")
    protected Object iterateMembers(@SuppressWarnings("unused") VirtualFrame frame, @SuppressWarnings("unused") Object range,
                    @SuppressWarnings("unused") @CachedLibrary("range") InteropLibrary rangeInterop) {
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

    protected String createMembersName() {
        return AnonymousFrameVariable.create("FOR_MEMBERS");
    }

    protected String createFrameVariable(String name) {
        return AnonymousFrameVariable.create(name);
    }

    protected WriteVariableNode createWriteInitialElementNode() {
        return WriteVariableNode.createAnonymous(var.getIdentifier(), Mode.REGULAR, (RNode) RContext.getASTBuilder().constant(RSyntaxNode.INTERNAL, RNull.instance), false);
    }

    protected WriteVariableNode createWriteVariable(String name) {
        return WriteVariableNode.createAnonymous(name, Mode.REGULAR, null);
    }

    protected LoopNode createForMembersLoopNode(String indexName, String positionName, String lengthName, String rangeName, String membersName) {
        return createLoopNode(new ForMembersRepeatingNode(this, var.getIdentifier(), RASTUtils.cloneNode(body), indexName, positionName, lengthName, rangeName, membersName));
    }

    protected LoopNode createForIndexLoopNode(String indexName, String lengthName, String rangeName) {
        return createLoopNode(new ForIndexRepeatingNode(this, var.getIdentifier(), RASTUtils.cloneNode(body), indexName, lengthName, rangeName));
    }

    private static LoopNode createLoopNode(AbstractRepeatingNode n) {
        return Truffle.getRuntime().createLoopNode(n);
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
        }

        private static RNode createPositionLoad(String positionName, String rangeName) {
            RCodeBuilder<RSyntaxNode> builder = RContext.getASTBuilder();
            RSyntaxNode receiver = builder.lookup(RSyntaxNode.INTERNAL, rangeName, false);
            RSyntaxNode position = builder.lookup(RSyntaxNode.INTERNAL, positionName, false);
            RSyntaxNode access = builder.lookup(RSyntaxNode.INTERNAL, "[[", true);
            return builder.call(RSyntaxNode.INTERNAL, access, receiver, position).asRNode();
        }

        protected abstract boolean writePosition(VirtualFrame frame, int index);

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
                if (index <= length) {
                    if (writePosition(frame, index)) {
                        writeElementNode.voidExecute(frame);
                        body.voidExecute(frame);
                    }
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
        protected boolean writePosition(VirtualFrame frame, int index) {
            // index already used as position
            return true;
        }

    }

    private static final class ForMembersRepeatingNode extends AbstractIndexRepeatingNode {

        @Child private LocalReadVariableNode readMembersNode;
        @Child private LocalReadVariableNode readRangeNode;
        @Child private WriteVariableNode writePositionNode;
        @Child private InteropLibrary membersInterop;
        @Child private InteropLibrary rangeInterop;
        @Child private InteropLibrary keyInterop;

        ForMembersRepeatingNode(ForNode forNode, String var, RNode body, String indexName, String positionName, String lengthName, String rangeName, String membersName) {
            super(forNode, var, body, indexName, positionName, lengthName, rangeName);

            this.writePositionNode = WriteVariableNode.createAnonymous(positionName, Mode.REGULAR, null);
            this.readMembersNode = LocalReadVariableNode.create(membersName, true);
            this.readRangeNode = LocalReadVariableNode.create(rangeName, true);
            this.membersInterop = InteropLibrary.getFactory().createDispatched(DSLConfig.getInteropLibraryCacheSize());
            this.rangeInterop = InteropLibrary.getFactory().createDispatched(1);
            this.keyInterop = InteropLibrary.getFactory().createDispatched(1);
        }

        @Override
        protected boolean writePosition(VirtualFrame frame, int index) {
            try {
                Object members = readMembersNode.execute(frame);
                assert members != null;
                Object range = readRangeNode.execute(frame);
                Object position = membersInterop.readArrayElement(members, index - 1);
                String key = keyInterop.asString(position);
                if (rangeInterop.isMemberReadable(range, key)) {
                    writePositionNode.execute(frame, key);
                    return true;
                } else {
                    return false;
                }
            } catch (UnsupportedMessageException | InvalidArrayIndexException ex) {
                CompilerDirectives.transferToInterpreter();
                throw RInternalError.shouldNotReachHere(ex, "could not read foreign member: " + (index - 1));
            }
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
