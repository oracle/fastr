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

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.RBuiltin.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "expression", kind = PRIMITIVE, nonEvalArgs = {-1}, lastParameterKind = LastParameterKind.VAR_ARGS_ALWAYS_ARRAY)
public abstract class Expression extends RBuiltinNode {
    private static final String[] PARAMETER_NAMES = new String[]{"..."};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Specialization
    public Object doExpression(Object[] args) {
        RList list = RDataFactory.createList(args);
        return RDataFactory.createExpression(list);
    }

    @Specialization
    public Object doExpression(RLanguage language) {
        RList list = RDataFactory.createList(new Object[]{language});
        return RDataFactory.createExpression(list);
    }

}
