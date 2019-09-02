/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.truffle.r.nodes.access.vector;

import static com.oracle.truffle.r.nodes.test.TestUtilities.generateInteger;
import static com.oracle.truffle.r.nodes.test.TestUtilities.generateComplex;
import static org.hamcrest.MatcherAssert.assertThat;

import com.oracle.truffle.r.nodes.test.TestBase;
import static com.oracle.truffle.r.nodes.test.TestUtilities.generateDouble;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorDataReuse;
import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class VectorManipulationTest extends TestBase {

    @Test
    public void dummy() {
        // to make sure this file is recognized as a test
    }

    @Test
    public void testVectorDataReuse() {
        execInContext(() -> {
            // Int
            RIntVector intVec = generateInteger(2, true).materialize();
            VectorDataReuse.Int intDataReuseNode = VectorDataReuse.Int.create();
            assertThat("Not temporary", intVec.isTemporary());
            int[] intData = intDataReuseNode.execute(intVec.materialize());
            assertThat("Invalid data reuse array", intVec.getInternalManagedData() == intData);

            intVec.incRefCount();
            intData = intDataReuseNode.execute(intVec.materialize());
            assertThat("Invalid data reuse array", intVec.getInternalManagedData() != intData);
            assertDataContents(intVec, intData);

            NativeDataAccess.toNative(intVec);
            intVec.allocateNativeContents();
            intData = intDataReuseNode.execute(intVec.materialize());
            assertThat("Invalid data reuse array", intVec.getInternalManagedData() != intData);
            assertDataContents(intVec, intData);

            intVec = generateInteger(2, true).materialize();
            assertThat("Not temporary", intVec.isTemporary());
            NativeDataAccess.toNative(intVec);
            intVec.allocateNativeContents();
            assertThat("Not a native mirror", intVec.getInternalManagedData() == null);
            intData = intDataReuseNode.execute(intVec.materialize());
            assertThat("Invalid data reuse array", intVec.getInternalManagedData() != intData);
            assertDataContents(intVec, intData);

            // Double
            RDoubleVector doubleVector = generateDouble(2, true).materialize();
            VectorDataReuse.Double doubleDataReuseNode = VectorDataReuse.Double.create();
            assertThat("Not temporary", doubleVector.isTemporary());
            double[] doubleData = doubleDataReuseNode.execute(doubleVector.materialize());
            assertThat("Invalid data reuse array", doubleVector.getInternalManagedData() == doubleData);

            doubleVector.incRefCount();
            doubleData = doubleDataReuseNode.execute(doubleVector.materialize());
            assertThat("Invalid data reuse array", doubleVector.getInternalManagedData() != doubleData);
            assertDataContents(doubleVector, doubleData);

            NativeDataAccess.toNative(doubleVector);
            doubleVector.allocateNativeContents();
            doubleData = doubleDataReuseNode.execute(doubleVector.materialize());
            assertThat("Invalid data reuse array", doubleVector.getInternalManagedData() != doubleData);
            assertDataContents(doubleVector, doubleData);

            doubleVector = generateDouble(2, true).materialize();
            assertThat("Not temporary", doubleVector.isTemporary());
            NativeDataAccess.toNative(doubleVector);
            doubleVector.allocateNativeContents();
            assertThat("Not a native mirror", doubleVector.getInternalManagedData() == null);
            doubleData = doubleDataReuseNode.execute(doubleVector.materialize());
            assertThat("Invalid data reuse array", doubleVector.getInternalManagedData() != doubleData);
            assertDataContents(doubleVector, doubleData);

            // Complex
            RComplexVector complexVector = generateComplex(2, true).materialize();
            VectorDataReuse.Complex complexDataReuseNode = VectorDataReuse.Complex.create();
            assertThat("Not temporary", complexVector.isTemporary());
            double[] complexData = complexDataReuseNode.execute(complexVector.materialize());
            assertThat("Invalid data reuse array", complexVector.getInternalManagedData() == complexData);

            complexVector.incRefCount();
            complexData = complexDataReuseNode.execute(complexVector.materialize());
            assertThat("Invalid data reuse array", complexVector.getInternalManagedData() != complexData);
            assertDataContents(complexVector, complexData);

            NativeDataAccess.toNative(complexVector);
            complexVector.allocateNativeContents();
            complexData = complexDataReuseNode.execute(complexVector.materialize());
            assertThat("Invalid data reuse array", complexVector.getInternalManagedData() != complexData);
            assertDataContents(complexVector, complexData);

            complexVector = generateComplex(2, true).materialize();
            assertThat("Not temporary", complexVector.isTemporary());
            NativeDataAccess.toNative(complexVector);
            complexVector.allocateNativeContents();
            assertThat("Not a native mirror", complexVector.getInternalManagedData() == null);
            complexData = complexDataReuseNode.execute(complexVector.materialize());
            assertThat("Invalid data reuse array", complexVector.getInternalManagedData() != complexData);
            assertDataContents(complexVector, complexData);

            return null;
        });
    }

    private static <ArrayT> void assertDataContents(RAbstractVector vec, ArrayT arr) {
        int len = vec.getLength();
        RType type = vec.getRType();
        for (int i = 0; i < len; i++) {
            Object expected = vec.getDataAtAsObject(i);
            Object tested;
            switch (type) {
                case Integer:
                    tested = ((int[]) arr)[i];
                    break;
                case Double:
                    tested = ((double[]) arr)[i];
                    break;
                case Complex:
                    tested = RComplex.valueOf(((double[]) arr)[2 * i], ((double[]) arr)[2 * i + 1]);
                    break;
                case List:
                    tested = ((Object[]) arr)[i];
                    break;
                default:
                    throw new AssertionError("Type check not implemented yet.");
            }
            assertThat("Values differ at index=" + i + ", expected=" + expected + ", tested=" + tested,
                            (expected == null && tested == null) || (expected != null && expected.equals(tested)));
        }
    }

}
