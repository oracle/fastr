/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.test.library.fastr;

import com.oracle.truffle.api.interop.TruffleObject;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.r.runtime.conn.SeekableMemoryByteChannel;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.test.TestBase;
import com.oracle.truffle.r.test.generate.FastRSession;
import java.io.File;
import org.junit.After;

public class TestInterop extends TestBase {

    private static final SeekableMemoryByteChannel CHANNEL = new SeekableMemoryByteChannel();
    private static final String CHANNEL_NAME = "_fastr_channel0";
    private static final String TEST_EVAL_FILE = "_testInteropEvalFile_testScript_.R";

    @After
    public void cleanup() {
        File f = new File(TEST_EVAL_FILE);
        if (f.exists()) {
            f.delete();
        }
    }

    @Test
    public void testInteropEval() {
        assertEvalFastR("eval.polyglot('R', '14 + 2')", "16");
        assertEvalFastR("eval.polyglot('R', '1')", "1");
        assertEvalFastR("eval.polyglot('R', '1L')", "1L");
        assertEvalFastR("eval.polyglot('R', 'TRUE')", "TRUE");
        assertEvalFastR("eval.polyglot('R', 'as.character(123)')", "as.character(123)");
    }

    @Test
    public void testInteropExport() {
        assertEvalFastR("export('foo', 14 + 2)", "invisible()");
        assertEvalFastR("export('foo', 'foo')", "invisible()");
        assertEvalFastR("export('foo', 1:100)", "invisible()");
        assertEvalFastR("export('foo', new.env())", "invisible()");
    }

    @Test
    public void testInteropEvalFile() {
        assertEvalFastR("fileConn<-file(\"" + TEST_EVAL_FILE + "\");writeLines(c(\"x<-c(1)\",\"cat(x)\"), fileConn);close(fileConn);eval.polyglot(languageId=\"R\", path=\"" +
                        TEST_EVAL_FILE + "\")",
                        "x<-c(1);cat(x)");
        assertEvalFastR("fileConn<-file(\"" + TEST_EVAL_FILE + "\");writeLines(c(\"x<-c(1)\",\"cat(x)\"), fileConn);close(fileConn);eval.polyglot(path=\"" + TEST_EVAL_FILE + "\")",
                        "x<-c(1);cat(x)");
        assertEvalFastR("tryCatch(eval.polyglot(path=\"/a/b.R\"),  error = function(e) e$message)", "cat('[1] \"Error reading file: /a/b.R\"\\n')");

        assertEvalFastR("eval.polyglot()", "cat('Error in eval.polyglot() :\n  Wrong arguments combination, please refer to ?eval.polyglot for more details.\\n')");
        assertEvalFastR("eval.polyglot(,'abc',)", "cat('Error in eval.polyglot(, \"abc\", ) :\n  No language id provided, please refer to ?eval.polyglot for more details.\\n')");
        assertEvalFastR("eval.polyglot('R', 'as.character(123)')", "as.character(123)");
        assertEvalFastR("eval.polyglot('someLanguage')", "cat('Error in eval.polyglot(\"someLanguage\") :\n  No code or path provided, please refer to ?eval.polyglot for more details.\\n')");
        assertEvalFastR("eval.polyglot('js', 'console.log(42)', 'file.js')",
                        "cat('Error in eval.polyglot(\"js\", \"console.log(42)\", \"file.js\") :\n  Wrong arguments combination, please refer to ?eval.polyglot for more details.\\n')");
        assertEvalFastR("f<-paste0(tempfile(),'.nonLanguageExtension'); file.create(f); tryCatch(eval.polyglot(path=f), finally=file.remove(f))",
                        "cat('Error in eval.polyglot(path = f) :\n  Could not find language corresponding to extension \\'nonLanguageExtension\\', you can specify the language id explicitly, please refer to ?eval.polyglot for more details.\\n')");
        assertEvalFastR("eval.polyglot('nonExistentLanguage', 'code')",
                        "cat('Error in eval.polyglot(\"nonExistentLanguage\", \"code\") :\n  Language with id \\'nonExistentLanguage\\' is not available. Did you start R with --polyglot?\\n')");
        assertEvalFastR("eval.polyglot(code='')", "cat('Error in eval.polyglot(code = \"\") :\n  No language id provided, please refer to ?eval.polyglot for more details.\\n')");
        assertEvalFastR("eval.polyglot(languageId='js')", "cat('Error in eval.polyglot(languageId = \"js\") :\n  No code or path provided, please refer to ?eval.polyglot for more details.\\n')");
    }

    @Test
    public void testInvoke() {
        assertEvalFastR("cl <- java.type('java.math.BigInteger'); fo <- new(cl, 'FFFFFFFFFFFFFFFFFF', 16L); fo@bitLength()", "print(72L)");
        assertEvalFastR("cl <- java.type('java.math.BigInteger'); fo <- 1:100; try(fo@bitLength(), silent=TRUE); fo <- new(cl, 'FFFFFFFFFFFFFFFFFF', 16L); fo@bitLength()",
                        "print(72L)");
        assertEvalFastR("cl <- java.type('java.math.BigInteger'); fo <- new(cl, 'FFFFFFFFFFFFFFFFFF', 16L); fo@bitLength",
                        "cat('Error in fo@bitLength :\n  cannot get a slot (\"bitLength\") from an object of type \"polyglot.value\"\n')");
        assertEvalFastR("cl <- java.type('java.math.BigInteger'); fo <- 1:100; try(fo@bitLength, silent=TRUE); fo <- new(cl, 'FFFFFFFFFFFFFFFFFF', 16L); fo@bitLength",
                        "cat('Error in fo@bitLength :\n  cannot get a slot (\"bitLength\") from an object of type \"polyglot.value\"\n')");
    }

