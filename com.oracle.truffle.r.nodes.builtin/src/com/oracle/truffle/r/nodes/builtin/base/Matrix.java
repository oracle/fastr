/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "matrix", kind = INTERNAL, parameterNames = {"data", "nrow", "ncol", "isTrue(byrow)", "dimnames", "missingNrow", "missingNcol"})
public abstract class Matrix extends RBuiltinNode {

    @Child private Transpose transpose;
    @Child private UpdateDimNames updateDimNames;

    private final ConditionProfile nrowMissingNcolGiven = ConditionProfile.createBinaryProfile();
    private final ConditionProfile nrowGivenNcolMissing = ConditionProfile.createBinaryProfile();
    private final ConditionProfile bothNrowNcolMissing = ConditionProfile.createBinaryProfile();
    private final ConditionProfile empty = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isList = ConditionProfile.createBinaryProfile();

    public abstract RAbstractVector execute(RAbstractVector data, int nrow, int ncol, byte byrow, Object dimnames, byte missingNr, byte missingNc);

    private RAbstractVector updateDimNames(RAbstractVector vector, Object o) {
        if (updateDimNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            updateDimNames = insert(UpdateDimNamesNodeGen.create(null));
        }
        return (RAbstractVector) updateDimNames.executeRAbstractContainer(vector, o);
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.firstIntegerWithError(1, RError.Message.NON_NUMERIC_MATRIX_EXTENT, null); // nrow
        casts.firstIntegerWithError(2, RError.Message.NON_NUMERIC_MATRIX_EXTENT, null); // ncol
        casts.toLogical(3); // byrow
    }

    @Specialization(guards = "!isTrue(byrow)")
    @SuppressWarnings("unused")
    protected RAbstractVector matrixbc(RAbstractVector data, int nrow, int ncol, byte byrow, RNull dimnames, byte missingNr, byte missingNc) {
        controlVisibility();
        int[] dim = computeDimByCol(data.getLength(), nrow, ncol, missingNr, missingNc);
        if (empty.profile(data.getLength() == 0)) {
            if (isList.profile(data instanceof RList)) {
                // matrix of NULL-s
                return data.copyResizedWithDimensions(dim, true);
            } else {
                RVector res = data.createEmptySameType(0, RDataFactory.COMPLETE_VECTOR);
                res.setDimensions(dim);
                return res;
            }
        } else {
            return data.copyResizedWithDimensions(dim, false);
        }
    }

    @Specialization(guards = "!isTrue(byrow)")
    @SuppressWarnings("unused")
    protected RAbstractVector matrixbc(RAbstractVector data, int nrow, int ncol, byte byrow, RList dimnames, byte missingNr, byte missingNc) {
        int[] dim = computeDimByCol(data.getLength(), nrow, ncol, missingNr, missingNc);
        RAbstractVector res;
        if (empty.profile(data.getLength() == 0)) {
            if (isList.profile(data instanceof RList)) {
                // matrix of NULL-s
                res = data.copyResizedWithDimensions(dim, true);
                res = updateDimNames(res, dimnames);
            } else {
                res = data.createEmptySameType(0, RDataFactory.COMPLETE_VECTOR);
                res.setDimensions(dim);
            }
        } else {
            res = data.copyResizedWithDimensions(dim, false);
            res = updateDimNames(res, dimnames);
        }
        controlVisibility();
        return res;
    }

    @Specialization(guards = "isTrue(byrow)")
    @SuppressWarnings("unused")
    protected RAbstractVector matrixbr(RAbstractVector data, int nrow, int ncol, byte byrow, RNull dimnames, byte missingNr, byte missingNc) {
        controlVisibility();
        int[] dim = computeDimByRow(data.getLength(), nrow, ncol, missingNr, missingNc);
        if (transpose == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            transpose = insert(TransposeNodeGen.create(null));
        }
        RAbstractVector res;
        if (empty.profile(data.getLength() == 0)) {
            if (isList.profile(data instanceof RList)) {
                // matrix of NULL-s
                res = data.copyResizedWithDimensions(dim, true);
            } else {
                res = data.createEmptySameType(0, RDataFactory.COMPLETE_VECTOR);
                res.setDimensions(dim);
            }
        } else {
            res = data.copyResizedWithDimensions(dim, false);
        }
        return (RAbstractVector) transpose.execute(res);
    }

    @Specialization(guards = "isTrue(byrow)")
    @SuppressWarnings("unused")
    protected RAbstractVector matrixbr(RAbstractVector data, int nrow, int ncol, byte byrow, RList dimnames, byte missingNr, byte missingNc) {
        int[] dim = computeDimByRow(data.getLength(), nrow, ncol, missingNr, missingNc);
        if (transpose == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            transpose = insert(TransposeNodeGen.create(null));
        }
        RAbstractVector res;
        if (empty.profile(data.getLength() == 0)) {
            if (isList.profile(data instanceof RList)) {
                // matrix of NULL-s
                res = (RVector) transpose.execute(data.copyResizedWithDimensions(dim, true));
                res = updateDimNames(res, dimnames);
            } else {
                res = data.createEmptySameType(0, RDataFactory.COMPLETE_VECTOR);
                res.setDimensions(dim);
            }
        } else {
            res = (RVector) transpose.execute(data.copyResizedWithDimensions(dim, false));
            res = updateDimNames(res, dimnames);
        }
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
                throw RError.error(this, RError.Message.NROW_ZERO);
            }
            if (empty.profile(size == 0)) {
                return new int[]{nrow, 0};
            } else {
                return new int[]{nrow, 1 + ((size - 1) / nrow)};
            }
        } else if (nrowMissingNcolGiven.profile(mnr && !mnc)) {
            if (ncol == 0 && size > 0) {
                throw RError.error(this, RError.Message.NCOL_ZERO);
            }
            if (empty.profile(size == 0)) {
                return new int[]{0, ncol};
            } else {
                return new int[]{1 + ((size - 1) / ncol), ncol};
            }
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
                throw RError.error(this, RError.Message.NROW_ZERO);
            }
            if (empty.profile(size == 0)) {
                return new int[]{0, nrow};
            } else {

                return new int[]{1 + ((size - 1) / nrow), nrow};
            }
        } else if (nrowMissingNcolGiven.profile(mnr && !mnc)) {
            if (ncol == 0 && size > 0) {
                throw RError.error(this, RError.Message.NCOL_ZERO);
            }
            if (empty.profile(size == 0)) {
                return new int[]{ncol, 0};
            } else {
                return new int[]{ncol, 1 + ((size - 1) / ncol)};
            }
        } else {
            // only missing case: both nrow and ncol were given
            return new int[]{ncol, nrow};
        }
    }

    //
    // Guards.
    //

    protected static boolean isTrue(byte byrow) {
        return RRuntime.fromLogical(byrow);
    }
}
