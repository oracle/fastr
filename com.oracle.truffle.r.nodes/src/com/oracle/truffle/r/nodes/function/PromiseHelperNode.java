/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import static com.oracle.truffle.r.runtime.context.FastROptions.PromiseCacheSize;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.PrimitiveValueProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.InlineCacheNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNodeFactory.GenerateValueForEagerPromiseNodeGen;
import com.oracle.truffle.r.nodes.function.call.CallerFrameClosureProvider;
import com.oracle.truffle.r.nodes.function.visibility.GetVisibilityNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.VirtualEvalFrame;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.EagerPromise;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseState;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.nodes.ShareObjectNode;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Holds {@link RPromise}-related functionality that cannot be implemented in
 * "com.oracle.truffle.r.runtime.data" due to package import restrictions.
 */
public final class PromiseHelperNode extends CallerFrameClosureProvider {

    public static final class PromiseCheckHelperNode extends RBaseNode {

        @Child private PromiseHelperNode promiseHelper;

        private final ConditionProfile isPromiseProfile = ConditionProfile.createCountingProfile();

        /**
         * Check promise evaluation and update visibility.
         *
         * @return If obj is an {@link RPromise}, it is evaluated and its result returned
         */
        public Object checkVisibleEvaluate(VirtualFrame frame, Object obj) {
            if (isPromiseProfile.profile(obj instanceof RPromise)) {
                if (promiseHelper == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    promiseHelper = insert(new PromiseHelperNode());
                }
                return promiseHelper.visibleEvaluate(frame, (RPromise) obj);
            }
            return obj;
        }

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

    public static final class PromiseDeoptimizeFrameNode extends RBaseNode {
        private final BranchProfile deoptimizeProfile = BranchProfile.create();

        /**
         * Guarantees, that all {@link RPromise}s in frame are deoptimized and thus are safe to
         * leave it's stack-branch.
         *
         * @param arguments The frame's arguments, which will be checked for {@link RPromise}s to
         *            deoptimize
         * @return Whether there was at least one {@link RPromise} which needed to be deoptimized.
         */
        public boolean deoptimizeFrame(Object[] arguments) {
            boolean deoptOne = false;
            for (Object value : arguments) {
                // If it's a promise, deoptimize it!
                if (value instanceof RPromise) {
                    RPromise promise = (RPromise) value;
                    if (!promise.isEvaluated()) {
                        deoptOne |= deoptimize(promise);
                    }
                }
            }
            return deoptOne;
        }

        private boolean deoptimize(RPromise promise) {
            if (!PromiseState.isFullPromise(promise.getState())) {
                deoptimizeProfile.enter();
                EagerPromise eager = (EagerPromise) promise;
                return eager.deoptimize();
            }

            // Nothing to do here; already the generic and slow RPromise
            return true;
        }
    }

    protected static final int RECURSIVE_PROMISE_LIMIT = 2;

    public PromiseHelperNode() {
        this((byte) DSLConfig.getCacheSize(RECURSIVE_PROMISE_LIMIT));
    }

    public PromiseHelperNode(byte recursiveCounter) {
        this.recursiveCounter = recursiveCounter;
    }

    private final byte recursiveCounter;
    @Child private InlineCacheNode promiseClosureCache;
    @Child private PromiseHelperNode nextNode = null;

    @CompilationFinal private PrimitiveValueProfile optStateProfile = PrimitiveValueProfile.createEqualityProfile();
    @CompilationFinal private ConditionProfile inOriginProfile = ConditionProfile.createBinaryProfile();
    @Child private SetVisibilityNode setVisibility;
    @Child private GenerateValueForEagerPromiseNode generateValueNonDefaultOptimizedNode;
    private final ValueProfile promiseFrameProfile = ValueProfile.createClassProfile();

    /**
     * Main entry point for proper evaluation of the given Promise when visibility updating is not
     * required; including {@link RPromise#isEvaluated()}, dependency cycles. Guarded by
     * {@link #isInOriginFrame(VirtualFrame,RPromise)}.
     *
     * @param frame The current {@link VirtualFrame}
     * @param promise The {@link RPromise} to evaluate
     * @return Evaluates the given {@link RPromise} in the given frame using the inline cache
     */
    public Object evaluate(VirtualFrame frame, RPromise promise) {
        return evaluateImpl(frame, promise, false);
    }

