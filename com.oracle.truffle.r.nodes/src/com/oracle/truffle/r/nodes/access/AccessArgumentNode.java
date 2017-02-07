/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.function.opt.EagerEvalHelper.getOptimizableConstant;
import static com.oracle.truffle.r.nodes.function.opt.EagerEvalHelper.isOptimizableDefault;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinRootNode;
import com.oracle.truffle.r.nodes.function.ArgumentStatePush;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.opt.EagerEvalHelper;
import com.oracle.truffle.r.nodes.function.opt.OptConstantPromiseNode;
import com.oracle.truffle.r.nodes.function.opt.OptVariablePromiseBaseNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.REmpty;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.Closure;
import com.oracle.truffle.r.runtime.data.RPromise.PromiseState;
import com.oracle.truffle.r.runtime.data.RPromise.RPromiseFactory;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * This {@link RNode} returns a function's argument specified by its formal index. It is used to
 * populate a function's new frame right after the actual function call and before the function's
 * actual body is executed.
 */
public final class AccessArgumentNode extends RNode {

    @Child private ReadArgumentNode readArgNode;

    @Child private PromiseHelperNode promiseHelper;

    /**
     * The formal index of this argument.
     */
    private final int index;

    public ReadArgumentNode getReadArgNode() {
        return readArgNode;
    }

    /**
     * Used to cache {@link RPromise} evaluations.
     */
    @Child private RNode optDefaultArgNode;
    @CompilationFinal private FormalArguments formals;
    @CompilationFinal private boolean hasDefaultArg;
    @CompilationFinal private RPromiseFactory factory;
    @CompilationFinal private boolean deoptimized;
    @CompilationFinal private boolean defaultArgCanBeOptimized = EagerEvalHelper.optConsts() || EagerEvalHelper.optDefault() || EagerEvalHelper.optExprs();

    private final ConditionProfile isMissingProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isEmptyProfile = ConditionProfile.createBinaryProfile();

    private AccessArgumentNode(int index) {
        this.index = index;
        this.readArgNode = new ReadArgumentNode(index);
    }

    public static AccessArgumentNode create(int index) {
        return new AccessArgumentNode(index);
    }

    public void setFormals(FormalArguments formals) {
        CompilerAsserts.neverPartOfCompilation();
        assert this.formals == null;
        this.formals = formals;
        hasDefaultArg = formals.hasDefaultArgument(index);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return doArgument(frame, readArgNode.execute(frame));
    }

    @Override
    public NodeCost getCost() {
        return hasDefaultArg ? NodeCost.MONOMORPHIC : NodeCost.NONE;
    }

    private Object doArgumentInternal(VirtualFrame frame) {
        assert !(getRootNode() instanceof RBuiltinRootNode) : getRootNode();
        // Insert default value
        checkPromiseFactory();
        if (canBeOptimized()) {
            if (checkInsertOptDefaultArg()) {
                return optDefaultArgNode.execute(frame);
            } else {
                /*
                 * Default arg cannot be optimized: Rewrite to default and assure that we don't take
                 * this path again
                 */
                CompilerDirectives.transferToInterpreterAndInvalidate();
                defaultArgCanBeOptimized = false;
            }
        }
        // Insert default value
        return factory.createPromise(frame.materialize());
    }

    private Object doArgument(VirtualFrame frame, Object arg) {
        if (arg == null) {
            CompilerDirectives.transferToInterpreter();
            throw RInternalError.shouldNotReachHere("null argument in slot " + index + " of " + getRootNode());
        }
        if (hasDefaultArg) {
            if (isMissingProfile.profile(arg == RMissing.instance) || isEmptyProfile.profile(arg == REmpty.instance)) {
                return doArgumentInternal(frame);
            }
        }
        return arg;
    }

    private boolean canBeOptimized() {
        return !deoptimized && defaultArgCanBeOptimized;
    }

    private void checkPromiseFactory() {
        if (factory == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            Closure defaultClosure = formals.getOrCreateClosure(formals.getDefaultArgument(index));
            factory = RPromiseFactory.create(PromiseState.Default, defaultClosure);
        }
    }

    private boolean checkInsertOptDefaultArg() {
        if (optDefaultArgNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            RNode defaultArg = formals.getDefaultArgument(index);
            RNode arg = (RNode) RASTUtils.unwrap(defaultArg);

            // TODO: all tests pass without it but perhaps we should "re-wrap" promises here?
            if (isOptimizableDefault(arg)) {
                optDefaultArgNode = new OptVariableDefaultPromiseNode(factory, (ReadVariableNode) RASTUtils.cloneNode(arg), ArgumentStatePush.INVALID_INDEX);
            } else {
                Object optimizableConstant = getOptimizableConstant(arg);
                if (optimizableConstant != null) {
                    optDefaultArgNode = new OptConstantPromiseNode(factory.getState(), arg, optimizableConstant);
                }
            }
            if (optDefaultArgNode == null) {
                // No success: Rewrite to default
                return false;
            }
            insert(optDefaultArgNode);
        }
        return true;
    }

    private final class OptVariableDefaultPromiseNode extends OptVariablePromiseBaseNode {

        OptVariableDefaultPromiseNode(RPromiseFactory factory, ReadVariableNode rvn, int wrapIndex) {
            super(factory, rvn, wrapIndex);
        }

        @Override
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
            return doArgument(frame, RMissing.instance);
        }

        @Override
        protected Object executeFallback(VirtualFrame frame) {
            return doArgument(frame, RMissing.instance);
        }

        @Override
        protected RNode createFallback() {
            throw RInternalError.shouldNotReachHere();
        }
    }
}
