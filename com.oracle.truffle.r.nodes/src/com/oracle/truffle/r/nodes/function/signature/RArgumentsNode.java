/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function.signature;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RArguments.DispatchArgs;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class RArgumentsNode extends RBaseNode {

    /*
     * This inline cache is not implemented using Truffle DSL because of null values provided for
     * certain parameters, on which Truffle DSL cannot specialize.
     */

    public abstract Object[] execute(RFunction function, RCaller caller, MaterializedFrame callerFrame, int depth, MaterializedFrame promiseFrame, Object[] evaluatedArgs,
                    ArgumentsSignature signature, DispatchArgs dispatchArgs);

    public static RArgumentsNode create() {
        return new RArgumentsUninitializedNode();
    }

    @Override
    public NodeCost getCost() {
        return NodeCost.NONE;
    }

    private static final class RArgumentsUninitializedNode extends RArgumentsNode {
        @Override
        public Object[] execute(RFunction function, RCaller caller, MaterializedFrame callerFrame, int depth, MaterializedFrame promiseFrame, Object[] evaluatedArgs, ArgumentsSignature signature,
                        DispatchArgs dispatchArgs) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return replace(new RArgumentsCachedNode(function)).execute(function, caller, callerFrame, depth, promiseFrame, evaluatedArgs, signature, dispatchArgs);
        }
    }

    private static final class RArgumentsCachedNode extends RArgumentsNode {
        private final RFunction cachedFunction;

        RArgumentsCachedNode(RFunction cachedFunction) {
            this.cachedFunction = cachedFunction;
        }

        @Override
        public Object[] execute(RFunction function, RCaller caller, MaterializedFrame callerFrame, int depth, MaterializedFrame promiseFrame, Object[] evaluatedArgs, ArgumentsSignature signature,
                        DispatchArgs dispatchArgs) {
            if (function == cachedFunction) {
                return RArguments.create(cachedFunction, caller, callerFrame, depth, promiseFrame, evaluatedArgs, signature, cachedFunction.getEnclosingFrame(), dispatchArgs);
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return replace(new RArgumentsGenericNode()).execute(function, caller, callerFrame, depth, promiseFrame, evaluatedArgs, signature, dispatchArgs);
            }
        }
    }

    private static final class RArgumentsGenericNode extends RArgumentsNode {
        @Override
        public Object[] execute(RFunction function, RCaller caller, MaterializedFrame callerFrame, int depth, MaterializedFrame promiseFrame, Object[] evaluatedArgs, ArgumentsSignature signature,
                        DispatchArgs dispatchArgs) {
            return RArguments.create(function, caller, callerFrame, depth, promiseFrame, evaluatedArgs, signature, function.getEnclosingFrame(), dispatchArgs);
        }
    }
}
