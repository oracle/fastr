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
package com.oracle.truffle.r.nodes.access.variables;

import java.util.ArrayList;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.SlowPathException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.StableValue;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RTypedValue;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor.FrameAndSlotLookupResult;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor.LookupResult;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RSourceSectionNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * This node is used to read a variable from the local or enclosing environments. It specializes to
 * a particular layout of frame descriptors and enclosing environments, and re-specializes in case
 * the layout changes.
 */
public final class ReadVariableNode extends RSourceSectionNode implements RSyntaxNode, RSyntaxLookup {

    private static final int MAX_INVALIDATION_COUNT = 3;

    private enum ReadKind {
        Normal,
        // return null (instead of throwing an error) if not found
        Silent,
        // copy semantics
        Copying,
        // start the lookup in the enclosing frame
        Super,
        // whether a promise should be forced to check its type or not during lookup
        ForcedTypeCheck;
    }

    public static ReadVariableNode create(String name) {
        return new ReadVariableNode(RSyntaxNode.SOURCE_UNAVAILABLE, name, RType.Any, ReadKind.Normal);
    }

    public static ReadVariableNode create(SourceSection src, String name, boolean shouldCopyValue) {
        ReadVariableNode rvn = new ReadVariableNode(src, name, RType.Any, shouldCopyValue ? ReadKind.Copying : ReadKind.Normal);
        return rvn;
    }

    public static ReadVariableNode createSilent(String name, RType mode) {
        return new ReadVariableNode(RSyntaxNode.SOURCE_UNAVAILABLE, name, mode, ReadKind.Silent);
    }

    public static ReadVariableNode createSuperLookup(SourceSection src, String name) {
        ReadVariableNode rvn = new ReadVariableNode(src, name, RType.Any, ReadKind.Super);
        return rvn;
    }

    public static ReadVariableNode createFunctionLookup(SourceSection src, String identifier) {
        ReadVariableNode result = new ReadVariableNode(src, identifier, RType.Function, ReadKind.Normal);
        return result;
    }

    public static ReadVariableNode createForcedFunctionLookup(SourceSection src, String name) {
        ReadVariableNode result = new ReadVariableNode(src, name, RType.Function, ReadKind.ForcedTypeCheck);
        return result;
    }

    @Child private PromiseHelperNode promiseHelper;
    @Child private CheckTypeNode checkTypeNode;
    @CompilationFinal private FrameLevel read;
    @CompilationFinal private boolean needsCopying;

    private final ConditionProfile isPromiseProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile copyProfile;
    private final BranchProfile unexpectedMissingProfile = BranchProfile.create();
    private final ValueProfile superEnclosingFrameProfile = ValueProfile.createClassProfile();

    private final Object identifier;
    private final String identifierAsString;
    private final RType mode;
    private final ReadKind kind;
    private int invalidationCount;

    @CompilationFinal private final boolean[] seenValueKinds = new boolean[FrameSlotKind.values().length];

    private ReadVariableNode(SourceSection sourceSection, Object identifier, RType mode, ReadKind kind) {
        super(sourceSection);
        this.identifier = identifier;
        this.identifierAsString = identifier.toString().intern();
        this.mode = mode;
        this.kind = kind;

        this.copyProfile = kind != ReadKind.Copying ? null : ConditionProfile.createBinaryProfile();
    }

    @Override
    public String getIdentifier() {
        return identifierAsString;
    }

    public RType getMode() {
        return mode;
    }

    @Override
    public void serializeImpl(RSerialize.State state) {
        state.setCarAsSymbol(identifierAsString);
    }

    @Override
    public boolean isSyntax() {
        return identifier instanceof String;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeInternal(frame, kind == ReadKind.Super ? superEnclosingFrameProfile.profile(RArguments.getEnclosingFrame(frame)) : frame);
    }

    public Object execute(VirtualFrame frame, Frame variableFrame) {
        assert frame != null;
        return executeInternal(frame, kind == ReadKind.Super ? superEnclosingFrameProfile.profile(RArguments.getEnclosingFrame(variableFrame)) : variableFrame);
    }

