/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.test.library.fastr;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.runtime.conn.SeekableMemoryByteChannel;
import com.oracle.truffle.r.test.TestBase;
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
        assertEvalFastR(".fastr.interop.eval('application/x-r', '14 + 2')", "16");
        assertEvalFastR(".fastr.interop.eval('application/x-r', '1')", "1");
        assertEvalFastR(".fastr.interop.eval('application/x-r', '1L')", "1L");
        assertEvalFastR(".fastr.interop.eval('application/x-r', 'TRUE')", "TRUE");
        assertEvalFastR(".fastr.interop.eval('application/x-r', 'as.character(123)')", "as.character(123)");
    }

    @Test
    public void testInteropExport() {
        assertEvalFastR(".fastr.interop.export('foo', 14 + 2)", "invisible()");
        assertEvalFastR(".fastr.interop.export('foo', 'foo')", "invisible()");
        assertEvalFastR(".fastr.interop.export('foo', 1:100)", "invisible()");
        assertEvalFastR(".fastr.interop.export('foo', new.env())", "invisible()");
    }

    @Test
    public void testInteropEvalFile() {
        assertEvalFastR("fileConn<-file(\"" + TEST_EVAL_FILE + "\");writeLines(c(\"x<-c(1)\",\"cat(x)\"), fileConn);close(fileConn);.fastr.interop.evalFile(\"" + TEST_EVAL_FILE +
                        "\",\"application/x-r\")",
                        "x<-c(1);cat(x)");
        assertEvalFastR("fileConn<-file(\"" + TEST_EVAL_FILE + "\");writeLines(c(\"x<-c(1)\",\"cat(x)\"), fileConn);close(fileConn);.fastr.interop.evalFile(\"" + TEST_EVAL_FILE + "\")",
                        "x<-c(1);cat(x)");
        assertEvalFastR("tryCatch(.fastr.interop.evalFile(\"/a/b.R\"),  error = function(e) e$message)", "cat('[1] \"Error reading file: /a/b.R\"\\n')");
    }

    /**
     * Used for testing interop functionality.
     */
    public static final class POJO {
        public int intValue = 1;
        public long longValue = 123412341234L;
        public char charValue = 'R';
        public short shortValue = -100;
        public boolean booleanValue = true;
        public String stringValue = "foo";
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

    @Override
    public void addPolyglotSymbols(PolyglotEngine.Builder builder) {
        for (TestJavaObject t : TestInterop.testJavaObjects) {
            builder.globalSymbol(t.name, JavaInterop.asTruffleObject(t.object));
        }
        builder.globalSymbol(CHANNEL_NAME, JavaInterop.asTruffleObject(CHANNEL));
    }

    @Test
    public void testPrinting() {
        assertEvalFastR("v <- .fastr.interop.import('testPOJO'); print(v)", "cat('$intValue\\n" +
                        "[1] 1\\n" +
                        "\\n" +
                        "$longValue\\n" +
                        "[1] 123412341234\\n" +
                        "\\n" +
                        "$charValue\\n" +
                        "[1] \"R\"\\n" +
                        "\\n" +
                        "$shortValue\\n" +
                        "[1] -100\\n" +
                        "\\n" +
                        "$booleanValue\\n" +
                        "[1] TRUE\\n" +
                        "\\n" +
                        "$stringValue\\n" +
                        "[1] \"foo\"\\n" +
                        "\\n" +
                        "attr(,\"is.truffle.object\")\\n" +
                        "[1] TRUE\\n')");
        assertEvalFastR("v <- .fastr.interop.import('testStringArray'); print(v)", "cat('[1] \"a\"   \"\"    \"foo\"\\n" +
                        "attr(,\"is.truffle.object\")\\n" +
                        "[1] TRUE\\n')");
        assertEvalFastR("v <- .fastr.interop.import('testIntArray'); print(v)", "cat('[1]   1  -5 199\\n" +
                        "attr(,\"is.truffle.object\")\\n" +
                        "[1] TRUE\\n')");
        assertEvalFastR("v <- .fastr.interop.import('testIntArray'); v", "cat('[1]   1  -5 199\\n" +
                        "attr(,\"is.truffle.object\")\\n" +
                        "[1] TRUE\\n')");
        assertEvalFastR("v <- .fastr.interop.import('testPOJO'); names(v)", "c('intValue', 'longValue', 'charValue', 'shortValue', 'booleanValue', 'stringValue')");
    }

    @Test
    public void testChannelConnection() throws IOException {

        final String line0 = "Hello, World!\n";
        final String line1 = "second line\n";
        CHANNEL.write(line0.getBytes());
        CHANNEL.write(line1.getBytes());
        long oldPos = CHANNEL.position();
        CHANNEL.position(0);
        assertEvalFastR(String.format("v <- .fastr.interop.import('%s'); zz <- .fastr.channelConnection(v, 'r+', 'native.enc'); res <- readLines(zz); close(zz); res",
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
        assertEvalFastR(String.format("v <- .fastr.interop.import('%s'); zz <- .fastr.channelConnection(v, 'r+', 'native.enc'); writeLines('%s', zz); close(zz); NULL ", CHANNEL_NAME, response),
                        "NULL");

        if (!generatingExpected()) {
            ByteBuffer buf = ByteBuffer.allocate(response.length());
            CHANNEL.setOpen(true);
            CHANNEL.position(0);
            CHANNEL.read(buf);
            Assert.assertEquals(response, new String(buf.array()));
        }
    }
}
