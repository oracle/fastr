/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.env.frame.FrameIndex;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;

public final class TemporarySlotNode extends Node {
    @CompilationFinal private int tempSlotIdx = FrameIndex.UNITIALIZED_INDEX;
    private int tempIdentifier;

    /**
     * Searches for an empty temporary slot in the given frame, and puts the given {@code value} there.
     * @param value Value to put in a temporary frame slot.
     * @return Index into auxiliary frame slot.
     */
    public int initialize(VirtualFrame frame, Object value) {
        FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        if (FrameIndex.isUninitializedIndex(tempSlotIdx)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            tempSlotIdx = FrameSlotChangeMonitor.findOrAddAuxiliaryFrameSlotNew(frameDescriptor, RFrameSlot.getTemp(tempIdentifier));
        }

        try {
            // If the frame slot is not empty, we have to find another empty temporary frame slot.
            if (FrameSlotChangeMonitor.getObjectNew(frame, tempSlotIdx) != null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                do {
                    tempIdentifier++;
                    RFrameSlot identifier = RFrameSlot.getTemp(tempIdentifier);
                    tempSlotIdx = FrameSlotChangeMonitor.findOrAddAuxiliaryFrameSlotNew(frameDescriptor, identifier);
                } while (FrameSlotChangeMonitor.getObjectNew(frame, tempSlotIdx) != null);
            }
        } catch (FrameSlotTypeException e) {
            CompilerDirectives.transferToInterpreter();
            throw RInternalError.shouldNotReachHere();
        }
        FrameSlotChangeMonitor.setObjectNew(frame, tempSlotIdx, value);
        return tempSlotIdx;
    }

    public static void cleanup(VirtualFrame frame, Object object, int tempSlotIdx) {
        try {
            assert FrameSlotChangeMonitor.getObjectNew(frame, tempSlotIdx) == object;
        } catch (FrameSlotTypeException e) {
            throw RInternalError.shouldNotReachHere();
        }
        FrameSlotChangeMonitor.setObjectNew(frame, tempSlotIdx, null);
    }
}
