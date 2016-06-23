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
package com.oracle.truffle.r.runtime;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.runtime.data.RFunction;

/**
 * A "fake" {@link VirtualFrame}, to be used by {@code REngine}.eval only!
 */
public abstract class VirtualEvalFrame implements VirtualFrame, MaterializedFrame {

    protected final MaterializedFrame originalFrame;
    @CompilationFinal protected final Object[] arguments;

    private VirtualEvalFrame(MaterializedFrame originalFrame, Object[] arguments) {
        this.originalFrame = originalFrame;
        this.arguments = arguments;
    }

    @Override
    public FrameDescriptor getFrameDescriptor() {
        return getOriginalFrame().getFrameDescriptor();
    }

    @Override
    public Object[] getArguments() {
        return arguments;
    }

    public abstract MaterializedFrame getOriginalFrame();

    @Override
    public MaterializedFrame materialize() {
        return this;
    }

    /*
     * Delegates to #originalFrame
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

    private static final class Substitute1 extends VirtualEvalFrame {

        @CompilationFinal private static Class<MaterializedFrame> frameClass;

        protected Substitute1(MaterializedFrame originalFrame, Object[] arguments) {
            super(originalFrame, arguments);
        }

        @Override
        public MaterializedFrame getOriginalFrame() {
            return frameClass.cast(originalFrame);
        }
    }

    private static final class Substitute2 extends VirtualEvalFrame {

        @CompilationFinal private static Class<MaterializedFrame> frameClass;

        protected Substitute2(MaterializedFrame originalFrame, Object[] arguments) {
            super(originalFrame, arguments);
        }

        @Override
        public MaterializedFrame getOriginalFrame() {
            return frameClass.cast(originalFrame);
        }
    }

    private static final class SubstituteGeneric extends VirtualEvalFrame {

        protected SubstituteGeneric(MaterializedFrame originalFrame, Object[] arguments) {
            super(originalFrame, arguments);
        }

        @Override
        public MaterializedFrame getOriginalFrame() {
            return originalFrame;
        }
    }

    public static VirtualEvalFrame create(MaterializedFrame originalFrame, RFunction function, RCaller call) {
        Object[] arguments = Arrays.copyOf(originalFrame.getArguments(), originalFrame.getArguments().length);
        arguments[RArguments.INDEX_IS_IRREGULAR] = true;
        arguments[RArguments.INDEX_FUNCTION] = function;
        arguments[RArguments.INDEX_CALL] = call;
        @SuppressWarnings("unchecked")
        Class<MaterializedFrame> clazz = (Class<MaterializedFrame>) originalFrame.getClass();
        if (Substitute1.frameClass == clazz) {
            return new Substitute1(originalFrame, arguments);
        } else if (Substitute1.frameClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Substitute1.frameClass = clazz;
            return new Substitute1(originalFrame, arguments);
        } else if (Substitute2.frameClass == clazz) {
            return new Substitute2(originalFrame, arguments);
        } else if (Substitute2.frameClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Substitute2.frameClass = clazz;
            return new Substitute2(originalFrame, arguments);
        } else {
            return new SubstituteGeneric(originalFrame, arguments);
        }
    }
}
