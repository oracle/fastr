/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;
import com.oracle.truffle.r.runtime.env.frame.*;

/**
 * This node is used to read a variable from the local or enclosing environments. It specializes to
 * a particular layout of frame descriptors and enclosing environments, and re-specializes in case
 * the layout changes.
 */
public class ReadVariableNode extends RNode implements RSyntaxNode, VisibilityController {

    public static enum ReadKind {
        Normal,
        // return null (instead of throwing an error) if not found
        Silent,
        // copy semantics
        Copying,
        // start the lookup in the enclosing frame
        Super,
        // lookup only within the current frame
        SilentLocal,
        // whether a promise should be forced to check its type or not
        Forced;
    }

    public static ReadVariableNode create(String name, RType mode, ReadKind kind) {
        return new ReadVariableNode(name, mode, kind);
    }

    public static ReadVariableNode create(String name, boolean shouldCopyValue) {
        return new ReadVariableNode(name, RType.Any, shouldCopyValue ? ReadKind.Copying : ReadKind.Normal);
    }

    public static ReadVariableNode create(SourceSection src, String name, boolean shouldCopyValue) {
        ReadVariableNode rvn = new ReadVariableNode(name, RType.Any, shouldCopyValue ? ReadKind.Copying : ReadKind.Normal);
        rvn.assignSourceSection(src);
        return rvn;
    }

    /**
     * Creates a function lookup for the given identifier. If throwError is true, then an error will
     * be thrown if the specified function is not found, if throwError is false, a {@code null}
     * value will silently be returned.
     */
    public static ReadVariableNode createFunctionLookup(SourceSection src, String identifier, boolean throwError) {
        ReadVariableNode result = new ReadVariableNode(identifier, RType.Function, throwError ? ReadKind.Normal : ReadKind.Silent);
        result.assignSourceSection(src);
        return result;
    }

    public static ReadVariableNode createSuperLookup(SourceSection src, String name) {
        ReadVariableNode rvn = new ReadVariableNode(name, RType.Any, ReadKind.Super);
        rvn.assignSourceSection(src);
        return rvn;
    }

    /**
     * Creates every {@link ReadVariableNode} out there.
     *
     * @param src A {@link SourceSection} for the variable
     * @param name The symbol the {@link ReadVariableNode} is meant to resolve
     * @param mode The mode of the variable
     *
     * @return The appropriate implementation of {@link ReadVariableNode}
     */
    public static ReadVariableNode createForced(SourceSection src, String name, RType mode) {
        ReadVariableNode result = new ReadVariableNode(name, mode, ReadKind.Forced);
        if (src != null) {
            result.assignSourceSection(src);
        }
        return result;
    }

    @Child protected PromiseHelperNode promiseHelper;
    @CompilationFinal private FrameLevel read;
    @CompilationFinal private boolean needsCopying;

    private final ConditionProfile isPromiseProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile copyProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile unexpectedMissingProfile = BranchProfile.create();
    private final ValueProfile superEnclosingFrameProfile = ValueProfile.createClassProfile();
    private final ConditionProfile isNullValueProfile = ConditionProfile.createBinaryProfile();
    private final ValueProfile valueProfile = ValueProfile.createClassProfile();

    private final String identifier;
    private final RType mode;
    private final ReadKind kind;

    @CompilationFinal private final boolean[] seenValueKinds = new boolean[FrameSlotKind.values().length];

    private ReadVariableNode(String identifier, RType mode, ReadKind kind) {
        this.identifier = identifier;
        this.mode = mode;
        this.kind = kind;
    }

    protected ReadVariableNode() {
        this.identifier = null;
        this.mode = null;
        this.kind = null;
    }

    public String getIdentifier() {
        return identifier;
    }

    public RType getMode() {
        return mode;
    }

    public ReadKind getKind() {
        return kind;
    }

    private boolean seenValueKind(FrameSlotKind slotKind) {
        return seenValueKinds[slotKind.ordinal()];
    }

    @Override
    public void deparse(RDeparse.State state) {
        state.append(identifier);
    }

    @Override
    public void serialize(RSerialize.State state) {
        state.setCarAsSymbol(identifier);
    }

