/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin(name = "any", kind = PRIMITIVE, parameterNames = {"...", "na.rm"})
public abstract class Any extends RBuiltinNode {

    private final NACheck naCheck = NACheck.create();

    @Child private CastLogicalNode castLogicalNode;

    public abstract Object execute(VirtualFrame frame, Object o);

    @CreateCast("arguments")
    protected RNode[] castArguments(RNode[] arguments) {
        arguments[0] = CastLogicalNodeGen.create(arguments[0], true, false, false);
        return arguments;
    }

    @Specialization
    protected byte any(byte value) {
        controlVisibility();
        return value;
    }

    @Specialization
    protected byte any(RLogicalVector vector) {
        controlVisibility();
        return accumulate(vector);
    }

    @Specialization
    protected byte any(@SuppressWarnings("unused") RNull vector) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte any(@SuppressWarnings("unused") RMissing vector) {
        controlVisibility();
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected byte any(VirtualFrame frame, RArgsValuesAndNames args) {
        if (castLogicalNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castLogicalNode = insert(CastLogicalNodeGen.create(null, true, false, false));
        }
        controlVisibility();
        boolean seenNA = false;
        Object[] argValues = args.getValues();
        for (Object argValue : argValues) {
            byte result;
            if (argValue instanceof RVector || argValue instanceof RSequence) {
                result = accumulate((RLogicalVector) castLogicalNode.executeLogical(frame, argValue));
            } else if (argValue == RNull.instance) {
                result = RRuntime.LOGICAL_FALSE;
            } else {
                result = (byte) castLogicalNode.executeByte(frame, argValue);
            }
            if (RRuntime.isNA(result)) {
                seenNA = true;
            } else if (result == RRuntime.LOGICAL_TRUE) {
                return RRuntime.LOGICAL_TRUE;
            }
        }
        return seenNA ? RRuntime.LOGICAL_NA : RRuntime.LOGICAL_FALSE;
    }

    private byte accumulate(RLogicalVector vector) {
        naCheck.enable(vector);
        boolean seenNA = false;
        for (int i = 0; i < vector.getLength(); i++) {
            byte b = vector.getDataAt(i);
            if (naCheck.check(b)) {
                seenNA = true;
            } else if (b == RRuntime.LOGICAL_TRUE) {
                return RRuntime.LOGICAL_TRUE;
            }
        }
        return seenNA ? RRuntime.LOGICAL_NA : RRuntime.LOGICAL_FALSE;
    }
}
