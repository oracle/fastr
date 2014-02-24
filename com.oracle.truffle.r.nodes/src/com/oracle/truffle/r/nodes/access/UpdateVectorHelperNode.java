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

import java.util.*;
import java.util.Map.Entry;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@SuppressWarnings({"static-method", "unused"})
@NodeChildren({@NodeChild(value = "vector", type = RNode.class), @NodeChild(value = "newValue", type = RNode.class), @NodeChild(value = "position", type = RNode.class)})
public abstract class UpdateVectorHelperNode extends CoercedBinaryOperationNode {

    // TODO: enable right NA check
    private final NACheck leftNACheck;
    private final NACheck rightNACheck;
    private final NACheck positionNACheck;

    @CompilationFinal private boolean isSubset;

    public void setSubset(boolean s) {
        isSubset = s;
    }

    protected UpdateVectorHelperNode() {
        leftNACheck = NACheck.create();
        rightNACheck = NACheck.create();
        positionNACheck = NACheck.create();
    }

    protected UpdateVectorHelperNode(UpdateVectorHelperNode other) {
        leftNACheck = other.leftNACheck;
        rightNACheck = other.rightNACheck;
        positionNACheck = other.positionNACheck;
        isSubset = other.isSubset;
    }

    @CreateCast({"position"})
    public RNode createCastPosition(RNode child) {
        return VectorPositionCastFactory.create(positionNACheck, child);
    }

    @Specialization(order = 23, guards = "isSingleElementEmptyPosition")
    public Object access(RAbstractVector vector, Object right, RAbstractVector position) {
        throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
    }

    protected boolean isSingleElementEmptyPosition(RAbstractVector vector, Object right, RAbstractVector position) {
        return !isSubset && position.getLength() == 0;
    }

    // Scalar assigned to logical vector with scalar position.

    @Specialization(order = 100, guards = "!positionEqualsZero")
    public RLogicalVector doLogical(RLogicalVector vector, byte right, int position) {
        return vector.updateDataAt(position - 1, right, rightNACheck);
    }

    @Specialization(order = 101, guards = "!positionEqualsZero")
    public RLogicalVector doLogical(RLogicalVector vector, RLogicalVector right, int position) {
        assert right.getLength() == 1;
        return doLogical(vector, right.getDataAt(0), position);
    }

    @Specialization(order = 200, guards = "!positionEqualsZero")
    public RIntVector doInt(RIntVector vector, int right, int position) {
        RIntVector result = vector;
        if (position > result.getLength()) {
            result = result.copyResized(position, true);
        }
        return result.updateDataAt(position - 1, right, rightNACheck);
    }

    @Specialization(order = 201, guards = "!positionEqualsZero")
    public RIntVector doInt(RIntVector vector, RIntVector right, int position) {
        assert right.getLength() == 1;
        return doInt(vector, right.getDataAt(0), position);
    }

    @Specialization(order = 300, guards = "!positionEqualsZero")
    public RDoubleVector doDouble(RDoubleVector vector, double right, int position) {
        RDoubleVector result = vector;
        if (position > result.getLength()) {
            result = result.copyResized(position, true);
        }
        return result.updateDataAt(position - 1, right, rightNACheck);
    }

    @Specialization(order = 301, guards = "!positionEqualsZero")
    public RDoubleVector doDouble(RDoubleVector vector, RDoubleVector right, int position) {
        assert right.getLength() == 1;
        return doDouble(vector, right.getDataAt(0), position);
    }

    @Specialization(order = 400, guards = "!positionEqualsZero")
    public RComplexVector doComplex(RComplexVector vector, RComplex right, int position) {
        return vector.updateDataAt(position - 1, right, rightNACheck);
    }

    @Specialization(order = 401, guards = "!positionEqualsZero")
    public RComplexVector doComplex(RComplexVector vector, RComplexVector right, int position) {
        assert right.getLength() == 1;
        return doComplex(vector, right.getDataAt(0), position);
    }