    @Override
    public RSyntaxNode substitute(REnvironment env) {
        RSyntaxNode result = RSyntaxNode.cast(RASTUtils.substituteName(identifier, env));
        if (result == null) {
            result = NodeUtil.cloneNode(this);
        }
        return result;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeInternal(frame, kind == ReadKind.Super ? superEnclosingFrameProfile.profile(RArguments.getEnclosingFrame(frame)) : frame);
    }

    public Object execute(VirtualFrame frame, Frame variableFrame) {
        return executeInternal(frame, kind == ReadKind.Super ? superEnclosingFrameProfile.profile(RArguments.getEnclosingFrame(variableFrame)) : variableFrame);
    }

    private Object executeInternal(VirtualFrame frame, Frame variableFrame) {
        controlVisibility();

        Object result;
        if (read == null) {
            initializeRead(frame, variableFrame);
        }
        try {
            result = read.execute(frame, variableFrame);
        } catch (InvalidAssumptionException | LayoutChangedException | FrameSlotTypeException e) {
            initializeRead(frame, variableFrame);
            try {
                result = read.execute(frame, variableFrame);
            } catch (InvalidAssumptionException | LayoutChangedException | FrameSlotTypeException e2) {
                /*
                 * This could be written a bit simpler using a loop, but partial evaluation will not
                 * see that it never actually loops in compiled code, and thus create suboptimal
                 * code.
                 */
                throw RInternalError.shouldNotReachHere("read initialization problem");
            }
        }
        if (needsCopying && copyProfile.profile(result instanceof RAbstractVector)) {
            result = ((RAbstractVector) result).copy();
        }
        if (kind != ReadKind.SilentLocal && isPromiseProfile.profile(result instanceof RPromise)) {
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

        public Mismatch(FrameLevel next, FrameSlot slot) {
            this.next = next;
            this.slot = slot;
        }

        @Override
        public Object execute(VirtualFrame frame, Frame variableFrame) throws InvalidAssumptionException, LayoutChangedException, FrameSlotTypeException {
            Object value = profiledGetValue(variableFrame, slot);
            if (kind == ReadKind.SilentLocal && value == RMissing.instance) {
                return null;
            }
            if (checkType(frame, value, isNullProfile)) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new LayoutChangedException();
            }
            return next.execute(frame, variableFrame);
        }

        @Override
        public String toString() {
            return "M" + next;
        }
    }

    private static final class DescriptorStableMatch extends DescriptorLevel {

        private final StableValue<Object> valueAssumption;

        public DescriptorStableMatch(StableValue<Object> valueAssumption) {
            this.valueAssumption = valueAssumption;
        }

        @Override
        public Object execute(VirtualFrame frame) throws InvalidAssumptionException {
            valueAssumption.getAssumption().check();
            return valueAssumption.getValue();
        }

        @Override
        public String toString() {
            return "f";
        }
    }

    private final class Match extends FrameLevel {

        private final FrameSlot slot;
        private final ConditionProfile isNullProfile = ConditionProfile.createBinaryProfile();

        public Match(FrameSlot slot) {
            this.slot = slot;
        }

        @Override
        public Object execute(VirtualFrame frame, Frame variableFrame) throws LayoutChangedException, FrameSlotTypeException {
            Object value = profiledGetValue(variableFrame, slot);
            if (kind == ReadKind.SilentLocal && value == RMissing.instance) {
                return null;
            }
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
            if (kind == ReadKind.Silent || kind == ReadKind.SilentLocal) {
                return null;
            } else {
                throw RError.error(mode == RType.Function ? RError.Message.UNKNOWN_FUNCTION : RError.Message.UNKNOWN_OBJECT, identifier);
            }
        }

        @Override
        public String toString() {
            return "U";
        }
    }

    private static final class NextFrameFromDescriptorLevel extends DescriptorLevel {

        private final FrameLevel next;
        private final StableValue<MaterializedFrame> enclosingFrameAssumption;

        public NextFrameFromDescriptorLevel(FrameLevel next, StableValue<MaterializedFrame> enclosingFrameAssumption) {
            this.next = next;
            this.enclosingFrameAssumption = enclosingFrameAssumption;
        }

