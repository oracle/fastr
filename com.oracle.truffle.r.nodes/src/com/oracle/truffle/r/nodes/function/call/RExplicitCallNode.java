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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.function.RCallBaseNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;

/**
 * Helper node that allows to call a given function with explicit arguments.
 */
public abstract class RExplicitCallNode extends Node {

    public static RExplicitCallNode create() {
        return RExplicitCallNodeGen.create();
    }

    public abstract Object execute(VirtualFrame frame, RFunction function, RArgsValuesAndNames args);

    private final RFrameSlot argsIdentifier = RFrameSlot.createTemp(true);
    @CompilationFinal private FrameSlot argsFrameSlot;

    @Specialization
    protected Object doCall(VirtualFrame frame, RFunction function, RArgsValuesAndNames args,
                    @Cached("createExplicitCall()") RCallBaseNode call) {
        if (argsFrameSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            argsFrameSlot = FrameSlotChangeMonitor.findOrAddFrameSlot(frame.getFrameDescriptor(), argsIdentifier, FrameSlotKind.Object);
        }
        try {
            frame.setObject(argsFrameSlot, args);
            FrameSlotChangeMonitor.setObject(frame, argsFrameSlot, args);
            return call.execute(frame, function);
        } finally {
            FrameSlotChangeMonitor.setObject(frame, argsFrameSlot, null);
        }
    }

    protected RCallBaseNode createExplicitCall() {
        return RCallNode.createExplicitCall(argsIdentifier);
    }
}
