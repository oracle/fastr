/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.*;

public final class RBuiltinRootNode extends RRootNode {

    @Child private RBuiltinNode builtin;

    private final RBuiltinNode uninitializedBuiltin;

    public RBuiltinRootNode(RBuiltinNode builtin, Object[] parameterNames, FrameDescriptor frameDescriptor) {
        super(builtin.getSourceSection(), parameterNames, frameDescriptor);
        this.builtin = builtin;
        this.uninitializedBuiltin = NodeUtil.cloneNode(builtin);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return builtin.execute(frame);
    }

    public RCallNode inline(CallArgumentsNode args) {
        return builtin.inline(args);
    }

    public boolean evaluatesArgs() {
        RBuiltin rBuiltin = builtin.getRBuiltin();
        return rBuiltin == null || rBuiltin.nonEvalArgs().length == 0;
    }

    public boolean evaluatesArg(int index) {
        RBuiltin rBuiltin = builtin.getRBuiltin();
        if (rBuiltin == null) {
            return true;
        } else {
            int[] nonEvalArgs = rBuiltin.nonEvalArgs();
            for (int i = 0; i < nonEvalArgs.length; i++) {
                int ix = nonEvalArgs[i];
                if (ix < 0 || ix == index) {
                    return false;
                }
            }
            return true;
        }

    }

    @Override
    public String getSourceCode() {
        return builtin.getSourceCode();
    }

    @Override
    public RootNode split() {
        return new RBuiltinRootNode(NodeUtil.cloneNode(uninitializedBuiltin), getParameterNames(), getFrameDescriptor().shallowCopy());
    }

}
