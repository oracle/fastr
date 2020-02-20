package com.oracle.truffle.r.test.runtime;

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
}

// TODO: Add DataptrFunctionMock that will check arguments, and so on ...
@ExportLibrary(InteropLibrary.class)
abstract class NativeFunctionMock implements TruffleObject {
    private boolean called;

    @ExportMessage
    public boolean isExecutable() {
        return true;
    }

    @ExportMessage
    Object execute(Object[] args) {
        Assert.assertTrue(args.length >= 1);
        assertInstanceParameter(args[0]);
        RIntVector instance = unwrapInstanceParameter(args[0]);
        setCalled();
        Object[] argsWithoutFirstArg = Arrays.copyOfRange(args, 1, args.length);
        return doExecute(instance, argsWithoutFirstArg);
    }

    private void assertInstanceParameter(Object instanceParam) {
        Assert.assertTrue(instanceParam instanceof NativeDataAccess.NativeMirror);
        RBaseObject mirroredObject = ((NativeDataAccess.NativeMirror) instanceParam).getDelegate();
        Assert.assertTrue(mirroredObject instanceof RIntVector);
    }

    private RIntVector unwrapInstanceParameter(Object instanceParam) {
        RBaseObject delegate = ((NativeDataAccess.NativeMirror) instanceParam).getDelegate();
        assert delegate instanceof RIntVector;
        return (RIntVector) delegate;
    }

    protected abstract Object doExecute(RIntVector instance, Object... args);

    public boolean wasCalled() {
        return called;
    }

    private void setCalled() {
        called = true;
    }
}

abstract class DataptrFunctionMock extends NativeFunctionMock {
    @Override
    protected Object doExecute(RIntVector instance, Object... args) {
        Assert.assertEquals(1, args.length);
        Assert.assertTrue(args[0] instanceof Boolean);
        return dataptr(instance, (boolean) args[0]);
    }

    protected abstract Object dataptr(RIntVector instance, boolean writeabble);
}

abstract class EltFunctionMock extends NativeFunctionMock {
    @Override
    protected Object doExecute(RIntVector instance, Object... args) {
        Assert.assertEquals(1, args.length);
        Assert.assertTrue(args[0] instanceof Integer);
        return elt(instance, (int) args[0]);
    }

    protected abstract int elt(RIntVector instance, int idx);
}

abstract class SumFunctionMock extends NativeFunctionMock {
    @Override
    protected Object doExecute(RIntVector instance, Object... args) {
        Assert.assertEquals(1, args.length);
        Assert.assertTrue(args[0] instanceof Boolean);
        return sum(instance, (boolean) args[0]);
    }

    protected abstract int sum(RIntVector instance, boolean naRm);
}

abstract class LengthFunctionMock extends NativeFunctionMock {
    @Override
    protected Object doExecute(RIntVector instance, Object... args) {
        Assert.assertEquals(0, args.length);
        return length(instance);
    }

    protected abstract int length(RIntVector instance);
}

class SimpleDescriptorWrapper {
    private Dataptr dataptrMethod = new Dataptr();
    private Length lengthMethod = new Length();
    private Elt eltMethod = new Elt();
    private Sum sumMethod = new Sum();
    private AltIntegerClassDescriptor descriptor = createDescriptor();
    private RIntVector altIntVector = RDataFactory.createAltIntVector(descriptor, new RAltRepData(RNull.instance, RNull.instance));
    private RAltIntVectorData altIntVectorData = (RAltIntVectorData) altIntVector.getData();
    private static final int LENGTH = 10;

    private AltIntegerClassDescriptor createDescriptor() {
        AltIntegerClassDescriptor descriptor = new AltIntegerClassDescriptor(SimpleDescriptorWrapper.class.getSimpleName(),
                "packageName", null);
        descriptor.registerDataptrMethod(dataptrMethod);
        descriptor.registerLengthMethod(lengthMethod);
        descriptor.registerEltMethod(eltMethod);
        descriptor.registerSumMethod(sumMethod);
        return descriptor;
    }

    public AltIntegerClassDescriptor getDescriptor() {
        return descriptor;
    }

    public Dataptr getDataptrMethod() {
        return dataptrMethod;
    }

    public Length getLengthMethod() {
        return lengthMethod;
    }

    public Elt getEltMethod() {
        return eltMethod;
    }

    public Sum getSumMethod() {
        return sumMethod;
    }

    public RAltIntVectorData getAltIntVectorData() {
        return altIntVectorData;
    }

    public RIntVector getAltIntVector() {
        return altIntVector;
    }

    public int getExpectedLength() {
        return LENGTH;
    }

    static final class Sum extends SumFunctionMock {
        @Override
        protected int sum(RIntVector instance, boolean naRm) {
            return 42;
        }
    }

    static final class Elt extends EltFunctionMock {
        @Override
        protected int elt(RIntVector instance, int idx) {
            return 1;
        }
    }

    static final class Dataptr extends DataptrFunctionMock {
        @Override
        protected Object dataptr(RIntVector instance, boolean writeabble) {
            return (long)0xdef00023;
        }
    }

    static final class Length extends LengthFunctionMock {
        @Override
        protected int length(RIntVector instance) {
            return LENGTH;
        }
    }
}

class IntVectorDescriptorWrapper {
    private final static int vecLength = 10;
    private final Dataptr dataptrMethod = new Dataptr();
    private final Length lengthMethod = new Length();
    private final Elt eltMethod = new Elt();
    private final AltIntegerClassDescriptor descriptor = createDescriptor();
    private final RIntVector altIntVector;

    IntVectorDescriptorWrapper() {
        RIntVector wrappedVector = createIntVector();
        RAltRepData altRepData = new RAltRepData(wrappedVector, RNull.instance);
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
        descriptor.registerDataptrMethod(dataptrMethod);
        descriptor.registerLengthMethod(lengthMethod);
        descriptor.registerEltMethod(eltMethod);
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

    static final class Dataptr extends DataptrFunctionMock {
        @Override
        protected Object dataptr(RIntVector instance, boolean writeabble) {
            return executeDataptr(instance);
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
