/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * A {@link WrapArgumentNode} is used to wrap all arguments to function calls to implement correct
 * copy semantics for vectors.
 *
 */
public final class WrapArgumentNode extends WrapArgumentBaseNode {

    private final boolean modeChange;
    private final int index;

    @Child private ArgumentStatePush argPushStateNode;

    private final BranchProfile refCounted = BranchProfile.create();

    private WrapArgumentNode(RNode operand, boolean modeChange, int index) {
        super(operand, modeChange);
        this.modeChange = modeChange;
        this.index = index;
        this.argPushStateNode = modeChange ? ArgumentStatePushNodeGen.create(index) : null;
    }

    public boolean modeChange() {
        return modeChange;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public NodeCost getCost() {
        return modeChange ? NodeCost.MONOMORPHIC : NodeCost.NONE;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        assert operand != null;
        Object result = operand.execute(frame);
        return execute(frame, result);
    }

    public Object execute(VirtualFrame frame, Object result) {
        if (modeChange) {
            RShareable rShareable = getShareable(result);
            if (rShareable != null) {
                shareable.enter();
                argPushStateNode.executeObject(frame, rShareable);
            } else if (argPushStateNode.refCounted()) {
                refCounted.enter();
                argPushStateNode.executeObject(frame, RNull.instance);
            }
        }
        return result;
    }

    @Override
    public byte executeByte(VirtualFrame frame) throws UnexpectedResultException {
        return operand.executeByte(frame);
    }

    @Override
    public int executeInteger(VirtualFrame frame) throws UnexpectedResultException {
        return operand.executeInteger(frame);
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return operand.executeDouble(frame);
    }

    @Override
    public RMissing executeMissing(VirtualFrame frame) throws UnexpectedResultException {
        return operand.executeMissing(frame);
    }

    @Override
    public RNull executeNull(VirtualFrame frame) throws UnexpectedResultException {
        return operand.executeNull(frame);
    }

    static WrapArgumentNode create(int index) {
        return new WrapArgumentNode(null, true, index);
    }

    static RNode create(RNode operand, boolean modeChange, int index) {
        if (operand instanceof WrapArgumentNode || operand instanceof ConstantNode) {
            return operand;
        } else {
            WrapArgumentNode wan = new WrapArgumentNode(operand, modeChange, index);
            return wan;
        }
    }
}
