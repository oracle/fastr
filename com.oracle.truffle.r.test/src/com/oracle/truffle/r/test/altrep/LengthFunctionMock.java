package com.oracle.truffle.r.test.altrep;

import com.oracle.truffle.r.runtime.data.RIntVector;
import org.junit.Assert;

abstract class LengthFunctionMock extends NativeFunctionMock {
    @Override
    protected Object doExecute(RIntVector instance, Object... args) {
        Assert.assertEquals(0, args.length);
        return length(instance);
    }

    protected abstract int length(RIntVector instance);
}
