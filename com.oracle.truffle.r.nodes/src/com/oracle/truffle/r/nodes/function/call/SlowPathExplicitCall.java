/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.WeakReference;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RRuntimeASTAccess.ExplicitFunctionCall;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;

public final class SlowPathExplicitCall extends TruffleBoundaryNode implements ExplicitFunctionCall {
    @Child private RExplicitCallNode slowPathCallNode;
    private WeakReference<FrameDescriptor> lastFrameDesc;

    public static SlowPathExplicitCall create() {
        return new SlowPathExplicitCall();
    }

    @TruffleBoundary
    public Object execute(MaterializedFrame evalFrame, Object callerFrame, RCaller caller, RFunction func, RArgsValuesAndNames args) {
        if (lastFrameDesc == null || lastFrameDesc.get() != evalFrame.getFrameDescriptor()) {
            lastFrameDesc = new WeakReference<>(evalFrame.getFrameDescriptor());
            slowPathCallNode = insert(RExplicitCallNode.create());
        }
        return slowPathCallNode.execute(evalFrame, func, args, caller, callerFrame);
    }

    @Override
    public Object call(VirtualFrame frame, RFunction function, RArgsValuesAndNames args) {
        return execute(frame.materialize(), null, null, function, args);
    }
}
