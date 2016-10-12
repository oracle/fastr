/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base.fastpaths;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.base.Matrix;
import com.oracle.truffle.r.nodes.builtin.base.MatrixNodeGen;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.nodes.unary.FirstIntNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RFastPathNode;

public abstract class MatrixFastPath extends RFastPathNode {

    protected static FirstIntNode createFirst() {
        return FirstIntNode.createWithError(RError.Message.NON_NUMERIC_MATRIX_EXTENT, null);
    }

    protected static Matrix createMatrix() {
        return MatrixNodeGen.create();
    }

    @Specialization
    protected Object matrix(RAbstractVector data, Object nrow, Object ncol, @SuppressWarnings("unused") RMissing byrow, Object dimnames, //
                    @Cached("create()") CastIntegerNode castRow, //
                    @Cached("create()") CastIntegerNode castCol, //
                    @Cached("createFirst()") FirstIntNode firstRow, //
                    @Cached("createFirst()") FirstIntNode firstCol, //
                    @Cached("createBinaryProfile()") ConditionProfile rowMissingProfile, //
                    @Cached("createBinaryProfile()") ConditionProfile colMissingProfile, //
                    @Cached("createBinaryProfile()") ConditionProfile dimMissingProfile, //
                    @Cached("createMatrix()") Matrix matrix) {
        boolean rowMissing = rowMissingProfile.profile(nrow == RMissing.instance);
        boolean colMissing = colMissingProfile.profile(ncol == RMissing.instance);
        int row = rowMissing ? 1 : firstRow.executeInt(castRow.execute(nrow));
        int col = colMissing ? 1 : firstCol.executeInt(castCol.execute(ncol));
        Object dim = dimMissingProfile.profile(dimnames == RMissing.instance) ? RNull.instance : dimnames;
        return matrix.execute(data, row, col, false, dim, rowMissing, colMissing);
    }

    @Fallback
    @SuppressWarnings("unused")
    protected Object fallback(Object data, Object nrow, Object ncol, Object byrow, Object dimnames) {
        return null;
    }
}
