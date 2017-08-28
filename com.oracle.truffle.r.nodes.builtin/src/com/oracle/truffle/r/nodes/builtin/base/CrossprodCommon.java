/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.complexValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.access.vector.ExtractListElement;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.GetReadonlyData;

/**
 * Implements the basic logic for {@code crossprod} and {@code tcrossprod}. Either the first matrix
 * is transposed or the second one depending on the constructor's argument value.
 */
public abstract class CrossprodCommon extends RBuiltinNode.Arg2 {

    @Child private Transpose transpose;
    @Child protected MatMult matMult = MatMultNodeGen.create(/* promoteDimNames: */ false);
    @Child private GetDimNamesAttributeNode getXDimNames = GetDimNamesAttributeNode.create();
    @Child private GetDimNamesAttributeNode getYDimNames = GetDimNamesAttributeNode.create();
    @Child private SetDimNamesAttributeNode setDimNames = SetDimNamesAttributeNode.create();
    @Child private ExtractListElement getRowNames = ExtractListElement.create();
    @Child private ExtractListElement colRowNames = ExtractListElement.create();
    private final ConditionProfile anyDimNames = ConditionProfile.createBinaryProfile();
    private final boolean transposeX;

    static {
        Casts casts = new Casts(CrossprodCommon.class);
        casts.arg(0).mustBe(numericValue().or(complexValue()), RError.Message.NUMERIC_COMPLEX_MATRIX_VECTOR);
        casts.arg(1).defaultError(RError.Message.NUMERIC_COMPLEX_MATRIX_VECTOR).allowNull().mustBe(numericValue().or(complexValue()));
    }

    protected CrossprodCommon(boolean transposeX) {
        this.transposeX = transposeX;
    }

    public static CrossprodCommon createCrossprod() {
        return CrossprodCommonNodeGen.create(true);
    }

    public static CrossprodCommon createTCrossprod() {
        return CrossprodCommonNodeGen.create(false);
    }

    @Specialization(guards = {"x.isMatrix()", "y.isMatrix()"})
    protected RDoubleVector crossprod(RAbstractDoubleVector x, RAbstractDoubleVector y,
                    @Cached("create()") GetDimAttributeNode getXDimsNode,
                    @Cached("create()") GetDimAttributeNode getYDimsNode) {
        int[] xDims = getXDimsNode.getDimensions(x);
        int[] yDims = getYDimsNode.getDimensions(y);
        int xRows = transposeX ? xDims[1] : xDims[0];
        int xCols = transposeX ? xDims[0] : xDims[1];
        int yRows = transposeX ? yDims[0] : yDims[1];
        int yCols = transposeX ? yDims[1] : yDims[0];
        RDoubleVector result = matMult.doubleMatrixMultiply(x, y, xRows, xCols, yRows, yCols, getXRowStride(xDims[0]), getXColStride(xDims[0]), getYRowStride(yDims[0]), getYColStride(yDims[0]),
                        false);
        return copyDimNames(x, y, result);
    }

    @Specialization
    protected Object crossprod(RAbstractVector x, RAbstractVector y) {
        return copyDimNames(x, y, (RAbstractVector) matMult.executeObject(transposeX(x), transposeY(y)));
    }

    @Specialization(guards = "x.isMatrix()")
    protected RDoubleVector crossprodDoubleMatrix(RAbstractDoubleVector x, @SuppressWarnings("unused") RNull y,
                    @Cached("create()") GetReadonlyData.Double getReadonlyData,
                    @Cached("create()") GetDimAttributeNode getDimsNode,
                    @Cached("create()") GetDimAttributeNode getResultDimsNode) {
        int[] xDims = getDimsNode.getDimensions(x);
        int xRows = transposeX ? xDims[1] : xDims[0];
        int xCols = transposeX ? xDims[0] : xDims[1];
        int yRows = transposeX ? xDims[0] : xDims[1];
        int yCols = transposeX ? xDims[1] : xDims[0];
        RDoubleVector result = mirror(
                        matMult.doubleMatrixMultiply(x, x, xRows, xCols, yRows, yCols, getXRowStride(xDims[0]), getXColStride(xDims[0]), getYRowStride(xDims[0]), getYColStride(xDims[0]), true),
                        getResultDimsNode,
                        getReadonlyData);
        return copyDimNames(x, x, result);
    }

    @Specialization
    protected Object crossprod(RAbstractVector x, @SuppressWarnings("unused") RNull y) {
        return copyDimNames(x, x, (RAbstractVector) matMult.executeObject(transposeX(x), transposeY(x)));
    }

    private Object transposeX(RAbstractVector x) {
        return transposeX ? transpose(x) : x;
    }

    private Object transposeY(RAbstractVector y) {
        return transposeX ? y : transpose(y);
    }

    private int getXRowStride(int rows) {
        return transposeX ? rows : 1;
    }

    private int getXColStride(int rows) {
        return transposeX ? 1 : rows;
    }

    private int getYRowStride(int rows) {
        return transposeX ? 1 : rows;
    }

    private int getYColStride(int rows) {
        return transposeX ? rows : 1;
    }

    private Object transpose(RAbstractVector value) {
        if (transpose == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            transpose = insert(TransposeNodeGen.create());
        }
        return transpose.execute(value);
    }

    // Copies row names from x and col names from y, note: what is cols and rows changes if the
    // matrix is transposed.
    private <T extends RAbstractVector> T copyDimNames(RAbstractVector x, RAbstractVector y, T result) {
        RList xDimNames = getXDimNames.getDimNames(x);
        RList yDimNames = getYDimNames.getDimNames(y);
        if (anyDimNames.profile(xDimNames != null || yDimNames != null)) {
            Object[] newDimnames = new Object[]{RNull.instance, RNull.instance};
            if (xDimNames != null) {
                newDimnames[0] = getRowNames.execute(xDimNames, transposeX ? 1 : 0);
            }
            if (yDimNames != null) {
                newDimnames[1] = getRowNames.execute(yDimNames, transposeX ? 1 : 0);
            }
            setDimNames.setDimNames(result, RDataFactory.createList(newDimnames));
        }
        return result;
    }

    private static RDoubleVector mirror(RDoubleVector result, GetDimAttributeNode getResultDimsNode, GetReadonlyData.Double getReadonlyData) {
        // Mirroring the result is not only good for performance, but it is also required to produce
        // the same result as GNUR.
        int[] resultDims = getResultDimsNode.getDimensions(result);
        assert result.isMatrix() && resultDims[0] == resultDims[1];
        int size = resultDims[0];
        double[] data = getReadonlyData.execute(result);
        for (int row = 0; row < size; row++) {
            int destIndex = row * size + row + 1;
            int sourceIndex = (row + 1) * size + row;
            for (int col = row + 1; col < size; col++) {
                data[destIndex] = data[sourceIndex];
                destIndex++;
                sourceIndex += size;
            }
        }
        return result;
    }

    // Note: these are just a placeholders for the builtin metadata, the actual code is in
    // CrossprodCommon
    @RBuiltin(name = "crossprod", kind = INTERNAL, parameterNames = {"x", "y"}, behavior = PURE)
    public abstract static class Crossprod extends Arg2 {
    }

    @RBuiltin(name = "tcrossprod", kind = INTERNAL, parameterNames = {"x", "y"}, behavior = PURE)
    public abstract static class TCrossprod extends Arg2 {
    }
}
