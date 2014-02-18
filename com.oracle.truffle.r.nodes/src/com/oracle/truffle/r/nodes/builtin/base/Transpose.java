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
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin("t")
@SuppressWarnings("unused")
public abstract class Transpose extends RBuiltinNode {

    private final NACheck check = NACheck.create();

    public abstract Object execute(VirtualFrame frame, Object o);

    @Specialization
    public RNull transpose(RNull value) {
        return value;
    }

    @Specialization
    public int transpose(int value) {
        return value;
    }

    @Specialization
    public double transpose(double value) {
        return value;
    }

    @Specialization
    public byte transpose(byte value) {
        return value;
    }

    @Specialization
    public RIntVector transpose(RAbstractIntVector vector) {
        return performAbstractIntVector(vector, vector.isMatrix() ? vector.getDimensions() : new int[]{vector.getLength(), 1});
    }

    private static RIntVector performAbstractIntVector(RAbstractIntVector vector, int[] dim) {
        int firstDim = dim[0];
        int secondDim = dim[1];
        int[] result = new int[vector.getLength()];
        int pos = 0;
        int pos2 = 0;
        for (int y = 0; y < secondDim; y++) {
            pos2 = y;
            for (int x = 0; x < firstDim; x++) {
                int value = vector.getDataAt(pos++);
                result[pos2] = value;
                pos2 += secondDim;
            }
        }
        int[] newDim = new int[]{secondDim, firstDim};
        RIntVector r = RDataFactory.createIntVector(result, vector.isComplete());
        r.copyAttributesFrom(vector);
        r.setDimensions(newDim);
        return r;
    }

    @Specialization
    public RDoubleVector transpose(RAbstractDoubleVector vector) {
        return performAbstractDoubleVector(vector, vector.isMatrix() ? vector.getDimensions() : new int[]{vector.getLength(), 1});
    }

    private static RDoubleVector performAbstractDoubleVector(RAbstractDoubleVector vector, int[] dim) {
        int firstDim = dim[0];
        int secondDim = dim[1];
        double[] result = new double[vector.getLength()];
        int pos = 0;
        int pos2 = 0;
        for (int y = 0; y < secondDim; y++) {
            pos2 = y;
            for (int x = 0; x < firstDim; x++) {
                double value = vector.getDataAt(pos++);
                result[pos2] = value;
                pos2 += secondDim;
            }
        }
        int[] newDim = new int[]{secondDim, firstDim};
        RDoubleVector r = RDataFactory.createDoubleVector(result, vector.isComplete());
        r.copyAttributesFrom(vector);
        r.setDimensions(newDim);
        return r;
    }

    @Specialization
    public RStringVector transpose(RAbstractStringVector vector) {
        return performAbstractStringVector(vector, vector.isMatrix() ? vector.getDimensions() : new int[]{vector.getLength(), 1});
    }

    private static RStringVector performAbstractStringVector(RAbstractStringVector vector, int[] dim) {
        int firstDim = dim[0];
        int secondDim = dim[1];
        String[] result = new String[vector.getLength()];
        int pos = 0;
        int pos2 = 0;
        for (int y = 0; y < secondDim; y++) {
            pos2 = y;
            for (int x = 0; x < firstDim; x++) {
                String value = vector.getDataAt(pos++);
                result[pos2] = value;
                pos2 += secondDim;
            }
        }
        int[] newDim = new int[]{secondDim, firstDim};
        RStringVector r = RDataFactory.createStringVector(result, vector.isComplete());
        r.copyAttributesFrom(vector);
        r.setDimensions(newDim);
        return r;
    }
}
