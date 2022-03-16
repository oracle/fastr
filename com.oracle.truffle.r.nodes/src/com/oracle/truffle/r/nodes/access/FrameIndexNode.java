/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
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
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.env.frame.FrameIndex;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class FrameIndexNode extends RBaseNode {

    public abstract int executeFrameIndex(Frame frame);

    public abstract boolean hasValue(Frame frame);

    public static FrameIndexNode create(String name) {
        return create(name, false);
    }

    public static FrameIndexNode create(Object identifier, boolean createIfAbsent) {
        return new UnresolvedFrameIndexNode(identifier, createIfAbsent);
    }

    public static FrameIndexNode createInitialized(FrameDescriptor frameDescriptor, Object identifier, boolean createIfAbsent) {
        return createInitializedInternal(frameDescriptor, identifier, createIfAbsent);
    }

    public static FrameIndexNode createInitializedWithIndex(FrameDescriptor frameDescriptor, int frameIndex) {
        assert FrameIndex.isInitializedIndex(frameIndex);
        assert FrameSlotChangeMonitor.containsIndex(frameDescriptor, frameIndex);
        return new PresentFrameIndexNode(frameIndex);
    }

    private static FrameIndexNode createInitializedInternal(FrameDescriptor frameDescriptor, Object identifier, boolean createIfAbsent) {
        int frameIndex;
        if (createIfAbsent) {
            frameIndex = FrameSlotChangeMonitor.findOrAddAuxiliaryFrameSlot(frameDescriptor, identifier);
        } else {
            frameIndex = FrameSlotChangeMonitor.getIndexOfIdentifier(frameDescriptor, identifier);
        }
        if (FrameIndex.isUninitializedIndex(frameIndex)) {
            var notInFrameAssumption = FrameSlotChangeMonitor.getNotInFrameAssumption(frameDescriptor, identifier);
            return new AbsentFrameIndexNode(frameDescriptor, identifier, notInFrameAssumption);
        } else {
            return new PresentFrameIndexNode(frameIndex);
        }
    }


    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    private static final class UnresolvedFrameIndexNode extends FrameIndexNode {

        private final Object identifier;
        private final boolean createIfAbsent;

        UnresolvedFrameIndexNode(Object identifier, boolean createIfAbsent) {
            this.identifier = identifier;
            this.createIfAbsent = createIfAbsent;
        }

        @Override
        public boolean hasValue(Frame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return resolveFrameIndex(frame).hasValue(frame);
        }

        @Override
        public int executeFrameIndex(Frame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return resolveFrameIndex(frame).executeFrameIndex(frame);
        }

        private FrameIndexNode resolveFrameIndex(Frame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
            FrameIndexNode newNode = createInitializedInternal(frameDescriptor, identifier, createIfAbsent);
            return replace(newNode);
        }
    }

    private static final class PresentFrameIndexNode extends FrameIndexNode {
        private final ConditionProfile isNullProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isObjectProfile = ConditionProfile.createBinaryProfile();
        private final ValueProfile frameTypeProfile = ValueProfile.createClassProfile();
        private final int frameIndex;

        public PresentFrameIndexNode(int frameIndex) {
            this.frameIndex = frameIndex;
        }

        @Override
        public int executeFrameIndex(Frame frame) {
            return frameIndex;
        }

        @Override
        public boolean hasValue(Frame frame) {
            Frame typedFrame = frameTypeProfile.profile(frame);
            FrameSlotKind slotKind = FrameSlotChangeMonitor.getFrameSlotKind(typedFrame.getFrameDescriptor(), frameIndex);
            if (!(isObjectProfile.profile(slotKind == FrameSlotKind.Object))) {
                return false;
            } else {
                Object value;
                try {
                    value = FrameSlotChangeMonitor.getObject(typedFrame, frameIndex);
                } catch (FrameSlotTypeException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
                return isNullProfile.profile(value != null);
            }
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    private static final class AbsentFrameIndexNode extends FrameIndexNode {
        private final FrameDescriptor frameDescriptor;
        private final Object identifier;
        @CompilationFinal private Assumption notInFrameAssumption;

        AbsentFrameIndexNode(FrameDescriptor frameDescriptor, Object identifier, Assumption notInFrameAssumption) {
            this.frameDescriptor = frameDescriptor;
            this.identifier = identifier;
            this.notInFrameAssumption = notInFrameAssumption;
        }

        @Override
        public int executeFrameIndex(Frame frame) {
            throw CompilerDirectives.shouldNotReachHere();
        }

        @Override
        public boolean hasValue(Frame frame) {
            if (notInFrameAssumption.isValid()) {
                return false;
            } else {
                int frameIndex = FrameSlotChangeMonitor.getIndexOfIdentifier(frameDescriptor, identifier);
                if (FrameIndex.isUninitializedIndex(frameIndex)) {
                    notInFrameAssumption = FrameSlotChangeMonitor.getNotInFrameAssumption(frameDescriptor, identifier);
                    return false;
                } else {
                    return replace(new PresentFrameIndexNode(frameIndex)).hasValue(frame);
                }
            }
        }
    }

}
