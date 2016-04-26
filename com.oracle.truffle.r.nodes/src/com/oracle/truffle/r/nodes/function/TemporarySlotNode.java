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
package com.oracle.truffle.r.nodes.function;

import java.util.function.Consumer;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.access.FrameSlotNode;
import com.oracle.truffle.r.runtime.RInternalError;

public final class TemporarySlotNode extends Node {

    private static final Object[] defaultTempIdentifiers = new Object[]{new Object(), new Object(), new Object(), new Object()};

    @Child private FrameSlotNode tempSlot;
    private int tempIdentifier;

    public FrameSlot initialize(VirtualFrame frame, Object value, Consumer<Object> initializer) {
        if (tempSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            tempSlot = insert(FrameSlotNode.createInitialized(frame.getFrameDescriptor(), defaultTempIdentifiers[0], true));
            initializer.accept(defaultTempIdentifiers[0]);
        }
        FrameSlot slot = tempSlot.executeFrameSlot(frame);
        try {
            if (frame.isObject(slot) && frame.getObject(slot) != null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                // keep the complete loop in the slow path
                do {
                    tempIdentifier++;
                    Object identifier = tempIdentifier < defaultTempIdentifiers.length ? defaultTempIdentifiers[tempIdentifier] : new Object();
                    tempSlot.replace(FrameSlotNode.createInitialized(frame.getFrameDescriptor(), identifier, true));
                    initializer.accept(identifier);
                    slot = tempSlot.executeFrameSlot(frame);
                } while (frame.isObject(slot) && frame.getObject(slot) != null);
            }
        } catch (FrameSlotTypeException e) {
            throw RInternalError.shouldNotReachHere();
        }
        frame.setObject(slot, value);
        return slot;
    }

    @SuppressWarnings("static-method")
    public void cleanup(VirtualFrame frame, FrameSlot slot) {
        frame.setObject(slot, null);
    }
}