    private Object executeInternal(VirtualFrame frame, Frame variableFrame) {
        if (kind != ReadKind.Silent) {
            RContext.getInstance().setVisible(true);
        }

        Object result;
        if (read == null) {
            initializeRead(frame, variableFrame);
        }
        try {
            result = read.execute(frame, variableFrame);
        } catch (InvalidAssumptionException | LayoutChangedException | FrameSlotTypeException e) {
            int iterations = 0;
            while (true) {
                iterations++;
                initializeRead(frame, variableFrame);
                try {
                    result = read.execute(frame, variableFrame);
                } catch (InvalidAssumptionException | LayoutChangedException | FrameSlotTypeException e2) {
                    if (iterations > 10) {
                        throw new RInternalError("too many iterations during RVN initialization: " + identifier);
                    }
                    continue;
                }
                break;
            }
        }
        if (needsCopying && copyProfile.profile(result instanceof RAbstractVector)) {
            result = ((RAbstractVector) result).copy();
        }
        if (isPromiseProfile.profile(result instanceof RPromise)) {
            if (promiseHelper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseHelper = insert(new PromiseHelperNode());
            }
            result = promiseHelper.evaluate(frame, (RPromise) result);
        }
        return result;
    }

    private void initializeRead(VirtualFrame frame, Frame variableFrame) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        read = initialize(frame, variableFrame);
        needsCopying = kind == ReadKind.Copying && !(read instanceof Match || read instanceof DescriptorStableMatch);
    }

    private static final class LayoutChangedException extends SlowPathException {
        private static final long serialVersionUID = 3380913774357492013L;
    }

    private abstract static class FrameLevel {

        public abstract Object execute(VirtualFrame frame, Frame variableFrame) throws InvalidAssumptionException, LayoutChangedException, FrameSlotTypeException;
    }

    private abstract static class DescriptorLevel extends FrameLevel {

        @Override
        public Object execute(VirtualFrame frame, Frame variableFrame) throws InvalidAssumptionException, LayoutChangedException, FrameSlotTypeException {
            return execute(frame);
        }

        public abstract Object execute(VirtualFrame frame) throws InvalidAssumptionException, LayoutChangedException, FrameSlotTypeException;

    }

    private final class Mismatch extends FrameLevel {

        private final FrameLevel next;
        private final FrameSlot slot;
        private final ConditionProfile isNullProfile = ConditionProfile.createBinaryProfile();
        private final ValueProfile frameProfile = ValueProfile.createClassProfile();

        private Mismatch(FrameLevel next, FrameSlot slot) {
            this.next = next;
            this.slot = slot;
        }

        @Override
        public Object execute(VirtualFrame frame, Frame variableFrame) throws InvalidAssumptionException, LayoutChangedException, FrameSlotTypeException {
            Frame profiledVariableFrame = frameProfile.profile(variableFrame);
            Object value = profiledGetValue(seenValueKinds, profiledVariableFrame, slot);
            if (checkType(frame, value, isNullProfile)) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new LayoutChangedException();
            }
            return next.execute(frame, profiledVariableFrame);
        }

        @Override
        public String toString() {
            return "M" + next;
        }
    }

    private static final class DescriptorStableMatch extends DescriptorLevel {

        private final Assumption assumption;
        private final Object value;

        private DescriptorStableMatch(Assumption assumption, Object value) {
            this.assumption = assumption;
            this.value = value;
        }

        @Override
        public Object execute(VirtualFrame frame) throws InvalidAssumptionException {
            assumption.check();
            return value;
        }

        @Override
        public String toString() {
            return "f";
        }
    }

    private final class Match extends FrameLevel {

        private final FrameSlot slot;
        private final ConditionProfile isNullProfile = ConditionProfile.createBinaryProfile();
        private final ValueProfile frameProfile = ValueProfile.createClassProfile();

        private Match(FrameSlot slot) {
            this.slot = slot;
        }

        @Override
        public Object execute(VirtualFrame frame, Frame variableFrame) throws LayoutChangedException, FrameSlotTypeException {
            Object value = profiledGetValue(seenValueKinds, frameProfile.profile(variableFrame), slot);
            if (!checkType(frame, value, isNullProfile)) {
                throw new LayoutChangedException();
            }
            return value;
        }

        @Override
        public String toString() {
            return "F";
        }
    }

    private final class Unknown extends DescriptorLevel {

        @Override
        public Object execute(VirtualFrame frame) {
            if (kind == ReadKind.Silent) {
                return null;
            } else {
                throw RError.error(RError.SHOW_CALLER, mode == RType.Function ? RError.Message.UNKNOWN_FUNCTION : RError.Message.UNKNOWN_OBJECT, identifier);
            }
        }

        @Override
        public String toString() {
            return "U";
        }
    }

    private static final class SingletonFrameLevel extends DescriptorLevel {

        private final FrameLevel next;
        private final MaterializedFrame singletonFrame;

        private SingletonFrameLevel(FrameLevel next, MaterializedFrame singletonFrame) {
            this.next = next;
            this.singletonFrame = singletonFrame;
        }

        @Override
        public Object execute(VirtualFrame frame) throws InvalidAssumptionException, LayoutChangedException, FrameSlotTypeException {
            return next.execute(frame, singletonFrame);
        }

        @Override
        public String toString() {
            return "~>" + next;
        }
    }

    private static final class NextFrameLevel extends FrameLevel {

        private final FrameLevel next;
        private final FrameDescriptor nextDescriptor;
        private final ValueProfile frameProfile = ValueProfile.createClassProfile();

        private NextFrameLevel(FrameLevel next, FrameDescriptor nextDescriptor) {
            this.next = next;
            this.nextDescriptor = nextDescriptor;
        }

        @Override
        public Object execute(VirtualFrame frame, Frame variableFrame) throws InvalidAssumptionException, LayoutChangedException, FrameSlotTypeException {
            MaterializedFrame nextFrame = frameProfile.profile(RArguments.getEnclosingFrame(variableFrame));
            if (nextDescriptor == null) {
                if (nextFrame != null) {
                    throw new LayoutChangedException();
                }
                return next.execute(frame, null);
            } else {
                if (nextFrame == null) {
                    throw new LayoutChangedException();
                }
                if (nextFrame.getFrameDescriptor() != nextDescriptor) {
                    throw new LayoutChangedException();
                }
                return next.execute(frame, nextFrame);
            }
        }

        @Override
        public String toString() {
            return "=>" + next;
        }
    }

    private static final class MultiAssumptionLevel extends FrameLevel {

        private final FrameLevel next;
        @CompilationFinal private final Assumption[] assumptions;

        private MultiAssumptionLevel(FrameLevel next, Assumption... assumptions) {
            this.next = next;
            this.assumptions = assumptions;
        }

        @Override
        @ExplodeLoop
        public Object execute(VirtualFrame frame, Frame variableFrame) throws InvalidAssumptionException, LayoutChangedException, FrameSlotTypeException {
            for (Assumption assumption : assumptions) {
                assumption.check();
            }
            return next.execute(frame, variableFrame);
        }

        @Override
        public String toString() {
            return "-" + assumptions.length + ">" + next;
        }
    }

    private final class Polymorphic extends FrameLevel {

        private final ConditionProfile isNullProfile = ConditionProfile.createBinaryProfile();
        @CompilationFinal private FrameSlot frameSlot;
        @CompilationFinal private Assumption notInFrame;

        private Polymorphic(Frame variableFrame) {
            frameSlot = variableFrame.getFrameDescriptor().findFrameSlot(identifier);
            notInFrame = frameSlot == null ? variableFrame.getFrameDescriptor().getNotInFrameAssumption(identifier) : null;
        }

        @Override
        public Object execute(VirtualFrame frame, Frame variableFrame) throws LayoutChangedException, FrameSlotTypeException {
            // check if the slot is missing / wrong type in current frame
            if (frameSlot == null) {
                try {
                    notInFrame.check();
                } catch (InvalidAssumptionException e) {
                    frameSlot = variableFrame.getFrameDescriptor().findFrameSlot(identifier);
                    notInFrame = frameSlot == null ? variableFrame.getFrameDescriptor().getNotInFrameAssumption(identifier) : null;
                }
            }
            if (frameSlot != null) {
                Object value = variableFrame.getValue(frameSlot);
                if (checkType(frame, value, isNullProfile)) {
                    return value;
                }
            }
            // search enclosing frames if necessary
            MaterializedFrame current = RArguments.getEnclosingFrame(variableFrame);
            while (current != null) {
                Object value = getValue(current);
                if (checkType(frame, value, isNullProfile)) {
                    return value;
                }
                current = RArguments.getEnclosingFrame(current);
            }
            if (kind == ReadKind.Silent) {
                return null;
            } else {
                throw RError.error(RError.SHOW_CALLER, mode == RType.Function ? RError.Message.UNKNOWN_FUNCTION : RError.Message.UNKNOWN_OBJECT, identifier);
            }
        }

        @TruffleBoundary
        private Object getValue(MaterializedFrame current) {
            FrameSlot slot = current.getFrameDescriptor().findFrameSlot(identifier);
            return slot == null ? null : current.getValue(slot);
        }

        @Override
        public String toString() {
            return "P";
        }
    }

    private final class LookupLevel extends DescriptorLevel {

        private final LookupResult lookup;
        private final ValueProfile frameProfile = ValueProfile.createClassProfile();
        private final ConditionProfile nullValueProfile = kind == ReadKind.Silent ? null : ConditionProfile.createBinaryProfile();

        private LookupLevel(LookupResult lookup) {
            this.lookup = lookup;
        }

        @Override
        public Object execute(VirtualFrame frame) throws InvalidAssumptionException, LayoutChangedException, FrameSlotTypeException {
            Object value;
            if (lookup instanceof FrameAndSlotLookupResult) {
                FrameAndSlotLookupResult frameAndSlotLookupResult = (FrameAndSlotLookupResult) lookup;
                value = profiledGetValue(seenValueKinds, frameProfile.profile(frameAndSlotLookupResult.getFrame()), frameAndSlotLookupResult.getSlot());
            } else {
                value = lookup.getValue();
            }
            if (kind != ReadKind.Silent && nullValueProfile.profile(value == null)) {
                throw RError.error(RError.SHOW_CALLER, mode == RType.Function ? RError.Message.UNKNOWN_FUNCTION : RError.Message.UNKNOWN_OBJECT, identifier);
            }
            return value;
        }
    }

    private FrameLevel initialize(VirtualFrame frame, Frame variableFrame) {
        if (identifier.toString().isEmpty()) {
            throw RError.error(RError.NO_CALLER, RError.Message.ZERO_LENGTH_VARIABLE);
        }

        /*
         * Check whether we need to go to the polymorphic case, which will not rely on any frame
         * descriptor assumptions (apart from the first frame).
         */
        if (++invalidationCount > MAX_INVALIDATION_COUNT) {
            RError.performanceWarning("polymorphic (slow path) lookup of symbol \"" + identifier + "\"");
            return new Polymorphic(variableFrame);
        }

        /*
         * Check whether we can fulfill the lookup by only looking at the current frame, and thus
         * without additional dependencies on frame descriptor layouts.
         */
        FrameSlot localSlot = variableFrame.getFrameDescriptor().findFrameSlot(identifier);
        // non-local reads can only be handled in a simple way if they are successful
        if (localSlot != null && checkTypeSlowPath(frame, getValue(seenValueKinds, variableFrame, localSlot))) {
            return new Match(localSlot);
        }

        /*
         * Check whether the frame descriptor information available in FrameSlotChangeMonitor is
         * enough to handle this lookup. This has the advantage of not depending on a specific
         * "enclosing frame descriptor" chain, so that attaching/detaching environments does not
         * necessarily invalidate lookups.
         */
        LookupResult lookup = FrameSlotChangeMonitor.lookup(variableFrame, identifier);
        if (lookup != null) {
            try {
                if (lookup.getValue() instanceof RPromise) {
                    evalPromiseSlowPathWithName(frame, (RPromise) lookup.getValue());
                }
                if (lookup != null) {
                    if (lookup.getValue() == null || checkTypeSlowPath(frame, lookup.getValue())) {
                        return new LookupLevel(lookup);
                    }
                }
            } catch (InvalidAssumptionException e) {
                // immediately invalidated...
            }
        }

        /*
         * If everything else fails: build the lookup from scratch, by recursively building
         * assumptions and checks.
         */
        ArrayList<Assumption> assumptions = new ArrayList<>();
        FrameLevel lastLevel = createLevels(frame, variableFrame, assumptions);
        if (!assumptions.isEmpty()) {
            lastLevel = new MultiAssumptionLevel(lastLevel, assumptions.toArray(new Assumption[assumptions.size()]));
        }

        if (FastROptions.PrintComplexLookups.getBooleanValue()) {
            System.out.println(identifier + " " + lastLevel);
        }
        return lastLevel;
    }

    /**
     * This function returns a "recipe" to find the value of this lookup, starting at varibleFrame.
     * It may record assumptions into the given ArrayList of assumptions. The result will be a
     * linked list of {@link FrameLevel} instances.
     */
    private FrameLevel createLevels(VirtualFrame frame, Frame variableFrame, ArrayList<Assumption> assumptions) {
        if (variableFrame == null) {
            // this means that we've arrived at the empty env during lookup
            return new Unknown();
        }
        // see if the current frame has a value of the given name
        FrameDescriptor currentDescriptor = variableFrame.getFrameDescriptor();
        FrameSlot frameSlot = currentDescriptor.findFrameSlot(identifier);
        if (frameSlot != null) {
            Object value = getValue(seenValueKinds, variableFrame, frameSlot);
            if (checkTypeSlowPath(frame, value)) {
                StableValue<Object> valueAssumption = FrameSlotChangeMonitor.getStableValueAssumption(currentDescriptor, frameSlot, value);
                if (valueAssumption != null) {
                    Assumption assumption = valueAssumption.getAssumption();
                    assert value == valueAssumption.getValue() || value.equals(valueAssumption.getValue()) : value + " vs. " + valueAssumption.getValue();
                    if (value instanceof RPromise) {
                        RPromise promise = (RPromise) value;
                        Object promiseValue = PromiseHelperNode.evaluateSlowPath(frame, promise);
                        if (promiseValue instanceof RFunction) {
                            value = promiseValue;
                        }
                    }
                    return new DescriptorStableMatch(assumption, value);
                } else {
                    return new Match(frameSlot);
                }
            }
        }

        // the identifier wasn't found in the current frame: try the next one
        MaterializedFrame next = RArguments.getEnclosingFrame(variableFrame);
        FrameLevel lastLevel = createLevels(frame, next, assumptions);

        /*
         * Here we look at the type of the recursive lookup result, to see if we need only a
         * specific FrameDescriptor (DescriptorLevel) or the actual frame (FrameLevel).
         */

        if (!(lastLevel instanceof DescriptorLevel)) {
            MaterializedFrame singleton = FrameSlotChangeMonitor.getSingletonFrame(next.getFrameDescriptor());
            if (singleton != null) {
                // use singleton frames to get from a frame descriptor to an actual frame
                lastLevel = new SingletonFrameLevel(lastLevel, singleton);
            }
        }

        Assumption enclosingDescriptorAssumption = FrameSlotChangeMonitor.getEnclosingFrameDescriptorAssumption(currentDescriptor);
        if (lastLevel instanceof DescriptorLevel && enclosingDescriptorAssumption != null) {
            assumptions.add(enclosingDescriptorAssumption);
        } else {
            lastLevel = new NextFrameLevel(lastLevel, next == null ? null : next.getFrameDescriptor());
        }

        if (frameSlot == null) {
            assumptions.add(currentDescriptor.getNotInFrameAssumption(identifier));
        } else {
            StableValue<Object> valueAssumption = FrameSlotChangeMonitor.getStableValueAssumption(currentDescriptor, frameSlot, getValue(seenValueKinds, variableFrame, frameSlot));
            if (valueAssumption != null && lastLevel instanceof DescriptorLevel) {
                assumptions.add(valueAssumption.getAssumption());
            } else {
                lastLevel = new Mismatch(lastLevel, frameSlot);
            }
        }
        return lastLevel;
    }

    @TruffleBoundary
    public static RFunction lookupFunction(String identifier, Frame variableFrame, boolean localOnly) {
        Frame current = variableFrame;
        do {
            // see if the current frame has a value of the given name
            FrameSlot frameSlot = current.getFrameDescriptor().findFrameSlot(identifier);
            if (frameSlot != null) {
                Object value = current.getValue(frameSlot);

                if (value != null) {
                    if (value == RMissing.instance) {
                        throw RError.error(RError.SHOW_CALLER, RError.Message.ARGUMENT_MISSING, identifier);
                    }
                    if (value instanceof RPromise) {
                        RPromise promise = (RPromise) value;
                        if (promise.isEvaluated()) {
                            value = promise.getValue();
                        } else {
                            value = PromiseHelperNode.evaluateSlowPath(null, promise);
                            return (RFunction) value;
                        }
                    }
                    if (RRuntime.checkType(value, RType.Function)) {
                        return (RFunction) value;
                    }
                }
            }
            if (localOnly) {
                return null;
            }
            current = RArguments.getEnclosingFrame(current);
        } while (current != null);
        return null;
    }

    @TruffleBoundary
    public static Object lookupAny(String identifier, Frame variableFrame, boolean localOnly) {
        Frame current = variableFrame;
        do {
            // see if the current frame has a value of the given name
            FrameSlot frameSlot = current.getFrameDescriptor().findFrameSlot(identifier);
            if (frameSlot != null) {
                Object value = current.getValue(frameSlot);

                if (value != null) {
                    if (value == RMissing.instance) {
                        throw RError.error(RError.SHOW_CALLER, RError.Message.ARGUMENT_MISSING, identifier);
                    }
                    if (value instanceof RPromise) {
                        return PromiseHelperNode.evaluateSlowPath(null, (RPromise) value);
                    }
                    return value;
                }
            }
            if (localOnly) {
                return null;
            }
            current = RArguments.getEnclosingFrame(current);
        } while (current != null);
        return null;
    }

    @TruffleBoundary
    public static RArgsValuesAndNames lookupVarArgs(Frame variableFrame) {
        Frame current = variableFrame;
        do {
            // see if the current frame has a value of the given name
            FrameSlot frameSlot = current.getFrameDescriptor().findFrameSlot(ArgumentsSignature.VARARG_NAME);
            if (frameSlot != null) {
                Object value = current.getValue(frameSlot);

                if (value != null) {
                    if (value == RNull.instance) {
                        return RArgsValuesAndNames.EMPTY;
                    } else if (value instanceof RArgsValuesAndNames) {
                        return (RArgsValuesAndNames) value;
                    } else {
                        return null;
                    }
                }
            }
            current = RArguments.getEnclosingFrame(current);
        } while (current != null);
        return null;
    }

    private static Object getValue(boolean[] seenValueKinds, Frame variableFrame, FrameSlot frameSlot) {
        assert variableFrame.getFrameDescriptor() == frameSlot.getFrameDescriptor();
        Object value = variableFrame.getValue(frameSlot);
        if (variableFrame.isObject(frameSlot)) {
            seenValueKinds[FrameSlotKind.Object.ordinal()] = true;
        } else if (variableFrame.isByte(frameSlot)) {
            seenValueKinds[FrameSlotKind.Byte.ordinal()] = true;
        } else if (variableFrame.isInt(frameSlot)) {
            seenValueKinds[FrameSlotKind.Int.ordinal()] = true;
        } else if (variableFrame.isDouble(frameSlot)) {
            seenValueKinds[FrameSlotKind.Double.ordinal()] = true;
        }
        return value;
    }

    static Object profiledGetValue(boolean[] seenValueKinds, Frame variableFrame, FrameSlot frameSlot) {
        assert variableFrame.getFrameDescriptor() == frameSlot.getFrameDescriptor();
        try {
            if (seenValueKinds[FrameSlotKind.Object.ordinal()] && variableFrame.isObject(frameSlot)) {
                return variableFrame.getObject(frameSlot);
            } else if (seenValueKinds[FrameSlotKind.Byte.ordinal()] && variableFrame.isByte(frameSlot)) {
                return variableFrame.getByte(frameSlot);
            } else if (seenValueKinds[FrameSlotKind.Int.ordinal()] && variableFrame.isInt(frameSlot)) {
                return variableFrame.getInt(frameSlot);
            } else if (seenValueKinds[FrameSlotKind.Double.ordinal()] && variableFrame.isDouble(frameSlot)) {
                return variableFrame.getDouble(frameSlot);
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                // re-profile to widen the set of expected types
                return getValue(seenValueKinds, variableFrame, frameSlot);
            }
        } catch (FrameSlotTypeException e) {
            throw new RInternalError(e, "unexpected frame slot type mismatch");
        }
    }

    /**
     * This method checks the value a RVN just read. It is used to determine whether the value just
     * read matches the expected type or if we have to look in a frame up the lexical chain. It
     * might:
     * <ul>
     * <li>throw an {@link RError}: if 'objArg' is a missing argument and this is not allowed</li>
     * <li>return {@code true}: if the type of 'objArg' matches the description in 'type'</li>
     * <li>return {@code false}: if the type of 'objArg' does not match the description in 'type'
     * </li>
     * </ul>
     * However, there is the special case of 'objArg' being a {@link RPromise}: Normally, it is
     * expected to match type and simply returns {@code true}. But in case of 'forcePromise' ==
     * {@code true}, the promise is evaluated and the result checked for it's type. This is only
     * used for function lookup, as we need to be sure that we read a function.
     *
     * @param frame The frame to (eventually) evaluate the {@link RPromise} in
     * @param objArg The object to check for proper type
     * @return see above
     */
    private boolean checkType(VirtualFrame frame, Object objArg, ConditionProfile isNullProfile) {
        Object obj = objArg;
        if ((isNullProfile == null && obj == null) || (isNullProfile != null && isNullProfile.profile(obj == null))) {
            return false;
        }
        if (obj == RMissing.instance) {
            unexpectedMissingProfile.enter();
            throw RError.error(RError.SHOW_CALLER, RError.Message.ARGUMENT_MISSING, getIdentifier());
        }
        if (mode == RType.Any) {
            return true;
        }
        if (isPromiseProfile.profile(obj instanceof RPromise)) {
            RPromise promise = (RPromise) obj;
            if (promiseHelper == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseHelper = insert(new PromiseHelperNode());
            }
            if (!promiseHelper.isEvaluated(promise)) {
                if (kind != ReadKind.ForcedTypeCheck) {
                    // since we do not know what type the evaluates to, it may match.
                    // we recover from a wrong type later
                    return true;
                } else {
                    obj = promiseHelper.evaluate(frame, promise);
                }
            } else {
                obj = promise.getValue();
            }
        }
        if (checkTypeNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            checkTypeNode = insert(CheckTypeNodeGen.create(mode));
        }
        return checkTypeNode.executeBoolean(obj);
    }

    private static final ThreadLocal<String> slowPathEvaluationName = new ThreadLocal<>();

    private boolean checkTypeSlowPath(VirtualFrame frame, Object objArg) {
        CompilerAsserts.neverPartOfCompilation();
        Object obj = objArg;
        if (obj == null) {
            return false;
        }
        if (obj == RMissing.instance) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.ARGUMENT_MISSING, getIdentifier());
        }
        if (mode == RType.Any) {
            return true;
        }
        if (obj instanceof RPromise) {
            RPromise promise = (RPromise) obj;

            if (!promise.isEvaluated()) {
                if (kind != ReadKind.ForcedTypeCheck) {
                    // since we do not know what type the evaluates to, it may match.
                    // we recover from a wrong type later
                    return true;
                } else {
                    obj = evalPromiseSlowPathWithName(frame, promise);
                }
            } else {
                obj = promise.getValue();
            }
        }
        return RRuntime.checkType(obj, mode);
    }

    private Object evalPromiseSlowPathWithName(VirtualFrame frame, RPromise promise) {
        Object obj;
        slowPathEvaluationName.set(identifierAsString);
        try {
            obj = PromiseHelperNode.evaluateSlowPath(frame, promise);
        } finally {
            slowPathEvaluationName.set(null);
        }
        return obj;
    }

    public static String getSlowPathEvaluationName() {
        return slowPathEvaluationName.get();
    }

    public boolean isForceForTypeCheck() {
        return kind == ReadKind.ForcedTypeCheck;
    }

    @Override
    public boolean isFunctionLookup() {
        return mode == RType.Function;
    }
}

