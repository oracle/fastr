/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * This is a node abstraction for the functionality defined in
 * {@link RMissingHelper#getMissingValue(Frame,String)}.
 */
public abstract class GetMissingValueNode extends RBaseNode {

    public static GetMissingValueNode create(String name) {
        return new UninitializedGetMissingValueNode(name);
    }

    public abstract Object execute(Frame frame);

    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    private static final class UninitializedGetMissingValueNode extends GetMissingValueNode {

        private final String name;

        private UninitializedGetMissingValueNode(String sym) {
            this.name = sym;
        }

        @Override
        public Object execute(Frame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            FrameSlot slot = frame.getFrameDescriptor().findFrameSlot(name);
            return replace(new ResolvedGetMissingValueNode(slot)).execute(frame);
        }
    }

    private static final class ResolvedGetMissingValueNode extends GetMissingValueNode {

        private final FrameSlot slot;
        private final ConditionProfile isObjectProfile = ConditionProfile.createBinaryProfile();

        private static final Object NON_PROMISE_OBJECT = new Object();

        private ResolvedGetMissingValueNode(FrameSlot slot) {
            this.slot = slot;
        }

        @Override
        public Object execute(Frame frame) {
            if (slot == null) {
                return null;
            }
            if (isObjectProfile.profile(frame.isObject(slot))) {
                try {
                    return FrameSlotChangeMonitor.getObject(slot, frame);
                } catch (FrameSlotTypeException e) {
                    return null;
                }
            } else {
                return NON_PROMISE_OBJECT;
            }
        }
    }
}
