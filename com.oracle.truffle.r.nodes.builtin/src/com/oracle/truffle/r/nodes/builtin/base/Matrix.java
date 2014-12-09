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
import com.oracle.truffle.api.nodes.Node.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "matrix", kind = INTERNAL, parameterNames = {"data", "nrow", "ncol", "byrow", "dimnames", "missingNrow", "missingNcol"})
public abstract class Matrix extends RBuiltinNode {

    @Child private Transpose transpose;
    @Child private UpdateDimNames updateDimNames;

    private final BinaryConditionProfile nrowMissingNcolGiven = (BinaryConditionProfile) ConditionProfile.createBinaryProfile();
    private final BinaryConditionProfile nrowGivenNcolMissing = (BinaryConditionProfile) ConditionProfile.createBinaryProfile();
    private final BinaryConditionProfile bothNrowNcolMissing = (BinaryConditionProfile) ConditionProfile.createBinaryProfile();

    private RAbstractVector updateDimNames(VirtualFrame frame, RAbstractVector vector, Object o) {
        if (updateDimNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            updateDimNames = insert(UpdateDimNamesFactory.create(new RNode[2], getBuiltin(), getSuppliedArgsNames()));
        }
        return updateDimNames.executeList(frame, vector, o);
    }

    @CreateCast("arguments")
    protected RNode[] castArguments(RNode[] args) {
        // nrow/ncol, at positions 1/2, are cast to int
        // from nrow/ncol, if they are vectors, the first element must be extracted
        // byrow, at position 3, is cast to logical
        args[1] = FirstIntNodeFactory.create(CastIntegerNodeFactory.create(args[1], false, false, false));
        args[2] = FirstIntNodeFactory.create(CastIntegerNodeFactory.create(args[2], false, false, false));
        args[3] = CastLogicalNodeFactory.create(args[3], false, false, false);
        return args;
    }

    @Specialization(guards = "!byRow")
    @SuppressWarnings("unused")
    protected RAbstractVector matrixbc(RAbstractVector data, int nrow, int ncol, byte byrow, RNull dimnames, byte missingNr, byte missingNc) {
        controlVisibility();
        int[] dim = computeDimByCol(data.getLength(), nrow, ncol, missingNr, missingNc);
        return data.copyResizedWithDimensions(dim);
    }

    @Specialization(guards = "!byRow")
    @SuppressWarnings("unused")
    protected RAbstractVector matrixbc(VirtualFrame frame, RAbstractVector data, int nrow, int ncol, byte byrow, RList dimnames, byte missingNr, byte missingNc) {
        int[] dim = computeDimByCol(data.getLength(), nrow, ncol, missingNr, missingNc);
        RAbstractVector res = data.copyResizedWithDimensions(dim);
        res = updateDimNames(frame, res, dimnames);
        controlVisibility();
        return res;
    }

    @Specialization(guards = "byRow")
    @SuppressWarnings("unused")
    protected RAbstractVector matrixbr(VirtualFrame frame, RAbstractVector data, int nrow, int ncol, byte byrow, RNull dimnames, byte missingNr, byte missingNc) {
        controlVisibility();
        int[] dim = computeDimByRow(data.getLength(), nrow, ncol, missingNr, missingNc);
        if (transpose == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            transpose = insert(TransposeFactory.create(new RNode[1], getBuiltin(), getSuppliedArgsNames()));
        }
        return (RAbstractVector) transpose.execute(frame, data.copyResizedWithDimensions(dim));
    }

    @Specialization(guards = "byRow")
    @SuppressWarnings("unused")
    protected RAbstractVector matrixbr(VirtualFrame frame, RAbstractVector data, int nrow, int ncol, byte byrow, RList dimnames, byte missingNr, byte missingNc) {
        int[] dim = computeDimByRow(data.getLength(), nrow, ncol, missingNr, missingNc);
        if (transpose == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            transpose = insert(TransposeFactory.create(new RNode[1], getBuiltin(), getSuppliedArgsNames()));
        }
        RAbstractVector res = (RVector) transpose.execute(frame, data.copyResizedWithDimensions(dim));
        res = updateDimNames(frame, res, dimnames);
        controlVisibility();
        return res;
    }

    //
    // Auxiliary methods.
    //

    private int[] computeDimByCol(int size, int nrow, int ncol, byte missingNr, byte missingNc) {
        final boolean mnr = missingNr == RRuntime.LOGICAL_TRUE;
        final boolean mnc = missingNc == RRuntime.LOGICAL_TRUE;
        if (bothNrowNcolMissing.profile(mnr && mnc)) {
            return new int[]{size, 1};
        } else if (nrowGivenNcolMissing.profile(!mnr && mnc)) {
            if (nrow == 0 && size > 0) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NROW_ZERO);
            }
            return new int[]{nrow, 1 + ((size - 1) / nrow)};
        } else if (nrowMissingNcolGiven.profile(mnr && !mnc)) {
            if (ncol == 0 && size > 0) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NCOL_ZERO);
            }
            return new int[]{1 + ((size - 1) / ncol), ncol};
        } else {
            // only missing case: both nrow and ncol were given
            return new int[]{nrow, ncol};
        }
    }

    private int[] computeDimByRow(int size, int nrow, int ncol, byte missingNr, byte missingNc) {
        final boolean mnr = missingNr == RRuntime.LOGICAL_TRUE;
        final boolean mnc = missingNc == RRuntime.LOGICAL_TRUE;
        if (bothNrowNcolMissing.profile(mnr && mnc)) {
            return new int[]{1, size};
        } else if (nrowGivenNcolMissing.profile(!mnr && mnc)) {
            if (nrow == 0 && size > 0) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NROW_ZERO);
            }
            return new int[]{1 + ((size - 1) / nrow), nrow};
        } else if (nrowMissingNcolGiven.profile(mnr && !mnc)) {
            if (ncol == 0 && size > 0) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.NCOL_ZERO);
            }
            return new int[]{ncol, 1 + ((size - 1) / ncol)};
        } else {
            // only missing case: both nrow and ncol were given
            return new int[]{ncol, nrow};
        }
    }

    //
    // Guards.
    //

    @SuppressWarnings("unused")
    protected static boolean byRow(RAbstractVector data, int nrow, int ncol, byte byrow) {
        return byrow == RRuntime.LOGICAL_TRUE;
    }

}
