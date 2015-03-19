/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@RBuiltin(name = "diag<-", kind = SUBSTITUTE, parameterNames = {"x", ""})
// 2nd parameter is "value", but should not be matched against, so "")
// TODO Implement in R
public abstract class UpdateDiag extends RInvisibleBuiltinNode {

    private final NACheck naCheck = NACheck.create();

    @Child private CastDoubleNode castDouble;

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    protected static boolean isMatrix(RAbstractVector vector) {
        return vector.hasDimensions() && vector.getDimensions().length == 2;
    }

    protected static boolean correctReplacementLength(RAbstractVector matrix, RAbstractVector replacement) {
        return replacement.getLength() == 1 || Math.min(matrix.getDimensions()[0], matrix.getDimensions()[1]) == replacement.getLength();
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!isMatrix(vector)")
    protected RIntVector updateDiagNoMatrix(RAbstractVector vector, RAbstractVector valueVector) {
        controlVisibility();
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this.getEncapsulatingSourceSection(), RError.Message.ONLY_MATRIX_DIAGONALS);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"isMatrix(vector)", "!correctReplacementLength(vector, valueVector)"})
    protected RIntVector updateDiagReplacementDiagonalLength(RAbstractVector vector, RAbstractVector valueVector) {
        controlVisibility();
        CompilerDirectives.transferToInterpreter();
        throw RError.error(this.getEncapsulatingSourceSection(), RError.Message.REPLACEMENT_DIAGONAL_LENGTH);
    }

    @Specialization(guards = {"isMatrix(vector)", "correctReplacementLength(vector, valueVector)"})
    @TruffleBoundary
    protected RAbstractIntVector updateDiag(RIntVector vector, RAbstractIntVector valueVector) {
        controlVisibility();
        RIntVector resultVector = vector;
        if (vector.isShared()) {
            resultVector = (RIntVector) vector.copy();
            resultVector.markNonTemporary();
        }
        int nrow = resultVector.getDimensions()[0];
        int size = Math.min(nrow, resultVector.getDimensions()[1]);
        int pos = 0;
        naCheck.enable(resultVector);
        for (int i = 0; i < size; i++) {
            resultVector.updateDataAt(pos, valueVector.getDataAt(i % valueVector.getLength()), naCheck);
            pos += nrow + 1;
        }
        return resultVector;
    }

    @Specialization(guards = {"isMatrix(vector)", "correctReplacementLength(vector, valueVector)"})
    @TruffleBoundary
    protected RAbstractDoubleVector updateDiag(RDoubleVector vector, RAbstractDoubleVector valueVector) {
        controlVisibility();
        RDoubleVector resultVector = vector;
        if (vector.isShared()) {
            resultVector = (RDoubleVector) vector.copy();
            resultVector.markNonTemporary();
        }
        int size = Math.min(resultVector.getDimensions()[0], resultVector.getDimensions()[1]);
        int nrow = resultVector.getDimensions()[0];
        int pos = 0;
        naCheck.enable(resultVector);
        for (int i = 0; i < size; i++) {
            resultVector.updateDataAt(pos, valueVector.getDataAt(i % valueVector.getLength()), naCheck);
            pos += nrow + 1;
        }
        return resultVector;
    }

    @Specialization(guards = {"isMatrix(vector)", "correctReplacementLength(vector, valueVector)"})
    protected RAbstractDoubleVector updateDiag(VirtualFrame frame, RIntVector vector, RAbstractDoubleVector valueVector) {
        controlVisibility();
        initCastDoubleNode();
        RDoubleVector resultVector = (RDoubleVector) castDouble.executeDouble(frame, vector);
        resultVector.copyAttributesFrom(attrProfiles, vector);
        return updateDiag(resultVector, valueVector);
    }

    private void initCastDoubleNode() {
        if (castDouble == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castDouble = insert(CastDoubleNodeGen.create(null, false, false, false));
        }
    }
}
