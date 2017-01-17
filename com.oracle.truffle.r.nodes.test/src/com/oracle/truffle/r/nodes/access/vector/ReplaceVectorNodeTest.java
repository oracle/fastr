/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.access.vector;

import static com.oracle.truffle.r.nodes.test.TestUtilities.createHandle;
import static com.oracle.truffle.r.nodes.test.TestUtilities.generateInteger;
import static com.oracle.truffle.r.nodes.test.TestUtilities.generateVector;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNodeGen;
import com.oracle.truffle.r.nodes.test.TestBase;
import com.oracle.truffle.r.nodes.test.TestUtilities.NodeHandle;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RInteger;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RunWith(Theories.class)
public class ReplaceVectorNodeTest extends TestBase {
    @Test
    public void dummy() {
        // to make sure this file is recognized as a test
    }

    @DataPoints public static RType[] vectorTypes = RType.getVectorTypes();

    @DataPoint public static RAbstractVector accessEmpty = RDataFactory.createEmptyLogicalVector();
    @DataPoint public static RAbstractVector accessFirst = RDataFactory.createIntVector(new int[]{1}, true);
    @DataPoint public static RAbstractVector accessSecond = RDataFactory.createIntVector(new int[]{2}, true);
    @DataPoint public static RAbstractVector accessEverythingButFirst = RDataFactory.createIntVector(new int[]{-1}, true);
    @DataPoint public static RAbstractVector accessNA = RDataFactory.createIntVector(new int[]{RRuntime.INT_NA}, false);
    @DataPoint public static RAbstractVector accessZero = RDataFactory.createIntVector(new int[]{0}, false);
    @DataPoint public static RAbstractVector accessFirstTwo = RDataFactory.createIntVector(new int[]{1, 2}, true);

    @DataPoint public static RAbstractVector accessPositiveSequence = RDataFactory.createIntSequence(1, 1, 2);
    @DataPoint public static RAbstractVector accessPositiveSequenceStride2 = RDataFactory.createIntSequence(1, 2, 2);
    @DataPoint public static RAbstractVector accessNegativeSequence = RDataFactory.createIntSequence(-1, -1, 2);

    @DataPoints public static ElementAccessMode[] allModes = ElementAccessMode.values();

    @Test
    public void testSubsetMultiDimension() {
        RAbstractIntVector vector;

        // replace rectangle with rectangle indices
        vector = generateInteger(20, true);
        vector.setDimensions(new int[]{5, 4});
        executeReplace(ElementAccessMode.SUBSET, vector, RInteger.valueOf(-1),
                        RDataFactory.createIntVector(new int[]{2, 3, 4}, true), RDataFactory.createIntVector(new int[]{2, 3}, true));
        assertIndicies(vector, 0, 1, 2, 3, 4, 5, -1, -1, -1, 9, 10, -1, -1, -1, 14, 15, 16, 17, 18, 19);

        // replace box with box indices
        vector = generateInteger(9, true);
        vector.setDimensions(new int[]{3, 3});
        executeReplace(ElementAccessMode.SUBSET, vector, RInteger.valueOf(-1),
                        RDataFactory.createIntVector(new int[]{2, 3}, true), RDataFactory.createIntVector(new int[]{2, 3}, true));
        assertIndicies(vector, 0, 1, 2, 3, -1, -1, 6, -1, -1);

        // replace three dimensions
        vector = generateInteger(24, true);
        vector.setDimensions(new int[]{2, 3, 4});
        executeReplace(ElementAccessMode.SUBSET, vector, RInteger.valueOf(-1),
                        RDataFactory.createIntVector(new int[]{2}, true), RDataFactory.createIntVector(new int[]{2}, true), RDataFactory.createIntVector(new int[]{2}, true));
        assertIndicies(vector, 0, 1, 2, 3, 4, 5, 6, 7, 8, -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23);

        // replace three dimensions
        vector = generateInteger(24, true);
        vector.setDimensions(new int[]{2, 3, 4});
        executeReplace(ElementAccessMode.SUBSET, vector, RInteger.valueOf(-1),
                        RDataFactory.createIntVector(new int[]{2}, true), RDataFactory.createIntVector(new int[]{2, 3}, true), RDataFactory.createIntVector(new int[]{2, 3, 4}, true));
        assertIndicies(vector, 0, 1, 2, 3, 4, 5, 6, 7, 8, -1, 10, -1, 12, 13, 14, -1, 16, -1, 18, 19, 20, -1, 22, -1);
    }

