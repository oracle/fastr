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
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.function.opt.*;
import com.oracle.truffle.r.options.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.*;

/**
 * This {@link RNode} returns a function's argument specified by its formal index (
 * {@link #getIndex()}). It is used to populate a function's new frame right after the actual
 * function call and before the function's actual body is executed.
 */
@NodeChild(value = "readArgNode", type = ReadArgumentNode.class)
public abstract class AccessArgumentNode extends RNode {

    /**
     * The formal index of this argument.
     */
    private final int index;

    public abstract ReadArgumentNode getReadArgNode();

    /**
     * Used to cache {@link RPromise} evaluations.
     */
    @Child private InlineCacheNode<VirtualFrame, RNode> promiseExpressionCache = InlineCacheNode.createExpression(3);
    @Child private RNode optDefaultArgNode = null;
    @CompilationFinal private FormalArguments formals = null;
    @CompilationFinal private RPromiseFactory factory = null;
    @CompilationFinal private boolean deoptimized = false;
    @CompilationFinal private boolean defaultArgCanBeOptimized = FastROptions.EagerEvalConstants.getValue() || FastROptions.EagerEvalVariables.getValue();  // true;

    private final BranchProfile strictEvaluation = BranchProfile.create();
    private final PromiseProfile promiseProfile = new PromiseProfile();

    public AccessArgumentNode(int index) {
        this.index = index;
    }

    public AccessArgumentNode(AccessArgumentNode prev) {
        this.index = prev.index;
    }

    /**
     * @param index {@link #getIndex()}
     * @return A fresh {@link AccessArgumentNode} for the given index
     */
    public static AccessArgumentNode create(Integer index) {
        return AccessArgumentNodeFactory.create(index, new ReadArgumentNode(index));
    }

    @Specialization
    public Object doArgument(VirtualFrame frame, RPromise promise) {
        return handlePromise(frame, promise, true);
    }

    @Specialization
    public Object doArgument(VirtualFrame frame, RArgsValuesAndNames varArgsContainer) {
        Object[] varArgs = varArgsContainer.getValues();
        for (int i = 0; i < varArgsContainer.length(); i++) {
            // DON'T use exprExecNode here, as caching would fail here: Every argument wrapped into
            // "..." is a different expression
            varArgs[i] = varArgs[i] instanceof RPromise ? handlePromise(frame, (RPromise) varArgs[i], false) : varArgs[i];
        }
        return varArgsContainer;
    }

    private Object handlePromise(VirtualFrame frame, RPromise promise, boolean useExprExecNode) {
        assert !promise.isNonArgument();
        CompilerAsserts.compilationConstant(useExprExecNode);

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

    @Specialization(guards = {"!hasDefaultArg", "!isVarArgIndex"})
    public Object doArgumentNoDefaultArg(RMissing argMissing) {
        // Simply return missing if there's no default arg OR it represents an empty "..."
        // (Empty "..." defaults to missing anyway, this way we don't have to rely on )
        return argMissing;
    }

    @Specialization(guards = {"hasDefaultArg", "canBeOptimized"})
    public Object doArgumentEagerDefaultArg(VirtualFrame frame, RMissing argMissing) {
        // Insert default value
        checkFormals();
        checkPromiseFactory();
        if (!checkInsertOptDefaultArg()) {
            // Default arg cannot be optimized: Rewrite to default and assure that we don't take
            // this path again
            CompilerDirectives.transferToInterpreterAndInvalidate();
            defaultArgCanBeOptimized = false;
            return doArgumentDefaultArg(frame, argMissing);
        }
        Object result = optDefaultArgNode.execute(frame);
        RArguments.setArgument(frame, index, result);   // Update RArguments for S3 dispatch to work
        return result;
    }

    @Specialization(guards = {"hasDefaultArg", "!canBeOptimized"})
    public Object doArgumentDefaultArg(VirtualFrame frame, @SuppressWarnings("unused") RMissing argMissing) {
        // Insert default value
        checkFormals();
        checkPromiseFactory();
        RPromise result = factory.createPromise(frame.materialize());
        RArguments.setArgument(frame, index, result);   // Update RArguments for S3 dispatch to work
        return result;
    }

    @SuppressWarnings("unused")
    protected boolean hasDefaultArg(RMissing argMissing) {
        checkFormals();
        return formals.getDefaultArg(getIndex()) != null;
    }

    @SuppressWarnings("unused")
    protected boolean isVarArgIndex(RMissing argMissing) {
        checkFormals();
        return formals.getVarArgIndex() == getIndex();
    }

    protected boolean canBeOptimized() {
        return !deoptimized && defaultArgCanBeOptimized;
    }

    private void checkFormals() {
        if (formals == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            formals = ((RRootNode) getRootNode()).getFormalArguments();
        }
    }

    private void checkPromiseFactory() {
        if (factory == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Closure defaultClosure = formals.getOrCreateClosure(formals.getDefaultArg(getIndex()));

            factory = RPromiseFactory.create(PromiseType.ARG_DEFAULT, defaultClosure, defaultClosure);
        }
    }

    private boolean checkInsertOptDefaultArg() {
        if (optDefaultArgNode == null) {
            checkFormals();
            RNode defaultArg = formals.getDefaultArg(getIndex());
            RNode arg = EagerEvalHelper.unfold(defaultArg);

            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (FastROptions.EagerEvalVariables.getValue() && EagerEvalHelper.isVariableArgument(arg)) {
                optDefaultArgNode = new OptVariableDefaultPromiseNode(factory, (ReadVariableNode) NodeUtil.cloneNode(arg));
            } else if (FastROptions.EagerEvalConstants.getValue() && EagerEvalHelper.isConstantArgument(arg)) {
                optDefaultArgNode = new OptConstantPromiseNode(factory);
            }
            if (optDefaultArgNode == null) {
                // No success: Rewrite to default
                return false;
            }
            insert(optDefaultArgNode);
        }
        return true;
    }

    @Fallback
    public Object doArgument(Object obj) {
        return obj;
    }

    public int getIndex() {
        return index;
    }

    protected final class OptVariableDefaultPromiseNode extends OptVariablePromiseBaseNode {

        public OptVariableDefaultPromiseNode(RPromiseFactory factory, ReadVariableNode rvn) {
            super(factory, rvn);
        }

        public void onSuccess(RPromise promise) {
            // TODO Auto-generated method stub

        }

        public void onFailure(RPromise promise) {
            // Assure that no further eager promises are created
            if (!deoptimized) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                deoptimized = true;
            }
        }

        @Override
        protected Object rewriteToAndExecuteFallback(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            deoptimized = true;
            return doArgumentDefaultArg(frame, RMissing.instance);
        }

        @Override
        protected Object executeFallback(VirtualFrame frame) {
            return doArgumentDefaultArg(frame, RMissing.instance);
        }

        @Override
        protected RNode createFallback() {
            throw RInternalError.shouldNotReachHere();
        }
    }

// public static final class ReadArgumentNode extends RNode {
// private final int index;
//
// private ReadArgumentNode(int index) {
// this.index = index;
// }
//
// @Override
// public Object execute(VirtualFrame frame) {
// return RArguments.getArgument(frame, index);
// }
//
// public int getIndex() {
// return index;
// }
// }
}