/*
 * This is RRuntime.checkType in the node form.
 */
abstract class CheckTypeNode extends RBaseNode {

    public abstract boolean executeBoolean(Object o);

    private final RType type;

    CheckTypeNode(RType type) {
        assert type != RType.Any;
        this.type = type;
    }

    @Specialization
    boolean checkType(@SuppressWarnings("unused") RAbstractIntVector o) {
        return type == RType.Integer || type == RType.Double;
    }

    @Specialization
    boolean checkType(@SuppressWarnings("unused") RAbstractDoubleVector o) {
        return type == RType.Integer || type == RType.Double;
    }

    @Specialization
    boolean checkType(@SuppressWarnings("unused") RAbstractRawVector o) {
        return type == RType.Logical;
    }

    @Specialization
    boolean checkType(@SuppressWarnings("unused") RAbstractStringVector o) {
        return type == RType.Character;
    }

    @Specialization
    boolean checkType(@SuppressWarnings("unused") RAbstractComplexVector o) {
        return type == RType.Complex;
    }

    @Specialization
    boolean checkType(@SuppressWarnings("unused") RFunction o) {
        return type == RType.Function || type == RType.Closure || type == RType.Builtin || type == RType.Special;
    }

    @Fallback
    boolean checkType(Object o) {
        if (type == RType.Function || type == RType.Closure || type == RType.Builtin || type == RType.Special) {
            return o instanceof TruffleObject && !(o instanceof RTypedValue);
        } else {
            return false;
        }
    }
}
