/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.runtime.RArguments;

/**
 * This frame is used for {@code namespace:base}. It delegates all operations to
 * {@link #packageBaseFrame}, however, it's enclosing frame is set to the frame for
 * {@code globalenv}.
 */
public final class NSBaseMaterializedFrame implements MaterializedFrame {

    private static final ValueProfile frameProfile = ValueProfile.createClassProfile();

    private final MaterializedFrame packageBaseFrame;
    @CompilationFinal private final Object[] arguments;

    // this frame descriptor is only used for lookups in FrameSlotChangeMonitor
    private final FrameDescriptor markerFrameDescriptor;

    public NSBaseMaterializedFrame(MaterializedFrame packageBaseFrame, MaterializedFrame globalFrame) {
        this.packageBaseFrame = packageBaseFrame;
        this.arguments = Arrays.copyOf(packageBaseFrame.getArguments(), packageBaseFrame.getArguments().length);
        this.markerFrameDescriptor = new FrameDescriptor();
        FrameSlotChangeMonitor.initializeNonFunctionFrameDescriptor("namespace:base", this);
        RArguments.initializeEnclosingFrame(this, globalFrame);
    }

    private MaterializedFrame getPackageBaseFrame() {
        return frameProfile.profile(packageBaseFrame);
    }

    public void updateGlobalFrame(MaterializedFrame globalFrame) {
        RArguments.setEnclosingFrame(this, globalFrame, true);
    }

    public FrameDescriptor getMarkerFrameDescriptor() {
        return markerFrameDescriptor;
    }

    @Override
    public FrameDescriptor getFrameDescriptor() {
        return getPackageBaseFrame().getFrameDescriptor();
    }

    @Override
    public Object[] getArguments() {
        return arguments;
    }

    @Override
    public MaterializedFrame materialize() {
        return this;
    }

    /*
     * Delegates to #originalFrame
     */

    @Override
    public Object getObject(FrameSlot slot) throws FrameSlotTypeException {
        return getPackageBaseFrame().getObject(slot);
    }

    @Override
    public void setObject(FrameSlot slot, Object value) {
        getPackageBaseFrame().setObject(slot, value);
    }

    @Override
    public byte getByte(FrameSlot slot) throws FrameSlotTypeException {
        return getPackageBaseFrame().getByte(slot);
    }

    @Override
    public void setByte(FrameSlot slot, byte value) {
        getPackageBaseFrame().setByte(slot, value);
    }

    @Override
    public boolean getBoolean(FrameSlot slot) throws FrameSlotTypeException {
        return getPackageBaseFrame().getBoolean(slot);
    }

    @Override
    public void setBoolean(FrameSlot slot, boolean value) {
        getPackageBaseFrame().setBoolean(slot, value);
    }

    @Override
    public int getInt(FrameSlot slot) throws FrameSlotTypeException {
        return getPackageBaseFrame().getInt(slot);
    }

    @Override
    public void setInt(FrameSlot slot, int value) {
        getPackageBaseFrame().setInt(slot, value);
    }

    @Override
    public long getLong(FrameSlot slot) throws FrameSlotTypeException {
        return getPackageBaseFrame().getLong(slot);
    }

    @Override
    public void setLong(FrameSlot slot, long value) {
        getPackageBaseFrame().setLong(slot, value);
    }

    @Override
    public float getFloat(FrameSlot slot) throws FrameSlotTypeException {
        return getPackageBaseFrame().getFloat(slot);
    }

    @Override
    public void setFloat(FrameSlot slot, float value) {
        getPackageBaseFrame().setFloat(slot, value);
    }

    @Override
    public double getDouble(FrameSlot slot) throws FrameSlotTypeException {
        return getPackageBaseFrame().getDouble(slot);
    }

    @Override
    public void setDouble(FrameSlot slot, double value) {
        getPackageBaseFrame().setDouble(slot, value);
    }

    @Override
    public Object getValue(FrameSlot slot) {
        return getPackageBaseFrame().getValue(slot);
    }

    @Override
    public boolean isObject(FrameSlot slot) {
        return getPackageBaseFrame().isObject(slot);
    }

    @Override
    public boolean isByte(FrameSlot slot) {
        return getPackageBaseFrame().isByte(slot);
    }

    @Override
    public boolean isBoolean(FrameSlot slot) {
        return getPackageBaseFrame().isBoolean(slot);
    }

    @Override
    public boolean isInt(FrameSlot slot) {
        return getPackageBaseFrame().isInt(slot);
    }

    @Override
    public boolean isLong(FrameSlot slot) {
        return getPackageBaseFrame().isLong(slot);
    }

    @Override
    public boolean isFloat(FrameSlot slot) {
        return getPackageBaseFrame().isFloat(slot);
    }

    @Override
    public boolean isDouble(FrameSlot slot) {
        return getPackageBaseFrame().isDouble(slot);
    }
}
