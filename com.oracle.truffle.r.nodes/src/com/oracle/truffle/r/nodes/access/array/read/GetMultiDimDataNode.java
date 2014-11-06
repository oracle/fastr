/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access.array.read;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@NodeChildren({@NodeChild(value = "data", type = RNode.class), @NodeChild(value = "vec", type = RNode.class), @NodeChild(value = "pos", type = RNode.class),
                @NodeChild(value = "currDimLevel", type = RNode.class), @NodeChild(value = "srcArrayBase", type = RNode.class), @NodeChild(value = "dstArrayBase", type = RNode.class),
                @NodeChild(value = "accSrcDimensions", type = RNode.class), @NodeChild(value = "accDstDimensions", type = RNode.class)})
abstract class GetMultiDimDataNode extends RNode {

    public abstract Object executeMultiDimDataGet(VirtualFrame frame, Object data, RAbstractVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase,
                    int accSrcDimensions, int accDstDimensions);

    private final NACheck posNACheck;
    private final NACheck elementNACheck;

    private final ConditionProfile dimLevelOneProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile negativeSrcArrayBaseProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile negativeSrcIndexProfile = ConditionProfile.createBinaryProfile();

    @Child private GetMultiDimDataNode getMultiDimDataRecursive;

    private Object getMultiDimData(VirtualFrame frame, Object data, RAbstractVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                    int accDstDimensions, NACheck posCheck, NACheck elementCheck) {
        if (getMultiDimDataRecursive == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            getMultiDimDataRecursive = insert(GetMultiDimDataNodeFactory.create(posCheck, elementCheck, null, null, null, null, null, null, null, null));
        }
        return getMultiDimDataRecursive.executeMultiDimDataGet(frame, data, vector, positions, currentDimLevel, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
    }

    protected GetMultiDimDataNode(NACheck posNACheck, NACheck elementNACheck) {
        this.posNACheck = posNACheck;
        this.elementNACheck = elementNACheck;
    }

    protected GetMultiDimDataNode(GetMultiDimDataNode other) {
        this.posNACheck = other.posNACheck;
        this.elementNACheck = other.elementNACheck;
    }

    @Specialization
    protected RList getData(VirtualFrame frame, Object d, RList vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
        Object[] data = (Object[]) d;
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        elementNACheck.enable(p);
        if (dimLevelOneProfile.profile(currentDimLevel == 1)) {
            for (int i = 0; i < p.getLength(); i++) {
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, p, i, newAccSrcDimensions);
                if (negativeSrcIndexProfile.profile(srcIndex == -1)) {
                    data[dstIndex] = RNull.instance;
                } else {
                    data[dstIndex] = vector.getDataAt(srcIndex);
                }
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                getMultiDimData(frame, data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
            }
        }
        return vector;
    }

    @Specialization
    protected RAbstractIntVector getData(VirtualFrame frame, Object d, RAbstractIntVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                    int accDstDimensions) {
        int[] data = (int[]) d;
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        elementNACheck.enable(p);
        if (dimLevelOneProfile.profile(currentDimLevel == 1)) {
            for (int i = 0; i < p.getLength(); i++) {
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, p, i, newAccSrcDimensions);
                if (negativeSrcIndexProfile.profile(srcIndex == -1)) {
                    data[dstIndex] = RRuntime.INT_NA;
                } else {
                    data[dstIndex] = vector.getDataAt(srcIndex);
                    elementNACheck.check(data[dstIndex]);
                }
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                getMultiDimData(frame, data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
            }
        }
        return vector;
    }

    @Specialization
    protected RAbstractDoubleVector getData(VirtualFrame frame, Object d, RAbstractDoubleVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase,
                    int accSrcDimensions, int accDstDimensions) {
        double[] data = (double[]) d;
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        elementNACheck.enable(p);
        if (dimLevelOneProfile.profile(currentDimLevel == 1)) {
            for (int i = 0; i < p.getLength(); i++) {
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, p, i, newAccSrcDimensions);
                if (negativeSrcIndexProfile.profile(srcIndex == -1)) {
                    data[dstIndex] = RRuntime.DOUBLE_NA;
                } else {
                    data[dstIndex] = vector.getDataAt(srcIndex);
                    elementNACheck.check(data[dstIndex]);
                }
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                getMultiDimData(frame, data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
            }
        }
        return vector;
    }

    @Specialization
    protected RAbstractLogicalVector getData(VirtualFrame frame, Object d, RAbstractLogicalVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase,
                    int accSrcDimensions, int accDstDimensions) {
        byte[] data = (byte[]) d;
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        elementNACheck.enable(p);
        if (dimLevelOneProfile.profile(currentDimLevel == 1)) {
            for (int i = 0; i < p.getLength(); i++) {
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, p, i, newAccSrcDimensions);
                if (negativeSrcIndexProfile.profile(srcIndex == -1)) {
                    data[dstIndex] = RRuntime.LOGICAL_NA;
                } else {
                    data[dstIndex] = vector.getDataAt(srcIndex);
                    elementNACheck.check(data[dstIndex]);
                }
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                getMultiDimData(frame, data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
            }
        }
        return vector;
    }

    @Specialization
    protected RAbstractStringVector getData(VirtualFrame frame, Object d, RAbstractStringVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase,
                    int accSrcDimensions, int accDstDimensions) {
        String[] data = (String[]) d;
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        elementNACheck.enable(p);
        if (dimLevelOneProfile.profile(currentDimLevel == 1)) {
            for (int i = 0; i < p.getLength(); i++) {
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, p, i, newAccSrcDimensions);
                if (negativeSrcIndexProfile.profile(srcIndex == -1)) {
                    data[dstIndex] = RRuntime.STRING_NA;
                } else {
                    data[dstIndex] = vector.getDataAt(srcIndex);
                    elementNACheck.check(data[dstIndex]);
                }
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                getMultiDimData(frame, data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
            }
        }
        return vector;
    }

    @Specialization
    protected RAbstractComplexVector getData(VirtualFrame frame, Object d, RAbstractComplexVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase,
                    int accSrcDimensions, int accDstDimensions) {
        double[] data = (double[]) d;
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        elementNACheck.enable(p);
        if (dimLevelOneProfile.profile(currentDimLevel == 1)) {
            for (int i = 0; i < p.getLength(); i++) {
                int dstIndex = (dstArrayBase + newAccDstDimensions * i) << 1;
                int srcIndex = getSrcIndex(srcArrayBase, p, i, newAccSrcDimensions);
                if (negativeSrcIndexProfile.profile(srcIndex == -1)) {
                    data[dstIndex] = RRuntime.COMPLEX_NA_REAL_PART;
                    data[dstIndex + 1] = RRuntime.COMPLEX_NA_IMAGINARY_PART;
                } else {
                    data[dstIndex] = vector.getDataAt(srcIndex).getRealPart();
                    data[dstIndex + 1] = vector.getDataAt(srcIndex).getImaginaryPart();
                    elementNACheck.check(data[dstIndex]);
                }
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                getMultiDimData(frame, data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
            }
        }
        return vector;
    }

    @Specialization
    protected RAbstractRawVector getData(VirtualFrame frame, Object d, RAbstractRawVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions,
                    int accDstDimensions) {
        byte[] data = (byte[]) d;
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        elementNACheck.enable(p);
        if (dimLevelOneProfile.profile(currentDimLevel == 1)) {
            for (int i = 0; i < p.getLength(); i++) {
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                int srcIndex = getSrcIndex(srcArrayBase, p, i, newAccSrcDimensions);
                if (negativeSrcIndexProfile.profile(srcIndex == -1)) {
                    data[dstIndex] = 0;
                } else {
                    data[dstIndex] = vector.getDataAt(srcIndex).getValue();
                }
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase = getNewArrayBase(srcArrayBase, p, i, newAccSrcDimensions);
                getMultiDimData(frame, data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions, posNACheck, elementNACheck);
            }
        }
        return vector;
    }

    private int getNewArrayBase(int srcArrayBase, RIntVector p, int i, int newAccSrcDimensions) {
        int newSrcArrayBase;
        if (negativeSrcArrayBaseProfile.profile(srcArrayBase == -1)) {
            newSrcArrayBase = -1;
        } else {
            int pos = p.getDataAt(i);
            if (elementNACheck.check(pos)) {
                newSrcArrayBase = -1;
            } else {
                newSrcArrayBase = srcArrayBase + newAccSrcDimensions * (pos - 1);
            }
        }
        return newSrcArrayBase;
    }

    private int getSrcIndex(int srcArrayBase, RIntVector p, int i, int newAccSrcDimensions) {
        if (negativeSrcArrayBaseProfile.profile(srcArrayBase == -1)) {
            return -1;
        } else {
            int pos = p.getDataAt(i);
            if (elementNACheck.check(pos)) {
                return -1;
            } else {
                return srcArrayBase + newAccSrcDimensions * (pos - 1);
            }
        }
    }

}
