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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.WriteLocalFrameVariableNode;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

/**
 * A {@link ArgumentStatePush} is used to bump up state transition for function arguments.
 */
public abstract class ArgumentStatePush extends Node {

    public abstract void executeObject(VirtualFrame frame, Object shareable);

    private final ConditionProfile isRefCountUpdateable = ConditionProfile.createBinaryProfile();

    private final int index;
    @CompilationFinal private int mask = 0;
    @Child private WriteLocalFrameVariableNode writeArgNode;

    public static final int MAX_COUNTED_ARGS = 8;
    public static final int INVALID_INDEX = -1;
    public static final int REF_COUNT_SIZE_THRESHOLD = 64;

    public ArgumentStatePush(int index) {
        this.index = index;
    }

    public boolean refCounted() {
        return mask > 0;
    }

    @Specialization
    public void transitionState(VirtualFrame frame, RShareable shareable) {
        if (isRefCountUpdateable.profile(!shareable.isSharedPermanent())) {
            shareable.incRefCount();
        }
        if (!FastROptions.RefCountIncrementOnly.getBooleanValue()) {
            if (mask == 0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                if (shareable instanceof RAbstractContainer) {
                    if (shareable instanceof RLanguage || ((RAbstractContainer) shareable).getLength() < REF_COUNT_SIZE_THRESHOLD) {
                        // don't decrement ref count for small objects or language objects- this is
                        // pretty conservative and can be further finessed
                        mask = -1;
                        return;
                    }
                }
                RFunction fun = RArguments.getFunction(frame);
                if (fun == null) {
                    mask = -1;
                    return;
                }
                Object root = fun.getRootNode();
                if (!(root instanceof FunctionDefinitionNode)) {
                    // root is RBuiltinRootNode
                    mask = -1;
                    return;
                }
                FunctionDefinitionNode fdn = (FunctionDefinitionNode) root;
                PostProcessArgumentsNode postProcessNode = fdn.getArgPostProcess();
                if (postProcessNode == null) {
                    // arguments to this function are not to be reference counted
                    mask = -1;
                    return;
                }
                // this is needed for when FunctionDefinitionNode is split by the Truffle runtime
                postProcessNode = postProcessNode.getActualNode();
                if (index >= Math.min(postProcessNode.getLength(), MAX_COUNTED_ARGS)) {
                    mask = -1;
                    return;
                }
                mask = 1 << index;
                int transArgsBitSet = postProcessNode.transArgsBitSet;
                postProcessNode.transArgsBitSet = transArgsBitSet | mask;
                writeArgNode = insert(WriteLocalFrameVariableNode.createForRefCount(Integer.valueOf(mask)));
            }
            if (mask != -1) {
                writeArgNode.execute(frame, shareable);
            }
        }
    }

    @Specialization(guards = "!isShareable(o)")
    public void transitionStateNonShareable(VirtualFrame frame, @SuppressWarnings("unused") Object o) {
        if (mask > 0) {
            // this argument used to be reference counted but is no longer
            CompilerDirectives.transferToInterpreterAndInvalidate();
            RFunction fun = RArguments.getFunction(frame);
            assert fun != null;
            FunctionDefinitionNode fdn = (FunctionDefinitionNode) fun.getRootNode();
            assert fdn != null;
            int transArgsBitSet = fdn.getArgPostProcess().transArgsBitSet;
            fdn.getArgPostProcess().transArgsBitSet = transArgsBitSet & (~mask);
            mask = -1;
        }
    }

    protected boolean isShareable(Object o) {
        return o instanceof RShareable;
    }

    public static void transitionStateSlowPath(Object o) {
        // this is expected to be used in rare cases where no RNode is easily available
        if (o instanceof RShareable) {
            RShareable shareable = (RShareable) o;
            // it's never decremented so no point in incrementing past shared state
            if (!shareable.isShared()) {
                shareable.incRefCount();
            }
        }
    }
}
