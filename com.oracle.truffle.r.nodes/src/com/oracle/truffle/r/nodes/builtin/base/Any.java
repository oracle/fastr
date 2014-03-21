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
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.RBuiltin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin(value = "any", lastParameterKind = LastParameterKind.VAR_ARGS_SPECIALIZE)
@SuppressWarnings("unused")
public abstract class Any extends RBuiltinNode {

    private final NACheck check = NACheck.create();

    @Child CastLogicalNode castLogicalNode;

    private byte castLogical(VirtualFrame frame, Object o) {
        if (castLogicalNode == null) {
            CompilerDirectives.transferToInterpreter();
            castLogicalNode = insert(CastLogicalNodeFactory.create(null, true, false));
        }
        return (byte) castLogicalNode.executeByte(frame, o);
    }

    private RLogicalVector castLogicalVector(VirtualFrame frame, Object o) {
        if (castLogicalNode == null) {
            CompilerDirectives.transferToInterpreter();
            castLogicalNode = insert(CastLogicalNodeFactory.create(null, true, false));
        }
        return (RLogicalVector) castLogicalNode.executeLogicalVector(frame, o);
    }

    private static final Object[] PARAMETER_NAMES = new Object[]{"..."};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Specialization
    public byte any(byte value) {
        return value;
    }

    @Specialization(order = 10)
    public byte any(int value) {
        check.enable(value);
        return check.convertIntToLogical(value);
    }

    @Specialization(order = 12)
    public byte any(double value) {
        check.enable(value);
        return check.convertDoubleToLogical(value);
    }

    @Specialization(order = 14)
    public byte any(RComplex value) {
        check.enable(value);
        return check.convertComplexToLogical(value);
    }

    @Specialization
    public byte any(VirtualFrame frame, String value) {
        check.enable(value);
        return check.convertStringToLogical(value);
    }

    @Specialization
    public byte any(RNull vector) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte any(RMissing vector) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte any(RLogicalVector vector) {
        check.enable(vector);
        boolean seenNA = false;
        for (int i = 0; i < vector.getLength(); i++) {
            byte b = vector.getDataAt(i);
            if (check.check(b)) {
                seenNA = true;
            } else if (b == RRuntime.LOGICAL_TRUE) {
                return RRuntime.LOGICAL_TRUE;
            }
        }
        return seenNA ? RRuntime.LOGICAL_NA : RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    public byte any(VirtualFrame frame, RIntVector vector) {
        return any(castLogicalVector(frame, vector));
    }

    @Specialization
    public byte any(VirtualFrame frame, RStringVector vector) {
        return any(castLogicalVector(frame, vector));
    }

    @Specialization
    public byte any(VirtualFrame frame, RDoubleVector vector) {
        return any(castLogicalVector(frame, vector));
    }

    @Specialization
    public byte any(VirtualFrame frame, RComplexVector vector) {
        return any(castLogicalVector(frame, vector));
    }

    @Specialization
    public byte any(VirtualFrame frame, RDoubleSequence sequence) {
        return any(castLogicalVector(frame, sequence));
    }

    @Specialization
    public byte any(VirtualFrame frame, RIntSequence sequence) {
        return any(castLogicalVector(frame, sequence));
    }

    @Specialization
    public byte any(VirtualFrame frame, RRawVector vector) {
        return any(castLogicalVector(frame, vector));
    }

    @Specialization
    public byte any(VirtualFrame frame, Object[] args) {
        boolean seenNA = false;
        check.enable(true);
        for (int i = 0; i < args.length; i++) {
            byte result;
            if (args[i] instanceof RVector || args[i] instanceof RSequence) {
                result = any(castLogicalVector(frame, args[i]));
            } else {
                result = any(castLogical(frame, args[i]));
            }
            if (check.check(result)) {
                seenNA = true;
            } else if (result == RRuntime.LOGICAL_TRUE) {
                return RRuntime.LOGICAL_TRUE;
            }
        }
        return seenNA ? RRuntime.LOGICAL_NA : RRuntime.LOGICAL_FALSE;
    }
}
