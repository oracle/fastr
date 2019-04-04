/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.nodes.profile.TruffleBoundaryNode;
import com.oracle.truffle.r.runtime.builtins.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.data.RFunction;

/**
 * Directly calls given built-in, unlike {@link RExplicitCallNode} does not go through argument
 * matching, dispatch etc.
 */
@NodeInfo(cost = NodeCost.NONE)
public abstract class CallRBuiltinCachedNode extends CallerFrameClosureProvider {

    @Child private SetVisibilityNode visibility = SetVisibilityNode.create();

    protected final int cacheLimit;

    protected CallRBuiltinCachedNode(int cacheLimit) {
        this.cacheLimit = cacheLimit;
    }

    public static CallRBuiltinCachedNode create(int cacheSize) {
        return CallRBuiltinCachedNodeGen.create(cacheSize);
    }

    public abstract Object execute(VirtualFrame frame, RFunction fun, Object[] arguments);

    @Specialization(guards = "builtin == fun.getRBuiltin()", limit = "cacheLimit")
    protected Object callBuiltin(VirtualFrame frame, @SuppressWarnings("unused") RFunction fun, Object[] arguments,
                    @Cached("fun.getRBuiltin()") RBuiltinDescriptor builtin,
                    @Cached("createBuiltinInline(builtin)") RBuiltinNode callBuiltin) {
        try {
            return callBuiltin.call(frame, arguments);
        } finally {
            visibility.execute(frame, builtin.getVisibility());
        }
    }

    @Specialization(replaces = "callBuiltin")
    protected Object callBuiltinGeneric(VirtualFrame frame, RFunction fun, Object[] arguments,
                    @Cached("create()") CallBuiltinGeneric callNode) {
        try {
            return callNode.execute(frame.materialize(), fun, arguments);
        } finally {
            visibility.execute(frame, fun.getRBuiltin().getVisibility());
        }
    }

    protected static RBuiltinNode createBuiltinInline(RBuiltinDescriptor builtin) {
        return RBuiltinNode.inline(builtin);
    }

    protected static final class CallBuiltinGeneric extends TruffleBoundaryNode {

        public static CallBuiltinGeneric create() {
            return new CallBuiltinGeneric();
        }

        @TruffleBoundary
        public Object execute(MaterializedFrame frame, RFunction fun, Object[] arguments) {
            RBuiltinNode call = insert(RBuiltinNode.inline(fun.getRBuiltin()));
            return call.call(frame, arguments);
        }
    }
}
