package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestRBase;

// Checkstyle: stop line length check
public class TestBuiltin_rawConnection extends TestRBase {

    @Override
    protected String getTestDir() {
        return "builtins/connection";
    }

    @Test
    public void testReadAppendText() {

        assertEval("{ rc <- rawConnection(raw(0), \"a+\"); close(rc); write(charToRaw(\"A\"), rc) }");
        assertEval("{ rv <- charToRaw(\"Hello\"); rc <- rawConnection(rv, \"a+\"); writeChar(\", World\", rc); res <- rawConnectionValue(rc); close(rc); res }");
        assertEval("{ rv <- charToRaw(\"Hello\"); rc <- rawConnection(rv, \"a+\"); writeChar(\", World\", rc); res <- rawToChar(rawConnectionValue(rc)); close(rc); res }");
        assertEval("{ rv <- charToRaw(\"Hello\"); rc <- rawConnection(rv, \"a+\"); write(charToRaw(\", World\"), rc); res <- rawConnectionValue(rc); close(rc); res }");
        assertEval("{ rv <- charToRaw(\"Hello\"); rc <- rawConnection(rv, \"a+\"); write(charToRaw(\", World\"), rc); res <- rawToChar(rawConnectionValue(rc)); close(rc); res }");
    }

    @Test
    public void testReadWriteText() {

        assertEval("{ rc <- rawConnection(raw(0), \"r+\"); close(rc); write(charToRaw(\"A\"), rc) }");
        assertEval("{ rv <- charToRaw(\"Hello\"); rc <- rawConnection(rv, \"r+\"); writeChar(\", World\", rc); res <- rawConnectionValue(rc); close(rc); res }");
        assertEval("{ rv <- charToRaw(\"Hello\"); rc <- rawConnection(rv, \"r+\"); writeChar(\", World\", rc); res <- rawToChar(rawConnectionValue(rc)); close(rc); res }");
        assertEval("{ rv <- charToRaw(\"Hello\"); rc <- rawConnection(rv, \"r+\"); write(charToRaw(\", World\"), rc); res <- rawConnectionValue(rc); close(rc); res }");
        assertEval("{ rv <- charToRaw(\"Hello\"); rc <- rawConnection(rv, \"r+\"); write(charToRaw(\", World\"), rc); res <- rawToChar(rawConnectionValue(rc)); close(rc); res }");
    }

    @Test
    public void testWriteText() {

        assertEval("{ s <- \"äöüß\"; rc <- rawConnection(raw(0), \"w\"); writeChar(s, rc); rawConnectionValue(rc) }");
        assertEval("{ rc <- rawConnection(raw(0), \"w\"); writeChar(\"Hello\", rc); writeChar(\", World\", rc); res <- rawConnectionValue(rc); close(rc); res }");
    }

    @Test
    public void testWriteBinary() {

        // this test is currently ignored, since 'charToRaw' is not compliant
        assertEval(Ignored.Unknown, "{ s <- \"äöüß\"; rc <- rawConnection(raw(0), \"wb\"); write(charToRaw(s), rc); res <- rawConnectionValue(rc); close(rc); res }");
        assertEval(Output.IgnoreWarningContext,
                        "{ zz <- rawConnection(raw(0), \"wb\"); x <- c(\"a\", \"this will be truncated\", \"abc\"); nc <- c(3, 10, 3); writeChar(x, zz, nc, eos = NULL); writeChar(x, zz, eos = \"\\r\\n\"); res <- rawConnectionValue(zz); close(zz); res }");
    }

}
