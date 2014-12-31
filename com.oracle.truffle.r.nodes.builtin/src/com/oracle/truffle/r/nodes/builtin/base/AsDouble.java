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
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "as.double", aliases = {"as.numeric"}, kind = PRIMITIVE, parameterNames = {"x", "..."})
// TODO define alias in R
@SuppressWarnings("unused")
public abstract class AsDouble extends RBuiltinNode {

    @Child private CastDoubleNode castDoubleNode;

    private void initCast() {
        if (castDoubleNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castDoubleNode = insert(CastDoubleNodeGen.create(null, false, false, false));
        }
    }

    private double castDouble(VirtualFrame frame, int o) {
        initCast();
        return (double) castDoubleNode.executeDouble(frame, o);
    }

    private double castDouble(VirtualFrame frame, double o) {
        initCast();
        return (double) castDoubleNode.executeDouble(frame, o);
    }

    private double castDouble(VirtualFrame frame, byte o) {
        initCast();
        return (double) castDoubleNode.executeDouble(frame, o);
    }

    private double castDouble(VirtualFrame frame, Object o) {
        initCast();
        return (double) castDoubleNode.executeDouble(frame, o);
    }

    private RDoubleVector castDoubleVector(VirtualFrame frame, Object o) {
        initCast();
        return (RDoubleVector) castDoubleNode.executeDouble(frame, o);
    }

    @Specialization
    protected double asDouble(double value) {
        controlVisibility();
        return value;
    }

    @Specialization
    protected double asDoubleInt(VirtualFrame frame, int value) {
        controlVisibility();
        return castDouble(frame, value);
    }

    @Specialization
    protected double asDouble(VirtualFrame frame, byte value) {
        controlVisibility();
        return castDouble(frame, value);
    }

    @Specialization
    protected double asDouble(VirtualFrame frame, RComplex value) {
        controlVisibility();
        return castDouble(frame, value);
    }

    @Specialization
    protected double asDouble(VirtualFrame frame, String value) {
        controlVisibility();
        return castDouble(frame, value);
    }

    @Specialization
    protected RDoubleVector asDouble(RNull vector) {
        controlVisibility();
        return RDataFactory.createDoubleVector(0);
    }

    @Specialization
    protected RDoubleVector asDouble(RDoubleVector vector) {
        controlVisibility();
        return RDataFactory.createDoubleVector(vector.getDataCopy(), vector.isComplete());
    }

    @Specialization
    protected RDoubleVector asDouble(RDoubleSequence sequence) {
        controlVisibility();
        return (RDoubleVector) sequence.createVector();
    }

    @Specialization
    protected RDoubleVector asDouble(VirtualFrame frame, RIntSequence sequence) {
        controlVisibility();
        double current = sequence.getStart();
        double[] result = new double[sequence.getLength()];
        for (int i = 0; i < sequence.getLength(); ++i) {
            result[i] = castDouble(frame, current);
            current += sequence.getStride();
        }
        return RDataFactory.createDoubleVector(result, RDataFactory.INCOMPLETE_VECTOR);
    }

    @Specialization
    protected RDoubleVector asDouble(VirtualFrame frame, RAbstractVector vector) {
        controlVisibility();
        return castDoubleVector(frame, vector);
    }

    @Specialization
    protected RDoubleVector asDouble(VirtualFrame frame, RFactor vector) {
        return asDouble(frame, vector.getVector());
    }

}
