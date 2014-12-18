/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.access.ReadVariableNode.UnResolvedReadLocalVariableNode;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.EagerFeedback;
import com.oracle.truffle.r.runtime.data.RPromise.RPromiseFactory;
import com.oracle.truffle.r.runtime.env.frame.*;

public abstract class OptVariablePromiseBaseNode extends PromiseNode implements EagerFeedback {
    protected final ReadVariableNode originalRvn;
    @Child private FrameSlotNode frameSlotNode;
    @Child private RNode fallback = null;
    @Child private ReadVariableNode readNode;

    public OptVariablePromiseBaseNode(RPromiseFactory factory, ReadVariableNode rvn) {
        super(factory);
        assert rvn.getForcePromise() == false;  // Should be caught by optimization check
        this.originalRvn = rvn;
        this.frameSlotNode = FrameSlotNode.create(rvn.getName(), false);
        this.readNode = UnResolvedReadLocalVariableNode.create(rvn.getName(), rvn.getMode(), rvn.getCopyValue(), rvn.getReadMissing());
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // If the frame slot we're looking for is not present yet, wait for it!
        if (!frameSlotNode.hasValue(frame)) {
            // We don't want to rewrite, as the the frame slot might show up later on (after 1.
            // execution), which might still be worth to optimize
            return executeFallback(frame);
        }
        FrameSlot slot = frameSlotNode.executeFrameSlot(frame);

        // Check if we may apply eager evaluation on this frame slot
        Assumption notChangedNonLocally = FrameSlotChangeMonitor.getMonitor(slot);
        if (!notChangedNonLocally.isValid()) {
            // Cannot apply optimizations, as the value to it got invalidated
            return rewriteToAndExecuteFallback(frame);
        }

        // Execute eagerly
        Object result = null;
        try {
            // This reads only locally, and frameSlotNode.hasValue that there is the proper
            // frameSlot there.
            result = readNode.execute(frame);
        } catch (Throwable t) {
            // If any error occurred, we cannot be sure what to do. Instead of trying to be
            // clever, we conservatively rewrite to default PromisedNode.
            return rewriteToAndExecuteFallback(frame);
        }

        // Create EagerPromise with the eagerly evaluated value under the assumption that the
        // value won't be altered until 1. read
        int frameId = RArguments.getDepth(frame);
        if (result instanceof RPromise) {
            return factory.createPromisedPromise((RPromise) result, notChangedNonLocally, frameId, this);
        } else {
            return factory.createEagerSuppliedPromise(result, notChangedNonLocally, frameId, this);
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
}
