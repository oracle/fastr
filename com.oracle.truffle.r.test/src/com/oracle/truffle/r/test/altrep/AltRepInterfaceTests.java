package com.oracle.truffle.r.test.altrep;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.context.AltRepContext;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.RAltIntVectorData;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.runtime.ffi.VectorRFFIWrapper;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory;
import com.oracle.truffle.r.runtime.ffi.util.NativeMemory.ElementType;
import com.oracle.truffle.r.test.TestBase;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class AltRepInterfaceTests extends TestBase {
    private AltRepContext altRepContext = AltRepContext.newContextState();

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
     * int *data = INTEGER(instance);
     * data[0];
     *
     * This code snippet does not have to materialize instance, because it has Elt method registered.
     */
    @Test
    public void readNativeIntData() {
        SimpleDescriptorWrapper simpleDescriptorWrapper = new SimpleDescriptorWrapper();
        RIntVector altIntVec = simpleDescriptorWrapper.getAltIntVector();
                //RDataFactory.createAltIntVector(simpleDescriptorWrapper.getDescriptor(), simpleDescriptorWrapper.getAltIntVectorData().getData());

        // int *data = INTEGER(instance);
        VectorRFFIWrapper rffiWrapper = VectorRFFIWrapper.get(altIntVec);
        Object elem = null;
        try {
            // int elem = data[0];
            elem = InteropLibrary.getFactory().getUncached(rffiWrapper).readArrayElement(rffiWrapper, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // altIntVec does not have to be materialized, because it has Elt method registered, therefore code
        // snippet "int elem = data[0]" downcalls into Elt method rather than into Dataptr method.
        Assert.assertFalse(altIntVec.isMaterialized());
        Assert.assertTrue(elem instanceof Integer);
        Assert.assertEquals(simpleDescriptorWrapper.getAltIntVector().getDataAt(0), elem);
    }

    /**
     * int *data = INTEGER(instance);
     * data[0] = 42;
     */
    @Test
    public void writeNativeIntData() {
        SimpleDescriptorWrapper simpleDescriptorWrapper = new SimpleDescriptorWrapper();
        RIntVector altIntVec = simpleDescriptorWrapper.getAltIntVector();

        // int *data = INTEGER(instance);
        VectorRFFIWrapper rffiWrapper = VectorRFFIWrapper.get(altIntVec);
        try {
            // data[0] = 42;
            InteropLibrary.getFactory().getUncached(rffiWrapper).writeArrayElement(rffiWrapper, 0, 42);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // data[0] = 42 calls into Dataptr method, so the whole vector has to be materialized.
        Assert.assertTrue(altIntVec.isMaterialized());
        Assert.assertEquals(42, altIntVec.getDataAt(0));
    }
}

