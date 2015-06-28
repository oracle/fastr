/*
 * Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * A {@link WrapDefaultArgumentNode} is used to wrap default function arguments as they are
 * essentially local variable writes and should be treated as such with respect to state transtions
 * of {@link RShareable}s.
 *
 */
public final class WrapDefaultArgumentNode extends WrapArgumentBaseNode {

    private final BranchProfile everSeenShared;
    private final BranchProfile everSeenTemporary;
    private final BranchProfile everSeenNonTemporary;

    private WrapDefaultArgumentNode(RNode operand) {
        super(operand, true);
        everSeenShared = BranchProfile.create();
        everSeenTemporary = BranchProfile.create();
        everSeenNonTemporary = BranchProfile.create();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object result = operand.execute(frame);
        return execute(result);
    }

    public Object execute(Object o) {
        Object result = o;
        RVector vector = getVector(result);
        if (vector != null) {
            shareable.enter();
            if (vector.isShared()) {
                everSeenShared.enter();
                result = ((RShareable) o).copy();
            } else if (vector.isTemporary()) {
                everSeenTemporary.enter();
                vector.markNonTemporary();
            } else {
                everSeenNonTemporary.enter();
                vector.makeShared();
            }
        }
        return result;

    }

    public static RNode create(RNode operand) {
        if (operand instanceof WrapArgumentNode || operand instanceof ConstantNode) {
            return operand;
        } else {
            return new WrapDefaultArgumentNode(operand);
        }
    }

    @Override
    public RSyntaxNode substitute(REnvironment env) {
        RNode sub = RSyntaxNode.cast(getOperand()).substitute(env).asRNode();
        if (sub instanceof RASTUtils.DotsNode) {
            return (RASTUtils.DotsNode) sub;
        } else {
            return RSyntaxNode.cast(create(sub));
        }
    }

}
