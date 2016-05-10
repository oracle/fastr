/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.access.RemoveAndAnswerNodeFactory.RemoveAndAnswerResolvedNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.VisibilityController;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * This node removes a slot from the current frame (i.e., sets it to {@code null} to allow fast-path
 * usage) and returns the slot's value. The node must be used with extreme caution as it does not
 * perform checking; it is to be used for internal purposes. A sample use case is a replacement.
 */
public abstract class RemoveAndAnswerNode extends RNode {

    public static RemoveAndAnswerNode create(Object name) {
        return new RemoveAndAnswerUninitializedNode(name.toString());
    }

    private static final class RemoveAndAnswerUninitializedNode extends RemoveAndAnswerNode {

        /**
         * The name of the variable that is to be removed and whose value is to be returned.
         */
        private final String name;

        RemoveAndAnswerUninitializedNode(String name) {
            this.name = name;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            FrameSlot fs = frame.getFrameDescriptor().findFrameSlot(name);
            return specialize(fs).execute(frame);
        }

        private RemoveAndAnswerNode specialize(FrameSlot fs) {
            if (fs == null) {
                RError.warning(RError.SHOW_CALLER, RError.Message.UNKNOWN_OBJECT, name);
            }
            return replace(RemoveAndAnswerResolvedNodeGen.create(fs));
        }
    }

    protected abstract static class RemoveAndAnswerResolvedNode extends RemoveAndAnswerNode implements VisibilityController {

        /**
         * The frame slot representing the variable that is to be removed and whose value is to be
         * returned.
         */
        private final FrameSlot slot;
        private final BranchProfile invalidateProfile = BranchProfile.create();

        protected RemoveAndAnswerResolvedNode(FrameSlot slot) {
            this.slot = slot;
        }

        protected boolean isObject(VirtualFrame frame) {
            return frame.isObject(slot);
        }

        protected boolean isInt(VirtualFrame frame) {
            return frame.isInt(slot);
        }

        protected boolean isDouble(VirtualFrame frame) {
            return frame.isDouble(slot);
        }

        protected boolean isByte(VirtualFrame frame) {
            return frame.isByte(slot);
        }

        @Specialization(guards = "isObject(frame)")
        protected Object doObject(VirtualFrame frame) {
            RContext.getInstance().setVisible(false);
            Object result;
            try {
                result = frame.getObject(slot);
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere();
            }

            // use null (not an R value) to represent "undefined"
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, slot, null, false, invalidateProfile);
            return result;
        }

        @Specialization(guards = "isInt(frame)")
        protected int doInt(VirtualFrame frame) {
            RContext.getInstance().setVisible(false);
            int result;
            try {
                result = frame.getInt(slot);
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere();
            }

            // use null (not an R value) to represent "undefined"
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, slot, null, false, invalidateProfile);
            return result;
        }

        @Specialization(guards = "isDouble(frame)")
        protected double doDouble(VirtualFrame frame) {
            RContext.getInstance().setVisible(false);
            double result;
            try {
                result = frame.getDouble(slot);
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere();
            }

            // use null (not an R value) to represent "undefined"
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, slot, null, false, invalidateProfile);
            return result;
        }

        @Specialization(guards = "isByte(frame)")
        protected byte doByte(VirtualFrame frame) {
            RContext.getInstance().setVisible(false);
            byte result;
            try {
                result = frame.getByte(slot);
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere();
            }

            // use null (not an R value) to represent "undefined"
            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, slot, null, false, invalidateProfile);
            return result;
        }
    }
}
