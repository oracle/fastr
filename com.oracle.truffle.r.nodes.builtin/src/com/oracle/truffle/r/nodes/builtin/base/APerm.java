/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

//TODO: Implement permuting with DimNames
@RBuiltin(name = "aperm", kind = INTERNAL, parameterNames = {"a", "perm", "resize"})
public abstract class APerm extends RBuiltinNode {

    private final BranchProfile errorProfile = new BranchProfile();

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @CreateCast("arguments")
    public RNode[] createCastPermute(RNode[] arguments) {
        arguments[1] = CastIntegerNodeFactory.create(CastToVectorNodeFactory.create(arguments[1], false, false, false, false), false, false, false);
        return arguments;
    }

    @Specialization
    protected RAbstractVector aPerm(RAbstractVector vector, RAbstractIntVector permVector, byte resize) {
        controlVisibility();

        if (!vector.isArray()) {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.FIRST_ARG_MUST_BE_ARRAY);
        }

        int[] dim = vector.getDimensions();
        int[] perm = getPermute(dim, permVector);

        int[] posV = new int[dim.length];
        int[] pDim = applyPermute(dim, perm, false);

        RVector realVector = vector.materialize();
        RVector result = realVector.createEmptySameType(vector.getLength(), vector.isComplete());

        if (resize == RRuntime.LOGICAL_NA) {
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_LOGICAL, "resize");
        }

        result.setDimensions(resize == RRuntime.LOGICAL_TRUE ? pDim : dim);

        // Move along the old array using stride
        for (int i = 0; i < result.getLength(); i++) {
            int pos = toPos(applyPermute(posV, perm, true), dim);
            result.transferElementSameType(i, realVector, pos);
            posV = incArray(posV, pDim);
        }

        return result;
    }

    private int[] getPermute(int[] dim, RAbstractIntVector perm) {
        int[] arrayPerm = new int[dim.length];
        if (perm.getLength() == 0) {
            // If perm missing, the default is a reverse of the dim.
            for (int i = 0; i < dim.length; i++) {
                arrayPerm[i] = dim.length - 1 - i;
            }
        } else if (perm.getLength() == dim.length) {
            // Check for valid permute
            boolean[] visited = new boolean[arrayPerm.length];
            for (int i = 0; i < perm.getLength(); i++) {
                int pos = perm.getDataAt(i) - 1; // Adjust to zero based permute.
                if (pos >= perm.getLength() || pos < 0) {
                    errorProfile.enter();
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.VALUE_OUT_OF_RANGE, "perm");
                }
                arrayPerm[i] = pos;
                if (visited[pos]) {
                    // Duplicate dimension mapping in permute
                    errorProfile.enter();
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "perm");
                }
                visited[pos] = true;
            }
        } else {
            // perm size error
            errorProfile.enter();
            throw RError.error(getEncapsulatingSourceSection(), RError.Message.IS_OF_WRONG_LENGTH, "perm", perm.getLength(), dim.length);
        }

        return arrayPerm;
    }

    /**
     * Apply permute to an equal sized array.
     */
    @SlowPath
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
    @SlowPath
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
    @SlowPath
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
