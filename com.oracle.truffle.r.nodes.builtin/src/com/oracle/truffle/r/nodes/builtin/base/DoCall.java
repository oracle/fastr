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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

// TODO Implement properly, this is a simple implementation that works when the function is a builtin,
// the environment doesn't matter, and named argument matching isn't required
@RBuiltin(name = "do.call", kind = INTERNAL)
public abstract class DoCall extends RBuiltinNode {
    @Child protected IndirectCallNode funCall = Truffle.getRuntime().createIndirectCallNode();

    @Specialization(guards = "lengthOne")
    public Object doDoCall(VirtualFrame frame, RAbstractStringVector fname, RList argsAsList, @SuppressWarnings("unused") REnvironment env) {
        RFunction func = RContext.getEngine().lookupBuiltin(fname.getDataAt(0));
        if (func == null) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.UNKNOWN_FUNCTION, fname);
        }
        Object[] args = new Object[argsAsList.getLength()];
        for (int i = 0; i < args.length; i++) {
            args[i] = argsAsList.getDataAt(i);
        }
        Object[] callArgs = RArguments.create(func, args);
        return funCall.call(frame, func.getTarget(), callArgs);
    }

    public static boolean lengthOne(RAbstractStringVector vec) {
        return vec.getLength() == 1;
    }
}
