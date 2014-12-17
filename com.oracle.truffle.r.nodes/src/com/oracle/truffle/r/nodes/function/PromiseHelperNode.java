/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

        public RArgsValuesAndNames checkEvaluateArgs(VirtualFrame frame, RArgsValuesAndNames args) {
            Object[] values = args.getValues();
            Object[] newValues = new Object[values.length];
            for (int i = 0; i < values.length; i++) {
                newValues[i] = checkEvaluate(frame, values[i]);
            }
            return new RArgsValuesAndNames(newValues, args.getNames());
        }
    }

    @Child private InlineCacheNode<VirtualFrame, RNode> expressionInlineCache;
    @Child private InlineCacheNode<Frame, Closure> promiseClosureCache;

    private final ValueProfile promiseFrameProfile = ValueProfile.createClassProfile();

    /**
     * Guarded by {@link #isInOriginFrame(VirtualFrame,RPromise)}.
     *
     * @param frame The current {@link VirtualFrame}
     * @param promise The {@link RPromise} to evaluate
     * @return Evaluates the given {@link RPromise} in the given frame using the given inline cache
     */
    public Object evaluate(VirtualFrame frame, RPromise promise) {
        if (isEvaluated(promise)) {
            return promise.getValue();
        }

        // Check for dependency cycle
        if (isUnderEvaluation(promise)) {
            throw RError.error(RError.Message.PROMISE_CYCLE);
        }

        // Evaluate guarded by underEvaluation
        try {
            promise.setUnderEvaluation(true);

            Object obj;
            if (isInOriginFrame(frame, promise)) {
                if (expressionInlineCache == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    expressionInlineCache = insert(InlineCacheNode.createExpression(3));
                }
                obj = expressionInlineCache.execute(frame, (RNode) promise.getRep());
            } else {
                Frame promiseFrame = promiseFrameProfile.profile(promise.getFrame());
                assert promiseFrame != null;
                SourceSection oldCallSource = RArguments.getCallSourceSection(promiseFrame);
                try {
                    RArguments.setCallSourceSection(promiseFrame, RArguments.getCallSourceSection(frame));

                    if (promiseClosureCache == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        promiseClosureCache = insert(InlineCacheNode.createPromise(3));
                    }

                    obj = promiseClosureCache.execute(promiseFrame, promise.getClosure());
                } finally {
                    RArguments.setCallSourceSection(promiseFrame, oldCallSource);
                }
            }
            setValue(obj, promise);
            return obj;
        } finally {
            promise.setUnderEvaluation(false);
        }
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

    @TruffleBoundary
    private static Object doEvalArgument(RPromise promise, SourceSection callSrc) {
        return RContext.getEngine().evalPromise(promise, callSrc);
    }

    /**
     * @return Whether this promise is of type {@link PromiseType#ARG_DEFAULT}
     */
    public boolean isDefault(RPromise promise) {
        return isDefaultProfile.profile(promise.isDefault());
    }

    public boolean isNullFrame(RPromise promise) {
        return isNullFrameProfile.profile(promise.getFrame() == null);
    }

    public boolean isEvaluated(RPromise promise) {
        return isEvaluatedProfile.profile(promise.isEvaluated());
    }

    private final ConditionProfile isEvaluatedProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile underEvaluationProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isNullFrameProfile = ConditionProfile.createBinaryProfile();

    private final ConditionProfile isInlinedProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isDefaultProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isFrameForEnvProfile = ConditionProfile.createBinaryProfile();

    private final ValueProfile valueProfile = ValueProfile.createClassProfile();

    public boolean isInlined(RPromise promise) {
        return isInlinedProfile.profile(promise.isInlined());
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

    /**
     * Only to be called from AccessArgumentNode, and in combination with
     * {@link #updateFrame(MaterializedFrame, RPromise)}!
     *
     * @return Whether this promise needs a callee environment set (see
     *         {@link #updateFrame(MaterializedFrame, RPromise)})
     */
    public boolean needsCalleeFrame(RPromise promise) {
        return !isInlined(promise) && isDefault(promise) && isNullFrame(promise) && !isEvaluated(promise);
    }

    /**
     * @param frame
     * @return Whether the given {@link RPromise} is in its origin context and thus can be resolved
     *         directly inside the AST.
     */
    public boolean isInOriginFrame(VirtualFrame frame, RPromise promise) {
        if (isInlined(promise)) {
            return true;
        }

        if (isDefault(promise) && isNullFrame(promise)) {
            return true;
        }

        if (frame == null) {
            return false;
        }
        return isFrameForEnvProfile.profile(frame == promise.getFrame());
    }

    /**
     * This method is necessary, as we have to create {@link RPromise}s before the actual function
     * call, but the callee frame and environment get created _after_ the call happened. This update
     * has to take place in AccessArgumentNode, just before arguments get stuffed into the fresh
     * environment for the function. Whether a {@link RPromise} needs one is determined by
     * {@link #needsCalleeFrame(RPromise)}!
     *
     * @param newFrame The REnvironment this promise is to be evaluated in
     */
    public void updateFrame(MaterializedFrame newFrame, RPromise promise) {
        assert promise.isDefault();
        if (isNullFrame(promise) && !isEvaluated(promise)) {
            promise.setFrame(newFrame);
        }
    }
}
