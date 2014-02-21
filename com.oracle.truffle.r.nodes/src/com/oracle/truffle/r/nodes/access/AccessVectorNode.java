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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.AccessVectorNodeFactory.AccessVectorVectorCastFactory;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ops.na.*;

@NodeChildren({@NodeChild("vector"), @NodeChild("position")})
public abstract class AccessVectorNode extends RNode {

    private final NACheck positionNACheck;
    private final NACheck resultNACheck;

    @CompilationFinal private boolean isSubset;

    public void setSubset(boolean s) {
        isSubset = s;
    }

    protected AccessVectorNode() {
        positionNACheck = NACheck.create();
        resultNACheck = NACheck.create();
    }

    protected AccessVectorNode(AccessVectorNode other) {
        this.positionNACheck = other.positionNACheck;
        this.resultNACheck = other.resultNACheck;
        this.isSubset = other.isSubset;
    }

    @CreateCast({"vector"})
    public RNode createCastVector(RNode child) {
        return AccessVectorVectorCastFactory.create(child);
    }

    @CreateCast({"position"})
    public RNode createCastPosition(RNode child) {
        return VectorPositionCastFactory.create(positionNACheck, child);
    }

    // Vector is NULL

    @Specialization(order = 0)
    public RNull access(RNull vector, @SuppressWarnings("unused") Object position) {
        return vector;
    }

    @Specialization(order = 23, guards = "isSingleElementEmptyPosition")
    @SuppressWarnings("unused")
    public Object access(RAbstractVector vector, RAbstractVector position) {
        throw RError.getSelectLessThanOne(getEncapsulatingSourceSection());
    }

    protected boolean isSingleElementEmptyPosition(@SuppressWarnings("unused") RAbstractVector vector, RAbstractVector position) {
        return !isSubset && position.getLength() == 0;
    }

    // Int sequence access

    @Specialization(order = 100, guards = "isInBounds")
    public int accessIntVectorIntScalar(RIntSequence vector, int position) {
        return accessInBoundsIntVector(vector, position);
    }

    @Specialization(order = 101, guards = "isNA")
    public int accessNA(@SuppressWarnings("unused") RIntSequence vector, @SuppressWarnings("unused") int position) {
        return RRuntime.INT_NA;
    }

    @Specialization(order = 108, guards = "isAboveLength")
    public int accessAboveLength(@SuppressWarnings("unused") RIntSequence vector, @SuppressWarnings("unused") int position) {
        return RRuntime.INT_NA;
    }

    @Specialization(order = 102, guards = "isMinusLength")
    public RIntSequence accessMinusLength(RIntSequence vector, @SuppressWarnings("unused") int position) {
        return vector.removeLast();
    }

    @Specialization(order = 103, guards = "isMinusOne")
    public RIntSequence accessMinusOne(RIntSequence vector, @SuppressWarnings("unused") int position) {
        return vector.removeFirst();
    }

    @Specialization(order = 104, guards = "isMinusInBounds")
    public RIntVector accessMinusInBounds(RIntSequence vector, int position) {
        return accessMinusInBoundsIntVector(vector, position);
    }

    @Specialization(order = 106, guards = "isBelowMinusLengthOrZero")
    public RIntVector accessBelowMinusLength(RIntSequence vector, int position) {
        return accessBelowMinusLengthHelper(vector, position);
    }

    @Specialization(order = 111, guards = "positiveSequenceInBounds")
    public RIntVector accessIntSequence(RIntSequence vector, RIntSequence positions) {
        assert positions.getStart() > 0 && positions.getEnd() > 0;
        return accessPositiveInBounds(vector, positions);
    }

    @Specialization(order = 112, guards = "!positiveSequenceInBounds")
    public RIntVector accessIntSequenceOutOfBounds(RIntSequence vector, RIntSequence positions) {
        assert positions.getStart() > 0 && positions.getEnd() > 0;
        return accessPositiveOutOfBounds(vector, positions);
    }

    @Specialization(order = 113)
    public RIntVector accessIntVectorIntVector(RIntSequence vector, RIntVector positions) {
        return accessAbstractIntVectorIntVector(vector, positions);
    }

    @Specialization(order = 114, guards = "isTrue")
    public RIntVector accessTrue(RIntSequence vector, @SuppressWarnings("unused") byte position) {
        return vector.materialize();
    }

    @SuppressWarnings("unused")
    @Specialization(order = 115, guards = "isFalse")
    public RIntVector accessFalse(RIntSequence vector, byte position) {
        return RDataFactory.createEmptyIntVector();
    }

    @SuppressWarnings("unused")
    @Specialization(order = 116, guards = "isNA")
    public RIntVector accessLogicalNA(RIntSequence vector, byte position) {
        return extractLogicalNAHelper(vector);
    }

    // Int vector access

    @Specialization(order = 200, guards = "isInBounds")
    public int accessIntVectorIntScalar(RIntVector vector, int position) {
        return accessInBoundsIntVector(vector, position);
    }

