/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function.opt;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.function.ArgumentStatePush;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.nodes.function.PromiseNode;
import com.oracle.truffle.r.nodes.function.WrapArgumentNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RCaller.UnwrapPromiseCallerProfile;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.EagerFeedback;
import com.oracle.truffle.r.runtime.data.RPromise.RPromiseFactory;
import com.oracle.truffle.r.runtime.env.frame.CannotOptimizePromise;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

//@formatter:off
/**
 * Creates optimizing {@link RPromise} for expressions that should have no side effects and that are
 * always evaluated in the callee, which means that they can be evaluated in the caller.
 *
 * Whether an expression has no side effects is decided in
 * {@code EvaluatedArgumentsVisitor#isSimpleArgument}.
 *
 * Whether an argument is always going to be forced (evaluated) is decided in
 * {@code EvaluatedArgumentsVisitor}.
 *
 * Example of such expression is {@code 3+a}. Full code example:
 *
 * <pre>
 *     foo <- function(x, y) if (x == 3) y else 42
 *     a <- 1
 *     b <- 2
 *     foo(3+a, 3+b)
 * </pre>
 *
 * The challenges for the optimization are:
 * <p>
 * Like with {@link OptVariablePromiseBaseNode} that the value of variable "a" can change between
 * the time we the function is called (and this factory is called to create the promise object) and
 * the time when the promise should be really evaluated (when the called function accessed the
 * argument for the first time). Here we use the same solution as
 * {@link OptVariablePromiseBaseNode}.
 * <p>
 * If the argument may not be accessed (e.g., "b" in the example) computing its value is unnecessary
 * waste although it should not have any visible side effects. We only eagerly evaluate those
 * arguments that are always forced regardless of the what control flow inside the function is
 * actually executed.
 * <p>
 * The lookups involved in the expression (e.g., "a" in {@code a+3}) may themselves be promises and
 * they can 1) cause undesirable side effects, 2) inspect the call stack -- and call stack, at the
 * point of the eager evaluation, does not have the callee ("foo" in the example) on the top, but
 * for the promises it must look like they are being evaluated in the callee ("foo") not in the
 * caller (global environment). Both these issues are solved by calling
 * {@code setEvaluateOnlyEagerPromises(true)} on the current {@link RCaller caller}, which instructs
 * any promise evaluation to evaluate optimized promises only or throw {@link CannotOptimizePromise}
 * exception otherwise. NB: The {@code evaluateOnlyEagerPromises} flag of the caller is restored
 * after the attempted evaluation of the promises.
 * <p>
 * As long as there are more eagerly evaluated promises in the argument list of a function, then for
 * the eager value of each promise to be valid it must also hold that any other eager promise is
 * valid. For example, suppose that {@code x} is an eager promise carrying a successfully evaluated
 * eager value. On the other hand, {@code y} was also identified as an eager promise by
 * {@code EvaluatedArgumentsVisitor}, but its eager evaluation fails by throwing
 * {@link CannotOptimizePromise}.
 *
 * <pre>
 * foo(x, y)
 * </pre>
 *
 * In such a case it is necessary to invalidate the eager value of {@code x} too, as the expression
 * in {@code x} may depend on the side-effects of {@code y}, which could occur in a wrong order when
 * being evaluated in the callee. In other words, both promises must be evaluated in the callee. NB:
 * The validity of the eager promises across the arguments is maintained via the
 * {@code allArgPromisesCanOptimize} assumption.
 *
 * @see OptVariablePromiseBaseNode
 * @see PromiseNode
 */
//@formatter:on
public final class OptForcedEagerPromiseNode extends PromiseNode implements EagerFeedback {

    @Child private RNode expr;
    @Child private PromiseHelperNode promiseHelper;

    private final UnwrapPromiseCallerProfile unwrapCallerProfile = new UnwrapPromiseCallerProfile();
    private final BranchProfile nonPromiseProfile = BranchProfile.create();
    private final RPromiseFactory factory;

    @Child private PromiseNode fallback;

    /**
     * Index of the argument for which the promise is to be created.
     */
    private final int wrapIndex;

    private final Assumption allArgPromisesCanOptimize;

    public OptForcedEagerPromiseNode(RPromiseFactory factory, int wrapIndex, Assumption allArgPromisesCanOptimize) {
        super(null);
        this.factory = factory;
        this.wrapIndex = wrapIndex;
        this.expr = (RNode) factory.getExpr();
        this.allArgPromisesCanOptimize = allArgPromisesCanOptimize;
    }

    /**
     * Creates a new {@link RPromise} every time.
     */
    @Override
    public Object execute(final VirtualFrame frame) {
        Object value = null;
        try {
            allArgPromisesCanOptimize.check();
        } catch (InvalidAssumptionException e) {
            return getFallback().execute(frame);
        }

        RCaller currentCaller = RArguments.getCall(frame);
        boolean previousEvalEagerOnly = currentCaller.evaluateOnlyEagerPromises();
        try {
            currentCaller.setEvaluateOnlyEagerPromises(true);

            // need to unwrap as re-wrapping happens when the value is retrieved (otherwise ref
            // count update happens twice)

            if (wrapIndex != ArgumentStatePush.INVALID_INDEX && expr instanceof WrapArgumentNode) {
                value = ((WrapArgumentNode) expr).getOperand().execute(frame);
            } else {
                value = expr.execute(frame);
            }
            if (value instanceof RPromise) {
                if (promiseHelper == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    promiseHelper = insert(new PromiseHelperNode());
                }
                value = promiseHelper.evaluate(frame, (RPromise) value);
            } else {
                nonPromiseProfile.enter();
            }
        } catch (CannotOptimizePromise ex) {
            allArgPromisesCanOptimize.invalidate();

            if (previousEvalEagerOnly) {
                throw ex;
            }
            value = null;
        } finally {
            currentCaller.setEvaluateOnlyEagerPromises(previousEvalEagerOnly);
        }
        if (value == null) {
            return getFallback().execute(frame);
        }
        RCaller call = RCaller.unwrapPromiseCaller(currentCaller, unwrapCallerProfile);
        if (CompilerDirectives.inInterpreter()) {
            return factory.createEagerSuppliedPromise(value, allArgPromisesCanOptimize, call, this, wrapIndex, frame.materialize());
        }
        return factory.createEagerSuppliedPromise(value, allArgPromisesCanOptimize, call, this, wrapIndex, null);
    }

    @Override
    public RSyntaxNode getRSyntaxNode() {
        return getPromiseExpr();
    }

    @Override
    public RSyntaxNode getPromiseExpr() {
        return expr.asRSyntaxNode();
    }

    private PromiseNode getFallback() {
        if (fallback == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            fallback = insert(new PromisedNode(factory));
        }
        return fallback;
    }

    @Override
    public void onFailure(RPromise promise) {
        replace(new PromisedNode(factory));
    }

}
