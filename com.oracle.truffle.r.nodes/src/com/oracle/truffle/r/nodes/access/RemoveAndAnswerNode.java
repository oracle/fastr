/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.parser.ast.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.env.frame.*;

/**
 * This node removes a slot from the current frame (i.e., sets it to {@code null} to allow fast-path
 * usage) and returns the slot's value. The node must be used with extreme caution as it does not
 * perform checking; it is to be used for internal purposes. A sample use case is a
 * {@linkplain RTruffleVisitor#visit(Replacement) replacement}.
 */
public abstract class RemoveAndAnswerNode extends RNode {

    public static RemoveAndAnswerNode create(String name) {
        return new RemoveAndAnswerUninitializedNode(name);
    }

    public static RemoveAndAnswerNode create(Object name) {
        return new RemoveAndAnswerUninitializedNode(name.toString());
    }

    protected static final class RemoveAndAnswerUninitializedNode extends RemoveAndAnswerNode {

        /**
         * The name of the variable that is to be removed and whose value is to be returned.
         */
        protected final String name;

        protected RemoveAndAnswerUninitializedNode(String name) {
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
                RError.warning(this.getEncapsulatingSourceSection(), RError.Message.UNKNOWN_OBJECT, name);
            }
            return replace(new RemoveAndAnswerResolvedNode(fs));
        }
    }

    protected static final class RemoveAndAnswerResolvedNode extends RemoveAndAnswerNode implements VisibilityController {

        @Override
        public boolean getVisibility() {
            return false;
        }

        /**
         * The frame slot representing the variable that is to be removed and whose value is to be
         * returned.
         */
        private final FrameSlot slot;
        private final BranchProfile invalidateProfile = new BranchProfile();

        protected RemoveAndAnswerResolvedNode(FrameSlot slot) {
            this.slot = slot;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            controlVisibility();
            Object result = frame.getValue(slot);
            frame.setObject(slot, null); // use null (not an R value) to represent "undefined"
            FrameSlotChangeMonitor.checkAndInvalidate(frame, slot, invalidateProfile);
            return result;
        }

    }

}
