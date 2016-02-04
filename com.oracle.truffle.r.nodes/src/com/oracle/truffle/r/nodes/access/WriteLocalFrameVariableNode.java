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
import com.oracle.truffle.api.profiles.*;
import com.oracle.truffle.r.nodes.access.WriteLocalFrameVariableNodeFactory.ResolvedWriteLocalFrameVariableNodeGen;
import com.oracle.truffle.r.nodes.access.WriteLocalFrameVariableNodeFactory.UnresolvedWriteLocalFrameVariableNodeGen;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.env.frame.*;
import com.oracle.truffle.r.runtime.nodes.*;

/**
 * {@link WriteLocalFrameVariableNode} captures a write to the "local", i.e., current, frame. There
 * are several clients capturing different "kinds" of writes to the frame, e.g. saving an argument,
 * a source-level write, a a write helper for another source node (e.g. {@code ForNode}.
 *
 * The state starts out a "unresolved" and transforms to "resolved".
 */
public abstract class WriteLocalFrameVariableNode extends BaseWriteVariableNode {

    public static WriteLocalFrameVariableNode create(String name, RNode rhs, Mode mode) {
        return UnresolvedWriteLocalFrameVariableNodeGen.create(rhs, name, mode);
    }

    public static WriteLocalFrameVariableNode createForRefCount(Object name) {
        return UnresolvedWriteLocalFrameVariableNodeGen.create(null, name, Mode.INVISIBLE);
    }

    @NodeField(name = "mode", type = Mode.class)
    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    public abstract static class UnresolvedWriteLocalFrameVariableNode extends WriteLocalFrameVariableNode {

        public abstract Mode getMode();

        @Specialization
        protected byte doLogical(VirtualFrame frame, byte value) {
            resolveAndSet(frame, value, FrameSlotKind.Byte);
            return value;
        }

        @Specialization
        protected int doInteger(VirtualFrame frame, int value) {
            resolveAndSet(frame, value, FrameSlotKind.Int);
            return value;
        }

        @Specialization
        protected double doDouble(VirtualFrame frame, double value) {
            resolveAndSet(frame, value, FrameSlotKind.Double);
            return value;
        }

        @Specialization
        protected Object doObject(VirtualFrame frame, Object value) {
            resolveAndSet(frame, value, FrameSlotKind.Object);
            return value;
        }

        private void resolveAndSet(VirtualFrame frame, Object value, FrameSlotKind initialKind) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            // it's slow path (unconditional replace) so toString() is fine as well
            if (getName().toString().isEmpty()) {
                throw RError.error(RError.NO_CALLER, RError.Message.ZERO_LENGTH_VARIABLE);
            }
            FrameSlot frameSlot = findOrAddFrameSlot(frame.getFrameDescriptor(), getName(), initialKind);
            replace(ResolvedWriteLocalFrameVariableNode.create(getRhs(), getName(), frameSlot, getMode())).execute(frame, value);
        }
    }

    @NodeFields({@NodeField(name = "frameSlot", type = FrameSlot.class), @NodeField(name = "mode", type = Mode.class)})
    public abstract static class ResolvedWriteLocalFrameVariableNode extends WriteLocalFrameVariableNode {

        private final ValueProfile storedObjectProfile = ValueProfile.createClassProfile();
        private final BranchProfile invalidateProfile = BranchProfile.create();

        public abstract Mode getMode();

        public static ResolvedWriteLocalFrameVariableNode create(RNode rhs, Object name, FrameSlot frameSlot, Mode mode) {
            return ResolvedWriteLocalFrameVariableNodeGen.create(rhs, name, frameSlot, mode);
        }

        @Specialization(guards = "isLogicalKind(frame, frameSlot)")
        protected byte doLogical(VirtualFrame frame, FrameSlot frameSlot, byte value) {
            FrameSlotChangeMonitor.setByteAndInvalidate(frame, frameSlot, value, false, invalidateProfile);
            return value;
        }

        @Specialization(guards = "isIntegerKind(frame, frameSlot)")
        protected int doInteger(VirtualFrame frame, FrameSlot frameSlot, int value) {
            FrameSlotChangeMonitor.setIntAndInvalidate(frame, frameSlot, value, false, invalidateProfile);
            return value;
        }

        @Specialization(guards = "isDoubleKind(frame, frameSlot)")
        protected double doDouble(VirtualFrame frame, FrameSlot frameSlot, double value) {
            FrameSlotChangeMonitor.setDoubleAndInvalidate(frame, frameSlot, value, false, invalidateProfile);
            return value;
        }

        @Specialization
        protected Object doObject(VirtualFrame frame, FrameSlot frameSlot, Object value) {
            Object newValue = shareObjectValue(frame, frameSlot, storedObjectProfile.profile(value), getMode(), false);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, frameSlot, newValue, false, invalidateProfile);
            return value;
        }

    }

}
