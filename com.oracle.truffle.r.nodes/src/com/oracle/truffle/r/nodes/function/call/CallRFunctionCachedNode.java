/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RArguments.DispatchArgs;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.data.RFunction;

@NodeInfo(cost = NodeCost.NONE)
public abstract class CallRFunctionCachedNode extends Node {

    protected final int cacheLimit;

    protected CallRFunctionCachedNode(int cacheLimit) {
        this.cacheLimit = cacheLimit;
    }

    public final Object execute(VirtualFrame frame, RFunction function, RCaller call, MaterializedFrame callerFrame, Object[] evaluatedArgs, DispatchArgs dispatchArgs) {
        Object[] callArgs = RArguments.create(function, call, callerFrame, evaluatedArgs, dispatchArgs);
        return execute(frame, function.getTarget(), callArgs);
    }

    public final Object execute(VirtualFrame frame, RFunction function, RCaller call, MaterializedFrame callerFrame, Object[] evaluatedArgs,
                    ArgumentsSignature suppliedSignature, MaterializedFrame enclosingFrame, DispatchArgs dispatchArgs) {
        Object[] callArgs = RArguments.create(function, call, callerFrame, evaluatedArgs, suppliedSignature, enclosingFrame, dispatchArgs);
        return execute(frame, function.getTarget(), callArgs);
    }

    protected abstract Object execute(VirtualFrame frame, CallTarget target, Object[] arguments);

    protected static DirectCallNode createDirectCallNode(CallTarget target) {
        return Truffle.getRuntime().createDirectCallNode(target);
    }

    @Specialization(guards = "target == callNode.getCallTarget()", limit = "cacheLimit")
    protected Object call(VirtualFrame frame, @SuppressWarnings("unused") CallTarget target, Object[] arguments,
                    @Cached("createDirectCallNode(target)") DirectCallNode callNode) {
        return callNode.call(frame, arguments);
    }

    protected static IndirectCallNode createIndirectCallNode() {
        return Truffle.getRuntime().createIndirectCallNode();
    }

    @Specialization
    protected Object call(VirtualFrame frame, CallTarget target, Object[] arguments,
                    @Cached("createIndirectCallNode()") IndirectCallNode callNode) {
        return callNode.call(frame, target, arguments);
    }
}
