/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Supplier;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Represents the caller of a function and stored in {@link RArguments}. A value of this type never
 * appears in a Truffle execution. Caller remembers its parent caller and frame number as described
 * in {@code sys.parent} R function documentation: frames are numbered from 0 (global environment).
 * Parent does not have to have the frame with number one less, e.g. with do.call(fun, args, envir)
 * when fun asks for parent, it should get 'envir', moreover, when evaluating promises parent frame
 * and frame with number one less are typically also not the same frames. See also builtins in
 * {@code FrameFunctions} for more details.
 *
 * NOTE: It is important to create new caller instances for each stack frame, so that
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
 * {@link com.oracle.truffle.api.CallTarget} and calls it with 'internal frame', in which case the
 * 'internal frame' appears in Truffle frames iteration.
 *
 * @see RArguments
 */
public final class RCaller {

    public static final RCaller topLevel = RCaller.createInvalid(null);

    /**
     * Determines the actual position of the corresponding frame on the execution call stack. When
     * one follows the {@link RCaller#parent} chain, then the depth is not always decreasing by only
     * one, the reason are promises, which may be evaluated somewhere deep down the call stack, but
     * their parent call frame from R prespective could be much higher up the actual execution call
     * stack.
     *
     * Note: this is depth of the frame where this {@link RCaller} is stored, not the depth of the
     * parent.
     */
    private final int depth;
    private boolean visibility;
    private final RCaller parent;

    /**
     * The payload can be
     * <ul>
     * <li>{@code null} for top level {@link RCaller} (~global environment)</li>
     * <li>{@link RSyntaxNode}</li>
     * <li>{@link Supplier} of the {@link RSyntaxNode}</li>
     * <li>{@link RCaller} (which marks promise evaluation frames, see {@link #isPromise()})</li>
     * <li>{@link REnvironment} (which marks promise evaluation frame with explicit "sys parent",
     * see {@link #hasSysParent()})</li>
     * </ul>
     * 
     * If the function was invoked via regular call node, then the syntax can be that call node
     * (RSyntaxNode case), if the function was invoked by other means and we do not have the actual
     * syntax for the invocation, we only provide it lazily via Supplier, so that we do not have to
     * always construct the AST nodes. {@link RCaller} with other types of {@link #payload} are used
     * for promise frames or other artificial situations.
     *
     * Note on promise evaluation frame with explicit "sys parent": the "sys parent" frame does not
     * have to be explicit if the environment has valid {@link RCaller} in its arguments array,
     * which is the case if the environment represents a frame of a function that is on the call
     * stack. If the environment does not come from currently evaluated function (e.g. manually
     * constructed), then we cannot use {@link RCaller} to identify it, since there's no
     * {@link #depth} that would point to the corresponding frame.
     */
    private final Object payload;

    private RCaller(Frame callingFrame, Object payload) {
        this(depthFromFrame(callingFrame), parentFromFrame(callingFrame), payload);
    }

    private RCaller(int depth, RCaller parent, Object payload) {
        assert payload == null || payload instanceof Supplier<?> || payload instanceof RCaller || payload instanceof REnvironment || payload instanceof RSyntaxNode : payload;
        this.depth = depth;
        this.parent = parent;
        this.payload = payload;
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

    public RCaller getParent() {
        return parent;
    }

    public RSyntaxElement getSyntaxNode() {
        assert payload != null && !(payload instanceof RCaller) : payload == null ? "null RCaller" : "promise RCaller";
        return payload instanceof RSyntaxElement ? (RSyntaxElement) payload : (RSyntaxElement) ((Supplier<?>) payload).get();
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
     * function will get its {@link #parent} set to {@link RCaller} and {@code depth+1} of its
     * caller frame. By using wrapped frame for which we set different {@link #depth} than the
     * actual evaluation frame, we can set the {@link #depth} to the correct value, which is the
     * {@link #depth} of the code that initiated the promise evaluation.
     *
     * Moreover, if the promise code invokes a function, this function should be tricked into
     * thinking that its caller is the actual evaluation frame. Since this {@link RCaller} will be
     * used as {@link #parent} inside the frame created for the invoked function, we use
     * {@link #isPromise()} to find out this is artificial {@link RCaller} and we should follow the
     * {@link #getParent()} chain until we reach the actual evaluation frame and take the real
     * parent from there.
     */
    public boolean isPromise() {
        return (payload instanceof RCaller) || (payload instanceof REnvironment);
    }

    /**
     * If the {@link RCaller} instance {@link #isPromise()}, then it may have explicitly set
     * "sys parent", which is what {@code parent.frame} should return. See {@code ParentFrame} built
     * in for details.
     */
    public boolean hasSysParent() {
        return payload instanceof REnvironment;
    }

    /**
     * {@link RCaller}s for actual promise store the original {@link RCaller} (of the frame that
     * invoked the promise) and in such case this method is a getter for it. You should check
     * {@link #isPromise()} and {@link #hasSysParent()} before accessing promise caller.
     */
    public RCaller getPromiseCaller() {
        return (RCaller) payload;
    }

    public REnvironment getSysParent() {
        assert payload instanceof REnvironment;
        return (REnvironment) payload;
    }

    public static RCaller createInvalid(Frame callingFrame) {
        return new RCaller(callingFrame, null);
    }

    public static RCaller createInvalid(Frame callingFrame, RCaller parent) {
        return new RCaller(depthFromFrame(callingFrame), parent, null);
    }

    public static RCaller create(Frame callingFrame, RSyntaxElement node) {
        assert node != null;
        return new RCaller(callingFrame, node);
    }

    public static RCaller create(Frame callingFrame, RCaller parent, RSyntaxElement node) {
        assert node != null;
        return new RCaller(depthFromFrame(callingFrame), parent, node);
    }

    public static RCaller create(Frame callingFrame, Supplier<RSyntaxElement> supplier) {
        assert supplier != null;
        return new RCaller(callingFrame, supplier);
    }

    public static RCaller create(int depth, RCaller parent, Object payload) {
        assert payload != null;
        return new RCaller(depth, parent, payload);
    }

    public static RCaller create(Frame callingFrame, RCaller parent, Supplier<RSyntaxElement> supplier) {
        assert supplier != null;
        return new RCaller(depthFromFrame(callingFrame), parent, supplier);
    }

    public static RCaller createForPromise(RCaller originalCaller, Frame frame) {
        int newDepth = frame == null ? 0 : RArguments.getDepth(frame);
        RCaller originalCall = frame == null ? null : RArguments.getCall(frame);
        return new RCaller(newDepth, originalCaller, originalCall);
    }

    public static RCaller createForPromise(RCaller originalCaller, Frame frame, REnvironment sysParent) {
        int newDepth = frame == null ? 0 : RArguments.getDepth(frame);
        return new RCaller(newDepth, originalCaller, sysParent);
    }

    public boolean getVisibility() {
        return visibility;
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }
}
