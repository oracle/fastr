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

import java.util.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

//TODO: Implement permuting with DimNames
@RBuiltin(name = "aperm", kind = INTERNAL, parameterNames = {"a", "perm", "..."})
public abstract class APerm extends RBuiltinNode {

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance), ConstantNode.create(RMissing.instance)};
    }

    @CreateCast("arguments")
    public RNode[] createCastPermute(RNode[] arguments) {
        RNode permVector = CastToVectorNodeFactory.create(arguments[1], false, false, false, false);
        return new RNode[]{arguments[0], CastIntegerNodeFactory.create(permVector, false, false, false), arguments[2]};
    }

    @Specialization
    public RAbstractVector aPerm(VirtualFrame frame, RAbstractVector vector, RAbstractIntVector permVector, byte resize) {
        controlVisibility();

        if (!vector.isArray()) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.FIRST_ARG_MUST_BE_ARRAY);
        }

        int[] dim = getDimensions(vector);
        int[] perm = getPermute(frame, dim, permVector);

        int[] posV = new int[dim.length];
        int[] pDim = applyPermute(dim, perm, false);

        RVector realVector = vector.materialize();
        RVector result = realVector.createEmptySameType(vector.getLength(), vector.isComplete());

        if (resize == RRuntime.LOGICAL_NA) {
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.INVALID_LOGICAL, "resize");
        } else if (resize == RRuntime.LOGICAL_TRUE) {
            result.setDimensions(pDim);
        } else {
            result.setDimensions(vector.getDimensions());
        }

        // Move along the old array using stride
        for (int i = 0; i < result.getLength(); i++) {
            int pos = toPos(applyPermute(posV, perm, true), dim);
            result.transferElementSameType(i, realVector, pos);
            posV = incArray(posV, pDim);
        }

        return result;
    }

    private int[] getPermute(VirtualFrame frame, int[] dim, RAbstractIntVector perm) {
        int[] arrayPerm = new int[dim.length];
        if (perm.getLength() == 0) {
            // If perm missing, the default is a reverse of the dim.
            for (int i = 0; i < dim.length; i++) {
                arrayPerm[i] = dim.length - 1 - i;
            }
        } else if (perm.getLength() == dim.length) {
            for (int i = 0; i < perm.getLength(); i++) {
                if (perm.getDataAt(i) > perm.getLength() || perm.getDataAt(i) < 1) {
                    throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.VALUE_OUT_OF_RANGE, "perm");
                }
                arrayPerm[i] = perm.getDataAt(i) - 1; // Adjust to zero based permute.
            }
            // Check for valid permute
            boolean[] visited = new boolean[arrayPerm.length];
            Arrays.fill(visited, false);
            for (int i = 0; i < arrayPerm.length; i++) {
                if (!visited[arrayPerm[i]]) {
                    visited[arrayPerm[i]] = true;
                } else {
                    // Duplicate dimension mapping in permute
                    throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.INVALID_ARGUMENT, "perm");
                }
            }
        } else {
            // perm size error
            throw RError.error(frame, getEncapsulatingSourceSection(), RError.Message.IS_OF_WRONG_LENGTH, "perm", perm.getLength(), dim.length);
        }

        return arrayPerm;
    }

    private static int[] getDimensions(RAbstractVector v) {
        // Get dimensions move to int array
        RIntVector dimV = RDataFactory.createIntVector(v.getDimensions(), RDataFactory.COMPLETE_VECTOR);
        int[] dim = new int[dimV.getLength()];
        for (int i = 0; i < dimV.getLength(); i++) {
            dim[i] = dimV.getDataAt(i);
        }
        return dim;
    }

    /**
     * Apply permute to an equal sized array.
     */
    private static int[] applyPermute(int[] a, int[] perm, boolean reverse) {
        int[] newA = a.clone();
        if (reverse) {
            for (int i = 0; i < newA.length; i++) {
                newA[perm[i]] = a[i];
            }
        } else {
            for (int i = 0; i < newA.length; i++) {
                // System.out.println("perm:" + i + ": " + perm[i] + "   a.length" + a.length);
                newA[i] = a[perm[i]];
            }
        }
        return newA;
    }

    /**
     * Increment a stride array.
     */
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
