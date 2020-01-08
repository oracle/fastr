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
import com.oracle.truffle.r.runtime.data.altrep.RAltIntegerVec;
import com.oracle.truffle.r.runtime.data.altrep.RAltRepData;
import com.oracle.truffle.r.test.TestBase;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class AltRepInterfaceTests extends TestBase {
    public static long VEC_LENGTH = 10;
    private AltRepContext altRepContext = AltRepContext.newContextState();

    @Test
    public void trivialLengthMethodTest() {
        SimpleDescriptorWrapper simpleDescriptorWrapper = new SimpleDescriptorWrapper();
        RAltIntegerVec altIntVec = simpleDescriptorWrapper.getInstance();
        int actualLength = altIntVec.getLength();
        Assert.assertEquals(simpleDescriptorWrapper.getExpectedLength(), actualLength);
        Assert.assertTrue(simpleDescriptorWrapper.getLengthMethod().wasCalled());
    }

    @Test
    public void intVecWrapperLengthMethodTest() {
        IntVectorDescriptorWrapper intVectorDescriptorWrapper = new IntVectorDescriptorWrapper();
        RAltIntegerVec altIntVec = intVectorDescriptorWrapper.getInstance();
        int actualLength = altIntVec.getLength();
        Assert.assertEquals(intVectorDescriptorWrapper.getExpectedLength(), actualLength);
        Assert.assertTrue(intVectorDescriptorWrapper.getLengthMethod().wasCalled());
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
        RAltIntegerVec instance = unwrapInstanceParameter(args[0]);
        setCalled();
        Object[] argsWithoutFirstArg = Arrays.copyOfRange(args, 1, args.length);
        return doExecute(instance, argsWithoutFirstArg);
    }

    private void assertInstanceParameter(Object instanceParam) {
        Assert.assertTrue(instanceParam instanceof NativeDataAccess.NativeMirror);
        RBaseObject mirroredObject = ((NativeDataAccess.NativeMirror) instanceParam).get();
        Assert.assertTrue(mirroredObject instanceof RAltIntegerVec);
    }

    private RAltIntegerVec unwrapInstanceParameter(Object instanceParam) {
        return (RAltIntegerVec) ((NativeDataAccess.NativeMirror) instanceParam).get();
    }

    protected abstract Object doExecute(RAltIntegerVec instance, Object... args);

    public boolean wasCalled() {
        return called;
    }

    private void setCalled() {
        called = true;
    }
}

class SimpleDescriptorWrapper {
    private Dataptr dataptrMethod = new Dataptr();
    private Length lengthMethod = new Length();
    private Elt eltMethod = new Elt();
    private Sum sumMethod = new Sum();
    private AltIntegerClassDescriptor descriptor = createDescriptor();
    private RAltIntegerVec instance = new RAltIntegerVec(descriptor, new RAltRepData(RNull.instance, RNull.instance), true);

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

    public RAltIntegerVec getInstance() {
        return instance;
    }

    public int getExpectedLength() {
        return 10;
    }

    static final class Sum extends NativeFunctionMock {
        @Override
        protected Object doExecute(RAltIntegerVec instance, Object... args) {
            Assert.assertEquals(1, args.length);
            return 42;
        }
    }

    static final class Elt extends NativeFunctionMock {
        @Override
        protected Object doExecute(RAltIntegerVec instance, Object... args) {
            Assert.assertEquals(1, args.length);
            return 1;
        }
    }

    static final class Dataptr extends NativeFunctionMock {
        @Override
        protected Object doExecute(RAltIntegerVec instance, Object... args) {
            return (long)0xdef00023;
        }
    }

    static final class Length extends NativeFunctionMock {
        @Override
        protected Object doExecute(RAltIntegerVec instance, Object... args) {
            return AltRepInterfaceTests.VEC_LENGTH;
        }
    }
}

class IntVectorDescriptorWrapper {
    private final static int vecLength = 10;
    private final Dataptr dataptrMethod = new Dataptr();
    private final Length lengthMethod = new Length();
    private final Elt eltMethod = new Elt();
    private final AltIntegerClassDescriptor descriptor = createDescriptor();
    private final RAltIntegerVec instance;

    IntVectorDescriptorWrapper() {
        RIntVector wrappedVector = createIntVector();
        RAltRepData data = new RAltRepData(wrappedVector, RNull.instance);
        instance = new RAltIntegerVec(descriptor, data, true);
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

    public RAltIntegerVec getInstance() {
        return instance;
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

    private static void checkInstanceData(RAltIntegerVec instance) {
        Assert.assertTrue(instance.getData1() instanceof RIntVector);
        Assert.assertEquals(vecLength, ((RIntVector) instance.getData1()).getLength());
    }

    private static RIntVector getInstanceData(RAltIntegerVec instance) {
        checkInstanceData(instance);
        return (RIntVector) instance.getData1();
    }

    private static long executeDataptr(RAltIntegerVec instance) {
        return getInstanceData(instance).allocateNativeContents();
    }

    private static long executeLength(RAltIntegerVec instance) {
        return getInstanceData(instance).getLength();
    }

    private static int executeElt(RAltIntegerVec instance, int idx) {
        return getInstanceData(instance).getDataAt(idx);
    }

    static final class Dataptr extends NativeFunctionMock {
        @Override
        protected Object doExecute(RAltIntegerVec instance, Object... args) {
            Assert.assertEquals(1, args.length);
            return executeDataptr(instance);
        }
    }

    static final class Length extends NativeFunctionMock {
        @Override
        protected Object doExecute(RAltIntegerVec instance, Object... args) {
            Assert.assertEquals(0, args.length);
            return executeLength(instance);
        }
    }

    static final class Elt extends NativeFunctionMock {
        @Override
        protected Object doExecute(RAltIntegerVec instance, Object... args) {
            Assert.assertEquals(1, args.length);
            Assert.assertTrue(args[0] instanceof Integer);
            return executeElt(instance, (int) args[0]);
        }
    }
}
