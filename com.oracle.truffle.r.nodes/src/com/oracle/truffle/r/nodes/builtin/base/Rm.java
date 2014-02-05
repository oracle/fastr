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
import com.oracle.truffle.r.nodes.builtin.RBuiltin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(value = {"rm", "remove"}, lastParameterKind = LastParameterKind.VAR_ARGS_SPECIALIZE)
public abstract class Rm extends RBuiltinNode {

    public static Rm create(String name) {
        RNode[] args = getParameterValues0();
        args[0] = ConstantNode.create(name);
        return RmFactory.create(args, RDefaultPackages.getInstance().lookupBuiltin("rm"));
    }

    private static RNode[] getParameterValues0() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RDataFactory.createStringVector(0)), ConstantNode.create(-1), ConstantNode.create(RMissing.instance),
                        ConstantNode.create(RRuntime.LOGICAL_FALSE)};
    }

    private static final Object[] PARAMETER_NAMES = new Object[]{"...", "list", "pos", "envir", "inherits"};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return getParameterValues0();
    }

    @Specialization
    @SuppressWarnings("unused")
    public Object rm(VirtualFrame frame, String name, RStringVector list, Object pos, RMissing envir, byte inherits) {
        removeFromCurrentFrame(frame, name);
        return RInvisible.INVISIBLE_NULL;
    }

    @Specialization
    @SuppressWarnings("unused")
    public Object rm(VirtualFrame frame, Object[] names, RStringVector list, Object pos, RMissing envir, byte inherits) {
        for (Object o : names) {
            assert o instanceof String;
            removeFromCurrentFrame(frame, (String) o);
        }
        return RInvisible.INVISIBLE_NULL;
    }

    private void removeFromCurrentFrame(VirtualFrame frame, String x) {
        // standard case for lookup in current frame
        Frame frm = frame;
        FrameSlot fs = frame.getFrameDescriptor().findFrameSlot(x);
        while (fs == null && frm != null) {
            frm = RArguments.get(frm).getEnclosingFrame();
            if (frm != null) {
                fs = frm.getFrameDescriptor().findFrameSlot(x);
            }
        }
        if (fs == null) {
            RError.warning(this.getEncapsulatingSourceSection(), RError.UNKNOWN_OBJECT, x);
        } else {
            frm.getFrameDescriptor().removeFrameSlot(x);
        }
    }

}
