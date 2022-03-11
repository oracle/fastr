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

import static com.oracle.truffle.r.runtime.context.FastROptions.SearchPathForcePromises;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.Message;
import com.oracle.truffle.api.library.ReflectionLibrary;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.StableValue;
import com.oracle.truffle.r.runtime.context.ChildContextInfo;
import com.oracle.truffle.r.runtime.context.FastROptions;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.RUnboundValue;

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

    public static final class FrameAndIndexLookupResult extends LookupResult {
        private final MaterializedFrame frame;
        private final int frameIndex;

        private FrameAndIndexLookupResult(String identifier, MaterializedFrame frame, int frameIndex) {
            super(identifier);
            this.frame = frame;
            this.frameIndex = frameIndex;
        }

        @Override
        public Object getValue() {
            // fast path execution should use getFrame / getSlot
            CompilerAsserts.neverPartOfCompilation("FrameAndIndexLookupResult.getValue() should not be used in fast path execution");
            return FrameSlotChangeMonitor.getObject(frame, frameIndex);
        }

        public MaterializedFrame getFrame() throws InvalidAssumptionException {
            assumption.check();
            return frame;
        }

        public int getFrameIndex() throws InvalidAssumptionException {
            assumption.check();
            return frameIndex;
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
        // TODO: @CompilationFinal?
        private WeakReference<MaterializedFrame> singletonFrame;
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

        /**
         * Mapping of identifiers to indexes.
         */
        private final Map<Object, Integer> indexes = new ConcurrentHashMap<>();
        /**
         * List of frame slot infos for auxiliary slots. The indexes into this list correspond to indexes into
         * auxiliary slots in a frame.
         */
        private final List<FrameSlotInfo> auxSlotInfos = new ArrayList<>();

        private WeakReference<FrameDescriptor> enclosingFrameDescriptor = new WeakReference<>(null);
        private Assumption enclosingFrameDescriptorAssumption = Truffle.getRuntime().createAssumption("enclosing frame descriptor");
        private final Assumption containsNoActiveBindingAssumption = Truffle.getRuntime().createAssumption("contains no active binding");

        private FrameDescriptorMetaData(String name, MaterializedFrame singletonFrame) {
            this.name = name;
            this.singletonFrame = singletonFrame == null ? null : new WeakReference<>(singletonFrame);
        }

        private FrameDescriptorMetaData(String name) {
            this(name, null);
        }

        public void setSingletonFrame(MaterializedFrame singletonFrame) {
            this.singletonFrame = new WeakReference<>(singletonFrame);
        }

        public void addIndex(Object identifier, int index) {
            CompilerAsserts.neverPartOfCompilation();
            indexes.put(identifier, index);
        }

        public Integer getIndex(Object identifier) {
            return indexes.get(identifier);
        }
        
        public FrameSlotInfo getAuxiliarySlotInfo(int auxSlotIdx) {
            return auxSlotInfos.get(auxSlotIdx);
        }

        public void addAuxSlotInfo(FrameSlotInfo slotInfo) {
            auxSlotInfos.add(slotInfo);
        }

        // TODO: Make more performant
        public Object getIdentifier(int frameIndex) {
            CompilerAsserts.neverPartOfCompilation();
            for (Map.Entry<Object, Integer> entry : indexes.entrySet()) {
                if (entry.getValue() == frameIndex) {
                    return entry.getKey();
                }
            }
            return null;
        }

        public Collection<Object> getIdentifiers() {
            CompilerAsserts.neverPartOfCompilation();
            return indexes.keySet();
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

        @TruffleBoundary
        @Override
        public String toString() {
            return "FrameDescriptorMetaData{" +
                            "@" + hashCode() +
                            ", name='" + name + '\'' +
                            ", previousLookups=" + previousLookups +
                            '}';
        }
    }

    /**
     * This function tries to fulfill the lookup for the given name in the given frame based only on
     * the static knowledge about the frame descriptor hierarchy and stable bindings. Returns
     * {@code null} in case this was not possible.
     */
    public static LookupResult lookup(Frame frame, Object identifier) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptorMetaData metaData = getDescriptorMetadata(frame);
        WeakReference<LookupResult> weakResult = metaData.lookupResults.get(identifier);
        LookupResult result = weakResult == null ? null : weakResult.get();
        if (result != null && result.isValid()) {
            return result;
        }
        Frame current = frame;
        while (true) {
            int frameIndex = FrameSlotChangeMonitor.getIndexOfIdentifier(current.getFrameDescriptor(), identifier);
            if (!FrameIndex.isUninitializedIndex(frameIndex)) {
                LookupResult lookupResult;
                StableValue<Object> stableValue = getFrameSlotInfo(current, frameIndex).stableValue;
                // if stableValue.getValue() == null, then this is a frame slot that doesn't have a
                // value, which can happen, e.g., when package creates a value in its namespace, but
                // then removes it in .onLoad
                if (stableValue == null || stableValue.getValue() != null) {
                    if (stableValue != null) {
                        lookupResult = new StableValueLookupResult(identifier.toString(), stableValue);
                    } else {
                        FrameDescriptorMetaData currentMetaData = getDescriptorMetadata(current);
                        if (currentMetaData.singletonFrame == null) {
                            // no stable value and no singleton frame
                            return null;
                        } else {
                            assert currentMetaData.singletonFrame.get() != null;
                            lookupResult = new FrameAndIndexLookupResult(identifier.toString(), currentMetaData.singletonFrame.get(), frameIndex);
                        }
                    }
                    addPreviousLookups(frame, current, identifier);
                    metaData.lookupResults.put(identifier, new WeakReference<>(lookupResult));
                    return lookupResult;
                }
            }
            Frame next = RArguments.getEnclosingFrame(current);
            /*
             * The following condition used to be an assertion, but it was turned into a condition
             * as the initialization procedure of the processx package breaks it by changing the
             * parent environment of the current one, i.e. by executing:
             *
             * env <- environment(); parent.env(env) <- baseenv()
             *
             * in errors.R.
             *
             * The official documentation of parent.env<- discourages from using it as it is
             * considered extremely dangerous. The builtin parent.env<- can also be removed in the
             * near future.
             */
            if (!isEnclosingFrameDescriptor(current, next)) {
                return null;
            }
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

    public static boolean isValidFrameDescriptor(FrameDescriptor frameDescriptor) {
        FrameDescriptorMetaData metadata = getDescriptorMetadata(frameDescriptor);
        int normalSlotsCount = frameDescriptor.getNumberOfSlots();
        int auxSlotsCount = frameDescriptor.getNumberOfAuxiliarySlots();
        int totalSlotsCount = normalSlotsCount + auxSlotsCount;
        if (metadata.indexes.size() != totalSlotsCount) {
            return false;
        }
        for (Map.Entry<Object, Integer> entry : metadata.indexes.entrySet()) {
            Object identifier = entry.getKey();
            int frameIndex = entry.getValue();
            if (!isFrameIndexInBounds(frameDescriptor, frameIndex)) {
                return false;
            }
            if (FrameIndex.representsNormalIndex(frameIndex)) {
                int normalSlotIdx = FrameIndex.toNormalIndex(frameIndex);
                Object slotInfo = frameDescriptor.getSlotInfo(normalSlotIdx);
                if (!(slotInfo instanceof FrameSlotInfo)) {
                    return false;
                }
                if (((FrameSlotInfo) slotInfo).identifier != identifier) {
                    return false;
                }
                Object slotName = frameDescriptor.getSlotName(normalSlotIdx);
                if (!(slotName instanceof String) && !(slotName instanceof RFrameSlot)) {
                    return false;
                }
                if (slotName != identifier) {
                    return false;
                }
            } else {
                assert FrameIndex.representsAuxiliaryIndex(frameIndex);
                int auxSlotIdx = FrameIndex.toAuxiliaryIndex(frameIndex);
                FrameSlotInfo slotInfo = metadata.getAuxiliarySlotInfo(auxSlotIdx);
                if (slotInfo.identifier != identifier) {
                    return false;
                }
            }
        }
        for (Map.Entry<Object, Integer> entry : frameDescriptor.getAuxiliarySlots().entrySet()) {
            Object identifier = entry.getKey();
            int auxSlotIdx = entry.getValue();
            int auxSlotIdxFromMetata = FrameIndex.toAuxiliaryIndex(metadata.getIndex(identifier));
            if (auxSlotIdx != auxSlotIdxFromMetata) {
                return false;
            }
        }
        // Check auxSlotInfos
        if (metadata.auxSlotInfos.size() != frameDescriptor.getNumberOfAuxiliarySlots()) {
            return false;
        }
        for (FrameSlotInfo slotInfo : metadata.auxSlotInfos) {
            if (slotInfo.identifier == null) {
                return false;
            }
        }
        return true;
    }

    public static FrameDescriptor createUninitializedFrameDescriptor(String name) {
        return FrameDescriptor.newBuilder().info(new FrameDescriptorMetaData(name)).build();
    }

    /**
     * Creates a {@link FrameDescriptor} for either environment or a function.
     *
     * @param name Name for debug purposes.
     * @param singletonFrame Null for function descriptors, not null for environment descriptors.
     */
    public static FrameDescriptor createFrameDescriptor(String name, MaterializedFrame singletonFrame) {
        return FrameDescriptor.newBuilder().info(new FrameDescriptorMetaData(name, singletonFrame)).build();
    }

    public static FrameDescriptor createFunctionFrameDescriptor(String name) {
        return createFrameDescriptor(name, null);
    }

    /**
     * Creates a {@link FrameDescriptor} with normal indexed slots.
     *
     * @param name Name for debug purposes.
     * @param singletonFrame
     * @param kinds Kind of normal slots to be initialized in the frame descriptor.
     * @param identifiers Identifiers of normal slots
     */
    public static FrameDescriptor createFrameDescriptor(String name, MaterializedFrame singletonFrame, FrameSlotKind[] kinds, Object[] identifiers) {
        assert kinds.length == identifiers.length;
        throw new UnsupportedOperationException("unimplemented");
    }

    public static FrameDescriptor copyFrameDescriptorWithMetadata(FrameDescriptor frameDescriptor) {
        FrameDescriptorMetaData metadata = getDescriptorMetadata(frameDescriptor);
        MaterializedFrame singletonFrame = metadata.singletonFrame != null ? metadata.singletonFrame.get() : null;
        FrameDescriptorMetaData newMetadata = new FrameDescriptorMetaData(metadata.name, singletonFrame);
        FrameDescriptor.Builder newDescriptorBuilder = FrameDescriptor.newBuilder();
        newDescriptorBuilder.info(newMetadata);
        // Copy indexed (normal) slots
        for (int i = 0; i < frameDescriptor.getNumberOfSlots(); i++) {
            newDescriptorBuilder.addSlot(frameDescriptor.getSlotKind(i), frameDescriptor.getSlotName(i), frameDescriptor.getSlotInfo(i));
        }
        FrameDescriptor newDescriptor = newDescriptorBuilder.build();
        // Copy auxiliary slots
        for (Map.Entry<Object, Integer> entry : frameDescriptor.getAuxiliarySlots().entrySet()) {
            // entry values may be different, but that would not matter.
            findOrAddAuxiliaryFrameSlot(newDescriptor, entry.getKey());
        }
        return newDescriptor;
    }

    public static Collection<Object> getIdentifiers(FrameDescriptor frameDescriptor) {
        assert isValidFrameDescriptor(frameDescriptor);
        return getDescriptorMetadata(frameDescriptor).getIdentifiers();
    }

    public static boolean containsIdentifier(FrameDescriptor frameDescriptor, Object identifier) {
        assert isValidFrameDescriptor(frameDescriptor);
        FrameDescriptorMetaData metadata = getDescriptorMetadata(frameDescriptor);
        Integer frameIndex = metadata.getIndex(identifier);
        return frameIndex != null;
    }

    public static boolean containsIndex(Frame frame, int frameIndex) {
        assert isValidFrameDescriptor(frame.getFrameDescriptor());
        return containsIndex(frame.getFrameDescriptor(), frameIndex);
    }

    public static boolean containsIndex(FrameDescriptor frameDescriptor, int frameIndex) {
        assert isValidFrameDescriptor(frameDescriptor);
        if (FrameIndex.representsAuxiliaryIndex(frameIndex)) {
            int auxSlotIdx = FrameIndex.toAuxiliaryIndex(frameIndex);
            return 0 <= auxSlotIdx && auxSlotIdx < frameDescriptor.getNumberOfAuxiliarySlots();
        } else {
            int normalSlotIdx = FrameIndex.toNormalIndex(frameIndex);
            return 0 <= normalSlotIdx && normalSlotIdx < frameDescriptor.getNumberOfSlots();
        }
    }

    /**
     * Returns index of the identifier.
     * 
     * @return null if the given identifier is not in the frame descriptor
     */
    public static int getIndexOfIdentifier(FrameDescriptor frameDescriptor, Object identifier) {
        assert isValidFrameDescriptor(frameDescriptor);
        FrameDescriptorMetaData descriptorMetadata = getDescriptorMetadata(frameDescriptor);
        Integer index = descriptorMetadata.getIndex(identifier);
        return index == null ? FrameIndex.UNITIALIZED_INDEX : index;
    }

    private static void addPreviousLookups(Frame from, Frame to, Object identifier) {
        Frame mark = from;
        while (true) {
            FrameDescriptorMetaData lookupMetaData = getDescriptorMetadata(mark);
            lookupMetaData.previousLookups.add(identifier);
            if (mark == to) {
                break;
            }
            mark = RArguments.getEnclosingFrame(mark);
        }
    }

    private static boolean isEnclosingFrameDescriptor(Frame current, Frame next) {
        assert current != null;
        FrameDescriptorMetaData metaData = getDescriptorMetadata(current);
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
                FrameDescriptorMetaData sub = getDescriptorMetadata(descriptor);
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

    private static FrameDescriptorMetaData getDescriptorMetadata(FrameDescriptor frameDescriptor) {
        Object descriptorInfo = frameDescriptor.getInfo();
        if (!(descriptorInfo instanceof FrameDescriptorMetaData)) {
            throw RInternalError.shouldNotReachHere("FrameDescriptor metadata should only be instances of FrameDescriptorMetaData");
        }
        return (FrameDescriptorMetaData) descriptorInfo;
    }

    private static FrameDescriptorMetaData getDescriptorMetadata(Frame frame) {
        return getDescriptorMetadata(handleBaseNamespaceEnv(frame));
    }

    private static FrameSlotInfo getFrameSlotInfo(Frame frame, int frameIndex) {
        FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        Object frameSlotInfo;
        if (FrameIndex.representsAuxiliaryIndex(frameIndex)) {
            int auxFrameIdx = FrameIndex.toAuxiliaryIndex(frameIndex);
            FrameDescriptorMetaData metaData = getDescriptorMetadata(frameDescriptor);
            frameSlotInfo = metaData.getAuxiliarySlotInfo(auxFrameIdx);
        } else {
            frameSlotInfo = frameDescriptor.getSlotInfo(FrameIndex.toNormalIndex(frameIndex));
        }
        if (!(frameSlotInfo instanceof FrameSlotInfo)) {
            throw RInternalError.shouldNotReachHere();
        }
        return (FrameSlotInfo) frameSlotInfo;
    }

    private static boolean isFrameIndexInBounds(Frame frame, int frameIndex) {
        return isFrameIndexInBounds(frame.getFrameDescriptor(), frameIndex);
    }

    private static boolean isFrameIndexInBounds(FrameDescriptor frameDescriptor, int frameIndex) {
        if (FrameIndex.isUninitializedIndex(frameIndex)) {
            return false;
        }
        if (FrameIndex.representsAuxiliaryIndex(frameIndex)) {
            int auxSlotIdx = FrameIndex.toAuxiliaryIndex(frameIndex);
            return 0 <= auxSlotIdx && auxSlotIdx < frameDescriptor.getNumberOfAuxiliarySlots();
        } else {
            int normalSlotIdx = FrameIndex.toNormalIndex(frameIndex);
            return 0 <= normalSlotIdx && normalSlotIdx < frameDescriptor.getNumberOfSlots();
        }
    }

    public static boolean isObject(Frame frame, int frameIndex) {
        return getFrameSlotKind(frame.getFrameDescriptor(), frameIndex) == FrameSlotKind.Object;
    }

    public static boolean isInt(Frame frame, int frameIndex) {
        return getFrameSlotKind(frame.getFrameDescriptor(), frameIndex) == FrameSlotKind.Int;
    }

    public static boolean isDouble(Frame frame, int frameIndex) {
        return getFrameSlotKind(frame.getFrameDescriptor(), frameIndex) == FrameSlotKind.Double;
    }

    public static boolean isByte(Frame frame, int frameIndex) {
        return getFrameSlotKind(frame.getFrameDescriptor(), frameIndex) == FrameSlotKind.Byte;
    }

    public static boolean isBoolean(Frame frame, int frameIndex) {
        return getFrameSlotKind(frame.getFrameDescriptor(), frameIndex) == FrameSlotKind.Boolean;
    }

    public static FrameSlotKind getFrameSlotKind(FrameDescriptor frameDescriptor, int index) {
        if (FrameIndex.representsAuxiliaryIndex(index)) {
            return FrameSlotKind.Object;
        } else {
            return frameDescriptor.getSlotKind(index);
        }
    }

    public static void setFrameSlotKind(FrameDescriptor frameDescriptor, int frameIndex, FrameSlotKind kind) {
        // It does not make sense to set any FrameSlotKind for an auxiliary value - they are always
        // objects, so we take care only of normal values.
        if (FrameIndex.representsNormalIndex(frameIndex)) {
            frameDescriptor.setSlotKind(FrameIndex.toNormalIndex(frameIndex), kind);
        }
    }

    public static boolean isEnclosingFrameDescriptor(FrameDescriptor descriptor, Frame newEnclosingFrame) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptorMetaData target = getDescriptorMetadata(descriptor);
        FrameDescriptor newEnclosingDescriptor = handleBaseNamespaceEnv(newEnclosingFrame);
        return target.getEnclosingFrameDescriptor() == newEnclosingDescriptor;
    }

    public static void initializeEnclosingFrame(FrameDescriptor frameDescriptor, Frame newEnclosingFrame) {
        FrameDescriptorMetaData target = getDescriptorMetadata(frameDescriptor);

        FrameDescriptor newEnclosingDescriptor = handleBaseNamespaceEnv(newEnclosingFrame);

        // this function can be called multiple times with the same enclosing descriptor
        if (target.getEnclosingFrameDescriptor() != newEnclosingDescriptor) {
            assert target.getEnclosingFrameDescriptor() == null : "existing enclosing descriptor while initializing " + target.name;
            assert target.lookupResults.isEmpty() : "existing lookup results while initializing " + target.name;

            target.updateEnclosingFrameDescriptor(newEnclosingDescriptor);
            if (newEnclosingDescriptor != null) {
                FrameDescriptorMetaData newEnclosing = getDescriptorMetadata(newEnclosingDescriptor);
                newEnclosing.subDescriptors.add(frameDescriptor);
            }
        }
    }

    public static void initializeEnclosingFrame(Frame frame, Frame newEnclosingFrame) {
        initializeEnclosingFrame(handleBaseNamespaceEnv(frame), newEnclosingFrame);
    }

    private static void setEnclosingFrame(FrameDescriptor descriptor, MaterializedFrame newEnclosingFrame, MaterializedFrame oldEnclosingFrame) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptorMetaData target = getDescriptorMetadata(descriptor);
        assert target != null : "frame descriptor wasn't registered properly for " + descriptor;

        // invalidate existing lookups
        invalidateAllNames(target);

        FrameDescriptor oldEnclosingDescriptor = target.getEnclosingFrameDescriptor();
        FrameDescriptor newEnclosingDescriptor = handleBaseNamespaceEnv(newEnclosingFrame);
        assert newEnclosingDescriptor == oldEnclosingDescriptor || (oldEnclosingDescriptor == null) == (oldEnclosingFrame == null) : "mismatch " + oldEnclosingDescriptor + " / " + oldEnclosingFrame;

        if (oldEnclosingDescriptor != null) {
            assert newEnclosingDescriptor == oldEnclosingDescriptor || oldEnclosingDescriptor == oldEnclosingFrame.getFrameDescriptor() : "mismatch " + oldEnclosingDescriptor + " / " +
                            oldEnclosingFrame.getFrameDescriptor();
            FrameDescriptorMetaData oldEnclosing = getDescriptorMetadata(oldEnclosingDescriptor);
            oldEnclosing.subDescriptors.remove(descriptor);
        }
        target.updateEnclosingFrameDescriptor(newEnclosingDescriptor);

        if (newEnclosingDescriptor != null) {
            FrameDescriptorMetaData newEnclosing = getDescriptorMetadata(newEnclosingDescriptor);
            assert !newEnclosing.name.equals("global") || !target.name.equals("base");
            newEnclosing.subDescriptors.add(descriptor);
        }
    }

    public static void setEnclosingFrame(Frame frame, MaterializedFrame newEnclosingFrame, MaterializedFrame oldEnclosingFrame) {
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
                invalidateAllNames(getDescriptorMetadata(sub));
            }
        }
    }

    public static void detach(Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptorMetaData position = getDescriptorMetadata(frame);
        FrameDescriptor oldEnclosingDescriptor = position.getEnclosingFrameDescriptor();
        FrameDescriptorMetaData oldEnclosing = getDescriptorMetadata(oldEnclosingDescriptor);
        FrameDescriptor newEnclosingDescriptor = oldEnclosing.getEnclosingFrameDescriptor();
        FrameDescriptorMetaData newEnclosing = getDescriptorMetadata(newEnclosingDescriptor);

        invalidateNames(oldEnclosing, oldEnclosing.getIdentifiers());

        position.updateEnclosingFrameDescriptor(newEnclosingDescriptor);
        oldEnclosing.updateEnclosingFrameDescriptor(null);
        oldEnclosing.subDescriptors.remove(frame.getFrameDescriptor());
        newEnclosing.subDescriptors.remove(oldEnclosingDescriptor);
        newEnclosing.subDescriptors.add(frame.getFrameDescriptor());
    }

    public static void attach(Frame frame, Frame newEnclosingFrame) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptorMetaData position = getDescriptorMetadata(frame);
        FrameDescriptorMetaData newEnclosing = getDescriptorMetadata(newEnclosingFrame);
        FrameDescriptor oldEnclosingDescriptor = position.getEnclosingFrameDescriptor();
        FrameDescriptorMetaData oldEnclosing = getDescriptorMetadata(oldEnclosingDescriptor);

        invalidateAllNames(newEnclosing);
        invalidateNames(position, newEnclosing.getIdentifiers());

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

    @ExportLibrary(ReflectionLibrary.class)
    public static final class MultiSlotData implements TruffleObject {

        private final Object[] data;

        public MultiSlotData(MultiSlotData prevValue) {
            data = Arrays.copyOf(prevValue.data, ChildContextInfo.contextNum());
        }

        public MultiSlotData() {
            data = new Object[ChildContextInfo.contextNum()];
        }

        public Object get(int ind) {
            return data[ind];
        }

        public void set(int ind, Object val) {
            data[ind] = val;
        }

        public void setAll(Object val) {
            Arrays.fill(data, val);
        }

        public void setAllDeepCopy(RSharingAttributeStorage val) {
            for (int i = 0; i < data.length; i++) {
                data[i] = val.deepCopy();
            }
        }

        @ExportMessage
        Object send(Message message, Object[] args,
                        @Cached BranchProfile notFoundProfile,
                        @CachedLibrary(limit = "5") ReflectionLibrary reflection) throws Exception {
            RContext ctx = RContext.getInstance(reflection);
            Object value = data[ctx.getMultiSlotInd()];
            if (value == null) {
                notFoundProfile.enter();
                return reflection.send(RUnboundValue.instance, message, args);
            }
            return reflection.send(value, message, args);
        }
    }

    /**
     * This class represents metadata about one particular frame slot and is saved inside {@link FrameDescriptor}.
     * It should not be associated with a value of any frame slot.
     */
    private static final class FrameSlotInfo {
        /**
         * This is meant to monitor updates performed on a frame slot. Each frame slot holds an
         * {@link Assumption} in it's "info" field; it is valid as long as no non-local update has
         * ever taken place.<br/>
         * The background to this rather strange assumption is that non-local reads are very hard to
         * keep track of thanks to R powerful language features. To keep the maintenance for the
         * assumption as cheap as possible, it checks only local reads - which is fast - and does a
         * more costly check on "<<-" but invalidates the assumption as soon as "eval" and the like
         * comes into play.<br/>
         */
        private final Assumption nonLocalModifiedAssumption;
        private final Assumption noMultiSlot;

        /**
         * An instance of {@link FrameSlotInfo} represents metadata of one frame slot inside
         * a particular {@link FrameDescriptor}.
         * This stable value is therefore stored under {@link FrameDescriptor}, which means that
         * the stable value is the same for any {@link Frame} with such a {@link FrameDescriptor}.
         *
         * An example of the stable value is {@code abs} builtin, which is stored in a frame slot
         * identified by {@code abs} in a {@link FrameDescriptor} representing base namespace.
         */
        @CompilationFinal private volatile StableValue<Object> stableValue;
        private int invalidationCount;
        private final boolean possibleMultiSlot;
        private final Object identifier;

        FrameSlotInfo(boolean isSingletonFrame, boolean isGlobalEnv, Object identifier, boolean isNewEnv) {
            nonLocalModifiedAssumption = Truffle.getRuntime().createAssumption(identifier + ":NonLocalModified");
            noMultiSlot = Truffle.getRuntime().createAssumption(identifier + ":NoMultiSlot");
            this.possibleMultiSlot = isSingletonFrame && !isNewEnv;
            this.identifier = identifier;
            if (isSingletonFrame) {
                stableValue = new StableValue<>(null, identifier.toString());
                invalidationCount = isGlobalEnv ? MAX_GLOBAL_ENV_INVALIDATION_COUNT : MAX_INVALIDATION_COUNT;
            } else {
                stableValue = null;
            }
        }

        FrameSlotInfo(FrameDescriptorMetaData metaData, Object identifier) {
            this(metaData.singletonFrame != null, "global".equals(metaData.name), identifier, metaData.name.startsWith("<new-env-"));
        }

        public boolean needsInvalidation() {
            return stableValue != null;
        }

        public boolean possibleMultiSlot() {
            return possibleMultiSlot;
        }

        /*
         * Special cases for primitive types to force value (instead of identity) comparison.
         */

        private void setValue(boolean value) {
            StableValue<Object> sv = stableValue;
            if (sv != null && (!(sv.getValue() instanceof Boolean) || ((boolean) sv.getValue()) != value)) {
                invalidateStableValue(sv, value);
            }
        }

        private void setValue(byte value) {
            StableValue<Object> sv = stableValue;
            if (sv != null && (!(sv.getValue() instanceof Byte) || ((byte) sv.getValue()) != value)) {
                invalidateStableValue(sv, value);
            }
        }

        private void setValue(int value) {
            StableValue<Object> sv = stableValue;
            if (sv != null && (!(sv.getValue() instanceof Integer) || ((int) sv.getValue()) != value)) {
                invalidateStableValue(sv, value);
            }
        }

        private void setValue(double value) {
            StableValue<Object> sv = stableValue;
            if (sv != null && (!(sv.getValue() instanceof Double) || ((double) sv.getValue()) != value)) {
                invalidateStableValue(sv, value);
            }
        }

        private void setValue(Object value) {
            StableValue<Object> sv = stableValue;
            if (sv != null && sv.getValue() != value) {
                invalidateStableValue(sv, value);
            }
        }

        private void invalidateStableValue(StableValue<Object> sv, Object value) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            sv.getAssumption().invalidate();
            if (invalidationCount > 0) {
                invalidationCount--;
                out("setting singleton value %s = %s", identifier, value == null ? "null" : value.getClass());
                stableValue = new StableValue<>(value, identifier.toString());
            } else {
                out("setting non-singleton value %s", identifier);
                stableValue = null;
            }
        }

        public StableValue<Object> getStableValue() {
            return stableValue;
        }

        private static void setNewMultiValue(Frame frame, int frameIndex, MultiSlotData data, Object newValue) {
            int ind = RContext.getInstance().getMultiSlotInd();
            data.set(ind, newValue);
            setObject(frame, frameIndex, data);
        }

        private static boolean evalAndSetPromise(Frame frame, int frameIndex, FrameSlotInfo info) {
            Object prevValue = info.stableValue.getValue();
            if (prevValue instanceof RPromise) {
                prevValue = RContext.getRRuntimeASTAccess().forcePromise("searchPathPromiseForce", prevValue);
                if (prevValue instanceof Boolean) {
                    setBoolean(frame, frameIndex, (boolean) prevValue);
                    info.setValue((boolean) prevValue);
                } else if (prevValue instanceof Byte) {
                    setByte(frame, frameIndex, (byte) prevValue);
                    info.setValue((byte) prevValue);
                } else if (prevValue instanceof Integer) {
                    setInt(frame, frameIndex, (int) prevValue);
                    info.setValue((int) prevValue);
                } else if (prevValue instanceof Double) {
                    setDouble(frame, frameIndex, (double) prevValue);
                    info.setValue((double) prevValue);
                } else {
                    setObject(frame, frameIndex, prevValue);
                    info.setValue(prevValue);
                }
                return true;
            } else {
                return false;
            }
        }

        public static void handleSearchPathMultiSlot(Frame frame, Object identifier, int[] indices, boolean replicate) {
            CompilerAsserts.neverPartOfCompilation();
            assert containsIdentifier(frame.getFrameDescriptor(), identifier);
            int frameIndex = getIndexOfIdentifier(frame.getFrameDescriptor(), identifier);
            while (true) {
                FrameSlotInfo info = getFrameSlotInfo(frame, frameIndex);
                Object prevValue = getObject(frame, frameIndex);
                MultiSlotData prevMultiSlotVal = null;
                // TODO: this takes assumption that the initial context has slot ID == 0, but this
                // may not be the case in embedding scenario if the user creates more than one
                // "initial context" in the JVM. The counters for slot index should be per "initial
                // context".
                if (prevValue instanceof MultiSlotData) {
                    prevMultiSlotVal = (MultiSlotData) prevValue;
                    prevValue = prevMultiSlotVal.get(0);
                }
                if (info.stableValue == null || isMutableRShareable(info.stableValue) || isMutableRShareable(prevValue) || !replicate) {
                    // create a multi slot for slots whose stableValue is null but also for all
                    // slots of the global frame (which are marked as !replicate)
                    info.stableValue = null;
                    info.nonLocalModifiedAssumption.invalidate();
                    info.noMultiSlot.invalidate();
                    info.invalidationCount = 0;
                    MultiSlotData data;

                    // TODO: do we have to worry that prevValue can be invalid?
                    if (prevMultiSlotVal != null) {
                        // this handles the case when we create share contexts for the second time -
                        // existing multi slots are an artifact of a previous executions and must
                        // be kept and extended. The slots of the previous contexts must be kept
                        // intact, so we replicate value only for newly created child contexts.
                        data = new MultiSlotData(prevMultiSlotVal);
                        // TODO: we just copied the data, update it, and then set it as new value
                        // for given slot. What if some other pre-existing context updates the data
                        // in between?
                        if (replicate) {
                            for (int i : indices) {
                                data.set(i, copyIfMutable(prevValue));
                            }
                        }
                    } else {
                        if (RContext.getInstance().getOption(SearchPathForcePromises)) {
                            prevValue = RContext.getRRuntimeASTAccess().forcePromise("searchPathPromiseForce", prevValue);
                        }
                        data = new MultiSlotData();
                        if (replicate) {
                            if (isMutableRShareable(prevValue)) {
                                // Mutable data structures that are not synchronized need to be
                                // copied
                                data.setAllDeepCopy((RSharingAttributeStorage) prevValue);
                            } else {
                                data.setAll(prevValue);
                            }
                        } else {
                            data.set(0, prevValue);
                        }
                    }
                    setObject(frame, frameIndex, data);
                    break;
                } else {
                    if (!RContext.getInstance().getOption(SearchPathForcePromises) || !evalAndSetPromise(frame, frameIndex, info)) {
                        break;
                    }
                    // otherwise stable value may get nullified and slot turned into multi slot
                }
            }
        }

        private static boolean isMutableRShareable(Object value) {
            return value instanceof RPairList;
        }

        // Copies any R object that is mutable and not thread safe.
        private static Object copyIfMutable(Object value) {
            if (value instanceof RPairList) {
                return ((RPairList) value).deepCopy();
            }
            return value;
        }

        @TruffleBoundary
        public synchronized void setMultiSlot(Frame frame, int frameIndex, Object newValue) {
            // TODO: perhaps putting the whole thing behind the Truffle boundary an overkill, but on
            // the other hand it shouldn't happen often and not on the fast path
            MultiSlotData data;
            if (stableValue == null) {
                // already a multi slot - should be visible to all threads
                assert containsIndex(frame, frameIndex);
                data = (MultiSlotData) getObject(frame, frameIndex);
                assert data != null;
                int ind = RContext.getInstance().getMultiSlotInd();
                data.set(ind, newValue);
            } else {
                nonLocalModifiedAssumption.invalidate();
                invalidationCount = 0;
                // TODO: is it necessary since we nullify stable value?
                stableValue.getAssumption().invalidate();
                noMultiSlot.invalidate();
                data = new MultiSlotData();
                Object prevValue = getObject(frame, frameIndex);
                // value was stable so this slot is set by primordial context
                data.set(0, prevValue);
                setNewMultiValue(frame, frameIndex, data, newValue);
                // this should create happens-before with stable value reads during lookup, thus
                // making preceding update to the actual frame OK to read without additional
                // synchronization
                stableValue = null;
            }
        }
    }

    /**
     * Retrieves the not-changed-locally {@link Assumption} for the given frame slot.
     *
     * @return The "not changed locally" assumption of the given slot at {@code frameIndex}.
     */
    public static Assumption getNotChangedNonLocallyAssumption(Frame frame, int frameIndex) {
        Assumption notChangedLocallyAssumption = getFrameSlotInfo(frame, frameIndex).nonLocalModifiedAssumption;
        assert isValidFrameDescriptor(frame.getFrameDescriptor());
        return notChangedLocallyAssumption;
    }

    public synchronized static int findOrAddAuxiliaryFrameSlot(FrameDescriptor frameDescriptor, Object identifier) {
        CompilerAsserts.neverPartOfCompilation();
        assert isValidFrameDescriptor(frameDescriptor);
        FrameDescriptorMetaData descriptorMetadata = getDescriptorMetadata(frameDescriptor);
        int auxSlotIdx = frameDescriptor.findOrAddAuxiliarySlot(identifier);
        int transformedAuxSlotIdx = FrameIndex.transformIndex(auxSlotIdx);
        if (descriptorMetadata.getIndex(identifier) == null) {
            descriptorMetadata.addIndex(identifier, transformedAuxSlotIdx);
            var slotInfo = new FrameSlotInfo(descriptorMetadata, identifier);
            descriptorMetadata.addAuxSlotInfo(slotInfo);
        }
        assert isValidFrameDescriptor(frameDescriptor);
        return transformedAuxSlotIdx;
    }

    // methods for changing frame slot contents

    /**
     * Checks if the assumption of the given {@code frameIndex} has to be invalidated.
     *
     * @param curFrame Current frame.
     * @param frameIndex Index of the slot into the frame; its "info" is assumed to be an Assumption, throws an
     *            {@link RInternalError} otherwise
     * @param invalidateProfile Used to guard the invalidation code.
     */
    private static void checkAndInvalidate(Frame curFrame, int frameIndex, boolean isNonLocal, BranchProfile invalidateProfile) {
        assert containsIndex(curFrame, frameIndex);
        if (getNotChangedNonLocallyAssumption(curFrame, frameIndex).isValid()) {
            // Check whether current frame is used outside a regular stack
            if (isNonLocal || RArguments.getIsIrregular(curFrame)) {
                // False positive: Also invalidates a slot in the current active frame if that one
                // is used inside eval or the like, but this cost is definitely negligible.
                if (invalidateProfile != null) {
                    invalidateProfile.enter();
                }
                getNotChangedNonLocallyAssumption(curFrame, frameIndex).invalidate();
            }
        }
    }

    private static boolean hasSharedContext() {
        return FastROptions.sharedContextsOptionValue && !RContext.isSingle();
    }

    /**
     * Sets the given value into an auxiliary frame slot on {@code auxFrameIdx} index. All the
     * auxiliary slots have {@code FrameSlotKind.Object} slot kind. Therefore, if we want to set a
     * primitive value there, we always have to box it.
     */
    private static void setAuxiliaryValue(Frame frame, int auxFrameIdx, Object value) {
        FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        assert 0 <= auxFrameIdx && auxFrameIdx < frameDescriptor.getNumberOfAuxiliarySlots();
        frame.setAuxiliarySlot(auxFrameIdx, value);
    }

    public static void setObject(Frame frame, Object identifier, Object newValue) {
        assert isValidFrameDescriptor(frame.getFrameDescriptor());
        FrameDescriptorMetaData descriptorMetaData = getDescriptorMetadata(frame);
        Integer frameIndex = descriptorMetaData.getIndex(identifier);
        if (frameIndex == null) {
            throw RInternalError.shouldNotReachHere("A frame slot should first be added with findOrAddAuxiliaryFrameSlot");
        }
        setObject(frame, (int) frameIndex, newValue);
    }

    public static void setObject(Frame frame, int frameIndex, Object newValue) {
        assert FrameIndex.isInitializedIndex(frameIndex);
        assert isFrameIndexInBounds(frame, frameIndex);
        assert isValidFrameDescriptor(frame.getFrameDescriptor());
        if (hasSharedContext()) {
            FrameSlotInfo slotInfo = getFrameSlotInfo(frame, frameIndex);
            if (isMultislot(slotInfo)) {
                slotInfo.setMultiSlot(frame, frameIndex, newValue);
            }
        } else {
            if (FrameIndex.representsNormalIndex(frameIndex)) {
                int normalSlotIdx = FrameIndex.toNormalIndex(frameIndex);
                frame.setObject(normalSlotIdx, newValue);
            } else {
                int auxSlotIdx = FrameIndex.toAuxiliaryIndex(frameIndex);
                setAuxiliaryValue(frame, auxSlotIdx, newValue);
            }
        }
    }

    public static void setObjectAndInvalidate(Frame frame, int frameIndex, Object newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        assert isValidFrameDescriptor(frame.getFrameDescriptor());
        assert !ActiveBinding.isActiveBinding(newValue);
        setAndInvalidate(frame, frameIndex, newValue, isNonLocal, invalidateProfile);
    }

    private static void setAndInvalidate(Frame frame, int frameIndex, Object newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        assert isValidFrameDescriptor(frame.getFrameDescriptor());
        FrameSlotInfo slotInfo = getFrameSlotInfo(frame, frameIndex);
        if (hasSharedContext() && isMultislot(slotInfo)) {
            slotInfo.setMultiSlot(frame, frameIndex, newValue);
        } else {
            setObject(frame, frameIndex, newValue);
            if (slotInfo.needsInvalidation()) {
                slotInfo.setValue(newValue);
            }
            checkAndInvalidate(frame, frameIndex, isNonLocal, invalidateProfile);
        }
    }

    public static void setBoolean(Frame frame, int frameIndex, boolean newValue) {
        assert isFrameIndexInBounds(frame, frameIndex);
        assert isValidFrameDescriptor(frame.getFrameDescriptor());
        if (hasSharedContext()) {
            FrameSlotInfo slotInfo = getFrameSlotInfo(frame, frameIndex);
            if (isMultislot(slotInfo)) {
                slotInfo.setMultiSlot(frame, frameIndex, newValue);
            }
        } else {
            if (FrameIndex.representsNormalIndex(frameIndex)) {
                frame.setBoolean(FrameIndex.toNormalIndex(frameIndex), newValue);
            } else {
                // Every auxiliary slot is an object slot
                setObject(frame, frameIndex, (Object) newValue);
            }
        }
    }

    public static void setByte(Frame frame, int frameIndex, byte newValue) {
        assert isValidFrameDescriptor(frame.getFrameDescriptor());
        if (hasSharedContext()) {
            FrameSlotInfo slotInfo = getFrameSlotInfo(frame, frameIndex);
            if (isMultislot(slotInfo)) {
                slotInfo.setMultiSlot(frame, frameIndex, newValue);
            }
        } else {
            if (FrameIndex.representsNormalIndex(frameIndex)) {
                frame.setByte(FrameIndex.toNormalIndex(frameIndex), newValue);
            } else {
                setAuxiliaryValue(frame, FrameIndex.toAuxiliaryIndex(frameIndex), newValue);
            }
        }
    }

    public static void setByteAndInvalidate(Frame frame, int index, byte newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        assert isValidFrameDescriptor(frame.getFrameDescriptor());
        FrameSlotInfo slotInfo = getFrameSlotInfo(frame, index);
        if (hasSharedContext()) {
            slotInfo.setMultiSlot(frame, index, newValue);
        } else {
            setByte(frame, index, newValue);
            if (slotInfo.needsInvalidation()) {
                slotInfo.setValue(newValue);
            }
            checkAndInvalidate(frame, index, isNonLocal, invalidateProfile);
        }
    }

    public static void setInt(Frame frame, int frameIndex, int newValue) {
        if (hasSharedContext()) {
            FrameSlotInfo slotInfo = getFrameSlotInfo(frame, frameIndex);
            if (isMultislot(slotInfo)) {
                slotInfo.setMultiSlot(frame, frameIndex, newValue);
            }
        } else {
            if (FrameIndex.representsNormalIndex(frameIndex)) {
                frame.setInt(FrameIndex.toNormalIndex(frameIndex), newValue);
            } else {
                setAuxiliaryValue(frame, FrameIndex.toAuxiliaryIndex(frameIndex), newValue);
            }
        }
    }

    public static void setIntAndInvalidate(Frame frame, int frameIndex, int newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        assert isValidFrameDescriptor(frame.getFrameDescriptor());
        FrameSlotInfo slotInfo = getFrameSlotInfo(frame, frameIndex);
        if (hasSharedContext()) {
            slotInfo.setMultiSlot(frame, frameIndex, newValue);
        } else {
            setInt(frame, frameIndex, newValue);
            if (slotInfo.needsInvalidation()) {
                slotInfo.setValue(newValue);
            }
            checkAndInvalidate(frame, frameIndex, isNonLocal, invalidateProfile);
        }
    }

    public static void setDouble(Frame frame, int frameIndex, double newValue) {
        assert isValidFrameDescriptor(frame.getFrameDescriptor());
        if (hasSharedContext()) {
            FrameSlotInfo slotInfo = getFrameSlotInfo(frame, frameIndex);
            if (isMultislot(slotInfo)) {
                slotInfo.setMultiSlot(frame, frameIndex, newValue);
            }
        } else {
            if (FrameIndex.representsNormalIndex(frameIndex)) {
                frame.setDouble(FrameIndex.toNormalIndex(frameIndex), newValue);
            } else {
                setAuxiliaryValue(frame, FrameIndex.toAuxiliaryIndex(frameIndex), newValue);
            }
        }
    }

    public static void setDoubleAndInvalidate(Frame frame, int frameIndex, double newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        assert isValidFrameDescriptor(frame.getFrameDescriptor());
        FrameSlotInfo slotInfo = getFrameSlotInfo(frame, frameIndex);
        if (hasSharedContext()) {
            slotInfo.setMultiSlot(frame, frameIndex, newValue);
        } else {
            setDouble(frame, frameIndex, newValue);
            if (slotInfo.needsInvalidation()) {
                slotInfo.setValue(newValue);
            }
            checkAndInvalidate(frame, frameIndex, isNonLocal, invalidateProfile);
        }
    }

    public static Object getValue(Frame frame, int frameIndex) {
        if (hasSharedContext()) {
            FrameSlotInfo info = getFrameSlotInfo(frame, frameIndex);
            if (info.noMultiSlot.isValid()) {
                return getObject(frame, frameIndex);
            }
            Object o = getObject(frame, frameIndex);
            if (!(o instanceof MultiSlotData)) {
                CompilerDirectives.transferToInterpreter();
                synchronized (info) {
                    assert containsIndex(frame, frameIndex);
                    o = getObject(frame, frameIndex);
                    assert o != null;
                }
            }
            return ((MultiSlotData) o).get(RContext.getInstance().getMultiSlotInd());
        } else {
            return getObject(frame, frameIndex);
        }
    }

    /**
     * Returns an object from an auxiliary slot and checks whether it is instance of given
     * {@code klass}, throws {@link FrameSlotTypeException} if the object in the auxiliary slot is
     * not an instance of klass.
     * 
     * @param frame
     * @param frameIndex Auxiliary frame index.
     * @param klass
     */
    private static <T> T getAuxiliaryCheckedObject(Frame frame, int frameIndex, Class<T> klass) {
        assert FrameIndex.representsAuxiliaryIndex(frameIndex);
        CompilerAsserts.partialEvaluationConstant(klass);
        Object object = getObject(frame, frameIndex);
        // if (object instanceof klass) ...
        if (klass.isInstance(object)) {
            return klass.cast(object);
        } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new FrameSlotTypeException();
        }
    }

    public static Object getObject(Frame frame, Object identifier) {
        int frameIndex = getIndexOfIdentifier(frame.getFrameDescriptor(), identifier);
        if (FrameIndex.isUninitializedIndex(frameIndex)) {
            return null;
        } else {
            return getObject(frame, frameIndex);
        }
    }

    public static Object getObject(Frame frame, int frameIndex) {
        assert FrameIndex.isInitializedIndex(frameIndex);
        assert isValidFrameDescriptor(frame.getFrameDescriptor());
        if (hasSharedContext()) {
            FrameSlotInfo info = getFrameSlotInfo(frame, frameIndex);
            if (info.noMultiSlot.isValid()) {
                return getObjectByIndex(frame, frameIndex);
            }
            Object o;
            try {
                o = getObjectByIndex(frame, frameIndex);
            } catch (FrameSlotTypeException e) {
                CompilerDirectives.transferToInterpreter();
                o = null;
            }
            if (!(o instanceof MultiSlotData)) {
                CompilerDirectives.transferToInterpreter();
                synchronized (info) {
                    assert containsIndex(frame, frameIndex);
                    o = getObjectByIndex(frame, frameIndex);
                    assert o != null;
                }
            }
            return ((MultiSlotData) o).get(RContext.getInstance().getMultiSlotInd());
        } else {
            return getObjectByIndex(frame, frameIndex);
        }
    }

    private static Object getObjectByIndex(Frame frame, int frameIndex) {
        if (FrameIndex.representsNormalIndex(frameIndex)) {
            return frame.getObject(FrameIndex.toNormalIndex(frameIndex));
        } else {
            return frame.getAuxiliarySlot(FrameIndex.toAuxiliaryIndex(frameIndex));
        }
    }

    public static boolean getBoolean(Frame frame, int frameIndex) {
        assert isFrameIndexInBounds(frame, frameIndex);
        assert isValidFrameDescriptor(frame.getFrameDescriptor());
        if (FrameIndex.representsNormalIndex(frameIndex)) {
            return frame.getBoolean(FrameIndex.toNormalIndex(frameIndex));
        } else {
            return getAuxiliaryCheckedObject(frame, frameIndex, Boolean.class);
        }
    }

    public static byte getByte(Frame frame, int frameIndex) {
        assert isFrameIndexInBounds(frame, frameIndex);
        assert isValidFrameDescriptor(frame.getFrameDescriptor());
        if (FrameIndex.representsNormalIndex(frameIndex)) {
            return frame.getByte(frameIndex);
        } else {
            return getAuxiliaryCheckedObject(frame, frameIndex, Byte.class);
        }
    }

    public static int getInt(Frame frame, int frameIndex) {
        assert isFrameIndexInBounds(frame, frameIndex);
        assert isValidFrameDescriptor(frame.getFrameDescriptor());
        if (FrameIndex.representsNormalIndex(frameIndex)) {
            return frame.getInt(FrameIndex.toNormalIndex(frameIndex));
        } else {
            return getAuxiliaryCheckedObject(frame, frameIndex, Integer.class);
        }
    }

    public static double getDouble(Frame frame, int frameIndex) {
        assert isFrameIndexInBounds(frame, frameIndex);
        assert isValidFrameDescriptor(frame.getFrameDescriptor());
        if (FrameIndex.representsNormalIndex(frameIndex)) {
            return frame.getDouble(FrameIndex.toNormalIndex(frameIndex));
        } else {
            return getAuxiliaryCheckedObject(frame, frameIndex, Double.class);
        }
    }

    public static void setActiveBinding(Frame frame, int frameIndex, ActiveBinding newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        setAndInvalidate(frame, frameIndex, newValue, isNonLocal, invalidateProfile);
        getContainsNoActiveBindingAssumption(frame.getFrameDescriptor()).invalidate();
    }

    /**
     * Initializes the internal data structures for a newly created frame descriptor that is
     * intended to be used for a non-function frame (and thus will only ever be used for one frame).
     */
    public static void initializeNonFunctionFrameDescriptor(FrameDescriptor frameDescriptor, MaterializedFrame singletonFrame) {
        assert singletonFrame != null;
        assert isValidFrameDescriptor(frameDescriptor);
        FrameDescriptorMetaData metaData = getDescriptorMetadata(frameDescriptor);
        metaData.setSingletonFrame(singletonFrame);
    }

    public static Assumption getEnclosingFrameDescriptorAssumption(FrameDescriptor frameDescriptor) {
        assert isValidFrameDescriptor(frameDescriptor);
        return getDescriptorMetadata(frameDescriptor).getEnclosingFrameDescriptorAssumption();
    }

    public static Assumption getContainsNoActiveBindingAssumption(FrameDescriptor descriptor) {
        assert isValidFrameDescriptor(descriptor);
        return getDescriptorMetadata(descriptor).getContainsNoActiveBindingAssumption();
    }

    public static StableValue<Object> getStableValueAssumption(Frame frame, int frameIndex, Object value) {
        CompilerAsserts.neverPartOfCompilation();
        StableValue<Object> stableValue = getFrameSlotInfo(frame, frameIndex).getStableValue();
        if (stableValue != null) {
            assert getDescriptorMetadata(frame).singletonFrame != null : "single frame slot within non-singleton descriptor";
            assert stableValue.getValue() == value || (stableValue.getValue() != null && (stableValue.getValue().equals(value) || !stableValue.getAssumption().isValid())) : stableValue.getValue() +
                            " vs. " + value;
        }
        return stableValue;
    }

    public static MaterializedFrame getSingletonFrame(FrameDescriptor descriptor) {
        WeakReference<MaterializedFrame> singleton = getDescriptorMetadata(descriptor).singletonFrame;
        return singleton == null ? null : singleton.get();
    }

    /*
     * This method should be called for frames of all environments on the search path.
     */
    public static synchronized void handleAllMultiSlots(Frame frame, int[] indices, boolean replicate) {
        for (Object identifier : getIdentifiers(frame.getFrameDescriptor())) {
            FrameSlotInfo.handleSearchPathMultiSlot(frame, identifier, indices, replicate);
        }
    }

    private static boolean isMultislot(FrameSlotInfo info) {
        return info.possibleMultiSlot() || !info.noMultiSlot.isValid();
    }

    /**
     * Nullifies a set of slots in a {@link MultiSlotData} to avoid memory leaks. When providing
     * {@code null} as indices, all subslots except the first one are nullified.
     */
    public static synchronized void cleanMultiSlots(Frame frame, int[] indices) {
        CompilerAsserts.neverPartOfCompilation();
        Object[] identifiers = getIdentifiers(frame.getFrameDescriptor()).toArray();
        for (int i = 0; i < identifiers.length; i++) {
            int frameIndex = getIndexOfIdentifier(frame.getFrameDescriptor(), identifiers[i]);
            Object value = getValue(frame, frameIndex);
            if (value instanceof MultiSlotData) {
                MultiSlotData msd = (MultiSlotData) value;
                if (indices != null) {
                    for (int j = 0; j < indices.length; j++) {
                        assert indices[j] != 0;
                        msd.set(indices[j], null);
                    }
                } else {
                    // only safe value of primordial context
                    Object initialValue = msd.get(0);
                    msd.setAll(null);
                    msd.set(0, initialValue);
                }
            }
        }
    }
}
