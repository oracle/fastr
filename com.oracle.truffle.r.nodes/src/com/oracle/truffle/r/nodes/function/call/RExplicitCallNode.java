/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function.call;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.access.FrameSlotNode;
import com.oracle.truffle.r.nodes.function.RCallBaseNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;

/**
 * Helper node that allows to call a given function with explicit arguments.
 */
public abstract class RExplicitCallNode extends Node {
    public static RExplicitCallNode create() {
        return RExplicitCallNodeGen.create();
    }

    public abstract Object execute(VirtualFrame frame, RFunction function, RArgsValuesAndNames args);

    /**
     * Helper method that wraps the argument into {@link RArgsValuesAndNames} and invokes the
     * {@link #execute(VirtualFrame, RFunction, RArgsValuesAndNames)} method.
     */
    public Object call(VirtualFrame frame, RFunction function, Object arg1) {
        return execute(frame, function, new RArgsValuesAndNames(new Object[]{arg1}, ArgumentsSignature.empty(1)));
    }

    @Specialization
    Object doCall(VirtualFrame frame, RFunction function, RArgsValuesAndNames args,
                    @Cached("createArgsIdentifier()") Object argsIdentifier,
                    @Cached("createExplicitCall(argsIdentifier)") RCallBaseNode call,
                    @Cached("createFrameSlotNode(argsIdentifier)") FrameSlotNode argumentsSlot) {
        FrameSlot argsFrameSlot = argumentsSlot.executeFrameSlot(frame);
        try {
            frame.setObject(argsFrameSlot, args);
            return call.execute(frame, function);
        } finally {
            frame.setObject(argsFrameSlot, null);
        }
    }

    static Object createArgsIdentifier() {
        return new Object();
    }

    static RCallBaseNode createExplicitCall(Object argsIdentifier) {
        return RCallNode.createExplicitCall(argsIdentifier);
    }

    static FrameSlotNode createFrameSlotNode(Object argsIdentifier) {
        return FrameSlotNode.createTemp(argsIdentifier, true);
    }
}
