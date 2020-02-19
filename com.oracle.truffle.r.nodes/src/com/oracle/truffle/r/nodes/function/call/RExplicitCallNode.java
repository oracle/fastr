/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function.call;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.function.RCallBaseNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.function.RCallNode.ExplicitArgs;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;
import com.oracle.truffle.r.runtime.RRuntimeASTAccess.ExplicitFunctionCall;

/**
 * Helper node that allows to call a given function with explicit arguments.
 */
public abstract class RExplicitCallNode extends Node implements ExplicitFunctionCall {

    public static RExplicitCallNode create() {
        return RExplicitCallNodeGen.create();
    }

    @Override
    public final Object call(VirtualFrame frame, RFunction function, RArgsValuesAndNames args) {
        return execute(frame, function, args, null, null);
    }

    public abstract Object execute(VirtualFrame frame, RFunction function, RArgsValuesAndNames args, RCaller explicitCaller, Object callerFrame);

    static FrameSlot initArgsFrameSlot(FrameDescriptor frameDesc) {
        return FrameSlotChangeMonitor.findOrAddFrameSlot(frameDesc, RFrameSlot.ExplicitCallArgs, FrameSlotKind.Object);
    }

    @Specialization(guards = "cachedFrameDesc == frame.getFrameDescriptor()", limit = "1")
    protected Object doFastCall(VirtualFrame frame, RFunction function, RArgsValuesAndNames args, RCaller caller, Object callerFrame,
                    @SuppressWarnings("unused") @Cached("frame.getFrameDescriptor()") FrameDescriptor cachedFrameDesc,
                    @Cached("createExplicitCall()") RCallBaseNode call,
                    @Cached("initArgsFrameSlot(cachedFrameDesc)") FrameSlot argsFrameSlot) {
        try {
            FrameSlotChangeMonitor.setObject(frame, argsFrameSlot, new ExplicitArgs(args, caller, callerFrame));
            return call.execute(frame, function);
        } finally {
            FrameSlotChangeMonitor.setObject(frame, argsFrameSlot, null);
        }
    }

    @Specialization(replaces = "doFastCall")
    protected Object doSlowCall(VirtualFrame frame, RFunction function, RArgsValuesAndNames args, RCaller caller, Object callerFrame,
                    @Cached("createExplicitCall()") RCallBaseNode call) {
        return slowPathCall(frame.materialize(), function, args, caller, callerFrame, call);
    }

    @TruffleBoundary
    private static Object slowPathCall(MaterializedFrame frame, RFunction function, RArgsValuesAndNames args, RCaller caller, Object callerFrame, RCallBaseNode call) {
        FrameSlot argsFrameSlot = initArgsFrameSlot(frame.getFrameDescriptor());
        try {
            FrameSlotChangeMonitor.setObject(frame, argsFrameSlot, new ExplicitArgs(args, caller, callerFrame));
            return call.execute(frame, function);
        } finally {
            FrameSlotChangeMonitor.setObject(frame, argsFrameSlot, null);
        }
    }

    protected RCallBaseNode createExplicitCall() {
        return RCallNode.createExplicitCall(RFrameSlot.ExplicitCallArgs);
    }
}
