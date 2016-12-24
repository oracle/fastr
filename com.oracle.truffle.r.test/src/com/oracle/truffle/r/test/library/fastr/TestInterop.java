/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Test;

import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.r.test.TestBase;

public class TestInterop extends TestBase {

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
        assertEvalFastR("fileConn<-file(\"testScript.R\");writeLines(c(\"x<-c(1)\",\"cat(x)\"), fileConn);close(fileConn);.fastr.interop.evalFile(\"testScript.R\",\"application/x-r\")",
                        "x<-c(1);cat(x)");
        assertEvalFastR("fileConn<-file(\"testScript.R\");writeLines(c(\"x<-c(1)\",\"cat(x)\"), fileConn);close(fileConn);.fastr.interop.evalFile(\"testScript.R\")", "x<-c(1);cat(x)");
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
    }
}
