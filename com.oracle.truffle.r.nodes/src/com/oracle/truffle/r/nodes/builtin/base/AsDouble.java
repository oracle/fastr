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
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin({"as.double", "as.numeric"})
@SuppressWarnings("unused")
public abstract class AsDouble extends RBuiltinNode {

    private final NACheck check = NACheck.create();

    @Child CastDoubleNode castDoubleNode;

    private double castDouble(VirtualFrame frame, Object o) {
        if (castDoubleNode == null) {
            CompilerDirectives.transferToInterpreter();
            castDoubleNode = adoptChild(CastDoubleNodeFactory.create(null, false, false));
        }
        return (double) castDoubleNode.executeDouble(frame, o);
    }

    private RDoubleVector castDoubleVector(VirtualFrame frame, Object o) {
        if (castDoubleNode == null) {
            CompilerDirectives.transferToInterpreter();
            castDoubleNode = adoptChild(CastDoubleNodeFactory.create(null, false, false));
        }
        return (RDoubleVector) castDoubleNode.executeDoubleVector(frame, o);
    }

    @Specialization
    public double asDouble(double value) {
        return value;
    }

    @Specialization(order = 10)
    public double asDoubleInt(int value) {
        check.enable(value);
        return check.convertIntToDouble(value);
    }

    @Specialization
    public double asDouble(byte value) {
        check.enable(value);
        return check.convertLogicalToDouble(value);
    }

    @Specialization
    public double asDouble(RComplex value) {
        check.enable(value);
        return check.convertComplexToDouble(value);
    }

    @Specialization
    public double asDouble(VirtualFrame frame, String value) {
        return castDouble(frame, value);
    }

    @Specialization
    public RDoubleVector asDouble(RNull vector) {
        return RDataFactory.createDoubleVector(0);
    }

    @Specialization
    public RDoubleVector asDouble(RDoubleVector vector) {
        return vector;
    }

    @Specialization
    public RDoubleVector asDouble(VirtualFrame frame, RIntVector vector) {
        return castDoubleVector(frame, vector);
    }

    @Specialization
    public RDoubleVector asDouble(VirtualFrame frame, RStringVector vector) {
        return castDoubleVector(frame, vector);
    }

    @Specialization
    public RDoubleVector asDouble(VirtualFrame frame, RLogicalVector vector) {
        return castDoubleVector(frame, vector);
    }

    @Specialization
    public RDoubleVector asDouble(VirtualFrame frame, RComplexVector vector) {
        return castDoubleVector(frame, vector);
    }

    @Specialization
    public RDoubleSequence asDouble(RDoubleSequence sequence) {
        return sequence;
    }

    @Specialization
    public RDoubleVector asDouble(VirtualFrame frame, RIntSequence sequence) {
        double current = sequence.getStart();
        double[] result = new double[sequence.getLength()];
        for (int i = 0; i < sequence.getLength(); ++i) {
            result[i] = castDouble(frame, current);
            current += sequence.getStride();
        }
        return RDataFactory.createDoubleVector(result, RDataFactory.INCOMPLETE_VECTOR);
    }
}
