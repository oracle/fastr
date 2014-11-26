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
package com.oracle.truffle.r.engine;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.runtime.*;

/**
 * A "fake" {@link VirtualFrame}, to be used by {@link REngine}.eval only!
 */
public final class VirtualEvalFrame implements VirtualFrame, MaterializedFrame {

    private final MaterializedFrame originalFrame;
    @CompilationFinal private final Object[] arguments;

    private VirtualEvalFrame(MaterializedFrame originalFrame, SourceSection callSrc, int depth) {
        this.originalFrame = originalFrame;
        this.arguments = Arrays.copyOf(originalFrame.getArguments(), originalFrame.getArguments().length);
        RArguments.setDepth(this, depth);
        RArguments.setCallSourceSection(this, callSrc);
    }

    protected static VirtualEvalFrame create(MaterializedFrame originalFrame, SourceSection callSrc, int depth) {
        return new VirtualEvalFrame(originalFrame, callSrc, depth);
    }

    public FrameDescriptor getFrameDescriptor() {
        return originalFrame.getFrameDescriptor();
    }

    public Object[] getArguments() {
        return arguments;
    }

    public MaterializedFrame materialize() {
        return this;
    }

    /*
     * Delegates to #originalFrame
     */

    public Object getObject(FrameSlot slot) throws FrameSlotTypeException {
        return originalFrame.getObject(slot);
    }

    public void setObject(FrameSlot slot, Object value) {
        originalFrame.setObject(slot, value);
    }

    public byte getByte(FrameSlot slot) throws FrameSlotTypeException {
        return originalFrame.getByte(slot);
    }

    public void setByte(FrameSlot slot, byte value) {
        originalFrame.setByte(slot, value);
    }

    public boolean getBoolean(FrameSlot slot) throws FrameSlotTypeException {
        return originalFrame.getBoolean(slot);
    }

    public void setBoolean(FrameSlot slot, boolean value) {
        originalFrame.setBoolean(slot, value);
    }

    public int getInt(FrameSlot slot) throws FrameSlotTypeException {
        return originalFrame.getInt(slot);
    }

    public void setInt(FrameSlot slot, int value) {
        originalFrame.setInt(slot, value);
    }

    public long getLong(FrameSlot slot) throws FrameSlotTypeException {
        return originalFrame.getLong(slot);
    }

    public void setLong(FrameSlot slot, long value) {
        originalFrame.setLong(slot, value);
    }

    public float getFloat(FrameSlot slot) throws FrameSlotTypeException {
        return originalFrame.getFloat(slot);
    }

    public void setFloat(FrameSlot slot, float value) {
        originalFrame.setFloat(slot, value);
    }

    public double getDouble(FrameSlot slot) throws FrameSlotTypeException {
        return originalFrame.getDouble(slot);
    }

    public void setDouble(FrameSlot slot, double value) {
        originalFrame.setDouble(slot, value);
    }

    public Object getValue(FrameSlot slot) {
        return originalFrame.getValue(slot);
    }

    public boolean isObject(FrameSlot slot) {
        return originalFrame.isObject(slot);
    }

    public boolean isByte(FrameSlot slot) {
        return originalFrame.isByte(slot);
    }

    public boolean isBoolean(FrameSlot slot) {
        return originalFrame.isBoolean(slot);
    }

    public boolean isInt(FrameSlot slot) {
        return originalFrame.isInt(slot);
    }

    public boolean isLong(FrameSlot slot) {
        return originalFrame.isLong(slot);
    }

    public boolean isFloat(FrameSlot slot) {
        return originalFrame.isFloat(slot);
    }

    public boolean isDouble(FrameSlot slot) {
        return originalFrame.isDouble(slot);
    }
}
