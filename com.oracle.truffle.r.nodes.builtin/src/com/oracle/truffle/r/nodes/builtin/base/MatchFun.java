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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "match.fun", kind = SUBSTITUTE, parameterNames = {"fun", "descend"})
// TODO revert to R
public abstract class MatchFun extends RBuiltinNode {

    @CompilationFinal protected String lastFun;

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_TRUE)};
    }

    // FIXME honour the descend parameter
    // FIXME implement actual semantics (lookup in caller environment)

    @Specialization
    public RFunction matchFun(RFunction fun, @SuppressWarnings("unused") byte descend) {
        return fun;
    }

    @Child protected ReadVariableNode lookup;

    @Specialization
    public Object matchFun(VirtualFrame frame, String fun, @SuppressWarnings("unused") byte descend) {
        controlVisibility();
        if (lookup == null || !fun.equals(lastFun)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lastFun = fun;
            ReadVariableNode rvn = ReadVariableNode.create(fun, RRuntime.TYPE_FUNCTION, false);
            lookup = lookup == null ? insert(rvn) : lookup.replace(rvn);
        }
        Object r = lookup.execute(frame);
        if (r == null) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.UNKNOWN_FUNCTION, fun);
        } else {
            return r;
        }
    }

}
