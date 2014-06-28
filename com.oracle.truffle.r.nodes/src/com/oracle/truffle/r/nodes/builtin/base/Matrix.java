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
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * <pre>
 * matrix &lt; -function(data = NA, nrow = 1, ncol = 1, byrow = FALSE, dimnames = NULL)
 * </pre>
 *
 * TODO rework as {@code .Internal} to use the wrapper in {@code matrix.R}.
 */
@RBuiltin(name = "matrix", kind = SUBSTITUTE)
// TODO INTERNAL
public abstract class Matrix extends RBuiltinNode {

    private static final String[] PARAMETER_NAMES = new String[]{"data", "nrow", "ncol", "byrow", "dimnames", "missingNrow", "missingNcol"};

    @Child private Transpose transpose = TransposeFactory.create(new RNode[1], getBuiltin());
    @Child private IsNumeric isNumeric = IsNumericFactory.create(new RNode[1], getBuiltin());
    @Child private CastIntegerNode castIntNode;

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RRuntime.LOGICAL_NA), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RRuntime.LOGICAL_FALSE),
                        ConstantNode.create(RNull.instance), ConstantNode.create(RRuntime.LOGICAL_TRUE), ConstantNode.create(RRuntime.LOGICAL_TRUE)};
    }

    @CreateCast("arguments")
    public RNode[] castArguments(RNode[] arguments) {
        arguments[3] = CastLogicalNodeFactory.create(arguments[3], true, false, false);
        return arguments;
    }

    @Specialization(guards = "isByRow", order = 12)
    @SuppressWarnings("unused")
    public Object matrixByRow(VirtualFrame frame, RAbstractVector data, RAbstractVector nrowp, RAbstractVector ncolp, byte byrow, RNull dimnames, byte missingNr, byte missingNc) {
        controlVisibility();
        RVector vdata = data.materialize().copy();
        int[] nrowncol = computeNrowNcol(frame, data, nrowp, ncolp);
        int[] rowColByRow = new int[]{nrowncol[1], nrowncol[0]};
        return transpose.execute(frame, vdata.copyResized(nrowncol[0] * nrowncol[1], false).copyWithNewDimensions(rowColByRow));
    }

    @Specialization(guards = "isByRow", order = 10)
    @SuppressWarnings("unused")
    public Object matrixByRow(VirtualFrame frame, RAbstractVector data, RMissing nrowp, RAbstractVector ncolp, byte byrow, RNull dimnames, byte missingNr, byte missingNc) {
        controlVisibility();
        RVector vdata = data.materialize().copy();
        int[] nrowncol = computeNrowNcol(frame, data, nrowp, ncolp);
        int[] rowColByRow = new int[]{nrowncol[1], nrowncol[0]};
        return transpose.execute(frame, vdata.copyResized(nrowncol[0] * nrowncol[1], false).copyWithNewDimensions(rowColByRow));
    }

    @Specialization(guards = "isByRow", order = 8)
    @SuppressWarnings("unused")
    public Object matrixByRow(VirtualFrame frame, RAbstractVector data, RAbstractVector nrowp, RMissing ncolp, byte byrow, RNull dimnames, byte missingNr, byte missingNc) {
        controlVisibility();
        RVector vdata = data.materialize().copy();
        int[] nrowncol = computeNrowNcol(frame, data, nrowp, ncolp);
        int[] rowColByRow = new int[]{nrowncol[1], nrowncol[0]};
        return transpose.execute(frame, vdata.copyResized(nrowncol[0] * nrowncol[1], false).copyWithNewDimensions(rowColByRow));
    }

    @Specialization(guards = "isByRow", order = 6)
    @SuppressWarnings("unused")
    public Object matrixByRow(VirtualFrame frame, RAbstractVector data, RMissing nrowp, RMissing ncolp, byte byrow, RNull dimnames, byte missingNr, byte missingNc) {
        controlVisibility();
        RVector vdata = data.materialize().copy();
        int[] nrowncol = computeNrowNcol(frame, data, nrowp, ncolp);
        int[] rowColByRow = new int[]{nrowncol[1], nrowncol[0]};
        return transpose.execute(frame, vdata.copyResized(nrowncol[0] * nrowncol[1], false).copyWithNewDimensions(rowColByRow));
    }

    @Specialization(guards = "!isByRow", order = 4)
    @SuppressWarnings("unused")
    public RAbstractVector matrix(VirtualFrame frame, RAbstractVector data, RAbstractVector nrowp, RAbstractVector ncolp, byte byrow, RNull dimnames, byte missingNr, byte missingNc) {
        controlVisibility();
        RVector vdata = data.materialize().copy();
        int[] nrowncol = computeNrowNcol(frame, data, nrowp, ncolp);
        return vdata.copyResized(nrowncol[0] * nrowncol[1], false).copyWithNewDimensions(nrowncol);
    }

    @Specialization(guards = "!isByRow", order = 2)
    @SuppressWarnings("unused")
    public RAbstractVector matrix(VirtualFrame frame, RAbstractVector data, RMissing nrowp, RAbstractVector ncolp, byte byrow, RNull dimnames, byte missingNr, byte missingNc) {
        controlVisibility();
        RVector vdata = data.materialize().copy();
        int[] nrowncol = computeNrowNcol(frame, data, nrowp, ncolp);
        return vdata.copyResized(nrowncol[0] * nrowncol[1], false).copyWithNewDimensions(nrowncol);
    }

    @Specialization(guards = "!isByRow", order = 1)
    @SuppressWarnings("unused")
    public RAbstractVector matrix(VirtualFrame frame, RAbstractVector data, RAbstractVector nrowp, RMissing ncolp, byte byrow, RNull dimnames, byte missingNr, byte missingNc) {
        controlVisibility();
        RVector vdata = data.materialize().copy();
        int[] nrowncol = computeNrowNcol(frame, data, nrowp, ncolp);
        return vdata.copyResized(nrowncol[0] * nrowncol[1], false).copyWithNewDimensions(nrowncol);
    }

    @Specialization(guards = "!isByRow", order = 0)
    @SuppressWarnings("unused")
    public RAbstractVector matrix(VirtualFrame frame, RAbstractVector data, RMissing nrowp, RMissing ncolp, byte byrow, RNull dimnames, byte missingNr, byte missingNc) {
        controlVisibility();
        RVector vdata = data.materialize().copy();
        int[] nrowncol = computeNrowNcol(frame, data, nrowp, ncolp);
        return vdata.copyResized(nrowncol[0] * nrowncol[1], false).copyWithNewDimensions(nrowncol);
    }

    @SuppressWarnings("unused")
    public static boolean isByRow(RAbstractVector data, RAbstractVector nrowp, RAbstractVector ncolp, byte byrow, RNull dimnames, byte missingNr, byte missingNc) {
        return byrow == RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    public static boolean isByRow(RAbstractVector data, RMissing nrowp, RAbstractVector ncolp, byte byrow, RNull dimnames, byte missingNr, byte missingNc) {
        return byrow == RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    public static boolean isByRow(RAbstractVector data, RAbstractVector nrowp, RMissing ncolp, byte byrow, RNull dimnames, byte missingNr, byte missingNc) {
        return byrow == RRuntime.LOGICAL_TRUE;
    }

    @SuppressWarnings("unused")
    public static boolean isByRow(RAbstractVector data, RMissing nrowp, RMissing ncolp, byte byrow, RNull dimnames, byte missingNr, byte missingNc) {
        return byrow == RRuntime.LOGICAL_TRUE;
    }

    private int getValue(VirtualFrame frame, RAbstractVector arg) {
        if (isNumeric.execute(frame, arg) == RRuntime.LOGICAL_FALSE) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NON_NUMERIC_MATRIX_EXTENT);
        }
        if (castIntNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castIntNode = insert(CastIntegerNodeFactory.create(null, false, false, false));
        }
        Object value = castIntNode.executeCast(frame, arg);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof RIntVector) {
            return ((RIntVector) value).getDataAt(0);
        }
        if (value instanceof RIntSequence) {
            return ((RIntSequence) value).getDataAt(0);
        }
        return RRuntime.INT_NA;
    }

    private int getNrow(VirtualFrame frame, RAbstractVector vecRow) {
        int nRow = getValue(frame, vecRow);
        if (nRow == RRuntime.INT_NA) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_NROW);
        }
        if (nRow < 0) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NEGATIVE_NROW);
        }
        return nRow;
    }

    private int getNcol(VirtualFrame frame, RAbstractVector vecCol) {
        int nCol = getValue(frame, vecCol);
        if (nCol == RRuntime.INT_NA) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_NCOL);
        }
        if (nCol < 0) {
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.NEGATIVE_NCOL);
        }
        return nCol;
    }

    @SuppressWarnings("unused")
    private int[] computeNrowNcol(VirtualFrame frame, RAbstractVector x, RMissing nrowp, RAbstractVector ncolp) {
        int xLen = x.getLength();
        int nRow = 0;
        int nCol = getNcol(frame, ncolp);
        if (nCol == 0) {
            if (xLen > 0) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NCOL_ZERO);
            } else {
                nRow = 0;
            }
        } else {
            nRow = 1 + ((xLen - 1) / nCol); // nRow = ceiling(xLen/nCol)
        }
        return new int[]{nRow, nCol};
    }

    @SuppressWarnings("unused")
    private int[] computeNrowNcol(VirtualFrame frame, RAbstractVector x, RAbstractVector nrowp, RMissing ncolp) {
        int xLen = x.getLength();
        int nCol = 0;
        int nRow = getNrow(frame, nrowp);
        if (nRow == 0) {
            if (xLen > 0) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NROW_ZERO);
            } else {
                nCol = 0;
            }
        } else {
            nCol = 1 + ((xLen - 1) / nRow);
        }
        return new int[]{nRow, nCol};
    }

    @SuppressWarnings("unused")
    private static int[] computeNrowNcol(VirtualFrame frame, RAbstractVector x, RMissing nrowp, RMissing ncolp) {
        return new int[]{x.getLength(), 1};
    }

    @SuppressWarnings("unused")
    private int[] computeNrowNcol(VirtualFrame frame, RAbstractVector x, RAbstractVector nrowp, RAbstractVector ncolp) {
        return new int[]{getValue(frame, nrowp), getValue(frame, ncolp)};
    }
}
