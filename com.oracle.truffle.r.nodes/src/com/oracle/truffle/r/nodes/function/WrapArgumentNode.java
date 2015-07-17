/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * A {@link WrapArgumentNode} is used to wrap all arguments to function calls to implement correct
 * copy semantics for vectors. As such it is not really a syntax node, but it is created during
 * parsing and therefore forms part of the syntactic backbone.
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
        this.argPushStateNode = ArgumentStatePushNodeGen.create(index, null);
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

    public static WrapArgumentNode create(int index) {
        return new WrapArgumentNode(null, true, index);
    }

    public static RSyntaxNode create(RNode operand, boolean modeChange, int index) {
        if (operand instanceof WrapArgumentNode || operand instanceof ConstantNode) {
            return (RSyntaxNode) operand;
        } else {
            WrapArgumentNode wan = new WrapArgumentNode(operand, modeChange, index);
            wan.assignSourceSection(operand.getSourceSection());
            return wan;
        }
    }

    @Override
    public RSyntaxNode substitute(REnvironment env) {
        RNode sub = RSyntaxNode.cast(getOperand()).substitute(env).asRNode();
        if (sub instanceof RASTUtils.DotsNode) {
            return (RASTUtils.DotsNode) sub;
        } else {
            return create(sub, modeChange, index);
        }
    }
}
