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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;

public final class TemporarySlotNode extends Node {

    private static final RFrameSlot[] defaultTempIdentifiers = new RFrameSlot[]{RFrameSlot.createTemp(true), RFrameSlot.createTemp(true), RFrameSlot.createTemp(true), RFrameSlot.createTemp(true)};

    @CompilationFinal private FrameSlot tempSlot;
    private int tempIdentifier;

    public FrameSlot initialize(VirtualFrame frame, Object value) {
        if (tempSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            tempSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frame.getFrameDescriptor(), defaultTempIdentifiers[0], FrameSlotKind.Object);
        }
        FrameSlot slot = tempSlot;
        try {
            if (frame.getObject(slot) != null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                // keep the complete loop in the slow path
                do {
                    tempIdentifier++;
                    RFrameSlot identifier = tempIdentifier < defaultTempIdentifiers.length ? defaultTempIdentifiers[tempIdentifier] : RFrameSlot.createTemp(true);
                    tempSlot = slot = FrameSlotChangeMonitor.findOrAddFrameSlot(frame.getFrameDescriptor(), identifier, FrameSlotKind.Object);
                    if (frame.getObject(slot) == null) {
                        break;
                    }
                } while (true);
            }
        } catch (FrameSlotTypeException e) {
            CompilerDirectives.transferToInterpreter();
            throw RInternalError.shouldNotReachHere();
        }
        FrameSlotChangeMonitor.setObject(frame, slot, value);
        return slot;
    }

    public static void cleanup(VirtualFrame frame, Object object, FrameSlot tempSlot) {
        try {
            assert FrameSlotChangeMonitor.getObject(tempSlot, frame) == object;
        } catch (FrameSlotTypeException e) {
            throw RInternalError.shouldNotReachHere();
        }
        FrameSlotChangeMonitor.setObject(frame, tempSlot, null);
    }
}
