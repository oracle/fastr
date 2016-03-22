/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.SUBSTITUTE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

@RBuiltin(name = "match.fun", kind = SUBSTITUTE, parameterNames = {"fun", "descend"})
// TODO revert to R
public abstract class MatchFun extends RBuiltinNode {

    @CompilationFinal private String lastFun;

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, RRuntime.LOGICAL_TRUE};
    }

    // FIXME honor the descend parameter
    // FIXME implement actual semantics (lookup in caller environment)

    @Specialization
    protected RFunction matchFun(RFunction fun, @SuppressWarnings("unused") byte descend) {
        return fun;
    }

    @Child private ReadVariableNode lookup;

    @Specialization
    protected Object matchFun(VirtualFrame frame, String fun, @SuppressWarnings("unused") byte descend) {
        controlVisibility();
        if (lookup == null || !fun.equals(lastFun)) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lastFun = fun;
            ReadVariableNode rvn = ReadVariableNode.createFunctionLookup(RSyntaxNode.INTERNAL, fun);
            lookup = lookup == null ? insert(rvn) : lookup.replace(rvn);
        }
        Object r = lookup.execute(frame);
        if (r == null) {
            throw RError.error(this, RError.Message.UNKNOWN_FUNCTION, fun);
        } else {
            return r;
        }
    }
}
