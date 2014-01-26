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
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@NodeFields({@NodeField(name = "namesPreservation", type = boolean.class), @NodeField(name = "dimensionsPreservation", type = boolean.class)})
public abstract class CastDoubleNode extends CastNode {

    private final NACheck naCheck = NACheck.create();

    public abstract Object executeDouble(VirtualFrame frame, Object o);

    public abstract Object executeDoubleVector(VirtualFrame frame, Object o);

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

    @Specialization(order = 1)
    public double doInt(int operand) {
        naCheck.enable(operand);
        return naCheck.convertIntToDouble(operand);
    }

    @Specialization(order = 10)
    public double doDouble(double operand) {
        return operand;
    }

    @Specialization(order = 20)
    public double doLogical(byte operand) {
        naCheck.enable(operand);
        return naCheck.convertLogicalToDouble(operand);
    }

    @Specialization(order = 30)
    public double doString(String operand) {
        naCheck.enable(operand);
        return naCheck.convertStringToDouble(operand);
    }

    @Specialization
    public RDoubleVector doIntVector(RIntVector operand) {
        return performAbstractIntVector(operand);
    }

    @Specialization
    public RDoubleVector doIntVector(RIntSequence operand) {
        return performAbstractIntVector(operand);
    }

    @Specialization(order = 101, guards = "preserveDimensions")
    public RDoubleVector doLogicalVectorDimsAndNames(RLogicalVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            byte value = operand.getDataAt(i);
            ddata[i] = naCheck.convertLogicalToDouble(value);
        }
        return RDataFactory.createDoubleVector(ddata, operand.isComplete(), operand.getDimensions());
    }

    @Specialization(order = 102, guards = "preserveNames")
    public RDoubleVector doLogicalVectorNames(RLogicalVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            byte value = operand.getDataAt(i);
            ddata[i] = naCheck.convertLogicalToDouble(value);
        }
        return RDataFactory.createDoubleVector(ddata, operand.isComplete(), operand.getNames());
    }

    @Specialization(order = 103)
    public RDoubleVector doLogicalVector(RLogicalVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            byte value = operand.getDataAt(i);
            ddata[i] = naCheck.convertLogicalToDouble(value);
        }
        return RDataFactory.createDoubleVector(ddata, operand.isComplete());
    }

    @Specialization(order = 104, guards = "preserveDimensions")
    public RDoubleVector doStringVectorDimsAndNames(RStringVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            String value = operand.getDataAt(i);
            ddata[i] = naCheck.convertStringToDouble(value);
        }
        return RDataFactory.createDoubleVector(ddata, operand.isComplete(), operand.getDimensions());
    }

    @Specialization(order = 105, guards = "preserveNames")
    public RDoubleVector doStringVectorNames(RStringVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            String value = operand.getDataAt(i);
            ddata[i] = naCheck.convertStringToDouble(value);
        }
        return RDataFactory.createDoubleVector(ddata, operand.isComplete(), operand.getNames());
    }

    @Specialization(order = 106)
    public RDoubleVector doStringVector(RStringVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            String value = operand.getDataAt(i);
            ddata[i] = naCheck.convertStringToDouble(value);
        }
        return RDataFactory.createDoubleVector(ddata, operand.isComplete(), operand.getNames());
    }

    @Specialization(order = 107, guards = "preserveDimensions")
    public RDoubleVector doComplexVectorDimsAndNames(RComplexVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            RComplex value = operand.getDataAt(i);
            ddata[i] = naCheck.convertComplexToDouble(value);
        }
        return RDataFactory.createDoubleVector(ddata, operand.isComplete(), operand.getDimensions());
    }

    @Specialization(order = 108, guards = "preserveNames")
    public RDoubleVector doComplexVectorNames(RComplexVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            RComplex value = operand.getDataAt(i);
            ddata[i] = naCheck.convertComplexToDouble(value);
        }
        return RDataFactory.createDoubleVector(ddata, operand.isComplete(), operand.getNames());
    }

    @Specialization(order = 109)
    public RDoubleVector doComplexVector(RComplexVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            RComplex value = operand.getDataAt(i);
            ddata[i] = naCheck.convertComplexToDouble(value);
        }
        return RDataFactory.createDoubleVector(ddata, operand.isComplete());
    }

    @Specialization
    public RDoubleVector doDoubleVector(RDoubleVector operand) {
        return operand;
    }

    @Specialization
    public RDoubleSequence doDoubleSequence(RDoubleSequence operand) {
        return operand;
    }

    @Generic
    public int doOther(Object operand) {
        CompilerDirectives.transferToInterpreter();
        throw new ConversionFailedException(operand.getClass().getName());
    }

    private RDoubleVector performAbstractIntVector(RAbstractIntVector operand) {
        naCheck.enable(operand);
        double[] ddata = new double[operand.getLength()];
        for (int i = 0; i < operand.getLength(); i++) {
            int value = operand.getDataAt(i);
            ddata[i] = naCheck.convertIntToDouble(value);
        }
        if (preserveDimensions()) {
            return RDataFactory.createDoubleVector(ddata, operand.isComplete(), operand.getDimensions());
        } else if (preserveNames()) {
            return RDataFactory.createDoubleVector(ddata, operand.isComplete(), operand.getNames());
        } else {
            return RDataFactory.createDoubleVector(ddata, operand.isComplete());
        }
    }

}
