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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseProfile;
import com.oracle.truffle.r.runtime.env.*;

/**
 * This {@link RNode} returns a function's argument specified by its formal index (
 * {@link #getIndex()}). It is used to populate a function's new frame right after the actual
 * function call and before the function's actual body is executed.
 */
// Fully qualified type name to circumvent compiler bug..
@NodeChild(value = "readArgNode", type = com.oracle.truffle.r.nodes.access.AccessArgumentNode.ReadArgumentNode.class)
@NodeField(name = "envProvider", type = EnvProvider.class)
public abstract class AccessArgumentNode extends RNode {

    /**
     * The {@link EnvProvider} is used to provide all arguments of with the same, lazily create
     * {@link REnvironment}.
     *
     * @return This arguments' {@link EnvProvider}
     */
    public abstract EnvProvider getEnvProvider();

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
     * Used to cache {@link RPromise} evaluations.
     */
    @Child private InlineCacheNode<VirtualFrame, RNode> promiseExpressionCache = InlineCacheNode.createExpression(3);

    private final BranchProfile needsCalleeFrame = new BranchProfile();
    private final BranchProfile strictEvaluation = new BranchProfile();
    private final PromiseProfile promiseProfile = new PromiseProfile();

    /**
     * @param index {@link #getIndex()}
     * @param envProvider
     * @return A fresh {@link AccessArgumentNode} for the given index, in the {@link REnvironment}
     *         specified by given {@link EnvProvider}
     */
    public static AccessArgumentNode create(Integer index, EnvProvider envProvider) {
        return AccessArgumentNodeFactory.create(new ReadArgumentNode(index), envProvider);
    }

    @Specialization
    public Object doArgument(VirtualFrame frame, RPromise promise) {
        return handlePromise(frame, promise, getEnvProvider(), true);
    }

    @Specialization
    public Object doArgument(VirtualFrame frame, RArgsValuesAndNames varArgsContainer) {
        Object[] varArgs = varArgsContainer.getValues();
        for (int i = 0; i < varArgsContainer.length(); i++) {
            // DON'T use exprExecNode here, as caching would fail here: Every argument wrapped into
            // "..." is a different expression
            varArgs[i] = varArgs[i] instanceof RPromise ? handlePromise(frame, (RPromise) varArgs[i], getEnvProvider(), false) : varArgs[i];
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

    private Object handlePromise(VirtualFrame frame, RPromise promise, EnvProvider envProvider, boolean useExprExecNode) {
        assert !promise.isNonArgument();
        CompilerAsserts.compilationConstant(useExprExecNode);

        // Check whether it is necessary to create a callee REnvironment for the promise
        if (promise.needsCalleeFrame(promiseProfile)) {
            needsCalleeFrame.enter();
            // In this case the promise might lack the proper REnvironment, as it was created before
            // the environment was
            promise.updateEnv(envProvider.getREnvironmentFor(frame), promiseProfile);
        }

        // Now force evaluation for INLINED (might be the case for arguments by S3MethodDispatch)
        if (promise.isInlined(promiseProfile)) {
            if (useExprExecNode) {
                return PromiseHelper.evaluate(frame, promiseExpressionCache, promise, promiseProfile);
            } else {
                strictEvaluation.enter();
                return promise.evaluate(frame, promiseProfile);
            }
        }
        return promise;
    }

    public static final class ReadArgumentNode extends RNode {
        private final int index;

        private ReadArgumentNode(int index) {
            this.index = index;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return RArguments.getArgument(frame, index);
        }

        public int getIndex() {
            return index;
        }
    }
}
