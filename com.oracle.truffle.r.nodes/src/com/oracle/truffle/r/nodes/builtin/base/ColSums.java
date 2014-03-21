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
import com.oracle.truffle.r.runtime.ops.*;

@RBuiltin("colSums")
public abstract class ColSums extends RBuiltinNode {

    private static final String[] PARAMETER_NAMES = new String[]{"x", "na.rm", "dims"};

    @Override
    public String[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(1)};
    }

    @Child protected BinaryArithmetic add = BinaryArithmetic.ADD.create();

    // FIXME don't ignore na.rm and dims

    @Specialization
    public RDoubleVector colSums(RIntVector x, @SuppressWarnings("unused") byte narm, @SuppressWarnings("unused") int dims) {
        assert x.isMatrix();
        int rows = x.getDimensions()[0];
        int cols = x.getDimensions()[1];
        double[] r = new double[cols];
        for (int c = 0; c < cols; ++c) {
            int cs = 0;
            for (int i = 0; i < rows; ++i) {
                cs = add.op(cs, x.getDataAt(c * rows + i));
            }
            r[c] = cs;
        }
        return RDataFactory.createDoubleVector(r, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    public RDoubleVector colSums(RLogicalVector x, @SuppressWarnings("unused") byte narm, @SuppressWarnings("unused") int dims) {
        assert x.isMatrix();
        int rows = x.getDimensions()[0];
        int cols = x.getDimensions()[1];
        double[] r = new double[cols];
        for (int c = 0; c < cols; ++c) {
            int cs = 0;
            for (int i = 0; i < rows; ++i) {
                cs = add.op(cs, x.getDataAt(c * rows + i));
            }
            r[c] = cs;
        }
        return RDataFactory.createDoubleVector(r, RDataFactory.COMPLETE_VECTOR);
    }

}
