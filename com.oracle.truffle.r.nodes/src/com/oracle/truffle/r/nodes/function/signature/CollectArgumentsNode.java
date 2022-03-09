/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function.signature;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.env.frame.FrameIndex;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

@ImportStatic(DSLConfig.class)
public abstract class CollectArgumentsNode extends RBaseNode {

    public abstract Object[] execute(VirtualFrame frame, ArgumentsSignature signature);

    protected Node[] createArgs(ArgumentsSignature signature, VirtualFrame frame) {
        Node[] reads = new Node[signature.getLength()];
        for (int i = 0; i < signature.getLength(); i++) {
            Object arg = RArguments.getArgument(frame, i);
            if (arg instanceof RPromise && ((RPromise) arg).isDefaultArgument()) {
                reads[i] = ConstantNode.create(RMissing.instance);
            } else {
                reads[i] = LocalReadVariableNode.create(signature.getName(i), false);
            }
        }
        return reads;
    }

    @ExplodeLoop
    @Specialization(limit = "getCacheSize(3)", guards = {"cachedSignature == signature"})
    protected Object[] combineCached(VirtualFrame frame, @SuppressWarnings("unused") ArgumentsSignature signature,
                    @Cached("signature") @SuppressWarnings("unused") ArgumentsSignature cachedSignature,
                    @Cached("createArgs(signature, frame)") Node[] reads,
                    @Cached("createBinaryProfile()") ConditionProfile valueMissingProfile,
                    @Cached("createBinaryProfile()") ConditionProfile defaultArgumentProfile) {
        Object[] result = new Object[reads.length];
        for (int i = 0; i < reads.length; i++) {
            Object value = reads[i] instanceof ConstantNode ? ((ConstantNode) reads[i]).getValue() : ((LocalReadVariableNode) reads[i]).execute(frame);
            if (defaultArgumentProfile.profile(value instanceof RPromise && ((RPromise) value).isDefaultArgument())) {
                // default arguments should be evaluated in the frame of the target function, if we
                // just use missing here, the target function itself will create the correct promise
                // for default argument with the correct exec frame
                result[i] = RMissing.instance;
            } else {
                result[i] = valueMissingProfile.profile(value == null) ? RMissing.instance : value;
            }
        }
        return result;
    }

    @Specialization
    protected Object[] combine(VirtualFrame frame, ArgumentsSignature signature) {
        return readFromMaterialized(frame.materialize(), signature);
    }

    @TruffleBoundary
    private static Object[] readFromMaterialized(MaterializedFrame frame, ArgumentsSignature signature) {
        Object[] result = new Object[signature.getLength()];
        FrameDescriptor desc = frame.getFrameDescriptor();
        for (int i = 0; i < signature.getLength(); i++) {
            int frameIndex = FrameSlotChangeMonitor.getIndexOfIdentifier(desc, signature.getName(i));
            if (FrameIndex.isUninitializedIndex(frameIndex)) {
                result[i] = RMissing.instance;
            } else {
                Object value = FrameSlotChangeMonitor.getObject(frame, frameIndex);
                if (value instanceof RPromise && ((RPromise) value).isDefaultArgument()) {
                    result[i] = RMissing.instance;
                } else {
                    result[i] = value == null ? RMissing.instance : value;
                }
            }
        }
        return result;
    }
}
