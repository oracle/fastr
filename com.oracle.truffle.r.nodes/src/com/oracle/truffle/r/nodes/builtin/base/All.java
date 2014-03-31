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

@RBuiltin(value = "all", lastParameterKind = LastParameterKind.VAR_ARGS_SPECIALIZE)
@SuppressWarnings("unused")
public abstract class All extends RBuiltinNode {

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
    public byte all(byte value) {
        controlVisibility();
        return value;
    }

    @Specialization(order = 10)
    public byte all(int value) {
        controlVisibility();
        check.enable(value);
        return check.convertIntToLogical(value);
    }

    @Specialization(order = 12)
    public byte all(double value) {
        controlVisibility();
        check.enable(value);
        return check.convertDoubleToLogical(value);
    }

    @Specialization(order = 14)
    public byte all(RComplex value) {
        controlVisibility();
        check.enable(value);
        return check.convertComplexToLogical(value);
    }

    @Specialization
    public byte all(VirtualFrame frame, String value) {
        controlVisibility();
        check.enable(value);
        return check.convertStringToLogical(value);
    }

    @Specialization
    public byte all(RNull vector) {
        controlVisibility();
        return RRuntime.LOGICAL_TRUE;
    }

    @Specialization
    public byte all(RMissing vector) {
        controlVisibility();
        return RRuntime.LOGICAL_TRUE;
    }

    @Specialization
    public byte all(RLogicalVector vector) {
        controlVisibility();
        for (int i = 0; i < vector.getLength(); i++) {
            byte b = vector.getDataAt(i);
            if (b != RRuntime.LOGICAL_TRUE) {
                return b;
            }
        }
        return RRuntime.LOGICAL_TRUE;
    }

    @Specialization
    public byte all(VirtualFrame frame, RIntVector vector) {
        controlVisibility();
        return all(castLogicalVector(frame, vector));
    }

    @Specialization
    public byte all(VirtualFrame frame, RStringVector vector) {
        controlVisibility();
        return all(castLogicalVector(frame, vector));
    }

    @Specialization
    public byte all(VirtualFrame frame, RDoubleVector vector) {
        controlVisibility();
        return all(castLogicalVector(frame, vector));
    }

    @Specialization
    public byte all(VirtualFrame frame, RComplexVector vector) {
        controlVisibility();
        return all(castLogicalVector(frame, vector));
    }

    @Specialization
    public byte all(VirtualFrame frame, RDoubleSequence sequence) {
        controlVisibility();
        return all(castLogicalVector(frame, sequence));
    }

    @Specialization
    public byte all(VirtualFrame frame, RIntSequence sequence) {
        controlVisibility();
        return all(castLogicalVector(frame, sequence));
    }

    @Specialization
    public byte all(VirtualFrame frame, RRawVector vector) {
        controlVisibility();
        return all(castLogicalVector(frame, vector));
    }

    @Specialization
    public byte all(VirtualFrame frame, Object[] args) {
        controlVisibility();
        for (int i = 0; i < args.length; i++) {
            byte result;
            if (args[i] instanceof RVector || args[i] instanceof RSequence) {
                result = all(castLogicalVector(frame, args[i]));
            } else {
                result = all(castLogical(frame, args[i]));
            }
            if (result != RRuntime.LOGICAL_TRUE) {
                return result;
            }
        }
        return RRuntime.LOGICAL_TRUE;
    }
}