    /**
     * Used for testing interop functionality.
     */
    public static final class POJO {
        public String stringValue = "foo";
        public char charValue = 'R';
        public int intValue = 1;
        public short shortValue = -100;
        public boolean booleanValue = true;
        public long longValue = 123412341234L;
    }

    private static final class TestJavaObject {
        public final String name;
        public Object object;

        private TestJavaObject(String name, Object object) {
            this.name = name;
            this.object = object;
        }
    }

    private static final TestJavaObject[] testJavaObjects = new TestJavaObject[]{new TestJavaObject("testPOJO", new POJO()), new TestJavaObject("testIntArray", new int[]{1, -5, 199}),
                    new TestJavaObject("testStringArray", new String[]{"a", "", "foo"})};

    // TODO: export/importSymbol
    @Override
    public void addPolyglotSymbols(org.graalvm.polyglot.Context context) {
        FastRSession.execInContext(context, () -> {
            RContext rContext = RContext.getInstance();
            for (TestJavaObject t : TestInterop.testJavaObjects) {
                TruffleObject tobj = (TruffleObject) rContext.getEnv().asGuestValue(t.object);
                context.getPolyglotBindings().putMember(t.name, tobj);
            }
            context.getPolyglotBindings().putMember(CHANNEL_NAME, rContext.getEnv().asGuestValue(CHANNEL));
            return null;
        });
    }

    @Test
    public void testPrinting() {
        assertEvalFastR("v <- import('testPOJO'); print(v)", "cat('[polyglot value]\\n" +
                        "$stringValue\\n" +
                        "[1] \"foo\"\\n" +
                        "\\n" +
                        "$charValue\\n" +
                        "[1] \"R\"\\n" +
                        "\\n" +
                        "$intValue\\n" +
                        "[1] 1\\n" +
                        "\\n" +
                        "$shortValue\\n" +
                        "[1] -100\\n" +
                        "\\n" +
                        "$booleanValue\\n" +
                        "[1] TRUE\\n" +
                        "\\n" +
                        "$longValue\\n" +
                        "[1] 123412341234\\n\\n')");
        assertEvalFastR("v <- import('testStringArray'); print(v)", "cat('[polyglot value]\\n[1] \"a\"   \"\"    \"foo\"\\n')");
        assertEvalFastR("v <- import('testIntArray'); print(v)", "cat('[polyglot value]\\n[1]   1  -5 199\\n')");
        assertEvalFastR("v <- import('testIntArray'); v", "cat('[polyglot value]\\n[1]   1  -5 199\\n')");
        assertEvalFastR("v <- import('testPOJO'); names(v)", "c('stringValue', 'charValue', 'intValue', 'shortValue', 'booleanValue', 'longValue', 'class')");
    }

    @Test
    public void testChannelConnection() throws IOException {

        final String line0 = "Hello, World!\n";
        final String line1 = "second line\n";
        CHANNEL.write(line0.getBytes());
        CHANNEL.write(line1.getBytes());
        long oldPos = CHANNEL.position();
        CHANNEL.position(0);
        assertEvalFastR(String.format("v <- import('%s'); zz <- .fastr.channelConnection(v, 'r+', 'native.enc'); res <- readLines(zz); close(zz); res",
                        CHANNEL_NAME),
                        "c('Hello, World!', 'second line')");

        if (!generatingExpected()) {
            // test if FastR consumed the data
            Assert.assertEquals(oldPos, CHANNEL.position());

            // re-open channel
            CHANNEL.setOpen(true);
            CHANNEL.position(0);
        }

        final String response = "hi there";
        assertEvalFastR(String.format("v <- import('%s'); zz <- .fastr.channelConnection(v, 'r+', 'native.enc'); writeLines('%s', zz); close(zz); NULL ", CHANNEL_NAME, response),
                        "NULL");

        if (!generatingExpected()) {
            ByteBuffer buf = ByteBuffer.allocate(response.length());
            CHANNEL.setOpen(true);
            CHANNEL.position(0);
            CHANNEL.read(buf);
            Assert.assertEquals(response, new String(buf.array()));
        }
    }

    @Test
    public void testDollar() {
        // tests the execute msg
        assertEvalFastR("tpojo <- import('testPOJO)'; tos <- tpojo$toString(); is.character(tos) && length(tos) == 1", "TRUE");
        assertEvalFastR("ja <- new(java.type('int[]'), 3); tos <- ja$toString(); is.character(tos) && length(tos) == 1", "TRUE");
    }

    @Test
    public void testSlot() {
        // tests the invoke msg
        assertEvalFastR("tpojo <- import('testPOJO)'; tpojo@toString(); is.character(tos) && length(tos) == 1", "TRUE");
        assertEvalFastR("ja <-new(java.type('int[]'), 3); tos <- ja@toString(); is.character(tos) && length(tos) == 1", "TRUE");
    }
}
