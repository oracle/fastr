package com.oracle.truffle.r.test.altrep;

import com.oracle.truffle.r.runtime.data.RIntVector;
import org.junit.Assert;

abstract class EltFunctionMock extends NativeFunctionMock {
    @Override
    protected Object doExecute(RIntVector instance, Object... args) {
        Assert.assertEquals(1, args.length);
        Assert.assertTrue(args[0] instanceof Integer);
        return elt(instance, (int) args[0]);
    }

    protected abstract int elt(RIntVector instance, int idx);
}
