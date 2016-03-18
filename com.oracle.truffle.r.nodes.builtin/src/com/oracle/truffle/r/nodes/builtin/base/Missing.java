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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.InlineCacheNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.GetMissingValueNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.RMissingHelper;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.nodes.RNode;

@RBuiltin(name = "missing", kind = PRIMITIVE, parameterNames = {"x"}, nonEvalArgs = 0)
public abstract class Missing extends RBuiltinNode {

    @Child private InlineCacheNode repCache;

    private final ConditionProfile isSymbolNullProfile = ConditionProfile.createBinaryProfile();

    private static InlineCacheNode createRepCache(int level) {
        Function<String, RNode> reify = symbol -> createNodeForRep(symbol, level);
        BiFunction<Frame, String, Object> generic = (frame, symbol) -> RRuntime.asLogical(RMissingHelper.isMissingArgument(frame, symbol));
        return InlineCacheNode.createCache(3, reify, generic);
    }

    private static RNode createNodeForRep(String symbol, int level) {
        if (symbol == null) {
            return ConstantNode.create(RRuntime.LOGICAL_FALSE);
        }
        return new MissingCheckLevel(symbol, level);
    }

    private static class MissingCheckLevel extends RNode {

        @Child private GetMissingValueNode getMissingValue;
        @Child private InlineCacheNode recursive;
        @Child private PromiseHelperNode promiseHelper;

        @CompilationFinal private FrameDescriptor recursiveDesc;

        private final ConditionProfile isNullProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isMissingProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isPromiseProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isSymbolNullProfile = ConditionProfile.createBinaryProfile();
        private final int level;

        MissingCheckLevel(String symbol, int level) {
            this.level = level;
            this.getMissingValue = GetMissingValueNode.create(symbol);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            // Read symbols value directly
            Object value = getMissingValue.execute(frame);
            if (isNullProfile.profile(value == null)) {
                // In case we are not able to read the symbol in current frame: This is not an
                // argument and thus return false
                return RRuntime.LOGICAL_FALSE;
            }

            if (isMissingProfile.profile(RMissingHelper.isMissing(value))) {
                return RRuntime.LOGICAL_TRUE;
            }

            // This might be a promise...
            if (isPromiseProfile.profile(value instanceof RPromise)) {
                RPromise promise = (RPromise) value;
                if (promiseHelper == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    promiseHelper = insert(new PromiseHelperNode());
                    recursiveDesc = promise.getFrame() != null ? promise.getFrame().getFrameDescriptor() : null;
                }
                if (level == 0 && promiseHelper.isDefault(promise)) {
                    return RRuntime.LOGICAL_TRUE;
                }
                if (level > 0 && promiseHelper.isEvaluated(promise)) {
                    return RRuntime.LOGICAL_FALSE;
                }
                // Check: If there is a cycle, return true. (This is done like in GNU R)
                if (promiseHelper.isUnderEvaluation(promise)) {
                    return RRuntime.LOGICAL_TRUE;
                }
                String symbol = RMissingHelper.unwrapName((RNode) promise.getRep());
                if (isSymbolNullProfile.profile(symbol == null)) {
                    return RRuntime.LOGICAL_FALSE;
                } else {
                    if (recursiveDesc != null) {
                        promiseHelper.materialize(promise); // Ensure that promise holds a frame
                    }
                    if (recursiveDesc == null || recursiveDesc != promise.getFrame().getFrameDescriptor()) {
                        if (promiseHelper.isEvaluated(promise)) {
                            return RRuntime.LOGICAL_FALSE;
                        } else {
                            return RRuntime.asLogical(RMissingHelper.isMissingName(promise));
                        }
                    } else {
                        if (recursiveDesc == null) {
                            promiseHelper.materialize(promise); // Ensure that promise holds a frame
                        }
                        try {
                            promise.setUnderEvaluation(true);
                            if (recursive == null) {
                                CompilerDirectives.transferToInterpreterAndInvalidate();
                                recursive = insert(createRepCache(level + 1));
                            }
                            return recursive.execute(promise.getFrame(), symbol);
                        } finally {
                            promise.setUnderEvaluation(false);
                        }
                    }
                }
            }
            return RRuntime.LOGICAL_FALSE;
        }
    }

    @Specialization
    protected byte missing(VirtualFrame frame, RPromise promise) {
        controlVisibility();
        if (repCache == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            repCache = insert(createRepCache(0));
        }
        String symbol = RMissingHelper.unwrapName((RNode) promise.getRep());
        return isSymbolNullProfile.profile(symbol == null) ? RRuntime.LOGICAL_FALSE : (byte) repCache.execute(frame, symbol);
    }

    @Specialization
    protected byte missing(@SuppressWarnings("unused") RMissing obj) {
        controlVisibility();
        return RRuntime.LOGICAL_TRUE;
    }

    @Fallback
    protected byte missing(@SuppressWarnings("unused") Object obj) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }
}
