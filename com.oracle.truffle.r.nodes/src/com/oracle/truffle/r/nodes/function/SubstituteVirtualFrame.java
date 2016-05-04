/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class SubstituteVirtualFrame implements VirtualFrame, MaterializedFrame {

    /*
     * These classes will specialize towards specific frame types, of which there are usually only
     * one or two in the system. As a result, when we know the type of the substitute frame, we also
     * know the type of the original frame.
     */

    private static final class Substitute1 extends SubstituteVirtualFrame {

        @CompilationFinal private static Class<MaterializedFrame> frameClass;

        protected Substitute1(MaterializedFrame originalFrame) {
            super(originalFrame);
        }

        @Override
        protected MaterializedFrame getOriginalFrame() {
            return frameClass.cast(originalFrame);
        }
    }

    private static final class Substitute2 extends SubstituteVirtualFrame {

        @CompilationFinal private static Class<MaterializedFrame> frameClass;

        protected Substitute2(MaterializedFrame originalFrame) {
            super(originalFrame);
        }

        @Override
        protected MaterializedFrame getOriginalFrame() {
            return frameClass.cast(originalFrame);
        }
    }

    private static final class SubstituteGeneric extends SubstituteVirtualFrame {

        protected SubstituteGeneric(MaterializedFrame originalFrame) {
            super(originalFrame);
        }

        @Override
        protected MaterializedFrame getOriginalFrame() {
            return originalFrame;
        }
    }

    public static VirtualFrame create(MaterializedFrame frame) {
        @SuppressWarnings("unchecked")
        Class<MaterializedFrame> clazz = (Class<MaterializedFrame>) frame.getClass();
        if (Substitute1.frameClass == clazz) {
            return new Substitute1(frame);
        } else if (Substitute1.frameClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Substitute1.frameClass = clazz;
            return new Substitute1(frame);
        } else if (Substitute2.frameClass == clazz) {
            return new Substitute2(frame);
        } else if (Substitute2.frameClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Substitute2.frameClass = clazz;
            return new Substitute2(frame);
        } else {
            return new SubstituteGeneric(frame);
        }
    }

    protected final MaterializedFrame originalFrame;

    protected SubstituteVirtualFrame(MaterializedFrame originalFrame) {
        this.originalFrame = originalFrame;
    }

    protected abstract MaterializedFrame getOriginalFrame();

    @Override
    public FrameDescriptor getFrameDescriptor() {
        return getOriginalFrame().getFrameDescriptor();
    }

    @Override
    public Object[] getArguments() {
        return getOriginalFrame().getArguments();
    }

    @Override
    public MaterializedFrame materialize() {
        return getOriginalFrame();
    }

    /*
     * Delegates to #getOriginalFrame()
     */

    @Override
    public Object getObject(FrameSlot slot) throws FrameSlotTypeException {
        return getOriginalFrame().getObject(slot);
    }

    @Override
    public void setObject(FrameSlot slot, Object value) {
        getOriginalFrame().setObject(slot, value);
    }

    @Override
    public byte getByte(FrameSlot slot) throws FrameSlotTypeException {
        return getOriginalFrame().getByte(slot);
    }

    @Override
    public void setByte(FrameSlot slot, byte value) {
        getOriginalFrame().setByte(slot, value);
    }

    @Override
    public boolean getBoolean(FrameSlot slot) throws FrameSlotTypeException {
        return getOriginalFrame().getBoolean(slot);
    }

    @Override
    public void setBoolean(FrameSlot slot, boolean value) {
        getOriginalFrame().setBoolean(slot, value);
    }

    @Override
    public int getInt(FrameSlot slot) throws FrameSlotTypeException {
        return getOriginalFrame().getInt(slot);
    }

    @Override
    public void setInt(FrameSlot slot, int value) {
        getOriginalFrame().setInt(slot, value);
    }

    @Override
    public long getLong(FrameSlot slot) throws FrameSlotTypeException {
        return getOriginalFrame().getLong(slot);
    }

    @Override
    public void setLong(FrameSlot slot, long value) {
        getOriginalFrame().setLong(slot, value);
    }

    @Override
    public float getFloat(FrameSlot slot) throws FrameSlotTypeException {
        return getOriginalFrame().getFloat(slot);
    }

    @Override
    public void setFloat(FrameSlot slot, float value) {
        getOriginalFrame().setFloat(slot, value);
    }

    @Override
    public double getDouble(FrameSlot slot) throws FrameSlotTypeException {
        return getOriginalFrame().getDouble(slot);
    }

    @Override
    public void setDouble(FrameSlot slot, double value) {
        getOriginalFrame().setDouble(slot, value);
    }

    @Override
    public Object getValue(FrameSlot slot) {
        return getOriginalFrame().getValue(slot);
    }

    @Override
    public boolean isObject(FrameSlot slot) {
        return getOriginalFrame().isObject(slot);
    }

    @Override
    public boolean isByte(FrameSlot slot) {
        return getOriginalFrame().isByte(slot);
    }

    @Override
    public boolean isBoolean(FrameSlot slot) {
        return getOriginalFrame().isBoolean(slot);
    }

    @Override
    public boolean isInt(FrameSlot slot) {
        return getOriginalFrame().isInt(slot);
    }

    @Override
    public boolean isLong(FrameSlot slot) {
        return getOriginalFrame().isLong(slot);
    }

    @Override
    public boolean isFloat(FrameSlot slot) {
        return getOriginalFrame().isFloat(slot);
    }

    @Override
    public boolean isDouble(FrameSlot slot) {
        return getOriginalFrame().isDouble(slot);
    }
}
