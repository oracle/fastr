/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.env.frame;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RLocale;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;

/**
 * Variant of {@link REnvFrameAccess} that provides access to an actual Truffle execution frame.
 * All the slots used within the frame that corresponds to the environment are auxiliary slots.
 */
public final class REnvTruffleFrameAccess extends REnvFrameAccess {

    private final MaterializedFrame frame;
    /**
     * Records which bindings are locked. In normal use we don't expect any bindings to be locked so
     * this set is allocated lazily.
     */
    private Set<String> lockedBindings;

    public REnvTruffleFrameAccess(MaterializedFrame frame) {
        this.frame = frame;
    }

    @Override
    public MaterializedFrame getFrame() {
        return frame;
    }

    @Override
    public Object get(String key) {
        CompilerAsserts.neverPartOfCompilation();
        int frameIndex = FrameSlotChangeMonitor.getIndexOfIdentifier(frame.getFrameDescriptor(), key);
        if (FrameIndex.isUninitializedIndex(frameIndex)) {
            return null;
        } else {
            Object value = FrameSlotChangeMonitor.getValue(frame, frameIndex);
            // special treatment for active binding: call bound function
            if (ActiveBinding.isActiveBinding(value)) {
                Object readValue = ((ActiveBinding) value).readValue();
                // special case: if the active binding returns RMissing, then this should behave
                // like the variable does not exist.
                return readValue != RMissing.instance ? readValue : null;
            }
            return value;
        }
    }

    @Override
    public boolean isActiveBinding(String key) {
        CompilerAsserts.neverPartOfCompilation();
        int frameIndex = FrameSlotChangeMonitor.getIndexOfIdentifier(frame.getFrameDescriptor(), key);
        if (FrameIndex.isUninitializedIndex(frameIndex)) {
            return false;
        } else {
            Object value = FrameSlotChangeMonitor.getValue(frame, frameIndex);
            return ActiveBinding.isActiveBinding(value);
        }
    }

    @Override
    public void put(String key, Object value) throws PutException {
        CompilerAsserts.neverPartOfCompilation();
        assert key != null;
        assert value != null;
        if (lockedBindings != null && lockedBindings.contains(key)) {
            throw new PutException(RError.Message.ENV_CHANGE_BINDING, key);
        }
        FrameSlotKind valueSlotKind = RRuntime.getSlotKind(value);
        FrameDescriptor fd = frame.getFrameDescriptor();

        int frameIndex = FrameSlotChangeMonitor.findOrAddAuxiliaryFrameSlot(fd, key);
        if (valueSlotKind != FrameSlotChangeMonitor.getFrameSlotKind(fd, frameIndex)) {
            // we must not toggle between slot kinds, so go to Object
            valueSlotKind = FrameSlotKind.Object;
        }

        switch (valueSlotKind) {
            case Byte:
                FrameSlotChangeMonitor.setByteAndInvalidate(frame, frameIndex, (byte) value, false, null);
                break;
            case Int:
                FrameSlotChangeMonitor.setIntAndInvalidate(frame, frameIndex, (int) value, false, null);
                break;
            case Double:
                FrameSlotChangeMonitor.setDoubleAndInvalidate(frame, frameIndex, (double) value, false, null);
                break;
            case Object:
                Object object;
                try {
                    object = FrameSlotChangeMonitor.getObject(frame, frameIndex);
                } catch (FrameSlotTypeException e) {
                    object = null;
                }

                if (object != null && ActiveBinding.isActiveBinding(object)) {
                    ((ActiveBinding) object).writeValue(value);
                } else {
                    FrameSlotChangeMonitor.setObjectAndInvalidate(frame, frameIndex, value, false, null);
                }
                break;
            case Illegal:
                break;
            default:
                throw new PutException(Message.GENERIC, "frame slot exception");
        }
    }

