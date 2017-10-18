/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.PrimitiveValueProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.InlineCacheNodes.InlineCacheNode;
import com.oracle.truffle.r.nodes.function.opt.ShareObjectNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.VirtualEvalFrame;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.EagerPromise;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseState;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Holds {@link RPromise}-related functionality that cannot be implemented in
 * "com.oracle.truffle.r.runtime.data" due to package import restrictions.
 */
public class PromiseHelperNode extends RBaseNode {

    public static class PromiseCheckHelperNode extends RBaseNode {

        @Child private PromiseHelperNode promiseHelper;

        private final ConditionProfile isPromiseProfile = ConditionProfile.createCountingProfile();

        /**
         * @return If obj is an {@link RPromise}, it is evaluated and its result returned
         */
        public Object checkEvaluate(VirtualFrame frame, Object obj) {
            if (isPromiseProfile.profile(obj instanceof RPromise)) {
                if (promiseHelper == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    promiseHelper = insert(new PromiseHelperNode());
                }
                return promiseHelper.evaluate(frame, (RPromise) obj);
            }
            return obj;
        }
    }

    public static class PromiseDeoptimizeFrameNode extends RBaseNode {
        private final BranchProfile deoptimizeProfile = BranchProfile.create();

        /**
         * Guarantees, that all {@link RPromise}s in frame are deoptimized and thus are safe to
         * leave it's stack-branch.
         *
         * @param frame The frame to check for {@link RPromise}s to deoptimize
         * @return Whether there was at least on {@link RPromise} which needed to be deoptimized.
         */
        @TruffleBoundary
        public boolean deoptimizeFrame(MaterializedFrame frame) {
            boolean deoptOne = false;
            for (FrameSlot slot : frame.getFrameDescriptor().getSlots().toArray(new FrameSlot[0])) {
                // We're only interested in RPromises
                if (slot.getKind() != FrameSlotKind.Object || !(slot.getIdentifier() instanceof String)) {
                    continue;
                }

                // Try to read it...
                try {
                    Object value = FrameSlotChangeMonitor.getObject(slot, frame);

                    // If it's a promise, deoptimize it!
                    if (value instanceof RPromise) {
                        RPromise promise = (RPromise) value;
                        if (!promise.isEvaluated()) {
                            deoptOne |= deoptimize(promise);
                        }
                    }
                } catch (FrameSlotTypeException err) {
                    // Should not happen after former check on FrameSlotKind!
                    throw RInternalError.shouldNotReachHere();
                }
            }
            return deoptOne;
        }

        private boolean deoptimize(RPromise promise) {
            if (!PromiseState.isDefaultOpt(promise.getState())) {
                deoptimizeProfile.enter();
                EagerPromise eager = (EagerPromise) promise;
                return eager.deoptimize();
            }

            // Nothing to do here; already the generic and slow RPromise
            return true;
        }
    }

    @Child private InlineCacheNode promiseClosureCache;

    @Child private PromiseHelperNode nextNode = null;

    @Children private final WrapArgumentNode[] wrapNodes = new WrapArgumentNode[ArgumentStatePush.MAX_COUNTED_ARGS];
    private final ConditionProfile shouldWrap = ConditionProfile.createBinaryProfile();

    @CompilationFinal private PrimitiveValueProfile optStateProfile = PrimitiveValueProfile.createEqualityProfile();
    private final ValueProfile isValidAssumptionProfile = ValueProfile.createIdentityProfile();
    private final ValueProfile promiseFrameProfile = ValueProfile.createClassProfile();

    /**
     * Main entry point for proper evaluation of the given Promise; including
     * {@link RPromise#isEvaluated()}, dependency cycles. Guarded by
     * {@link #isInOriginFrame(VirtualFrame,RPromise)}.
     *
     * @param frame The current {@link VirtualFrame}
     * @param promise The {@link RPromise} to evaluate
     * @return Evaluates the given {@link RPromise} in the given frame using the given inline cache
     */
    public Object evaluate(VirtualFrame frame, RPromise promise) {
        Object value = promise.getRawValue();
        if (isEvaluatedProfile.profile(value != null)) {
            return value;
        }

        int state = optStateProfile.profile(promise.getState());
        if (PromiseState.isExplicit(state)) {
            CompilerDirectives.transferToInterpreter();
            // reset profiles, this is very likely a one-time event
            isEvaluatedProfile = ConditionProfile.createBinaryProfile();
            optStateProfile = PrimitiveValueProfile.createEqualityProfile();
            return evaluateSlowPath(frame, promise);
        }
        if (PromiseState.isDefaultOpt(state)) {
            return generateValueDefault(frame, promise);
        } else {
            return generateValueNonDefault(frame, state, (EagerPromise) promise);
        }
    }

