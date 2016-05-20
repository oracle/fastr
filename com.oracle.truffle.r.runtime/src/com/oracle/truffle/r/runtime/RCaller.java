/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Represents the caller of a function and stored in {@link RArguments}. A value of this type never
 * appears in a Truffle execution.
 */
public final class RCaller {

    private static final Object PROMISE_MARKER = new Object();

    private final int depth;
    private final RCaller parent;
    /**
     * payload can be an RSyntaxNode, a {@link Supplier}, or an PROMISE_MARKER.
     */
    private final Object payload;

    private RCaller(Frame callingFrame, Object nodeOrSupplier) {
        this.depth = depthFromFrame(callingFrame);
        this.parent = parentFromFrame(callingFrame);
        this.payload = nodeOrSupplier;
        if (parent == null || payload == null) {
            System.console();
        }
    }

    private static int depthFromFrame(Frame callingFrame) {
        return callingFrame == null ? 0 : RArguments.getCall(callingFrame).getDepth() + 1;
    }

    private static RCaller parentFromFrame(Frame callingFrame) {
        return callingFrame == null ? null : RArguments.getCall(callingFrame);
    }

    private RCaller(int depth, RCaller parent, Object nodeOrSupplier) {
        this.depth = depth;
        this.parent = parent;
        this.payload = nodeOrSupplier;
        if (parent == null || payload == null) {
            System.console();
        }
    }

    public int getDepth() {
        return depth;
    }

    public RCaller getParent() {
        return parent;
    }

    public RSyntaxNode getSyntaxNode() {
        assert payload != null && payload != PROMISE_MARKER : payload == null ? "null RCaller" : "promise RCaller";
        return payload instanceof RSyntaxNode ? (RSyntaxNode) payload : (RSyntaxNode) ((Supplier<?>) payload).get();
    }

    public boolean isValidCaller() {
        return payload != null;
    }

    public boolean isPromise() {
        return payload == PROMISE_MARKER;
    }

    public static RCaller createInvalid(Frame callingFrame) {
        return new RCaller(callingFrame, null);
    }

    public static RCaller createInvalid(Frame callingFrame, RCaller parent) {
        return new RCaller(depthFromFrame(callingFrame), parent, null);
    }

    public static RCaller create(Frame callingFrame, RSyntaxNode node) {
        assert node != null;
        return new RCaller(callingFrame, node);
    }

    public static RCaller create(Frame callingFrame, Supplier<RSyntaxNode> supplier) {
        assert supplier != null;
        return new RCaller(callingFrame, supplier);
    }

    public static RCaller create(Frame callingFrame, RCaller parent, Supplier<RSyntaxNode> supplier) {
        assert supplier != null;
        return new RCaller(depthFromFrame(callingFrame), parent, supplier);
    }

    public static RCaller createForPromise(RCaller original, int newDepth) {
        return new RCaller(newDepth, original, PROMISE_MARKER);
    }
}
