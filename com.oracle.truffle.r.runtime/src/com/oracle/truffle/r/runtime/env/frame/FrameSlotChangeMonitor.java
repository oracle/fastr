/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * This is meant to monitor updates performed on {@link FrameSlot}. Each {@link FrameSlot} holds an
 * {@link Assumption} in it's "info" field; it is valid as long as no non-local update has ever
 * taken place.<br/>
 * The background to this rather strange assumption is that non-local reads are very hard to keep
 * track of thanks to R powerful language features. To keep the maintenance for the assumption as
 * cheap as possible, it checks only local reads - which is fast - and does a more costly check on
 * "<<-" but invalidates the assumption as soon as "eval" and the like comes into play.<br/>
 *
 *
 * @see #checkAndInvalidate(Frame, FrameSlot, boolean, BranchProfile)
 * @see #getMonitor(FrameSlot)
 */
public final class FrameSlotChangeMonitor {

    public static final FrameDescriptor NAMESPACE_BASE_MARKER_FRAME_DESCRIPTOR = new FrameDescriptor();

    private static final int MAX_FUNCTION_INVALIDATION_COUNT = 2;
    private static final int MAX_INVALIDATION_COUNT = 1;

    @SuppressWarnings("unused")
    private static void out(String format, Object... args) {
// System.out.println(String.format(format, args));
    }

    public abstract static class FrameSlotInfo {
        private boolean nonLocalModified = false;

        public final boolean isNonLocalModified() {
            return nonLocalModified;
        }

        public final void setNonLocalModified() {
            nonLocalModified = true;
        }
    }

    private static final class FrameSlotInfoImpl extends FrameSlotInfo {
        @CompilationFinal private StableValue<Object> stableValue;
        private final Object identifier;
        private int invalidationCount;

        public FrameSlotInfoImpl(boolean isSingletonFrame, Object identifier) {
            this.identifier = identifier;
            if (isSingletonFrame) {
                stableValue = new StableValue<>(null, identifier.toString());
                invalidationCount = 0;
            } else {
                stableValue = null;
            }
        }

        public boolean needsInvalidation() {
            return stableValue != null;
        }

        public void setValue(Object value) {
            if (stableValue != null && stableValue.getValue() != value) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stableValue.getAssumption().invalidate();
                int maxInvalidationCount = value instanceof RFunction ? MAX_FUNCTION_INVALIDATION_COUNT : MAX_INVALIDATION_COUNT;
                if (invalidationCount++ < maxInvalidationCount) {
                    out("setting singleton value %s = %s", identifier, value.getClass());
                    stableValue = new StableValue<>(value, identifier.toString());
                } else {
                    out("setting non-singleton value %s", identifier);
                    stableValue = null;
                }
            }
        }

        public void invalidateValue() {
            if (stableValue != null) {
                stableValue.getAssumption().invalidate();
                stableValue = null;
            }
        }

