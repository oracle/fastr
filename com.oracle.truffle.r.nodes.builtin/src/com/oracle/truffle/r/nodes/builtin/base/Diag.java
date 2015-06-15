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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin(name = "diag", kind = SUBSTITUTE, parameterNames = {"x", "nrow", "ncol"})
// TODO INTERNAL
@SuppressWarnings("unused")
public abstract class Diag extends RBuiltinNode {

    private NACheck check = NACheck.create();

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{1, RMissing.instance, RMissing.instance};
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toInteger(1);
        casts.toInteger(2);
    }

    @Specialization
    protected RNull dim(RNull vector, int rows, int cols) {
        controlVisibility();
        return RNull.instance;
    }

    @Specialization
    protected RIntVector dim(int val, int rows, int cols) {
        controlVisibility();
        int[] data = new int[rows * cols];
        for (int i = 0; i < Math.min(cols, rows); i++) {
            data[i * rows + i] = val;
        }
        return RDataFactory.createIntVector(data, RDataFactory.COMPLETE_VECTOR, new int[]{rows, cols});
    }

    @Specialization
    protected RIntVector dim(int val, int rows, RMissing cols) {
        return dim(val, rows, rows);
    }

    @Specialization
    protected RDoubleVector dim(double val, int rows, int cols) {
        controlVisibility();
        double[] data = new double[rows * cols];
        for (int i = 0; i < Math.min(cols, rows); i++) {
            data[i * rows + i] = val;
        }
        return RDataFactory.createDoubleVector(data, RDataFactory.COMPLETE_VECTOR, new int[]{rows, cols});
    }

    @Specialization
    protected RDoubleVector dim(double val, int rows, RMissing cols) {
        return dim(val, rows, rows);
    }

    @Specialization
    protected RLogicalVector dim(byte val, int rows, int cols) {
        controlVisibility();
        byte[] data = new byte[rows * cols];
        for (int i = 0; i < Math.min(cols, rows); i++) {
            data[i * rows + i] = val;
        }
        return RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR, new int[]{rows, cols});
    }

    @Specialization
    protected RLogicalVector dim(byte val, int rows, RMissing cols) {
        return dim(val, rows, rows);
    }

    @Specialization(guards = "isMatrix(vector)")
    protected RIntVector dimWithDimensions(RIntVector vector, Object rows, Object cols) {
        controlVisibility();
        int size = Math.min(vector.getDimensions()[0], vector.getDimensions()[1]);
        int[] result = new int[size];
        int nrow = vector.getDimensions()[0];
        int pos = 0;
        check.enable(vector);
        for (int i = 0; i < size; i++) {
            int value = vector.getDataAt(pos);
            check.check(value);
            result[i] = value;
            pos += nrow + 1;
        }
        return RDataFactory.createIntVector(result, check.neverSeenNA());
    }

    @Specialization(guards = "isMatrix(vector)")
    protected RDoubleVector dimWithDimensions(RDoubleVector vector, Object rows, Object cols) {
        controlVisibility();
        int size = Math.min(vector.getDimensions()[0], vector.getDimensions()[1]);
        double[] result = new double[size];
        int nrow = vector.getDimensions()[0];
        int pos = 0;
        check.enable(vector);
        for (int i = 0; i < size; i++) {
            double value = vector.getDataAt(pos);
            check.check(value);
            result[i] = value;
            pos += nrow + 1;
        }
        return RDataFactory.createDoubleVector(result, check.neverSeenNA());
    }

    public static boolean isMatrix(RIntVector vector) {
        return vector.hasDimensions() && vector.getDimensions().length == 2;
    }

    public static boolean isMatrix(RDoubleVector vector) {
        return vector.hasDimensions() && vector.getDimensions().length == 2;
    }
}
