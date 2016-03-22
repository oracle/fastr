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
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.nodes.RNode;

/**
 * Encapsulates the nodes that increment reference count incremented when the argument node is
 * wrapped.
 */
final class PreProcessArgumentsNode extends RNode {

    @Children private final ArgumentStatePush[] sequence;

    private PreProcessArgumentsNode(ArgumentStatePush[] sequence) {
        this.sequence = sequence;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw RInternalError.shouldNotReachHere();
    }

    public int getLength() {
        return sequence.length;
    }

    static PreProcessArgumentsNode create(int length) {
        ArgumentStatePush[] argStatePushNodes = new ArgumentStatePush[length];
        for (int i = 0; i < length; i++) {
            argStatePushNodes[i] = ArgumentStatePushNodeGen.create(i, null);
        }
        return new PreProcessArgumentsNode(argStatePushNodes);
    }

    @ExplodeLoop
    public RNull execute(VirtualFrame frame, Object[] args) {
        assert args.length == sequence.length;
        for (int i = 0; i < sequence.length; i++) {
            sequence[i].executeObject(frame, args[i]);
        }
        return RNull.instance;
    }
}
