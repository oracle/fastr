/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;

@TypeSystemReference(RTypes.class)
public abstract class FrameSlotNode extends Node {

    public static enum InternalFrameSlot {
        /**
         * Stores the expression that needs to be executed when the function associated with the
         * frame terminates.
         */
        OnExit
    }

    public abstract boolean hasValue(Frame frame);

    public FrameSlot executeFrameSlot(@SuppressWarnings("unused") VirtualFrame frame) {
        throw new UnsupportedOperationException();
    }

    static FrameSlot findFrameSlot(Frame frame, Object identifier) {
        return frame.getFrameDescriptor().findFrameSlot(identifier);
    }

    static Assumption getAssumption(Frame frame, Object identifier) {
        return frame.getFrameDescriptor().getNotInFrameAssumption(identifier);
    }

    public static FrameSlotNode create(String name) {
        return create(name, false);
    }

    public static FrameSlotNode create(String name, boolean createIfAbsent) {
        return new UnresolvedFrameSlotNode(name, createIfAbsent);
    }

    public static FrameSlotNode create(InternalFrameSlot slot, boolean createIfAbsent) {
        return new UnresolvedFrameSlotNode(slot, createIfAbsent);
    }

    public static FrameSlotNode create(FrameSlot slot) {
        return new PresentFrameSlotNode(slot);
    }

    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    private static final class UnresolvedFrameSlotNode extends FrameSlotNode {

        private final Object identifier;
        private final boolean createIfAbsent;

        public UnresolvedFrameSlotNode(Object identifier, boolean createIfAbsent) {
            this.identifier = identifier;
            this.createIfAbsent = createIfAbsent;
        }

        @Override
        public boolean hasValue(Frame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return resolveFrameSlot(frame).hasValue(frame);
        }

        @Override
        public FrameSlot executeFrameSlot(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return resolveFrameSlot(frame).executeFrameSlot(frame);
        }

        private FrameSlotNode resolveFrameSlot(Frame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
            FrameSlotNode newNode;
            FrameSlot frameSlot;
            if (createIfAbsent) {
                frameSlot = findOrAddFrameSlot(frameDescriptor, identifier);
            } else {
                frameSlot = frameDescriptor.findFrameSlot(identifier);
            }
            if (frameSlot != null) {
                newNode = new PresentFrameSlotNode(frameSlot);
            } else {
                newNode = new AbsentFrameSlotNode(getAssumption(frame, identifier), identifier);
            }
            return replace(newNode);
        }
    }

    private static final class AbsentFrameSlotNode extends FrameSlotNode {

        @CompilationFinal private Assumption assumption;
        private final Object identifier;

        public AbsentFrameSlotNode(Assumption assumption, Object identifier) {
            this.assumption = assumption;
            this.identifier = identifier;
        }

        @Override
        public boolean hasValue(Frame frame) {
            try {
                assumption.check();
            } catch (InvalidAssumptionException e) {
                final FrameSlot frameSlot = frame.getFrameDescriptor().findFrameSlot(identifier);
                if (frameSlot != null) {
                    return replace(new PresentFrameSlotNode(frameSlot)).hasValue(frame);
                } else {
                    assumption = frame.getFrameDescriptor().getVersion();
                }
            }
            return false;
        }
    }

    private static final class PresentFrameSlotNode extends FrameSlotNode {

        private final ConditionProfile isObjectProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isNullProfile = ConditionProfile.createBinaryProfile();
        private final FrameSlot frameSlot;

        private final ValueProfile frameTypeProfile = ValueProfile.createClassProfile();

        public PresentFrameSlotNode(FrameSlot frameSlot) {
            this.frameSlot = frameSlot;
        }

        @Override
        public FrameSlot executeFrameSlot(VirtualFrame frame) {
            return frameSlot;
        }

        @Override
        public boolean hasValue(Frame frame) {
            try {
                Frame typedFrame = frameTypeProfile.profile(frame);
                return !isObjectProfile.profile(typedFrame.isObject(frameSlot)) || isNullProfile.profile(typedFrame.getObject(frameSlot) != null);
            } catch (FrameSlotTypeException e) {
                throw new IllegalStateException();
            }
        }
    }
}
