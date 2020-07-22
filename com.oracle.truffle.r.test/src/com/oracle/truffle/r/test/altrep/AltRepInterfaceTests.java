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
package com.oracle.truffle.r.test.altrep;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.ffi.RObjectDataPtr;
import com.oracle.truffle.r.test.TestBase;
import org.junit.Assert;
import org.junit.Test;

public class AltRepInterfaceTests extends TestBase {
    @Test
    public void trivialLengthMethodTest() {
        SimpleDescriptorWrapper simpleDescriptorWrapper = new SimpleDescriptorWrapper();
        RIntVector altIntVec = simpleDescriptorWrapper.getAltIntVector();
        int actualLength = altIntVec.getLength();
        Assert.assertEquals(simpleDescriptorWrapper.getExpectedLength(), actualLength);
        Assert.assertTrue(simpleDescriptorWrapper.getLengthMethod().wasCalled());
    }

    @Test
    public void intVecWrapperLengthMethodTest() {
        IntVectorDescriptorWrapper intVectorDescriptorWrapper = new IntVectorDescriptorWrapper();
        RIntVector altIntVec = intVectorDescriptorWrapper.getAltIntVector();
        int actualLength = altIntVec.getLength();
        Assert.assertEquals(intVectorDescriptorWrapper.getExpectedLength(), actualLength);
        Assert.assertTrue(intVectorDescriptorWrapper.getLengthMethod().wasCalled());
    }

    /**
     * int *data = INTEGER(instance); data[0];
     *
     * This code snippet does not have to materialize instance, because it has Elt method
     * registered.
     */
    @Test
    public void readNativeIntData() throws Exception {
        SimpleDescriptorWrapper simpleDescriptorWrapper = new SimpleDescriptorWrapper();
        RIntVector altIntVec = simpleDescriptorWrapper.getAltIntVector();
        // RDataFactory.createAltIntVector(simpleDescriptorWrapper.getDescriptor(),
        // simpleDescriptorWrapper.getAltIntVectorData().getData());

        // int *data = INTEGER(instance);
        RObjectDataPtr objDataPtr = RObjectDataPtr.getUncached(altIntVec);
        // int elem = data[0];
        Object elem = InteropLibrary.getFactory().getUncached(objDataPtr).readArrayElement(objDataPtr, 0);

        // altIntVec does not have to be materialized, because it has Elt method registered,
        // therefore code
        // snippet "int elem = data[0]" downcalls into Elt method rather than into Dataptr method.
        Assert.assertFalse(altIntVec.isMaterialized());
        Assert.assertTrue(elem instanceof Integer);
        Assert.assertEquals(simpleDescriptorWrapper.getAltIntVector().getDataAt(0), elem);
    }

    /**
     * int *data = INTEGER(instance); data[0] = 42;.
     */
    @Test
    public void writeNativeIntData() throws Exception {
        SimpleDescriptorWrapper simpleDescriptorWrapper = new SimpleDescriptorWrapper();
        RIntVector altIntVec = simpleDescriptorWrapper.getAltIntVector();

        // int *data = INTEGER(instance);
        RObjectDataPtr objDataptr = RObjectDataPtr.getUncached(altIntVec);
        // data[0] = 42;
        InteropLibrary.getFactory().getUncached(objDataptr).writeArrayElement(objDataptr, 0, 42);

        // data[0] = 42 calls into Dataptr method, so the whole vector has to be materialized.
        Assert.assertTrue(altIntVec.isMaterialized());
        Assert.assertEquals(42, altIntVec.getDataAt(0));
    }
}
