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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.EmptyTypeSystemFlatLayout;
import com.oracle.truffle.r.nodes.access.WriteLocalFrameVariableNode;
import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

/**
 * A {@link ArgumentStatePush} is used to bump up state transition for function arguments.
 */
@TypeSystemReference(EmptyTypeSystemFlatLayout.class)
public abstract class ArgumentStatePush extends Node {

    public abstract void executeObject(VirtualFrame frame, Object shareable);

    @Child private WriteLocalFrameVariableNode write;

    private final ConditionProfile isRefCountUpdateable = ConditionProfile.createBinaryProfile();

    private final int index;

    @Child private WriteLocalFrameVariableNode writeArgNode;

    public static final int MAX_COUNTED_ARGS = 8;
    public static final int INVALID_INDEX = -1;
    public static final int REF_COUNT_SIZE_THRESHOLD = 64;

    public ArgumentStatePush(int index) {
        this.index = index;
    }

    protected int createWriteArgMask(VirtualFrame frame, RShareable shareable) {
        if (shareable instanceof RAbstractContainer) {
            if (shareable instanceof RLanguage || ((RAbstractContainer) shareable).getLength() < REF_COUNT_SIZE_THRESHOLD) {
                // don't decrement ref count for small objects or language objects- this
                // is pretty conservative and can be further finessed
                return -1;
            }
        }
        RFunction fun = RArguments.getFunction(frame);
        if (fun == null) {
            return -1;
        }
        Object root = fun.getRootNode();
        if (!(root instanceof FunctionDefinitionNode)) {
            // root is RBuiltinRootNode
            return -1;
        }
        FunctionDefinitionNode fdn = (FunctionDefinitionNode) root;
        PostProcessArgumentsNode postProcessNode = fdn.getArgPostProcess();
        if (postProcessNode == null) {
            // arguments to this function are not to be reference counted
            return -1;
        }
        if (!postProcessNode.updateBits(index)) {
            // this will fail if the index is too big
            return -1;
        }
        return 1 << index;
    }

    @Specialization
    public void transitionState(VirtualFrame frame, RSharingAttributeStorage shareable,
                    @Cached("createWriteArgMask(frame, shareable)") int writeArgMask) {
        if (isRefCountUpdateable.profile(!shareable.isSharedPermanent())) {
            shareable.incRefCount();
            if (writeArgMask != -1 && !FastROptions.RefCountIncrementOnly.getBooleanValue()) {
                if (write == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    write = insert(WriteLocalFrameVariableNode.createForRefCount(Integer.valueOf(writeArgMask)));
                }
                write.execute(frame, shareable);
            }
        }
    }

    @Specialization
    public void transitionState(RShareable shareable) {
        throw RInternalError.shouldNotReachHere("unexpected RShareable that is not RSharingAttributeStorage: " + shareable.getClass());
    }

    @Specialization(guards = "!isShareable(o)")
    public void transitionStateNonShareable(@SuppressWarnings("unused") Object o) {
        // do nothing
    }

    protected boolean isShareable(Object o) {
        return o instanceof RShareable;
    }
}
