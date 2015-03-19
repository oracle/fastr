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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.VarargPromise;
import com.oracle.truffle.r.runtime.data.RPromise.*;

/**
 * Holds {@link RPromise}-related functionality that cannot be implemented in
 * "com.oracle.truffle.r.runtime.data" due to package import restrictions.
 */
public class PromiseHelperNode extends Node {

    public static class PromiseCheckHelperNode extends Node {

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

    public static class PromiseDeoptimizeFrameNode extends Node {
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
            for (FrameSlot slot : frame.getFrameDescriptor().getSlots()) {
                // We're only interested in RPromises
                if (slot.getKind() != FrameSlotKind.Object) {
                    continue;
                }

                // Try to read it...
                try {
                    Object value = frame.getObject(slot);

                    // If it's a promise, deoptimize it!
                    if (value instanceof RPromise) {
                        deoptOne |= deoptimize((RPromise) value);
                    }
                } catch (FrameSlotTypeException err) {
                    // Should not happen after former check on FrameSlotKind!
                    throw RInternalError.shouldNotReachHere();
                }
            }
            return deoptOne;
        }

        private boolean deoptimize(RPromise promise) {
            OptType optType = promise.getOptType();
            if (optType == OptType.EAGER || optType == OptType.PROMISED) {
                deoptimizeProfile.enter();
                EagerPromise eager = (EagerPromise) promise;
                return eager.deoptimize();
            }

            // Nothing to do here; already the generic and slow RPromise
            return true;
        }
    }

    @Child private InlineCacheNode<VirtualFrame, RNode> expressionInlineCache;
    @Child private InlineCacheNode<Frame, Closure> promiseClosureCache;

    @Child private PromiseHelperNode nextNode = null;

    private final ValueProfile promiseFrameProfile = ValueProfile.createClassProfile();
    private final BranchProfile varArgProfile = BranchProfile.create();
    private final BranchProfile multiVarArgProfile = BranchProfile.create();

    /**
     * Guarded by {@link #isInOriginFrame(VirtualFrame,RPromise)}.
     *
     * @param frame The current {@link VirtualFrame}
     * @param promise The {@link RPromise} to evaluate
     * @return Evaluates the given {@link RPromise} in the given frame using the given inline cache
     */
    public Object evaluate(VirtualFrame frame, RPromise promise) {
        SourceSection callSrc = null;
        if (frame != null) {
            callSrc = RArguments.getCallSourceSection(frame);
        }
        return doEvaluate(frame, promise, callSrc);
    }

    /**
     * Main entry point for proper evaluation of the given Promise; including
     * {@link RPromise#isEvaluated()}, propagation of CallSrc and dependency cycles. Actual
     * evaluation is delegated to {@link #generateValue(VirtualFrame, RPromise, SourceSection)}.
     *
     * @return The value the given Promise evaluates to
     */
    private Object doEvaluate(VirtualFrame frame, RPromise promise, SourceSection callSrc) {
        RPromise current = promise;
        if (current.getOptType() == OptType.VARARG) {
            varArgProfile.enter();
            current = ((VarargPromise) current).getVararg();
            while (current.getOptType() == OptType.VARARG) {
                multiVarArgProfile.enter();
                current = ((VarargPromise) current).getVararg();
            }
        }
        if (isEvaluated(current)) {
            return current.getValue();
        }

        // Check for dependency cycle
        if (isUnderEvaluation(current)) {
            throw RError.error(RError.Message.PROMISE_CYCLE);
        }

        // Evaluate guarded by underEvaluation
        Object obj = generateValue(frame, current, callSrc);
        setValue(obj, current);
        return obj;
    }

    /**
     * This method allows subclasses to override the evaluation method easily while maintaining
     * {@link #isEvaluated(RPromise)} and {@link #isUnderEvaluation(RPromise)} semantics.
     *
     * @param frame The {@link VirtualFrame} of the environment the Promise is forced in
     * @return The value this Promise represents
     */
    private Object generateValue(VirtualFrame frame, RPromise promise, SourceSection callSrc) {
        if (isOptDefaultProfile.profile(promise.getOptType() == OptType.DEFAULT)) {
            return generateValueDefault(frame, promise, callSrc);
        } else {
            assert promise.getOptType() == OptType.EAGER || promise.getOptType() == OptType.PROMISED;
            return generateValueEager(frame, (EagerPromise) promise, callSrc);
        }
    }

