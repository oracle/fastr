/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access.array.write;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.nodes.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@TypeSystemReference(RTypes.class)
abstract class SetMultiDimDataNode extends RBaseNode {

    public abstract Object executeMultiDimDataSet(RAbstractContainer value, RAbstractVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                    int accDstDimensions);

    private final NACheck posNACheck;
    private final NACheck elementNACheck;
    private final ConditionProfile dimLevelOneProfile = ConditionProfile.createBinaryProfile();
    private final boolean isSubset;

    @Child private SetMultiDimDataNode setMultiDimDataRecursive;

    private Object setMultiDimData(RAbstractVector value, RAbstractVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                    int accDstDimensions, NACheck posCheck, NACheck elementCheck) {
        if (setMultiDimDataRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            setMultiDimDataRecursive = insert(SetMultiDimDataNodeGen.create(posCheck, elementCheck, this.isSubset));
        }
        return setMultiDimDataRecursive.executeMultiDimDataSet(value, vector, positions, currentDimLevel, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
    }

    protected SetMultiDimDataNode(NACheck posNACheck, NACheck elementNACheck, boolean isSubset) {
        this.posNACheck = posNACheck;
        this.elementNACheck = elementNACheck;
        this.isSubset = isSubset;
    }

    protected SetMultiDimDataNode(SetMultiDimDataNode other) {
        this.posNACheck = other.posNACheck;
        this.elementNACheck = other.elementNACheck;
        this.isSubset = other.isSubset;
    }

    private int getNewArrayBase(int srcArrayBase, int pos, int newAccSrcDimensions) {
        int newSrcArrayBase;
        if (posNACheck.check(pos)) {
            throw RError.error(this, RError.Message.NA_SUBSCRIPTED);
        } else {
            newSrcArrayBase = srcArrayBase + newAccSrcDimensions * (pos - 1);
        }
        return newSrcArrayBase;
    }

    private int getSrcIndex(int srcArrayBase, int pos, int newAccSrcDimensions) {
        if (posNACheck.check(pos)) {
            throw RError.error(this, RError.Message.NA_SUBSCRIPTED);
        } else {
            return srcArrayBase + newAccSrcDimensions * (pos - 1);
        }
    }

    @FunctionalInterface
    private interface UpdateFunction<ValueT extends RAbstractVector, VectorT extends RVector> {
        void apply(VectorT vector, ValueT value, int srcIndex, int destIndex, NACheck elementNACheck);
    }

    private <ValueT extends RAbstractVector, VectorT extends RVector> VectorT setDataInternal(ValueT value, VectorT vector, Object[] positions, int currentDimLevel, int srcArrayBase,
                    int dstArrayBase, int accSrcDimensions, int accDstDimensions, UpdateFunction<ValueT, VectorT> update) {
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        posNACheck.enable(p);
        elementNACheck.enable(value);
        if (dimLevelOneProfile.profile(currentDimLevel == 1)) {
            for (int i = 0; i < p.getLength(); i++) {
                int pos = p.getDataAt(i);
                if (posNACheck.check(pos)) {
                    UpdateArrayHelperNode.handleNaMultiDim(value, false, isSubset, this);
                    continue;
                }
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, pos, newAccSrcDimensions);
                update.apply(vector, value, srcIndex, dstIndex, elementNACheck);
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int pos = p.getDataAt(i);
                if (posNACheck.check(pos)) {
                    UpdateArrayHelperNode.handleNaMultiDim(value, false, isSubset, this);
                    continue;
                }
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, pos, newAccSrcDimensions);
                setMultiDimData(value, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
            }
        }
        return vector;
    }

    @Specialization
    protected RList setData(RAbstractVector value, RList vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
        return setDataInternal(value, vector, positions, currentDimLevel, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, (vec, val, src, dest, na) -> {
            vec.updateDataAt(src, val.getDataAtAsObject(dest % val.getLength()), na);
        });
    }

    @Specialization
    protected RIntVector setData(RAbstractIntVector value, RIntVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
        return setDataInternal(value, vector, positions, currentDimLevel, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, (vec, val, src, dest, na) -> {
            vec.updateDataAt(src, val.getDataAt(dest % val.getLength()), na);
        });
    }

    @Specialization
    protected RDoubleVector setData(RAbstractDoubleVector value, RDoubleVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                    int accDstDimensions) {
        return setDataInternal(value, vector, positions, currentDimLevel, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, (vec, val, src, dest, na) -> {
            vec.updateDataAt(src, val.getDataAt(dest % val.getLength()), na);
        });
    }

    @Specialization
    protected RLogicalVector setData(RAbstractLogicalVector value, RLogicalVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                    int accDstDimensions) {
        return setDataInternal(value, vector, positions, currentDimLevel, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, (vec, val, src, dest, na) -> {
            vec.updateDataAt(src, val.getDataAt(dest % val.getLength()), na);
        });
    }

    @Specialization
    protected RStringVector setData(RAbstractStringVector value, RStringVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                    int accDstDimensions) {
        return setDataInternal(value, vector, positions, currentDimLevel, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, (vec, val, src, dest, na) -> {
            vec.updateDataAt(src, val.getDataAt(dest % val.getLength()), na);
        });
    }

    @Specialization
    protected RComplexVector setData(RAbstractComplexVector value, RComplexVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                    int accDstDimensions) {
        return setDataInternal(value, vector, positions, currentDimLevel, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, (vec, val, src, dest, na) -> {
            vec.updateDataAt(src, val.getDataAt(dest % val.getLength()), na);
        });
    }

    @Specialization
    protected RRawVector setData(RAbstractRawVector value, RRawVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
        return setDataInternal(value, vector, positions, currentDimLevel, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions, (vec, val, src, dest, na) -> {
            vec.updateDataAt(src, val.getDataAt(dest % val.getLength()));
        });
    }
}
