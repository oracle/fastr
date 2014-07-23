/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

@RBuiltin(name = "anyNA", kind = RBuiltinKind.PRIMITIVE)
public abstract class AnyNA extends RBuiltinNode {

    private static final String[] PARAMETER_NAMES = new String[]{"x", "recursive"};

    @Child IsNA isna;
    @Child Any any;

    @Override
    public String[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(false)};
    }

    @Specialization
    // TODO recursive == TRUE
    public Object anyNA(VirtualFrame frame, Object x, byte recursive) {
        if (RRuntime.fromLogical(recursive)) {
            throw RError.nyi(getEncapsulatingSourceSection(), "recursive = TRUE not implemented");
        }
        if (x == RNull.instance) {
            return RRuntime.LOGICAL_FALSE;
        }
        if (isna == null) {
            isna = insert(IsNAFactory.create(new RNode[1], getBuiltin()));
            any = insert(AnyFactory.create(new RNode[1], getBuiltin()));
        }
        Object val = isna.execute(frame, x);
        if (!(val instanceof Byte)) {
            val = any.execute(frame, val);
        }
        return val;
    }
}
