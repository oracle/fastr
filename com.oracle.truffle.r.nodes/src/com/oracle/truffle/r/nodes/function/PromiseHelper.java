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

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseProfile;

/**
 * Holds {@link RPromise}-related functionality that cannot be implemented in
 * "com.oracle.truffle.r.runtime.data" due to package import restrictions.
 */
public class PromiseHelper {
    /**
     * Guarded by {@link RPromise#isInOriginFrame(VirtualFrame,PromiseProfile)}.
     *
     * @param frame The current {@link VirtualFrame}
     * @param expressionInlineCache An inline cache that will be used to quickly execute expression
     * @param promise The {@link RPromise} to evaluate
     * @param profile the profile for the site that operates on the promise
     * @return Evaluates the given {@link RPromise} in the given frame using the given inline cache
     */
    public static Object evaluate(VirtualFrame frame, InlineCacheNode<VirtualFrame, RNode> expressionInlineCache, RPromise promise, PromiseProfile profile) {
        if (promise.isEvaluated(profile) || !promise.isInOriginFrame(frame, profile)) {
            return promise.evaluate(frame, profile);
        }

        // Check for dependency cycle
        if (promise.isUnderEvaluation(profile)) {
            throw RError.error(RError.Message.PROMISE_CYCLE);
        }

        // Evaluate guarded by underEvaluation
        try {
            promise.setUnderEvaluation(true);

            Object obj = expressionInlineCache.execute(frame, (RNode) promise.getRep());
            promise.setValue(obj, profile);
            return obj;
        } finally {
            promise.setUnderEvaluation(false);
        }
    }
}
