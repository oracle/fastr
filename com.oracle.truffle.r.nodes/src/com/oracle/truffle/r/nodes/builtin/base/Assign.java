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

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.REnvironment.PutException;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "assign", kind = SUBSTITUTE)
// TODO INTERNAL
public abstract class Assign extends RInvisibleBuiltinNode {

    // TODO convert to .Internal using assign.R to simplify the environment specializations

    @Child private WriteVariableNode writeVariableNode;

    @CompilationFinal private String lastName;

    // FIXME deal with omitted parameters: pos, imemdiate

    private static final Object[] PARAMETER_NAMES = new Object[]{"x", "value", "pos", "envir", "inherits", "immediate"};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(-1), ConstantNode.create(RMissing.instance),
                        ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RRuntime.LOGICAL_TRUE)};
    }

    private void ensureWrite(String x) {
        if (writeVariableNode == null || !x.equals(lastName)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lastName = x;
            // must write to super (effect must be visible in caller frame of the assign builtin)
            WriteVariableNode wvn = WriteVariableNode.create(lastName, null, false, false);
            writeVariableNode = writeVariableNode == null ? insert(wvn) : writeVariableNode.replace(wvn);
        }
    }

    @Specialization(order = 1, guards = {"noEnv", "!doesInheritS"})
    @SuppressWarnings("unused")
    public Object assignNoInherit(VirtualFrame frame, String x, Object value, Object pos, RMissing envir, byte inherits, byte immediate) {
        controlVisibility();
        ensureWrite(x);
        writeVariableNode.execute(frame, value);
        return value;
    }

    @Specialization(order = 2, guards = {"noEnv", "doesInheritS"})
    @SuppressWarnings("unused")
    public Object assignInherit(VirtualFrame frame, String x, Object value, Object pos, RMissing envir, byte inherits, byte immediate) {
        controlVisibility();

        MaterializedFrame frm = frame.materialize();

        // find the frame to write to
        FrameSlot slot = frm.getFrameDescriptor().findFrameSlot(x);
        MaterializedFrame encl = frm;
        while (slot == null && !REnvironment.isGlobalEnvFrame(frm)) {
            frm = encl;
            slot = frm.getFrameDescriptor().findFrameSlot(x);
            encl = RArguments.getEnclosingFrame(frm);
        }

        if (slot == null) {
            slot = frm.getFrameDescriptor().addFrameSlot(x);
        }
        frm.setObject(slot, value);
        return value;
    }

    @Specialization(order = 10, guards = "!doesInherit")
    @SuppressWarnings("unused")
    public Object assignNoInherit(String x, Object value, REnvironment pos, RMissing envir, byte inherits, byte immediate) {
        controlVisibility();
        if (pos == REnvironment.emptyEnv()) {
            throw RError.getGenericError(getEncapsulatingSourceSection(), "cannot assign values in the empty environment");
        }
        try {
            pos.put(x, value);
        } catch (PutException ex) {
            throw RError.getGenericError(getEncapsulatingSourceSection(), ex.getMessage());
        }
        return value;
    }

    @Specialization(order = 11, guards = "!doesInheritX")
    @SuppressWarnings("unused")
    public Object assignNoInherit(String x, Object value, int pos, REnvironment envir, byte inherits, byte immediate) {
        return assignNoInherit(x, value, envir, RMissing.instance, inherits, immediate);
    }

    @Specialization(order = 12, guards = "doesInherit")
    @SuppressWarnings("unused")
    public Object assignInherit(String x, Object value, REnvironment pos, RMissing envir, byte inherits, byte immediate) {
        controlVisibility();
        REnvironment env = pos;
        while (env != null) {
            if (env.get(x) != null) {
                break;
            }
            env = env.getParent();
        }
        try {
            if (env != null) {
                env.put(x, value);
            } else {
                REnvironment.globalEnv().put(x, value);
            }
        } catch (PutException ex) {
            throw RError.getGenericError(getEncapsulatingSourceSection(), ex.getMessage());
        }
        return value;
    }

    @Specialization(order = 20, guards = "!doesInherit")
    public Object assignNoInherit(RStringVector x, Object value, REnvironment pos, RMissing envir, byte inherits, byte immediate) {
        controlVisibility();
        return assignNoInherit(x.getDataAt(0), value, pos, envir, inherits, immediate);
    }

    @Specialization(order = 21, guards = "doesInherit")
    public Object assignInherit(RStringVector x, Object value, REnvironment pos, RMissing envir, byte inherits, byte immediate) {
        controlVisibility();
        return assignInherit(x.getDataAt(0), value, pos, envir, inherits, immediate);
    }

    @Specialization(order = 22, guards = "doesInheritX")
    public Object assignInherit(RStringVector x, Object value, @SuppressWarnings("unused") int pos, REnvironment envir, byte inherits, byte immediate) {
        controlVisibility();
        return assignInherit(x.getDataAt(0), value, envir, RMissing.instance, inherits, immediate);
    }

    @SuppressWarnings("unused")
    protected static boolean doesInherit(Object x, Object value, REnvironment pos, RMissing envir, byte inherits, byte immediate) {
        return inherits == RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    protected static boolean doesInheritX(Object x, Object value, int pos, REnvironment envir, byte inherits, byte immediate) {
        return inherits == RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    protected static boolean doesInheritS(String x, Object value, Object pos, RMissing envir, byte inherits, byte immediate) {
        return inherits == RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    protected static boolean noEnv(String x, Object value, Object pos, RMissing envir, byte inherits, byte immediate) {
        return !(pos instanceof REnvironment);
    }
}
