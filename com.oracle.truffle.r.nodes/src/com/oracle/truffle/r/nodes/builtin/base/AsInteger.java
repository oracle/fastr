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
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;
import com.sun.org.apache.xml.internal.utils.*;

@RBuiltin("as.integer")
@SuppressWarnings("unused")
public abstract class AsInteger extends RBuiltinNode {

    private final NACheck check = NACheck.create();

    @Child CastIntegerNode castIntNode;

    private int castInt(VirtualFrame frame, Object o) {
        if (castIntNode == null) {
            CompilerDirectives.transferToInterpreter();
            castIntNode = adoptChild(CastIntegerNodeFactory.create(null, false, false));
        }
        return (int) castIntNode.executeInt(frame, o);
    }

    private RIntVector castIntVector(VirtualFrame frame, Object o) {
        if (castIntNode == null) {
            CompilerDirectives.transferToInterpreter();
            castIntNode = adoptChild(CastIntegerNodeFactory.create(null, false, false));
        }
        return (RIntVector) castIntNode.executeIntVector(frame, o);
    }

    @Specialization
    public int asInteger(int value) {
        return value;
    }

    @Specialization
    public int asInteger(double value) {
        check.enable(value);
        return check.convertDoubleToInt(value);
    }

    @Specialization
    public int asInteger(byte value) {
        return RRuntime.logical2int(value);
    }

    @Specialization
    public int asInteger(RComplex value) {
        check.enable(value);
        return check.convertComplexToInt(value);
    }

    @Specialization
    public int asInteger(RRaw value) {
        // Raw values cannot be NA.
        return (value.getValue()) & 0xFF;
    }

    @Specialization
    public int asInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return RRuntime.INT_NA;
        }
    }

    @Specialization
    public int asInteger(RNull vector) {
        return RRuntime.INT_NA;
    }

    @Specialization(order = 71)
    public RIntVector asInteger(RIntVector vector) {
        return RDataFactory.createIntVector(vector.getDataCopy(), vector.isComplete());
    }

    @Specialization
    public RIntVector asInteger(VirtualFrame frame, RDoubleVector vector) {
        return castIntVector(frame, vector);
    }

    @Specialization
    public RIntVector asInteger(VirtualFrame frame, RStringVector vector) {
        return castIntVector(frame, vector);
    }

    @Specialization
    public RIntVector asInteger(VirtualFrame frame, RLogicalVector vector) {
        return castIntVector(frame, vector);
    }

    @Specialization
    public RIntVector asInteger(VirtualFrame frame, RComplexVector vector) {
        return castIntVector(frame, vector);
    }

    @Specialization
    public RIntVector asInteger(VirtualFrame frame, RRawVector vector) {
        return castIntVector(frame, vector);
    }

    @Specialization
    public RIntVector asInteger(RIntSequence sequence) {
        return (RIntVector) sequence.createVector();
    }

    @Specialization
    public RIntVector asInteger(VirtualFrame frame, RDoubleSequence sequence) {
        return castIntVector(frame, sequence);
    }

    @Specialization
    public RIntVector asInteger(VirtualFrame frame, RList list) {
        return castIntVector(frame, list);
    }
}
