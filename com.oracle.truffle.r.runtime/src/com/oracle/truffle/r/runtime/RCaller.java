/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.CannotOptimizePromise;
import com.oracle.truffle.r.runtime.env.frame.RFrameSlot;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Represents the caller of a function and other information related an R stack frame evaluation
 * context. It is created by the caller of the function and passed in the arguments array.
 * {@link RCaller} instances for all R stack frames form a linked list.
 *
 * A value of this type never appears in a Truffle execution. The closest concept in GNU-R is
 * RCNTXT, see {@code main/context.c}.
 *
 * On the high level {@link RCaller} instance holds:
 * <ul>
 * <li>link to the {@link RCaller} associated with the previous R stack frame.</li>
 * <li>frame number as described in {@code sys.parent} R function documentation: frames are numbered
 * from 0 (global environment).</li>
 * <li>the AST of the call that invoked this context or a link to the "sys parent" in promise
 * evaluation frames -- logical parent in the sense of R's {@code parent.frame} semantics. These are
 * crammed in the {@link #payload} field (because often {@link #previous} is the same as the logical
 * parent).</li>
 * </ul>
 *
 * NOTE: It is important to create new unique caller instances for each stack frame, so that
 * {@link ReturnException#getTarget()} can uniquely identify the target frame.
 *
 * Example:
 *
 * <pre>
 *     foo <- function(a) a
 *     bar <- function() foo(promiseFun(1))
 *     promiseFun <- function(a) a+1
 *     bar()
 * </pre>
 *
 * When {@code promiseFun} is entered (which is at the point where {@code a} is evaluated in
 * {@code foo}), the stack frames will look like:
 *
 * <pre>
 *   promiseFun      (depth = 3, parent = bar, payload = "promiseFun(1)")
 *   internal frame  (depth = 2, parent = global, payload = RCaller:bar)  <-- this may not be a real Truffle execution frame!
 *   foo             (depth = 2, parent = bar, payload = "foo(promiseFun(1))")
 *   bar             (depth = 1, parent = global, payload = "bar()")
 *   global          (depth = 0, parent = null, payload = null)
 * </pre>
 *
 * Where the 'internal frame' wraps the frame of bar so that the promise code can access all the
 * local variables of bar, but the {@link RCaller} can be different: the depth that would normally
 * be 1 is 2, and parent and payload are different (see docs of {@link #isPromise()}). The purpose
 * of {@link #payload} in such frames is that we can use it to reach the actual frame where the
 * promise is logically evaluated, should the promise call some stack introspection built-in, e.g.
 * {@code parent.frame()}. The reason why depths is 2 is that any consecutive function call uses
 * current depth + 1 as the new depth and we need the new depth to be 3.
 *
 * Note that the 'internal frame' may not be on the real execution stack (i.e. not iterable by
 * Truffle). The {@code InlineCacheNode} directly injects the AST of the promise into the calling
 * function AST (foo in this case), but passes the 'internal frame' to the execute method instead of
 * the current {@code VirtualFrame} (so that the injected AST thinks that it is executed inside bar
 * and not foo). If the cache is full, then the {@code InlineCacheNode} creates a new
 * {@link com.oracle.truffle.api.CallTarget} and calls it with 'internal frame', in which case there
 * will be additional frame when iterating frames with Truffle, but that frame will hold the
 * 'internal frame' inside its arguments array.
 *
 * For debugging purposes, there is {@code com.oracle.truffle.r.nodes.builtin.fastr.FastRRCallerTrace}
 * builtin that prints RCaller hierarchy from the current Truffle execution frame.
 *
 * See {@code FrameFunctions}.
 *
 * @see RArguments
 */
public final class RCaller {

    public static final RCaller topLevel = RCaller.createInvalid(null);

    /**
     * Determines the actual position of the corresponding frame on the Truffle execution call
     * stack.
     *
     * When one follows the {@link RCaller#previous} chain, then the depth should be decreasing by
     * one or stay the same. The reason for it staying the same are artificial frames used for
     * promise evaluation. When iterating {@link RCaller} instances chain via {@link #previous} one
     * should skip such artificial {@link RCaller}s.
     *
     * Note: this is depth of the frame where this {@link RCaller} is stored (in the arguments
     * array), not the depth of the parent.
     */
    private final int depth;

    /**
     * @see RFrameSlot#Visibility
     */
    private boolean visibility;

    /**
     * Link to the {@link RCaller} stored in the arguments array of the previous R stack frame. We
     * need this link so that we do not have to walk the stack frames if we need to reach
     * {@link RCaller} instances on the stack.
     *
     * If this {@link RCaller} is artificial {@link RCaller} used for promise evaluation, then this
     * is not the R level parent frame in the sense of {@code parent.frame()} R function. In such
     * case {@link #payload} gives the R level parent frame.
     */
    private final RCaller previous;

    /**
     * The payload can be
     * <ul>
     * <li>{@code null} for top level {@link RCaller} (~global environment)</li>
     * <li>{@link RSyntaxNode} or its {@link Supplier supplier} in a regular (i.e. non-promise
     * evaluation) frame</li>
     * <li>{@link RCaller} in a promise evaluation frame, see {@link #isPromise()}</li>
     * <li>{@link NonPromiseLogicalParent} or {@link PromiseLogicalParent}</li>
     * </ul>
     *
     * If the function was invoked via regular call node, then the syntax can be that call node
     * (RSyntaxNode case), if the function was invoked by other means and we do not have the actual
     * syntax for the invocation, we only provide it lazily via Supplier, so that we do not have to
     * always construct the AST nodes.
     *
     * If this {@link RCaller} represents an artificial context for promise evaluation, the payload
     * points to the {@link RCaller} of the context where the promise should be logically evaluated.
     */
    private final Object payload;

    /**
     * This flag instructs {@code PromiseHelperNode} to evaluate eager promises only. Otherwise, it
     * throws {@code CannotOptimizePromise}. Also see {@code OptForcedEagerPromiseNode}.
     */
    private boolean evaluateOnlyEagerPromises;

    private RCaller(Frame callingFrame, Object payload) {
        this(depthFromFrame(callingFrame), parentFromFrame(callingFrame), payload);
    }

    private RCaller(int depth, RCaller previous, Object payload) {
        assert payload == null || payload instanceof Supplier<?> || payload instanceof RCaller || payload instanceof LogicalParent || payload instanceof RSyntaxNode : payload;
        this.depth = depth;
        this.previous = previous;
        this.payload = payload;
        this.evaluateOnlyEagerPromises = previous != null ? previous.evaluateOnlyEagerPromises : false;
    }

    private static int depthFromFrame(Frame callingFrame) {
        return callingFrame == null ? 0 : RArguments.getCall(callingFrame).getDepth() + 1;
    }

    private static RCaller parentFromFrame(Frame callingFrame) {
        return callingFrame == null ? null : RArguments.getCall(callingFrame);
    }

    public int getDepth() {
        return depth;
    }

    public RCaller getPrevious() {
        return previous;
    }

    public RCaller getLogicalParent() {
        if (payload instanceof LogicalParent) {
            return ((LogicalParent) payload).parent;
        } else {
            if (payload instanceof RCaller) {
                assert isPromise();
                return (RCaller) payload;
            } else {
                assert !isPromise();
                return previous;
            }
        }
    }

    public RCaller getLogicalParent(BranchProfile payloadProfile) {
        if (payload instanceof LogicalParent) {
            payloadProfile.enter();
            return ((LogicalParent) payload).parent;
        } else {
            if (payload instanceof RCaller) {
                assert isPromise();
                return (RCaller) payload;
            } else {
                assert !isPromise();
                return previous;
            }
        }
    }

    public RSyntaxElement getSyntaxNode() {
        assert payload != null;
        assert !isPromise();
        Object res = payload instanceof NonPromiseLogicalParent ? ((NonPromiseLogicalParent) payload).callNodeOrSupplier : payload;
        return res instanceof RSyntaxElement ? (RSyntaxElement) res : (RSyntaxElement) ((Supplier<?>) res).get();
    }

    public static boolean isValidCaller(RCaller caller) {
        return caller != null && caller.isValidCaller();
    }

    public boolean isValidCaller() {
        return payload != null;
    }

    /**
     * Promise evaluation frame is artificial frame (does not exist on the R level) that is created
     * to evaluate a promise in its context. This trick is also used for {@code eval},
     * {@code do.call} and similar and frames created by those builtins may also return {@code true}
     * from {@link #isPromise()}.
     *
     * Terminology: actual evaluation frame is a frame of the function that created the promise and
     * the frame in whose context the promise code should be evaluated.
     *
     * The artificial promise evaluation frame, marked by the {@link #isPromise()} flag, wraps the
     * actual evaluation frame in a way that locals are delegated to the actual evaluation frame,
     * but arguments array is altered. We cannot use the actual evaluation frame as is, because when
     * there is a function call inside the promise code, the new frame created for the invoked
     * function will get its {@link #previous} set to {@link RCaller} and {@code depth+1} of its
     * caller frame. By using wrapped frame for which we set different {@link #depth} than the
     * actual evaluation frame, we can set the {@link #depth} to the correct value, which is the
     * {@link #depth} of the code that initiated the promise evaluation.
     *
     * Moreover, if the promise code invokes a function, this function should be tricked into
     * thinking that its caller is the actual evaluation frame. Since this {@link RCaller} will be
     * used as {@link #previous} inside the frame created for the invoked function, we use
     * {@link #isPromise()} to find out this is artificial {@link RCaller} and we should follow the
     * {@link #getPrevious()} chain until we reach the actual evaluation frame and take the real
     * parent from there.
     */
    public boolean isPromise() {
        return (payload instanceof RCaller) || (payload instanceof PromiseLogicalParent);
    }

    public static void iterateCallers(RCaller start, Consumer<RCaller> consumer) {
        RCaller call = start;
        while (RCaller.isValidCaller(call)) {
            if (!call.isPromise()) {
                consumer.accept(call);
            }
            call = call.getPrevious();
        }
    }

    public static RCaller unwrapPrevious(RCaller callerIn) {
        RCaller caller = callerIn;
        while (caller != null && caller.isPromise()) {
            caller = caller.getPrevious();
        }
        return caller;
    }

    /**
     * @see #unwrapPromiseCaller(RCaller, UnwrapPromiseCallerProfile)
     */
    public static RCaller unwrapPromiseCaller(RCaller callerIn) {
        RCaller caller = callerIn;
        while (RCaller.isValidCaller(caller) && caller.isPromise()) {
            caller = caller.getLogicalParent();
        }
        return caller;
    }

    /**
     * If the given {@link RCaller} is stored in an artificial promise evaluation frame, then this
     * follows the {@link #payload} until it reaches the {@link RCaller} of the real frame where the
     * promise should be evaluated logically.
     */
    public static RCaller unwrapPromiseCaller(RCaller callerIn, UnwrapPromiseCallerProfile profile) {
        RCaller caller = callerIn;
        if (profile.firstPromiseProfile.profile(RCaller.isValidCaller(caller) && caller.isPromise())) {
            caller = caller.getLogicalParent(profile.sysParentProfile);
        }
        if (!RCaller.isValidCaller(caller) || !caller.isPromise()) {
            return caller;
        }
        profile.morePromisesProfile.enter();
        return unwrapPromiseCaller(caller);
    }

    /**
     * If this {@link RCaller} happens to be selected as a result of {@code parent.frame} or
     * similar, then this value (if not {@code null}) should be used as the resulting environment.
     */
    public static REnvironment unwrapSysParent(RCaller callerIn, UnwrapSysParentProfile profile) {
        // TODO: unit-test (only found in the wild in futile.logger tests)
        // TODO: use in sys.parent?
        RCaller caller = callerIn;
        while (profile.firstPromiseProfile.profile(RCaller.isValidCaller(caller) && caller.isPromise())) {
            if (caller.payload instanceof PromiseLogicalParent) {
                profile.sysParentProfile.enter();
                return ((PromiseLogicalParent) caller.payload).envOverride;
            } else {
                caller = (RCaller) caller.payload;
            }
        }
        return null;
    }

    /**
     * Given a caller, this method returns the closest non-promise (regular) caller or a promise
     * caller holding the "sys-parent" environment in the logical parent branch of the caller.
     */
    public static RCaller getRegularOrSysParentCaller(RCaller callerIn, UnwrapSysParentProfile profile) {
        RCaller caller = callerIn;
        while (profile.firstPromiseProfile.profile(RCaller.isValidCaller(caller) && caller.isPromise())) {
            if (caller.payload instanceof PromiseLogicalParent) {
                profile.sysParentProfile.enter();
                return caller;
            } else {
                caller = (RCaller) caller.payload;
            }
        }
        return caller;
    }

    /**
     * If the {@link RCaller} instance {@link #isPromise()}, then it may have explicitly set the
     * "sys parent" environment, which overrides the result {@code parent.frame} would return if the
     * traversing of the call stack ends up selecting this {@link RCaller} as the result. I.e. in
     * such a case, the "sys parent" is not the parent of this {@link RCaller}, instead, it is an
     * alternative result that should be used instead of this {@link RCaller} if the next
     * {@link RCaller} asks for a {@code parent.frame}.
     *
     * There is a difference between how the stack is traversed if the
     * {@link PromiseLogicalParent#envOverride} of {@link RCaller} for a promise frame is an
     * artificial environment (it doesn't really have its place on the call stack) or environment of
     * a function that is on the call stack. That is why we further distinguish this situation here.
     *
     * NOTE: there is one potential issue and one potential optimization:
     *
     * The issue is with an environment of a function that is no longer on the call stack. Should we
     * treat it as "function sys parent"? We may have to store in RCaller whether the function has
     * terminated yet, i.e. is not on the call stack anymore? We can also optimize this and store
     * the RCaller of such function instead of its materialized frame.
     *
     * Opportunity: for sys parents representing environments of a function, we do not have to
     * materialize the frame and store it here, we could just use it's {@link RCaller}, but what to
     * do if the function is popped off the stack?
     *
     * See {@code ParentFrame} built in for more details.
     */
    public boolean isNonFunctionSysParent() {
        if (payload instanceof PromiseLogicalParent) {
            PromiseLogicalParent promiseLogicalParent = (PromiseLogicalParent) payload;
            return !(promiseLogicalParent.envOverride instanceof REnvironment.Function);
        }
        return false;
    }

    public boolean hasEnvOverride() {
        return payload instanceof PromiseLogicalParent && ((PromiseLogicalParent) payload).envOverride != null;
    }

    public REnvironment getEnvOverride() {
        if (payload instanceof PromiseLogicalParent) {
            return ((PromiseLogicalParent) payload).envOverride;
        }
        return null;
    }

    public boolean hasParentOverridden() {
        return payload instanceof NonPromiseLogicalParent && ((NonPromiseLogicalParent) payload).parent != null;
    }

    public static RCaller createInvalid(Frame callingFrame) {
        return new RCaller(callingFrame, null);
    }

    public static RCaller createInvalid(Frame callingFrame, RCaller previous) {
        return new RCaller(depthFromFrame(callingFrame), previous, null);
    }

    public static RCaller create(Frame callingFrame, RSyntaxElement node) {
        assert node != null;
        return new RCaller(callingFrame, node);
    }

    public static RCaller create(Frame callingFrame, RCaller previous, RSyntaxElement node) {
        assert node != null;
        return new RCaller(depthFromFrame(callingFrame), previous, node);
    }

    public static RCaller create(Frame callingFrame, Supplier<RSyntaxElement> supplier) {
        assert supplier != null;
        return new RCaller(callingFrame, supplier);
    }

    public static RCaller create(int depth, RCaller previous, Object payload) {
        assert payload != null;
        return new RCaller(depth, previous, payload);
    }

    public static RCaller create(Frame callingFrame, RCaller previous, Supplier<RSyntaxElement> supplier) {
        assert supplier != null;
        return new RCaller(depthFromFrame(callingFrame), previous, supplier);
    }

    /**
     * Creates {@link RCaller} object for a promise evaluation.
     *
     * @see #isPromise()
     *
     * @param originalCaller {@link RCaller} object inside the frame where the promise should be
     *            evaluated.
     * @param currentCaller the current {@link RCaller} instance where the promise is actually being
     *            evaluated, will be used as the pointer to the previous {@link RCaller}.
     */
    public static RCaller createForPromise(RCaller originalCaller, RCaller currentCaller) {
        int newDepth = currentCaller == null ? 0 : currentCaller.getDepth();
        return new RCaller(newDepth, currentCaller, originalCaller);
    }

    /**
     * Creates {@link RCaller} object for a promise evaluation.
     *
     * @see #isPromise()
     *
     * @param originalCaller the logical parent of the promise.
     * @param sysParent environment where the promise should be evaluated. Note that is will become
     *            the logical parent frame should the promise code invoke some function that uses
     *            the {@code parent.frame} R function, but not the logical parent frame of the
     *            promise itself, e.g. {@code eval(quote(parent.frame())}, that will still parent of
     *            the frame pointed at by the {@code originalCaller}.
     * @param currentCaller the current {@link RCaller} instance where the promise is actually being
     *            evaluated, will be used as the pointer to the previous {@link RCaller}.
     */
    public static RCaller createForPromise(RCaller originalCaller, REnvironment sysParent, RCaller currentCaller) {
        int newDepth = currentCaller == null ? 0 : currentCaller.getDepth();
        return new RCaller(newDepth, currentCaller, new PromiseLogicalParent(sysParent, originalCaller));
    }

    /**
     * Creates {@link RCaller} for evaluation of the generic method. The logical parent of such
     * method should be the caller of the "dispatch" function (the function that calls
     * {@code UseMethod("xyz")}), but we still need to keep the actual parent.
     *
     * @param dispatchingCaller The logical parent.
     * @param call The syntax of the call, can be supplier of the result.
     * @param currentCaller The current {@link RCaller}.
     */
    public static RCaller createForGenericFunctionCall(RCaller dispatchingCaller, Object call, RCaller currentCaller) {
        return new RCaller(currentCaller.getDepth() + 1, currentCaller, new NonPromiseLogicalParent(dispatchingCaller, call));
    }

    public boolean getVisibility() {
        return visibility;
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }

    public RCaller withLogicalParent(RCaller logicalParent) {
        assert !isPromise();
        return new RCaller(this.depth, this.previous, new NonPromiseLogicalParent(logicalParent, this.payload));
    }

    public boolean evaluateOnlyEagerPromises() {
        return evaluateOnlyEagerPromises;
    }

    public void setEvaluateOnlyEagerPromises(boolean evaluateOnlyEagerPromises) {
        this.evaluateOnlyEagerPromises = evaluateOnlyEagerPromises;
    }

    public void checkEagerPromiseOnly() {
        if (evaluateOnlyEagerPromises()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new CannotOptimizePromise();
        }
    }

    /**
     * An instance of one of two subclasses of this class is held in the {@link RCaller#payload}
     * field. Let's call such an {@link RCaller} instance the owner of this instance. The
     * {@link #parent} field contains the logical parent of the owner.
     */
    private abstract static class LogicalParent {
        final RCaller parent;

        LogicalParent(RCaller parent) {
            this.parent = parent;
        }
    }

    /**
     * This class is used for promise callers only. The field {@link #envOverride} holds the
     * environment that should replace the result of {@code parent.frame}, when the owner is
     * selected as the result.
     */
    private static final class PromiseLogicalParent extends LogicalParent {
        /**
         * See {@link RCaller#isNonFunctionSysParent()}.
         */
        final REnvironment envOverride;

        PromiseLogicalParent(REnvironment envOverride, RCaller parent) {
            super(parent);
            this.envOverride = envOverride;
        }
    }

    /**
     * This class is used for non-promise callers only. The field {@link #callNodeOrSupplier} holds
     * the syntax of the call or its supplier {@code Supplier} (like {@link RCaller#payload} for
     * non-promise {@link RCaller}s).
     */
    private static final class NonPromiseLogicalParent extends LogicalParent {
        /**
         * Field that holds the call AST node {@link RSyntaxNode} or its supplier {@code Supplier}.
         */
        final Object callNodeOrSupplier;

        NonPromiseLogicalParent(RCaller parent, Object callNodeOrSupplier) {
            super(parent);
            assert callNodeOrSupplier == null || callNodeOrSupplier instanceof Supplier<?> || callNodeOrSupplier instanceof RSyntaxNode : callNodeOrSupplier;
            this.callNodeOrSupplier = callNodeOrSupplier;
        }
    }

    public static final class UnwrapSysParentProfile {
        private final ConditionProfile firstPromiseProfile;
        private final BranchProfile sysParentProfile;

        public UnwrapSysParentProfile() {
            firstPromiseProfile = ConditionProfile.createBinaryProfile();
            sysParentProfile = BranchProfile.create();
        }
    }

    public static final class UnwrapPromiseCallerProfile {
        private final ConditionProfile firstPromiseProfile;
        private final BranchProfile sysParentProfile;
        private final BranchProfile morePromisesProfile;

        public UnwrapPromiseCallerProfile() {
            firstPromiseProfile = ConditionProfile.createBinaryProfile();
            morePromisesProfile = BranchProfile.create();
            sysParentProfile = BranchProfile.create();
        }
    }
}
