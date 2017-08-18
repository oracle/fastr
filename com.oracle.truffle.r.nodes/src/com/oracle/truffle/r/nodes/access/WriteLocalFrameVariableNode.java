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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * {@link WriteLocalFrameVariableNode} captures a write to the "local", i.e., current, frame. There
 * are several clients capturing different "kinds" of writes to the frame, e.g. saving an argument,
 * a source-level write, a a write helper for another source node (e.g. {@code ForNode}).
 */
@ImportStatic(FrameSlotKind.class)
public abstract class WriteLocalFrameVariableNode extends BaseWriteVariableNode {

    public static WriteLocalFrameVariableNode create(String name, Mode mode, RNode rhs) {
        return WriteLocalFrameVariableNodeGen.create(name, mode, rhs);
    }

    public static WriteLocalFrameVariableNode createForRefCount(String name) {
        return WriteLocalFrameVariableNodeGen.create(name, Mode.INVISIBLE, null);
    }

    private final Mode mode;

    private final ValueProfile storedObjectProfile = ValueProfile.createClassProfile();
    private final BranchProfile invalidateProfile = BranchProfile.create();
    private final ConditionProfile isActiveBindingProfile = ConditionProfile.createBinaryProfile();
    @CompilationFinal private Assumption containsNoActiveBinding;

    protected WriteLocalFrameVariableNode(String name, Mode mode) {
        super(name);
        this.mode = mode;
    }

    protected FrameSlot findOrAddFrameSlot(VirtualFrame frame, FrameSlotKind initialKind) {
        return FrameSlotChangeMonitor.findOrAddFrameSlot(frame.getFrameDescriptor(), getName(), initialKind);
    }

    @Specialization(guards = "isLogicalKind(frame, frameSlot)")
    protected byte doLogical(VirtualFrame frame, byte value,
                    @Cached("findOrAddFrameSlot(frame, Byte)") FrameSlot frameSlot) {
        FrameSlotChangeMonitor.setByteAndInvalidate(frame, frameSlot, value, false, invalidateProfile);
        return value;
    }

    @Specialization(guards = "isIntegerKind(frame, frameSlot)")
    protected int doInteger(VirtualFrame frame, int value,
                    @Cached("findOrAddFrameSlot(frame, Int)") FrameSlot frameSlot) {
        FrameSlotChangeMonitor.setIntAndInvalidate(frame, frameSlot, value, false, invalidateProfile);
        return value;
    }

    @Specialization(guards = "isDoubleKind(frame, frameSlot)")
    protected double doDouble(VirtualFrame frame, double value,
                    @Cached("findOrAddFrameSlot(frame, Double)") FrameSlot frameSlot) {
        FrameSlotChangeMonitor.setDoubleAndInvalidate(frame, frameSlot, value, false, invalidateProfile);
        return value;
    }

    @Specialization
    protected Object doObject(VirtualFrame frame, Object value,
                    @Cached("findOrAddFrameSlot(frame, Object)") FrameSlot frameSlot) {
        if (containsNoActiveBinding == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            containsNoActiveBinding = FrameSlotChangeMonitor.getContainsNoActiveBindingAssumption(frame.getFrameDescriptor());
        }
        if (containsNoActiveBinding.isValid()) {
            Object newValue = shareObjectValue(frame, frameSlot, storedObjectProfile.profile(value), mode, false);
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, frameSlot, newValue, false, invalidateProfile);
        } else {
            // it's a local variable lookup; so use 'frame' for both, executing and looking up
            return handleActiveBinding(frame, frame, value, frameSlot, invalidateProfile, isActiveBindingProfile, storedObjectProfile, mode);
        }
        return value;
    }
}
