/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.StableValue;
import com.oracle.truffle.r.runtime.data.RPromise;

/**
 * This class maintains information about the current hierarchy of environments in the system. This
 * information is described as assumptions that will be invalidated if the layout changes, and thus
 * make sure that code is properly deoptimized.
 */
public final class FrameSlotChangeMonitor {

    /*
     * The following classes describe the result of a previous lookup that successfully delivered a
     * result based on the system's knowledge about the hierarchy of environments and the stable
     * values of certain bindings. Most function lookups can be answered based only on this
     * information.
     *
     * These lookups are stored for caching and invalidation, i.e., to save on repeated lookups and
     * to invalidate lookups in case the environment hierarchy changes.
     */

    public abstract static class LookupResult {
        protected final Assumption assumption;

        private LookupResult(String identifier) {
            this.assumption = Truffle.getRuntime().createAssumption("lookup \"" + identifier + "\" (" + this.getClass().getSimpleName() + ")");
        }

        public boolean isValid() {
            return assumption.isValid();
        }

        public abstract Object getValue() throws InvalidAssumptionException;

        private void invalidate() {
            assumption.invalidate();
        }
    }

    private static final class StableValueLookupResult extends LookupResult {
        private final StableValue<Object> value;
        @CompilationFinal private Object unwrappedValue;

        private StableValueLookupResult(String identifier, StableValue<Object> value) {
            super(identifier);
            this.value = value;
        }

        @Override
        public boolean isValid() {
            return super.isValid() && value.getAssumption().isValid();
        }

        @Override
        public Object getValue() throws InvalidAssumptionException {
            assumption.check();
            StableValue<Object> result = value;
            result.getAssumption().check();
            if (unwrappedValue == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                Object resultValue = result.getValue();
                if (resultValue instanceof RPromise) {
                    if (((RPromise) resultValue).isEvaluated()) {
                        unwrappedValue = ((RPromise) resultValue).getValue();
                    } else {
                        return resultValue;
                    }
                } else {
                    unwrappedValue = resultValue;
                }
            }
            return unwrappedValue;
        }
    }

    private static final class MissingLookupResult extends LookupResult {

        private MissingLookupResult(String identifier) {
            super(identifier);
        }

        @Override
        public Object getValue() throws InvalidAssumptionException {
            assumption.check();
            return null;
        }
    }

    public static final class FrameAndSlotLookupResult extends LookupResult {
        private final MaterializedFrame frame;
        private final FrameSlot slot;

        private FrameAndSlotLookupResult(String identifier, MaterializedFrame frame, FrameSlot slot) {
            super(identifier);
            this.frame = frame;
            this.slot = slot;
        }

        @Override
        public Object getValue() {
            // fast path execution should use getFrame / getSlot
            CompilerAsserts.neverPartOfCompilation("FrameAndSlotLookupResult.getValue() should not be used in fast path execution");
            return frame.getValue(slot);
        }

        public MaterializedFrame getFrame() throws InvalidAssumptionException {
            assumption.check();
            return frame;
        }

        public FrameSlot getSlot() throws InvalidAssumptionException {
            assumption.check();
            return slot;
        }
    }

    /**
     * Every frame descriptor in the system will be associated with a FrameDescriptorMetaData
     * object. For function environments, one frame descriptor corresponds to many actual
     * environments, while for manually created environment, there is always one frame descriptor
     * for one environment.
     */
    private static final class FrameDescriptorMetaData {
        private final String name; // name for debug purposes
        private final WeakReference<MaterializedFrame> singletonFrame;
        private final Set<FrameDescriptor> subDescriptors = Collections.newSetFromMap(new WeakHashMap<>(2));

        /**
         * This set contains all lookups that have been performed "across" this frame descriptor. If
         * a binding with one of these names is modified, then the lookups in this frame descriptor
         * and all child frame descriptors need to be checked.
         */
        private final Set<Object> previousLookups = new HashSet<>();
        /**
         * A set of all lookups that started in this frame descriptor.
         */
        private final WeakHashMap<Object, WeakReference<LookupResult>> lookupResults = new WeakHashMap<>(2);

        private WeakReference<FrameDescriptor> enclosingFrameDescriptor = new WeakReference<>(null);
        private Assumption enclosingFrameDescriptorAssumption = Truffle.getRuntime().createAssumption("enclosing frame descriptor");
        private Assumption containsNoActiveBindingAssumption = Truffle.getRuntime().createAssumption("contains no active binding");

