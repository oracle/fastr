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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameDescriptor.Builder;
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
import com.oracle.truffle.api.profiles.ValueProfile;
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
import com.oracle.truffle.r.runtime.parsermetadata.FunctionScope;

/**
 * This class handles all the accesses and manipulations with {@link FrameDescriptor frame
 * descriptors} and {@link Frame frames}, e.g., frame descriptor allocation and getting values from
 * a frame. For every frame descriptor, we maintain {@link FrameDescriptorMetaData metadata}
 * describing, among other things, the mapping of identifiers to their indexes in the frame, as in
 * FastR, we use both auxiliary and indexed frame slots.
 * <p>
 * <h3>Frame indexes</h3> In FastR, we use frame descriptors for both functions and environments.
 * This makes the transformation between the function's evaluation frame and an environment easy.
 * Auxiliary slots are needed for symbols in the environment, because we do not know them ahead of
 * time, and for symbols in the function defined outside the function (e.g. adding a symbol to the
 * caller via {@code parent.frame}).
 * <p>
 * The meaning of frame indexes returned by the methods in this class is described in
 * {@link FrameIndex} - it is an integer that encodes an index to both auxiliary and indexed slots,
 * and {@link FrameSlotChangeMonitor} handles that transparently. You should not try to use that
 * integer to access a frame slot directly, as the result would be undefined.
 *
 * <h3>Frame descriptor metadata</h3> For every frame descriptor, we maintain
 * {@link FrameDescriptorMetaData metadata} where we keep:
 * <ul>
 * <li>The mapping of identifiers to their indexes</li>
 * <li>For a frame descriptor representing an environment, its parent environment</li>
 * <li>Results of previous symbol lookups</li>
 * <li>Assumptions about the layout of the environment hierarchy.</li>
 * </ul>
 * <p>
 * See {@link #assertValidFrameDescriptor(FrameDescriptor)} for expected contents of a frame
 * descriptor.
 */
public final class FrameSlotChangeMonitor {
    /**
     * Count of internal indexed (normal) frame slots. Currently only visibility.
     */
    public static final int INTERNAL_INDEXED_SLOT_COUNT = 1;

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

