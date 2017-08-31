/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RArguments.DispatchArgs;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.data.RFunction;

@NodeInfo(cost = NodeCost.NONE)
public abstract class CallRFunctionCachedNode extends CallRFunctionBaseNode {

    @Child private SetVisibilityNode visibility = SetVisibilityNode.create();

    protected final int cacheLimit;

    protected CallRFunctionCachedNode(int cacheLimit) {
        this.cacheLimit = cacheLimit;
    }

    public final Object execute(VirtualFrame frame, RFunction function, RCaller call, Object[] evaluatedArgs, DispatchArgs dispatchArgs) {
        Object[] callArgs = RArguments.create(function, call, getCallerFrameObject(frame), evaluatedArgs, dispatchArgs);
        return execute(frame, function.getTarget(), callArgs, call);
    }

    public final Object execute(VirtualFrame frame, RFunction function, RCaller call, Object[] evaluatedArgs,
                    ArgumentsSignature suppliedSignature, MaterializedFrame enclosingFrame, DispatchArgs dispatchArgs) {
        Object[] callArgs = RArguments.create(function, call, getCallerFrameObject(frame), evaluatedArgs, suppliedSignature, enclosingFrame, dispatchArgs);
        return execute(frame, function.getTarget(), callArgs, call);
    }

    protected abstract Object execute(VirtualFrame frame, CallTarget target, Object[] arguments, RCaller caller);

    protected static DirectCallNode createDirectCallNode(CallTarget target) {
        return Truffle.getRuntime().createDirectCallNode(target);
    }

    @Specialization(guards = "target == callNode.getCallTarget()", limit = "cacheLimit")
    protected Object call(VirtualFrame frame, @SuppressWarnings("unused") CallTarget target, Object[] arguments, RCaller caller,
                    @Cached("createDirectCallNode(target)") DirectCallNode callNode) {
        try {
            return callNode.call(arguments);
        } finally {
            visibility.executeAfterCall(frame, caller);
        }
    }

    protected static IndirectCallNode createIndirectCallNode() {
        return Truffle.getRuntime().createIndirectCallNode();
    }

    @Specialization
    protected Object call(VirtualFrame frame, CallTarget target, Object[] arguments, RCaller caller,
                    @Cached("createIndirectCallNode()") IndirectCallNode callNode) {
        try {
            return callNode.call(target, arguments);
        } finally {
            visibility.executeAfterCall(frame, caller);
        }
    }
}
