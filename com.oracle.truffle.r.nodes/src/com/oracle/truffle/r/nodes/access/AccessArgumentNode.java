/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.function.opt.EagerEvalHelper.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.variables.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.function.opt.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseType;
import com.oracle.truffle.r.runtime.data.RPromise.RPromiseFactory;
import com.oracle.truffle.r.runtime.env.*;

/**
 * This {@link RNode} returns a function's argument specified by its formal index (
 * {@link #getIndex()}). It is used to populate a function's new frame right after the actual
 * function call and before the function's actual body is executed.
 */
@NodeChild(value = "readArgNode", type = ReadArgumentNode.class)
public abstract class AccessArgumentNode extends RNode {

    @Child private PromiseHelperNode promiseHelper;

    /**
     * The formal index of this argument.
     */
    private final int index;

    public abstract ReadArgumentNode getReadArgNode();

    /**
     * Used to cache {@link RPromise} evaluations.
     */
    @Child private RNode optDefaultArgNode;
    @CompilationFinal private FormalArguments formals;
    @CompilationFinal private boolean hasDefaultArg;
    @CompilationFinal private RPromiseFactory factory;
    @CompilationFinal private boolean deoptimized;
    @CompilationFinal private boolean defaultArgCanBeOptimized = EagerEvalHelper.optConsts() || EagerEvalHelper.optDefault() || EagerEvalHelper.optExprs();

    protected AccessArgumentNode(int index) {
        this.index = index;
    }

    public void setFormals(FormalArguments formals) {
        CompilerAsserts.neverPartOfCompilation();
        assert this.formals == null;
        this.formals = formals;
        hasDefaultArg = formals.hasDefaultArgumentAt(getIndex());
    }

    /**
     * @param index {@link #getIndex()}
     * @return A fresh {@link AccessArgumentNode} for the given index
     */
    public static AccessArgumentNode create(int index) {
        return AccessArgumentNodeGen.create(index, new ReadArgumentNode(index));
    }

    @Override
    public RNode substitute(REnvironment env) {
        return this;
    }

    @Specialization(guards = {"hasDefaultArg()"})
    protected Object doArgumentDefaultArg(VirtualFrame frame, @SuppressWarnings("unused") RMissing argMissing) {
        assert !(getRootNode() instanceof RBuiltinRootNode) : getRootNode();
        Object result;
        if (canBeOptimized()) {
            // Insert default value
            checkPromiseFactory();
            if (checkInsertOptDefaultArg()) {
                result = optDefaultArgNode.execute(frame);
                // Update RArguments for S3 dispatch to work
                RArguments.setArgument(frame, index, result);
                return result;
            } else {
                // Default arg cannot be optimized: Rewrite to default and assure that we don't take
                // this path again
                CompilerDirectives.transferToInterpreterAndInvalidate();
                defaultArgCanBeOptimized = false;
            }
        }
        // Insert default value
        checkPromiseFactory();
        result = factory.createPromise(frame.materialize());
        RArguments.setArgument(frame, index, result);   // Update RArguments for S3 dispatch to work
        return result;
    }

    protected boolean hasDefaultArg() {
        return hasDefaultArg;
    }

    private boolean canBeOptimized() {
        return !deoptimized && defaultArgCanBeOptimized;
    }

    private void checkPromiseFactory() {
        if (factory == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Closure defaultClosure = formals.getOrCreateClosure(formals.getDefaultArgumentAt(getIndex()));
            factory = RPromiseFactory.create(PromiseType.ARG_DEFAULT, defaultClosure);
        }
    }

    private boolean checkInsertOptDefaultArg() {
        if (optDefaultArgNode == null) {
            RNode defaultArg = formals.getDefaultArgumentAt(getIndex());
            RNode arg = EagerEvalHelper.unfold(defaultArg);

            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (isOptimizableDefault(arg)) {
                optDefaultArgNode = new OptVariableDefaultPromiseNode(factory, (ReadVariableNode) NodeUtil.cloneNode(arg));
            } else if (isOptimizableConstant(arg)) {
                optDefaultArgNode = new OptConstantPromiseNode(factory.getType(), (ConstantNode) arg);
            }
            // else if (isOptimizableExpression(arg)) {
            // System.err.println(" >>> DEF " + arg.getSourceSection().getCode());
            // }
            if (optDefaultArgNode == null) {
                // No success: Rewrite to default
                return false;
            }
            insert(optDefaultArgNode);
        }
        return true;
    }

    @Fallback
    protected Object doArgument(Object obj) {
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
}
