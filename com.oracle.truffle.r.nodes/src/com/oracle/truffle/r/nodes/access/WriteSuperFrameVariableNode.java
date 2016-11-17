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
package com.oracle.truffle.r.nodes.access;

import static com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor.findOrAddFrameSlot;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.WriteLocalFrameVariableNodeFactory.UnresolvedWriteLocalFrameVariableNodeGen;
import com.oracle.truffle.r.nodes.access.WriteSuperFrameVariableNodeFactory.ResolvedWriteSuperFrameVariableNodeGen;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * {@link WriteSuperFrameVariableNode} captures a write to a variable in some parent frame.
 *
 * The state starts out a "unresolved" and transforms to "resolved".
 */
abstract class WriteSuperFrameVariableNode extends BaseWriteVariableNode {

    static WriteVariableNode create(String name, RNode rhs, Mode mode) {
        return new UnresolvedWriteSuperFrameVariableNode(name, rhs, mode);
    }

    protected abstract void execute(VirtualFrame frame, Object value, MaterializedFrame enclosingFrame);

    @Override
    public final Object execute(VirtualFrame frame) {
        Object value = getRhs().execute(frame);
        execute(frame, value);
        return value;
    }

    @NodeChildren({@NodeChild(value = "enclosingFrame", type = AccessEnclosingFrameNode.class), @NodeChild(value = "frameSlotNode", type = FrameSlotNode.class)})
    protected abstract static class ResolvedWriteSuperFrameVariableNode extends WriteSuperFrameVariableNode {

        private final ValueProfile storedObjectProfile = ValueProfile.createClassProfile();
        private final BranchProfile invalidateProfile = BranchProfile.create();
        private final ValueProfile enclosingFrameProfile = ValueProfile.createClassProfile();

        private final Mode mode;

        public ResolvedWriteSuperFrameVariableNode(Mode mode) {
            this.mode = mode;
        }

        protected abstract FrameSlotNode getFrameSlotNode();

        @Specialization(guards = "isLogicalKind(enclosingFrame, frameSlot)")
        protected void doLogical(byte value, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            FrameSlotChangeMonitor.setByteAndInvalidate(enclosingFrameProfile.profile(enclosingFrame), frameSlot, value, true, invalidateProfile);
        }

        @Specialization(guards = "isIntegerKind(enclosingFrame, frameSlot)")
        protected void doInteger(int value, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            FrameSlotChangeMonitor.setIntAndInvalidate(enclosingFrameProfile.profile(enclosingFrame), frameSlot, value, true, invalidateProfile);
        }

        @Specialization(guards = "isDoubleKind(enclosingFrame, frameSlot)")
        protected void doDouble(double value, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            FrameSlotChangeMonitor.setDoubleAndInvalidate(enclosingFrameProfile.profile(enclosingFrame), frameSlot, value, true, invalidateProfile);
        }

        @Specialization
        protected void doObject(Object value, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
            MaterializedFrame profiledFrame = enclosingFrameProfile.profile(enclosingFrame);
            Object newValue = shareObjectValue(profiledFrame, frameSlot, storedObjectProfile.profile(value), mode, true);
            FrameSlotChangeMonitor.setObjectAndInvalidate(profiledFrame, frameSlot, newValue, true, invalidateProfile);
        }
    }

    private static final class UnresolvedWriteSuperFrameVariableNode extends WriteSuperFrameVariableNode {

        @Child private RNode rhs;

        private final String name;
        private final Mode mode;

        UnresolvedWriteSuperFrameVariableNode(String name, RNode rhs, Mode mode) {
            this.rhs = rhs;
            this.name = name;
            this.mode = mode;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public RNode getRhs() {
            return rhs;
        }

        @Override
        public void execute(VirtualFrame frame, Object value, MaterializedFrame enclosingFrame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (name.isEmpty()) {
                throw RError.error(RError.NO_CALLER, RError.Message.ZERO_LENGTH_VARIABLE);
            }
            final WriteSuperFrameVariableNode writeNode;
            if (REnvironment.isGlobalEnvFrame(enclosingFrame)) {
                /*
                 * we've reached the global scope, do unconditional write. if this is the first node
                 * in the chain, needs the rhs and enclosingFrame nodes
                 */
                AccessEnclosingFrameNode enclosingFrameNode = RArguments.getEnclosingFrame(frame) == enclosingFrame ? new AccessEnclosingFrameNode() : null;
                writeNode = ResolvedWriteSuperFrameVariableNodeGen.create(mode, rhs, enclosingFrameNode,
                                FrameSlotNode.create(findOrAddFrameSlot(enclosingFrame.getFrameDescriptor(), name, FrameSlotKind.Illegal)), name);
            } else {
                ResolvedWriteSuperFrameVariableNode actualWriteNode = ResolvedWriteSuperFrameVariableNodeGen.create(mode, null, null, FrameSlotNode.create(name), name);
                writeNode = new WriteSuperFrameVariableConditionalNode(actualWriteNode, new UnresolvedWriteSuperFrameVariableNode(name, null, mode), rhs);
            }
            replace(writeNode).execute(frame, value, enclosingFrame);
        }

        @Override
        public void execute(VirtualFrame frame, Object value) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            MaterializedFrame enclosingFrame = RArguments.getEnclosingFrame(frame);
            if (enclosingFrame != null) {
                execute(frame, value, enclosingFrame);
            } else {
                // we're in global scope, do a local write instead
                replace(UnresolvedWriteLocalFrameVariableNodeGen.create(rhs, name, mode)).execute(frame, value);
            }
        }
    }

    private static final class WriteSuperFrameVariableConditionalNode extends WriteSuperFrameVariableNode {

        @Child private ResolvedWriteSuperFrameVariableNode writeNode;
        @Child private WriteSuperFrameVariableNode nextNode;
        @Child private RNode rhs;

        private final ValueProfile enclosingFrameProfile = ValueProfile.createClassProfile();
        private final ConditionProfile hasValueProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile nullSuperFrameProfile = ConditionProfile.createBinaryProfile();

        WriteSuperFrameVariableConditionalNode(ResolvedWriteSuperFrameVariableNode writeNode, WriteSuperFrameVariableNode nextNode, RNode rhs) {
            this.writeNode = writeNode;
            this.nextNode = nextNode;
            this.rhs = rhs;
        }

        @Override
        public Object getName() {
            return writeNode.getName();
        }

        @Override
        public RNode getRhs() {
            return rhs;
        }

        @Override
        public void execute(VirtualFrame frame, Object value, MaterializedFrame enclosingFrame) {
            MaterializedFrame profiledEnclosingFrame = enclosingFrameProfile.profile(enclosingFrame);
            if (hasValueProfile.profile(writeNode.getFrameSlotNode().hasValue(profiledEnclosingFrame))) {
                writeNode.execute(frame, value, profiledEnclosingFrame);
            } else {
                MaterializedFrame superFrame = RArguments.getEnclosingFrame(profiledEnclosingFrame);
                if (nullSuperFrameProfile.profile(superFrame == null)) {
                    // Might be the case if "{ x <<- 42 }": This is in globalEnv!
                    superFrame = REnvironment.globalEnv().getFrame();
                }
                nextNode.execute(frame, value, superFrame);
            }
        }

        @Override
        public void execute(VirtualFrame frame, Object value) {
            assert RArguments.getEnclosingFrame(frame) != null;
            execute(frame, value, RArguments.getEnclosingFrame(frame));
        }
    }
}
