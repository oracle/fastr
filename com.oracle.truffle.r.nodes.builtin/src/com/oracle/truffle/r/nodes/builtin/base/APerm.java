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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asIntegerVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.complexValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

// TODO: add (permuted) dimnames to the result
@RBuiltin(name = "aperm", kind = INTERNAL, parameterNames = {"a", "perm", "resize"}, behavior = PURE)
public abstract class APerm extends RBuiltinNode {

    private final BranchProfile errorProfile = BranchProfile.create();
    private final BranchProfile emptyPermVector = BranchProfile.create();
    private final ConditionProfile mustResize = ConditionProfile.createBinaryProfile();

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("a").mustNotBeNull(RError.Message.FIRST_ARG_MUST_BE_ARRAY);
        casts.arg("perm").allowNull().mustBe(numericValue().or(stringValue()).or(complexValue())).mapIf(numericValue().or(complexValue()), asIntegerVector());
        casts.arg("resize").mustBe(numericValue().or(logicalValue()), Message.INVALID_LOGICAL, "resize").asLogicalVector().findFirst();
    }

    private void checkErrorConditions(RAbstractVector vector) {
        if (!vector.isArray()) {
            errorProfile.enter();
            throw RError.error(RError.SHOW_CALLER, RError.Message.FIRST_ARG_MUST_BE_ARRAY);
        }
    }

    @Specialization
    protected RAbstractVector aPerm(RAbstractVector vector, @SuppressWarnings("unused") RNull permVector, byte resize,
                    @Cached("create()") GetDimAttributeNode getDimsNode,
                    @Cached("create()") SetDimAttributeNode setDimNode) {
        checkErrorConditions(vector);

        int[] dim = getDimsNode.getDimensions(vector);
        final int diml = dim.length;

        RVector<?> result = vector.createEmptySameType(vector.getLength(), vector.isComplete());

        if (mustResize.profile(resize == RRuntime.LOGICAL_TRUE)) {
            int[] pDim = new int[diml];
            for (int i = 0; i < diml; i++) {
                pDim[i] = dim[diml - 1 - i];
            }
            setDimNode.setDimensions(result, pDim);
        } else {
            setDimNode.setDimensions(result, dim);
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
    protected RAbstractVector aPerm(RAbstractVector vector, RAbstractIntVector permVector, byte resize,
                    @Cached("create()") GetDimAttributeNode getDimsNode,
                    @Cached("create()") SetDimAttributeNode setDimsNode) {
        checkErrorConditions(vector);

        int[] dim = getDimsNode.getDimensions(vector);
        int[] perm = getPermute(dim, permVector);

        int[] posV = new int[dim.length];
        int[] pDim = applyPermute(dim, perm, false);

        RVector<?> result = vector.createEmptySameType(vector.getLength(), vector.isComplete());

        setDimsNode.setDimensions(result, resize == RRuntime.LOGICAL_TRUE ? pDim : dim);

        // Move along the old array using stride
        for (int i = 0; i < result.getLength(); i++) {
            int pos = toPos(applyPermute(posV, perm, true), dim);
            result.transferElementSameType(i, vector, pos);
            posV = incArray(posV, pDim);
        }

        return result;
    }

    @Specialization
    protected RAbstractVector aPerm(RAbstractVector vector, RAbstractStringVector permVector, byte resize,
                    @Cached("create()") GetDimAttributeNode getDimsNode,
                    @Cached("create()") SetDimAttributeNode setDimsNode,
                    @Cached("create()") GetDimNamesAttributeNode getDimNamesNode) {
        RList dimNames = getDimNamesNode.getDimNames(vector);
        if (dimNames == null) {
            // TODO: this error is reported after IS_OF_WRONG_LENGTH in GnuR
            errorProfile.enter();
            throw RError.error(this, RError.Message.DOES_NOT_HAVE_DIMNAMES, "a");
        }

        int[] perm = new int[permVector.getLength()];
        for (int i = 0; i < perm.length; i++) {
            for (int dimNamesIdx = 0; dimNamesIdx < dimNames.getLength(); dimNamesIdx++) {
                if (dimNames.getDataAt(dimNamesIdx).equals(permVector.getDataAt(i))) {
                    perm[i] = dimNamesIdx;
                    break;
                }
            }
            // TODO: not found dimname error
        }

        // Note: if this turns out to be slow, we can cache the permutation
        return aPerm(vector, RDataFactory.createIntVector(perm, true), resize, getDimsNode, setDimsNode);
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
                    throw RError.error(RError.SHOW_CALLER, RError.Message.VALUE_OUT_OF_RANGE, "perm");
                }
                arrayPerm[i] = pos;
                if (visited[pos]) {
                    // Duplicate dimension mapping in permute
                    errorProfile.enter();
                    throw RError.error(RError.SHOW_CALLER, RError.Message.INVALID_ARGUMENT, "perm");
                }
                visited[pos] = true;
            }
            return arrayPerm;
        } else {
            // perm size error
            errorProfile.enter();
            throw RError.error(RError.SHOW_CALLER, RError.Message.IS_OF_WRONG_LENGTH, "perm", perm.getLength(), dim.length);
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
