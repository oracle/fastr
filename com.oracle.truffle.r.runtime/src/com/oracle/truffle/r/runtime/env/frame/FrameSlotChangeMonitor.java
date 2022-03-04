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
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameDescriptor.Builder;
import com.oracle.truffle.api.frame.FrameSlot;
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
import com.oracle.truffle.r.runtime.RLogger;
import com.oracle.truffle.r.runtime.StableValue;
import com.oracle.truffle.r.runtime.context.ChildContextInfo;
import com.oracle.truffle.r.runtime.context.FastROptions;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.RUnboundValue;
import com.oracle.truffle.r.runtime.env.frame.FrameIndex.IndexType;

/**
 * This class maintains information about the current hierarchy of environments in the system. This
 * information is described as assumptions that will be invalidated if the layout changes, and thus
 * make sure that code is properly deoptimized.
 */
public final class FrameSlotChangeMonitor {

    private static final TruffleLogger logger = RLogger.getLogger(RLogger.LOGGER_FRAMES);
    private static final boolean NEW_FRAME_STRUCTURE_ASSERTS = true;

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
            return FrameSlotChangeMonitor.getValue(slot, frame);
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
        private final Map<Object, FrameIndex> indexes = new ConcurrentHashMap<>();

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

        public void addIndex(Object identifier, FrameIndex index) {
            CompilerAsserts.neverPartOfCompilation();
            indexes.put(identifier, index);
        }

        public FrameIndex getIndex(Object identifier) {
            return indexes.get(identifier);
        }

