/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.runtime.data.RFunction;

/**
 * A "fake" {@link VirtualFrame} that delegates everything to the wrapped frame except for the
 * arguments array getter.
 */
public abstract class VirtualEvalFrame implements MaterializedFrame {

    @CompilationFinal(dimensions = 1) protected final Object[] arguments;
    final MaterializedFrame originalFrame;

    private VirtualEvalFrame(MaterializedFrame originalFrame, Object[] arguments) {
        this.arguments = arguments;
        this.originalFrame = originalFrame;
    }

    public abstract MaterializedFrame getOriginalFrame();

    @Override
    public final FrameDescriptor getFrameDescriptor() {
        return getOriginalFrame().getFrameDescriptor();
    }

    @Override
    public final Object[] getArguments() {
        return arguments;
    }

    @Override
    public final MaterializedFrame materialize() {
        return this;
    }

    /*
     * Delegates to #getOriginalFrame()
     */

    @Override
    public Object getObject(int slot) throws FrameSlotTypeException {
        return getOriginalFrame().getObject(slot);
    }

    @Override
    public void setObject(int slot, Object value) {
        getOriginalFrame().setObject(slot, value);
    }

    @Override
    public byte getByte(int slot) throws FrameSlotTypeException {
        return getOriginalFrame().getByte(slot);
    }

    @Override
    public void setByte(int slot, byte value) {
        getOriginalFrame().setByte(slot, value);
    }

    @Override
    public boolean getBoolean(int slot) throws FrameSlotTypeException {
        return getOriginalFrame().getBoolean(slot);
    }

    @Override
    public void setBoolean(int slot, boolean value) {
        getOriginalFrame().setBoolean(slot, value);
    }

    @Override
    public int getInt(int slot) throws FrameSlotTypeException {
        return getOriginalFrame().getInt(slot);
    }

    @Override
    public void setInt(int slot, int value) {
        getOriginalFrame().setInt(slot, value);
    }

    @Override
    public long getLong(int slot) throws FrameSlotTypeException {
        return getOriginalFrame().getLong(slot);
    }

    @Override
    public void setLong(int slot, long value) {
        getOriginalFrame().setLong(slot, value);
    }

    @Override
    public float getFloat(int slot) throws FrameSlotTypeException {
        return getOriginalFrame().getFloat(slot);
    }

    @Override
    public void setFloat(int slot, float value) {
        getOriginalFrame().setFloat(slot, value);
    }

    @Override
    public double getDouble(int slot) throws FrameSlotTypeException {
        return getOriginalFrame().getDouble(slot);
    }

    @Override
    public void setDouble(int slot, double value) {
        getOriginalFrame().setDouble(slot, value);
    }

    @Override
    public Object getValue(int slot) {
        return getOriginalFrame().getValue(slot);
    }

    @Override
    public void copy(int srcSlot, int destSlot) {
        getOriginalFrame().copy(srcSlot, destSlot);
    }

    @Override
    public void swap(int first, int second) {
        getOriginalFrame().swap(first, second);
    }

    @Override
    public byte getTag(int slot) {
        return getOriginalFrame().getTag(slot);
    }

    @Override
    public boolean isObject(int slot) {
        return getOriginalFrame().isObject(slot);
    }

    @Override
    public boolean isByte(int slot) {
        return getOriginalFrame().isByte(slot);
    }

    @Override
    public boolean isBoolean(int slot) {
        return getOriginalFrame().isBoolean(slot);
    }

    @Override
    public boolean isInt(int slot) {
        return getOriginalFrame().isInt(slot);
    }

    @Override
    public boolean isLong(int slot) {
        return getOriginalFrame().isLong(slot);
    }

    @Override
    public boolean isFloat(int slot) {
        return getOriginalFrame().isFloat(slot);
    }

    @Override
    public boolean isDouble(int slot) {
        return getOriginalFrame().isDouble(slot);
    }

    @Override
    public void clear(int slot) {
        getOriginalFrame().clear(slot);
    }

    @Override
    public Object getAuxiliarySlot(int slot) {
        return getOriginalFrame().getAuxiliarySlot(slot);
    }

    @Override
    public void setAuxiliarySlot(int slot, Object value) {
        getOriginalFrame().setAuxiliarySlot(slot, value);
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
        return create(originalFrame, function, null, call);
    }

    public static VirtualEvalFrame create(MaterializedFrame originalFrame, RFunction function, Object callerFrame, RCaller call) {
        Object[] arguments = Arrays.copyOf(originalFrame.getArguments(), originalFrame.getArguments().length);
        arguments[RArguments.INDEX_IS_IRREGULAR] = true;
        arguments[RArguments.INDEX_FUNCTION] = function;
        if (call != null) {
            // Otherwise leave here the call from originalFrame
            arguments[RArguments.INDEX_CALL] = call;
        } else if (arguments[RArguments.INDEX_CALL] == null) {
            arguments[RArguments.INDEX_CALL] = RCaller.topLevel;
        }
        if (callerFrame != null) {
            arguments[RArguments.INDEX_CALLER_FRAME] = callerFrame;
        }
        MaterializedFrame unwrappedFrame = originalFrame instanceof VirtualEvalFrame ? ((VirtualEvalFrame) originalFrame).getOriginalFrame() : originalFrame;
        Class<MaterializedFrame> clazz = getMaterializedFrameClass(unwrappedFrame);
        if (Substitute1.frameClass == clazz) {
            return new Substitute1(unwrappedFrame, arguments);
        } else if (Substitute1.frameClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Substitute1.frameClass = clazz;
            return new Substitute1(unwrappedFrame, arguments);
        } else if (Substitute2.frameClass == clazz) {
            return new Substitute2(unwrappedFrame, arguments);
        } else if (Substitute2.frameClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Substitute2.frameClass = clazz;
            return new Substitute2(unwrappedFrame, arguments);
        } else {
            return new SubstituteGeneric(unwrappedFrame, arguments);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<MaterializedFrame> getMaterializedFrameClass(MaterializedFrame unwrappedFrame) {
        return (Class<MaterializedFrame>) unwrappedFrame.getClass();
    }
}
