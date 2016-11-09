/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * A {@link WrapDefaultArgumentNode} is used to wrap default function arguments as they are
 * essentially local variable writes and should be treated as such with respect to state transitions
 * of {@link RShareable}s.
 *
 */
public final class WrapDefaultArgumentNode extends WrapArgumentBaseNode {

    private final ConditionProfile isShared = ConditionProfile.createBinaryProfile();

    private WrapDefaultArgumentNode(RNode operand) {
        super(operand, true);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object result = operand.execute(frame);
        RShareable rShareable = getShareable(result);
        if (rShareable != null) {
            shareable.enter();
            if (isShared.profile(rShareable.isShared())) {
                return rShareable.copy();
            } else {
                ((RShareable) result).incRefCount();
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
}
