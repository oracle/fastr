/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.env.frame;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.REnvironment.*;

/**
 * Allows an {@link REnvironment} without a Truffle {@link Frame}, e.g. one created by
 * {@code attach} to appear to be a {@link MaterializedFrame} and therefore be inserted in the
 * enclosing frame hierarchy used for unquoted variable lookup. It ought to be possible to share
 * code from Truffle, but the relevant classes are final. Life would be easier if the environment
 * was immutable. No attempt is currently being made to make variable access efficient.
 */
public class REnvMaterializedFrame implements MaterializedFrame {
    private final Map<String, Object> map;
    private final FrameDescriptor descriptor;
    private final Object[] arguments;
    private byte[] tags;

    public REnvMaterializedFrame(UsesREnvMap env) {
        descriptor = new FrameDescriptor();
        REnvMapFrameAccess frameAccess = env.getFrameAccess();
        map = frameAccess.getMap();
        tags = new byte[map.size()];
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            FrameSlotKind kind = getFrameSlotKindForValue(entry.getValue());
            descriptor.addFrameSlot(entry.getKey(), kind);
            tags[i++] = (byte) kind.ordinal();
        }
        frameAccess.setMaterializedFrame(this);
        arguments = RArguments.createUnitialized();
        RArguments.setEnvironment(this, (REnvironment) env);
    }

    /**
     * Assignment to the frame, need to keep the Truffle view in sync.
     */
    public void put(String name, Object value) {
        // If this variable exists already, then there is nothing to do (currently)
        // as the Truffle read/write methods use the backing map, which will
        // have been updated by our caller. However, if it is a new variable
        // we have to add a slot for it.
        FrameSlot slot = descriptor.findFrameSlot(name);
        if (slot == null) {
            FrameSlotKind kind = getFrameSlotKindForValue(value);
            slot = descriptor.addFrameSlot(name, kind);
            resize();
            tags[slot.getIndex()] = (byte) kind.ordinal();
        }
    }

    /**
     * Removal of variable from frame.
     */
    public void rm(String name) {
        descriptor.removeFrameSlot(name);
    }

    private static FrameSlotKind getFrameSlotKindForValue(Object value) {
        if (value instanceof Double) {
            return FrameSlotKind.Double;
        } else if (value instanceof Byte) {
            return FrameSlotKind.Byte;
        } else if (value instanceof Integer) {
            return FrameSlotKind.Int;
        } else {
            return FrameSlotKind.Object;
        }
    }

    public FrameDescriptor getFrameDescriptor() {
        return descriptor;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public Object getObject(FrameSlot slot) throws FrameSlotTypeException {
        verifyGet(slot, FrameSlotKind.Object);
        return map.get(slot.getIdentifier());
    }

    public void setObject(FrameSlot slot, Object value) {
        verifySet(slot, FrameSlotKind.Object);
        map.put((String) slot.getIdentifier(), value);
    }

    public byte getByte(FrameSlot slot) throws FrameSlotTypeException {
        verifyGet(slot, FrameSlotKind.Byte);
        return (byte) map.get(slot.getIdentifier());
    }

    public void setByte(FrameSlot slot, byte value) {
        verifySet(slot, FrameSlotKind.Byte);
        map.put((String) slot.getIdentifier(), value);
    }

    public boolean getBoolean(FrameSlot slot) throws FrameSlotTypeException {
        verifyGet(slot, FrameSlotKind.Boolean);
        return (boolean) map.get(slot.getIdentifier());
    }

    public void setBoolean(FrameSlot slot, boolean value) {
        verifySet(slot, FrameSlotKind.Boolean);
        map.put((String) slot.getIdentifier(), value);
    }

    public int getInt(FrameSlot slot) throws FrameSlotTypeException {
        verifyGet(slot, FrameSlotKind.Int);
        return (int) map.get(slot.getIdentifier());
    }

    public void setInt(FrameSlot slot, int value) {
        verifySet(slot, FrameSlotKind.Int);
        map.put((String) slot.getIdentifier(), value);
    }

    public long getLong(FrameSlot slot) throws FrameSlotTypeException {
        verifyGet(slot, FrameSlotKind.Long);
        return (long) map.get(slot.getIdentifier());
    }

    public void setLong(FrameSlot slot, long value) {
        verifySet(slot, FrameSlotKind.Long);
        map.put((String) slot.getIdentifier(), value);
    }

    public float getFloat(FrameSlot slot) throws FrameSlotTypeException {
        verifyGet(slot, FrameSlotKind.Float);
        return (float) map.get(slot.getIdentifier());
    }

    public void setFloat(FrameSlot slot, float value) {
        verifySet(slot, FrameSlotKind.Float);
        map.put((String) slot.getIdentifier(), value);
    }

    public double getDouble(FrameSlot slot) throws FrameSlotTypeException {
        verifyGet(slot, FrameSlotKind.Double);
        return (double) map.get(slot.getIdentifier());
    }

    public void setDouble(FrameSlot slot, double value) {
        verifySet(slot, FrameSlotKind.Double);
        map.put((String) slot.getIdentifier(), value);
    }

    @Override
    public Object getValue(FrameSlot slot) {
        int slotIndex = slot.getIndex();
        if (slotIndex >= getTags().length) {
            resize();
        }
        return map.get(slot.getIdentifier());
    }

    public MaterializedFrame materialize() {
        return this;
    }

    private byte[] getTags() {
        return tags;
    }

    private boolean resize() {
        int oldSize = tags.length;
        int newSize = descriptor.getSize();
        if (newSize > oldSize) {
            tags = Arrays.copyOf(tags, newSize);
            return true;
        }
        return false;
    }

    private byte getTag(FrameSlot slot) {
        int slotIndex = slot.getIndex();
        if (slotIndex >= getTags().length) {
            resize();
        }
        return getTags()[slotIndex];
    }

    @Override
    public boolean isObject(FrameSlot slot) {
        return getTag(slot) == FrameSlotKind.Object.ordinal();
    }

    @Override
    public boolean isByte(FrameSlot slot) {
        return getTag(slot) == FrameSlotKind.Byte.ordinal();
    }

    @Override
    public boolean isBoolean(FrameSlot slot) {
        return getTag(slot) == FrameSlotKind.Boolean.ordinal();
    }

    @Override
    public boolean isInt(FrameSlot slot) {
        return getTag(slot) == FrameSlotKind.Int.ordinal();
    }

    @Override
    public boolean isLong(FrameSlot slot) {
        return getTag(slot) == FrameSlotKind.Long.ordinal();
    }

    @Override
    public boolean isFloat(FrameSlot slot) {
        return getTag(slot) == FrameSlotKind.Float.ordinal();
    }

    @Override
    public boolean isDouble(FrameSlot slot) {
        return getTag(slot) == FrameSlotKind.Double.ordinal();
    }

    private void verifySet(FrameSlot slot, FrameSlotKind accessKind) {
        int slotIndex = slot.getIndex();
        if (slotIndex >= getTags().length) {
            if (!resize()) {
                illegal("The frame slot '%s' is not known by the frame descriptor.", slot);
            }
        }
        getTags()[slotIndex] = (byte) accessKind.ordinal();
    }

    private void verifyGet(FrameSlot slot, FrameSlotKind accessKind) throws FrameSlotTypeException {
        int slotIndex = slot.getIndex();
        if (slotIndex >= getTags().length) {
            if (!resize()) {
                illegal("The frame slot '%s' is not known by the frame descriptor.", slot);
            }
        }
        byte tag = this.getTags()[slotIndex];
        if (tag != accessKind.ordinal()) {
            if (slot.getKind() == accessKind || tag == 0) {
                descriptor.getTypeConversion().updateFrameSlot(this, slot, getValue(slot));
                if (getTags()[slotIndex] == accessKind.ordinal()) {
                    return;
                }
            }
            throw frameSlotTypeException();
        }
    }

    @SlowPath
    private static void illegal(String message, FrameSlot slot) {
        throw new IllegalArgumentException(String.format(message, slot));
    }

    @SlowPath
    private static FrameSlotTypeException frameSlotTypeException() throws FrameSlotTypeException {
        throw new FrameSlotTypeException();
    }

}
