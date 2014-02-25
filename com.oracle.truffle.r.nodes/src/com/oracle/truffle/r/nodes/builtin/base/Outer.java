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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin("outer")
public abstract class Outer extends RBuiltinNode {

    private static final Object[] PARAMETER_NAMES = new Object[]{"x", "y", "FUN"};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create("*")};
    }

    @SuppressWarnings("unused")
    @Specialization(order = 1, guards = "functionIsPlus")
    public RIntVector outerPlus(RAbstractIntVector x, RAbstractIntVector y, String function) {
        int[] dimX = x.getDimensions();
        int[] dimY = y.getDimensions();
        int[] result = new int[dimProd(dimX) * dimProd(dimY)];

        int pos = 0;
        for (int b = 0; b < dimProd(dimY); b++) {
            int valueY = y.getDataAt(b);
            for (int a = 0; a < dimProd(dimX); a++) {
                int valueX = x.getDataAt(a);
                result[pos] = valueX + valueY;
                pos++;
            }
        }
        int[] dimResult = new int[dimX.length + dimY.length];
        System.arraycopy(dimX, 0, dimResult, 0, dimX.length);
        System.arraycopy(dimY, 0, dimResult, dimX.length, dimY.length);
        return RDataFactory.createIntVector(result, true, dimResult);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 20, guards = "functionIsMinus")
    public RIntVector outerMinus(RAbstractIntVector x, RAbstractIntVector y, String function) {
        int[] dimX = x.getDimensions();
        int[] dimY = y.getDimensions();
        int[] result = new int[dimProd(dimX) * dimProd(dimY)];

        int pos = 0;
        for (int b = 0; b < dimProd(dimY); b++) {
            int valueY = y.getDataAt(b);
            for (int a = 0; a < dimProd(dimX); a++) {
                int valueX = x.getDataAt(a);
                result[pos] = valueX - valueY;
                pos++;
            }
        }
        int[] dimResult = new int[dimX.length + dimY.length];
        System.arraycopy(dimX, 0, dimResult, 0, dimX.length);
        System.arraycopy(dimY, 0, dimResult, dimX.length, dimY.length);
        return RDataFactory.createIntVector(result, true, dimResult);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 21, guards = "functionIsMinus")
    public RDoubleVector outerMinus(RAbstractDoubleVector x, RAbstractDoubleVector y, String function) {
        int[] dimX = x.getDimensions();
        int[] dimY = y.getDimensions();
        int lengthX = dimX == null ? x.getLength() : dimProd(dimX);
        int lengthY = dimY == null ? y.getLength() : dimProd(dimY);
        double[] result = new double[lengthX * lengthY];

        int pos = 0;
        for (int b = 0; b < lengthY; b++) {
            double valueY = y.getDataAt(b);
            for (int a = 0; a < lengthX; a++) {
                double valueX = x.getDataAt(a);
                result[pos] = valueX - valueY;
                pos++;
            }
        }
        int[] dimResult = new int[dimCount(x) + dimCount(y)];
        if (dimX != null) {
            System.arraycopy(dimX, 0, dimResult, 0, dimX.length);
        } else {
            dimResult[0] = lengthX;
        }
        if (dimY != null) {
            System.arraycopy(dimY, 0, dimResult, dimX.length, dimY.length);
        } else {
            dimResult[dimCount(x)] = lengthY;
        }
        return RDataFactory.createDoubleVector(result, true, dimResult);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 30, guards = "functionIsLt")
    public RLogicalVector outerLt(RAbstractDoubleVector x, RAbstractDoubleVector y, String function) {
        int[] dimX = x.getDimensions();
        int[] dimY = y.getDimensions();
        int lengthX = dimX == null ? x.getLength() : dimProd(dimX);
        int lengthY = dimY == null ? y.getLength() : dimProd(dimY);
        byte[] result = new byte[lengthX * lengthY];

        int pos = 0;
        for (int b = 0; b < lengthY; b++) {
            double valueY = y.getDataAt(b);
            for (int a = 0; a < lengthX; a++) {
                double valueX = x.getDataAt(a);
                result[pos] = (valueX < valueY) ? RRuntime.LOGICAL_TRUE : RRuntime.LOGICAL_FALSE;
                pos++;
            }
        }
        int[] dimResult = new int[dimCount(x) + dimCount(y)];
        if (dimX != null) {
            System.arraycopy(dimX, 0, dimResult, 0, dimX.length);
        } else {
            dimResult[0] = lengthX;
        }
        if (dimY != null) {
            System.arraycopy(dimY, 0, dimResult, dimX.length, dimY.length);
        } else {
            dimResult[dimCount(x)] = lengthY;
        }
        return RDataFactory.createLogicalVector(result, true, dimResult);
    }

    @SuppressWarnings("unused")
    @Specialization(order = 41, guards = "functionIsMult")
    public RDoubleVector outerMult(RAbstractDoubleVector x, RAbstractDoubleVector y, String function) {
        return outerMultImpl(x, y);
    }

    static RDoubleVector outerMultImpl(RAbstractDoubleVector x, RAbstractDoubleVector y) {
        int[] dimX = x.getDimensions();
        int[] dimY = y.getDimensions();
        int lengthX = dimX == null ? x.getLength() : dimProd(dimX);
        int lengthY = dimY == null ? y.getLength() : dimProd(dimY);
        double[] result = new double[lengthX * lengthY];

        int pos = 0;
        for (int b = 0; b < lengthY; b++) {
            double valueY = y.getDataAt(b);
            for (int a = 0; a < lengthX; a++) {
                double valueX = x.getDataAt(a);
                result[pos] = valueX * valueY;
                pos++;
            }
        }
        int[] dimResult = new int[dimCount(x) + dimCount(y)];
        if (dimX != null) {
            System.arraycopy(dimX, 0, dimResult, 0, dimX.length);
        } else {
            dimResult[0] = lengthX;
        }
        if (dimY != null) {
            System.arraycopy(dimY, 0, dimResult, dimX.length, dimY.length);
        } else {
            dimResult[dimCount(x)] = lengthY;
        }
        return RDataFactory.createDoubleVector(result, true, dimResult);
    }

    private static int dimCount(RAbstractVector vector) {
        return vector.getDimensions() != null ? vector.getDimensions().length : 1;
    }

    private static int dimProd(int[] dim) {
        int prod = 1;
        for (int i = 0; i < dim.length; i++) {
            prod *= dim[i];
        }
        return prod;
    }

    @SuppressWarnings("unused")
    protected boolean functionIsPlus(RAbstractVector x, RAbstractVector y, String function) {
        return function.equals("+");
    }

    @SuppressWarnings("unused")
    protected boolean functionIsMinus(RAbstractVector x, RAbstractVector y, String function) {
        return function.equals("-");
    }

    @SuppressWarnings("unused")
    protected boolean functionIsMult(RAbstractVector x, RAbstractVector y, String function) {
        return function.equals("*");
    }

    @SuppressWarnings("unused")
    protected boolean functionIsLt(RAbstractVector x, RAbstractVector y, String function) {
        return function.equals("<");
    }

}