    /**
     * Evaluation of the given Promise updating visibility; including {@link RPromise#isEvaluated()}
     * , dependency cycles. Guarded by {@link #isInOriginFrame(VirtualFrame,RPromise)}.
     *
     * @param frame The current {@link VirtualFrame}
     * @param promise The {@link RPromise} to evaluate
     * @return Evaluates the given {@link RPromise} in the given frame using the inline cache
     */
    public Object visibleEvaluate(VirtualFrame frame, RPromise promise) {
        return evaluateImpl(frame, promise, true);
    }

    private Object evaluateImpl(VirtualFrame frame, RPromise promise, boolean visibleExec) {
        Object value = promise.getRawValue();
        if (isEvaluatedProfile.profile(value != null)) {
            return value;
        }

        int state = optStateProfile.profile(promise.getState());
        if (isExplicitProfile.profile(PromiseState.isExplicit(state))) {
            CompilerDirectives.transferToInterpreter();
            // reset profiles, this is very likely a one-time event
            isEvaluatedProfile = ConditionProfile.createBinaryProfile();
            optStateProfile = PrimitiveValueProfile.createEqualityProfile();
            return evaluateSlowPath(frame, promise);
        }

        // Default values of arguments are evaluated in the frame of the function that takes
        // them, we do not need to retrieve the frame of the promise, we already have it.
        //
        // Non-default (a.k.a. "supplied") arguments we need to evaluate in the frame of the
        // function that supplied them and that would mean frame materialization, we first try
        // to see if the promise can be optimized.

        if (isFullPromiseProfile.profile(PromiseState.isFullPromise(state))) {
            return generateValueFromFullPromise(frame, promise, visibleExec);
        } else {
            return generateValueFromEagerPromise(frame, state, (EagerPromise) promise, visibleExec);
        }
    }

