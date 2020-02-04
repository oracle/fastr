/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.function.GetCallerFrameNodeFactory.GetCallerFrameInternalNodeGen;
import com.oracle.truffle.r.runtime.CallerFrameClosure;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RCaller.UnwrapSysParentProfile;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.VirtualEvalFrame;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * This node returns the logical parent of the current frame. In other words, it works as
 * {@code parent.frame(1)}.
 */
public final class GetCallerFrameNode extends RBaseNode {

    @Child private GetCallerFrameInternalNode getCallerFrameInternal = GetCallerFrameInternalNodeGen.create();

    private final UnwrapSysParentProfile unwrapSysParentProfile = new UnwrapSysParentProfile();

    private GetCallerFrameNode() {
    }

    public static GetCallerFrameNode create() {
        return new GetCallerFrameNode();
    }

    @Override
    public NodeCost getCost() {
        return NodeCost.NONE;
    }

    public MaterializedFrame execute(VirtualFrame frame) {
        Object callerFrameObject = RArguments.getCallerFrame(frame);
        RCaller parent = RArguments.getCall(frame);
        RCaller grandParent = RCaller.unwrapPromiseCaller(parent).getPrevious();
        grandParent = RCaller.getRegularOrSysParentCaller(grandParent, unwrapSysParentProfile);
        return getCallerFrameInternal.execute(frame, callerFrameObject, grandParent);
    }

    abstract static class GetCallerFrameInternalNode extends Node {

        private final BranchProfile closureProfile = BranchProfile.create();
        @CompilationFinal private boolean slowPathInitialized;

        public abstract MaterializedFrame execute(VirtualFrame frame, Object callerFrameObject, RCaller grandParent);

        static boolean isInPromiseFrame(VirtualFrame frame) {
            return frame instanceof VirtualEvalFrame;
        }

        static boolean hasEnvOverride(RCaller grandParent) {
            return grandParent != null && grandParent.hasEnvOverride();
        }

        @Specialization(guards = "hasEnvOverride(grandParent)")
        protected MaterializedFrame handleMaterializedFrameInPromiseFrame(@SuppressWarnings("unused") VirtualFrame frame, @SuppressWarnings("unused") Object callerFrame, RCaller grandParent) {
            return grandParent.getEnvOverride().getFrame();
        }

        @Specialization(guards = "!hasEnvOverride(grandParent)")
        protected MaterializedFrame handleFrameClosure(VirtualFrame frame, CallerFrameClosure callerFrameClosure, RCaller grandParent,
                        @Cached("createBinaryProfile()") ConditionProfile notInPromiseFrameProfile,
                        @Cached("createBinaryProfile()") ConditionProfile nonNullCallerFrameProfile) {
            if (slowPathInitialized) {
                closureProfile.enter();
            } else {
                // don't initialize the profile at the first call
                CompilerDirectives.transferToInterpreterAndInvalidate();
                slowPathInitialized = true;
            }

            // inform the responsible call node to create a caller frame
            callerFrameClosure.setNeedsCallerFrame();

            if (notInPromiseFrameProfile.profile(!isInPromiseFrame(frame))) {
                // if interpreted, we will have a materialized frame in the closure
                MaterializedFrame materializedCallerFrame = callerFrameClosure.getMaterializedCallerFrame();
                if (nonNullCallerFrameProfile.profile(materializedCallerFrame != null)) {
                    return materializedCallerFrame;
                }
            }

            return searchForFrameByCaller(frame, grandParent, nonNullCallerFrameProfile);
        }

        @Specialization(guards = {"!hasEnvOverride(grandParent)", "!isInPromiseFrame(frame)"})
        protected MaterializedFrame handleMaterializedFrameInRegularFrame(@SuppressWarnings("unused") VirtualFrame frame, MaterializedFrame callerFrame,
                        @SuppressWarnings("unused") RCaller grandParent) {
            return callerFrame;
        }

        @Specialization(guards = {"!hasEnvOverride(grandParent)", "isInPromiseFrame(frame)"})
        protected MaterializedFrame handleMaterializedFrameInPromiseFrame(VirtualFrame frame, @SuppressWarnings("unused") MaterializedFrame callerFrame, RCaller grandParent,
                        @Cached("createBinaryProfile()") ConditionProfile nonNullCallerFrameProfile) {
            return searchForFrameByCaller(frame, grandParent, nonNullCallerFrameProfile);
        }

        @Fallback
        protected MaterializedFrame handleOthers(VirtualFrame frame, Object callerFrameObject, @SuppressWarnings("unused") RCaller grandParent) {
            assert callerFrameObject == null;
            // S3 method can be dispatched from top-level where there is no caller frame
            // Since RArguments does not allow to create arguments with a 'null' caller frame, this
            // must be the top level case.
            return frame.materialize();
        }

        private static MaterializedFrame searchForFrameByCaller(Frame frame, RCaller grandParent, ConditionProfile nonNullCallerFrameProfile) {
            RCaller regularGrandParent = RCaller.unwrapPromiseCaller(grandParent);

            Frame frameFromChain = Utils.getStackFrame(frame.materialize(), regularGrandParent);
            if (nonNullCallerFrameProfile.profile(frameFromChain != null)) {
                return frameFromChain.materialize();
            }
            MaterializedFrame slowPathFrame = searchForFrameByCallerAndNotify(regularGrandParent);
            if (slowPathFrame != null) {
                return slowPathFrame;
            } else {
                return frame.materialize();
            }
        }

        @TruffleBoundary
        private static MaterializedFrame searchForFrameByCallerAndNotify(RCaller parent) {
            RError.performanceWarning("slow caller frame access");
            // for now, get it on the very slow path
            Frame callerFrame = parent == null ? null : Utils.getStackFrame(FrameAccess.MATERIALIZE, parent);
            if (callerFrame != null) {
                return callerFrame.materialize();
            }
            return null;
        }

    }
}
