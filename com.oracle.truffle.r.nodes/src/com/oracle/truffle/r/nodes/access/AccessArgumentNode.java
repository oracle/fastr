/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.instrument.*;
//import com.oracle.truffle.r.nodes.ReadArgumentsNodeWrapper;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * This {@link RNode} returns a function's argument specified by its formal index (
 * {@link #getIndex()}). It is used to populate a function's new frame right after the actual
 * function call and before the function's actual body is executed.
 */
// Fully qualified type name to circumvent compiler bug..
@NodeChild(value = "readArgNode", type = com.oracle.truffle.r.nodes.access.AccessArgumentNode.ReadArgumentNode.class)
public abstract class AccessArgumentNode extends RNode {

    @Child private PromiseHelperNode promiseHelper = new PromiseHelperNode();

    /**
     * @return The {@link ReadArgumentNode} that does the actual extraction from
     *         {@link RArguments#getArgument(Frame, int)}
     */
    public abstract ReadArgumentNode getReadArgNode();

    /**
     * @return Formal, 0-based index of the argument to read
     */
    public Integer getIndex() {
        return getReadArgNode().getIndex();
    }

    /**
     * @param index {@link #getIndex()}
     * @return A fresh {@link AccessArgumentNode} for the given index
     */
    public static AccessArgumentNode create(Integer index) {
        return AccessArgumentNodeFactory.create(new ReadArgumentNode(index));
    }

    @Override
    public RNode substitute(REnvironment env) {
        return this;
    }

    @Specialization
    public Object doArgument(VirtualFrame frame, RPromise promise) {
        return handlePromise(frame, promise);
    }

    @Specialization
    public Object doArgument(VirtualFrame frame, RArgsValuesAndNames varArgsContainer) {
        Object[] varArgs = varArgsContainer.getValues();
        for (int i = 0; i < varArgsContainer.length(); i++) {
            // DON'T use exprExecNode here, as caching would fail here: Every argument wrapped into
            // "..." is a different expression
            varArgs[i] = varArgs[i] instanceof RPromise ? handlePromise(frame, (RPromise) varArgs[i]) : varArgs[i];
        }
        return varArgsContainer;
    }

    @Specialization(guards = {"!isPromise", "!isRArgsValuesAndNames"})
    public Object doArgument(Object obj) {
        return obj;
    }

    public static boolean isPromise(Object obj) {
        return obj instanceof RPromise;
    }

    public static boolean isRArgsValuesAndNames(Object obj) {
        return obj instanceof RArgsValuesAndNames;
    }

    private Object handlePromise(VirtualFrame frame, RPromise promise) {
        assert !promise.isNonArgument();

        // Check whether it is necessary to create a callee REnvironment for the promise
        if (promiseHelper.needsCalleeFrame(promise)) {
            // In this case the promise might lack the proper REnvironment, as it was created before
            // the environment was
            promiseHelper.updateFrame(frame.materialize(), promise);
        }

        // Now force evaluation for INLINED (might be the case for arguments by S3MethodDispatch)
        if (promiseHelper.isInlined(promise)) {
            return promiseHelper.evaluate(frame, promise);
        }
        return promise;
    }

    @CreateWrapper
    public static class ReadArgumentNode extends RNode {
        private final int index;

        private ReadArgumentNode(int index) {
            this.index = index;
        }

        /**
         * for WrapperNode subclass.
         */
        protected ReadArgumentNode() {
            index = 0;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return RArguments.getArgument(frame, index);
        }

        public int getIndex() {
            return index;
        }

        @Override
        public ProbeNode.WrapperNode createWrapperNode(RNode node) {
            return new ReadArgumentNodeWrapper((ReadArgumentNode) node);
        }
    }
}
