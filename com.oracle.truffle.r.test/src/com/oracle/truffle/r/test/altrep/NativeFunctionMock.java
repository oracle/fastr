package com.oracle.truffle.r.test.altrep;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RIntVector;
import org.junit.Assert;

import java.util.Arrays;

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
