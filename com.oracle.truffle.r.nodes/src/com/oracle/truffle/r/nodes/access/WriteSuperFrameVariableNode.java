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

import static com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.nodes.Node.*;
import com.oracle.truffle.api.profiles.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.WriteVariableNode.Mode;
import com.oracle.truffle.r.nodes.access.WriteLocalFrameVariableNodeFactory.UnresolvedWriteLocalFrameVariableNodeGen;
import com.oracle.truffle.r.nodes.access.BaseWriteVariableNode.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.frame.*;
import com.oracle.truffle.r.runtime.nodes.*;

/**
 * {@link WriteSuperFrameVariableNode} captures a write to a variable in some parent frame.
 *
 * The state starts out a "unresolved" and transforms to "resolved".
 */
@SuppressWarnings("unused")
@NodeChildren({@NodeChild(value = "enclosingFrame", type = AccessEnclosingFrameNode.class), @NodeChild(value = "frameSlotNode", type = FrameSlotNode.class)})
@NodeField(name = "mode", type = Mode.class)
public abstract class WriteSuperFrameVariableNode extends WriteSuperFrameVariableNodeHelper {
    private final ValueProfile storedObjectProfile = ValueProfile.createClassProfile();
    private final BranchProfile invalidateProfile = BranchProfile.create();
    private final ValueProfile enclosingFrameProfile = ValueProfile.createClassProfile();

    protected abstract FrameSlotNode getFrameSlotNode();

    public abstract Mode getMode();

    public static WriteVariableNode create(String name, RNode rhs, Mode mode) {
        return new UnresolvedWriteSuperFrameVariableNode(name, rhs, mode);
    }

    @Specialization(guards = "isLogicalKind(frame, frameSlot)")
    protected void doLogical(VirtualFrame frame, byte value, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
        FrameSlotChangeMonitor.setByteAndInvalidate(enclosingFrameProfile.profile(enclosingFrame), frameSlot, value, true, invalidateProfile);
    }

    @Specialization(guards = "isIntegerKind(frame, frameSlot)")
    protected void doInteger(VirtualFrame frame, int value, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
        FrameSlotChangeMonitor.setIntAndInvalidate(enclosingFrameProfile.profile(enclosingFrame), frameSlot, value, true, invalidateProfile);
    }

    @Specialization(guards = "isDoubleKind(frame, frameSlot)")
    protected void doDouble(VirtualFrame frame, double value, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
        FrameSlotChangeMonitor.setDoubleAndInvalidate(enclosingFrameProfile.profile(enclosingFrame), frameSlot, value, true, invalidateProfile);
    }

    @Specialization
    protected void doObject(VirtualFrame frame, Object value, MaterializedFrame enclosingFrame, FrameSlot frameSlot) {
        MaterializedFrame profiledFrame = enclosingFrameProfile.profile(enclosingFrame);
        Object newValue = shareObjectValue(profiledFrame, frameSlot, storedObjectProfile.profile(value), getMode(), true);
        FrameSlotChangeMonitor.setObjectAndInvalidate(profiledFrame, frameSlot, newValue, true, invalidateProfile);
    }

    public static class UnresolvedWriteSuperFrameVariableNode extends WriteSuperFrameVariableNodeHelper {

        @Child private RNode rhs;
        private final String symbol;
        private final BaseWriteVariableNode.Mode mode;

        public UnresolvedWriteSuperFrameVariableNode(String symbol, RNode rhs, BaseWriteVariableNode.Mode mode) {
            this.rhs = rhs;
            this.symbol = symbol;
            this.mode = mode;
        }

        @Override
        public String getName() {
            return symbol;
        }

        @Override
        public RNode getRhs() {
            return rhs;
        }

        @Override
        public void execute(VirtualFrame frame, Object value, MaterializedFrame enclosingFrame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (getName().isEmpty()) {
                throw RError.error(RError.NO_CALLER, RError.Message.ZERO_LENGTH_VARIABLE);
            }
            final WriteSuperFrameVariableNodeHelper writeNode;
            if (REnvironment.isGlobalEnvFrame(enclosingFrame)) {
                /*
                 * we've reached the global scope, do unconditional write. if this is the first node
                 * in the chain, needs the rhs and enclosingFrame nodes
                 */
                AccessEnclosingFrameNode enclosingFrameNode = RArguments.getEnclosingFrame(frame) == enclosingFrame ? new AccessEnclosingFrameNode() : null;
                writeNode = WriteSuperFrameVariableNodeGen.create(getRhs(), enclosingFrameNode,
                                FrameSlotNode.create(findOrAddFrameSlot(enclosingFrame.getFrameDescriptor(), symbol, FrameSlotKind.Illegal)), getName(), mode);
            } else {
                WriteSuperFrameVariableNode actualWriteNode = WriteSuperFrameVariableNodeGen.create(null, null, FrameSlotNode.create(symbol), this.getName(), mode);
                writeNode = new WriteSuperFrameVariableConditionalNode(actualWriteNode, new UnresolvedWriteSuperFrameVariableNode(symbol, null, mode), getRhs());
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
                replace(UnresolvedWriteLocalFrameVariableNodeGen.create(getRhs(), symbol, mode)).execute(frame, value);
            }
        }

    }

    public static class WriteSuperFrameVariableConditionalNode extends WriteSuperFrameVariableNodeHelper {

        @Child private WriteSuperFrameVariableNode writeNode;
        @Child private WriteSuperFrameVariableNodeHelper nextNode;
        @Child private RNode rhs;

        private final ValueProfile enclosingFrameProfile = ValueProfile.createClassProfile();
        private final ConditionProfile hasValueProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile nullSuperFrameProfile = ConditionProfile.createBinaryProfile();

        WriteSuperFrameVariableConditionalNode(WriteSuperFrameVariableNode writeNode, WriteSuperFrameVariableNodeHelper nextNode, RNode rhs) {
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