    @Specialization(order = 201, guards = "isNA")
    public int accessNA(@SuppressWarnings("unused") RIntVector vector, @SuppressWarnings("unused") int position) {
        return RRuntime.INT_NA;
    }

    @Specialization(order = 208, guards = "isAboveLength")
    public int accessAboveLength(@SuppressWarnings("unused") RIntVector vector, @SuppressWarnings("unused") int position) {
        return RRuntime.INT_NA;
    }

    @Specialization(order = 202, guards = "isMinusLength")
    public RIntVector accessMinusLength(RIntVector vector, @SuppressWarnings("unused") int position) {
        return vector.removeLast();
    }

    @Specialization(order = 203, guards = "isMinusOne")
    public RIntVector accessMinusOne(RIntVector vector, @SuppressWarnings("unused") int position) {
        return vector.removeFirst();
    }

    @Specialization(order = 204, guards = "isMinusInBounds")
    public RIntVector accessMinusInBounds(RIntVector vector, int position) {
        return accessMinusInBoundsIntVector(vector, position);
    }

    @Specialization(order = 206, guards = "isBelowMinusLengthOrZero")
    public RIntVector accessBelowMinusLength(RIntVector vector, int position) {
        return accessBelowMinusLengthHelper(vector, position);
    }

    @Specialization(order = 211, guards = "positiveSequenceInBounds")
    public RIntVector accessIntSequence(RIntVector vector, RIntSequence positions) {
        assert positions.getStart() > 0 && positions.getEnd() > 0;
        return accessPositiveInBounds(vector, positions);
    }

    @Specialization(order = 212, guards = "!positiveSequenceInBounds")
    public RIntVector accessIntSequenceOutOfBounds(RIntVector vector, RIntSequence positions) {
        assert positions.getStart() > 0 && positions.getEnd() > 0;
        return accessPositiveOutOfBounds(vector, positions);
    }

    @Specialization(order = 213)
    public RIntVector accessIntVectorIntVector(RIntVector vector, RIntVector positions) {
        return accessAbstractIntVectorIntVector(vector, positions);
    }

    @Specialization(order = 214, guards = "isTrue")
    public RIntVector accessTrue(RIntVector vector, @SuppressWarnings("unused") byte position) {
        vector.makeShared();
        return vector;
    }

    @SuppressWarnings("unused")
    @Specialization(order = 215, guards = "isFalse")
    public RIntVector accessFalse(RIntVector vector, byte position) {
        return RDataFactory.createEmptyIntVector();
    }

    @SuppressWarnings("unused")
    @Specialization(order = 216, guards = "isNA")
    public RIntVector accessLogicalNA(RIntVector vector, byte position) {
        return extractLogicalNAHelper(vector);
    }

    @Specialization(order = 220)
    public RIntVector accessIntVectorLogicalVector(RAbstractIntVector vector, RLogicalVector position) {
        int resultLength = 0;
        int posIndex = 0;
        for (int vecIndex = 0; vecIndex < vector.getLength(); vecIndex++) {
            byte posValue = position.getDataAt(posIndex);
            if (posValue != RRuntime.LOGICAL_FALSE) {
                resultLength++;
            }
            posIndex = Utils.incMod(posIndex, position.getLength());
        }

        positionNACheck.enable(position);
        resultNACheck.enable(vector);
        int[] result = new int[resultLength];
        int resultIndex = 0;
        posIndex = 0;
        for (int vecIndex = 0; vecIndex < vector.getLength(); vecIndex++) {
            byte posValue = position.getDataAt(posIndex);
            if (posValue != RRuntime.LOGICAL_FALSE) {
                int value;
                if (!positionNACheck.check(posValue)) {
                    value = vector.getDataAt(vecIndex);
                    resultNACheck.check(value);
                } else {
                    value = RRuntime.INT_NA;
                }
                result[resultIndex++] = value;
            }
            posIndex = Utils.incMod(posIndex, position.getLength());
        }

        return RDataFactory.createIntVector(result, positionNACheck.neverSeenNA() && resultNACheck.neverSeenNA());
    }