        private FrameDescriptorMetaData(String name, MaterializedFrame singletonFrame) {
            this.name = name;
            this.singletonFrame = singletonFrame == null ? null : new WeakReference<>(singletonFrame);
        }

        public void updateEnclosingFrameDescriptor(FrameDescriptor newEnclosingDescriptor) {
            CompilerAsserts.neverPartOfCompilation();
            if (enclosingFrameDescriptorAssumption != null) {
                enclosingFrameDescriptorAssumption.invalidate();
            }
            enclosingFrameDescriptor = new WeakReference<>(newEnclosingDescriptor);
            enclosingFrameDescriptorAssumption = Truffle.getRuntime().createAssumption("enclosing frame descriptor");
        }

        public FrameDescriptor getEnclosingFrameDescriptor() {
            CompilerAsserts.neverPartOfCompilation();
            assert enclosingFrameDescriptorAssumption.isValid();
            return enclosingFrameDescriptor.get();
        }

        public Assumption getEnclosingFrameDescriptorAssumption() {
            return enclosingFrameDescriptorAssumption;
        }

        public Assumption getContainsNoActiveBindingAssumption() {
            return containsNoActiveBindingAssumption;
        }
    }

    private static final WeakHashMap<FrameDescriptor, FrameDescriptorMetaData> frameDescriptors = new WeakHashMap<>();

