/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;

/**
 * This node reifies a runtime object into the AST by creating nodes for frequently encountered
 * values. This can be used to bridge the gap between code as runtime data and executed code.
 */
@TypeSystemReference(RTypes.class)
public abstract class CallInlineCacheNode extends Node {

    public abstract Object execute(VirtualFrame frame, CallTarget target, Object[] arguments);

    /**
     * Creates an inline cache.
     *
     * @param maxPicDepth maximum number of entries in the polymorphic inline cache
     */
    public static CallInlineCacheNode create(int maxPicDepth) {
        return new UninitializedCallInlineCacheNode(maxPicDepth);
    }

    @NodeInfo(cost = NodeCost.UNINITIALIZED)
    private static final class UninitializedCallInlineCacheNode extends CallInlineCacheNode {

        private final int maxPicDepth;

        /** The current depth of the inline cache. */
        private int picDepth = 0;

        public UninitializedCallInlineCacheNode(int maxPicDepth) {
            this.maxPicDepth = maxPicDepth;
        }

        @Override
        public Object execute(VirtualFrame frame, CallTarget target, Object[] arguments) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            // Specialize below
            CallInlineCacheNode replacement;
            if (picDepth < maxPicDepth) {
                picDepth += 1;
                replacement = new DirectCallInlineCacheNode(target, this);
            } else {
                replacement = new GenericCallInlineCacheNode();
            }
            return replace(replacement).execute(frame, target, arguments);
        }
    }

    private static final class DirectCallInlineCacheNode extends CallInlineCacheNode {

        private final CallTarget originalTarget;
        @Child private DirectCallNode callNode;
        @Child private CallInlineCacheNode next;

        protected DirectCallInlineCacheNode(CallTarget originalTarget, CallInlineCacheNode next) {
            this.originalTarget = originalTarget;
            this.callNode = Truffle.getRuntime().createDirectCallNode(originalTarget);
            this.next = next;
        }

        @Override
        public Object execute(VirtualFrame frame, CallTarget target, Object[] arguments) {
            return target == originalTarget ? callNode.call(frame, arguments) : next.execute(frame, target, arguments);
        }
    }

    private static final class GenericCallInlineCacheNode extends CallInlineCacheNode {

        @Child private IndirectCallNode indirectCall = Truffle.getRuntime().createIndirectCallNode();

        @Override
        public Object execute(VirtualFrame frame, CallTarget target, Object[] arguments) {
            return indirectCall.call(frame, target, arguments);
        }
    }
}
