/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function.visibility;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;

@NodeInfo(cost = NodeCost.NONE)
public abstract class SetVisibilityNode extends Node {

    public abstract void execute(Frame frame, boolean value);

    public abstract void execute(VirtualFrame frame, RVisibility visibility);

    public abstract void executeAfterCall(VirtualFrame frame);

    public abstract void executeEndOfFunction(VirtualFrame frame);

    private SetVisibilityNode() {
    }

    public static SetVisibilityNode create() {
        if (FastROptions.IgnoreVisibility.getBooleanValue()) {
            return new SetVisibilityNoopNode();
        } else if (FastROptions.OptimizeVisibility.getBooleanValue()) {
            return new SetVisibilityOptimizedNode();
        } else {
            return new SetVisibilityDefaultNode();
        }
    }

    private static final class SetVisibilityNoopNode extends SetVisibilityNode {

        @Override
        public void execute(Frame frame, boolean value) {
            // nothing to do
        }

        @Override
        public void execute(VirtualFrame frame, RVisibility visibility) {
            // nothing to do
        }

        @Override
        public void executeAfterCall(VirtualFrame frame) {
            // nothing to do
        }

        @Override
        public void executeEndOfFunction(VirtualFrame frame) {
            // nothing to do
        }
    }

    private static final class SetVisibilityDefaultNode extends SetVisibilityNode {

        @Override
        public void execute(Frame frame, boolean value) {
            RContext.getInstance().setVisible(value);
        }

        @Override
        public void execute(VirtualFrame frame, RVisibility visibility) {
            if (visibility == RVisibility.ON) {
                execute(frame, true);
            } else if (visibility == RVisibility.OFF) {
                execute(frame, false);
            }
        }

        @Override
        public void executeAfterCall(VirtualFrame frame) {
            // nothing to do
        }

        @Override
        public void executeEndOfFunction(VirtualFrame frame) {
            // nothing to do
        }
    }

    /**
     * See {@link RFrameSlot#Visibility}.
     */
    private static final class SetVisibilityOptimizedNode extends SetVisibilityNode {

        @CompilationFinal private FrameSlot frameSlot;

        @Override
        public void execute(Frame frame, boolean value) {
            if (frameSlot == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                frameSlot = frame.getFrameDescriptor().findOrAddFrameSlot(RFrameSlot.Visibility, FrameSlotKind.Object);
            }
            frame.setObject(frameSlot, value);
        }

        @Override
        public void execute(VirtualFrame frame, RVisibility visibility) {
            if (visibility == RVisibility.ON) {
                execute(frame, true);
            } else if (visibility == RVisibility.OFF) {
                execute(frame, false);
            }
        }

        @Override
        public void executeAfterCall(VirtualFrame frame) {
            if (frameSlot == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                frameSlot = frame.getFrameDescriptor().findOrAddFrameSlot(RFrameSlot.Visibility, FrameSlotKind.Object);
            }
            frame.setObject(frameSlot, null);
        }

        @Override
        public void executeEndOfFunction(VirtualFrame frame) {
            if (frameSlot == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                frameSlot = frame.getFrameDescriptor().findOrAddFrameSlot(RFrameSlot.Visibility, FrameSlotKind.Object);
            }
            try {
                Object visibility = frame.getObject(frameSlot);
                if (visibility != null) {
                    RContext.getInstance().setVisible(visibility == Boolean.TRUE);
                }
            } catch (FrameSlotTypeException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    public static void executeAfterCallSlowPath(Frame frame) {
        frame.setObject(frame.getFrameDescriptor().findOrAddFrameSlot(RFrameSlot.Visibility, FrameSlotKind.Object), null);
    }
}