    private Object generateValueFromFullPromise(VirtualFrame frame, RPromise promise, boolean visibleExec) {
        // Check for dependency cycle
        if (isUnderEvaluation(promise)) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.PROMISE_CYCLE);
        }
        try {
            if (promiseClosureCache == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                promiseClosureCache = insert(InlineCacheNode.create(DSLConfig.getCacheSize(RContext.getInstance(this).getNonNegativeIntOption(PromiseCacheSize))));
            }
            // TODO: no wrapping of arguments here?, why we do not have to set visibility here?
            promise.setUnderEvaluation();
            boolean inOrigin = inOriginProfile.profile(isInOriginFrame(frame, promise));
            Frame execFrame = inOrigin ? frame : wrapPromiseFrame(frame, promiseFrameProfile.profile(promise.getFrame()), promise, getCallerFrameObject(frame));
            Object value = promiseClosureCache.execute(execFrame, promise.getClosure());
            if (visibleExec && !inOrigin && frame != null) {
                if (setVisibility == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    setVisibility = insert(SetVisibilityNode.create());
                }
                setVisibility.execute(frame, getVisibilitySlowPath(execFrame.materialize()));
            }
            assert promise.getRawValue() == null;
            assert value != null;

            if (value instanceof RPromise) {
                RPromise nextPromise = (RPromise) value;
                value = evaluateNextNode(frame, visibleExec, nextPromise);
            }

            promise.setValue(value);
            return value;
        } finally {
            promise.resetUnderEvaluation();
        }
    }

    private Object evaluateNextNode(VirtualFrame frame, boolean visibleExec, RPromise nextPromise) {
        Object value;
        if (recursiveCounter == 0) {
            value = evaluateNextNodeSlowPath(frame.materialize(), nextPromise);
        } else {
            if (nextNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                nextNode = insert(new PromiseHelperNode((byte) (recursiveCounter - 1)));
            }
            value = visibleExec ? nextNode.visibleEvaluate(frame, nextPromise) : nextNode.evaluate(frame, nextPromise);
        }
        return value;
    }

    @TruffleBoundary
    private static Object evaluateNextNodeSlowPath(MaterializedFrame frame, RPromise nextPromise) {
        return evaluateSlowPath(frame, nextPromise);
    }

    @TruffleBoundary
    private static boolean getVisibilitySlowPath(MaterializedFrame frame) {
        return GetVisibilityNode.executeSlowPath(frame);
    }

    private Object generateValueFromEagerPromise(VirtualFrame frame, int state, EagerPromise promise, boolean visibleExec) {
        assert !PromiseState.isFullPromise(state);
        if (!isDeoptimized(promise)) {
            if (generateValueNonDefaultOptimizedNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                generateValueNonDefaultOptimizedNode = insert(GenerateValueForEagerPromiseNodeGen.create(recursiveCounter));
            }
            Object result = generateValueNonDefaultOptimizedNode.execute(frame, state, promise, visibleExec);
            if (result != null) {
                return result;
            } else {
                // Fallback: eager evaluation failed, now take the slow path
                CompilerDirectives.transferToInterpreter();
                promise.notifyFailure();
                promise.materialize();
            }
        }

        // Call
        return generateValueFromFullPromise(frame, promise, visibleExec);
    }

    @TruffleBoundary
    public static Object evaluateSlowPath(RPromise promise) {
        return evaluateSlowPath(null, promise);
    }

    public static Object evaluateSlowPath(VirtualFrame frame, RPromise promise) {
        return evaluateSlowPath(frame, promise, false);
    }

    public static Object evaluateSlowPath(VirtualFrame frame, RPromise promise, boolean visibleExec) {
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
                Object obj = generateValueFromFullPromiseSlowPath(frame, promise, visibleExec);
                // if the value is temporary, we increment the reference count. The reason is that
                // temporary values are considered available to be reused and altered (e.g. as a
                // result of arithmetic operation), which is what we do not want to happen to a
                // value that we are saving as the promise result.
                if (obj instanceof RSharingAttributeStorage) {
                    RSharingAttributeStorage shareable = (RSharingAttributeStorage) obj;
                    if (shareable.isTemporary()) {
                        shareable.incRefCount();
                    }
                }
                promise.setValue(obj);
                return obj;
            }
        }
        Object obj;
        if (PromiseState.isFullPromise(state)) {
            // Evaluate guarded by underEvaluation
            obj = generateValueFromFullPromiseSlowPath(frame, promise, visibleExec);
        } else {
            obj = generateValueFromEagerPromiseSlowPath(frame, state, (EagerPromise) promise, visibleExec);
        }
        promise.setValue(obj);
        return obj;
    }

    private static Object generateValueFromFullPromiseSlowPath(VirtualFrame frame, RPromise promise, boolean visibleExec) {
        // Check for dependency cycle
        if (promise.isUnderEvaluation()) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.PROMISE_CYCLE);
        }
        try {
            promise.setUnderEvaluation();

            MaterializedFrame evalFrame;
            Object res;
            if (promise.isInOriginFrame(frame)) {
                evalFrame = frame.materialize();
                res = promise.getClosure().eval(evalFrame);
            } else {
                evalFrame = wrapPromiseFrame(frame, promise.getFrame(), promise, frame != null ? frame.materialize() : null);
                res = promise.getClosure().eval(evalFrame);
            }

            if (res instanceof RPromise) {
                RPromise nestedPromise = (RPromise) res;
                res = evaluateSlowPath(frame, nestedPromise, visibleExec);
            }

            assert evalFrame != null;
            if (visibleExec) {
                SetVisibilityNode.executeSlowPath(frame, getVisibilitySlowPath(evalFrame));
            }
            return res;
        } finally {
            promise.resetUnderEvaluation();
        }
    }

    private static VirtualEvalFrame wrapPromiseFrame(VirtualFrame frame, MaterializedFrame promiseFrame, RPromise promise, Object callerFrameObject) {
        assert promise != null;
        assert promiseFrame != null;
        return VirtualEvalFrame.create(promiseFrame, RArguments.getFunction(promiseFrame), callerFrameObject,
                        RCaller.createForPromise(RArguments.getCall(promiseFrame), RArguments.getCall(frame), promise));
    }

    private static Object generateValueFromEagerPromiseSlowPath(VirtualFrame frame, int state, EagerPromise promise, boolean visibleExec) {
        assert !PromiseState.isFullPromise(state);
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
        return generateValueFromFullPromiseSlowPath(frame, promise, visibleExec);
    }

    /**
     * Materializes the promises' frame. After execution, it is guaranteed to be !=
     * <code>null</code>
     */
    public void materialize(RPromise promise) {
        if (!isFullPromiseProfile.profile(PromiseState.isFullPromise(promise.getState()))) {
            EagerPromise eager = (EagerPromise) promise;
            eager.materialize();
        }
        // otherwise: already the generic and slow RPromise
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

    // Eager
    private final ConditionProfile isExplicitProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isFullPromiseProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isDeoptimizedProfile = ConditionProfile.createBinaryProfile();

    /**
     * @return The state of the {@link RPromise#isUnderEvaluation()} flag.
     */
    public boolean isUnderEvaluation(RPromise promise) {
        return underEvaluationProfile.profile(promise.isUnderEvaluation());
    }

    /**
     * @return Whether the given {@link RPromise} is in its origin context and thus can be resolved
     *         directly inside the AST.
     */
    private boolean isInOriginFrame(VirtualFrame frame, RPromise promise) {
        if (isDefaultArgument(promise) && isNullFrame(promise)) {
            return true;
        }
        if (frame == null) {
            return false;
        }
        return isFrameForEnvProfile.profile(frame == promise.getFrame());
    }

    /**
     * Attempts to generate the value of the given promise in an optimized way without having to
     * materialize the promise's exec frame. If that's not possible, returns {@code null}.
     *
     * Note: we have to create a new instance of {@link WrapArgumentNode} for each
     * {@link EagerPromise#wrapIndex()} we encounter, but only for {@link EagerPromise#wrapIndex()}
     * that are up to {@link ArgumentStatePush#MAX_COUNTED_ARGS}, this is also because the
     * {@link WrapArgumentNode} takes the argument index as constructor parameter. Values of R
     * arguments have to be channeled through the {@link WrapArgumentNode} so that the reference
     * counting can work for the arguments, but the reference counting is only done for up to
     * {@link ArgumentStatePush#MAX_COUNTED_ARGS} arguments.
     */
    @ImportStatic(DSLConfig.class)
    protected abstract static class GenerateValueForEagerPromiseNode extends Node {

        protected static final int ASSUMPTION_CACHE_SIZE = ArgumentStatePush.MAX_COUNTED_ARGS + 4;
        protected static final int CACHE_SIZE = ArgumentStatePush.MAX_COUNTED_ARGS * 2;

        // If set to -1, then no further recursion should take place
        // This avoids having to invoke DSLConfig.getCacheSize in PE'd code
        private final byte recursiveCounter;
        @Child private PromiseHelperNode nextNode = null;
        @Child private SetVisibilityNode visibility;

        protected GenerateValueForEagerPromiseNode(byte recursiveCounter) {
            this.recursiveCounter = recursiveCounter;
        }

        public abstract Object execute(VirtualFrame frame, int state, EagerPromise promise, boolean visibleExec);

        // @formatter:off
        // data from "rutgen" tests
        // column A: # of distinct tuples (assumption, wrapIndex, state) observed per GenerateValueNonDefaultOptimizedNode instance
        // column B: # of GenerateValueNonDefaultOptimizedNode instances
        // A    B
        // 1    10555
        // 2    1387
        // 3    308
        // 4    199
        // 5    54
        // 6    34
        // 7    40
        // 8    8
        // 9    31
        // 10   8
        // 11   4
        // 12   4
        // 14   19
        // >14  <=4
        // @formatter:on

        @Specialization(guards = {
                        "eagerAssumption == promise.getIsValidAssumption()",
                        "isCompatibleEagerValueProfile(eagerValueProfile, state)",
                        "isCompatibleWrapNode(wrapArgumentNode, promise, state)"}, limit = "getCacheSize(ASSUMPTION_CACHE_SIZE)", assumptions = "eagerAssumption")
        Object doCachedAssumption(VirtualFrame frame, int state, EagerPromise promise, boolean visibleExec,
                        @SuppressWarnings("unused") @Cached("promise.getIsValidAssumption()") Assumption eagerAssumption,
                        @Cached("createEagerValueProfile(state)") ValueProfile eagerValueProfile,
                        @Cached("createWrapArgumentNode(promise, state)") WrapArgumentNode wrapArgumentNode) {
            return generateValue(frame, state, promise, wrapArgumentNode, eagerValueProfile, visibleExec);
        }

        @Specialization(replaces = "doCachedAssumption", guards = {
                        "isCompatibleWrapNode(wrapArgumentNode, promise, state)",
                        "isCompatibleEagerValueProfile(eagerValueProfile, state)"}, //
                        limit = "CACHE_SIZE")
        Object doUncachedAssumption(VirtualFrame frame, int state, EagerPromise promise, boolean visibleExec,
                        @Cached("createBinaryProfile()") ConditionProfile isValidProfile,
                        @Cached("createEagerValueProfile(state)") ValueProfile eagerValueProfile,
                        @Cached("createWrapArgumentNode(promise, state)") WrapArgumentNode wrapArgumentNode) {
            // Note: the assumption inside the promise is not constant anymore, so we profile the
            // result of isValid
            if (isValidProfile.profile(promise.isValid())) {
                return generateValue(frame, state, promise, wrapArgumentNode, eagerValueProfile, visibleExec);
            } else {
                return null;
            }
        }

        @Specialization(replaces = "doUncachedAssumption")
        Object doFallback(@SuppressWarnings("unused") int state, @SuppressWarnings("unused") EagerPromise promise, @SuppressWarnings("unused") boolean visibleExec) {
            throw RInternalError.shouldNotReachHere("The cache of doUncachedAssumption should never overflow");
        }

        // If promise evaluates to another promise, we create another RPromiseHelperNode to evaluate
        // that, but only up to certain recursion level
        private Object evaluateNextNode(VirtualFrame frame, RPromise nextPromise, boolean visibleExec) {
            if (recursiveCounter == 0) {
                return evaluateNextNodeSlowPath(frame.materialize(), nextPromise, visibleExec);
            }
            if (nextNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                nextNode = insert(new PromiseHelperNode((byte) (recursiveCounter - 1)));
            }
            return visibleExec
                            ? nextNode.visibleEvaluate(frame, nextPromise)
                            : nextNode.evaluate(frame, nextPromise);
        }

        @TruffleBoundary
        private static Object evaluateNextNodeSlowPath(MaterializedFrame frame, RPromise nextPromise, boolean visibleExec) {
            return PromiseHelperNode.evaluateSlowPath(frame, nextPromise, visibleExec);
        }

        private Object generateValue(VirtualFrame frame, int state, EagerPromise promise, WrapArgumentNode wrapArgumentNode, ValueProfile eagerValueProfile, boolean visibleExec) {
            Object value;
            if (PromiseState.isEager(state)) {
                assert eagerValueProfile != null;
                value = getEagerValue(frame, promise, wrapArgumentNode, eagerValueProfile, visibleExec);
            } else {
                RPromise nextPromise = (RPromise) promise.getEagerValue();
                value = evaluateNextNode(frame, nextPromise, visibleExec);
            }
            assert promise.getRawValue() == null;
            assert value != null;
            promise.setValue(value);
            return value;
        }

        /**
         * for R arguments that need to be wrapped using WrapArgumentNode, creates the
         * WrapArgumentNode, otherwise returns null.
         */
        static WrapArgumentNode createWrapArgumentNode(EagerPromise promise, int state) {
            return needsWrapNode(promise.wrapIndex(), state) ? WrapArgumentNode.create(promise.wrapIndex()) : null;
        }

        static boolean isCompatibleWrapNode(WrapArgumentNode wrapNode, EagerPromise promise, int state) {
            if (needsWrapNode(promise.wrapIndex(), state)) {
                return wrapNode != null && wrapNode.getIndex() == promise.wrapIndex();
            } else {
                return wrapNode == null;
            }
        }

        private static boolean needsWrapNode(int wrapIndex, int state) {
            return PromiseState.isEager(state) && wrapIndex != ArgumentStatePush.INVALID_INDEX && wrapIndex < ArgumentStatePush.MAX_COUNTED_ARGS;
        }

        static boolean isCompatibleEagerValueProfile(ValueProfile profile, int state) {
            return !PromiseState.isEager(state) || profile != null;
        }

        static ValueProfile createEagerValueProfile(int state) {
            return PromiseState.isEager(state) ? ValueProfile.createClassProfile() : null;
        }

        /**
         * Returns {@link EagerPromise#getEagerValue()} profiled and takes care of wrapping the
         * value with {@link WrapArgumentNode}.
         */
        private Object getEagerValue(VirtualFrame frame, EagerPromise promise, WrapArgumentNode wrapArgumentNode, ValueProfile eagerValueProfile, boolean visibleExec) {
            Object o = promise.getEagerValue();
            if (wrapArgumentNode != null) {
                // handle the reference counting
                wrapArgumentNode.execute(frame, o);
            }
            if (visibleExec) {
                if (visibility == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    visibility = insert(SetVisibilityNode.create());
                }
                visibility.execute(frame, true);
            }
            return eagerValueProfile.profile(o);
        }
    }
}
