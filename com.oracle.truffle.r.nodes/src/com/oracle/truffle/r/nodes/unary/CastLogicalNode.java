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
package com.oracle.truffle.r.nodes.unary;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.unary.ConvertNode.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@NodeFields({@NodeField(name = "namesPreservation", type = boolean.class), @NodeField(name = "dimensionsPreservation", type = boolean.class)})
public abstract class CastLogicalNode extends CastNode {

    private final NACheck naCheck = NACheck.create();

    public abstract Object executeByte(VirtualFrame frame, Object o);

    public abstract Object executeLogicalVector(VirtualFrame frame, Object o);

    protected abstract boolean isNamesPreservation();

    protected abstract boolean isDimensionsPreservation();

    protected boolean preserveNames() {
        return isNamesPreservation();
    }

    protected boolean preserveDimensions() {
        return isDimensionsPreservation();
    }

    @Specialization
    public RNull doNull(@SuppressWarnings("unused") RNull operand) {
        return RNull.instance;
    }

    @Specialization
    public byte doLogical(byte operand) {
        return operand;
    }

    @Specialization
    public byte doDouble(double operand) {
        naCheck.enable(operand);
        return naCheck.convertDoubleToLogical(operand);
    }

    @Specialization
    public byte doInt(int operand) {
        naCheck.enable(operand);
        return naCheck.convertIntToLogical(operand);
    }

    @Specialization
    public byte doString(String operand) {
        naCheck.enable(operand);
        return naCheck.convertStringToLogical(operand);
    }

    @Specialization
    public byte doRaw(RRaw operand) {
        return RRuntime.raw2logical(operand);
    }

    @Specialization
    public RLogicalVector doLogicalVector(RLogicalVector operand) {
        return operand;
    }

    @Specialization
    public RLogicalVector doIntVector(RIntVector operand) {
        return performAbstractIntVector(operand);
    }

    @Specialization
    public RLogicalVector doIntSequence(RIntSequence operand) {
        return performAbstractIntVector(operand);
    }

    @Specialization
    public RLogicalVector doDoubleVector(RDoubleVector operand) {
        return performAbstractDoubleVector(operand);
    }

    @Specialization
    public RLogicalVector doDoubleSequence(RDoubleSequence operand) {
        return performAbstractDoubleVector(operand);
    }

    @Specialization(order = 101, guards = "preserveDimensions")
    public RLogicalVector doStringVectorDims(RStringVector operand) {
        naCheck.enable(operand);
        byte[] ddata = new byte[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            String value = operand.getDataAt(i);
            ddata[i] = naCheck.convertStringToLogical(value);
        }
        return RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA(), operand.getDimensions());
    }

    @Specialization(order = 102, guards = "preserveNames")
    public RLogicalVector doStringVectorNames(RStringVector operand) {
        naCheck.enable(operand);
        byte[] ddata = new byte[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            String value = operand.getDataAt(i);
            ddata[i] = naCheck.convertStringToLogical(value);
        }
        return RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA(), operand.getNames());
    }

    @Specialization(order = 103)
    public RLogicalVector doStringVector(RStringVector operand) {
        naCheck.enable(operand);
        byte[] ddata = new byte[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            String value = operand.getDataAt(i);
            ddata[i] = naCheck.convertStringToLogical(value);
        }
        return RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA());
    }

    @Specialization(order = 104, guards = "preserveDimensions")
    public RLogicalVector doComplexVectorDims(RComplexVector operand) {
        naCheck.enable(operand);
        byte[] ddata = new byte[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            RComplex value = operand.getDataAt(i);
            ddata[i] = naCheck.convertComplexToLogical(value);
        }
        return RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA(), operand.getDimensions());
    }

    @Specialization(order = 105, guards = "preserveNames")
    public RLogicalVector doComplexVectorNames(RComplexVector operand) {
        naCheck.enable(operand);
        byte[] ddata = new byte[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            RComplex value = operand.getDataAt(i);
            ddata[i] = naCheck.convertComplexToLogical(value);
        }
        return RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA(), operand.getNames());
    }

    @Specialization(order = 106)
    public RLogicalVector doComplexVector(RComplexVector operand) {
        naCheck.enable(operand);
        byte[] ddata = new byte[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            RComplex value = operand.getDataAt(i);
            ddata[i] = naCheck.convertComplexToLogical(value);
        }
        return RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA());
    }

    @Specialization(order = 107, guards = "preserveDimensions")
    public RLogicalVector doRawVectorDims(RRawVector operand) {
        byte[] ddata = new byte[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            RRaw value = operand.getDataAt(i);
            ddata[i] = RRuntime.raw2logical(value);
        }
        return RDataFactory.createLogicalVector(ddata, RDataFactory.COMPLETE_VECTOR, operand.getDimensions());
    }

    @Specialization(order = 108, guards = "preserveNames")
    public RLogicalVector doRawVectorNames(RRawVector operand) {
        byte[] ddata = new byte[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            RRaw value = operand.getDataAt(i);
            ddata[i] = RRuntime.raw2logical(value);
        }
        return RDataFactory.createLogicalVector(ddata, RDataFactory.COMPLETE_VECTOR, operand.getNames());
    }

    @Specialization(order = 109)
    public RLogicalVector doRawVector(RRawVector operand) {
        byte[] ddata = new byte[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            RRaw value = operand.getDataAt(i);
            ddata[i] = RRuntime.raw2logical(value);
        }
        return RDataFactory.createLogicalVector(ddata, RDataFactory.COMPLETE_VECTOR);
    }

    @Generic
    public int doOther(Object operand) {
        CompilerDirectives.transferToInterpreter();
        throw new ConversionFailedException(operand.getClass().getName());
    }

    private RLogicalVector performAbstractIntVector(RAbstractIntVector operand) {
        naCheck.enable(operand);
        byte[] ddata = new byte[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            int value = operand.getDataAt(i);
            ddata[i] = naCheck.convertIntToLogical(value);
        }
        if (preserveDimensions()) {
            return RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA(), operand.getDimensions());
        } else if (preserveNames()) {
            return RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA(), operand.getNames());
        } else {
            return RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA());
        }
    }

    private RLogicalVector performAbstractDoubleVector(RAbstractDoubleVector operand) {
        naCheck.enable(operand);
        byte[] ddata = new byte[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            double value = operand.getDataAt(i);
            ddata[i] = naCheck.convertDoubleToLogical(value);
        }
        if (preserveDimensions()) {
            return RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA(), operand.getDimensions());
        } else if (preserveNames()) {
            return RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA(), operand.getNames());
        } else {
            return RDataFactory.createLogicalVector(ddata, naCheck.neverSeenNA());
        }
    }

}
