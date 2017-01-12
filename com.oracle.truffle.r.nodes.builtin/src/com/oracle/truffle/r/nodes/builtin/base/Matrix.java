/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "matrix", kind = INTERNAL, parameterNames = {"data", "nrow", "ncol", "byrow", "dimnames", "missingNr", "missingNc"}, behavior = PURE)
public abstract class Matrix extends RBuiltinNode {

    @Child private Transpose transpose;
    @Child private UpdateDimNames updateDimNames;

    private final ConditionProfile byrowProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isListProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile nrowMissingNcolGiven = ConditionProfile.createBinaryProfile();
    private final ConditionProfile nrowGivenNcolMissing = ConditionProfile.createBinaryProfile();
    private final ConditionProfile bothNrowNcolMissing = ConditionProfile.createBinaryProfile();
    private final ConditionProfile empty = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isList = ConditionProfile.createBinaryProfile();

    public abstract RAbstractVector execute(RAbstractVector data, int nrow, int ncol, boolean byrow, Object dimnames, boolean missingNr, boolean missingNc);

    private RAbstractVector updateDimNames(RAbstractVector vector, Object o) {
        if (updateDimNames == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            updateDimNames = insert(UpdateDimNamesNodeGen.create());
        }
        return (RAbstractVector) updateDimNames.executeRAbstractContainer(vector, o);
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("data").asVector().mustBe(instanceOf(RAbstractVector.class));
        casts.arg("nrow").asIntegerVector().findFirst(RError.Message.NON_NUMERIC_MATRIX_EXTENT);
        casts.arg("ncol").asIntegerVector().findFirst(RError.Message.NON_NUMERIC_MATRIX_EXTENT);
        casts.arg("byrow").asLogicalVector().findFirst().map(toBoolean());
        casts.arg("dimnames").allowNull().mustBe(instanceOf(RAbstractListVector.class));
        casts.arg("missingNr").asLogicalVector().findFirst().map(toBoolean());
        casts.arg("missingNc").asLogicalVector().findFirst().map(toBoolean());
    }

    @Specialization
    protected RAbstractVector matrix(RAbstractVector data, int nrow, int ncol, boolean byrow, Object dimnames, boolean missingNr, boolean missingNc,
                    @Cached("create()") SetDimAttributeNode setDimNode) {
        int[] dim;
        if (byrowProfile.profile(byrow)) {
            dim = computeDimByRow(data.getLength(), nrow, ncol, missingNr, missingNc);
        } else {
            dim = computeDimByCol(data.getLength(), nrow, ncol, missingNr, missingNc);
        }
        RAbstractVector res;
        if (empty.profile(data.getLength() == 0)) {
            if (isList.profile(data instanceof RAbstractListVector)) {
                // matrix of NULL-s
                res = copyResizedWithDimensions(data, dim);
                if (byrowProfile.profile(byrow)) {
                    if (transpose == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        transpose = insert(TransposeNodeGen.create());
                    }
                    res = (RVector<?>) transpose.execute(res);
                }
                if (isListProfile.profile(dimnames instanceof RAbstractListVector)) {
                    res = updateDimNames(res, dimnames);
                }
            } else {
                res = data.createEmptySameType(0, RDataFactory.COMPLETE_VECTOR);
                setDimNode.setDimensions(res, dim);
            }
        } else {
            res = data.copyResizedWithDimensions(dim, false);
            if (byrowProfile.profile(byrow)) {
                if (transpose == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    transpose = insert(TransposeNodeGen.create());
                }
                res = (RVector<?>) transpose.execute(res);
            }
            if (isListProfile.profile(dimnames instanceof RAbstractListVector)) {
                res = updateDimNames(res, dimnames);
            }
        }
        return res;
    }

    @TruffleBoundary
    private static RVector<?> copyResizedWithDimensions(RAbstractVector data, int[] dim) {
        return data.copyResizedWithDimensions(dim, true);
    }

    private int[] computeDimByCol(int size, int nrow, int ncol, boolean missingNr, boolean missingNc) {
        if (bothNrowNcolMissing.profile(missingNr && missingNc)) {
            return new int[]{size, 1};
        } else if (nrowGivenNcolMissing.profile(!missingNr && missingNc)) {
            if (nrow == 0 && size > 0) {
                throw RError.error(this, RError.Message.NROW_ZERO);
            }
            if (empty.profile(size == 0)) {
                return new int[]{nrow, 0};
            } else {
                return new int[]{nrow, 1 + ((size - 1) / nrow)};
            }
        } else if (nrowMissingNcolGiven.profile(missingNr && !missingNc)) {
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

    private int[] computeDimByRow(int size, int nrow, int ncol, boolean missingNr, boolean missingNc) {
        if (bothNrowNcolMissing.profile(missingNr && missingNc)) {
            return new int[]{1, size};
        } else if (nrowGivenNcolMissing.profile(!missingNr && missingNc)) {
            if (nrow == 0 && size > 0) {
                throw RError.error(this, RError.Message.NROW_ZERO);
            }
            if (empty.profile(size == 0)) {
                return new int[]{0, nrow};
            } else {

                return new int[]{1 + ((size - 1) / nrow), nrow};
            }
        } else if (nrowMissingNcolGiven.profile(missingNr && !missingNc)) {
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
}