    /**
     * This function tries to fulfill the lookup for the given name in the given frame based only on
     * the static knowledge about the frame descriptor hierarchy and stable bindings. Returns
     * {@code null} in case this was not possible.
     */
    public static synchronized LookupResult lookup(Frame frame, Object identifier) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptorMetaData metaData = getMetaData(frame);
        WeakReference<LookupResult> weakResult = metaData.lookupResults.get(identifier);
        LookupResult result = weakResult == null ? null : weakResult.get();
        if (result != null && result.isValid()) {
            return result;
        }
        Frame current = frame;
        while (true) {
            FrameSlot slot = current.getFrameDescriptor().findFrameSlot(identifier);
            if (slot != null) {
                LookupResult lookupResult;
                StableValue<Object> stableValue = getFrameSlotInfo(slot).stableValue;
                if (stableValue != null) {
                    lookupResult = new StableValueLookupResult(identifier.toString(), stableValue);
                } else {
                    FrameDescriptorMetaData currentMetaData = getMetaData(current);
                    if (currentMetaData.singletonFrame == null) {
                        return null;
                    } else {
                        assert currentMetaData.singletonFrame.get() != null;
                        lookupResult = new FrameAndSlotLookupResult(identifier.toString(), currentMetaData.singletonFrame.get(), slot);
                    }
                }
                addPreviousLookups(frame, current, identifier);
                metaData.lookupResults.put(identifier, new WeakReference<>(lookupResult));
                return lookupResult;
            }
            Frame next = RArguments.getEnclosingFrame(current);
            assert isEnclosingFrameDescriptor(current, next) : "the enclosing frame descriptor assumptions do not match the actual enclosing frame descriptor: " + getMetaData(current).name + " -> " +
                            getMetaData(next).name;
            if (next == null) {
                // leave "current" if we hit the empty env
                break;
            }
            current = next;
        }
        // not frame slot found: missing value
        addPreviousLookups(frame, current, identifier);
        LookupResult lookupResult = new MissingLookupResult(identifier.toString());
        metaData.lookupResults.put(identifier, new WeakReference<>(lookupResult));
        return lookupResult;
    }

    private static void addPreviousLookups(Frame from, Frame to, Object identifier) {
        Frame mark = from;
        while (true) {
            FrameDescriptorMetaData lookupMetaData = getMetaData(mark);
            lookupMetaData.previousLookups.add(identifier);
            if (mark == to) {
                break;
            }
            mark = RArguments.getEnclosingFrame(mark);
        }
    }

    private static boolean isEnclosingFrameDescriptor(Frame current, Frame next) {
        assert current != null;
        FrameDescriptorMetaData metaData = getMetaData(current);
        FrameDescriptor nextDesc = next == null ? null : handleBaseNamespaceEnv(next);
        return metaData.getEnclosingFrameDescriptor() == nextDesc;
    }

    private static synchronized void invalidateNames(FrameDescriptorMetaData metaData, Collection<Object> identifiers) {
        if (metaData.previousLookups.removeAll(identifiers)) {
            for (Object identifier : identifiers) {
                WeakReference<LookupResult> result = metaData.lookupResults.remove(identifier);
                if (result != null) {
                    LookupResult lookup = result.get();
                    if (lookup != null) {
                        lookup.invalidate();
                    }
                }
            }
            for (FrameDescriptor descriptor : metaData.subDescriptors) {
                FrameDescriptorMetaData sub = getMetaData(descriptor);
                invalidateNames(sub, identifiers);
            }
        }
    }

    /**
     * Special handling (return a marker frame descriptor) for the namespace:base environment.
     */
    private static FrameDescriptor handleBaseNamespaceEnv(Frame frame) {
        return frame == null ? null : frame instanceof NSBaseMaterializedFrame ? ((NSBaseMaterializedFrame) frame).getMarkerFrameDescriptor() : frame.getFrameDescriptor();
    }

    private static FrameDescriptorMetaData getMetaData(FrameDescriptor descriptor) {
        FrameDescriptorMetaData result = frameDescriptors.get(descriptor);
        assert result != null : "null metadata for " + descriptor;
        return result;
    }

    private static FrameDescriptorMetaData getMetaData(Frame frame) {
        return getMetaData(handleBaseNamespaceEnv(frame));
    }

    private static FrameDescriptorMetaData getDescriptorMetaData(FrameDescriptor descriptor) {
        assert descriptor != null : "initializing enclosing of null descriptor";
        FrameDescriptorMetaData target = getMetaData(descriptor);
        assert target != null : "frame descriptor wasn't registered properly";
        return target;
    }

    public static synchronized boolean isEnclosingFrameDescriptor(FrameDescriptor descriptor, Frame newEnclosingFrame) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptorMetaData target = getDescriptorMetaData(descriptor);
        FrameDescriptor newEnclosingDescriptor = handleBaseNamespaceEnv(newEnclosingFrame);
        return target.getEnclosingFrameDescriptor() == newEnclosingDescriptor;
    }

    public static synchronized void initializeEnclosingFrame(FrameDescriptor descriptor, Frame newEnclosingFrame) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptorMetaData target = getDescriptorMetaData(descriptor);

        FrameDescriptor newEnclosingDescriptor = handleBaseNamespaceEnv(newEnclosingFrame);

        // this function can be called multiple times with the same enclosing descriptor
        if (target.getEnclosingFrameDescriptor() != newEnclosingDescriptor) {
            assert target.getEnclosingFrameDescriptor() == null : "existing enclosing descriptor while initializing " + target.name;
            assert target.lookupResults.isEmpty() : "existing lookup results while initializing " + target.name;

            target.updateEnclosingFrameDescriptor(newEnclosingDescriptor);
            if (newEnclosingDescriptor != null) {
                FrameDescriptorMetaData newEnclosing = getMetaData(newEnclosingDescriptor);
                newEnclosing.subDescriptors.add(descriptor);
            }
        }
    }

    public static synchronized void initializeEnclosingFrame(Frame frame, Frame newEnclosingFrame) {
        initializeEnclosingFrame(handleBaseNamespaceEnv(frame), newEnclosingFrame);
    }

    private static synchronized void setEnclosingFrame(FrameDescriptor descriptor, MaterializedFrame newEnclosingFrame, MaterializedFrame oldEnclosingFrame) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptorMetaData target = getMetaData(descriptor);
        assert target != null : "frame descriptor wasn't registered properly for " + descriptor;

        // invalidate existing lookups
        invalidateAllNames(target);

        FrameDescriptor oldEnclosingDescriptor = target.getEnclosingFrameDescriptor();
        assert (oldEnclosingDescriptor == null) == (oldEnclosingFrame == null) : "mismatch " + oldEnclosingDescriptor + " / " + oldEnclosingFrame;

        if (oldEnclosingDescriptor != null) {
            assert oldEnclosingDescriptor == oldEnclosingFrame.getFrameDescriptor() : "mismatch " + oldEnclosingDescriptor + " / " + oldEnclosingFrame.getFrameDescriptor();
            FrameDescriptorMetaData oldEnclosing = getMetaData(oldEnclosingDescriptor);
            oldEnclosing.subDescriptors.remove(descriptor);
        }
        FrameDescriptor newEnclosingDescriptor = handleBaseNamespaceEnv(newEnclosingFrame);
        target.updateEnclosingFrameDescriptor(newEnclosingDescriptor);

        if (newEnclosingDescriptor != null) {
            FrameDescriptorMetaData newEnclosing = getMetaData(newEnclosingDescriptor);
            assert !newEnclosing.name.equals("global") || !target.name.equals("base");
            newEnclosing.subDescriptors.add(descriptor);
        }
    }

    public static synchronized void setEnclosingFrame(Frame frame, MaterializedFrame newEnclosingFrame, MaterializedFrame oldEnclosingFrame) {
        setEnclosingFrame(handleBaseNamespaceEnv(frame), newEnclosingFrame, oldEnclosingFrame);
    }

    private static void invalidateAllNames(FrameDescriptorMetaData target) {
        for (Map.Entry<Object, WeakReference<LookupResult>> entry : target.lookupResults.entrySet()) {
            LookupResult lookup = entry.getValue().get();
            if (lookup != null) {
                lookup.invalidate();
            }
        }
        target.lookupResults.clear();
        if (!target.previousLookups.isEmpty()) {
            target.previousLookups.clear();
            for (FrameDescriptor sub : target.subDescriptors) {
                invalidateAllNames(getMetaData(sub));
            }
        }
    }

    public static synchronized void detach(Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptorMetaData position = getMetaData(frame);
        FrameDescriptor oldEnclosingDescriptor = position.getEnclosingFrameDescriptor();
        FrameDescriptorMetaData oldEnclosing = getMetaData(oldEnclosingDescriptor);
        FrameDescriptor newEnclosingDescriptor = oldEnclosing.getEnclosingFrameDescriptor();
        FrameDescriptorMetaData newEnclosing = getMetaData(newEnclosingDescriptor);

        invalidateNames(oldEnclosing, oldEnclosingDescriptor.getIdentifiers());

        position.updateEnclosingFrameDescriptor(newEnclosingDescriptor);
        oldEnclosing.updateEnclosingFrameDescriptor(null);
        oldEnclosing.subDescriptors.remove(frame.getFrameDescriptor());
        newEnclosing.subDescriptors.remove(oldEnclosingDescriptor);
        newEnclosing.subDescriptors.add(frame.getFrameDescriptor());
    }

    public static synchronized void attach(Frame frame, Frame newEnclosingFrame) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptorMetaData position = getMetaData(frame);
        FrameDescriptorMetaData newEnclosing = getMetaData(newEnclosingFrame);
        FrameDescriptor oldEnclosingDescriptor = position.getEnclosingFrameDescriptor();
        FrameDescriptorMetaData oldEnclosing = getMetaData(oldEnclosingDescriptor);

        invalidateAllNames(newEnclosing);
        invalidateNames(position, newEnclosingFrame.getFrameDescriptor().getIdentifiers());

        newEnclosing.previousLookups.clear();
        newEnclosing.previousLookups.addAll(oldEnclosing.previousLookups);

        position.updateEnclosingFrameDescriptor(newEnclosingFrame.getFrameDescriptor());
        newEnclosing.updateEnclosingFrameDescriptor(oldEnclosingDescriptor);
        assert frame.getFrameDescriptor() == handleBaseNamespaceEnv(frame);
        assert !newEnclosing.name.equals("global") || !position.name.equals("base");
        newEnclosing.subDescriptors.add(frame.getFrameDescriptor());
        oldEnclosing.subDescriptors.remove(frame.getFrameDescriptor());
        oldEnclosing.subDescriptors.add(newEnclosingFrame.getFrameDescriptor());
    }

    private static final int MAX_INVALIDATION_COUNT = 2;
    private static final int MAX_GLOBAL_ENV_INVALIDATION_COUNT = 1;

    @SuppressWarnings("unused")
    private static void out(String format, Object... args) {
        // System.out.println(String.format(format, args));
    }

    private static final class FrameSlotInfoImpl {
        /**
         * This is meant to monitor updates performed on {@link FrameSlot}. Each {@link FrameSlot}
         * holds an {@link Assumption} in it's "info" field; it is valid as long as no non-local
         * update has ever taken place.<br/>
         * The background to this rather strange assumption is that non-local reads are very hard to
         * keep track of thanks to R powerful language features. To keep the maintenance for the
         * assumption as cheap as possible, it checks only local reads - which is fast - and does a
         * more costly check on "<<-" but invalidates the assumption as soon as "eval" and the like
         * comes into play.<br/>
         */
        private final Assumption nonLocalModifiedAssumption = Truffle.getRuntime().createAssumption();

        @CompilationFinal private StableValue<Object> stableValue;
        private int invalidationCount;

        // TODO @CompilationFinal ?
        @CompilationFinal private boolean activeBinding;

        FrameSlotInfoImpl(boolean isSingletonFrame, boolean isGlobalEnv, Object identifier) {
            if (isSingletonFrame) {
                stableValue = new StableValue<>(null, identifier.toString());
                invalidationCount = isGlobalEnv ? MAX_GLOBAL_ENV_INVALIDATION_COUNT : MAX_INVALIDATION_COUNT;
            } else {
                stableValue = null;
            }
        }

        public boolean needsInvalidation() {
            return stableValue != null;
        }

        /*
         * Special cases for primitive types to force value (instead of identity) comparison.
         */

        public void setValue(byte value, FrameSlot slot) {
            if (stableValue != null && (!(stableValue.getValue() instanceof Byte) || ((byte) stableValue.getValue()) != value)) {
                invalidateStableValue(value, slot);
            }
        }

        public void setValue(int value, FrameSlot slot) {
            if (stableValue != null && (!(stableValue.getValue() instanceof Integer) || ((int) stableValue.getValue()) != value)) {
                invalidateStableValue(value, slot);
            }
        }

        public void setValue(double value, FrameSlot slot) {
            if (stableValue != null && (!(stableValue.getValue() instanceof Double) || ((double) stableValue.getValue()) != value)) {
                invalidateStableValue(value, slot);
            }
        }

        public void setValue(Object value, FrameSlot slot) {
            if (stableValue != null && stableValue.getValue() != value) {
                invalidateStableValue(value, slot);
            }
            activeBinding = ActiveBinding.isActiveBinding(value);
        }

        private void invalidateStableValue(Object value, FrameSlot slot) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stableValue.getAssumption().invalidate();
            if (invalidationCount > 0) {
                invalidationCount--;
                out("setting singleton value %s = %s", slot.getIdentifier(), value == null ? "null" : value.getClass());
                stableValue = new StableValue<>(value, String.valueOf(slot.getIdentifier()));
            } else {
                out("setting non-singleton value %s", slot.getIdentifier());
                stableValue = null;
            }
        }

        public StableValue<Object> getStableValue() {
            return stableValue;
        }

        public boolean isActiveBinding() {
            return activeBinding;
        }
    }

    /**
     * Retrieves the not-changed-locally {@link Assumption} for the given frame slot.
     *
     * @return The "not changed locally" assumption of the given {@link FrameSlot}
     */
    public static Assumption getNotChangedNonLocallyAssumption(FrameSlot slot) {
        return getFrameSlotInfo(slot).nonLocalModifiedAssumption;
    }

    private static FrameSlotInfoImpl getFrameSlotInfo(FrameSlot slot) {
        Object info = slot.getInfo();
        if (!(info instanceof FrameSlotInfoImpl)) {
            CompilerDirectives.transferToInterpreter();
            throw RInternalError.shouldNotReachHere("Each FrameSlot should hold a FrameSlotInfo in its info field!");
        }
        return (FrameSlotInfoImpl) info;
    }

    // methods for creating new frame slots

    public static synchronized FrameSlot findOrAddFrameSlot(FrameDescriptor fd, Object identifier, FrameSlotKind initialKind) {
        CompilerAsserts.neverPartOfCompilation();
        FrameSlot frameSlot = fd.findFrameSlot(identifier);
        if (frameSlot != null) {
            return frameSlot;
        } else {
            FrameDescriptorMetaData metaData = getMetaData(fd);
            invalidateNames(metaData, Arrays.asList(identifier));
            return fd.addFrameSlot(identifier, new FrameSlotInfoImpl(metaData.singletonFrame != null, "global".equals(metaData.name), identifier), initialKind);
        }
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

        if (getNotChangedNonLocallyAssumption(slot).isValid()) {
            // Check whether current frame is used outside a regular stack
            if (isNonLocal || RArguments.getIsIrregular(curFrame)) {
                // False positive: Also invalidates a slot in the current active frame if that one
                // is used inside eval or the like, but this cost is definitely negligible.
                if (invalidateProfile != null) {
                    invalidateProfile.enter();
                }
                getNotChangedNonLocallyAssumption(slot).invalidate();
            }
        }
    }

    public static void setByteAndInvalidate(Frame frame, FrameSlot frameSlot, byte newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        frame.setByte(frameSlot, newValue);
        FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
        if (info.needsInvalidation()) {
            info.setValue(newValue, frameSlot);
        }
        checkAndInvalidate(frame, frameSlot, isNonLocal, invalidateProfile);
    }

    public static void setIntAndInvalidate(Frame frame, FrameSlot frameSlot, int newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        frame.setInt(frameSlot, newValue);
        FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
        if (info.needsInvalidation()) {
            info.setValue(newValue, frameSlot);
        }
        checkAndInvalidate(frame, frameSlot, isNonLocal, invalidateProfile);
    }

    public static void setDoubleAndInvalidate(Frame frame, FrameSlot frameSlot, double newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        frame.setDouble(frameSlot, newValue);
        FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
        if (info.needsInvalidation()) {
            info.setValue(newValue, frameSlot);
        }
        checkAndInvalidate(frame, frameSlot, isNonLocal, invalidateProfile);
    }

    public static void setObjectAndInvalidate(Frame frame, FrameSlot frameSlot, Object newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        assert !ActiveBinding.isActiveBinding(newValue);
        frame.setObject(frameSlot, newValue);
        FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
        if (info.needsInvalidation()) {
            info.setValue(newValue, frameSlot);
        }
        checkAndInvalidate(frame, frameSlot, isNonLocal, invalidateProfile);
    }

    public static void setActiveBinding(Frame frame, FrameSlot frameSlot, ActiveBinding newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        frame.setObject(frameSlot, newValue);
        FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
        if (info.needsInvalidation()) {
            info.setValue(newValue, frameSlot);
        }
        checkAndInvalidate(frame, frameSlot, isNonLocal, invalidateProfile);
        getContainsNoActiveBindingAssumption(frame.getFrameDescriptor()).invalidate();
    }

    /**
     * Initializes the internal data structures for a newly created frame descriptor that is
     * intended to be used for a non-function frame (and thus will only ever be used for one frame).
     */
    public static synchronized void initializeNonFunctionFrameDescriptor(String name, MaterializedFrame frame) {
        frameDescriptors.put(handleBaseNamespaceEnv(frame), new FrameDescriptorMetaData(name, frame));
    }

    public static synchronized void initializeFunctionFrameDescriptor(String name, FrameDescriptor frameDescriptor) {
        frameDescriptors.put(frameDescriptor, new FrameDescriptorMetaData(name, null));
    }

    public static synchronized Assumption getEnclosingFrameDescriptorAssumption(FrameDescriptor descriptor) {
        CompilerAsserts.neverPartOfCompilation();
        return frameDescriptors.get(descriptor).getEnclosingFrameDescriptorAssumption();
    }

    public static synchronized Assumption getContainsNoActiveBindingAssumption(FrameDescriptor descriptor) {
        CompilerAsserts.neverPartOfCompilation();
        final FrameDescriptorMetaData frameDescriptorMetaData = frameDescriptors.get(descriptor);
        return frameDescriptorMetaData != null ? frameDescriptorMetaData.getContainsNoActiveBindingAssumption() : null;
    }

    public static synchronized boolean isContainsNoActiveBindingAssumptionValid(FrameDescriptor descriptor) {
        Assumption assumption = getContainsNoActiveBindingAssumption(descriptor);
        return assumption != null ? assumption.isValid() : true;
    }

    public static synchronized StableValue<Object> getStableValueAssumption(FrameDescriptor descriptor, FrameSlot frameSlot, Object value) {
        CompilerAsserts.neverPartOfCompilation();
        StableValue<Object> stableValue = getFrameSlotInfo(frameSlot).getStableValue();
        if (stableValue != null) {
            assert getMetaData(descriptor).singletonFrame != null : "single frame slot within non-singleton descriptor";
            assert stableValue.getValue() == value || (stableValue.getValue() != null && (stableValue.getValue().equals(value) || !stableValue.getAssumption().isValid())) : stableValue.getValue() +
                            " vs. " + value;
        }
        return stableValue;
    }

    public static synchronized MaterializedFrame getSingletonFrame(FrameDescriptor descriptor) {
        WeakReference<MaterializedFrame> singleton = getMetaData(descriptor).singletonFrame;
        return singleton == null ? null : singleton.get();
    }

    public static boolean isValidFrameDescriptor(FrameDescriptor frameDesc) {
        return getMetaData(frameDesc) != null;
    }

    public static boolean isActiveBinding(FrameSlot slot) {
        return ((FrameSlotInfoImpl) slot.getInfo()).isActiveBinding();
    }
}
