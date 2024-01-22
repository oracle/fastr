package com.oracle.truffle.r.test.integration;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

public class EmbeddingSmokeTest {
    @Test
    public void basicUsage() {
        try (Context context = Context.newBuilder().allowAllAccess(true).build()) {
            Value result = context.eval("R", "c(1, 2) + c(3, 4)");
            Assert.assertTrue(result.hasArrayElements());
            assertEquals(4, result.getArrayElement(0));
            assertEquals(6, result.getArrayElement(1));
        }
    }

    public static void assertEquals(int expected, Value actual) {
        Assert.assertTrue(actual + " is int", actual.fitsInInt());
        Assert.assertEquals(actual.toString(), expected, actual.asInt());
    }
}
