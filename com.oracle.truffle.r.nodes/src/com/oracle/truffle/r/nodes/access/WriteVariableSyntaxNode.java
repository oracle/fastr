/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access;

import static com.oracle.truffle.api.nodes.NodeCost.NONE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.access.WriteVariableNode.Mode;
import com.oracle.truffle.r.nodes.control.OperatorNode;
import com.oracle.truffle.r.nodes.function.ArgumentMatcher;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;

/**
 * This node represents a write to a variable on the syntax level, i.e., in the R source code.
 * <p>
 * When executed in the context of an eager promise evaluation, it must be checked whether there are
 * no side effects, which is the important assumption for the eager promise evaluation. Unless the
 * write node writes outside the current frame (i.e. if {@code isSuper} is false), it is assumed
 * that the operation has no side effect and thus safe w.r.t. the eager promise evaluation. Any
 * changes made by this node in the current frame will be destroyed in the case that any subsequent
 * operation will cancel the ongoing eager promise evaluation by throwing
 * {@code CannotOptimizePromise}.
 * <p>
 * NB: When an assignment is present in an argument expression, the argument is evicted from the
 * eager evaluation (see {@code matchNodes} in {@link ArgumentMatcher}). Therefore, in the following
 * example, the argument of {@code foo} will not be evaluated eagerly:
 *
 * <pre>
 * foo <- function(a) a
 * bar <- function() { x <- 3; foo(x <- 4); print(x); }
 * </pre>
 */
@NodeInfo(cost = NONE)
public final class WriteVariableSyntaxNode extends OperatorNode {

    @Child private WriteVariableNode write;
    @Child private SetVisibilityNode visibility;

    private final RSyntaxElement lhs;
    private final boolean isSuper;

    public WriteVariableSyntaxNode(SourceSection source, RSyntaxLookup operator, RSyntaxElement lhs, String name, RNode rhs, boolean isSuper) {
        super(source, operator);
        this.lhs = lhs;
        this.write = WriteVariableNode.createAnonymous(name, Mode.REGULAR, rhs, isSuper);
        this.isSuper = isSuper;
        assert write != null;
    }

    @Override
    public void voidExecute(VirtualFrame frame) {
        if (isSuper) {
            RArguments.getCall(frame).checkEagerPromiseOnly();
        }
        write.execute(frame);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (isSuper) {
            RArguments.getCall(frame).checkEagerPromiseOnly();
        }
        return write.execute(frame);
    }

    @Override
    public int executeInteger(VirtualFrame frame) throws UnexpectedResultException {
        if (isSuper) {
            RArguments.getCall(frame).checkEagerPromiseOnly();
        }
        return write.executeInteger(frame);
    }

    @Override
    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        if (isSuper) {
            RArguments.getCall(frame).checkEagerPromiseOnly();
        }
        return write.executeDouble(frame);
    }

    @Override
    public byte executeByte(VirtualFrame frame) throws UnexpectedResultException {
        if (isSuper) {
            RArguments.getCall(frame).checkEagerPromiseOnly();
        }
        return write.executeByte(frame);
    }

    @Override
    public Object visibleExecute(VirtualFrame frame) {
        if (isSuper) {
            RArguments.getCall(frame).checkEagerPromiseOnly();
        }
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
