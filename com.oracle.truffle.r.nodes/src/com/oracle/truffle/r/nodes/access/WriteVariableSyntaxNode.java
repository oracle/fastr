/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access;

import static com.oracle.truffle.api.nodes.NodeCost.NONE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.access.WriteVariableNode.Mode;
import com.oracle.truffle.r.nodes.control.OperatorNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;

/**
 * This node represents a write to a variable on the syntax level, i.e., in the R source code.
 */
@NodeInfo(cost = NONE)
public final class WriteVariableSyntaxNode extends OperatorNode {

    @Child private WriteVariableNode write;
    @Child private SetVisibilityNode visibility;

    private final RSyntaxElement lhs;

    public WriteVariableSyntaxNode(SourceSection source, RSyntaxLookup operator, RSyntaxElement lhs, String name, RNode rhs, boolean isSuper) {
        super(source, operator);
        this.lhs = lhs;
        this.write = WriteVariableNode.createAnonymous(name, Mode.REGULAR, rhs, isSuper);
        assert write != null;
    }

    @Override
    public void voidExecute(VirtualFrame frame) {
        write.execute(frame);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return write.execute(frame);
    }

    @Override
    public int executeInteger(VirtualFrame frame) throws UnexpectedResultException {
        return write.executeInteger(frame);
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return write.executeDouble(frame);
    }

    @Override
    public byte executeByte(VirtualFrame frame) throws UnexpectedResultException {
        return write.executeByte(frame);
    }

    @Override
    public Object visibleExecute(VirtualFrame frame) {
        Object result = write.execute(frame);
        if (visibility == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            visibility = insert(SetVisibilityNode.create());
        }
        visibility.execute(frame, false);
        return result;
    }

    @Override
    public RSyntaxElement[] getSyntaxArguments() {
        return new RSyntaxElement[]{lhs, write.getRhs().asRSyntaxNode()};
    }

    @Override
    public ArgumentsSignature getSyntaxSignature() {
        return ArgumentsSignature.empty(2);
    }
}
