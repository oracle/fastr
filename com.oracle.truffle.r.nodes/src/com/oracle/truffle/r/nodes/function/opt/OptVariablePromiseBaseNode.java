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
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.r.nodes.access.FrameIndexNode;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.function.PromiseNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.RCaller.UnwrapPromiseCallerProfile;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.EagerFeedback;
import com.oracle.truffle.r.runtime.data.RPromise.RPromiseFactory;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Creates an optimized promise for a lookup of a symbol.
 *
 * This factory-node gets executed when a function is being called and therefore it takes place
 * inside the frame of the caller. However, there may be an issue when the promise reads the value
 * of a symbol whose value can change between now (the function is being called) and the time when
 * the promise is actually evaluated (the argument is accessed for the first time inside the
 * callee). Example:
 *
 * <pre>
 *     foo <- function(x) { assign('a', 42, parent.frame()); return(x) }
 *     a <- 22
 *     foo(a) # must give 42
 * </pre>
 *
 * The problem is solved by getting
 * {@link FrameSlotChangeMonitor#getNotChangedNonLocallyAssumption(Frame, int)} for the given frame
 * slot and putting that into the promise object. The promise object, when asked for its value
 * checks this assumption first, which happens in
 * {@link com.oracle.truffle.r.nodes.function.PromiseHelperNode}.
 *
 * The implementors override functions that handle the fallback case when the assumption is
 * invalidated.
 */
public abstract class OptVariablePromiseBaseNode extends PromiseNode implements EagerFeedback {
    private final UnwrapPromiseCallerProfile unwrapCallerProfile = new UnwrapPromiseCallerProfile();
    private final RSyntaxLookup originalRvn;
    @Child private FrameIndexNode frameIndexNode;
    @Child private RNode fallback = null;
    @Child private LocalReadVariableNode readNode;
    private final int wrapIndex;

    public OptVariablePromiseBaseNode(RPromiseFactory factory, RSyntaxLookup lookup, int wrapIndex) {
        super(factory);
        // Should be caught by optimization check
        assert !lookup.isFunctionLookup();
        this.originalRvn = lookup;
        this.frameIndexNode = FrameIndexNode.create(lookup.getIdentifier(), false);
        this.readNode = LocalReadVariableNode.create(lookup.getIdentifier(), false);
        this.wrapIndex = wrapIndex;
    }

    @Override
    public RSyntaxNode getPromiseExpr() {
        return (RSyntaxNode) originalRvn;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // If the frame slot we're looking for is not present yet, wait for it!
        if (!frameIndexNode.hasValue(frame)) {
            // We don't want to rewrite, as the the frame slot might show up later on (after 1.
            // execution), which might still be worth to optimize
            return executeFallback(frame);
        }
        int frameIndex = frameIndexNode.executeFrameIndex(frame);

        // Check if we may apply eager evaluation on this frame slot
        Assumption notChangedNonLocally = FrameSlotChangeMonitor.getNotChangedNonLocallyAssumption(frame, frameIndex);
        try {
            notChangedNonLocally.check();
        } catch (InvalidAssumptionException e) {
            // Cannot apply optimizations, as the value to it got invalidated
            return rewriteToAndExecuteFallback(frame);
        }

        // Execute eagerly
        // This reads only locally, and frameSlotNode.hasValue that there is the proper
        // frameSlot there.
        Object result = readNode.execute(frame);
        if (result == null) {
            // Cannot apply optimizations, as the value was removed
            return rewriteToAndExecuteFallback(frame);
        }

        // Create EagerPromise with the eagerly evaluated value under the assumption that the
        // value won't be altered until 1. read
        RCaller call = RCaller.unwrapPromiseCaller(RArguments.getCall(frame), unwrapCallerProfile);

        MaterializedFrame execFrame = null;
        if (CompilerDirectives.inInterpreter()) {
            execFrame = frame.materialize();
        }

        if (result instanceof RPromise) {
            return factory.createPromisedPromise((RPromise) result, notChangedNonLocally, call, this, execFrame);
        } else {
            return factory.createEagerSuppliedPromise(result, notChangedNonLocally, call, this, wrapIndex, execFrame);
        }
    }

    protected abstract RNode createFallback();

    protected Object executeFallback(VirtualFrame frame) {
        return checkFallback().execute(frame);
    }

    protected Object rewriteToAndExecuteFallback(VirtualFrame frame) {
        return rewriteToFallback().execute(frame);
    }

    protected RNode rewriteToFallback() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        checkFallback();
        return replace(fallback);
    }

    private RNode checkFallback() {
        if (fallback == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            insert(fallback = createFallback());
        }
        return fallback;
    }

    @Override
    public RSyntaxNode getRSyntaxNode() {
        return (RSyntaxNode) originalRvn;
    }
}