        // TODO: Make more performant
        public Object getIdentifier(FrameIndex frameIndex) {
            CompilerAsserts.neverPartOfCompilation();
            for (Map.Entry<Object, FrameIndex> entry : indexes.entrySet()) {
                if (entry.getValue().equals(frameIndex)) {
                    return entry.getKey();
                }
            }
            return null;
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
                // if stableValue.getValue() == null, then this is a frame slot that doesn't have a
                // value, which can happen, e.g., when package creates a value in its namespace, but
                // then removes it in .onLoad
                if (stableValue == null || stableValue.getValue() != null) {
                    if (stableValue != null) {
                        lookupResult = new StableValueLookupResult(identifier.toString(), stableValue);
                    } else {
                        FrameDescriptorMetaData currentMetaData = getMetaData(current);
                        if (currentMetaData.singletonFrame == null) {
                            // no stable value and no singleton frame
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

    @Deprecated
    private static synchronized FrameDescriptorMetaData getMetaData(FrameDescriptor descriptor) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptorMetaData result = frameDescriptors.get(descriptor);
        assert result != null : "null metadata for " + descriptor;
        return result;
    }

    @Deprecated
    private static FrameDescriptorMetaData getMetaData(Frame frame) {
        return getMetaData(handleBaseNamespaceEnv(frame));
    }

    private static FrameDescriptorMetaData getMetaDataNew(FrameDescriptor frameDescriptor) {
        Object descriptorInfo = frameDescriptor.getInfo();
        if (!(descriptorInfo instanceof FrameDescriptorMetaData)) {
            throw RInternalError.shouldNotReachHere("FrameDescriptor metadata should only be instances of FrameDescriptorMetaData");
        }
        return (FrameDescriptorMetaData) descriptorInfo;
    }

    private static FrameDescriptorMetaData getMetaDataNew(Frame frame) {
        return getMetaDataNew(handleBaseNamespaceEnv(frame));
    }

    @Deprecated
    private static FrameDescriptorMetaData getDescriptorMetaData(FrameDescriptor descriptor) {
        assert descriptor != null : "initializing enclosing of null descriptor";
        FrameDescriptorMetaData target = getMetaData(descriptor);
        assert target != null : "frame descriptor wasn't registered properly";
        return target;
    }

    public static FrameSlotKind getFrameSlotKindNew(FrameDescriptor frameDescriptor, FrameIndex index) {
        switch (index.indexType) {
            case NormalIndex:
                return frameDescriptor.getSlotKind(index.index);
            case AuxiliaryIndex:
                // TODO: Anything in auxiliary index is always object frame slot kind?
                return FrameSlotKind.Object;
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    public static void setFrameSlotKindNew(FrameDescriptor frameDescriptor, FrameIndex index, FrameSlotKind kind) {
        // It does not make sense to set any FrameSlotKind for an auxiliary value - they are always
        // objects, so we take care only of normal values.
        if (index.indexType == IndexType.NormalIndex) {
            frameDescriptor.setSlotKind(index.index, kind);
        }
    }

    public static synchronized boolean isEnclosingFrameDescriptor(FrameDescriptor descriptor, Frame newEnclosingFrame) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptorMetaData target = getDescriptorMetaData(descriptor);
        FrameDescriptor newEnclosingDescriptor = handleBaseNamespaceEnv(newEnclosingFrame);
        return target.getEnclosingFrameDescriptor() == newEnclosingDescriptor;
    }

    @Deprecated
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

    public static void initializeEnclosingFrameNew(FrameDescriptor frameDescriptor, Frame newEnclosingFrame) {
        FrameDescriptorMetaData target = getMetaDataNew(frameDescriptor);

        FrameDescriptor newEnclosingDescriptor = handleBaseNamespaceEnv(newEnclosingFrame);

        // this function can be called multiple times with the same enclosing descriptor
        if (target.getEnclosingFrameDescriptor() != newEnclosingDescriptor) {
            assert target.getEnclosingFrameDescriptor() == null : "existing enclosing descriptor while initializing " + target.name;
            assert target.lookupResults.isEmpty() : "existing lookup results while initializing " + target.name;

            target.updateEnclosingFrameDescriptor(newEnclosingDescriptor);
            if (newEnclosingDescriptor != null) {
                FrameDescriptorMetaData newEnclosing = getMetaDataNew(newEnclosingDescriptor);
                newEnclosing.subDescriptors.add(frameDescriptor);
            }
        }
    }

    @Deprecated
    public static synchronized void initializeEnclosingFrame(Frame frame, Frame newEnclosingFrame) {
        initializeEnclosingFrame(handleBaseNamespaceEnv(frame), newEnclosingFrame);
    }

    public static void initializeEnclosingFrameNew(Frame frame, Frame newEnclosingFrame) {
        initializeEnclosingFrameNew(handleBaseNamespaceEnv(frame), newEnclosingFrame);
    }

    private static synchronized void setEnclosingFrame(FrameDescriptor descriptor, MaterializedFrame newEnclosingFrame, MaterializedFrame oldEnclosingFrame) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptorMetaData target = getMetaData(descriptor);
        assert target != null : "frame descriptor wasn't registered properly for " + descriptor;

        // invalidate existing lookups
        invalidateAllNames(target);

        FrameDescriptor oldEnclosingDescriptor = target.getEnclosingFrameDescriptor();
        FrameDescriptor newEnclosingDescriptor = handleBaseNamespaceEnv(newEnclosingFrame);
        assert newEnclosingDescriptor == oldEnclosingDescriptor || (oldEnclosingDescriptor == null) == (oldEnclosingFrame == null) : "mismatch " + oldEnclosingDescriptor + " / " + oldEnclosingFrame;

        if (oldEnclosingDescriptor != null) {
            assert newEnclosingDescriptor == oldEnclosingDescriptor || oldEnclosingDescriptor == oldEnclosingFrame.getFrameDescriptor() : "mismatch " + oldEnclosingDescriptor + " / " +
                            oldEnclosingFrame.getFrameDescriptor();
            FrameDescriptorMetaData oldEnclosing = getMetaData(oldEnclosingDescriptor);
            oldEnclosing.subDescriptors.remove(descriptor);
        }
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

    private static final class FrameSlotInfoImpl {
        /**
         * This is meant to monitor updates performed on {@code FrameSlot}. Each {@code FrameSlot}
         * holds an {@link Assumption} in it's "info" field; it is valid as long as no non-local
         * update has ever taken place.<br/>
         * The background to this rather strange assumption is that non-local reads are very hard to
         * keep track of thanks to R powerful language features. To keep the maintenance for the
         * assumption as cheap as possible, it checks only local reads - which is fast - and does a
         * more costly check on "<<-" but invalidates the assumption as soon as "eval" and the like
         * comes into play.<br/>
         */
        private final Assumption nonLocalModifiedAssumption;
        private final Assumption noMultiSlot;

        @CompilationFinal private volatile StableValue<Object> stableValue;
        private int invalidationCount;
        private final boolean possibleMultiSlot;

        FrameSlotInfoImpl(boolean isSingletonFrame, boolean isGlobalEnv, Object identifier, boolean isNewEnv) {
            nonLocalModifiedAssumption = Truffle.getRuntime().createAssumption(identifier + ":NonLocalModified");
            noMultiSlot = Truffle.getRuntime().createAssumption(identifier + ":NoMultiSlot");
            this.possibleMultiSlot = isSingletonFrame && !isNewEnv;
            if (isSingletonFrame) {
                stableValue = new StableValue<>(null, identifier.toString());
                invalidationCount = isGlobalEnv ? MAX_GLOBAL_ENV_INVALIDATION_COUNT : MAX_INVALIDATION_COUNT;
            } else {
                stableValue = null;
            }
        }

        FrameSlotInfoImpl(FrameDescriptorMetaData metaData, Object identifier) {
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

        private void setValue(boolean value, FrameSlot slot) {
            StableValue<Object> sv = stableValue;
            if (sv != null && (!(sv.getValue() instanceof Boolean) || ((boolean) sv.getValue()) != value)) {
                invalidateStableValue(sv, value, slot);
            }
        }

        private void setValue(byte value, FrameSlot slot) {
            StableValue<Object> sv = stableValue;
            if (sv != null && (!(sv.getValue() instanceof Byte) || ((byte) sv.getValue()) != value)) {
                invalidateStableValue(sv, value, slot);
            }
        }

        private void setValue(int value, FrameSlot slot) {
            StableValue<Object> sv = stableValue;
            if (sv != null && (!(sv.getValue() instanceof Integer) || ((int) sv.getValue()) != value)) {
                invalidateStableValue(sv, value, slot);
            }
        }

        private void setValue(double value, FrameSlot slot) {
            StableValue<Object> sv = stableValue;
            if (sv != null && (!(sv.getValue() instanceof Double) || ((double) sv.getValue()) != value)) {
                invalidateStableValue(sv, value, slot);
            }
        }

        @Deprecated
        private void setValue(Object value, FrameSlot slot) {
            StableValue<Object> sv = stableValue;
            if (sv != null && sv.getValue() != value) {
                invalidateStableValue(sv, value, slot);
            }
        }

        private void setValueNew(Object value, Object identifier) {
            StableValue<Object> sv = stableValue;
            if (sv != null && sv.getValue() != value) {
                invalidateStableValueNew(sv, value, identifier);
            }
        }

        @Deprecated
        private void invalidateStableValue(StableValue<Object> sv, Object value, FrameSlot slot) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            sv.getAssumption().invalidate();
            if (invalidationCount > 0) {
                invalidationCount--;
                out("setting singleton value %s = %s", slot.getIdentifier(), value == null ? "null" : value.getClass());
                stableValue = new StableValue<>(value, String.valueOf(slot.getIdentifier()));
            } else {
                out("setting non-singleton value %s", slot.getIdentifier());
                stableValue = null;
            }
        }

        private void invalidateStableValueNew(StableValue<Object> sv, Object value, Object identifier) {
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

        private static void setNewMultiValue(Frame frame, FrameSlot slot, MultiSlotData data, Object newValue) {
            int ind = RContext.getInstance().getMultiSlotInd();
            data.set(ind, newValue);
            frame.setObject(slot, data);
        }

        private static boolean evalAndSetPromise(Frame frame, FrameSlot slot, FrameSlotInfoImpl info) {
            Object prevValue = info.stableValue.getValue();
            if (prevValue instanceof RPromise) {
                prevValue = RContext.getRRuntimeASTAccess().forcePromise("searchPathPromiseForce", prevValue);
                if (prevValue instanceof Boolean) {
                    frame.setBoolean(slot, (boolean) prevValue);
                    info.setValue((boolean) prevValue, slot);
                } else if (prevValue instanceof Byte) {
                    frame.setByte(slot, (byte) prevValue);
                    info.setValue((byte) prevValue, slot);
                } else if (prevValue instanceof Integer) {
                    frame.setInt(slot, (int) prevValue);
                    info.setValue((int) prevValue, slot);
                } else if (prevValue instanceof Double) {
                    frame.setDouble(slot, (double) prevValue);
                    info.setValue((double) prevValue, slot);
                } else {
                    frame.setObject(slot, prevValue);
                    info.setValue(prevValue, slot);
                }
                return true;
            } else {
                return false;
            }
        }

        public static void handleSearchPathMultiSlot(Frame frame, FrameSlot slot, int[] indices, boolean replicate) {
            CompilerAsserts.neverPartOfCompilation();
            while (true) {
                FrameSlotInfoImpl info = (FrameSlotInfoImpl) slot.getInfo();
                Object prevValue = frame.getValue(slot);
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
                    frame.setObject(slot, data);
                    break;
                } else {
                    if (!RContext.getInstance().getOption(SearchPathForcePromises) || !evalAndSetPromise(frame, slot, info)) {
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
        public synchronized void setMultiSlot(Frame frame, FrameSlot slot, Object newValue) {
            // TODO: perhaps putting the whole thing behind the Truffle boundary an overkill, but on
            // the other hand it shouldn't happen often and not on the fast path
            MultiSlotData data;
            if (stableValue == null) {
                // already a multi slot - should be visible to all threads
                assert slotExists(slot, frame) : slot;
                data = (MultiSlotData) frame.getValue(slot);
                assert data != null : slot;
                int ind = RContext.getInstance().getMultiSlotInd();
                data.set(ind, newValue);
            } else {
                nonLocalModifiedAssumption.invalidate();
                invalidationCount = 0;
                // TODO: is it necessary since we nullify stable value?
                stableValue.getAssumption().invalidate();
                noMultiSlot.invalidate();
                data = new MultiSlotData();
                Object prevValue = frame.getValue(slot);
                // value was stable so this slot is set by primordial context
                data.set(0, prevValue);
                setNewMultiValue(frame, slot, data, newValue);
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
     * @return The "not changed locally" assumption of the given {@code FrameSlot}
     */
    @Deprecated
    public static Assumption getNotChangedNonLocallyAssumption(FrameSlot slot) {
        return getFrameSlotInfo(slot).nonLocalModifiedAssumption;
    }

    public static Assumption getNotChangedNonLocallyAssumptionNew(Frame frame, FrameIndex frameIndex) {
        Assumption notChangedLocallyAssumption = getFrameSlotInfoNew(frame, frameIndex).nonLocalModifiedAssumption;
        assertFrameStructureNew(frame);
        return notChangedLocallyAssumption;
    }

    @Deprecated
    private static FrameSlotInfoImpl getFrameSlotInfo(FrameSlot slot) {
        Object info = slot.getInfo();
        if (!(info instanceof FrameSlotInfoImpl)) {
            CompilerDirectives.transferToInterpreter();
            throw RInternalError.shouldNotReachHere("Each FrameSlot should hold a FrameSlotInfo in its info field! " + slot.getIdentifier().getClass() + " " + slot.getIdentifier());
        }
        return (FrameSlotInfoImpl) info;
    }

    /**
     * A wrapper class for a value of each auxiliary slot.
     * TODO: Mark as dataclass
     */
    private static final class AuxiliarySlotValue {
        public final FrameSlotInfoImpl frameSlotInfo;
        public final Object identifier;
        // TODO: @CompilationFinal?
        public Object value;

        public AuxiliarySlotValue(FrameSlotInfoImpl frameSlotInfo, Object identifier) {
            this(frameSlotInfo, identifier, null);
        }

        private AuxiliarySlotValue(FrameSlotInfoImpl frameSlotInfo, Object identifier, Object value) {
            assert frameSlotInfo != null;
            assert identifier != null;
            this.frameSlotInfo = frameSlotInfo;
            this.identifier = identifier;
            this.value = value;
        }
    }

    private static void initializeAuxFrameSlotInfoNew(Frame frame, int auxFrameIdx, Object identifier) {
        assert frame.getAuxiliarySlot(auxFrameIdx) == null : "FrameSlotInfo for an aux slot should be initialized just once";
        FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        assert 0 <= auxFrameIdx && auxFrameIdx < frameDescriptor.getNumberOfAuxiliarySlots();
        FrameDescriptorMetaData metadata = getMetaDataNew(frameDescriptor);
        var frameSlotInfo = new FrameSlotInfoImpl(metadata, identifier);
        var auxSlotInfo = new AuxiliarySlotValue(frameSlotInfo, identifier);
        frame.setAuxiliarySlot(auxFrameIdx, auxSlotInfo);
    }

    private static FrameSlotInfoImpl getFrameSlotInfoNew(Frame frame, FrameIndex frameIndex) {
        FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        Object frameSlotInfo = null;
        switch (frameIndex.indexType) {
            case NormalIndex:
                frameSlotInfo = frameDescriptor.getSlotInfo(frameIndex.index);
                break;
            case AuxiliaryIndex:
                int auxFrameIdx = frameIndex.index;
                if (frame.getAuxiliarySlot(auxFrameIdx) == null) {
                    Object identifier = getIdentifierNew(frame, frameIndex);
                    initializeAuxFrameSlotInfoNew(frame, auxFrameIdx, identifier);
                }
                AuxiliarySlotValue auxiliarySlotValue = (AuxiliarySlotValue) frame.getAuxiliarySlot(auxFrameIdx);
                assert auxiliarySlotValue != null;
                frameSlotInfo = auxiliarySlotValue.frameSlotInfo;
                break;
        }
        if (!(frameSlotInfo instanceof FrameSlotInfoImpl)) {
            throw RInternalError.shouldNotReachHere();
        }
        return (FrameSlotInfoImpl) frameSlotInfo;
    }

    // methods for creating new frame slots

    @Deprecated
    public static FrameSlot findOrAddFrameSlot(FrameDescriptor fd, String identifier, FrameSlotKind initialKind) {
        return findOrAddFrameSlot(fd, (Object) identifier, initialKind);
    }

    public static FrameIndex findOrAddAuxiliaryFrameSlotNew(FrameDescriptor frameDescriptor, Object identifier) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptorMetaData descriptorMetadata = getMetaDataNew(frameDescriptor);
        int auxSlotIdx = frameDescriptor.findOrAddAuxiliarySlot(identifier);
        if (descriptorMetadata.getIndex(identifier) == null) {
            var frameIndex = new FrameIndex(IndexType.AuxiliaryIndex, auxSlotIdx);
            descriptorMetadata.addIndex(identifier, frameIndex);
        }
        return descriptorMetadata.getIndex(identifier);
    }

    public static FrameIndex getIndexOfIdentifier(FrameDescriptor frameDescriptor, Object identifier) {
        FrameDescriptorMetaData descriptorMetadata = getMetaDataNew(frameDescriptor);
        return descriptorMetadata.getIndex(identifier);
    }

    @Deprecated
    public static FrameSlot findOrAddFrameSlot(FrameDescriptor fd, RFrameSlot identifier, FrameSlotKind initialKind) {
        return findOrAddFrameSlot(fd, (Object) identifier, initialKind);
    }

    @Deprecated
    private static synchronized FrameSlot findOrAddFrameSlot(FrameDescriptor fd, Object identifier, FrameSlotKind initialKind) {
        CompilerAsserts.neverPartOfCompilation();
        assert identifier instanceof String || identifier instanceof RFrameSlot;
        FrameSlot frameSlot = fd.findFrameSlot(identifier);
        if (frameSlot != null) {
            return frameSlot;
        } else {
            FrameDescriptorMetaData metaData = getMetaData(fd);
            invalidateNames(metaData, Arrays.asList(identifier));
            return fd.addFrameSlot(identifier, new FrameSlotInfoImpl(metaData.singletonFrame != null, "global".equals(metaData.name), identifier, metaData.name.startsWith("<new-env-")),
                            initialKind);
        }
    }

    // methods for changing frame slot contents

    /**
     * Checks if the assumption of the given {@code FrameSlot} has to be invalidated.
     *
     * @param curFrame
     * @param slot {@code FrameSlot}; its "info" is assumed to be an Assumption, throws an
     *            {@link RInternalError} otherwise
     * @param invalidateProfile Used to guard the invalidation code.
     */
    @Deprecated
    private static void checkAndInvalidate(Frame curFrame, FrameSlot slot, boolean isNonLocal, BranchProfile invalidateProfile) {
        assert curFrame.getFrameDescriptor().getSlots().contains(slot) : slot.getIdentifier();
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

    /**
     * Checks if the assumption of the given {@code frameIndex} has to be invalidated.
     *
     * @param curFrame Current frame.
     * @param frameIndex Index of the slot into the frame; its "info" is assumed to be an Assumption, throws an
     *            {@link RInternalError} otherwise
     * @param invalidateProfile Used to guard the invalidation code.
     */
    private static void checkAndInvalidateNew(Frame curFrame, FrameIndex frameIndex, boolean isNonLocal, BranchProfile invalidateProfile) {
        // TODO: Assert that curFrame contains frameIndex
        //  assert curFrame.getFrameDescriptor().getSlots().contains(slot) : slot.getIdentifier();
        if (getNotChangedNonLocallyAssumptionNew(curFrame, frameIndex).isValid()) {
            // Check whether current frame is used outside a regular stack
            if (isNonLocal || RArguments.getIsIrregular(curFrame)) {
                // False positive: Also invalidates a slot in the current active frame if that one
                // is used inside eval or the like, but this cost is definitely negligible.
                if (invalidateProfile != null) {
                    invalidateProfile.enter();
                }
                getNotChangedNonLocallyAssumptionNew(curFrame, frameIndex).invalidate();
            }
        }
    }

    public static void setBooleanAndInvalidate(Frame frame, FrameSlot frameSlot, boolean newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
        if (FastROptions.sharedContextsOptionValue && isMultislot(info) && !RContext.isSingle()) {
            info.setMultiSlot(frame, frameSlot, newValue);
        } else {
            frame.setBoolean(frameSlot, newValue);
            if (info.needsInvalidation()) {
                info.setValue(newValue, frameSlot);
            }
            checkAndInvalidate(frame, frameSlot, isNonLocal, invalidateProfile);
        }
    }

    public static void setBoolean(Frame frame, FrameSlot frameSlot, boolean newValue) {
        if (FastROptions.sharedContextsOptionValue && !RContext.isSingle()) {
            FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
            if (isMultislot(info)) {
                info.setMultiSlot(frame, frameSlot, newValue);
                return;
            }
        }
        frame.setBoolean(frameSlot, newValue);
    }

    @Deprecated
    public static void setByteAndInvalidate(Frame frame, FrameSlot frameSlot, byte newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
        if (FastROptions.sharedContextsOptionValue && isMultislot(info) && !RContext.isSingle()) {
            info.setMultiSlot(frame, frameSlot, newValue);
        } else {
            frame.setByte(frameSlot, newValue);
            if (info.needsInvalidation()) {
                info.setValue(newValue, frameSlot);
            }
            checkAndInvalidate(frame, frameSlot, isNonLocal, invalidateProfile);
        }
    }

    public static void setByteAndInvalidateNew(Frame frame, FrameIndex index, byte newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        assertFrameStructureNew(frame);
        checkSharedContextAfterFrameMigration();
        FrameSlotInfoImpl slotInfo = getFrameSlotInfoNew(frame, index);
        Object identifier = getIdentifierNew(frame, index);
        setByteNew(frame, index, newValue);
        if (slotInfo.needsInvalidation()) {
            slotInfo.setValueNew(newValue, identifier);
        }
        checkAndInvalidateNew(frame, index, isNonLocal, invalidateProfile);
    }

    private static void setByteNew(Frame frame, FrameIndex index, byte newValue) {
        checkSharedContextAfterFrameMigration();
        switch (index.indexType) {
            case NormalIndex:
                frame.setByte(index.index, newValue);
            case AuxiliaryIndex:
                setAuxiliaryValueNew(frame, index.index, getIdentifierNew(frame, index), newValue);
        }
    }

    /**
     * Sets the given value into an auxiliary frame slot on {@code auxFrameIdx} index.
     * All the auxiliary slots have {@code FrameSlotKind.Object} slot kind. Therefore,
     * if we want to set a value there, we always have to box it.
     */
    private static void setAuxiliaryValueNew(Frame frame, int auxFrameIdx, Object identifier, Object value) {
        assert identifier != null;
        assertFrameStructureNew(frame);
        FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        assert 0 <= auxFrameIdx && auxFrameIdx < frameDescriptor.getNumberOfAuxiliarySlots();
        Object auxiliarySlotValue = frame.getAuxiliarySlot(auxFrameIdx);
        if (auxiliarySlotValue == null) {
            // Auxiliary slot was not initialized yet.
            initializeAuxFrameSlotInfoNew(frame, auxFrameIdx, identifier);
            setObjectNew(frame, identifier, value);
        } else {
            assert auxiliarySlotValue instanceof AuxiliarySlotValue;
            assert ((AuxiliarySlotValue) auxiliarySlotValue).identifier == identifier;
            ((AuxiliarySlotValue) auxiliarySlotValue).value = value;
            // TODO: Do we need here another call to frame.setAuxiliarySlot?
        }
    }

    public static void setByte(Frame frame, FrameSlot frameSlot, byte newValue) {
        if (FastROptions.sharedContextsOptionValue && !RContext.isSingle()) {
            FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
            if (isMultislot(info)) {
                info.setMultiSlot(frame, frameSlot, newValue);
                return;
            }
        }
        frame.setByte(frameSlot, newValue);
    }

    public static void setIntAndInvalidate(Frame frame, FrameSlot frameSlot, int newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
        if (FastROptions.sharedContextsOptionValue && isMultislot(info) && !RContext.isSingle()) {
            info.setMultiSlot(frame, frameSlot, newValue);
        } else {
            frame.setInt(frameSlot, newValue);
            if (info.needsInvalidation()) {
                info.setValue(newValue, frameSlot);
            }
            checkAndInvalidate(frame, frameSlot, isNonLocal, invalidateProfile);
        }
    }

    public static void setInt(Frame frame, FrameSlot frameSlot, int newValue) {
        if (FastROptions.sharedContextsOptionValue && !RContext.isSingle()) {
            FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
            if (isMultislot(info)) {
                info.setMultiSlot(frame, frameSlot, newValue);
                return;
            }
        }
        frame.setInt(frameSlot, newValue);
    }

    public static void setDoubleAndInvalidate(Frame frame, FrameSlot frameSlot, double newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
        if (FastROptions.sharedContextsOptionValue && isMultislot(info) && !RContext.isSingle()) {
            info.setMultiSlot(frame, frameSlot, newValue);
        } else {
            frame.setDouble(frameSlot, newValue);
            if (info.needsInvalidation()) {
                info.setValue(newValue, frameSlot);
            }
            checkAndInvalidate(frame, frameSlot, isNonLocal, invalidateProfile);
        }
    }

    public static void setDouble(Frame frame, FrameSlot frameSlot, double newValue) {
        if (FastROptions.sharedContextsOptionValue && !RContext.isSingle()) {
            FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
            if (isMultislot(info)) {
                info.setMultiSlot(frame, frameSlot, newValue);
                return;
            }
        }
        frame.setDouble(frameSlot, newValue);
    }

    @Deprecated
    public static void setObjectAndInvalidate(Frame frame, FrameSlot frameSlot, Object newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        assert !ActiveBinding.isActiveBinding(newValue);
        setAndInvalidate(frame, frameSlot, newValue, isNonLocal, invalidateProfile);
    }

    public static void setObjectAndInvalidateNew(Frame frame, FrameIndex frameIndex, Object newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        assertFrameStructureNew(frame);
        assert !ActiveBinding.isActiveBinding(newValue);
        setAndInvalidateNew(frame, frameIndex, newValue, isNonLocal, invalidateProfile);
    }

    @Deprecated
    private static void setAndInvalidate(Frame frame, FrameSlot frameSlot, Object newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
        if (FastROptions.sharedContextsOptionValue && isMultislot(info) && !RContext.isSingle()) {
            info.setMultiSlot(frame, frameSlot, newValue);
        } else {
            frame.setObject(frameSlot, newValue);
            if (info.needsInvalidation()) {
                info.setValue(newValue, frameSlot);
            }
            checkAndInvalidate(frame, frameSlot, isNonLocal, invalidateProfile);
        }
    }

    private static void setAndInvalidateNew(Frame frame, FrameIndex frameIndex, Object newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        assertFrameStructureNew(frame);
        checkSharedContextAfterFrameMigration();
        FrameSlotInfoImpl slotInfo = getFrameSlotInfoNew(frame, frameIndex);
        Object identifier = getIdentifierNew(frame, frameIndex);
        setObjectNew(frame, frameIndex, newValue);
        if (slotInfo.needsInvalidation()) {
            slotInfo.setValueNew(newValue, identifier);
        }
        checkAndInvalidateNew(frame, frameIndex, isNonLocal, invalidateProfile);
    }

    @Deprecated
    public static void setObject(Frame frame, FrameSlot frameSlot, Object newValue) {
        if (FastROptions.sharedContextsOptionValue && !RContext.isSingle()) {
            FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
            if (isMultislot(info)) {
                info.setMultiSlot(frame, frameSlot, newValue);
                return;
            }
        }
        frame.setObject(frameSlot, newValue);
    }

    public static Object getObjectNew(Frame frame, Object identifier) {
        checkSharedContextAfterFrameMigration();
        FrameIndex index = getIndexOfIdentifier(frame.getFrameDescriptor(), identifier);
        if (index == null) {
            // The object is most probably not in the frame at all.
            // TODO: Throw an exception?
            return null;
        }
        return getObjectNew(frame, index);
    }

    public static Object getObjectNew(Frame frame, FrameIndex index) {
        checkSharedContextAfterFrameMigration();
        Object object = null;
        switch (index.indexType) {
            case NormalIndex:
                object = frame.getObject(index.index);
                break;
            case AuxiliaryIndex:
                Object auxiliarySlotValue = frame.getAuxiliarySlot(index.index);
                if (auxiliarySlotValue == null) {
                    Object identifier = getIdentifierNew(frame, index);
                    initializeAuxFrameSlotInfoNew(frame, index.index, identifier);
                } else {
                    assert auxiliarySlotValue instanceof AuxiliarySlotValue;
                    object = ((AuxiliarySlotValue) auxiliarySlotValue).value;
                }
                break;
            default:
                throw RInternalError.shouldNotReachHere();
        }
        assertFrameStructureNew(frame);
        return object;
    }

    // TODO: Do this also for other primitive types.
    public static boolean getBooleanNew(Frame frame, FrameIndex frameIndex) {
        checkSharedContextAfterFrameMigration();
        assertFrameIndexInBoundsNew(frame, frameIndex);
        boolean ret;
        if (frameIndex.indexType == IndexType.NormalIndex) {
            ret = frame.getBoolean(frameIndex.index);
        } else {
            Object object = getObjectNew(frame, frameIndex);
            assert object instanceof Boolean;
            ret = (boolean) object;
        }
        assertFrameStructureNew(frame);
        return ret;
    }

    public static void setBooleanNew(Frame frame, FrameIndex frameIndex, boolean newValue) {
        assertFrameIndexInBoundsNew(frame, frameIndex);
        checkSharedContextAfterFrameMigration();
        if (frameIndex.indexType == IndexType.NormalIndex) {
            // TODO: Some invalidation?
            frame.setBoolean(frameIndex.index, newValue);
        } else {
            // Every auxiliary slot is an object slot
            setObjectNew(frame, frameIndex, newValue);
        }
        assertFrameStructureNew(frame);
    }

    public static void setObjectNew(Frame frame, Object identifier, Object newValue) {
        checkSharedContextAfterFrameMigration();
        FrameDescriptorMetaData descriptorMetaData = getMetaDataNew(frame);
        FrameIndex index = descriptorMetaData.getIndex(identifier);
        if (index == null) {
            throw RInternalError.shouldNotReachHere("A frame slot should first be added with findOrAddAuxiliaryFrameSlot");
        }
        setObjectNew(frame, index, identifier, newValue);
    }

    public static void setObjectNew(Frame frame, FrameIndex frameIndex, Object newValue) {
        checkSharedContextAfterFrameMigration();
        Object identifier = getIdentifierNew(frame, frameIndex);
        setObjectNew(frame, frameIndex, identifier, newValue);
    }

    private static void assertFrameIndexInBoundsNew(Frame frame, FrameIndex frameIndex) {
        FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        switch (frameIndex.indexType) {
            case NormalIndex:
                assert 0 <= frameIndex.index && frameIndex.index < frameDescriptor.getNumberOfSlots();
                break;
            case AuxiliaryIndex:
                assert 0 <= frameIndex.index && frameIndex.index < frameDescriptor.getNumberOfAuxiliarySlots();
                break;
        }
    }

    @TruffleBoundary
    private static void assertFrameStructureNew(Frame frame) {
        if (!NEW_FRAME_STRUCTURE_ASSERTS) {
            return;
        }
        FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        FrameDescriptorMetaData metadata = getMetaDataNew(frameDescriptor);
        int normalSlotsCount = frameDescriptor.getNumberOfSlots();
        int auxSlotsCount = frameDescriptor.getNumberOfAuxiliarySlots();
        int totalSlotsCount = normalSlotsCount + auxSlotsCount;
        assert metadata.indexes.size() == totalSlotsCount;
        for (Map.Entry<Object, FrameIndex> entry : metadata.indexes.entrySet()) {
            Object identifier = entry.getKey();
            FrameIndex frameIndex = entry.getValue();
            assertFrameIndexInBoundsNew(frame, frameIndex);
            switch (frameIndex.indexType) {
                case NormalIndex:
                    Object slotInfo = frameDescriptor.getSlotInfo(frameIndex.index);
                    assert slotInfo instanceof FrameSlotInfoImpl;
                    Object slotName = frameDescriptor.getSlotName(frameIndex.index);
                    assert slotName instanceof String || slotName instanceof RFrameSlot;
                    assert slotName == identifier;
                    // TODO: Also check slot value.
                    break;
                case AuxiliaryIndex:
                    Object auxSlot = frame.getAuxiliarySlot(frameIndex.index);
                    assert auxSlot != null;
                    assert auxSlot instanceof AuxiliarySlotValue;
                    AuxiliarySlotValue auxSlotValue = (AuxiliarySlotValue) auxSlot;
                    assert auxSlotValue.identifier == identifier;
                    break;
            }
        }
    }

    private static void setObjectNew(Frame frame, FrameIndex index, Object identifier, Object newValue) {
        assert index != null;
        checkSharedContextAfterFrameMigration();
        FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        switch (index.indexType) {
            case NormalIndex:
                assert 0 <= index.index && index.index < frameDescriptor.getNumberOfSlots();
                assert frameDescriptor.getSlotName(index.index) == identifier;
                frame.setObject(index.index, newValue);
                break;
            case AuxiliaryIndex:
                setAuxiliaryValueNew(frame, index.index, identifier, newValue);
                break;
            default:
                throw RInternalError.shouldNotReachHere();
        }
        assertFrameStructureNew(frame);
    }

    private static Object getIdentifierNew(Frame frame, FrameIndex frameIndex) {
        assert frameIndex != null;
        assertFrameIndexInBoundsNew(frame, frameIndex);
        FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        switch (frameIndex.indexType) {
            case NormalIndex:
                return frameDescriptor.getSlotName(frameIndex.index);
            case AuxiliaryIndex:
                AuxiliarySlotValue auxiliarySlotValue = (AuxiliarySlotValue) frame.getAuxiliarySlot(frameIndex.index);
                if (auxiliarySlotValue == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    Object identifier = getMetaDataNew(frameDescriptor).getIdentifier(frameIndex);
                    assert identifier != null;
                    return identifier;
                } else {
                    return auxiliarySlotValue.identifier;
                }
            default:
                throw RInternalError.shouldNotReachHere();
        }
    }

    public static boolean containsIdentifierNew(FrameDescriptor frameDescriptor, Object identifier) {
        FrameDescriptorMetaData metadata = getMetaDataNew(frameDescriptor);
        FrameIndex frameIndex = metadata.getIndex(identifier);
        return frameIndex != null;
    }

    private static void checkSharedContextAfterFrameMigration() {
        if (FastROptions.sharedContextsOptionValue && !RContext.isSingle()) {
            throw RInternalError.unimplemented();
        }
    }

    public static void setActiveBinding(Frame frame, FrameSlot frameSlot, ActiveBinding newValue, boolean isNonLocal, BranchProfile invalidateProfile) {
        setAndInvalidate(frame, frameSlot, newValue, isNonLocal, invalidateProfile);
        getContainsNoActiveBindingAssumption(frame.getFrameDescriptor()).invalidate();
    }

    /**
     * Initializes the internal data structures for a newly created frame descriptor that is
     * intended to be used for a non-function frame (and thus will only ever be used for one frame).
     * TODO (Frame API migration) : Remove - every FrameDescriptor allocation should be associated with
     *   FrameDescriptorMetadata in constructor
     */
    @Deprecated
    public static synchronized void initializeNonFunctionFrameDescriptor(String name, MaterializedFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptor key = handleBaseNamespaceEnv(frame);
        FrameDescriptorMetaData descriptorMetaData = new FrameDescriptorMetaData(name, frame);
        logger.fine(() -> String.format("initializing Non-function frame descriptor: name='%s', frameDescriptor={%s}, descriptorMetadata={%s}",
                        name, frameDescriptorToString(key), logFrameDescriptorMetadata(descriptorMetaData)));
        frameDescriptors.put(key, descriptorMetaData);
    }

    public static void initializeNonFunctionFrameDescriptorNew(FrameDescriptor frameDescriptor, MaterializedFrame singletonFrame) {
        assert singletonFrame != null;
        FrameDescriptorMetaData metaData = getMetaDataNew(frameDescriptor);
        metaData.setSingletonFrame(singletonFrame);
    }

    public static FrameDescriptor createUninitializedFrameDescriptorNew(String name) {
        FrameDescriptor descriptor = FrameDescriptor.newBuilder().info(new FrameDescriptorMetaData(name)).build();
        logger.info(() -> String.format("createUnitializedFrameDescriptorNew: name = '%s', descriptor = %s",
                            name, descriptor));
        return descriptor;
    }

    /**
     *
     * @param name For debug purposes.
     * @param singletonFrame Null for function descriptors, not null for environment descriptors.
     * @return
     */
    public static FrameDescriptor createFrameDescriptorNew(String name, MaterializedFrame singletonFrame) {
        FrameDescriptor descriptor = FrameDescriptor.newBuilder().info(new FrameDescriptorMetaData(name, singletonFrame)).build();
        logger.info(() -> String.format("createFrameDescriptorNew: name = '%s', descriptor = %s, (singletonFrame != null) == %b",
                            name, descriptor, singletonFrame != null));
        return descriptor;
    }

    public static FrameDescriptor createFunctionFrameDescriptorNew(String name) {
        return createFrameDescriptorNew(name, null);
    }

    public static FrameDescriptor createFrameDescriptorNew(String name, MaterializedFrame singletonFrame, FrameSlotKind[] kinds, Object[] identifiers) {
        assert kinds.length == identifiers.length;
        Builder builder = FrameDescriptor.newBuilder();
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * TODO (Frame API migration) : Remove - every FrameDescriptor allocation should be associated with
     *   FrameDescriptorMetadata in constructor
     */
    @Deprecated
    public static synchronized FrameDescriptor initializeFunctionFrameDescriptor(String name, FrameDescriptor frameDescriptor) {
        CompilerAsserts.neverPartOfCompilation();
        FrameDescriptor key = frameDescriptor;
        FrameDescriptorMetaData descriptorMetaData = new FrameDescriptorMetaData(name, null);
        logger.fine(() -> String.format("initializing function frame descriptor: name='%s', frameDescriptor={%s}, descriptorMetadata={%s}",
                        name, frameDescriptorToString(key), logFrameDescriptorMetadata(descriptorMetaData)));
        frameDescriptors.put(key, descriptorMetaData);
        return frameDescriptor;
    }

    public static String findAndLogFrameDescriptorByName(String frameName) {
        Entry<FrameDescriptor, FrameDescriptorMetaData> entry = FrameSlotChangeMonitor.findFrameDescriptorByName(frameName);
        if (entry == null) {
            return null;
        }
        return frameDescriptorToString(entry);
    }

    public static String findAndLogFrameDescriptorByIdx(int frameIdx) {
        if (frameIdx > frameDescriptors.entrySet().size()) {
            return null;
        }
        Object element = frameDescriptors.entrySet().toArray()[frameIdx];
        var entry = (Map.Entry<FrameDescriptor, FrameDescriptorMetaData>) element;
        return frameDescriptorToString(entry);
    }

    private static String frameDescriptorToString(Entry<FrameDescriptor, FrameDescriptorMetaData> entry) {
        StringBuilder sb = new StringBuilder();
        FrameDescriptor frameDescriptor = entry.getKey();
        sb.append("FrameDescriptor{");
        sb.append("size = ").append(frameDescriptor.getSize()).append(", ");
        FrameDescriptorMetaData metaData = getMetaData(frameDescriptor);
        sb.append("metaData = {");
        sb.append("name = '").append(metaData.name).append("' ");
        sb.append("lookupResults = [");
        metaData.lookupResults.forEach((key, lookupResult) -> {
            sb.append("{");
            var referent = lookupResult.get();
            if (referent != null) {
                sb.append("isValid = ").append(referent.isValid()).append(", ");
                try {
                    sb.append("value = ").append(referent.getValue().toString());
                } catch (InvalidAssumptionException e) {
                    sb.append("InvalidAssumptionException");
                }
            } else {
                sb.append("null");
            }
            sb.append("}");
        });
        if (metaData.lookupResults.size() > 0) {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append("]"); // End of lookupResults
        sb.append("}, "); // End of metadata
        sb.append("slots = [");
        for (FrameSlot slot : frameDescriptor.getSlots()) {
            sb.append(slotToString(slot)).append(", ");
        }
        if (frameDescriptor.getSlots().size() > 0) {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append("]"); // End of slots
        sb.append("}"); // End of FrameDescriptor
        return sb.toString();
    }

    private static String slotToString(FrameSlot frameSlot) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("identifier = '").append(frameSlot.getIdentifier().toString()).append("', ");
        sb.append("kind = ").append(frameSlot.getKind().name()).append(", ");
        FrameSlotInfoImpl info = getFrameSlotInfo(frameSlot);
        sb.append("info = {");
        sb.append("invalidationCount = ").append(info.invalidationCount).append(", ");
        sb.append("stableValue = ").append(info.stableValue != null ? info.stableValue.toString() : "null").append(", ");
        sb.append("nonLocalModifiedAssumption = ").append(info.nonLocalModifiedAssumption.toString());
        sb.append("}");
        sb.append("}");
        return sb.toString();
    }

    @TruffleBoundary
    private static String frameDescriptorToStringNew(FrameDescriptor frameDescriptor) {
        StringBuilder sb = new StringBuilder();
        sb.append("numberOfSlots = ").append(frameDescriptor.getNumberOfSlots()).append(", ");
        sb.append("numberOfAuxiliarySlots = ").append(frameDescriptor.getNumberOfAuxiliarySlots()).append(", ");
        FrameDescriptorMetaData metadata = getMetaDataNew(frameDescriptor);
        sb.append("metadata = {").append(metadata).append("}");
        return sb.toString();
    }

    @TruffleBoundary
    private static String frameDescriptorToString(FrameDescriptor frameDescriptor) {
        StringBuilder sb = new StringBuilder();
        sb.append("numberOfSlots = ").append(frameDescriptor.getNumberOfSlots()).append(", ");
        return sb.toString();
    }

    @TruffleBoundary
    private static String logFrameDescriptorMetadata(FrameDescriptorMetaData frameDescriptorMetaData) {
        return frameDescriptorMetaData.toString();
    }

    @TruffleBoundary
    public static Map.Entry<FrameDescriptor, FrameDescriptorMetaData> findFrameDescriptorByName(String name) {
        for (var entry : frameDescriptors.entrySet()) {
            if (entry.getValue().name.equals(name)) {
                return entry;
            }
        }
        return null;
    }

    @TruffleBoundary
    public static List<String> getFrameDescriptorNames() {
        List<String> names = new ArrayList<>();
        for (var entry : frameDescriptors.entrySet()) {
            names.add(entry.getValue().name);
        }
        return names;
    }

    @Deprecated
    public static synchronized Assumption getEnclosingFrameDescriptorAssumption(FrameDescriptor descriptor) {
        CompilerAsserts.neverPartOfCompilation();
        return frameDescriptors.get(descriptor).getEnclosingFrameDescriptorAssumption();
    }

    public static Assumption getEnclosingFrameDescriptorAssumptionNew(FrameDescriptor frameDescriptor) {
        return getMetaDataNew(frameDescriptor).getEnclosingFrameDescriptorAssumption();
    }

    @Deprecated
    public static synchronized Assumption getContainsNoActiveBindingAssumption(FrameDescriptor descriptor) {
        CompilerAsserts.neverPartOfCompilation();
        return frameDescriptors.get(descriptor).getContainsNoActiveBindingAssumption();
    }

    public static Assumption getContainsNoActiveBindingAssumptionNew(FrameDescriptor descriptor) {
        return getMetaDataNew(descriptor).getContainsNoActiveBindingAssumption();
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

    public static boolean isValidFrameDescriptorNew(FrameDescriptor frameDescriptor) {
        return getMetaDataNew(frameDescriptor) != null;
    }

    /*
     * This method should be called for frames of all environments on the search path.
     */
    public static synchronized void handleAllMultiSlots(Frame frame, int[] indices, boolean replicate) {
        // make a copy avoid potential updates to the array iterated over
        FrameSlot[] slots = new FrameSlot[frame.getFrameDescriptor().getSlots().size()];
        slots = frame.getFrameDescriptor().getSlots().toArray(slots);
        for (int i = 0; i < slots.length; i++) {
            FrameSlotInfoImpl.handleSearchPathMultiSlot(frame, slots[i], indices, replicate);
        }
    }

    @Deprecated
    public static Object getObject(FrameSlot slot, Frame frame) throws FrameSlotTypeException {
        if (FastROptions.sharedContextsOptionValue && !RContext.isSingle()) {
            FrameSlotInfoImpl info = getFrameSlotInfo(slot);
            if (info.noMultiSlot.isValid()) {
                return frame.getObject(slot);
            }
            Object o;
            try {
                o = frame.getObject(slot);
            } catch (FrameSlotTypeException e) {
                CompilerDirectives.transferToInterpreter();
                o = null;
            }
            if (!(o instanceof MultiSlotData)) {
                CompilerDirectives.transferToInterpreter();
                synchronized (info) {
                    assert slotExists(slot, frame) : slot;
                    o = frame.getObject(slot);
                    assert o != null : slot;
                }
            }
            return ((MultiSlotData) o).get(RContext.getInstance().getMultiSlotInd());
        } else {
            return frame.getObject(slot);
        }
    }

    public static Object getValue(FrameSlot slot, Frame frame) {
        if (FastROptions.sharedContextsOptionValue && !RContext.isSingle()) {
            FrameSlotInfoImpl info = getFrameSlotInfo(slot);
            if (info.noMultiSlot.isValid()) {
                return frame.getValue(slot);
            }
            Object o = frame.getValue(slot);
            if (!(o instanceof MultiSlotData)) {
                CompilerDirectives.transferToInterpreter();
                synchronized (info) {
                    assert slotExists(slot, frame) : slot;
                    o = frame.getValue(slot);
                    assert o != null : slot;
                }
            }
            return ((MultiSlotData) o).get(RContext.getInstance().getMultiSlotInd());
        } else {
            return frame.getValue(slot);
        }
    }

    @TruffleBoundary
    private static boolean slotExists(FrameSlot slot, Frame frame) {
        return frame.getFrameDescriptor().findFrameSlot(slot.getIdentifier()) != null;
    }

    private static boolean isMultislot(FrameSlotInfoImpl info) {
        return info.possibleMultiSlot() || !info.noMultiSlot.isValid();
    }

    /**
     * Nullifies a set of slots in a {@link MultiSlotData} to avoid memory leaks. When providing
     * {@code null} as indices, all subslots except the first one are nullified.
     */
    public static synchronized void cleanMultiSlots(Frame frame, int[] indices) {
        CompilerAsserts.neverPartOfCompilation();
        // make a copy avoid potential updates to the array iterated over
        FrameSlot[] slots = frame.getFrameDescriptor().getSlots().toArray(new FrameSlot[0]);

        for (int i = 0; i < slots.length; i++) {
            Object value = frame.getValue(slots[i]);
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
