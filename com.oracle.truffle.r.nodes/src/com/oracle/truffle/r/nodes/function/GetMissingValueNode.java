/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.env.frame.FrameIndex;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;

/**
 * This is a node abstraction for the functionality defined in
 * {@link RMissingHelper#getMissingValue(Frame,String)}.
 */
public abstract class GetMissingValueNode extends RBaseNode {

    public static GetMissingValueNode create(String name) {
        return new UninitializedGetMissingValueNode(name);
    }

    public abstract Object execute(Frame frame);

    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    private static final class UninitializedGetMissingValueNode extends GetMissingValueNode {

        private final String name;
        private final int varArgIndex;

        private UninitializedGetMissingValueNode(String sym) {
            varArgIndex = RSyntaxLookup.getVariadicComponentIndex(sym) - 1;
            if (varArgIndex >= 0) {
                this.name = ArgumentsSignature.VARARG_NAME;
            } else {
                this.name = sym;
            }
        }

        @Override
        public Object execute(Frame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            int frameIndex = FrameSlotChangeMonitor.getIndexOfIdentifier(frame.getFrameDescriptor(), name);
            return varArgIndex < 0 ? replace(new ResolvedGetMissingValueNode(frameIndex)).execute(frame) : replace(new ResolvedGetMissingVarArgIndexedValueNode(frameIndex, varArgIndex)).execute(frame);
        }
    }

    private static final class ResolvedGetMissingValueNode extends GetMissingValueNode {

        private final int frameIndex;
        private final ConditionProfile isObjectProfile = ConditionProfile.createBinaryProfile();

        private static final Object NON_PROMISE_OBJECT = new Object();

        private ResolvedGetMissingValueNode(int frameIndex) {
            this.frameIndex = frameIndex;
        }

        @Override
        public Object execute(Frame frame) {
            if (FrameIndex.isUninitializedIndex(frameIndex)) {
                return null;
            }
            if (isObjectProfile.profile(FrameSlotChangeMonitor.isObject(frame, frameIndex))) {
                try {
                    return FrameSlotChangeMonitor.getObject(frame, frameIndex);
                } catch (FrameSlotTypeException e) {
                    return null;
                }
            } else {
                return NON_PROMISE_OBJECT;
            }
        }
    }

    private static final class ResolvedGetMissingVarArgIndexedValueNode extends GetMissingValueNode {

        private final int varArgFrameIndex;
        private final int varArgIndex;
        private final ConditionProfile isArgMissingProfile = ConditionProfile.createBinaryProfile();

        private ResolvedGetMissingVarArgIndexedValueNode(int varArgFrameIndex, int varArgIndex) {
            assert FrameIndex.isInitializedIndex(varArgFrameIndex);
            this.varArgFrameIndex = varArgFrameIndex;
            this.varArgIndex = varArgIndex;
        }

        @Override
        public Object execute(Frame frame) {
            RArgsValuesAndNames varArgs = (RArgsValuesAndNames) FrameSlotChangeMonitor.getObject(frame, varArgFrameIndex);
            if (isArgMissingProfile.profile(varArgIndex >= varArgs.getLength())) {
                return RMissing.instance;
            } else {
                return varArgs.getArguments()[varArgIndex];
            }
        }
    }

}