    private static void assertIndicies(RAbstractIntVector vector, int... expectedValues) {
        int[] actual = new int[vector.getLength()];
        for (int i = 0; i < expectedValues.length; i++) {
            actual[i] = vector.getDataAt(i);

        }
        assertThat(actual, is(expectedValues));
    }

    @Test
    public void testSubsetSingleDimension() {
        RAbstractIntVector vector;

        // replace scalar with sequence stride=1
        vector = generateInteger(9, true);
        executeReplace(ElementAccessMode.SUBSET, vector, RInteger.valueOf(-1), new Object[]{RDataFactory.createIntSequence(5, 1, 3)});
        assertIndicies(vector, 0, 1, 2, 3, -1, -1, -1, 7, 8);

        // replace scalar with sequence stride>1
        vector = generateInteger(9, true);
        executeReplace(ElementAccessMode.SUBSET, vector, RInteger.valueOf(-1), new Object[]{RDataFactory.createIntSequence(5, 2, 2)});
        assertIndicies(vector, 0, 1, 2, 3, -1, 5, -1, 7, 8);

        // replace scalar with negative integer vector
        vector = generateInteger(4, true);
        executeReplace(ElementAccessMode.SUBSET, vector, RInteger.valueOf(-1), new Object[]{RDataFactory.createIntVector(new int[]{-2}, true)});
        assertIndicies(vector, -1, 1, -1, -1);

        // replace scalar with logical scalar
        vector = generateInteger(3, true);
        executeReplace(ElementAccessMode.SUBSET, vector, RInteger.valueOf(-1),
                        new Object[]{RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_TRUE}, true)});
        assertIndicies(vector, -1, -1, -1);

        // replace scalar with logical vector
        vector = generateInteger(4, true);
        executeReplace(ElementAccessMode.SUBSET, vector, RInteger.valueOf(-1),
                        new Object[]{RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE}, true)});
        assertIndicies(vector, -1, 1, -1, 3);

        // replace vector indexed by logical vector
        vector = generateInteger(4, true);
        executeReplace(ElementAccessMode.SUBSET, vector, RDataFactory.createIntVector(new int[]{-1, -2}, true),
                        new Object[]{RDataFactory.createLogicalVector(new byte[]{RRuntime.LOGICAL_TRUE, RRuntime.LOGICAL_FALSE}, true)});
        assertIndicies(vector, -1, 1, -2, 3);

        // replace scalar with integer vector
        vector = generateInteger(9, true);
        executeReplace(ElementAccessMode.SUBSET, vector, RInteger.valueOf(-1), new Object[]{RDataFactory.createIntVector(new int[]{9, 8}, true)});
        assertIndicies(vector, 0, 1, 2, 3, 4, 5, 6, -1, -1);

        // replace scalar with integer scalar
        vector = generateInteger(9, true);
        executeReplace(ElementAccessMode.SUBSET, vector, RInteger.valueOf(-1), new Object[]{RDataFactory.createIntVector(new int[]{9}, true)});
        assertIndicies(vector, 0, 1, 2, 3, 4, 5, 6, 7, -1);
    }

    @Theory
    public void testNames(RType targetType) {
        RAbstractVector vector = generateVector(targetType, 4, true);
        RStringVector names = (RStringVector) generateVector(RType.Character, 4, true);
        vector.setNames(names);

        RAbstractVector value = generateVector(targetType, 4, true);
        RStringVector valueNames = (RStringVector) generateVector(RType.Character, 4, true);
        value.setNames(valueNames);

        RAbstractVector result = executeReplace(ElementAccessMode.SUBSET, vector, value, RLogical.TRUE);

        RStringVector newNames = result.getNames();
        assertThat(newNames.getLength(), is(names.getLength()));
        assertThat(newNames.getDataAt(0), is(names.getDataAt(0)));
        assertThat(newNames.getDataAt(1), is(names.getDataAt(1)));
        assertThat(newNames.getDataAt(2), is(names.getDataAt(2)));
        assertThat(newNames.getDataAt(3), is(names.getDataAt(3)));
    }

    @Theory
    public void testCompletenessAfterReplace(RType targetType) {
        RAbstractVector vector = generateVector(targetType, 4, false);
        RAbstractVector replaceWith = generateVector(targetType, 1, true);

        assumeThat(vector.isComplete(), is(false));
        RAbstractVector result = executeReplace(ElementAccessMode.SUBSET, vector, replaceWith, RInteger.valueOf(1));
        assertThat(result.isComplete(), is(false));
    }

    @Theory
    public void testCompletenessAfterReplaceAll(RType targetType) {
        RAbstractVector vector = generateVector(targetType, 4, false);
        RAbstractVector replaceWith = generateVector(targetType, 1, true);

        assumeThat(vector.isComplete(), is(false));
        executeReplace(ElementAccessMode.SUBSET, vector, replaceWith, RLogical.valueOf(true));

        // TODO we would need to find out if we replace all elements. we should support this.
        // assertThat(result.isComplete(), is(true));
    }

    @Theory
    public void testCompletenessPositionNA(RType targetType) {
        RAbstractVector vector = generateVector(targetType, 4, true);
        RAbstractVector replaceWith = generateVector(targetType, 1, true);

        RAbstractVector result = executeReplace(ElementAccessMode.SUBSET, vector, replaceWith, RLogical.NA);

        assertThat(result.isComplete(), is(true));
    }

    @Theory
    public void testCompletenessOutOfBounds(RType targetType) {
        assumeTrue(targetType != RType.Raw);
        RAbstractVector vector = generateVector(targetType, 4, true);
        RAbstractVector replaceWith = generateVector(targetType, 1, true);

        RAbstractVector result = executeReplace(ElementAccessMode.SUBSET, vector, replaceWith, RInteger.valueOf(10));

        assertThat(result.isComplete(), is(false));
    }

    @Theory
    public void testCasts(RType targetType, RType valueType) {
        if (targetType != valueType) {
            assumeTrue(targetType != RType.Raw && valueType != RType.Raw);
        }
        RType resultType = RType.maxPrecedence(targetType, valueType);

        RAbstractVector vector = generateVector(targetType, 4, true);
        RAbstractVector value = generateVector(valueType, 4, true);

        RAbstractVector result = executeReplace(ElementAccessMode.SUBSET, vector, value, accessFirst);
        assertThat(result.getRType(), is(resultType));
    }

    @Theory
    public void testSubsetSingleDimensionTheory(RType targetType, RAbstractVector position) {
        assumeTrue(position.getLength() <= 4);
        assumeTrue(position.getLength() >= 1);
        assumeTrue(position.isComplete());

        RAbstractVector vector = generateVector(targetType, 4, true);
        RAbstractVector value = generateVector(targetType, 4, true);

        RAbstractVector result = executeReplace(ElementAccessMode.SUBSET, vector, value, position);
        assertThat(result, is(sameInstance(vector)));
    }

    @Theory
    public void testSubscriptSingleDimensionTheory(RType targetType, RAbstractVector position) {
        assumeTrue(position.getLength() == 1);
        if (position instanceof RAbstractIntVector) {
            assumeTrue(((RAbstractIntVector) position).getDataAt(0) > 0);
        }

        RAbstractVector vector = generateVector(targetType, 4, true);
        RAbstractVector value = generateVector(targetType, 1, true);

        executeReplace(ElementAccessMode.SUBSCRIPT, vector, value, position);

    }

    private NodeHandle<ReplaceVectorNode> handle;
    private NodeHandle<BoxPrimitiveNode> box;
    private ElementAccessMode currentMode;

    @Before
    public void setUp() {
        handle = null;
    }

    @After
    public void tearDown() {
        handle = null;
    }

    private RAbstractVector executeReplace(ElementAccessMode mode, Object vector, Object value, Object... positions) {
        if (handle == null || this.currentMode != mode) {
            handle = create(mode);
            this.currentMode = mode;
        }
        if (box == null) {
            box = createHandle(BoxPrimitiveNodeGen.create(), (node, args) -> node.execute(args[0]));
        }

        return (RAbstractVector) box.call(handle.call(vector, positions, value));
    }

    private static NodeHandle<ReplaceVectorNode> create(ElementAccessMode mode) {
        return createHandle(ReplaceVectorNode.create(mode, false),
                        (node, args) -> node.apply(null, args[0], (Object[]) args[1], args[2]));
    }
}
