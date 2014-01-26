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
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin("diag")
@SuppressWarnings("unused")
public abstract class Diag extends RBuiltinNode {

    private NACheck check = NACheck.create();

    @Specialization
    public RNull dim(RNull vector) {
        return RNull.instance;
    }

    @Specialization
    public RNull dim(int vector) {
        return RNull.instance;
    }

    @Specialization
    public RNull dim(double vector) {
        return RNull.instance;
    }

    @Specialization
    public RNull dim(byte vector) {
        return RNull.instance;
    }

    @Specialization(guards = "isMatrix")
    public RIntVector dimWithDimensions(RIntVector vector) {
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

    @Specialization(guards = "isMatrix")
    public RDoubleVector dimWithDimensions(RDoubleVector vector) {
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

    public static boolean isMatrix(RAbstractVector vector) {
        return vector.hasDimensions() && vector.getDimensions().length == 2;
    }
}