    @Override
    public void rm(String key) throws PutException {
        CompilerAsserts.neverPartOfCompilation();
        assert key != null;
        if (lockedBindings != null) {
            lockedBindings.remove(key);
        }
        FrameDescriptor fd = frame.getFrameDescriptor();
        if (!FrameSlotChangeMonitor.containsIdentifier(fd, key)) {
            throw new PutException(RError.Message.UNKNOWN_OBJECT, key);
        } else {
            int frameIndex = FrameSlotChangeMonitor.getIndexOfIdentifier(fd, key);
            if (FrameSlotChangeMonitor.getFrameSlotKind(fd, frameIndex) != FrameSlotKind.Object) {
                FrameSlotChangeMonitor.setFrameSlotKind(fd, frameIndex, FrameSlotKind.Object);
            }

            Assumption containsNoActiveBindingAssumption = FrameSlotChangeMonitor.getContainsNoActiveBindingAssumption(fd);
            Object result = null;
            // special treatment for active binding: call bound function
            try {
                if (!containsNoActiveBindingAssumption.isValid() && ActiveBinding.isActiveBinding(result = FrameSlotChangeMonitor.getObject(frame, frameIndex))) {
                    ActiveBinding binding = (ActiveBinding) result;
                    if (binding.isHidden()) {
                        binding.setInitialized(false);
                        return;
                    }
                }
            } catch (FrameSlotTypeException e) {
                // ignore
            }

            FrameSlotChangeMonitor.setObjectAndInvalidate(frame, frameIndex, null, false, null);
        }
    }

    @Override
    @TruffleBoundary
    public RStringVector ls(boolean allNames, Pattern pattern, boolean sorted) {
        FrameDescriptor fd = frame.getFrameDescriptor();

        ArrayList<String> names = new ArrayList<>(fd.getNumberOfAuxiliarySlots());
        getStringIdentifiersAndValues(frame, names, null);

        ArrayList<String> matchedNamesList = new ArrayList<>(fd.getNumberOfAuxiliarySlots());
        for (String name : names) {
            if (REnvironment.includeName(name, allNames, pattern)) {
                matchedNamesList.add(name);
            }
        }
        String[] data = matchedNamesList.toArray(new String[0]);
        if (sorted) {
            Locale locale = RContext.getInstance().stateRLocale.getLocale(RLocale.COLLATE);
            Collator collator = locale == Locale.ROOT || locale == null ? null : RLocale.getOrderCollator(locale);
            Arrays.sort(data, (o1, o2) -> RLocale.compare(collator, o1, o2));
        }
        return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
    }

    @Override
    @TruffleBoundary
    public boolean bindingIsLocked(String key) {
        return lockedBindings != null && lockedBindings.contains(key);
    }

    @Override
    @TruffleBoundary
    public void lockBindings() {
        for (Map.Entry<Object, Integer> entry : frame.getFrameDescriptor().getAuxiliarySlots().entrySet()) {
            Object identifier = entry.getKey();
            if (identifier instanceof String) {
                lockBinding((String) identifier);
            }
        }
    }

    @Override
    @TruffleBoundary
    public void lockBinding(String key) {
        if (lockedBindings == null) {
            lockedBindings = new HashSet<>();
        }
        lockedBindings.add(key);
    }

    @Override
    @TruffleBoundary
    public void unlockBinding(String key) {
        if (lockedBindings != null) {
            lockedBindings.remove(key);
        }
    }

    public static void getStringIdentifiersAndValues(Frame frame, List<String> names, List<Object> values) {
        assert names != null;
        FrameDescriptor fd = frame.getFrameDescriptor();
        for (Object identifier : FrameSlotChangeMonitor.getIdentifiers(fd)) {
            if (identifier instanceof String) {
                int frameIndex = FrameSlotChangeMonitor.getIndexOfIdentifier(fd, identifier);
                Object value = FrameSlotChangeMonitor.getValue(frame, frameIndex);
                if (value == null || !ActiveBinding.isListed(value)) {
                    continue;
                }
                names.add((String) identifier);
                if (values != null) {
                    values.add(value);
                }
            }
        }
    }
}