        public StableValue<Object> getStableValue() {
            return stableValue;
        }
    }

    /**
     * Retrieves the not-changed-locally {@link Assumption} in the {@link FrameSlot#getInfo()}
     * field.
     *
     * @param slot
     * @return The "not changed locally" assumption of the given {@link FrameSlot}
     *
     * @see FrameSlotChangeMonitor
     */
    public static FrameSlotInfo getMonitor(FrameSlot slot) {
        return getFrameSlotInfo(slot);
    }

    public static FrameSlotInfoImpl getFrameSlotInfo(FrameSlot slot) {
        Object info = slot.getInfo();
        if (!(info instanceof FrameSlotInfoImpl)) {
            CompilerDirectives.transferToInterpreter();
            throw RInternalError.shouldNotReachHere("Each FrameSlot should hold a FrameSlotInfo in it's info field!");
        }
        return (FrameSlotInfoImpl) info;
    }

    // method for creating new frame slots

    public static FrameSlot addFrameSlot(FrameDescriptor fd, Object identifier, FrameSlotKind kind) {
        boolean isSingletonFrame = descriptorSingletonAssumptions.containsKey(fd);
        return fd.addFrameSlot(identifier, new FrameSlotInfoImpl(isSingletonFrame, identifier), kind);
    }

    public static FrameSlot findOrAddFrameSlot(FrameDescriptor fd, Object identifier) {
        FrameSlot frameSlot = fd.findFrameSlot(identifier);
        return frameSlot != null ? frameSlot : addFrameSlot(fd, identifier, FrameSlotKind.Illegal);
    }

    public static FrameSlot findOrAddFrameSlot(FrameDescriptor fd, Object identifier, FrameSlotKind initialKind) {
        FrameSlot frameSlot = fd.findFrameSlot(identifier);
        return frameSlot != null ? frameSlot : addFrameSlot(fd, identifier, initialKind);
    }

    // methods for changing frame slot contents

    /**
     * Checks if the assumption of the given {@link FrameSlot} has to be invalidated.
     *
     * @param curFrame
     * @param slot {@link FrameSlot}; its "info" is assumed to be an Assumption, throws an
     *            {@link RInternalError} otherwise
     * @param invalidateProfile Used to guard the invalidation code.
     */
    private static void checkAndInvalidate(Frame curFrame, FrameSlot slot, boolean isNonLocal, BranchProfile invalidateProfile) {
        assert curFrame.getFrameDescriptor() == slot.getFrameDescriptor();

        // Check whether current frame is used outside a regular stack
        if (isNonLocal || RArguments.getIsIrregular(curFrame)) {
            // False positive: Also invalidates a slot in the current active frame if that one is
            // used inside eval or the like, but this cost is definitely negligible.
            if (invalidateProfile != null) {
                invalidateProfile.enter();
            }
            getMonitor(slot).setNonLocalModified();
        }
    }

    public static void setByteAndInvalidate(Frame frame, FrameSlot frameSlot, byte newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        frame.setByte(frameSlot, newValue);
        FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
        if (info.needsInvalidation()) {
            info.setValue(newValue);
        }
        checkAndInvalidate(frame, frameSlot, isNonLocal, invalidateProfile);
    }

    public static void setIntAndInvalidate(Frame frame, FrameSlot frameSlot, int newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        frame.setInt(frameSlot, newValue);
        FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
        if (info.needsInvalidation()) {
            info.setValue(newValue);
        }
        checkAndInvalidate(frame, frameSlot, isNonLocal, invalidateProfile);
    }

    public static void setDoubleAndInvalidate(Frame frame, FrameSlot frameSlot, double newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        frame.setDouble(frameSlot, newValue);
        FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
        if (info.needsInvalidation()) {
            info.setValue(newValue);
        }
        checkAndInvalidate(frame, frameSlot, isNonLocal, invalidateProfile);
    }

    public static void setObjectAndInvalidate(Frame frame, FrameSlot frameSlot, Object newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        frame.setObject(frameSlot, newValue);
        FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
        if (info.needsInvalidation()) {
            info.setValue(newValue);
        }
        checkAndInvalidate(frame, frameSlot, isNonLocal, invalidateProfile);
    }

    // update enclosing frames

    public static void invalidateEnclosingFrame(Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        MaterializedFrame enclosingFrame = RArguments.getEnclosingFrame(frame);
        getOrInitializeEnclosingFrameAssumption(frame, frame.getFrameDescriptor(), null, enclosingFrame);
        getOrInitializeEnclosingFrameDescriptorAssumption(frame, frame.getFrameDescriptor(), null, enclosingFrame == null ? null : enclosingFrame.getFrameDescriptor());
    }

    private static final WeakHashMap<FrameDescriptor, StableValue<MaterializedFrame>> descriptorEnclosingFrameAssumptions = new WeakHashMap<>();
    private static final WeakHashMap<FrameDescriptor, Boolean> descriptorSingletonAssumptions = new WeakHashMap<>();
    private static final WeakHashMap<FrameDescriptor, StableValue<FrameDescriptor>> descriptorEnclosingDescriptorAssumptions = new WeakHashMap<>();

    private static int rewriteFrameDescriptorAssumptionsCount;

    /**
     * Initializes the internal data structures for a newly created frame descriptor that is
     * intended to be used for a non-function frame (and thus will only ever be used for one frame).
     *
     * The namespace:base environment needs to be handled specially, because it shares a frame (and
     * thus, also a frame descriptor) with the package:base environment.
     */
    public static synchronized void initializeNonFunctionFrameDescriptor(FrameDescriptor originalFrameDescriptor, boolean isNamespaceBase) {
        FrameDescriptor frameDescriptor = isNamespaceBase ? NAMESPACE_BASE_MARKER_FRAME_DESCRIPTOR : originalFrameDescriptor;
        descriptorEnclosingFrameAssumptions.put(frameDescriptor, StableValue.invalidated());
        descriptorEnclosingDescriptorAssumptions.put(frameDescriptor, StableValue.invalidated());
        if (!isNamespaceBase) {
            descriptorSingletonAssumptions.put(originalFrameDescriptor, Boolean.FALSE);
        }
    }

    public static synchronized void initializeFunctionFrameDescriptor(FrameDescriptor frameDescriptor) {
        descriptorEnclosingFrameAssumptions.put(frameDescriptor, StableValue.invalidated());
        descriptorEnclosingDescriptorAssumptions.put(frameDescriptor, StableValue.invalidated());
    }

    public static synchronized StableValue<MaterializedFrame> getEnclosingFrameAssumption(FrameDescriptor descriptor) {
        return descriptorEnclosingFrameAssumptions.get(descriptor);
    }

    public static synchronized StableValue<FrameDescriptor> getEnclosingFrameDescriptorAssumption(FrameDescriptor descriptor) {
        return descriptorEnclosingDescriptorAssumptions.get(descriptor);
    }

    /**
     * Special handling (return a marker frame) for the namespace:base environment.
     */
    private static FrameDescriptor handleBaseNamespaceEnv(Frame frame, FrameDescriptor originalFrameDescriptor) {
        return frame instanceof NSBaseMaterializedFrame ? NAMESPACE_BASE_MARKER_FRAME_DESCRIPTOR : originalFrameDescriptor;
    }

    public static synchronized StableValue<FrameDescriptor> getOrInitializeEnclosingFrameDescriptorAssumption(Frame frame, FrameDescriptor originalFrameDescriptor, StableValue<FrameDescriptor> value,
                    FrameDescriptor newValue) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptor frameDescriptor = handleBaseNamespaceEnv(frame, originalFrameDescriptor);
        if (value != null) {
            value.getAssumption().invalidate();
        }
        StableValue<FrameDescriptor> currentValue = descriptorEnclosingDescriptorAssumptions.get(frameDescriptor);
        if (currentValue.getAssumption().isValid()) {
            if (currentValue.getValue() == newValue) {
                return currentValue;
            } else {
                currentValue.getAssumption().invalidate();
            }
        }
        currentValue = new StableValue<>(newValue, "enclosing frame descriptor");
        descriptorEnclosingDescriptorAssumptions.put(frameDescriptor, currentValue);
        if (value != null && value != StableValue.<FrameDescriptor> invalidated()) {
            assert rewriteFrameDescriptorAssumptionsCount++ < 100;
        }
        return currentValue;
    }

    public static synchronized StableValue<MaterializedFrame> getOrInitializeEnclosingFrameAssumption(Frame frame, FrameDescriptor originalFrameDescriptor, StableValue<MaterializedFrame> value,
                    MaterializedFrame newValue) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptor frameDescriptor = handleBaseNamespaceEnv(frame, originalFrameDescriptor);
        if (value != null) {
            value.getAssumption().invalidate();
        }
        StableValue<MaterializedFrame> currentValue = descriptorEnclosingFrameAssumptions.get(frameDescriptor);
        if (currentValue == null) {
            return null;
        }
        if (currentValue.getAssumption().isValid()) {
            if (currentValue.getValue() == newValue) {
                return currentValue;
            } else {
                currentValue.getAssumption().invalidate();
            }
        }
        if (currentValue == StableValue.<MaterializedFrame> invalidated()) {
            currentValue = new StableValue<>(newValue, "enclosing frame");
            descriptorEnclosingFrameAssumptions.put(frameDescriptor, currentValue);
            return currentValue;
        } else {
            descriptorEnclosingFrameAssumptions.remove(frameDescriptor);
            return null;
        }
    }

    public static boolean checkSingletonFrame(VirtualFrame vf) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        return checkSingletonFrameInternal(vf);
    }

    private static synchronized boolean checkSingletonFrameInternal(VirtualFrame vf) {
        Boolean value = descriptorSingletonAssumptions.get(vf.getFrameDescriptor());
        if (value == null) {
            return false;
        } else if (value == Boolean.FALSE) {
            out("marking frame descriptor %s as singleton", vf.getFrameDescriptor());
            descriptorSingletonAssumptions.put(vf.getFrameDescriptor(), Boolean.TRUE);
            return true;
        } else {
            out("marking frame descriptor %s as non-singleton", vf.getFrameDescriptor());
            for (FrameSlot slot : vf.getFrameDescriptor().getSlots()) {
                if (getFrameSlotInfo(slot).needsInvalidation()) {
                    getFrameSlotInfo(slot).invalidateValue();
                    out("  invalidating singleton slot %s", slot.getIdentifier());
                }
            }
            descriptorSingletonAssumptions.remove(vf.getFrameDescriptor());
            return false;
        }
    }

    public static synchronized StableValue<Object> getStableValueAssumption(FrameDescriptor descriptor, FrameSlot frameSlot, Object value) {
        CompilerAsserts.neverPartOfCompilation();
        StableValue<Object> stableValue = getFrameSlotInfo(frameSlot).getStableValue();
        if (stableValue != null) {
            assert descriptorSingletonAssumptions.containsKey(descriptor) : "single frame slot within non-singleton descriptor";
            assert stableValue.getValue() == value || (stableValue.getValue() != null && (stableValue.getValue().equals(value) || !stableValue.getAssumption().isValid())) : stableValue.getValue() +
                            " vs. " + value;
        }
        return stableValue;
    }

    public static void updateValue(FrameSlot slot, Object value) {
        FrameSlotInfoImpl info = getFrameSlotInfo(slot);
        if (info.needsInvalidation()) {
            info.setValue(value);
        }
    }
}
