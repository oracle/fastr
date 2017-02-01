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

import static com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor.findOrAddFrameSlot;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class FrameSlotNode extends RBaseNode {

    public abstract boolean hasValue(Frame frame);

    public abstract FrameSlot executeFrameSlot(Frame frame);

    private static Assumption getAssumption(FrameDescriptor frameDescriptor, Object identifier) {
        return frameDescriptor.getNotInFrameAssumption(identifier);
    }

    public static FrameSlotNode create(String name) {
        return create(name, false);
    }

    public static FrameSlotNode create(String name, boolean createIfAbsent) {
        return new UnresolvedFrameSlotNode(name, createIfAbsent);
    }

    public static FrameSlotNode createTemp(Object name, boolean createIfAbsent) {
        return new UnresolvedFrameSlotNode(name, createIfAbsent);
    }

    public static FrameSlotNode create(RFrameSlot slot, boolean createIfAbsent) {
        return new UnresolvedFrameSlotNode(slot, createIfAbsent);
    }

    public static FrameSlotNode createInitialized(FrameDescriptor frameDescriptor, Object identifier, boolean createIfAbsent) {
        FrameSlotNode newNode;
        FrameSlot frameSlot;
        if (createIfAbsent) {
            frameSlot = findOrAddFrameSlot(frameDescriptor, identifier, FrameSlotKind.Illegal);
        } else {
            frameSlot = frameDescriptor.findFrameSlot(identifier);
        }
        if (frameSlot != null) {
            newNode = new PresentFrameSlotNode(frameSlot);
        } else {
            newNode = new AbsentFrameSlotNode(getAssumption(frameDescriptor, identifier), identifier);
        }
        return newNode;
    }

    public static FrameSlotNode create(FrameSlot slot) {
        return new PresentFrameSlotNode(slot);
    }

    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    private static final class UnresolvedFrameSlotNode extends FrameSlotNode {

        private final Object identifier;
        private final boolean createIfAbsent;

        UnresolvedFrameSlotNode(Object identifier, boolean createIfAbsent) {
            this.identifier = identifier;
            this.createIfAbsent = createIfAbsent;
        }

        @Override
        public boolean hasValue(Frame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return resolveFrameSlot(frame).hasValue(frame);
        }

        @Override
        public FrameSlot executeFrameSlot(Frame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return resolveFrameSlot(frame).executeFrameSlot(frame);
        }

        private FrameSlotNode resolveFrameSlot(Frame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
            FrameSlotNode newNode = createInitialized(frameDescriptor, identifier, createIfAbsent);
            return replace(newNode);
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    private static final class AbsentFrameSlotNode extends FrameSlotNode {

        @CompilationFinal private Assumption assumption;
        private final Object identifier;

        AbsentFrameSlotNode(Assumption assumption, Object identifier) {
            this.assumption = assumption;
            this.identifier = identifier;
        }

        @Override
        public FrameSlot executeFrameSlot(Frame frame) {
            throw new UnsupportedOperationException();
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
                    assumption = getAssumption(frame.getFrameDescriptor(), identifier);
                }
            }
            return false;
        }
    }

    public static final class PresentFrameSlotNode extends FrameSlotNode {

        private final ConditionProfile isObjectProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isNullProfile = ConditionProfile.createBinaryProfile();
        private final FrameSlot frameSlot;

        private final ValueProfile frameTypeProfile = ValueProfile.createClassProfile();

        PresentFrameSlotNode(FrameSlot frameSlot) {
            this.frameSlot = frameSlot;
        }

        public FrameSlot getFrameSlot() {
            return frameSlot;
        }

        @Override
        public FrameSlot executeFrameSlot(Frame frame) {
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
