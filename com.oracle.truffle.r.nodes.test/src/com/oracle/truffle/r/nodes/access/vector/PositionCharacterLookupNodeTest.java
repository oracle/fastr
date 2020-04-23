/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.access.vector;

import com.oracle.truffle.r.nodes.test.TestBase;
import com.oracle.truffle.r.nodes.test.TestUtilities;
import com.oracle.truffle.r.nodes.test.TestUtilities.NodeHandle;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

public class PositionCharacterLookupNodeTest extends TestBase {
    private RIntVector matrix;
    private RIntVector vecWithNames;

    private NodeHandle<PositionCharacterLookupNode> createNodeHandle(int numPositions, int positionIndex) {
        AtomicReference<NodeHandle<PositionCharacterLookupNode>> handle = new AtomicReference<>();
        execInContext(() -> {
            handle.set(TestUtilities.createHandle(
                            new PositionCharacterLookupNode(ElementAccessMode.SUBSET, numPositions, positionIndex, false, true),
                            (node, args) -> {
                                RAbstractContainer target = (RAbstractContainer) args[0];
                                RStringVector position = (RStringVector) args[1];
                                int notFoundStartIndex = target.getLength();
                                return node.execute(target, position, notFoundStartIndex);
                            }));
            return null;
        });
        return handle.get();
    }

    private void initMatrix() {
        RStringVector firstDimName = RDataFactory.createStringVector(new String[]{"a", "b"}, true);
        RStringVector secondDimName = RDataFactory.createStringVector(new String[]{"A", "B", "C"}, true);
        RList dimNames = RDataFactory.createList(new Object[]{firstDimName, secondDimName});

        // matrix:
        // A B C
        // a 1 2 3
        // b 4 5 6
        int[] dims = {2, 3};
        matrix = RDataFactory.createIntVector(new int[]{1, 2, 3, 4, 5, 6}, true, dims);
        matrix.setDimNames(dimNames);
    }

    private void initVecWithNames() {
        RStringVector names = RDataFactory.createStringVector(new String[]{"a", "b", "c"}, true);
        vecWithNames = RDataFactory.createIntVector(new int[]{1, 2, 3}, true, names);
    }

    @Before
    public void init() {
        execInContext(() -> {
            initMatrix();
            initVecWithNames();
            return null;
        });
    }

    /**
     * Tests v["b"] case.
     */
    @Test
    public void vectorSubsetWithNameTest() {
        NodeHandle<PositionCharacterLookupNode> handle = createNodeHandle(1, 0);

        RStringVector index = RDataFactory.createStringVector("b");

        RIntVector positionVector = (RIntVector) handle.call(vecWithNames, index);

        Assert.assertEquals(1, positionVector.getLength());
        Assert.assertEquals(2, positionVector.getDataAt(0));
    }

    /**
     * Tests m["a", "B"] case.
     */
    @Test
    public void indexMatrixWithOneRowTest() {
        NodeHandle<PositionCharacterLookupNode> handleDimX = createNodeHandle(2, 0);
        NodeHandle<PositionCharacterLookupNode> handleDimY = createNodeHandle(2, 1);

        RStringVector dimXIndex = RDataFactory.createStringVectorFromScalar("a");
        RStringVector dimYIndex = RDataFactory.createStringVectorFromScalar("B");

        // Check X dimension character lookup.
        RIntVector dimXPositionVector = (RIntVector) handleDimX.call(matrix, dimXIndex);
        Assert.assertEquals(1, dimXPositionVector.getLength());
        Assert.assertEquals(1, dimXPositionVector.getDataAt(0));

        // Check Y dimension character lookup.
        RIntVector dimYPositionVector = (RIntVector) handleDimY.call(matrix, dimYIndex);
        Assert.assertEquals(1, dimYPositionVector.getLength());
        Assert.assertEquals(2, dimYPositionVector.getDataAt(0));
    }
}
