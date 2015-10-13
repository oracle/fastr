/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import static com.oracle.truffle.r.runtime.RDispatch.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.GetFunctionsFactory.GetNodeGen;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.nodes.*;

@RBuiltin(name = "xtfrm", kind = PRIMITIVE, parameterNames = {"x"}, dispatch = INTERNAL_GENERIC)
public abstract class Xtfrm extends RBuiltinNode {
    @Child private GetFunctions.Get getNode;

    @Specialization
    protected Object xtfrm(VirtualFrame frame, Object x) {
        /*
         * Although this is a PRIMITIVE, there is an xtfrm.default that we must call if "x" is not
         * of a class that already has an xtfrm.class function defined. We only get here in the
         * default case.
         */
        if (getNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getNode = insert(GetNodeGen.create(new RNode[4], null, null));
        }
        RFunction func = (RFunction) getNode.execute(frame, "xtfrm.default", RArguments.getEnvironment(frame), RType.Function.getName(), RRuntime.LOGICAL_TRUE);
        return RContext.getEngine().evalFunction(func, null, x);
    }
}
