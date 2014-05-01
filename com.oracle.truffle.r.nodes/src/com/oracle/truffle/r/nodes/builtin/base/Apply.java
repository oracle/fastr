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
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.RBuiltin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(value = "apply", lastParameterKind = LastParameterKind.VAR_ARGS_SPECIALIZE)
public abstract class Apply extends RBuiltinNode {

    private static final Object[] PARAMETER_NAMES = new Object[]{"X", "MARGIN", "FUN", "..."};

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @Specialization(order = 1, guards = "rowMargin")
    @SuppressWarnings("unused")
    public Object applyRows(VirtualFrame frame, RDoubleVector x, double margin, RFunction fun, Object args) {
        controlVisibility();
        int[] xdim = x.getDimensions();
        final int rows = xdim == null ? x.getLength() : xdim[0];
        final int cols = xdim == null ? 1 : xdim[1];
        Object[] result = new Object[rows];
        for (int row = 0; row < rows; ++row) {
            double[] rowData = new double[cols];
            for (int i = 0; i < cols; ++i) {
                rowData[i] = x.getDataAt(i * rows + row);
            }
            RDoubleVector rowVec = RDataFactory.createDoubleVector(rowData, RDataFactory.COMPLETE_VECTOR);
            result[row] = executeFunction(frame, fun, RDataFactory.createDoubleVector(rowData, RDataFactory.COMPLETE_VECTOR));
        }
        return RDataFactory.createObjectVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization(order = 2, guards = "colMargin")
    @SuppressWarnings("unused")
    public Object applyCols(VirtualFrame frame, RDoubleVector x, double margin, RFunction fun, Object args) {
        controlVisibility();
        int[] xdim = x.getDimensions();
        final int rows = xdim == null ? x.getLength() : xdim[0];
        final int cols = xdim == null ? 1 : xdim[1];
        Object[] result = new Object[cols];
        for (int col = 0; col < cols; ++col) {
            double[] colData = new double[rows];
            for (int i = 0; i < rows; ++i) {
                colData[i] = x.getDataAt(col * rows + i);
            }
            RDoubleVector colVec = RDataFactory.createDoubleVector(colData, RDataFactory.COMPLETE_VECTOR);
            result[col] = executeFunction(frame, fun, RDataFactory.createDoubleVector(colData, RDataFactory.COMPLETE_VECTOR));
        }
        return RDataFactory.createObjectVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    @SuppressWarnings("unused")
    protected static boolean rowMargin(RAbstractVector x, double margin, RFunction fun, Object args) {
        return margin == 1.0;
    }

    @SuppressWarnings("unused")
    protected static boolean colMargin(RAbstractVector x, double margin, RFunction fun, Object args) {
        return margin == 2.0;
    }

    private static Object executeFunction(VirtualFrame frame, RFunction fun, Object input) {
        Object[] args = RArguments.create(fun, new Object[]{input});
        return fun.call(frame, args);
    }

}