    @Specialization(order = 500, guards = "!positionEqualsZero")
    public RStringVector doString(RStringVector vector, String right, int position) {
        RStringVector result = vector;
        if (position > result.getLength()) {
            result = result.copyResized(position, true);
        }
        return result.updateDataAt(position - 1, right, rightNACheck);
    }

    @Specialization(order = 501, guards = "!positionEqualsZero")
    public RStringVector doString(RStringVector vector, RStringVector right, int position) {
        assert right.getLength() == 1;
        return doString(vector, right.getDataAt(0), position);
    }

    @Specialization(order = 999, guards = "positionEqualsZero")
    public RAbstractVector doPos0(RAbstractVector vector, RAbstractVector right, int position) {
        assert position == 0;
        return vector;
    }

    public static boolean positiveSequenceInBounds(RAbstractVector vector, RIntVector right, RIntSequence position) {
        return position.getEnd() <= vector.getLength();
    }

    @CompilationFinal private boolean hasSeenDifferentSize;

    @Specialization(order = 1000, guards = "positiveSequenceInBounds")
    public RIntVector doIntVectorIntVectorIntSequence(RIntVector vector, RIntVector right, RIntSequence positions) {
        assert positions.getStart() > 0 && positions.getEnd() > 0;
        int rightIndex = 0;
        if (positions.getLength() != right.getLength()) {
            if (!hasSeenDifferentSize) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                hasSeenDifferentSize = true;
            }
        }
        for (int i = 0; i < positions.getLength(); ++i) {
            int index = positions.getDataAt(i) - 1;
            int data;
            if (hasSeenDifferentSize) {
                data = right.getDataAt(rightIndex);
                rightIndex = Utils.incMod(rightIndex, right.getLength());
            } else {
                data = right.getDataAt(i);
            }
            vector.updateDataAt(index, data, rightNACheck);
        }
        return vector;
    }

    // TODO(tw): Remove duplicate logic with AccessVectorNode once basic benchmarks work.

    @CompilationFinal private boolean hasSeenNA;
    @CompilationFinal private boolean hasSeenPositive;
    @CompilationFinal private boolean hasSeenZero;
    @CompilationFinal private boolean hasSeenNegative;
    @CompilationFinal private boolean hasSeenZeroNegatives;
    @CompilationFinal private boolean hasSeenOutOfBounds;

    @Specialization(order = 1001)
    public RIntVector doIntVectorIntVector(RIntVector vector, RIntVector right, RIntVector positions) {

        int positionLength = positions.getLength();
        int maxPositiveOutOfBounds = 0;
        positionNACheck.enable(positions);
        for (int i = 0; i < positionLength; ++i) {
            int pos = positions.getDataAt(i);
            if (positionNACheck.check(pos)) {
                if (!hasSeenNA) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    hasSeenNA = true;
                }
            } else if (pos > 0) {
                if (!hasSeenPositive) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    hasSeenPositive = true;
                }
                if (pos > vector.getLength()) {
                    if (!hasSeenOutOfBounds) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        hasSeenOutOfBounds = true;
                    }
                    maxPositiveOutOfBounds = Math.max(maxPositiveOutOfBounds, pos);
                }
            } else if (pos == 0) {
                if (!hasSeenZero) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    hasSeenZero = true;
                }
            } else {
                if (!hasSeenNegative) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    hasSeenNegative = true;
                }
            }
        }

        if (hasSeenNegative) {
            if (hasSeenPositive || hasSeenNA) {
                CompilerDirectives.transferToInterpreter();
                throw RError.getOnlyZeroMixed(getEncapsulatingSourceSection());
            }
            boolean[] selectVector = new boolean[vector.getLength()];
            int selectionCount = selectVector.length;
            Arrays.fill(selectVector, true);
            for (int i = 0; i < positionLength; ++i) {
                int pos = positions.getDataAt(i);
                if (hasSeenZero && pos == 0) {
                    continue;
                }

                assert pos < 0;
                int index = (-pos) - 1;
                if (index < selectVector.length && selectVector[index]) {
                    selectVector[index] = false;
                    selectionCount--;
                }
            }

            int[] result = new int[selectionCount];
            int inputIndex = 0;
            for (int i = 0; i < selectVector.length; ++i) {
                if (selectVector[i]) {
                    vector.updateDataAt(i, right.getDataAt(inputIndex), rightNACheck);
                    inputIndex = Utils.incMod(inputIndex, right.getLength());
                }
            }
            return vector;
        } else {
            if (hasSeenNA) {
                CompilerDirectives.transferToInterpreter();
                throw RError.getNASubscripted(getEncapsulatingSourceSection());
            }

            if (!hasSeenZeroNegatives) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                hasSeenZeroNegatives = true;
            }

            RIntVector resultVector = vector;
            if (hasSeenOutOfBounds && maxPositiveOutOfBounds > vector.getLength()) {
                // Adjust result vector size
                resultVector = vector.copyResized(maxPositiveOutOfBounds, true);
            }

            int inputIndex = 0;
            for (int i = 0; i < positionLength; ++i) {
                int pos = positions.getDataAt(i);
                if (hasSeenZero && pos == 0) {
                    continue;
                }

                resultVector.updateDataAt(pos - 1, right.getDataAt(inputIndex), rightNACheck);
                inputIndex = Utils.incMod(inputIndex, right.getLength());
            }
            return resultVector;
        }
    }

    @Specialization(order = 1002)
    public RIntVector doDouble(RIntVector left, RIntVector right, RLogicalVector positions) {
        rightNACheck.enable(right);
        positionNACheck.enable(positions);
        int rightIndex = 0;
        int posIndex = 0;
        for (int leftIndex = 0; leftIndex < left.getLength(); leftIndex++) {
            byte value = positions.getDataAt(posIndex);
            if (positionNACheck.check(value)) {
                throw RError.getNASubscripted(getEncapsulatingSourceSection());
            }
            if (value == RRuntime.LOGICAL_TRUE) {
                rightIndex = Utils.incMod(rightIndex, right.getLength());
                left.updateDataAt(leftIndex, right.getDataAt(rightIndex), rightNACheck);
            }
            posIndex = Utils.incMod(posIndex, positions.getLength());
        }
        return left;
    }

    // list updates

    @Specialization(order = 1200, guards = "positionEqualsZero")
    public RList doList(RList list, Object right, int position) {
        if (isSubset) {
            return list;
        } else {
            throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
        }
    }

    @Specialization(order = 1201, guards = "singlePositionInBounds")
    public RList doList(RList list, RAbstractVector right, int position) {
        if (isSubset) {
            list.updateDataAt(position - 1, right.getDataAtAsObject(0), rightNACheck);
        } else {
            list.updateDataAt(position - 1, right, rightNACheck);
        }
        return list;
    }

    @Specialization(order = 1210, guards = "singlePositionInBounds")
    public RList doList(RList list, RNull right, int position) {
        Object[] data = list.getDataCopy();
        Object[] result = new Object[data.length - 1];
        System.arraycopy(data, 0, result, 0, position - 1);
        System.arraycopy(data, position, result, position - 1, data.length - position);
        return RDataFactory.createList(result);
    }

    @Specialization(order = 1211, guards = "singlePositionOutOfBounds")
    public RList doListOutOfBounds(RList list, RNull right, int position) {
        if (isSubset) {
            Object[] data = list.getDataCopy();
            Object[] result = new Object[position - 1];
            Arrays.fill(result, RNull.instance);
            System.arraycopy(data, 0, result, 0, data.length);
            return RDataFactory.createList(result);
        } else {
            return list;
        }
    }

    @Specialization(order = 1220)
    public RList doList(RList list, RNull right, RIntVector position) {
        Object[] data = list.getDataCopy();
        int deleted = 0;
        for (int i = 0; i < position.getLength(); ++i) {
            int pos = position.getDataAt(i) - 1;
            if (data[pos] != right) {
                ++deleted;
            }
            data[pos] = right;
        }
        Object[] result = new Object[data.length - deleted];
        int w = 0; // write index
        for (int i = 0; i < data.length; ++i) {
            if (data[i] != right) {
                result[w++] = data[i];
            }
        }
        return RDataFactory.createList(result);
    }

    // Vector assigned to vector

    private RLogicalVector doLogical(RLogicalVector vector, RAbstractLogicalVector right, RIntVector positions) {
        throw Utils.nyi();
    }

    private RIntVector doInt(RIntVector vector, RAbstractIntVector right, RIntVector positions) {
        throw Utils.nyi();
    }

    @Specialization(order = 1336)
    public RDoubleVector doDouble(RDoubleVector left, RAbstractDoubleVector right, RAbstractIntVector positions) {
        RIntVector posv = positions.materialize();
        rightNACheck.enable(right);
        for (int posIndex = 0, rightIndex = 0; posIndex < positions.getLength(); ++posIndex, rightIndex = Utils.incMod(rightIndex, right.getLength())) {
            left.updateDataAt(positions.getDataAt(posIndex) - 1, right.getDataAt(rightIndex), rightNACheck);
        }
        return left;
    }

    @Specialization(order = 1337)
    public RDoubleVector doDouble(RDoubleVector left, RDoubleVector right, RLogicalVector positions) {
        rightNACheck.enable(right);
        positionNACheck.enable(positions);
        int rightIndex = 0;
        int posIndex = 0;
        for (int leftIndex = 0; leftIndex < left.getLength(); leftIndex++) {
            byte value = positions.getDataAt(posIndex);
            if (positionNACheck.check(value)) {
                throw RError.getNASubscripted(getEncapsulatingSourceSection());
            }
            if (value == RRuntime.LOGICAL_TRUE) {
                rightIndex = Utils.incMod(rightIndex, right.getLength());
                left.updateDataAt(leftIndex, right.getDataAt(rightIndex), rightNACheck);
            }
            posIndex = Utils.incMod(posIndex, positions.getLength());
        }
        return left;
    }

    @Specialization(order = 1338)
    public RAbstractVector doStringPositions(RAbstractVector left, RAbstractVector right, RStringVector positions) {
        rightNACheck.enable(right);
        return updateWithStringPositions((RVector) left, (RVector) right, positions);
    }

    @CompilerDirectives.SlowPath
    private RAbstractVector updateWithStringPositions(RVector left, RVector right, RStringVector positions) {
        HashMap<String, Integer> nameMap = new HashMap<>();
        Object previousNamesObject = left.getNames();
        if (previousNamesObject != null && previousNamesObject != RNull.instance) {
            RStringVector previousNames = (RStringVector) previousNamesObject;
            for (int i = 0; i < previousNames.getLength(); ++i) {
                String cur = previousNames.getDataAt(i);
                if (cur.length() > 0 && cur != RRuntime.STRING_NA) {
                    if (!nameMap.containsKey(cur)) {
                        nameMap.put(cur, i);
                    }
                }
            }
        }

        int newSize = left.getLength();
        int[] targetPositions = new int[positions.getLength()];
        for (int i = 0; i < positions.getLength(); ++i) {
            int index = -1;
            String s = positions.getDataAt(i);
            if (s.length() == 0 || s == RRuntime.STRING_NA) {
                targetPositions[i] = (newSize++);
            } else {
                Integer foundIndex = nameMap.get(s);
                if (foundIndex != null) {
                    targetPositions[i] = foundIndex;
                } else {
                    targetPositions[i] = (newSize++);
                    nameMap.put(s, targetPositions[i]);
                }
            }
        }

        RVector newVector = left.copyResized(newSize, true);
        int rightIndex = 0;
        for (int i = 0; i < targetPositions.length; ++i) {
            newVector.transferElementSameType(targetPositions[i], right, rightIndex);
            if (rightIndex == right.getLength() - 1) {
                rightIndex = 0;
            } else {
                rightIndex++;
            }
        }

        NACheck naCheck = NACheck.create();
        if (newSize > left.getLength()) {
            RStringVector newNames = null;
            if (previousNamesObject == null || previousNamesObject == RNull.instance) {
                newNames = RDataFactory.createStringVector(newSize);
            } else {
                newNames = ((RStringVector) previousNamesObject).copyResized(newSize, true);
            }
            for (Entry<String, Integer> entry : nameMap.entrySet()) {
                int index = entry.getValue();
                if (previousNamesObject == null || previousNamesObject == RNull.instance || index >= ((RStringVector) previousNamesObject).getLength()) {
                    newNames.updateDataAt(index, entry.getKey(), naCheck);
                }
            }

            newVector.setNames(newNames);
        }
        return newVector;
    }

    private RComplexVector doComplex(RComplexVector createEmptyComplexVector, RAbstractComplexVector right, RIntVector positions) {
        throw Utils.nyi();
    }

    private RStringVector doString(RStringVector createEmptyStringVector, RAbstractStringVector right, RIntVector positions) {
        throw Utils.nyi();
    }

    @Specialization(order = 2000, guards = "positionEqualsOne")
    public int doNull1(RNull nul, RAbstractIntVector right, int position) {
        return right.getDataAt(0);
    }

    @Specialization(order = 2010, guards = "positionEqualsOne")
    public double doNull1(RNull nul, RDoubleVector right, int position) {
        return right.getDataAt(0);
    }

    @Specialization(order = 2011, guards = "!positionEqualsOne")
    public RDoubleVector doNull(RNull nul, RAbstractDoubleVector right, int position) {
        double[] result = new double[position];
        if (position != 0) {
            if (right.getLength() == 0) {
                CompilerDirectives.transferToInterpreter();
                throw RError.getReplacementZero(getEncapsulatingSourceSection());
            }
            for (int i = 0; i < position - 1; i++) {
                result[i] = RRuntime.DOUBLE_NA;
            }
            result[position - 1] = right.getDataAt(0);
        }
        return RDataFactory.createDoubleVector(result, position == 0);
    }

    // FIXME DSL bug: Object arg1 alone does not work
    protected static boolean positionEqualsZero(RAbstractVector arg0, RAbstractVector arg1, int position) {
        return position == 0;
    }

    protected static boolean positionEqualsZero(RAbstractVector arg0, Object arg1, int position) {
        return position == 0;
    }

    protected static boolean positionEqualsOne(RNull arg0, RAbstractVector arg1, int position) {
        return position == 1;
    }

    // FIXME DSL bug: Object arg1 should suffice
    protected static boolean singlePositionInBounds(RAbstractVector arg0, RAbstractVector arg1, int position) {
        return position >= 1 && position <= arg0.getLength();
    }

    // FIXME DSL bug: Object arg1 should suffice
    protected static boolean singlePositionInBounds(RAbstractVector arg0, RNull arg1, int position) {
        return position >= 1 && position <= arg0.getLength();
    }

    // FIXME DSL bug: Object arg1 should suffice
    protected static boolean singlePositionOutOfBounds(RAbstractVector arg0, RAbstractVector arg1, int position) {
        return !positionEqualsZero(arg0, arg1, position) && (position < 0 || position > arg0.getLength());
    }

    // FIXME DSL bug: Object arg1 should suffice
    protected static boolean singlePositionOutOfBounds(RAbstractVector arg0, RNull arg1, int position) {
        return !positionEqualsZero(arg0, arg1, position) && (position < 0 || position > arg0.getLength());
    }
}