    private Object generateValueDefault(VirtualFrame frame, RPromise promise, SourceSection callSrc) {
        try {
            promise.setUnderEvaluation(true);

            if (isInOriginFrame(frame, promise)) {
                if (expressionInlineCache == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    expressionInlineCache = insert(InlineCacheNode.createExpression(3));
                }
                return expressionInlineCache.execute(frame, (RNode) promise.getRep());
            } else {
                Frame promiseFrame = promiseFrameProfile.profile(promise.getFrame());
                assert promiseFrame != null;
                SourceSection oldCallSource = RArguments.getCallSourceSection(promiseFrame);
                try {
                    RArguments.setCallSourceSection(promiseFrame, callSrc);

                    if (promiseClosureCache == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        promiseClosureCache = insert(InlineCacheNode.createPromise(3));
                    }

                    return promiseClosureCache.execute(promiseFrame, promise.getClosure());
                } finally {
                    RArguments.setCallSourceSection(promiseFrame, oldCallSource);
                }
            }
        } finally {
            promise.setUnderEvaluation(false);
        }
    }

    private Object generateValueEager(VirtualFrame frame, EagerPromise promise, SourceSection callSrc) {
        if (isDeoptimized(promise)) {
            // execFrame already materialized, feedback already given. Now we're a
            // plain'n'simple RPromise
            return generateValueDefault(frame, promise, callSrc);
        } else if (promise.isValid()) {
            if (isOptEagerProfile.profile(promise.getOptType() == OptType.EAGER)) {
                return getEagerValue(promise);
            } else {
                assert promise.getOptType() == OptType.PROMISED;
                return getPromisedEagerValue(frame, promise, callSrc);
            }
        } else {
            fallbackProfile.enter();
            promise.notifyFailure();

            // Fallback: eager evaluation failed, now take the slow path
            promise.materialize();

            // Call
            return generateValueDefault(frame, promise, callSrc);
        }
    }

    private Object getPromisedEagerValue(VirtualFrame frame, EagerPromise promise, SourceSection callSrc) {
        RPromise nextPromise = (RPromise) promise.getEagerValue();
        return checkNextNode().doEvaluate(frame, nextPromise, callSrc);
    }

    public static Object evaluateSlowPath(VirtualFrame frame, RPromise promise) {
        if (promise.isEvaluated()) {
            return promise.getValue();
        }

        // Check for dependency cycle
        if (promise.isUnderEvaluation()) {
            throw RError.error(RError.Message.PROMISE_CYCLE);
        }

        // Evaluate guarded by underEvaluation
        try {
            promise.setUnderEvaluation(true);

            Object obj;
            if (promise.isInOriginFrame(frame)) {
                obj = RContext.getEngine().eval(RDataFactory.createLanguage(promise.getRep()), frame.materialize());
            } else {
                SourceSection callSrc = frame != null ? RArguments.getCallSourceSection(frame) : null;
                obj = RContext.getEngine().evalPromise(promise, callSrc);
            }
            promise.setValue(obj);
            return obj;
        } finally {
            promise.setUnderEvaluation(false);
        }
    }

    /**
     * Materializes the promises' frame. After execution, it is guaranteed to be !=
     * <code>null</code>
     */
    public void materialize(RPromise promise) {
        if (isOptEagerProfile.profile(promise.getOptType() == OptType.EAGER) || isOptPromisedProfile.profile(promise.getOptType() == OptType.PROMISED)) {
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

    /**
     * @return Whether this promise is of type {@link PromiseType#ARG_DEFAULT}
     */
    public boolean isDefault(RPromise promise) {
        return isDefaultProfile.profile(promise.isDefault());
    }

    public boolean isNullFrame(RPromise promise) {
        return isNullFrameProfile.profile(promise.isNullFrame());
    }

    public boolean isEvaluated(RPromise promise) {
        return isEvaluatedProfile.profile(promise.isEvaluated());
    }

    public boolean isDeoptimized(EagerPromise promise) {
        return isDeoptimizedProfile.profile(promise.isDeoptimized());
    }

    private final ConditionProfile isEvaluatedProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile underEvaluationProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isNullFrameProfile = ConditionProfile.createBinaryProfile();

    private final ConditionProfile isDefaultProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isFrameForEnvProfile = ConditionProfile.createBinaryProfile();

    private final ValueProfile valueProfile = ValueProfile.createClassProfile();

    // Eager
    private final ConditionProfile isOptDefaultProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isOptEagerProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isOptPromisedProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isDeoptimizedProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile fallbackProfile = BranchProfile.create();
    private final ValueProfile eagerValueProfile = ValueProfile.createClassProfile();

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

    /**
     * Returns {@link EagerPromise#getEagerValue()} profiled.
     */
    public Object getEagerValue(EagerPromise promise) {
        return eagerValueProfile.profile(promise.getEagerValue());
    }

    /**
     * @param frame
     * @return Whether the given {@link RPromise} is in its origin context and thus can be resolved
     *         directly inside the AST.
     */
    public boolean isInOriginFrame(VirtualFrame frame, RPromise promise) {
        if (isDefault(promise) && isNullFrame(promise)) {
            return true;
        }

        if (frame == null) {
            return false;
        }
        return isFrameForEnvProfile.profile(frame == promise.getFrame());
    }
}
