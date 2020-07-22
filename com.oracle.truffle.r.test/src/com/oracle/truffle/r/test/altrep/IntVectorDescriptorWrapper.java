package com.oracle.truffle.r.test.altrep;

import com.oracle.truffle.r.runtime.data.RAltIntVectorData;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.altrep.AltIntegerClassDescriptor;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import org.junit.Assert;

public class IntVectorDescriptorWrapper {
    private static final int vecLength = 10;
    private final DataptrMethod dataptrMethod;
    private final Length lengthMethod;
    private final Elt eltMethod;
    private final AltIntegerClassDescriptor descriptor;
    private final RIntVector altIntVector;

    public IntVectorDescriptorWrapper() {
        RIntVector wrappedVector = createIntVector();
        RAltRepData altRepData = new RAltRepData(wrappedVector, RNull.instance);
        dataptrMethod = new DataptrMethod();
        lengthMethod = new Length();
        eltMethod = new Elt();
        descriptor = createDescriptor();
        altIntVector = RDataFactory.createAltIntVector(descriptor, altRepData);
    }

    private static RIntVector createIntVector() {
        RIntVector vector = RDataFactory.createIntVector(vecLength);
        for (int i = 0; i < vecLength; i++) {
            vector.setElement(i, i);
        }
        return vector;
    }

    private AltIntegerClassDescriptor createDescriptor() {
        AltIntegerClassDescriptor descriptor = new AltIntegerClassDescriptor(IntVectorDescriptorWrapper.class.getSimpleName(),
                        "packageName", null);
        // descriptor.registerDataptrMethod(dataptrMethod);
        // descriptor.registerLengthMethod(lengthMethod);
        // descriptor.registerEltMethod(eltMethod);
        return descriptor;
    }

    public int getExpectedLength() {
        return vecLength;
    }

    public Length getLengthMethod() {
        return lengthMethod;
    }

    public AltIntegerClassDescriptor getDescriptor() {
        return descriptor;
    }

    public RIntVector getAltIntVector() {
        return altIntVector;
    }

    private static void checkInstanceData(RIntVector instance) {
        Assert.assertTrue(instance.getData() instanceof RAltIntVectorData);
        RAltIntVectorData altIntVectorData = (RAltIntVectorData) instance.getData();
        Assert.assertTrue(altIntVectorData.getData1() instanceof RIntVector);
        Assert.assertEquals(vecLength, ((RIntVector) altIntVectorData.getData1()).getLength());
    }

    private static RIntVector getInstanceData(RIntVector instance) {
        checkInstanceData(instance);
        return (RIntVector) ((RAltIntVectorData) instance.getData()).getData1();
    }

    private static long executeDataptr(RIntVector instance) {
        return getInstanceData(instance).allocateNativeContents();
    }

    private static int executeLength(RIntVector instance) {
        return getInstanceData(instance).getLength();
    }

    private static int executeElt(RIntVector instance, int idx) {
        return getInstanceData(instance).getDataAt(idx);
    }

    static final class DataptrMethod extends DataptrFunctionMock {
        @Override
        protected Dataptr dataptr(RIntVector instance, boolean writeabble) {
            return new Dataptr(executeDataptr(instance));
        }
    }

    static final class Length extends LengthFunctionMock {
        @Override
        protected int length(RIntVector instance) {
            return executeLength(instance);
        }
    }

    static final class Elt extends EltFunctionMock {
        @Override
        protected int elt(RIntVector instance, int idx) {
            return executeElt(instance, idx);
        }
    }
}
