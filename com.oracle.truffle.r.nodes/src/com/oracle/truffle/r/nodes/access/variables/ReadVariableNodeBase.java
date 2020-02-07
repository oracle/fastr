/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access.variables;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public class ReadVariableNodeBase extends RBaseNode {
    // Used as a bitset, access using setKind and hasKind static helper methods
    @CompilationFinal private byte seenValueKinds;

    private static byte setKind(byte set, FrameSlotKind kind) {
        return (byte) (set | (1 << kind.ordinal()));
    }

    private static boolean hasKind(byte set, FrameSlotKind kind) {
        return (set & (1 << kind.ordinal())) != 0;
    }

    protected final Object getValue(Frame variableFrame, FrameSlot frameSlot) {
        assert variableFrame.getFrameDescriptor().getSlots().contains(frameSlot) : frameSlot.getIdentifier();
        Object value = variableFrame.getValue(frameSlot);
        if (variableFrame.isObject(frameSlot)) {
            value = FrameSlotChangeMonitor.getValue(frameSlot, variableFrame);
            this.seenValueKinds = setKind(this.seenValueKinds, FrameSlotKind.Object);
        } else if (variableFrame.isByte(frameSlot)) {
            this.seenValueKinds = setKind(this.seenValueKinds, FrameSlotKind.Byte);
        } else if (variableFrame.isInt(frameSlot)) {
            this.seenValueKinds = setKind(this.seenValueKinds, FrameSlotKind.Int);
        } else if (variableFrame.isDouble(frameSlot)) {
            this.seenValueKinds = setKind(this.seenValueKinds, FrameSlotKind.Double);
        }
        return value;
    }

    final Object profiledGetValue(Frame variableFrame, FrameSlot frameSlot) {
        assert variableFrame.getFrameDescriptor().getSlots().contains(frameSlot) : frameSlot.getIdentifier();
        try {
            if (hasKind(this.seenValueKinds, FrameSlotKind.Object) && variableFrame.isObject(frameSlot)) {
                return FrameSlotChangeMonitor.getObject(frameSlot, variableFrame);
            } else if (hasKind(this.seenValueKinds, FrameSlotKind.Byte) && variableFrame.isByte(frameSlot)) {
                return variableFrame.getByte(frameSlot);
            } else if (hasKind(this.seenValueKinds, FrameSlotKind.Int) && variableFrame.isInt(frameSlot)) {
                return variableFrame.getInt(frameSlot);
            } else if (hasKind(this.seenValueKinds, FrameSlotKind.Double) && variableFrame.isDouble(frameSlot)) {
                return variableFrame.getDouble(frameSlot);
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                // re-profile to widen the set of expected types
                return getValue(variableFrame, frameSlot);
            }
        } catch (FrameSlotTypeException e) {
            CompilerDirectives.transferToInterpreter();
            throw new RInternalError(e, "unexpected frame slot type mismatch");
        }
    }
}
