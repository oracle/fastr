/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.utilities.AssumedValue;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * Encapsulates the nodes that decrement reference count incremented when the argument node is
 * unwrapped.
 */
public final class PostProcessArgumentsNode extends RNode {

    @CompilationFinal(dimensions = 1) private final FrameSlot[] frameSlots;

    // stays the same during cloning
    private final AssumedValue<Integer> transArgsBitSet;

    private final ConditionProfile isNonNull = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isRefCountUpdateable = ConditionProfile.createBinaryProfile();

    private PostProcessArgumentsNode(int length) {
        this.frameSlots = new FrameSlot[Math.min(length, ArgumentStatePush.MAX_COUNTED_ARGS)];
        this.transArgsBitSet = new AssumedValue<>("PostProcessArgumentsNode.transArgsBitSet", 0);
    }

    public static PostProcessArgumentsNode create(int length) {
        return new PostProcessArgumentsNode(length);
    }

    public int getLength() {
        return frameSlots.length;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        int bits = transArgsBitSet.get();
        if (bits != 0) {
            for (int i = 0; i < frameSlots.length; i++) {
                int mask = 1 << i;
                if ((bits & mask) != 0) {
                    if (frameSlots[i] == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        synchronized (FrameSlotChangeMonitor.class) {
                            frameSlots[i] = frame.getFrameDescriptor().findOrAddFrameSlot(mask, FrameSlotKind.Object);
                        }
                    }
                    RShareable s;
                    try {
                        s = (RShareable) frame.getObject(frameSlots[i]);
                    } catch (FrameSlotTypeException e) {
                        throw RInternalError.shouldNotReachHere();
                    }
                    if (isNonNull.profile(s != null)) {
                        if (isRefCountUpdateable.profile(!s.isSharedPermanent())) {
                            s.decRefCount();
                        }
                    }
                }
            }
        }
        return RNull.instance;
    }

    public boolean updateBits(int index) {
        if (index < frameSlots.length) {
            int bits = transArgsBitSet.get();
            int newBits = bits | (1 << index);
            if (newBits != bits) {
                transArgsBitSet.set(newBits);
            }
            return true;
        }
        return false;
    }
}
