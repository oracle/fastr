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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.nodes.function.RCallNode.VarArgsAsObjectArrayNodeFactory;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "positions", type = RNode[].class)})
public abstract class AccessArrayNode extends RNode {

    private final NACheck elementNACheck = NACheck.create();
    private final NACheck namesNACheck = NACheck.create();

    private static final VarArgsAsObjectArrayNodeFactory varArgAsObjectArrayNodeFactory = new VarArgsAsObjectArrayNodeFactory();

    abstract RNode getVector();

    @CreateCast({"vector"})
    public RNode createCastVector(RNode child) {
        return CastToVectorNodeFactory.create(child, false, false);
    }

    @CreateCast({"positions"})
    public RNode[] createCastPositions(RNode[] children) {
        RNode[] positions = new RNode[children.length];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = ArrayPositionCastFactory.create(i, getVector(), children[i], true);
        }
        return new RNode[]{varArgAsObjectArrayNodeFactory.makeList(positions, null)};
    }

    @SuppressWarnings("unused")
    @Specialization(order = 1, guards = "wrongDimensions")
    Object access(RAbstractVector vector, Object[] positions) {
        throw RError.getIncorrectDimensions(getEncapsulatingSourceSection());
    }

    private RStringVector getNames(RAbstractVector vector, Object[] positions, int currentDimLevel) {
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int numPositions = p.getLength();
        if (numPositions > 1) {
            RList dimNames = vector.getDimNames();
            RStringVector srcNames = dimNames == null ? null : (dimNames.getDataAt(currentDimLevel - 1) == RNull.instance ? null : (RStringVector) dimNames.getDataAt(currentDimLevel - 1));
            if (srcNames == null) {
                return null;
            } else {
                namesNACheck.enable(!srcNames.isComplete() || !p.isComplete());
                String[] namesData = new String[numPositions];
                for (int i = 0; i < p.getLength(); i++) {
                    int pos = p.getDataAt(i);
                    if (namesNACheck.check(pos)) {
                        namesData[i] = RRuntime.STRING_NA;
                    } else {
                        namesData[i] = srcNames.getDataAt(pos - 1);
                        namesNACheck.check(namesData[i]);
                    }
                }
                RStringVector dstNames = RDataFactory.createStringVector(namesData, namesNACheck.neverSeenNA());
                return dstNames;
            }
        }
        if (currentDimLevel == 1) {
            return null;
        } else {
            return getNames(vector, positions, currentDimLevel - 1);
        }
    }

    private void getDimNames(RList dstDimNames, RAbstractVector vector, Object[] positions, int currentSrcDimLevel, int currentDstDimLevel) {
        if (currentSrcDimLevel == 0) {
            return;
        }
        RIntVector p = (RIntVector) positions[currentSrcDimLevel - 1];
        int numPositions = p.getLength();
        if (numPositions > 1) {
            RList srcDimNames = vector.getDimNames();
            RStringVector srcNames = srcDimNames == null ? null : (srcDimNames.getDataAt(currentSrcDimLevel - 1) == RNull.instance ? null
                            : (RStringVector) srcDimNames.getDataAt(currentSrcDimLevel - 1));
            if (srcNames == null) {
                dstDimNames.updateDataAt(currentDstDimLevel - 1, RNull.instance, null);
            } else {
                namesNACheck.enable(!srcNames.isComplete() || !p.isComplete());
                String[] namesData = new String[numPositions];
                for (int i = 0; i < p.getLength(); i++) {
                    int pos = p.getDataAt(i);
                    if (namesNACheck.check(pos)) {
                        namesData[i] = RRuntime.STRING_NA;
                    } else {
                        namesData[i] = srcNames.getDataAt(pos - 1);
                        namesNACheck.check(namesData[i]);
                    }
                }
                RStringVector dstNames = RDataFactory.createStringVector(namesData, namesNACheck.neverSeenNA());
                dstDimNames.updateDataAt(currentDstDimLevel - 1, dstNames, null);
            }
            getDimNames(dstDimNames, vector, positions, currentSrcDimLevel - 1, currentDstDimLevel - 1);
        } else {
            if (p.getDataAt(0) == 0) {
                dstDimNames.updateDataAt(currentDstDimLevel - 1, RNull.instance, null);
                getDimNames(dstDimNames, vector, positions, currentSrcDimLevel - 1, currentDstDimLevel - 1);
            } else {
                getDimNames(dstDimNames, vector, positions, currentSrcDimLevel - 1, currentDstDimLevel);
            }
        }

    }

    private void getData(int[] data, RIntVector vector, Object[] positions, int currentDimLevel, int srcArrayBase, int dstArrayBase, int accSrcDimensions, int accDstDimensions) {
        int[] srcDimensions = vector.getDimensions();
        RIntVector p = (RIntVector) positions[currentDimLevel - 1];
        int srcDimSize = srcDimensions[currentDimLevel - 1];
        int newAccSrcDimensions = accSrcDimensions / srcDimSize;
        int newAccDstDimensions = accDstDimensions / p.getLength();
        elementNACheck.enable(p);
        if (currentDimLevel == 1) {
            for (int i = 0; i < p.getLength(); i++) {
                int dstIndex = dstArrayBase + newAccDstDimensions * i;
                if (srcArrayBase == -1) {
                    data[dstIndex] = RRuntime.INT_NA;
                } else {
                    int pos = p.getDataAt(i);
                    if (elementNACheck.check(pos)) {
                        data[dstIndex] = RRuntime.INT_NA;
                    } else {
                        int srcIndex = srcArrayBase + newAccSrcDimensions * (pos - 1);
                        data[dstIndex] = vector.getDataAt(srcIndex);
                        elementNACheck.check(data[dstIndex]);
                    }
                }
            }
        } else {
            for (int i = 0; i < p.getLength(); i++) {
                int newDstArrayBase = dstArrayBase + newAccDstDimensions * i;
                int newSrcArrayBase;
                if (srcArrayBase == -1) {
                    newSrcArrayBase = -1;
                } else {
                    int pos = p.getDataAt(i);
                    if (elementNACheck.check(pos)) {
                        newSrcArrayBase = -1;
                    } else {
                        newSrcArrayBase = srcArrayBase + newAccSrcDimensions * (pos - 1);
                    }
                }
                getData(data, vector, positions, currentDimLevel - 1, newSrcArrayBase, newDstArrayBase, newAccSrcDimensions, newAccDstDimensions);
            }
        }
    }

    @Specialization(order = 10)
    RIntVector access(RIntVector vector, Object[] positions) {
        // compute length of dimensions array and of the resulting vector
        int dimLength = 0;
        int resLength = 1;
        int multi = 0; // how many dimension > 1 ?
        boolean zeroDim = false;
        for (int i = 0; i < positions.length; i++) {
            RIntVector p = (RIntVector) positions[i];
            int pLength = p.getLength();
            if (pLength == 1 && p.getDataAt(0) != 0) {
                continue;
            }
            if (pLength > 1) {
                multi++;
                if (resLength == 0) {
                    // dimension of size 0
                    zeroDim = true;
                } else {
                    resLength *= pLength;
                }
            } else {
                resLength = 0;
            }
            dimLength++;
        }
        // create dimensions array
        int[] dimensions = null;

        if (multi > 1 || zeroDim) {
            dimensions = new int[dimLength];
            int ind = 0;
            for (int i = 0; i < positions.length; i++) {
                RIntVector p = (RIntVector) positions[i];
                int pLength = p.getLength();
                if (pLength == 1 && p.getDataAt(0) != 0) {
                    continue;
                }
                if (pLength > 1) {
                    dimensions[ind++] = pLength;
                } else {
                    dimensions[ind++] = 0;
                }
            }
        }
        int[] srcDimensions = vector.getDimensions();
        int numSrcDimensions = srcDimensions.length;
        int[] data;
        if (resLength == 0) {
            data = new int[0];
        } else {
            data = new int[resLength];
            RIntVector p = (RIntVector) positions[positions.length - 1];
            int srcDimSize = srcDimensions[numSrcDimensions - 1];
            int accSrcDimensions = vector.getLength() / srcDimSize;
            int accDstDimensions = resLength / p.getLength();

            elementNACheck.enable(!vector.isComplete() || !p.isComplete());
            for (int i = 0; i < p.getLength(); i++) {
                int dstArrayBase = accDstDimensions * i;
                int srcArrayBase;
                int pos = p.getDataAt(i);
                if (elementNACheck.check(pos)) {
                    srcArrayBase = -1; // fill with NAs at the lower levels
                } else {
                    srcArrayBase = accSrcDimensions * (pos - 1);
                }
                getData(data, vector, positions, numSrcDimensions - 1, srcArrayBase, dstArrayBase, accSrcDimensions, accDstDimensions);
            }
        }
        RList dimNames = vector.getDimNames();
        if (dimNames == null) {
            return RDataFactory.createIntVector(data, elementNACheck.neverSeenNA(), dimensions);
        } else {
            if (dimensions == null) {
                // construct names
                RStringVector names = getNames(vector, positions, numSrcDimensions);
                return RDataFactory.createIntVector(data, true, dimensions, names);
            } else {
                // construct dimnames
                RIntVector resultVector = RDataFactory.createIntVector(data, elementNACheck.neverSeenNA(), dimensions);
                int numDstDimensions = dimLength;
                RList dstDimNames = RDataFactory.createList(new Object[dimLength]);
                getDimNames(dstDimNames, vector, positions, numSrcDimensions, numDstDimensions);
                resultVector.setDimNames(dstDimNames);
                return resultVector;
            }
        }
    }

    protected boolean wrongDimensions(RAbstractVector vector, Object[] positions) {
        return vector.getDimensions() == null || vector.getDimensions().length != positions.length;
    }

    public static AccessArrayNode create(RNode vector, RNode[] positions) {
        return AccessArrayNodeFactory.create(vector, positions);
    }

}
