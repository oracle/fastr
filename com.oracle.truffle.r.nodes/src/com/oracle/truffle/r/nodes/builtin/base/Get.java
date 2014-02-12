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
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin("get")
public abstract class Get extends RBuiltinNode {

    private static final Object[] PARAMETER_NAMES = new Object[]{"x", "pos", "envir", "mode", "inherits"};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(-1), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.TYPE_ANY),
                        ConstantNode.create(RRuntime.LOGICAL_TRUE)};
    }

    @Specialization
    @SuppressWarnings("unused")
    public Object get(RAbstractStringVector x, REnvironment pos, RMissing envir, String mode, byte inherits) {
        String sx = x.getDataAt(0);
        REnvironment env = pos;
        Object r = env.get(sx);
        while (r == null && env != null) {
            env = env.getParent();
            if (env != null) {
                r = env.get(sx);
            }
        }
        return r;
    }

    @Specialization
    @SuppressWarnings("unused")
    public Object get(VirtualFrame frame, String x, int pos, RMissing envir, String mode, byte inherits) {
        // standard case for lookup in current frame
        Frame frm = frame;
        FrameSlot fs = frame.getFrameDescriptor().findFrameSlot(x);
        while (fs == null && frm != null) {
            frm = RArguments.get(frm).getEnclosingFrame();
            if (frm != null) {
                fs = frm.getFrameDescriptor().findFrameSlot(x);
            }
        }
        if (fs != null) {
            Object v = frm.getValue(fs);
            if (v != null) {
                return v;
            }
        }
        throw RError.getUnknownVariable(this.getEncapsulatingSourceSection(), x);
    }

}