        @Override
        public Object execute(VirtualFrame frame) throws InvalidAssumptionException, LayoutChangedException, FrameSlotTypeException {
            enclosingFrameAssumption.getAssumption().check();
            return next.execute(frame, enclosingFrameAssumption.getValue());
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
        private final ConditionProfile isEvalFrame = ConditionProfile.createBinaryProfile();

        public NextFrameLevel(FrameLevel next, FrameDescriptor nextDescriptor) {
            this.next = next;
            this.nextDescriptor = nextDescriptor;
        }

        @Override
        public Object execute(VirtualFrame frame, Frame variableFrame) throws InvalidAssumptionException, LayoutChangedException, FrameSlotTypeException {
            MaterializedFrame nextFrame = frameProfile.profile(RArguments.getEnclosingFrame(variableFrame, isEvalFrame));
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

        public MultiAssumptionLevel(FrameLevel next, Assumption[] assumptions) {
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

    private FrameLevel initialize(VirtualFrame frame, Frame variableFrame) {
        if (identifier.isEmpty()) {
            throw RError.error(RError.Message.ZERO_LENGTH_VARIABLE);
        }

        class ReadVariableLevel {
            public final FrameDescriptor descriptor;
            public final FrameSlot slot;
            public final StableValue<Object> valueAssumption;

            public final StableValue<FrameDescriptor> enclosingDescriptorAssumption;
            public final StableValue<MaterializedFrame> enclosingFrameAssumption;

            public final MaterializedFrame nextFrame;

            public ReadVariableLevel(FrameDescriptor descriptor, FrameSlot slot, StableValue<Object> valueAssumption, StableValue<FrameDescriptor> enclosingDescriptorAssumption,
                            StableValue<MaterializedFrame> enclosingFrameAssumption, MaterializedFrame nextFrame) {
                this.descriptor = descriptor;
                this.slot = slot;
                this.valueAssumption = valueAssumption;
                this.enclosingDescriptorAssumption = enclosingDescriptorAssumption;
                this.enclosingFrameAssumption = enclosingFrameAssumption;
                this.nextFrame = nextFrame;
            }

            public FrameDescriptor nextDescriptor() {
                return nextFrame == null ? null : nextFrame.getFrameDescriptor();
            }
        }

        ArrayList<ReadVariableLevel> levels = new ArrayList<>();

        Frame current = variableFrame;
        FrameDescriptor currentDescriptor = current.getFrameDescriptor();
        boolean match = false;
        do {
            // see if the current frame has a value of the given name
            FrameSlot frameSlot = currentDescriptor.findFrameSlot(identifier);
            StableValue<Object> valueAssumption = null;
            if (frameSlot != null) {
                Object value = getValue(current, frameSlot);
                valueAssumption = FrameSlotChangeMonitor.getStableValueAssumption(currentDescriptor, frameSlot, value);
                if (kind == ReadKind.SilentLocal && value == RMissing.instance) {
                    match = false;
                } else {
                    match = checkType(frame, value, null);
                }
            }

            // figure out how to get to the next frame or descriptor
            MaterializedFrame next = RArguments.getEnclosingFrame(current);
            FrameDescriptor nextDescriptor = next == null ? null : next.getFrameDescriptor();
            StableValue<MaterializedFrame> enclosingFrameAssumption = FrameSlotChangeMonitor.getOrInitializeEnclosingFrameAssumption(current, currentDescriptor, null, next);
            StableValue<FrameDescriptor> enclosingDescriptorAssumption = FrameSlotChangeMonitor.getOrInitializeEnclosingFrameDescriptorAssumption(current, currentDescriptor, null, nextDescriptor);

            levels.add(new ReadVariableLevel(currentDescriptor, frameSlot, valueAssumption, enclosingDescriptorAssumption, enclosingFrameAssumption, next));

            current = next;
            currentDescriptor = nextDescriptor;
        } while (kind != ReadKind.SilentLocal && current != null && !match);

        FrameLevel lastLevel = null;

        boolean complex = false;
        ListIterator<ReadVariableLevel> iter = levels.listIterator(levels.size());
        if (match) {
            ReadVariableLevel level = levels.get(levels.size() - 1);
            if (level.valueAssumption != null) {
                lastLevel = new DescriptorStableMatch(level.valueAssumption);
            } else {
                complex = true;
                lastLevel = new Match(level.slot);
            }
            iter.previous();
        } else {
            lastLevel = new Unknown();
        }

        ArrayList<Assumption> assumptions = new ArrayList<>();
        while (iter.hasPrevious()) {
            ReadVariableLevel level = iter.previous();
            if (lastLevel instanceof DescriptorLevel) {
                if (level.enclosingDescriptorAssumption != null) {
                    assumptions.add(level.enclosingDescriptorAssumption.getAssumption());
                } else {
                    complex = true;
                    lastLevel = new NextFrameLevel(lastLevel, level.nextDescriptor());
                }
            } else {
                if (level.enclosingFrameAssumption != null) {
                    lastLevel = new NextFrameFromDescriptorLevel(lastLevel, level.enclosingFrameAssumption);
                } else {
                    complex = true;
                    lastLevel = new NextFrameLevel(lastLevel, level.nextDescriptor());
                }
            }

            if (level.slot == null) {
                if (lastLevel instanceof DescriptorLevel) {
                    assumptions.add(level.descriptor.getNotInFrameAssumption(identifier));
                } else {
                    assumptions.add(level.descriptor.getNotInFrameAssumption(identifier));
                }
            } else {
                if (level.valueAssumption != null && lastLevel instanceof DescriptorLevel) {
                    assumptions.add(level.valueAssumption.getAssumption());
                } else {
                    complex = true;
                    lastLevel = new Mismatch(lastLevel, level.slot);
                }
            }
        }
        if (!assumptions.isEmpty()) {
            lastLevel = new MultiAssumptionLevel(lastLevel, assumptions.toArray(new Assumption[assumptions.size()]));
        }

        if (FastROptions.PrintComplexLookups.getValue() && levels.size() > 1 && complex) {
            System.out.println(identifier + " " + lastLevel);
        }

        return lastLevel;
    }

    @TruffleBoundary
    public static RFunction lookupFunction(String identifier, Frame variableFrame) {
        Frame current = variableFrame;
        do {
            // see if the current frame has a value of the given name
            FrameSlot frameSlot = current.getFrameDescriptor().findFrameSlot(identifier);
            if (frameSlot != null) {
                Object value = current.getValue(frameSlot);

                if (value != null) {
                    if (value == RMissing.instance) {
                        throw RError.error(RError.Message.ARGUMENT_MISSING, identifier);
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
            current = RArguments.getEnclosingFrame(current);
        } while (current != null);
        return null;
    }

    private Object getValue(Frame variableFrame, FrameSlot frameSlot) {
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

    private Object profiledGetValue(Frame variableFrame, FrameSlot frameSlot) throws FrameSlotTypeException {
        if (seenValueKind(FrameSlotKind.Object) && variableFrame.isObject(frameSlot)) {
            Object result = variableFrame.getObject(frameSlot);
            return isNullValueProfile.profile(result == null) ? null : valueProfile.profile(result);
        } else if (seenValueKind(FrameSlotKind.Byte) && variableFrame.isByte(frameSlot)) {
            return variableFrame.getByte(frameSlot);
        } else if (seenValueKind(FrameSlotKind.Int) && variableFrame.isInt(frameSlot)) {
            return variableFrame.getInt(frameSlot);
        } else if (seenValueKind(FrameSlotKind.Double) && variableFrame.isDouble(frameSlot)) {
            return variableFrame.getDouble(frameSlot);
        } else {
            throw new FrameSlotTypeException();
        }
    }

    /**
     * This method checks the value a RVN just read. It is used to determine whether the value just
     * read matches the expected type or if we have to look in a frame up the lexical chain. It
     * might:
     * <ul>
     * <li>throw an {@link RError}: if 'objArg' is a missing argument and this is not allowed</li>
     * <li>return {@code true}: if the type of 'objArg' matches the description in 'type'</li>
     * <li>return {@code false}: if the type of 'objArg' does not match the description in 'type'</li>
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
    protected boolean checkType(VirtualFrame frame, Object objArg, ConditionProfile isNullProfile) {
        Object obj = objArg;
        if ((isNullProfile == null && obj == null) || (isNullProfile != null && isNullProfile.profile(obj == null))) {
            return false;
        }
        if (obj == RMissing.instance) {
            unexpectedMissingProfile.enter();
            throw RError.error(RError.Message.ARGUMENT_MISSING, getIdentifier());
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
                if (kind != ReadKind.Forced) {
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
        return RRuntime.checkType(obj, mode);
    }
}
