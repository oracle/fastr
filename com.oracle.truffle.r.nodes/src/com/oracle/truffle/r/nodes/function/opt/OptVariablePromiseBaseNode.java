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
package com.oracle.truffle.r.nodes.function.opt;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.r.nodes.access.FrameSlotNode;
import com.oracle.truffle.r.nodes.access.variables.LocalReadVariableNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.function.PromiseNode;
import com.oracle.truffle.r.nodes.function.SubstituteVirtualFrame;
import com.oracle.truffle.r.runtime.PromiseEvalFrame;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RPromise.EagerFeedback;
import com.oracle.truffle.r.runtime.data.RPromise.RPromiseFactory;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

public abstract class OptVariablePromiseBaseNode extends PromiseNode implements EagerFeedback {
    private final ReadVariableNode originalRvn;
    @Child private FrameSlotNode frameSlotNode;
    @Child private RNode fallback = null;
    @Child private LocalReadVariableNode readNode;
    private final int wrapIndex;

    public OptVariablePromiseBaseNode(RPromiseFactory factory, ReadVariableNode rvn, int wrapIndex) {
        super(factory);
        // Should be caught by optimization check
        assert !rvn.isForceForTypeCheck() && rvn.getMode() == RType.Any;
        this.originalRvn = rvn;
        this.frameSlotNode = FrameSlotNode.create(rvn.getIdentifier(), false);
        this.readNode = LocalReadVariableNode.create(rvn.getIdentifier(), false);
        this.wrapIndex = wrapIndex;
    }

    @Override
    public RSyntaxNode getPromiseExpr() {
        return originalRvn;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (frame instanceof PromiseEvalFrame) {
            frame = SubstituteVirtualFrame.create(((PromiseEvalFrame) frame).getOriginalFrame());
        }
        // If the frame slot we're looking for is not present yet, wait for it!
        if (!frameSlotNode.hasValue(frame)) {
            // We don't want to rewrite, as the the frame slot might show up later on (after 1.
            // execution), which might still be worth to optimize
            return executeFallback(frame);
        }
        FrameSlot slot = frameSlotNode.executeFrameSlot(frame);

        // Check if we may apply eager evaluation on this frame slot
        Assumption notChangedNonLocally = FrameSlotChangeMonitor.getNotChangedNonLocallyAssumption(slot);
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
        int frameId = RArguments.getDepth(frame);
        if (result instanceof RPromise) {
            return factory.createPromisedPromise((RPromise) result, notChangedNonLocally, frameId, this);
        } else {
            return factory.createEagerSuppliedPromise(result, notChangedNonLocally, frameId, this, wrapIndex);
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
        return originalRvn;
    }
}
