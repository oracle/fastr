package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestBuiltin_unserialize extends TestBase {
    @Test
    public void testserializeAndUnserializeDataFrame() {
        test("data.frame(col1=c(9,8,7), col2=1:3)");
    }

    @Test
    public void testserializeAndUnserializeVector() {
        test("c(1, 2, 3, 4)");
    }

    @Test
    public void testserializeAndUnserializeScalars() {
        test("3L");
        test("42");
        test("\"Hello world\"");
        test("3+2i");
        test("TRUE");
    }

    @Test
    public void testserializeAndUnserializeMtcars() {
        test("head(mtcars)");
    }

    @Test
    public void testserializeAndUnserializeClosure() {
        // N.B.: FastR does not preserve code formatting like GNU R does
        assertEval(Ignored.OutputFormatting, "unserialize(serialize(function (x) { x }, NULL))");
    }

    /**
     * Runs serialize and unserialize with given expression.
     */
    private void test(String expr) {
        assertEval("unserialize(serialize(" + expr + ", NULL))");
    }
}
