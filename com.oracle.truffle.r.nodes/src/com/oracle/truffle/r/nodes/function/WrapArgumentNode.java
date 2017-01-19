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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * A {@link WrapArgumentNode} is used to wrap all arguments to function calls to implement correct
 * copy semantics for vectors.
 *
 */
public final class WrapArgumentNode extends WrapArgumentBaseNode {

    private final int index;

    @Child private ArgumentStatePush argPushStateNode;

    private WrapArgumentNode(RNode operand, int index) {
        super(operand);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    @Override
    protected Object handleShareable(VirtualFrame frame, RSharingAttributeStorage shareable) {
        if (argPushStateNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            argPushStateNode = insert(ArgumentStatePushNodeGen.create(index));
        }
        argPushStateNode.executeObject(frame, shareable);
        return shareable;
    }

    @Override
    public byte executeByte(VirtualFrame frame) throws UnexpectedResultException {
        try {
            return operand.executeByte(frame);
        } catch (UnexpectedResultException e) {
            throw new UnexpectedResultException(execute(frame, e.getResult()));
        }
    }

    @Override
    public int executeInteger(VirtualFrame frame) throws UnexpectedResultException {
        try {
            return operand.executeInteger(frame);
        } catch (UnexpectedResultException e) {
            throw new UnexpectedResultException(execute(frame, e.getResult()));
        }
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        try {
            return operand.executeDouble(frame);
        } catch (UnexpectedResultException e) {
            throw new UnexpectedResultException(execute(frame, e.getResult()));
        }
    }

    @Override
    public RMissing executeMissing(VirtualFrame frame) throws UnexpectedResultException {
        try {
            return operand.executeMissing(frame);
        } catch (UnexpectedResultException e) {
            throw new UnexpectedResultException(execute(frame, e.getResult()));
        }
    }

    @Override
    public RNull executeNull(VirtualFrame frame) throws UnexpectedResultException {
        try {
            return operand.executeNull(frame);
        } catch (UnexpectedResultException e) {
            throw new UnexpectedResultException(execute(frame, e.getResult()));
        }
    }

    static WrapArgumentNode create(int index) {
        return new WrapArgumentNode(null, index);
    }

    static RNode create(RNode operand, int index) {
        assert !(operand instanceof WrapArgumentNode);
        if (operand instanceof ConstantNode) {
            return operand;
        } else {
            return new WrapArgumentNode(operand, index);
        }
    }
}