    private static RIntVector accessBelowMinusLengthHelper(RAbstractIntVector vector, int position) {
        if (position == RRuntime.INT_NA) {
            return RDataFactory.createIntVector(new int[]{RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR);
        } else if (position == 0) {
            return RDataFactory.createEmptyIntVector();
        }
        return (RIntVector) vector.materialize().makeShared();
    }

    private static RIntVector extractLogicalNAHelper(RAbstractIntVector vector) {
        int[] data = new int[vector.getLength()];
        Arrays.fill(data, RRuntime.INT_NA);
        return RDataFactory.createIntVector(data, RDataFactory.INCOMPLETE_VECTOR);
    }

    private static RIntVector accessPositiveInBounds(RAbstractIntVector vector, RAbstractIntVector positions) {
        int[] result = new int[positions.getLength()];
        for (int i = 0; i < result.length; ++i) {
            int index = positions.getDataAt(i) - 1;
            result[i] = vector.getDataAt(index);
        }
        return RDataFactory.createIntVector(result, vector.isComplete());
    }

    private static RIntVector accessPositiveOutOfBounds(RAbstractIntVector vector, RIntSequence positions) {
        int[] result = new int[positions.getLength()];
        int i = 0;
        for (; i < result.length; ++i) {
            int index = positions.getDataAt(i) - 1;
            if (index >= vector.getLength()) {
                break;
            }
            result[i] = vector.getDataAt(index);
        }
        for (; i < result.length; ++i) {
            result[i] = RRuntime.INT_NA;
        }
        return RDataFactory.createIntVector(result, vector.isComplete());
    }

    @CompilationFinal private boolean hasSeenPositive;
    @CompilationFinal private boolean hasSeenZero;
    @CompilationFinal private boolean hasSeenNegative;
    @CompilationFinal private boolean hasSeenZeroNegatives;
    @CompilationFinal private boolean hasSeenOutOfBounds;

    private RIntVector accessAbstractIntVectorIntVector(RAbstractIntVector vector, RIntVector positions) {
        int positiveCount = 0;
        int negativeCount = 0;
        int zeroCount = 0;
        positionNACheck.enable(positions);
        int positionLength = positions.getLength();
        for (int i = 0; i < positionLength; ++i) {
            int pos = positions.getDataAt(i);
            if (pos > 0 || positionNACheck.check(pos)) {
                if (!hasSeenPositive) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    hasSeenPositive = true;
                }
                positiveCount++;
            } else if (pos == 0) {
                if (!hasSeenZero) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    hasSeenZero = true;
                }
                zeroCount++;
            } else {
                if (!hasSeenNegative) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    hasSeenNegative = true;
                }
                negativeCount++;
            }
        }

        if (hasSeenNegative && negativeCount > 0) {
            if (hasSeenPositive && positiveCount > 0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
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
            int resultIndex = 0;
            for (int i = 0; i < selectVector.length; ++i) {
                if (selectVector[i]) {
                    result[resultIndex++] = vector.getDataAt(i);
                }
            }
            return RDataFactory.createIntVector(result, vector.isComplete());
        } else {
            if (!hasSeenZeroNegatives) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                hasSeenZeroNegatives = true;
            }
            int[] result = new int[positionLength - zeroCount];
            int resultPosition = 0;
            for (int i = 0; i < positionLength; ++i) {
                int pos = positions.getDataAt(i);
                if (hasSeenZero && pos == 0) {
                    continue;
                }

                int curResult = 0;
                if (pos > vector.getLength() || positionNACheck.check(pos)) {
                    if (!hasSeenOutOfBounds) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        hasSeenOutOfBounds = true;
                    }
                    curResult = RRuntime.INT_NA;
                } else {
                    curResult = vector.getDataAt(pos - 1);
                }

                int index = i;
                if (hasSeenZero) {
                    index = resultPosition;
                }

                result[index] = curResult;

                if (hasSeenZero) {
                    resultPosition++;
                }
            }
            return RDataFactory.createIntVector(result, vector.isComplete());
        }
    }

    public static boolean positiveSequenceInBounds(RAbstractVector vector, RIntSequence position) {
        return position.getEnd() <= vector.getLength();
    }

    public static boolean positiveVectorInBounds(RAbstractVector vector, RIntVector position) {
        int vectorLength = vector.getLength();
        for (int i = 0; i < position.getLength(); ++i) {
            if (position.getDataAt(i) <= 0 || position.getDataAt(i) > vectorLength) {
                return false;
            }
        }
        return true;
    }

    public static boolean nullNames(RAbstractVector vector) {
        return vector.getNames() == null || vector.getNames() == RNull.instance;
    }

    @Specialization(order = 239, guards = "nullNames")
    @SuppressWarnings("unused")
    public RIntVector accessIntVectorStringNullNames(RAbstractIntVector vector, String index) {
        return RDataFactory.createIntVector(new int[]{RRuntime.INT_NA}, RDataFactory.INCOMPLETE_VECTOR);
    }

    @Specialization(order = 240, guards = "!nullNames")
    public RIntVector accessIntVectorString(RAbstractIntVector vector, String index) {
        if (vector.getNames() == null) {
            return RDataFactory.createIntVector(new int[]{RRuntime.INT_NA}, false);
        } else if (index.length() == 0 || index == RRuntime.STRING_NA) {
            return createIntNA();
        } else {
            int found = -1;
            RStringVector vnames = (RStringVector) vector.getNames();
            for (int i = 0; i < vnames.getLength(); ++i) {
                String cur = vnames.getDataAt(i);
                if (cur.length() != 0 && cur != RRuntime.STRING_NA && index.equals(cur)) {
                    found = i;
                    break;
                }
            }

            if (found == -1) {
                return createIntNA();
            } else {
                RIntVector result = RDataFactory.createIntVector(new int[]{vector.getDataAt(found)}, vector.isComplete());
                result.setNames(RDataFactory.createStringVector(new String[]{vnames.getDataAt(found)}, true));
                return result;
            }
        }
    }

    private static RIntVector createIntNA() {
        RIntVector result = RDataFactory.createIntVector(new int[]{RRuntime.INT_NA}, false);
        result.setNames(RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, false));
        return result;
    }

    // Double vector access

    @Specialization(order = 300, guards = "isInBounds")
    public double access(RDoubleVector vector, int position) {
        return accessInBoundsDoubleVector(vector, position);
    }

    @Specialization(order = 301, guards = "isNA")
    public double accessNA(@SuppressWarnings("unused") RDoubleVector vector, @SuppressWarnings("unused") int position) {
        return RRuntime.DOUBLE_NA;
    }

    @Specialization(order = 302, guards = "isMinusLength")
    public RDoubleVector accessMinusLength(RDoubleVector vector, @SuppressWarnings("unused") int position) {
        return vector.removeLast();
    }

    @Specialization(order = 303, guards = "isMinusOne")
    public RDoubleVector accessMinusOne(RDoubleVector vector, @SuppressWarnings("unused") int position) {
        return vector.removeFirst();
    }

    @Specialization(order = 304, guards = "isMinusInBounds")
    public RDoubleVector accessMinusInBounds(RDoubleVector vector, int position) {
        return accessMinusInBoundsDoubleVector(vector, position);
    }

    @Specialization(order = 306, guards = "isBelowMinusLengthOrZero")
    public RDoubleVector accessBelowMinusLength(RDoubleVector vector, int position) {
        if (position == RRuntime.INT_NA) {
            return RDataFactory.createDoubleVector(new double[]{RRuntime.DOUBLE_NA}, RDataFactory.INCOMPLETE_VECTOR);
        } else if (position == 0) {
            return RDataFactory.createEmptyDoubleVector();
        }
        return (RDoubleVector) vector.makeShared();
    }

    @Specialization(order = 308, guards = "isAboveLength")
    public double accessAboveLength(@SuppressWarnings("unused") RDoubleVector vector, @SuppressWarnings("unused") int position) {
        return RRuntime.DOUBLE_NA;
    }

    @Specialization(order = 311, guards = "positiveSequenceInBounds")
    public RDoubleVector accessDoubleSequence(RDoubleVector vector, RIntSequence positions) {
        assert positions.getStart() > 0 && positions.getEnd() > 0;
        double[] result = new double[positions.getLength()];
        for (int i = 0; i < result.length; ++i) {
            int index = positions.getDataAt(i) - 1;
            result[i] = vector.getDataAt(index);
        }
        return RDataFactory.createDoubleVector(result, vector.isComplete());
    }

    @Specialization(order = 312, guards = "!positiveSequenceInBounds")
    public RDoubleVector accessDoubleSequenceOutOfBounds(RIntVector vector, RIntSequence positions) {
        assert positions.getStart() > 0 && positions.getEnd() > 0;
        double[] result = new double[positions.getLength()];
        int i = 0;
        for (; i < result.length; ++i) {
            int index = positions.getDataAt(i) - 1;
            if (index >= vector.getLength()) {
                break;
            }
            result[i] = vector.getDataAt(index);
        }
        for (; i < result.length; ++i) {
            result[i] = RRuntime.DOUBLE_NA;
        }
        return RDataFactory.createDoubleVector(result, vector.isComplete());
    }

    @Specialization(order = 313, guards = "isTrue")
    public RDoubleVector accessTrue(RDoubleVector vector, @SuppressWarnings("unused") byte position) {
        vector.makeShared();
        return vector;
    }

    @SuppressWarnings("unused")
    @Specialization(order = 314, guards = "isFalse")
    public RDoubleVector accessFalse(RDoubleVector vector, byte position) {
        return RDataFactory.createEmptyDoubleVector();
    }

    @Specialization(order = 315, guards = "isNA")
    public RDoubleVector accessLogicalNA(RDoubleVector vector, @SuppressWarnings("unused") byte position) {
        double[] data = new double[vector.getLength()];
        Arrays.fill(data, RRuntime.DOUBLE_NA);
        return RDataFactory.createDoubleVector(data, RDataFactory.INCOMPLETE_VECTOR);
    }

    @Specialization(order = 320)
    public RDoubleVector accessLogicalVector(RDoubleVector vector, RLogicalVector position) {
        int resultLength = 0;
        int posIndex = 0;
        for (int vecIndex = 0; vecIndex < vector.getLength(); vecIndex++) {
            byte posValue = position.getDataAt(posIndex);
            if (posValue != RRuntime.LOGICAL_FALSE) {
                resultLength++;
            }
            posIndex = Utils.incMod(posIndex, position.getLength());
        }

        positionNACheck.enable(position);
        resultNACheck.enable(vector);
        double[] result = new double[resultLength];
        int resultIndex = 0;
        posIndex = 0;
        for (int vecIndex = 0; vecIndex < vector.getLength(); vecIndex++) {
            byte posValue = position.getDataAt(posIndex);
            if (posValue != RRuntime.LOGICAL_FALSE) {
                double value;
                if (!positionNACheck.check(posValue)) {
                    value = vector.getDataAt(vecIndex);
                    resultNACheck.check(value);
                } else {
                    value = RRuntime.DOUBLE_NA;
                }
                result[resultIndex++] = value;
            }
            posIndex = Utils.incMod(posIndex, position.getLength());
        }

        return RDataFactory.createDoubleVector(result, positionNACheck.neverSeenNA() && resultNACheck.neverSeenNA());
    }

    @Specialization(order = 350)
    public RDoubleVector accessDoubleVectorIntVector(RDoubleVector vector, RIntVector positions) {
        return accessAbstractDoubleVectorIntVector(vector, positions);
    }

    private RDoubleVector accessAbstractDoubleVectorIntVector(RAbstractDoubleVector vector, RIntVector positions) {
        int positiveCount = 0;
        int negativeCount = 0;
        int zeroCount = 0;
        positionNACheck.enable(positions);
        int positionLength = positions.getLength();
        for (int i = 0; i < positionLength; ++i) {
            int pos = positions.getDataAt(i);
            if (pos > 0 || positionNACheck.check(pos)) {
                if (!hasSeenPositive) {
                    CompilerDirectives.transferToInterpreter();
                    hasSeenPositive = true;
                }
                positiveCount++;
            } else if (pos == 0) {
                if (!hasSeenZero) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    hasSeenZero = true;
                }
                zeroCount++;
            } else {
                if (!hasSeenNegative) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    hasSeenNegative = true;
                }
                negativeCount++;
            }
        }

        if (hasSeenNegative && negativeCount > 0) {
            if (hasSeenPositive && positiveCount > 0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
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

            double[] result = new double[selectionCount];
            int resultIndex = 0;
            for (int i = 0; i < selectVector.length; ++i) {
                if (selectVector[i]) {
                    result[resultIndex++] = vector.getDataAt(i);
                }
            }
            return RDataFactory.createDoubleVector(result, vector.isComplete());
        } else {
            if (!hasSeenZeroNegatives) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                hasSeenZeroNegatives = true;
            }
            double[] result = new double[positionLength - zeroCount];
            int resultPosition = 0;
            for (int i = 0; i < positionLength; ++i) {
                int pos = positions.getDataAt(i);
                if (hasSeenZero && pos == 0) {
                    continue;
                }

                double curResult = 0;
                if (pos > vector.getLength() || positionNACheck.check(pos)) {
                    if (!hasSeenOutOfBounds) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        hasSeenOutOfBounds = true;
                    }
                    curResult = RRuntime.DOUBLE_NA;
                } else {
                    curResult = vector.getDataAt(pos - 1);
                }

                int index = i;
                if (hasSeenZero) {
                    index = resultPosition;
                }

                result[index] = curResult;

                if (hasSeenZero) {
                    resultPosition++;
                }
            }
            return RDataFactory.createDoubleVector(result, vector.isComplete());
        }
    }

    // complex vector

    @Specialization(order = 406, guards = "isBelowMinusLengthOrZero")
    public RComplexVector accessBelowMinusLength(RComplexVector vector, int position) {
        if (position == RRuntime.INT_NA) {
            RComplex complexNA = RRuntime.createComplexNA();
            return RDataFactory.createComplexVector(new double[]{complexNA.getRealPart(), complexNA.getImaginaryPart()}, RDataFactory.INCOMPLETE_VECTOR);
        } else if (position == 0) {
            return RDataFactory.createEmptyComplexVector();
        }
        return (RComplexVector) vector.makeShared();
    }

    // string vector

    @Specialization(order = 600, guards = "isInBounds")
    public String accessIntVectorIntScalar(RStringVector vector, int position) {
        return accessInBoundsStringVector(vector, position);
    }

    @Specialization(order = 601, guards = "isNA")
    public String accessNA(@SuppressWarnings("unused") RStringVector vector, @SuppressWarnings("unused") int position) {
        return RRuntime.STRING_NA;
    }

    @Specialization(order = 608, guards = "isAboveLength")
    public String accessAboveLength(@SuppressWarnings("unused") RStringVector vector, @SuppressWarnings("unused") int position) {
        return RRuntime.STRING_NA;
    }

    @Specialization(order = 610, guards = "positiveSequenceInBounds")
    public RStringVector accessStringSequence(RStringVector vector, RIntSequence positions) {
        assert positions.getStart() > 0 && positions.getEnd() > 0;
        return accessPositiveInBounds(vector, positions);
    }

    @Specialization(order = 611, guards = "nullNames")
    @SuppressWarnings("unused")
    public RStringVector accessStringStringNullNames(RStringVector vector, String index) {
        return RDataFactory.createStringVector(RRuntime.STRING_NA);
    }

    @Specialization(order = 612, guards = "!nullNames")
    public RStringVector accessStringString(RStringVector vector, String index) {
        if (vector.getNames() == null) {
            return RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, false);
        } else if (index.length() == 0 || index == RRuntime.STRING_NA) {
            return createStringNA();
        } else {
            int found = -1;
            RStringVector vnames = (RStringVector) vector.getNames();
            for (int i = 0; i < vnames.getLength(); ++i) {
                String cur = vnames.getDataAt(i);
                if (cur.length() != 0 && cur != RRuntime.STRING_NA && index.equals(cur)) {
                    found = i;
                    break;
                }
            }

            if (found == -1) {
                return createStringNA();
            } else {
                RStringVector result = RDataFactory.createStringVector(new String[]{vector.getDataAt(found)}, vector.isComplete());
                result.setNames(RDataFactory.createStringVector(new String[]{vnames.getDataAt(found)}, true));
                return result;
            }
        }
    }

    private static RStringVector createStringNA() {
        RStringVector result = RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, false);
        result.setNames(RDataFactory.createStringVector(new String[]{RRuntime.STRING_NA}, false));
        return result;
    }

    @Specialization(order = 613)
    public RStringVector accessStringVectorIntVector(RStringVector vector, RIntVector positions) {
        return accessAbstractStringVectorIntVector(vector, positions);
    }

    private RStringVector accessAbstractStringVectorIntVector(RAbstractStringVector vector, RIntVector positions) {
        int positiveCount = 0;
        int negativeCount = 0;
        int zeroCount = 0;
        positionNACheck.enable(positions);
        int positionLength = positions.getLength();
        for (int i = 0; i < positionLength; ++i) {
            int pos = positions.getDataAt(i);
            if (pos > 0 || positionNACheck.check(pos)) {
                if (!hasSeenPositive) {
                    CompilerDirectives.transferToInterpreter();
                    hasSeenPositive = true;
                }
                positiveCount++;
            } else if (pos == 0) {
                if (!hasSeenZero) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    hasSeenZero = true;
                }
                zeroCount++;
            } else {
                if (!hasSeenNegative) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    hasSeenNegative = true;
                }
                negativeCount++;
            }
        }

        if (hasSeenNegative && negativeCount > 0) {
            if (hasSeenPositive && positiveCount > 0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
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

            String[] result = new String[selectionCount];
            int resultIndex = 0;
            for (int i = 0; i < selectVector.length; ++i) {
                if (selectVector[i]) {
                    result[resultIndex++] = vector.getDataAt(i);
                }
            }
            return RDataFactory.createStringVector(result, vector.isComplete());
        } else {
            if (!hasSeenZeroNegatives) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                hasSeenZeroNegatives = true;
            }
            String[] result = new String[positionLength - zeroCount];
            int resultPosition = 0;
            for (int i = 0; i < positionLength; ++i) {
                int pos = positions.getDataAt(i);
                if (hasSeenZero && pos == 0) {
                    continue;
                }

                String curResult = null;
                if (pos > vector.getLength() || positionNACheck.check(pos)) {
                    if (!hasSeenOutOfBounds) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        hasSeenOutOfBounds = true;
                    }
                    curResult = RRuntime.STRING_NA;
                } else {
                    curResult = vector.getDataAt(pos - 1);
                }

                int index = i;
                if (hasSeenZero) {
                    index = resultPosition;
                }

                result[index] = curResult;

                if (hasSeenZero) {
                    resultPosition++;
                }
            }
            return RDataFactory.createStringVector(result, vector.isComplete());
        }
    }

    private static String accessInBoundsStringVector(RStringVector vector, int position) {
        return vector.getDataAt(position - 1);
    }

    private static RStringVector accessPositiveInBounds(RAbstractStringVector vector, RAbstractIntVector positions) {
        String[] result = new String[positions.getLength()];
        for (int i = 0; i < result.length; ++i) {
            int index = positions.getDataAt(i) - 1;
            result[i] = vector.getDataAt(index);
        }
        return RDataFactory.createStringVector(result, vector.isComplete());
    }

    @Specialization(order = 649, guards = "nullNames")
    @SuppressWarnings("unused")
    public RStringVector accessStringVectorStringVectorNullNames(RStringVector vector, RAbstractStringVector positions) {
        String[] result = new String[positions.getLength()];
        Arrays.fill(result, RRuntime.STRING_NA);
        return RDataFactory.createStringVector(result, RDataFactory.INCOMPLETE_VECTOR);
    }

    @Specialization(order = 650, guards = "!nullNames")
    public RStringVector accessStringVectorStringVector(RStringVector vector, RAbstractStringVector positions) {
        positionNACheck.enable(positions);
        resultNACheck.enable(true);
        int positionLength = positions.getLength();
        RStringVector resultNames = RDataFactory.createStringVector(positionLength);
        RStringVector resultVector = RDataFactory.createStringVector(positionLength);
        RStringVector vectorNames = (RStringVector) vector.getNames();
        for (int i = 0; i < positionLength; ++i) {
            String pos = positions.getDataAt(i);
            String value = RRuntime.STRING_NA;
            String name = RRuntime.STRING_NA;
            if (!positionNACheck.check(pos)) {
                for (int j = 0; j < vectorNames.getLength(); ++j) {
                    if (pos.equals(vectorNames.getDataAt(j))) {
                        value = vector.getDataAt(j);
                        name = pos;
                        break;
                    }
                }
            }
            resultVector.updateDataAt(i, value, resultNACheck);
            resultNames.updateDataAt(i, name, resultNACheck);
        }
        resultVector.setNames(resultNames);
        return resultVector;
    }

    // list access

    @Specialization(order = 700, guards = "isInBounds")
    public Object access(RList list, int position) {
        if (isSubset) {
            Object element = list.getDataAt(position - 1);
            return RDataFactory.createList(new Object[]{element});
        } else {
            return list.getDataAt(position - 1);
        }
    }

    @Specialization(order = 701, guards = "isMinusInBounds")
    public Object accessNegativeInBounds(RList list, int position) {
        if (isSubset) {
            final int pos = -position;
            Object[] data = list.getDataCopy();
            Object[] result = new Object[data.length - 1];
            System.arraycopy(data, 0, result, 0, pos - 1);
            System.arraycopy(data, pos, result, pos - 1, data.length - pos);
            return RDataFactory.createList(result);
        } else {
            throw RError.getSelectMoreThanOne(null);
        }
    }

    @Specialization(order = 702, guards = "isNA")
    @SuppressWarnings("unused")
    public Object accessBelowMinusLengthOrZeroOrNA(RList list, int position) {
        if (isSubset) {
            Object[] data = new Object[list.getLength()];
            Arrays.fill(data, RNull.instance);
            return RDataFactory.createList(data);
        } else {
            return RNull.instance;
        }
    }

    @Specialization(order = 703, guards = "isNA")
    public Object accessBelowMinusLengthOrZeroOrNA(RList list, byte position) {
        return accessBelowMinusLengthOrZeroOrNA(list, RRuntime.logical2int(position));
    }

    @Specialization(order = 704, guards = "isZero")
    @SuppressWarnings("unused")
    public Object accessZeroPosition(RList list, int position) {
        if (isSubset) {
            return RDataFactory.createList();
        } else {
            throw RError.getSelectLessThanOne(null);
        }
    }

    @Specialization(order = 705, guards = "isAboveLength")
    @SuppressWarnings("unused")
    public Object accessAboveLength(RList list, int position) {
        if (isSubset) {
            return RDataFactory.createList(new Object[]{RNull.instance});
        } else {
            throw RError.getSubscriptBounds(null);
        }
    }

    @Specialization(order = 706, guards = "isBelowMinusLength")
    @SuppressWarnings("unused")
    public Object accessNegativeOutOfBounds(RList list, int position) {
        if (isSubset) {
            return list;
        } else {
            throw RError.getSelectMoreThanOne(null);
        }
    }

    @Specialization(order = 707)
    public Object accessIntVector(@SuppressWarnings("unused") RList list, RIntVector position) {
        if (isSubset) {
            if (position.getLength() == 0) {
                return RDataFactory.createList();
            }
        }
        throw Utils.nyi();
    }

    // raw vector

    @Specialization(order = 800, guards = "isMinusInBounds")
    public RRawVector accessMinusInBounds(RRawVector vector, int position) {
        return accessMinusInBoundsRawVector(vector, position);
    }

    // guards

    public static boolean isTrue(@SuppressWarnings("unused") Object vector, byte position) {
        return position == RRuntime.LOGICAL_TRUE;
    }

    public static boolean isFalse(@SuppressWarnings("unused") Object vector, byte position) {
        return position == RRuntime.LOGICAL_FALSE;
    }

    public static boolean isNA(@SuppressWarnings("unused") Object vector, byte position) {
        return RRuntime.isNA(position);
    }

    public static boolean isNA(@SuppressWarnings("unused") Object vector, int position) {
        return RRuntime.isNA(position);
    }

    public static boolean isInBounds(RAbstractVector vector, int position) {
        return position >= 1 && position <= vector.getLength();
    }

    public static boolean isInBounds(RList list, int position) {
        return position >= 1 && position <= list.getLength();
    }

    public static boolean isInBounds(RList list, byte position) {
        return position >= 1 && position <= list.getLength();
    }

    public static boolean isMinusLength(RAbstractVector vector, int position) {
        return position == -vector.getLength();
    }

    public static boolean isMinusInBounds(RAbstractVector vector, int position) {
        return isInBounds(vector, -position);
    }

    public static boolean isBelowMinusLengthOrZero(RAbstractVector vector, int position) {
        return position < -vector.getLength() || position == 0;
    }

    public static boolean isBelowMinusLengthOrZero(RAbstractVector vector, byte position) {
        return position < -vector.getLength() || position == 0;
    }

    @SuppressWarnings("unused")
    public static boolean isNA(RList list, int position) {
        return RRuntime.isNA(position);
    }

    @SuppressWarnings("unused")
    public static boolean isNA(RList list, byte position) {
        return RRuntime.isNA(position);
    }

    public static boolean isBelowMinusLength(RList list, int position) {
        return position < -list.getLength();
    }

    @SuppressWarnings("unused")
    public static boolean isZero(RList list, int position) {
        return position == 0;
    }

    public static boolean isAboveLength(RAbstractVector vector, int position) {
        return position > vector.getLength();
    }

    @SuppressWarnings("unused")
    public static boolean isMinusOne(Object vector, int position) {
        return position == -1;
    }

    private static double accessInBoundsDoubleVector(RDoubleVector vector, int position) {
        return vector.getDataAt(position - 1);
    }

    private static int accessInBoundsIntVector(RAbstractIntVector vector, int position) {
        return vector.getDataAt(position - 1);
    }

    @com.oracle.truffle.api.CompilerDirectives.SlowPath
    private static RIntVector accessMinusInBoundsIntVector(RAbstractIntVector vector, int position) {
        assert vector.getLength() > 0;
        assert -position >= 1 && -position <= vector.getLength();
        int newLength = vector.getLength() - 1;
        int[] newData = new int[newLength];
        int j = 0;
        for (int i = 0; i < newLength; ++i) {
            if (j == -position - 1) {
                ++j;
            }
            newData[i] = vector.getDataAt(j);
            ++j;
        }
        return RDataFactory.createIntVector(newData, vector.isComplete());
    }

    @com.oracle.truffle.api.CompilerDirectives.SlowPath
    private static RDoubleVector accessMinusInBoundsDoubleVector(RDoubleVector vector, int position) {
        assert vector.getLength() > 0;
        assert -position >= 1 && -position <= vector.getLength();
        int newLength = vector.getLength() - 1;
        double[] newData = new double[newLength];
        int j = 0;
        for (int i = 0; i < newLength; ++i) {
            if (j == -position - 1) {
                ++j;
            }
            newData[i] = vector.getDataAt(j);
            ++j;
        }
        return RDataFactory.createDoubleVector(newData, vector.isComplete());
    }

    @com.oracle.truffle.api.CompilerDirectives.SlowPath
    private static RRawVector accessMinusInBoundsRawVector(RRawVector vector, int position) {
        assert vector.getLength() > 0;
        assert -position >= 1 && -position <= vector.getLength();
        int newLength = vector.getLength() - 1;
        byte[] newData = new byte[newLength];
        int j = 0;
        for (int i = 0; i < newLength; ++i) {
            if (j == -position - 1) {
                ++j;
            }
            newData[i] = vector.getDataAt(j).getValue();
            ++j;
        }
        return RDataFactory.createRawVector(newData);
    }

    public static AccessVectorNode create(RNode vector, RNode position) {
        return AccessVectorNodeFactory.create(vector, position);
    }

    @NodeChild("operand")
    public abstract static class AccessVectorVectorCast extends RNode {

        @Specialization
        public RNull doNull(RNull operand) {
            return operand;
        }

        @Specialization
        public RLogicalVector doBoolean(byte operand) {
            return RDataFactory.createLogicalVectorFromScalar(operand);
        }

        @Specialization
        public RIntVector doInt(int operand) {
            return RDataFactory.createIntVectorFromScalar(operand);
        }

        @Specialization
        public RDoubleVector doDouble(double operand) {
            return RDataFactory.createDoubleVectorFromScalar(operand);
        }

        @Specialization
        public RStringVector doString(String operand) {
            return RDataFactory.createStringVectorFromScalar(operand);
        }

        @Specialization
        public RComplexVector doComplex(RComplex operand) {
            return RDataFactory.createComplexVectorFromScalar(operand);
        }

        @Specialization
        public RDoubleVector doDoubleSequence(RDoubleSequence operand) {
            return (RDoubleVector) operand.createVector();
        }

        @Specialization
        public RIntSequence doIntVector(RIntSequence operand) {
            return operand;
        }

        @Specialization
        public RIntVector doIntVector(RIntVector operand) {
            return operand;
        }

        @Specialization
        public RDoubleVector doDoubleVector(RDoubleVector operand) {
            return operand;
        }

        @Specialization
        public RComplexVector doComplexVector(RComplexVector operand) {
            return operand;
        }

        @Specialization
        public RLogicalVector doLogicalVector(RLogicalVector operand) {
            return operand;
        }

        @Specialization
        public RStringVector doStringVector(RStringVector operand) {
            return operand;
        }

        @Specialization
        public RRawVector doRawVector(RRawVector operand) {
            return operand;
        }

        @Specialization
        public RList doList(RList operand) {
            return operand;
        }
    }
}
