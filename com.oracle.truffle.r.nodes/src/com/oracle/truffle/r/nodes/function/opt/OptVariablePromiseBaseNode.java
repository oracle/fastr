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
        this.frameSlotNode = FrameSlotNode.create(rvn.getSymbolName(), false);
        this.readNode = UnResolvedReadLocalVariableNode.create(rvn.getSymbol(), rvn.getMode(), rvn.getCopyValue(), rvn.getReadMissing());
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