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
@RBuiltin(name = "aperm", kind = INTERNAL)
public abstract class APerm extends RBuiltinNode {

    @CreateCast("arguments")
    public RNode[] createCastPermute(RNode[] arguments) {
        RNode permVector = CastToVectorNodeFactory.create(arguments[1], false, false, false, false);
        return new RNode[]{arguments[0], CastIntegerNodeFactory.create(permVector, false, false, false), arguments[2]};
    }

    @Specialization
    public RIntVector aPerm(RAbstractIntVector vector, RAbstractIntVector permVector, byte resize) {
        controlVisibility();

        int[] dim = getDimensions(vector);
        int[] perm = getPermute(dim, permVector);

        int[] posV = new int[dim.length]; // Position vector for each dimension
        int[] newDim = applyPermute(dim, perm, false); // Position vector for striding
        int[] result = new int[vector.getLength()];

        // Move along the old array
        for (int i = 0; i < result.length; i++) {
            int posOld = toPos(applyPermute(posV, perm, true), dim);
            result[i] = vector.getDataAt(posOld);

            posV = incArray(posV, newDim);

            // System.out.println("[" + i + "] = pos:" + posOld);
        }
        RIntVector r = RDataFactory.createIntVector(result, vector.isComplete());
        r.copyAttributesFrom(vector);

        if (resize == 0) {
            r.setDimensions(dim);
        } else {
            r.setDimensions(newDim);
        }

        return r;
    }

    @Specialization
    public RDoubleVector aPerm(RAbstractDoubleVector vector, RAbstractIntVector permVector, byte resize) {
        controlVisibility();

        int[] dim = getDimensions(vector);
        int[] perm = getPermute(dim, permVector);

        int[] posV = new int[dim.length];
        int[] newDim = applyPermute(dim, perm, false);
        double[] result = new double[vector.getLength()];

        for (int i = 0; i < result.length; i++) {
            int posOld = toPos(applyPermute(posV, perm, true), dim);
            result[i] = vector.getDataAt(posOld);
            posV = incArray(posV, newDim);
        }

        RDoubleVector r = RDataFactory.createDoubleVector(result, vector.isComplete());
        r.copyAttributesFrom(vector);

        if (resize == 0) {
            r.setDimensions(dim);
        } else {
            r.setDimensions(newDim);
        }

        return r;
    }

    @Specialization
    public RLogicalVector aPerm(RAbstractLogicalVector vector, RAbstractIntVector permVector, byte resize) {
        controlVisibility();

        int[] dim = getDimensions(vector);
        int[] perm = getPermute(dim, permVector);

        int[] posV = new int[dim.length];
        int[] newDim = applyPermute(dim, perm, false);
        byte[] result = new byte[vector.getLength()];

        for (int i = 0; i < result.length; i++) {
            int posOld = toPos(applyPermute(posV, perm, true), dim);
            result[i] = vector.getDataAt(posOld);
            posV = incArray(posV, newDim);
        }

        RLogicalVector r = RDataFactory.createLogicalVector(result, vector.isComplete());
        r.copyAttributesFrom(vector);

        if (resize == 0) {
            r.setDimensions(dim);
        } else {
            r.setDimensions(newDim);
        }

        return r;
    }

    @Specialization
    public RComplexVector aPerm(RAbstractComplexVector vector, RAbstractIntVector permVector, byte resize) {
        controlVisibility();

        int[] dim = getDimensions(vector);
        int[] perm = getPermute(dim, permVector);

        int[] posV = new int[dim.length];
        int[] newDim = applyPermute(dim, perm, false);
        double[] result = new double[vector.getLength() * 2];

        for (int i = 0; i < result.length; i++) {
            int posOld = toPos(applyPermute(posV, perm, true), dim);
            result[2 * i] = vector.getDataAt(posOld).getRealPart();
            result[2 * i + 1] = vector.getDataAt(i).getImaginaryPart();
            posV = incArray(posV, newDim);
        }

        RComplexVector r = RDataFactory.createComplexVector(result, vector.isComplete());
        r.copyAttributesFrom(vector);

        if (resize == 0) {
            r.setDimensions(dim);
        } else {
            r.setDimensions(newDim);
        }

        return r;
    }

    @Specialization
    public RStringVector aPerm(RAbstractStringVector vector, RAbstractIntVector permVector, byte resize) {
        controlVisibility();

        int[] dim = getDimensions(vector);
        int[] perm = getPermute(dim, permVector);

        int[] posV = new int[dim.length];
        int[] newDim = applyPermute(dim, perm, false);
        String[] result = new String[vector.getLength()];

        for (int i = 0; i < result.length; i++) {
            int posOld = toPos(applyPermute(posV, perm, true), dim);
            result[i] = vector.getDataAt(i);
            posV = incArray(posV, newDim);
        }

        RStringVector r = RDataFactory.createStringVector(result, vector.isComplete());
        r.copyAttributesFrom(vector);

        if (resize == 0) {
            r.setDimensions(dim);
        } else {
            r.setDimensions(newDim);
        }

        return r;
    }

    @Specialization
    public RRawVector aPerm(RAbstractRawVector vector, RAbstractIntVector permVector, byte resize) {
        controlVisibility();

        int[] dim = getDimensions(vector);
        int[] perm = getPermute(dim, permVector);

        int[] posV = new int[dim.length];
        int[] newDim = applyPermute(dim, perm, false);
        byte[] result = new byte[vector.getLength()];

        for (int i = 0; i < result.length; i++) {
            int posOld = toPos(applyPermute(posV, perm, true), dim);
            result[i] = vector.getDataAt(i).getValue();
            posV = incArray(posV, newDim);
        }

        RRawVector r = RDataFactory.createRawVector(result);
        r.copyAttributesFrom(vector);

        if (resize == 0) {
            r.setDimensions(dim);
        } else {
            r.setDimensions(newDim);
        }

        return r;
    }

    private int[] getPermute(int[] dim, RAbstractIntVector perm) {
        int[] arrayPerm = new int[dim.length];
        if (perm.getLength() == 0) {
            // If perm missing, the default is a reverse of the dim.
            for (int i = 0; i < dim.length; i++) {
                arrayPerm[i] = dim.length - 1 - i;
            }
        } else if (perm.getLength() == dim.length) {
            for (int i = 0; i < perm.getLength(); i++) {
                if (perm.getDataAt(i) > perm.getLength() || perm.getDataAt(i) < 1) {
                    throw RError.error(RError.Message.VALUE_OUT_OF_RANGE, "perm");
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
                    throw RError.error(RError.Message.INVALID_ARGUMENT, "perm");
                }
            }
        } else {
            // perm size error
            throw RError.error(RError.Message.INVALID_ARGUMENT, "perm");
        }

        return arrayPerm;
    }

    private int[] getDimensions(RAbstractVector v) {
        if (!v.isArray()) {
            throw RError.error(RError.Message.ARGUMENT_NOT_ARRAY, "perm");
        }

        // Get dimensions move to int array
        RIntVector dimV = RDataFactory.createIntVector(v.getDimensions(), RDataFactory.COMPLETE_VECTOR);
        int dim[] = new int[dimV.getLength()];
        for (int i = 0; i < dimV.getLength(); i++) {
            dim[i] = dimV.getDataAt(i);
        }
        return dim;
    }

    // Apply permute to an equal sized array
    private int[] applyPermute(int[] a, int[] perm, boolean reverse) {
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

    // Increment a stride array
    private int[] incArray(int[] a, int[] dim) {
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

    // Stride array to a linear position
    private int toPos(int[] a, int[] dim) {
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
