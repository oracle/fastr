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
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin("matrix")
public abstract class Matrix extends RBuiltinNode {

    private static final String[] PARAMETER_NAMES = new String[]{"data", "nrow", "ncol", "byrow", "dimnames"};

    @Child private Transpose transpose = TransposeFactory.create(new RNode[1], getBuiltin());

    @Override
    public Object[] getParameterNames() {
        return PARAMETER_NAMES;
    }

    @Override
    public RNode[] getParameterValues() {
        // illegal defaults (-1) are given for nrow and ncol; they are handled specially if missing
        return new RNode[]{ConstantNode.create(RRuntime.LOGICAL_NA), ConstantNode.create(-1), ConstantNode.create(-1), ConstantNode.create(RRuntime.LOGICAL_FALSE), ConstantNode.create(RNull.instance)};
    }

    @CreateCast("arguments")
    public RNode[] castArguments(RNode[] arguments) {
        // nrow and ncol (positions 1, 2) are actually int
        arguments[1] = CastIntegerNodeFactory.create(arguments[1], true, false);
        arguments[2] = CastIntegerNodeFactory.create(arguments[2], true, false);
        return arguments;
    }

    protected static int[] computeNrowNcol(RAbstractVector v, int nrowp, int ncolp) {
        int[] nrowncol = new int[2];
        if (nrowp == -1 && ncolp >= 0) {
            nrowncol[0] = v.getLength() / ncolp;
            nrowncol[1] = ncolp;
        } else if (nrowp >= 0 && ncolp == -1) {
            nrowncol[0] = nrowp;
            nrowncol[1] = v.getLength() / nrowp;
        } else if (nrowp >= 0 && ncolp >= 0) {
            nrowncol[0] = nrowp;
            nrowncol[1] = ncolp;
        } else {
            nrowncol[0] = v.getLength();
            nrowncol[1] = 1;
        }
        return nrowncol;
    }

    @Specialization(guards = "isByRow")
    public Object matrixByRow(VirtualFrame frame, RAbstractVector data, int nrowp, int ncolp, @SuppressWarnings("unused") byte byrow, @SuppressWarnings("unused") RNull dimnames) {
        RVector vdata = data.materialize().copy();
        int[] nrowncol = computeNrowNcol(vdata, ncolp, nrowp);
        return transpose.execute(frame, vdata.copyResized(nrowncol[0] * nrowncol[1], false).copyWithNewDimensions(nrowncol));
    }

    @Specialization(guards = "!isByRow")
    public RAbstractVector matrix(@SuppressWarnings("unused") VirtualFrame frame, RAbstractVector data, int nrowp, int ncolp, @SuppressWarnings("unused") byte byrow,
                    @SuppressWarnings("unused") RNull dimnames) {
        RVector vdata = data.materialize().copy();
        int[] nrowncol = computeNrowNcol(vdata, nrowp, ncolp);
        return vdata.copyResized(nrowncol[0] * nrowncol[1], false).copyWithNewDimensions(nrowncol);
    }

    @SuppressWarnings("unused")
    public static boolean isByRow(RAbstractVector data, int nrowp, int ncolp, byte byrow, RNull dimnames) {
        return byrow == RRuntime.LOGICAL_TRUE;
    }
}
