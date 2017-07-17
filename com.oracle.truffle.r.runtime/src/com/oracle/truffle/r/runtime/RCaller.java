/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

import java.util.function.Supplier;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;

/**
 * Represents the caller of a function and stored in {@link RArguments}. A value of this type never
 * appears in a Truffle execution.
 */
public final class RCaller {

    public static final RCaller topLevel = RCaller.createInvalid(null);

    /**
     * Determines the actual position of the corresponding frame on the execution call stack. When
     * one follows the {@link RCaller#parent} chain, then the depth is not always decreasing by only
     * one, the reason are promises, which may be evaluated somewhere deep down the call stack, but
     * their parent call frame from R prespective could be much higher up the actual execution call
     * stack.
     */
    private final int depth;
    private boolean visibility;
    private final RCaller parent;
    /**
     * The payload can be an RSyntaxNode, a {@link Supplier}, or an {@link RCaller} (which marks
     * promise evaluation frames).
     */
    private final Object payload;
    /**
     * Marks those callers whose parent should not be taken into account when iterating R level
     * frames using e.g. {@code parent.frame()}. This is the case for function invoked through
     * {@code do.call} -- R pretends that they were called by the caller of {@code do.call} so that
     * code like {@code eval(formula, parent.frame(2))} gives the same results regardless of whether
     * the function was invoked directly or through {@code do.call}.
     */
    private final boolean parentIsInternal;

    private RCaller(Frame callingFrame, Object nodeOrSupplier, boolean parentIsInternal) {
        this.depth = depthFromFrame(callingFrame);
        this.parent = parentFromFrame(callingFrame);
        this.payload = nodeOrSupplier;
        this.parentIsInternal = parentIsInternal;
    }

    private static int depthFromFrame(Frame callingFrame) {
        return callingFrame == null ? 0 : RArguments.getCall(callingFrame).getDepth() + 1;
    }

    private static RCaller parentFromFrame(Frame callingFrame) {
        return callingFrame == null ? null : RArguments.getCall(callingFrame);
    }

    private RCaller(int depth, RCaller parent, Object nodeOrSupplier, boolean parentIsInternal) {
        this.depth = depth;
        this.parent = parent;
        this.payload = nodeOrSupplier;
        this.parentIsInternal = parentIsInternal;
    }

    public RCaller withInternalParent() {
        return new RCaller(depth, parent, payload, true);
    }

    public int getDepth() {
        return depth;
    }

    public RCaller getParent() {
        return parent;
    }

    public boolean hasInternalParent() {
        return parentIsInternal;
    }

    public RSyntaxElement getSyntaxNode() {
        assert payload != null && !(payload instanceof RCaller) : payload == null ? "null RCaller" : "promise RCaller";
        return payload instanceof RSyntaxElement ? (RSyntaxElement) payload : (RSyntaxElement) ((Supplier<?>) payload).get();
    }

    public boolean isValidCaller() {
        return payload != null;
    }

    public boolean isPromise() {
        return payload instanceof RCaller;
    }

    public RCaller getPromiseOriginalCall() {
        return (RCaller) payload;
    }

    public static RCaller createInvalid(Frame callingFrame) {
        return new RCaller(callingFrame, null, false);
    }

    public static RCaller createInvalid(Frame callingFrame, RCaller parent) {
        return new RCaller(depthFromFrame(callingFrame), parent, null, false);
    }

    public static RCaller create(Frame callingFrame, RSyntaxElement node) {
        assert node != null;
        return new RCaller(callingFrame, node, false);
    }

    public static RCaller create(Frame callingFrame, RCaller parent, RSyntaxElement node) {
        assert node != null;
        return new RCaller(depthFromFrame(callingFrame), parent, node, false);
    }

    public static RCaller createWithInternalParent(Frame callingFrame, Supplier<RSyntaxElement> supplier) {
        assert supplier != null;
        return new RCaller(callingFrame, supplier, true);
    }

    public static RCaller create(Frame callingFrame, Supplier<RSyntaxElement> supplier) {
        assert supplier != null;
        return new RCaller(callingFrame, supplier, false);
    }

    public static RCaller create(Frame callingFrame, RCaller parent, Supplier<RSyntaxElement> supplier) {
        assert supplier != null;
        return new RCaller(depthFromFrame(callingFrame), parent, supplier, false);
    }

    public static RCaller createForPromise(RCaller originalCaller, Frame frame) {
        int newDepth = frame == null ? 0 : RArguments.getDepth(frame);
        RCaller originalCall = frame == null ? null : RArguments.getCall(frame);
        return new RCaller(newDepth, originalCaller, originalCall, false);
    }

    public boolean getVisibility() {
        return visibility;
    }

    public void setVisibility(boolean visibility) {
        this.visibility = visibility;
    }
}