    private Object generateValueDefault(VirtualFrame frame, RPromise promise) {
        // Check for dependency cycle
        if (isUnderEvaluation(promise)) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.PROMISE_CYCLE);
        }
        try {
            if (promiseClosureCache == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseClosureCache = insert(InlineCacheNode.createPromise(FastROptions.PromiseCacheSize.getNonNegativeIntValue()));
            }
            promise.setUnderEvaluation();
            Frame execFrame = isInOriginFrame(frame, promise) ? frame : wrapPromiseFrame(frame, promiseFrameProfile.profile(promise.getFrame()));
            Object value = promiseClosureCache.execute(execFrame, promise.getClosure());
            assert promise.getRawValue() == null;
            assert value != null;
            promise.setValue(value);
            return value;
        } finally {
            promise.resetUnderEvaluation();
        }
    }

    private Object generateValueNonDefault(VirtualFrame frame, int state, EagerPromise promise) {
        assert !PromiseState.isDefaultOpt(state);
        if (!isDeoptimized(promise)) {
            Assumption eagerAssumption = isValidAssumptionProfile.profile(promise.getIsValidAssumption());
            if (eagerAssumption.isValid()) {
                Object value;
                if (PromiseState.isEager(state)) {
                    assert PromiseState.isEager(state);
                    value = getEagerValue(frame, promise);
                } else {
                    RPromise nextPromise = (RPromise) promise.getEagerValue();
                    value = checkNextNode().evaluate(frame, nextPromise);
                }
                assert promise.getRawValue() == null;
                assert value != null;
                promise.setValue(value);
                return value;
            } else {
                CompilerDirectives.transferToInterpreter();
                promise.notifyFailure();

                // Fallback: eager evaluation failed, now take the slow path
                promise.materialize();
            }
        }
        // Call
        return generateValueDefault(frame, promise);
    }

    @TruffleBoundary
    public static Object evaluateSlowPath(RPromise promise) {
        return evaluateSlowPath(null, promise);
    }

    public static Object evaluateSlowPath(VirtualFrame frame, RPromise promise) {
        CompilerAsserts.neverPartOfCompilation();
        if (promise.isEvaluated()) {
            return promise.getValue();
        }

        int state = promise.getState();
        if (PromiseState.isExplicit(state)) {
            synchronized (promise) {
                if (promise.isEvaluated()) {
                    return promise.getValue();
                }
                Object obj = generateValueDefaultSlowPath(frame, promise);
                // if the value is temporary, we increment the reference count. The reason is that
                // temporary values are considered available to be reused and altered (e.g. as a
                // result of arithmetic operation), which is what we do not want to happen to a
                // value that we are saving as the promise result.
                if (obj instanceof RShareable) {
                    RShareable shareable = (RShareable) obj;
                    if (shareable.isTemporary()) {
                        shareable.incRefCount();
                    }
                }
                promise.setValue(obj);
                return obj;
            }
        }
        Object obj;
        if (PromiseState.isDefaultOpt(state)) {
            // Evaluate guarded by underEvaluation
            obj = generateValueDefaultSlowPath(frame, promise);
        } else {
            obj = generateValueEagerSlowPath(frame, state, (EagerPromise) promise);
        }
        promise.setValue(obj);
        return obj;
    }

    private static Object generateValueDefaultSlowPath(VirtualFrame frame, RPromise promise) {
        // Check for dependency cycle
        if (promise.isUnderEvaluation()) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.PROMISE_CYCLE);
        }
        try {
            promise.setUnderEvaluation();

            if (promise.isInOriginFrame(frame)) {
                return promise.getClosure().eval(frame.materialize());
            } else {
                return promise.getClosure().eval(wrapPromiseFrame(frame, promise.getFrame()));
            }
        } finally {
            promise.resetUnderEvaluation();
        }
    }

    private static VirtualEvalFrame wrapPromiseFrame(VirtualFrame frame, MaterializedFrame promiseFrame) {
        assert promiseFrame != null;
        return VirtualEvalFrame.create(promiseFrame, RArguments.getFunction(promiseFrame), RCaller.createForPromise(RArguments.getCall(promiseFrame), frame));
    }

    private static Object generateValueEagerSlowPath(VirtualFrame frame, int state, EagerPromise promise) {
        assert !PromiseState.isDefaultOpt(state);
        if (!promise.isDeoptimized()) {
            Assumption eagerAssumption = promise.getIsValidAssumption();
            if (eagerAssumption.isValid()) {
                if (!PromiseState.isEager(state)) {
                    RPromise nextPromise = (RPromise) promise.getEagerValue();
                    return evaluateSlowPath(frame, nextPromise);
                } else {
                    Object o = promise.getEagerValue();
                    if (promise.wrapIndex() != ArgumentStatePush.INVALID_INDEX) {
                        return ShareObjectNode.share(o);
                    }
                    return o;
                }
            } else {
                promise.notifyFailure();

                // Fallback: eager evaluation failed, now take the slow path
                promise.materialize();
            }
        }
        // Call
        return generateValueDefaultSlowPath(frame, promise);
    }

    /**
     * Materializes the promises' frame. After execution, it is guaranteed to be !=
     * <code>null</code>
     */
    public void materialize(RPromise promise) {
        if (isDefaultOptProfile.profile(!PromiseState.isDefaultOpt(promise.getState()))) {
            EagerPromise eager = (EagerPromise) promise;
            eager.materialize();
        }
        // otherwise: already the generic and slow RPromise
    }

    private PromiseHelperNode checkNextNode() {
        if (nextNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            nextNode = insert(new PromiseHelperNode());
        }
        return nextNode;
    }

    private boolean isNullFrame(RPromise promise) {
        return isNullFrameProfile.profile(promise.isNullFrame());
    }

    private boolean isDeoptimized(EagerPromise promise) {
        return isDeoptimizedProfile.profile(promise.isDeoptimized());
    }

    /**
     * @return Whether this promise represents a default (as opposed to supplied) argument.
     */
    public boolean isDefaultArgument(RPromise promise) {
        return isDefaultProfile.profile(promise.isDefaultArgument());
    }

    public boolean isEvaluated(RPromise promise) {
        return isEvaluatedProfile.profile(promise.isEvaluated());
    }

    @CompilationFinal private ConditionProfile isEvaluatedProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile underEvaluationProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isNullFrameProfile = ConditionProfile.createBinaryProfile();

    private final ConditionProfile isDefaultProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isFrameForEnvProfile = ConditionProfile.createBinaryProfile();

    private final ValueProfile valueProfile = ValueProfile.createClassProfile();

    // Eager
    private final ConditionProfile isDefaultOptProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isDeoptimizedProfile = ConditionProfile.createBinaryProfile();
    private final ValueProfile eagerValueProfile = ValueProfile.createClassProfile();

    public PromiseHelperNode() {
    }

    /**
     * @return The state of the {@link RPromise#isUnderEvaluation()} flag.
     */
    public boolean isUnderEvaluation(RPromise promise) {
        return underEvaluationProfile.profile(promise.isUnderEvaluation());
    }

    /**
     * Used in case the {@link RPromise} is evaluated outside.
     *
     * @param newValue
     */
    public void setValue(Object newValue, RPromise promise) {
        promise.setValue(valueProfile.profile(newValue));
    }

    private static final int UNINITIALIZED = -1;
    private static final int GENERIC = -2;
    @CompilationFinal private int cachedWrapIndex = UNINITIALIZED;

    @Child private SetVisibilityNode visibility;

    /**
     * Returns {@link EagerPromise#getEagerValue()} profiled.
     */
    @ExplodeLoop
    private Object getEagerValue(VirtualFrame frame, EagerPromise promise) {
        Object o = promise.getEagerValue();
        int wrapIndex = promise.wrapIndex();
        if (shouldWrap.profile(wrapIndex != ArgumentStatePush.INVALID_INDEX)) {
            if (cachedWrapIndex == UNINITIALIZED) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                cachedWrapIndex = wrapIndex;
            }
            if (cachedWrapIndex != GENERIC && wrapIndex != cachedWrapIndex) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                cachedWrapIndex = GENERIC;
            }
            if (cachedWrapIndex != GENERIC) {
                if (cachedWrapIndex < ArgumentStatePush.MAX_COUNTED_ARGS) {
                    if (wrapNodes[cachedWrapIndex] == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        wrapNodes[cachedWrapIndex] = insert(WrapArgumentNode.create(cachedWrapIndex));
                    }
                    wrapNodes[cachedWrapIndex].execute(frame, o);
                }
            } else {
                for (int i = 0; i < ArgumentStatePush.MAX_COUNTED_ARGS; i++) {
                    if (wrapIndex == i) {
                        if (wrapNodes[i] == null) {
                            CompilerDirectives.transferToInterpreterAndInvalidate();
                            wrapNodes[i] = insert(WrapArgumentNode.create(i));
                        }
                        wrapNodes[i].execute(frame, o);
                    }
                }
            }
        }
        if (visibility == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            visibility = insert(SetVisibilityNode.create());
        }
        visibility.execute(frame, true);
        return eagerValueProfile.profile(o);
    }

    /**
     * @param frame
     * @return Whether the given {@link RPromise} is in its origin context and thus can be resolved
     *         directly inside the AST.
     */
    public boolean isInOriginFrame(VirtualFrame frame, RPromise promise) {
        if (isDefaultArgument(promise) && isNullFrame(promise)) {
            return true;
        }
        if (frame == null) {
            return false;
        }
        return isFrameForEnvProfile.profile(frame == promise.getFrame());
    }
}
