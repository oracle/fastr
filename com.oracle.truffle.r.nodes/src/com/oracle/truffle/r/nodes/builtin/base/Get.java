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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.*;
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

    // TODO
    // 1. handle mode parameter (we should perhaps guard against mode being set in call right now)
    // 2. revert to .Internal using get.R

    private static final Object[] PARAMETER_NAMES = new Object[]{"x", "pos", "envir", "mode", "inherits"};

    @Child protected ReadVariableNode lookUpInherit;
    @Child protected ReadVariableNode lookUpNoInherit;

    @CompilationFinal protected String lastX;
    @CompilationFinal protected String lastMode;

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(-1), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.TYPE_ANY),
                        ConstantNode.create(RRuntime.LOGICAL_TRUE)};
    }

    public abstract Object execute(VirtualFrame frame, String name, int pos, RMissing envir, String mode, byte inherits);

    @Specialization(order = 0)
    @SuppressWarnings("unused")
    public Object get(VirtualFrame frame, String x, int pos, RMissing envir, String mode, byte inherits) {
        controlVisibility();
        boolean doesInherit = inherits == RRuntime.LOGICAL_TRUE;
        ReadVariableNode lookup = null;
        if (doesInherit) {
            lookup = lookUpInherit = setLookUp(lookUpInherit, x, mode, doesInherit);
        } else {
            lookup = lookUpNoInherit = setLookUp(lookUpNoInherit, x, mode, doesInherit);
        }
        try {
            return lookup.execute(frame);
        } catch (RError e) {
            throw RError.getUnknownVariable(getEncapsulatingSourceSection(), x);
        }
    }

    private ReadVariableNode setLookUp(ReadVariableNode lookup, final String x, final String mode, boolean inherits) {
        if (lookup == null || !lastX.equals(x) || !lastMode.equals(mode)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lastX = x;
            lastMode = mode;
            ReadVariableNode rvn = ReadVariableNode.create(x, mode, false, inherits);
            ReadVariableNode newLookup = lookup == null ? insert(rvn) : lookup.replace(rvn);
            return newLookup;
        }
        return lookup;
    }

    @Specialization(order = 1)
    @SuppressWarnings("unused")
    public Object get(RAbstractStringVector x, REnvironment pos, RMissing envir, String mode, byte inherits) {
        controlVisibility();
        String sx = x.getDataAt(0);
        REnvironment env = pos;
        Object r = env.get(sx);
        if (r == null && inherits == RRuntime.LOGICAL_TRUE) {
            while (r == null && env != null) {
                env = env.getParent();
                if (env != null) {
                    r = env.get(sx);
                }
            }
        }
        if (r == null) {
            throw RError.getUnknownVariable(getEncapsulatingSourceSection(), sx);
        } else {
            return r;
        }
    }

    @Specialization(order = 2)
    @SuppressWarnings("unused")
    public Object get(RAbstractStringVector x, int pos, REnvironment envir, String mode, byte inherits) {
        return get(x, envir, RMissing.instance, mode, inherits);

    }

}
