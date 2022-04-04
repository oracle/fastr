/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.env.frame.FrameIndex;
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

    protected final Object getValue(Frame variableFrame, int frameIndex) {
        assert FrameSlotChangeMonitor.containsIndex(variableFrame, frameIndex);
        Object value = FrameSlotChangeMonitor.getValue(variableFrame, frameIndex);
        if (isObjectSlot(variableFrame, frameIndex)) {
            seenValueKinds = setKind(seenValueKinds, FrameSlotKind.Object);
        } else if (isByteSlot(variableFrame, frameIndex)) {
            seenValueKinds = setKind(seenValueKinds, FrameSlotKind.Byte);
        } else if (isIntSlot(variableFrame, frameIndex)) {
            seenValueKinds = setKind(seenValueKinds, FrameSlotKind.Int);
        } else if (isDoubleSlot(variableFrame, frameIndex)) {
            seenValueKinds = setKind(seenValueKinds, FrameSlotKind.Double);
        }
        return value;
    }

    final Object profiledGetValue(Frame variableFrame, int frameIndex) {
        assert FrameSlotChangeMonitor.containsIndex(variableFrame, frameIndex);
        try {
            if (hasKind(this.seenValueKinds, FrameSlotKind.Object) && isObjectSlot(variableFrame, frameIndex)) {
                return FrameSlotChangeMonitor.getObject(variableFrame, frameIndex);
            } else if (hasKind(this.seenValueKinds, FrameSlotKind.Byte) && isByteSlot(variableFrame, frameIndex)) {
                return FrameSlotChangeMonitor.getByte(variableFrame, frameIndex);
            } else if (hasKind(this.seenValueKinds, FrameSlotKind.Int) && isIntSlot(variableFrame, frameIndex)) {
                return FrameSlotChangeMonitor.getInt(variableFrame, frameIndex);
            } else if (hasKind(this.seenValueKinds, FrameSlotKind.Double) && isDoubleSlot(variableFrame, frameIndex)) {
                return FrameSlotChangeMonitor.getDouble(variableFrame, frameIndex);
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                // re-profile to widen the set of expected types
                return getValue(variableFrame, frameIndex);
            }
        } catch (FrameSlotTypeException e) {
            CompilerDirectives.transferToInterpreter();
            throw new RInternalError(e, "unexpected frame slot type mismatch");
        }
    }

    private static boolean isObjectSlot(Frame frame, int frameIndex) {
        if (FrameIndex.representsAuxiliaryIndex(frameIndex)) {
            return true;
        } else {
            return frame.isObject(FrameIndex.toNormalIndex(frameIndex));
        }
    }

    private static boolean isByteSlot(Frame frame, int frameIndex) {
        if (FrameIndex.representsNormalIndex(frameIndex)) {
            return frame.isByte(FrameIndex.toNormalIndex(frameIndex));
        } else {
            return false;
        }
    }

    private static boolean isIntSlot(Frame frame, int frameIndex) {
        if (FrameIndex.representsNormalIndex(frameIndex)) {
            return frame.isInt(FrameIndex.toNormalIndex(frameIndex));
        } else {
            return false;
        }
    }

    private static boolean isDoubleSlot(Frame frame, int frameIndex) {
        if (FrameIndex.representsNormalIndex(frameIndex)) {
            return frame.isDouble(FrameIndex.toNormalIndex(frameIndex));
        } else {
            return false;
        }
    }
}
