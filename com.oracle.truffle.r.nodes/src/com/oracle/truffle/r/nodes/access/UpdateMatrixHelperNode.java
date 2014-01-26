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
package com.oracle.truffle.r.nodes.access;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@SuppressWarnings({"unused"})
@NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "newValue", type = RNode.class), @NodeChild(value = "firstPosition", type = RNode.class),
                @NodeChild(value = "secondPosition", type = RNode.class)})
public abstract class UpdateMatrixHelperNode extends CoercedBinaryOperationNode {

    // TODO: enable right NA check
    private final NACheck leftNACheck = NACheck.create();
    private final NACheck rightNACheck = NACheck.create();
    private final NACheck positionNACheck = NACheck.create();

    @CompilationFinal private boolean isSubset;

    public void setSubset(boolean s) {
        isSubset = s;
    }

    @CreateCast({"firstPosition", "secondPosition"})
    public RNode createCastPosition(RNode child) {
        return VectorPositionCastFactory.create(positionNACheck, child);
    }

    // Scalar assigned to logical vector with scalar position.

    @Specialization(order = 100)
    public RLogicalVector doLogical(RLogicalVector vector, byte right, int firstPosition, int secondPosition) {
        return vector.updateDataAt(vector.convertToIndex(firstPosition, secondPosition), right, rightNACheck);
    }

    @Specialization(order = 200)
    public RIntVector doInt(RIntVector vector, int right, int firstPosition, int secondPosition) {
        return vector.updateDataAt(vector.convertToIndex(firstPosition, secondPosition), right, rightNACheck);
    }

    @Specialization(order = 300)
    public RDoubleVector doDouble(RDoubleVector vector, double right, int firstPosition, int secondPosition) {
        return vector.updateDataAt(vector.convertToIndex(firstPosition, secondPosition), right, rightNACheck);
    }

    @Specialization(order = 400)
    public RComplexVector doComplex(RComplexVector vector, RComplex right, int firstPosition, int secondPosition) {
        return vector.updateDataAt(vector.convertToIndex(firstPosition, secondPosition), right, rightNACheck);
    }

    @Specialization(order = 500)
    public RStringVector doString(RStringVector vector, String right, int firstPosition, int secondPosition) {
        return vector.updateDataAt(vector.convertToIndex(firstPosition, secondPosition), right, rightNACheck);
    }

}
