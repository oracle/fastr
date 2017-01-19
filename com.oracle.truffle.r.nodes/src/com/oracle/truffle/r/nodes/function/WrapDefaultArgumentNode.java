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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.runtime.data.RShareable;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * A {@link WrapDefaultArgumentNode} is used to wrap default function arguments as they are
 * essentially local variable writes and should be treated as such with respect to state transitions
 * of {@link RShareable}s.
 *
 */
public final class WrapDefaultArgumentNode extends WrapArgumentBaseNode {

    @CompilationFinal private ConditionProfile isShared;
    @CompilationFinal private ValueProfile copyProfile;

    private WrapDefaultArgumentNode(RNode operand) {
        super(operand);
    }

    @Override
    protected Object handleShareable(VirtualFrame frame, RSharingAttributeStorage shareable) {
        if (isShared == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isShared = ConditionProfile.createBinaryProfile();
        }
        if (isShared.profile(shareable.isShared())) {
            if (copyProfile == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                copyProfile = ValueProfile.createClassProfile();
            }
            return copyProfile.profile(shareable).copy();
        } else {
            shareable.incRefCount();
            return shareable;
        }
    }

    public static RNode create(RNode operand) {
        assert !(operand instanceof WrapArgumentNode);
        if (operand instanceof ConstantNode) {
            return operand;
        } else {
            return new WrapDefaultArgumentNode(operand);
        }
    }
}
