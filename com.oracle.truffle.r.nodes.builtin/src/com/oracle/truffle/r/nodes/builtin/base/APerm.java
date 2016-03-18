/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

//TODO: Implement permuting with DimNames
@RBuiltin(name = "aperm", kind = INTERNAL, parameterNames = {"a", "perm", "resize"})
public abstract class APerm extends RBuiltinNode {

    private final BranchProfile errorProfile = BranchProfile.create();
    private final BranchProfile emptyPermVector = BranchProfile.create();
    private final ConditionProfile mustResize = ConditionProfile.createBinaryProfile();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toInteger(1);
    }

    private void checkErrorConditions(RAbstractVector vector, byte resize) {
        if (!vector.isArray()) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.FIRST_ARG_MUST_BE_ARRAY);
        }
        if (resize == RRuntime.LOGICAL_NA) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_LOGICAL, "resize");
        }
    }

    @Specialization
    protected RAbstractVector aPerm(RAbstractVector vector, @SuppressWarnings("unused") RNull permVector, byte resize) {
        controlVisibility();
        checkErrorConditions(vector, resize);

        int[] dim = vector.getDimensions();
        final int diml = dim.length;

        RVector result = vector.createEmptySameType(vector.getLength(), vector.isComplete());

        if (mustResize.profile(resize == RRuntime.LOGICAL_TRUE)) {
            int[] pDim = new int[diml];
            for (int i = 0; i < diml; i++) {
                pDim[i] = dim[diml - 1 - i];
            }
            result.setDimensions(pDim);
        } else {
            result.setDimensions(dim);
        }

        // Move along the old array using stride
        int[] posV = new int[diml];
        int[] ap = new int[diml];
        for (int i = 0; i < result.getLength(); i++) {
            for (int j = 0; j < ap.length; j++) {
                ap[diml - 1 - j] = posV[j];
            }
            int pos = toPos(ap, dim);
            result.transferElementSameType(i, vector, pos);
            for (int j = 0; j < diml; j++) {
                posV[j]++;
                if (posV[j] < dim[diml - 1 - j]) {
                    break;
                }
                posV[j] = 0;
            }
        }

        return result;
    }

    @Specialization
    protected RAbstractVector aPerm(RAbstractVector vector, RAbstractIntVector permVector, byte resize) {
        controlVisibility();
        checkErrorConditions(vector, resize);

        int[] dim = vector.getDimensions();
        int[] perm = getPermute(dim, permVector);

        int[] posV = new int[dim.length];
        int[] pDim = applyPermute(dim, perm, false);

        RVector result = vector.createEmptySameType(vector.getLength(), vector.isComplete());

        result.setDimensions(resize == RRuntime.LOGICAL_TRUE ? pDim : dim);

        // Move along the old array using stride
        for (int i = 0; i < result.getLength(); i++) {
            int pos = toPos(applyPermute(posV, perm, true), dim);
            result.transferElementSameType(i, vector, pos);
            posV = incArray(posV, pDim);
        }

        return result;
    }

    private static int[] getReverse(int[] dim) {
        int[] arrayPerm = new int[dim.length];
        for (int i = 0; i < dim.length; i++) {
            arrayPerm[i] = dim.length - 1 - i;
        }
        return arrayPerm;
    }

    private int[] getPermute(int[] dim, RAbstractIntVector perm) {
        if (perm.getLength() == 0) {
            // If perm missing, the default is a reverse of the dim.
            emptyPermVector.enter();
            return getReverse(dim);
        } else if (perm.getLength() == dim.length) {
            // Check for valid permute
            int[] arrayPerm = new int[dim.length];
            boolean[] visited = new boolean[arrayPerm.length];
            for (int i = 0; i < perm.getLength(); i++) {
                int pos = perm.getDataAt(i) - 1; // Adjust to zero based permute.
                if (pos >= perm.getLength() || pos < 0) {
                    errorProfile.enter();
                    throw RError.error(this, RError.Message.VALUE_OUT_OF_RANGE, "perm");
                }
                arrayPerm[i] = pos;
                if (visited[pos]) {
                    // Duplicate dimension mapping in permute
                    errorProfile.enter();
                    throw RError.error(this, RError.Message.INVALID_ARGUMENT, "perm");
                }
                visited[pos] = true;
            }
            return arrayPerm;
        } else {
            // perm size error
            errorProfile.enter();
            throw RError.error(this, RError.Message.IS_OF_WRONG_LENGTH, "perm", perm.getLength(), dim.length);
        }
    }

    /**
     * Apply permute to an equal sized array.
     */
    @TruffleBoundary
    private static int[] applyPermute(int[] a, int[] perm, boolean reverse) {
        int[] newA = a.clone();
        if (reverse) {
            for (int i = 0; i < newA.length; i++) {
                newA[perm[i]] = a[i];
            }
        } else {
            for (int i = 0; i < newA.length; i++) {
                newA[i] = a[perm[i]];
            }
        }
        return newA;
    }

    /**
     * Increment a stride array.
     */
    @TruffleBoundary
    private static int[] incArray(int[] a, int[] dim) {
        int[] newA = a.clone();
        for (int i = 0; i < newA.length; i++) {
            newA[i]++;
            if (newA[i] < dim[i]) {
                break;
            }
            newA[i] = 0;
        }
        return newA;
    }

    /**
     * Stride array to a linear position.
     */
    @TruffleBoundary
    private static int toPos(int[] a, int[] dim) {
        int pos = a[0];
        for (int i = 1; i < a.length; i++) {
            int dimSizeBefore = 1; // Total size of dimensions before the ith dimension.
            for (int j = i - 1; j >= 0; j--) {
                dimSizeBefore *= dim[j];
            }
            pos += a[i] * dimSizeBefore;
        }
        return pos;
    }
}
