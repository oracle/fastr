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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.function.GetBaseEnvFrameNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;

/**
 * Helper node that allows to call a function from base environment by name. This node makes
 * assumption that a function in base environment is not going to change and can be cached.
 */
public abstract class RExplicitBaseEnvCallDispatcher extends Node {
    private final BranchProfile errorProfile = BranchProfile.create();
    @Child private LocalReadVariableNode readFunc;
    @Child RExplicitCallNode callNode = RExplicitCallNode.create();
    @Child GetBaseEnvFrameNode getBaseEnvFrameNode = GetBaseEnvFrameNode.create();

    public RExplicitBaseEnvCallDispatcher(LocalReadVariableNode readFunc) {
        this.readFunc = readFunc;
    }

    public static RExplicitBaseEnvCallDispatcher create(String funcName) {
        return RExplicitBaseEnvCallDispatcherNodeGen.create(LocalReadVariableNode.create(funcName, true));
    }

    /**
     * Helper method that wraps the argument into {@link RArgsValuesAndNames} and invokes the
     * {@link #execute(VirtualFrame, RArgsValuesAndNames)} method.
     */
    public Object call(VirtualFrame frame, Object target) {
        return execute(frame, new RArgsValuesAndNames(new Object[]{target}, ArgumentsSignature.empty(1)));
    }

    public abstract Object execute(VirtualFrame frame, RArgsValuesAndNames arguments);

    @Specialization
    public Object doCached(VirtualFrame frame, RArgsValuesAndNames arguments,
                    @Cached("getFunction(frame)") RFunction function) {
        return callNode.execute(frame, function, arguments);
    }

    RFunction getFunction(VirtualFrame frame) {
        Object function = readFunc.execute(frame, getBaseEnvFrameNode.execute());
        assert function instanceof RFunction : "unexpected that '" + readFunc.getIdentifier() + "' in base environment is not a function";
        return (RFunction) function;
    }
}