        @Override
        public String toString() {
            return "LookupResult{" +
                            "assumption=" + assumption +
                            '}';
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

        @Override
        public String toString() {
            return "StableValueLookupResult{" +
                            "value=" + value +
                            ", unwrappedValue=" + unwrappedValue +
                            '}';
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
        /**
         * If not null, the corresponding {@link FrameDescriptor} associated with this metadata is a
         * frame descriptor for an environment.
         */
        private WeakReference<MaterializedFrame> singletonFrame;
        /**
         * Being a subdescriptor is an inverse relation to being an enclosing descriptor. E.g. if
         * env1 has env2 as an enclosing environment, then descriptor of env1 is a subdescriptor of
         * env2.
         */
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
         * Mapping of identifiers to indexes (transformed with {@link FrameIndex}) into the frame.
         */
        private final Map<Object, Integer> indexes = new LinkedHashMap<>();
        /**
         * List of frame slot infos for auxiliary slots. The indexes into this list correspond to
         * indexes into auxiliary slots in a frame.
         */
        @CompilationFinal(dimensions = 1) private FrameSlotInfo[] auxSlotInfos = new FrameSlotInfo[10];
        @CompilationFinal private int auxSlotInfosElements;

        private final Map<Object, Assumption> notInFrameAssumptions = new HashMap<>();

        private WeakReference<FrameDescriptor> enclosingFrameDescriptor = new WeakReference<>(null);
        private Assumption enclosingFrameDescriptorAssumption;
        private final Assumption containsNoActiveBindingAssumption;

        private FrameDescriptorMetaData(String name, MaterializedFrame singletonFrame) {
            this.name = name;
            this.singletonFrame = singletonFrame == null ? null : new WeakReference<>(singletonFrame);
            this.enclosingFrameDescriptorAssumption = Truffle.getRuntime().createAssumption(getAssumptionNamePrefix() + "enclosing frame descriptor");
            this.containsNoActiveBindingAssumption = Truffle.getRuntime().createAssumption(getAssumptionNamePrefix() + "contains no active binding");
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
            CompilerAsserts.neverPartOfCompilation();
            return indexes.get(identifier);
        }

        public FrameSlotInfo getAuxiliarySlotInfo(int auxSlotIdx) {
            return auxSlotInfos[auxSlotIdx];
        }

        public void addAuxSlotInfo(FrameSlotInfo slotInfo) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (auxSlotInfosElements >= auxSlotInfos.length) {
                int newLength = auxSlotInfos.length * 2;
                auxSlotInfos = Arrays.copyOf(auxSlotInfos, newLength);
            }
            auxSlotInfos[auxSlotInfosElements] = slotInfo;
            auxSlotInfosElements++;
        }

        public List<Object> getIdentifiers() {
            CompilerAsserts.neverPartOfCompilation();
            return new ArrayList<>(indexes.keySet());
        }

        public Assumption getNotInFrameAssumption(Object identifier) {
            CompilerAsserts.neverPartOfCompilation();
            if (!notInFrameAssumptions.containsKey(identifier)) {
                String assumptionName = String.format("identifier '%s' not in frame assumption", identifier);
                Assumption assumption = Truffle.getRuntime().createAssumption(assumptionName);
                notInFrameAssumptions.put(identifier, assumption);
                return assumption;
            } else {
                return notInFrameAssumptions.get(identifier);
            }
        }

        void tryInvalidateNotInFrameAssumption(Object identifier) {
            var assumption = notInFrameAssumptions.get(identifier);
            if (assumption != null) {
                assumption.invalidate();
                notInFrameAssumptions.remove(identifier);
            }
        }

        public void updateEnclosingFrameDescriptor(FrameDescriptor newEnclosingDescriptor) {
            CompilerAsserts.neverPartOfCompilation();
            if (enclosingFrameDescriptorAssumption != null) {
                enclosingFrameDescriptorAssumption.invalidate();
            }
            enclosingFrameDescriptor = new WeakReference<>(newEnclosingDescriptor);
            enclosingFrameDescriptorAssumption = Truffle.getRuntime().createAssumption(getAssumptionNamePrefix() + "enclosing frame descriptor");
        }

        private String getAssumptionNamePrefix() {
            return name == null ? "" : "(" + name + ")";
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
            StringBuilder sb = new StringBuilder();
            sb.append("FrameDescriptorMetaData{\n");
            sb.append("  name = '").append(name).append("',\n");
            sb.append("  previousLookups = [").append(previousLookups).append("],\n");
            sb.append("  lookupResults = {\n");
            lookupResults.forEach(
                            (key, weakResult) -> {
                                sb.append("    ").append("'").append(key).append("': ");
                                if (weakResult == null || weakResult.get() == null) {
                                    sb.append("null");
                                } else {
                                    LookupResult lookupResult = weakResult.get();
                                    assert lookupResult != null;
                                    sb.append(lookupResult);
                                }
                                sb.append(",\n");
                            });
            sb.append("  },\n"); // lookupResults
            sb.append("  subDescriptors = [\n");
            subDescriptors.forEach((descriptor) -> sb.append("    ").append(descriptor).append(",\n"));
            sb.append("  ]\n"); // subDescriptors
            sb.append("}"); // FrameDescriptorMetadata
            return sb.toString();
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
            if (FrameIndex.isInitializedIndex(frameIndex)) {
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

    /**
     * Checks whether the given {@code frameDescriptor} conforms to the expected structure,
     * including its metadata. Note that this method is very thorough and therefore slow, it should
     * be used only sparingly in some assert statements.
     */
    public static boolean assertValidFrameDescriptor(FrameDescriptor frameDescriptor) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptorMetaData metadata = getDescriptorMetadata(frameDescriptor);
        int normalSlotsCount = frameDescriptor.getNumberOfSlots();
        int auxSlotsCount = frameDescriptor.getNumberOfAuxiliarySlots();
        int totalSlotsCount = normalSlotsCount + auxSlotsCount;
        assert metadata.indexes.size() == totalSlotsCount;
        for (Map.Entry<Object, Integer> entry : metadata.indexes.entrySet()) {
            Object identifier = entry.getKey();
            int frameIndex = entry.getValue();
            assert isFrameIndexInBounds(frameDescriptor, frameIndex);
            if (FrameIndex.representsNormalIndex(frameIndex)) {
                int normalSlotIdx = FrameIndex.toNormalIndex(frameIndex);
                Object slotInfo = frameDescriptor.getSlotInfo(normalSlotIdx);
                assert slotInfo instanceof FrameSlotInfo;
                assert ((FrameSlotInfo) slotInfo).identifier == identifier;
                Object slotName = frameDescriptor.getSlotName(normalSlotIdx);
                assert slotName instanceof String || slotName instanceof RFrameSlot;
                assert slotName == identifier;
            } else {
                assert FrameIndex.representsAuxiliaryIndex(frameIndex);
                int auxSlotIdx = FrameIndex.toAuxiliaryIndex(frameIndex);
                FrameSlotInfo slotInfo = metadata.getAuxiliarySlotInfo(auxSlotIdx);
                assert slotInfo.identifier == identifier;
            }
        }
        // Check internal indexed frames
        // Only visibility now
        assert frameDescriptor.getNumberOfSlots() >= 1;
        int visibilityFrameIdx = FrameIndex.toNormalIndex(RFrameSlot.Visibility.getFrameIdx());
        assert frameDescriptor.getSlotName(visibilityFrameIdx) == RFrameSlot.Visibility;
        assert frameDescriptor.getSlotKind(visibilityFrameIdx) == FrameSlotKind.Boolean;

        // Check auxiliary slots.
        for (Map.Entry<Object, Integer> entry : frameDescriptor.getAuxiliarySlots().entrySet()) {
            Object identifier = entry.getKey();
            int auxSlotIdx = entry.getValue();
            int auxSlotIdxFromMetata = FrameIndex.toAuxiliaryIndex(metadata.getIndex(identifier));
            assert auxSlotIdx == auxSlotIdxFromMetata;
        }
        // Check auxSlotInfos
        assert metadata.auxSlotInfosElements == frameDescriptor.getNumberOfAuxiliarySlots();
        for (int i = 0; i < metadata.auxSlotInfosElements; i++) {
            FrameSlotInfo slotInfo = metadata.auxSlotInfos[i];
            assert slotInfo.identifier != null;
        }
        return true;
    }

    /**
     * Used for creating new frame descriptors for environments, not for functions.
     *
     * @param name Name of the environment
     */
    public static FrameDescriptor createUninitializedFrameDescriptor(String name) {
        FrameDescriptorMetaData metaData = new FrameDescriptorMetaData(name);
        Builder builder = FrameDescriptor.newBuilder();
        builder.info(metaData);
        addInternalIndexedSlots(builder, metaData);
        return builder.build();
    }

    /**
     * Creates a frame descriptor suitable for environment representation.
     *
     * @param name Name for debug purposes
     * @param singletonFrame A frame that will hold all the values within the environment as
     *            auxiliary slots.
     */
    public static FrameDescriptor createEnvironmentFrameDescriptor(String name, MaterializedFrame singletonFrame) {
        assert singletonFrame != null;
        FrameDescriptorMetaData metaData = new FrameDescriptorMetaData(name, singletonFrame);
        FrameDescriptor.Builder builder = FrameDescriptor.newBuilder();
        builder.info(metaData);
        addInternalIndexedSlots(builder, metaData);
        FrameDescriptor descriptor = builder.build();
        assert assertValidFrameDescriptor(descriptor);
        return descriptor;
    }

    /**
     * Create a frame descriptor suitable for functions. The returned frame descriptor will mostly
     * contain indexed slots, maybe some auxiliary slots.
     *
     * @param name Name for debug purposes
     */
    public static FrameDescriptor createFunctionFrameDescriptor(String name) {
        Builder builder = FrameDescriptor.newBuilder();
        FrameDescriptorMetaData descriptorMetaData = new FrameDescriptorMetaData(name);
        builder.info(descriptorMetaData);
        addInternalIndexedSlots(builder, descriptorMetaData);
        FrameDescriptor frameDescriptor = builder.build();
        assert assertValidFrameDescriptor(frameDescriptor);
        return frameDescriptor;
    }

    /**
     * Creates a {@link FrameDescriptor} with normal indexed slots.
     *
     * @param name Name for debug purposes.
     */
    public static FrameDescriptor createFunctionFrameDescriptor(String name, FunctionScope functionScope) {
        int localVariableCount = functionScope.getLocalVariableCount();
        Builder builder = FrameDescriptor.newBuilder(INTERNAL_INDEXED_SLOT_COUNT + localVariableCount);
        var descriptorMetadata = new FrameDescriptorMetaData(name);
        builder.info(descriptorMetadata);
        addInternalIndexedSlots(builder, descriptorMetadata);
        for (int i = 0; i < localVariableCount; i++) {
            String identifier = functionScope.getLocalVariableName(i);
            int frameIndex = builder.addSlot(functionScope.getLocalVariableKind(i), identifier, new FrameSlotInfo(descriptorMetadata, identifier));
            descriptorMetadata.addIndex(identifier, FrameIndex.toNormalIndex(frameIndex));
        }
        FrameDescriptor frameDescriptor = builder.build();
        assert assertValidFrameDescriptor(frameDescriptor);
        return frameDescriptor;
    }

    /**
     * Internal indexed slots are used only for function frame descriptors, not for environment
     * frame descriptors. See documentation of {@link RFrameSlot}.
     */
    private static void addInternalIndexedSlots(FrameDescriptor.Builder builder, FrameDescriptorMetaData metaData) {
        for (RFrameSlot internalFrameSlot : RFrameSlot.internalIndexedSlots) {
            builder.addSlot(internalFrameSlot.getSlotKind(), internalFrameSlot, new FrameSlotInfo(metaData, internalFrameSlot));
            metaData.addIndex(internalFrameSlot, internalFrameSlot.getFrameIdx());
        }
    }

    public static FrameDescriptor copyFrameDescriptorWithMetadata(FrameDescriptor frameDescriptor) {
        FrameDescriptorMetaData metadata = getDescriptorMetadata(frameDescriptor);
        MaterializedFrame singletonFrame = metadata.singletonFrame != null ? metadata.singletonFrame.get() : null;
        FrameDescriptorMetaData newMetadata = new FrameDescriptorMetaData(metadata.name, singletonFrame);
        FrameDescriptor.Builder newDescriptorBuilder = FrameDescriptor.newBuilder(frameDescriptor.getNumberOfSlots());
        newDescriptorBuilder.info(newMetadata);
        // Copy indexed (normal) slots
        for (int i = 0; i < frameDescriptor.getNumberOfSlots(); i++) {
            Object identifier = frameDescriptor.getSlotName(i);
            newDescriptorBuilder.addSlot(frameDescriptor.getSlotKind(i), identifier, frameDescriptor.getSlotInfo(i));
            newMetadata.addIndex(identifier, i);
        }
        FrameDescriptor newDescriptor = newDescriptorBuilder.build();
        // Copy auxiliary slots
        for (Map.Entry<Object, Integer> entry : frameDescriptor.getAuxiliarySlots().entrySet()) {
            // entry values may be different, but that would not matter.
            findOrAddAuxiliaryFrameSlot(newDescriptor, entry.getKey());
        }
        assert assertValidFrameDescriptor(newDescriptor);
        return newDescriptor;
    }

    public static List<Object> getIdentifiers(FrameDescriptor frameDescriptor) {
        return getDescriptorMetadata(frameDescriptor).getIdentifiers();
    }

    public static boolean containsIdentifier(FrameDescriptor frameDescriptor, Object identifier) {
        FrameDescriptorMetaData metadata = getDescriptorMetadata(frameDescriptor);
        Integer frameIndex = metadata.getIndex(identifier);
        return frameIndex != null;
    }

    public static boolean containsIndex(Frame frame, int frameIndex) {
        return containsIndex(frame.getFrameDescriptor(), frameIndex);
    }

    public static boolean containsIndex(FrameDescriptor frameDescriptor, int frameIndex) {
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
     * @return {@code FrameIndex.UNITIALIZED_INDEX} if the given identifier is not in the frame
     *         descriptor
     */
    public static int getIndexOfIdentifier(FrameDescriptor frameDescriptor, Object identifier) {
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

    private static synchronized void invalidatePreviousLookups(FrameDescriptorMetaData metaData, Collection<Object> identifiers) {
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
                invalidatePreviousLookups(sub, identifiers);
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
        assert descriptorInfo instanceof FrameDescriptorMetaData;
        return (FrameDescriptorMetaData) descriptorInfo;
    }

    private static FrameDescriptorMetaData getDescriptorMetadata(Frame frame) {
        return getDescriptorMetadata(handleBaseNamespaceEnv(frame));
    }

    private static FrameSlotInfo getFrameSlotInfo(Frame frame, int frameIndex) {
        return getFrameSlotInfo(frame.getFrameDescriptor(), frameIndex);
    }

    private static FrameSlotInfo getFrameSlotInfo(FrameDescriptor frameDescriptor, int frameIndex) {
        CompilerAsserts.partialEvaluationConstant(frameIndex);
        Object frameSlotInfo;
        if (FrameIndex.representsAuxiliaryIndex(frameIndex)) {
            int auxFrameIdx = FrameIndex.toAuxiliaryIndex(frameIndex);
            FrameDescriptorMetaData metaData = getDescriptorMetadata(frameDescriptor);
            frameSlotInfo = metaData.getAuxiliarySlotInfo(auxFrameIdx);
        } else {
            frameSlotInfo = frameDescriptor.getSlotInfo(FrameIndex.toNormalIndex(frameIndex));
        }
        assert frameSlotInfo instanceof FrameSlotInfo;
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
        if (FrameIndex.representsNormalIndex(frameIndex)) {
            return frame.isObject(FrameIndex.toNormalIndex(frameIndex));
        } else {
            // All auxiliary slots are considered objects
            return true;
        }
    }

    public static boolean isInt(Frame frame, int frameIndex) {
        if (FrameIndex.representsNormalIndex(frameIndex)) {
            return frame.isInt(FrameIndex.toNormalIndex(frameIndex));
        } else {
            return false;
        }
    }

    public static boolean isDouble(Frame frame, int frameIndex) {
        if (FrameIndex.representsNormalIndex(frameIndex)) {
            return frame.isDouble(FrameIndex.toNormalIndex(frameIndex));
        } else {
            return false;
        }
    }

    public static boolean isByte(Frame frame, int frameIndex) {
        if (FrameIndex.representsNormalIndex(frameIndex)) {
            return frame.isByte(FrameIndex.toNormalIndex(frameIndex));
        } else {
            return false;
        }
    }

    public static boolean isBoolean(Frame frame, int frameIndex) {
        if (FrameIndex.representsNormalIndex(frameIndex)) {
            return frame.isBoolean(FrameIndex.toNormalIndex(frameIndex));
        } else {
            return false;
        }
    }

    /**
     * Get slot kind of the given frame index from the frame descriptor.
     */
    public static FrameSlotKind getFrameSlotKindInFrameDescriptor(FrameDescriptor frameDescriptor, int frameIndex) {
        if (FrameIndex.representsAuxiliaryIndex(frameIndex)) {
            return FrameSlotKind.Object;
        } else {
            return frameDescriptor.getSlotKind(frameIndex);
        }
    }

    public static boolean isPrimitiveFrameSlotKind(Frame frame, int frameIndex) {
        return !FrameIndex.representsAuxiliaryIndex(frameIndex) && frame.getTag(FrameIndex.toNormalIndex(frameIndex)) != FrameSlotKind.Object.ordinal();
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
        invalidateAllLookups(target);

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

    private static void invalidateAllLookups(FrameDescriptorMetaData target) {
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
                invalidateAllLookups(getDescriptorMetadata(sub));
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

        invalidatePreviousLookups(oldEnclosing, oldEnclosing.getIdentifiers());

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

        invalidateAllLookups(newEnclosing);
        invalidatePreviousLookups(position, newEnclosing.getIdentifiers());

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
     * This class represents metadata about one particular frame slot and is saved inside
     * {@link FrameDescriptor}. It should not be associated with a value of any frame slot.
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
         * An instance of {@link FrameSlotInfo} represents metadata of one frame slot inside a
         * particular {@link FrameDescriptor}. This stable value is therefore stored under
         * {@link FrameDescriptor}, which means that the stable value is the same for any
         * {@link Frame} with such a {@link FrameDescriptor}.
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
        public synchronized void setMultiSlot(MaterializedFrame frame, int frameIndex, Object newValue) {
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

        @Override
        public String toString() {
            return "FrameSlotInfo{" +
                            "identifier='" + identifier + "'" +
                            ", stableValue=" + stableValue +
                            ", invalidationCount=" + invalidationCount +
                            ", nonLocalModifiedAssumption=" + nonLocalModifiedAssumption +
                            ", noMultiSlot=" + noMultiSlot +
                            ", possibleMultiSlot=" + possibleMultiSlot +
                            '}';
        }
    }

    /**
     * Retrieves the not-changed-locally {@link Assumption} for the given frame slot.
     *
     * @return The "not changed locally" assumption of the given slot at {@code frameIndex}.
     */
    public static Assumption getNotChangedNonLocallyAssumption(Frame frame, int frameIndex) {
        return getNotChangedNonLocallyAssumption(frame.getFrameDescriptor(), frameIndex);
    }

    private static Assumption getNotChangedNonLocallyAssumption(FrameDescriptor frameDescriptor, int frameIndex) {
        return getFrameSlotInfo(frameDescriptor, frameIndex).nonLocalModifiedAssumption;
    }

    public static synchronized int findOrAddAuxiliaryFrameSlot(FrameDescriptor frameDescriptor, Object identifier) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptorMetaData descriptorMetadata = getDescriptorMetadata(frameDescriptor);
        int auxSlotIdx = frameDescriptor.findOrAddAuxiliarySlot(identifier);
        int transformedAuxSlotIdx = FrameIndex.transformAuxiliaryIndex(auxSlotIdx);
        Integer frameIndex = descriptorMetadata.getIndex(identifier);
        if (frameIndex == null) {
            // The identifier was not in the frameDescriptor before, we have to put it there and
            // invalidate all the related assumptions.
            descriptorMetadata.addIndex(identifier, transformedAuxSlotIdx);
            var slotInfo = new FrameSlotInfo(descriptorMetadata, identifier);
            descriptorMetadata.addAuxSlotInfo(slotInfo);
            invalidatePreviousLookups(descriptorMetadata, Collections.singletonList(identifier));
            descriptorMetadata.tryInvalidateNotInFrameAssumption(identifier);
        } else {
            if (FrameIndex.representsNormalIndex(frameIndex)) {
                throw RInternalError.shouldNotReachHere("Frame index for '" + identifier + "' already present as normal frame index");
            }
        }
        assert assertValidFrameDescriptor(frameDescriptor);
        return transformedAuxSlotIdx;
    }

    // methods for changing frame slot contents

    /**
     * Checks if the assumption of the given {@code frameIndex} has to be invalidated.
     *
     * @param curFrame Current frame.
     * @param frameIndex Index of the slot into the frame; its "info" is assumed to be an
     *            Assumption, throws an {@link RInternalError} otherwise
     * @param invalidateProfile Used to guard the invalidation code.
     * @param currFrameDescriptor An optional Frame descriptor of {@code curFrame}. Passing non-null
     *            descriptor is useful when the passed descriptor is profiled in the caller.
     */
    private static void checkAndInvalidate(Frame curFrame, int frameIndex, boolean isNonLocal, BranchProfile invalidateProfile, FrameDescriptor currFrameDescriptor) {
        assert containsIndex(curFrame, frameIndex);
        FrameDescriptor frameDescriptor;
        if (currFrameDescriptor != null) {
            assert currFrameDescriptor == curFrame.getFrameDescriptor();
            frameDescriptor = currFrameDescriptor;
        } else {
            frameDescriptor = curFrame.getFrameDescriptor();
        }
        if (getNotChangedNonLocallyAssumption(frameDescriptor, frameIndex).isValid()) {
            // Check whether current frame is used outside a regular stack
            if (isNonLocal || RArguments.getIsIrregular(curFrame)) {
                // False positive: Also invalidates a slot in the current active frame if that one
                // is used inside eval or the like, but this cost is definitely negligible.
                if (invalidateProfile != null) {
                    invalidateProfile.enter();
                }
                getNotChangedNonLocallyAssumption(frameDescriptor, frameIndex).invalidate();
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
        assert 0 <= auxFrameIdx && auxFrameIdx < frame.getFrameDescriptor().getNumberOfAuxiliarySlots();
        frame.setAuxiliarySlot(auxFrameIdx, value);
    }

    public static void setObject(Frame frame, Object identifier, Object newValue) {
        FrameDescriptorMetaData descriptorMetaData = getDescriptorMetadata(frame);
        Integer frameIndex = descriptorMetaData.getIndex(identifier);
        assert frameIndex != null : "A frame slot should first be added with findOrAddAuxiliaryFrameSlot";
        setObject(frame, (int) frameIndex, newValue);
    }

    public static void setObject(Frame frame, int frameIndex, Object newValue) {
        assert FrameIndex.isInitializedIndex(frameIndex);
        assert isFrameIndexInBounds(frame, frameIndex);
        if (hasSharedContext()) {
            FrameSlotInfo slotInfo = getFrameSlotInfo(frame, frameIndex);
            if (isMultislot(slotInfo)) {
                slotInfo.setMultiSlot(frame.materialize(), frameIndex, newValue);
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

    /**
     *
     * @param frame
     * @param frameIndex
     * @param newValue
     * @param isNonLocal
     * @param invalidateProfile Branch profile for the invalidation.
     * @param frameDescriptorProfile Identity profile for the frame descriptor of {@code frame}.
     */
    public static void setObjectAndInvalidate(Frame frame, int frameIndex, Object newValue, boolean isNonLocal, BranchProfile invalidateProfile, ValueProfile frameDescriptorProfile) {
        assert !ActiveBinding.isActiveBinding(newValue);
        assert invalidateProfile != null;
        assert frameDescriptorProfile != null;
        setAndInvalidate(frame, frameIndex, newValue, isNonLocal, invalidateProfile, frameDescriptorProfile);
    }

    /**
     * Slow-path version of
     * {@link #setObjectAndInvalidate(Frame, int, Object, boolean, BranchProfile, ValueProfile)}.
     */
    public static void setObjectAndInvalidate(Frame frame, int frameIndex, Object newValue, boolean isNonLocal) {
        setObjectAndInvalidate(frame, frameIndex, newValue, isNonLocal, BranchProfile.getUncached(), ValueProfile.getUncached());
    }

    private static void setAndInvalidate(Frame frame, int frameIndex, Object newValue, boolean isNonLocal, BranchProfile invalidateProfile, ValueProfile frameDescriptorProfile) {
        assert frameDescriptorProfile != null;
        FrameDescriptor profiledFrameDescriptor = frameDescriptorProfile.profile(frame.getFrameDescriptor());
        FrameSlotInfo slotInfo = getFrameSlotInfo(profiledFrameDescriptor, frameIndex);
        if (hasSharedContext() && isMultislot(slotInfo)) {
            slotInfo.setMultiSlot(frame.materialize(), frameIndex, newValue);
        } else {
            setObject(frame, frameIndex, newValue);
            if (slotInfo.needsInvalidation()) {
                slotInfo.setValue(newValue);
            }
            checkAndInvalidate(frame, frameIndex, isNonLocal, invalidateProfile, profiledFrameDescriptor);
        }
    }

    public static void setBoolean(Frame frame, int frameIndex, boolean newValue) {
        assert isFrameIndexInBounds(frame, frameIndex);
        if (hasSharedContext()) {
            FrameSlotInfo slotInfo = getFrameSlotInfo(frame, frameIndex);
            if (isMultislot(slotInfo)) {
                slotInfo.setMultiSlot(frame.materialize(), frameIndex, newValue);
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
        if (hasSharedContext()) {
            FrameSlotInfo slotInfo = getFrameSlotInfo(frame, frameIndex);
            if (isMultislot(slotInfo)) {
                slotInfo.setMultiSlot(frame.materialize(), frameIndex, newValue);
            }
        } else {
            if (FrameIndex.representsNormalIndex(frameIndex)) {
                frame.setByte(FrameIndex.toNormalIndex(frameIndex), newValue);
            } else {
                setAuxiliaryValue(frame, FrameIndex.toAuxiliaryIndex(frameIndex), newValue);
            }
        }
    }

    public static void setByteAndInvalidate(Frame frame, int index, byte newValue, boolean isNonLocal, BranchProfile invalidateProfile, ValueProfile frameDescriptorProfile) {
        assert frameDescriptorProfile != null;
        FrameDescriptor profiledFrameDescriptor = frameDescriptorProfile.profile(frame.getFrameDescriptor());
        FrameSlotInfo slotInfo = getFrameSlotInfo(profiledFrameDescriptor, index);
        if (hasSharedContext()) {
            slotInfo.setMultiSlot(frame.materialize(), index, newValue);
        } else {
            setByte(frame, index, newValue);
            if (slotInfo.needsInvalidation()) {
                slotInfo.setValue(newValue);
            }
            checkAndInvalidate(frame, index, isNonLocal, invalidateProfile, profiledFrameDescriptor);
        }
    }

    /**
     * Slow-path version of
     * {@link #setByteAndInvalidate(Frame, int, byte, boolean, BranchProfile, ValueProfile)}.
     */
    public static void setByteAndInvalidate(Frame frame, int index, byte newValue, boolean isNonLocal) {
        setByteAndInvalidate(frame, index, newValue, isNonLocal, null, ValueProfile.getUncached());
    }

    public static void setInt(Frame frame, int frameIndex, int newValue) {
        if (hasSharedContext()) {
            FrameSlotInfo slotInfo = getFrameSlotInfo(frame, frameIndex);
            if (isMultislot(slotInfo)) {
                slotInfo.setMultiSlot(frame.materialize(), frameIndex, newValue);
            }
        } else {
            if (FrameIndex.representsNormalIndex(frameIndex)) {
                frame.setInt(FrameIndex.toNormalIndex(frameIndex), newValue);
            } else {
                setAuxiliaryValue(frame, FrameIndex.toAuxiliaryIndex(frameIndex), newValue);
            }
        }
    }

    public static void setIntAndInvalidate(Frame frame, int frameIndex, int newValue, boolean isNonLocal, BranchProfile invalidateProfile, ValueProfile frameDescriptorProfile) {
        assert frameDescriptorProfile != null;
        FrameDescriptor profiledFrameDescriptor = frameDescriptorProfile.profile(frame.getFrameDescriptor());
        FrameSlotInfo slotInfo = getFrameSlotInfo(profiledFrameDescriptor, frameIndex);
        if (hasSharedContext()) {
            slotInfo.setMultiSlot(frame.materialize(), frameIndex, newValue);
        } else {
            setInt(frame, frameIndex, newValue);
            if (slotInfo.needsInvalidation()) {
                slotInfo.setValue(newValue);
            }
            checkAndInvalidate(frame, frameIndex, isNonLocal, invalidateProfile, profiledFrameDescriptor);
        }
    }

    /**
     * Slow-path version of
     * {@link #setIntAndInvalidate(Frame, int, int, boolean, BranchProfile, ValueProfile)}.
     */
    public static void setIntAndInvalidate(Frame frame, int frameIndex, int newValue, boolean isNonLocal) {
        setIntAndInvalidate(frame, frameIndex, newValue, isNonLocal, null, ValueProfile.getUncached());
    }

    public static void setDouble(Frame frame, int frameIndex, double newValue) {
        if (hasSharedContext()) {
            FrameSlotInfo slotInfo = getFrameSlotInfo(frame, frameIndex);
            if (isMultislot(slotInfo)) {
                slotInfo.setMultiSlot(frame.materialize(), frameIndex, newValue);
            }
        } else {
            if (FrameIndex.representsNormalIndex(frameIndex)) {
                frame.setDouble(FrameIndex.toNormalIndex(frameIndex), newValue);
            } else {
                setAuxiliaryValue(frame, FrameIndex.toAuxiliaryIndex(frameIndex), newValue);
            }
        }
    }

    public static void setDoubleAndInvalidate(Frame frame, int frameIndex, double newValue, boolean isNonLocal, BranchProfile invalidateProfile, ValueProfile frameDescriptorProfile) {
        assert frameDescriptorProfile != null;
        FrameDescriptor profiledFrameDescriptor = frameDescriptorProfile.profile(frame.getFrameDescriptor());
        FrameSlotInfo slotInfo = getFrameSlotInfo(profiledFrameDescriptor, frameIndex);
        if (hasSharedContext()) {
            slotInfo.setMultiSlot(frame.materialize(), frameIndex, newValue);
        } else {
            setDouble(frame, frameIndex, newValue);
            if (slotInfo.needsInvalidation()) {
                slotInfo.setValue(newValue);
            }
            checkAndInvalidate(frame, frameIndex, isNonLocal, invalidateProfile, profiledFrameDescriptor);
        }
    }

    /**
     * Slow-path version of
     * {@link #setDoubleAndInvalidate(Frame, int, double, boolean, BranchProfile, ValueProfile)}.
     */
    public static void setDoubleAndInvalidate(Frame frame, int frameIndex, double newValue, boolean isNonLocal) {
        setDoubleAndInvalidate(frame, frameIndex, newValue, isNonLocal, null, ValueProfile.getUncached());
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
            if (FrameIndex.representsNormalIndex(frameIndex)) {
                return frame.getValue(FrameIndex.toNormalIndex(frameIndex));
            } else {
                return frame.getAuxiliarySlot(FrameIndex.toAuxiliaryIndex(frameIndex));
            }
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
                    assert containsIndex(frame.materialize(), frameIndex);
                    o = getObjectByIndex(frame.materialize(), frameIndex);
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
        if (FrameIndex.representsNormalIndex(frameIndex)) {
            return frame.getBoolean(FrameIndex.toNormalIndex(frameIndex));
        } else {
            return getAuxiliaryCheckedObject(frame, frameIndex, Boolean.class);
        }
    }

    public static byte getByte(Frame frame, int frameIndex) {
        assert isFrameIndexInBounds(frame, frameIndex);
        if (FrameIndex.representsNormalIndex(frameIndex)) {
            return frame.getByte(frameIndex);
        } else {
            return getAuxiliaryCheckedObject(frame, frameIndex, Byte.class);
        }
    }

    public static int getInt(Frame frame, int frameIndex) {
        assert isFrameIndexInBounds(frame, frameIndex);
        if (FrameIndex.representsNormalIndex(frameIndex)) {
            return frame.getInt(FrameIndex.toNormalIndex(frameIndex));
        } else {
            return getAuxiliaryCheckedObject(frame, frameIndex, Integer.class);
        }
    }

    public static double getDouble(Frame frame, int frameIndex) {
        assert isFrameIndexInBounds(frame, frameIndex);
        if (FrameIndex.representsNormalIndex(frameIndex)) {
            return frame.getDouble(FrameIndex.toNormalIndex(frameIndex));
        } else {
            return getAuxiliaryCheckedObject(frame, frameIndex, Double.class);
        }
    }

    /**
     * @param invalidateProfile Branch profile for the invalidation.
     * @param frameDescriptorProfile Identity profile for the frame descriptor of {@code frame}.
     */
    public static void setActiveBinding(Frame frame, int frameIndex, ActiveBinding newValue, boolean isNonLocal, BranchProfile invalidateProfile, ValueProfile frameDescriptorProfile) {
        setAndInvalidate(frame, frameIndex, newValue, isNonLocal, invalidateProfile, frameDescriptorProfile);
        getContainsNoActiveBindingAssumption(frame.getFrameDescriptor()).invalidate();
    }

    /**
     * Slow-path version of
     * {@link #setActiveBinding(Frame, int, ActiveBinding, boolean, BranchProfile, ValueProfile)}.
     */
    public static void setActiveBinding(Frame frame, int frameIndex, ActiveBinding newValue, boolean isNonLocal) {
        setActiveBinding(frame, frameIndex, newValue, isNonLocal, BranchProfile.getUncached(), ValueProfile.getUncached());
    }

    /**
     * Initializes the internal data structures for a newly created frame descriptor that is
     * intended to be used for a non-function frame (and thus will only ever be used for one frame).
     */
    public static void initializeNonFunctionFrameDescriptor(FrameDescriptor frameDescriptor, MaterializedFrame singletonFrame) {
        assert singletonFrame != null;
        FrameDescriptorMetaData metaData = getDescriptorMetadata(frameDescriptor);
        metaData.setSingletonFrame(singletonFrame);
    }

    public static Assumption getEnclosingFrameDescriptorAssumption(FrameDescriptor frameDescriptor) {
        return getDescriptorMetadata(frameDescriptor).getEnclosingFrameDescriptorAssumption();
    }

    public static Assumption getContainsNoActiveBindingAssumption(FrameDescriptor descriptor) {
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

    public static Assumption getNotInFrameAssumption(FrameDescriptor frameDescriptor, Object identifier) {
        assert !containsIdentifier(frameDescriptor, identifier) : "Cannot get notInFrameAssumption for an existing identifier";
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptorMetaData metaData = getDescriptorMetadata(frameDescriptor);
        return metaData.getNotInFrameAssumption(identifier);
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

    public static String frameDescriptorMetadataToString(FrameDescriptor frameDescriptor) {
        FrameDescriptorMetaData frameDescriptorMetaData = getDescriptorMetadata(frameDescriptor);
        return frameDescriptorMetaData.toString();
    }

    public static String frameSlotInfoToString(Frame frame, Object identifier) {
        FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        assert containsIdentifier(frameDescriptor, identifier);
        int frameIndex = getIndexOfIdentifier(frameDescriptor, identifier);
        FrameSlotInfo slotInfo = getFrameSlotInfo(frame, frameIndex);
        return slotInfo.toString();
    }
}
