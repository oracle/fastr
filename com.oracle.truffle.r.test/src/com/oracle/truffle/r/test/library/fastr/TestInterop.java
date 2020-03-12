/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.nodes.builtin.fastr.FastRInterop;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.graalvm.polyglot.PolyglotException;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.r.runtime.conn.SeekableMemoryByteChannel;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.test.TestBase;
import com.oracle.truffle.r.test.generate.FastRContext;
import com.oracle.truffle.r.test.generate.FastRSession;
import static com.oracle.truffle.r.test.library.fastr.Utils.errorIn;
import java.io.File;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;

public class TestInterop extends TestBase {

    private static final SeekableMemoryByteChannel CHANNEL = new SeekableMemoryByteChannel();
    private static final String CHANNEL_NAME = "_fastr_channel0";
    private static final String TEST_EVAL_FILE = "_testInteropEvalFile_testScript_.R";

    private static final class TestVariable {
        public final String name;
        public Object value;
        public String convertsTo;
        public String rValue;
        public Class<?> clazz;

        private TestVariable(String name, Object value, String convertsTo) {
            this(name, value, convertsTo, null);
        }

        private TestVariable(String name, Object value, String convertsTo, String rValue) {
            this.name = name;
            this.value = value;
            this.convertsTo = convertsTo;
            this.rValue = rValue != null ? rValue : "" + value;
            clazz = value.getClass();
        }
    }

    private static final TestVariable[] testVariables = new TestVariable[]{
                    new TestVariable("testBooleanVariable", true, "'logical'", "TRUE"),
                    new TestVariable("testByteVariable", (byte) 1, "'integer'"),
                    new TestVariable("testCharVariable", 'a', "'character'", "'a'"),
                    new TestVariable("testDoubleVariable", 2.1, "'double'"),
                    new TestVariable("testFloatVariable", 3.1F, "'double'"),
                    new TestVariable("testIntegerVariable", 4, "'integer'"),
                    new TestVariable("testShortVariable", (short) 5, "'integer'"),
                    new TestVariable("testStringVariable", "abc", "'character'", "'abc'")
    };

    @Before
    public void testInit() {
        FastRInterop.testingMode();
    }

    @After
    public void cleanup() {
        File f = new File(TEST_EVAL_FILE);
        if (f.exists()) {
            f.delete();
        }
    }

    @Test
    public void testPolyglotAccessWhenPolyglotBindingsAreDisabled() {
        try (org.graalvm.polyglot.Context context = FastRSession.getContextBuilder("R", "llvm").build()) {
            context.eval("R", "eval.polyglot('js', '1+3')");
            Assert.fail("no PolyglotException exception occurred");
        } catch (PolyglotException ex) {
            String message = ex.getMessage();
            assertTrue(message, message.contains("Language with id 'js' is not available. Did you start R with --polyglot or use allowPolyglotAccess when building the context?"));
        }
    }

    @Test
    public void testInteropEval() {
        assertEvalFastR("eval.polyglot('R', '14 + 2')", "16");
        assertEvalFastR("eval.polyglot('R', '1')", "1");
        assertEvalFastR("eval.polyglot('R', '1L')", "1L");
        assertEvalFastR("eval.polyglot('R', 'TRUE')", "TRUE");
        assertEvalFastR("eval.polyglot('R', 'as.character(123)')", "as.character(123)");

        assertEvalFastR("eval.polyglot(, 'bar')",
                        "cat('Error in eval.polyglot(, \"bar\") :\\n  No language id provided, please refer to ?eval.polyglot for more details.\\n')");
        // Checkstyle: stop
        assertEvalFastR("eval.polyglot(,,'bar')",
                        "cat('Error in eval.polyglot(, , \"bar\") :\\n  Could not find language corresponding to extension \\'bar\\', you can specify the language id explicitly, please refer to ?eval.polyglot for more details.\\n')");
        // Checkstyle: resume
        assertEvalFastR("eval.polyglot('foo', 'bar')",
                        "cat('Error in eval.polyglot(\"foo\", \"bar\") :\\n  Language with id \\'foo\\' is not available. Did you start R with --polyglot or use allowPolyglotAccess when building the context?\\n')");
        assertEvalFastR("eval.polyglot('nfi', 'foo.bar')",
                        "cat('Error in eval.polyglot(\"nfi\", \"foo.bar\") :\\n  Language with id \\'nfi\\' is not available. Did you start R with --polyglot or use allowPolyglotAccess when building the context?\\n')");
        // Checkstyle: stop
        assertEvalFastR("eval.polyglot('foo',, 'bar')",
                        "cat('Error in eval.polyglot(\"foo\", , \"bar\") :\\n  Language with id \\'foo\\' is not available. Did you start R with --polyglot or use allowPolyglotAccess when building the context?\\n')");
        assertEvalFastR("eval.polyglot('nfi',,'foo.bar')",
                        "cat('Error in eval.polyglot(\"nfi\", , \"foo.bar\") :\\n  Language with id \\'nfi\\' is not available. Did you start R with --polyglot or use allowPolyglotAccess when building the context?\\n')");
        // Checkstyle: resume
    }

    @Test
    public void testInteropImportValue() {
        for (TestVariable t : testVariables) {
            assertEvalFastR("v <- import('" + t.name + "'); typeof(v)", t.convertsTo);
            assertEvalFastR("v <- import('" + t.name + "'); v", "" + t.rValue);
        }
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
                        "cat('Error in eval.polyglot(\"nonExistentLanguage\", \"code\") :\n  Language with id \\'nonExistentLanguage\\' is not available. Did you start R with --polyglot or use allowPolyglotAccess when building the context?\\n')");
        assertEvalFastR("eval.polyglot(code='')", "cat('Error in eval.polyglot(code = \"\") :\n  No language id provided, please refer to ?eval.polyglot for more details.\\n')");
        assertEvalFastR("eval.polyglot(languageId='js')", "cat('Error in eval.polyglot(languageId = \"js\") :\n  No code or path provided, please refer to ?eval.polyglot for more details.\\n')");
    }

    @Test
    public void testInvoke() {
        assertEvalFastR("cl <- java.type('java.math.BigInteger'); fo <- new(cl, 'FFFFFFFFFFFFFFFFFF', 16L); fo@bitLength()", "print(72L)");
        assertEvalFastR("cl <- java.type('java.math.BigInteger'); fo <- 1:100; try(fo@bitLength(), silent=TRUE); fo <- new(cl, 'FFFFFFFFFFFFFFFFFF', 16L); fo@bitLength()",
                        "print(72L)");
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

    private static final TestJavaObject[] testJavaObjects = new TestJavaObject[]{
                    new TestJavaObject("testPOJO", new POJO()),
                    new TestJavaObject("testPOJOWrite", new POJO()),
                    new TestJavaObject("testIntArray", new int[]{1, -5, 199}),
                    new TestJavaObject("testStringArray", new String[]{"a", "", "foo"})};

    // TODO: export/importSymbol
    @Override
    public void addPolyglotSymbols(FastRContext context) {
        FastRSession.execInContext(context, () -> {
            RContext rContext = RContext.getInstance();
            for (TestJavaObject t : TestInterop.testJavaObjects) {
                context.getPolyglotBindings().putMember(t.name, rContext.getEnv().asGuestValue(t.object));
            }
            context.getPolyglotBindings().putMember("foreignObjectFactory", rContext.getEnv().asGuestValue(new FOFactory()));

            for (TestVariable t : TestInterop.testVariables) {
                context.getContext().getBindings("R").putMember(t.name, t.value);
                context.getPolyglotBindings().putMember(t.name, t.value);
                context.getContext().getBindings("R").putMember(t.name + "Read", (ProxyExecutable) (Value... args) -> {
                    assertEquals(context.getContext().getBindings("R").getMember(t.name).as(t.clazz), t.value);
                    return true;
                });
            }
            context.getPolyglotBindings().putMember(CHANNEL_NAME, rContext.getEnv().asGuestValue(CHANNEL));
            return null;
        });
    }

    @Test
    public void testPrinting() {
        assertEvalFastR("v <- import('testPOJO'); print(v)", "cat('[polyglot value]\\n" +
                        "$class\\n" +
                        "[polyglot value]\\n" +
                        "\\n" +
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
        assertEvalFastR("tpojo <- import('testPOJO'); tos <- tpojo$toString(); is.character(tos) && length(tos) == 1", "TRUE");
        assertEvalFastR("ja <- new(java.type('int[]'), 3); tos <- ja$toString(); is.character(tos) && length(tos) == 1", "TRUE");
        assertEvalFastR("ja <- new(java.type('int[]'), 3); ts <- ja$toString; tos <- ts(); is.character(tos) && length(tos) == 1", "TRUE");

        assertEvalFastR("tpojo <- import('testPOJOWrite'); tpojo$stringValue <- '$aaa'; tpojo$stringValue", "'$aaa'");

    }

    @Test
    public void testSlot() {
        // tests the invoke msgs
        assertEvalFastR("tpojo <- import('testPOJO'); tos <- tpojo@toString(); is.character(tos) && length(tos) == 1", "TRUE");
        assertEvalFastR("ja <-new(java.type('int[]'), 3); tos <- ja@toString(); is.character(tos) && length(tos) == 1", "TRUE");
        assertEvalFastR("ja <-new(java.type('int[]'), 3); ts <- ja@toString; tos <- ts(); is.character(tos) && length(tos) == 1", "TRUE");

        assertEvalFastR("tpojo <- import('testPOJOWrite'); tpojo@stringValue <- '@aaa'; tpojo@stringValue", "'@aaa'");
    }

    public void testVariableWrite() {
        for (TestVariable t : testVariables) {
            assertEvalFastR("typeof(" + t.name + ")", t.convertsTo);
            assertEvalFastR(t.name, "" + t.rValue);
            assertEvalFastR(t.name + "Read()", "TRUE");
        }
    }

    private static final String CREATE_FO = "foreignObjectFactory <- import('foreignObjectFactory'); fo <- foreignObjectFactory$createInvocableNotReadable(); ";

    @Test
    public void testInvocableNoReadable() {
        String member = InvocableNotReadable.MEMBER_NAME;

        assertEvalFastR(CREATE_FO + "names(fo)", "print('" + member + "')");
        assertEvalFastR(CREATE_FO + "fo", "cat('[polyglot value]\n$" + member + "\n[not readable value]\n\n')");
        assertEvalFastR(CREATE_FO + "fo$" + member, errorIn("fo$" + member, "invalid index/identifier during foreign access: " + member));
        assertEvalFastR(CREATE_FO + "fo$" + member + "()", errorIn("fo$" + member, "invalid index/identifier during foreign access: " + member));
        assertEvalFastR(CREATE_FO + "fo@" + member + "()", "print(42)");

        assertEvalFastR(CREATE_FO + "as.list(fo)", "cat('named list()\n')");
    }

    public static class FOFactory {
        public static TruffleObject createInvocableNotReadable() {
            return new InvocableNotReadable();
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static class InvocableNotReadable implements TruffleObject {
        private static final String MEMBER_NAME = "invocable";
        private static ExecutableTO invocable = new ExecutableTO();

        @ExportMessage
        public boolean hasMembers() {
            return true;
        }

        @SuppressWarnings("unused")
        @ExportMessage
        public Object getMembers(boolean includeInternals) {
            return new Array(MEMBER_NAME);
        }

        @SuppressWarnings("unused")
        @ExportMessage
        public boolean isMemberReadable(String name) {
            return false;
        }

        @SuppressWarnings("unused")
        @ExportMessage
        public boolean isMemberInvocable(String name) {
            return MEMBER_NAME.equals(name);
        }

        @SuppressWarnings("unused")
        @ExportMessage
        public Object invokeMember(String name, Object... args) throws UnsupportedMessageException {
            if (!MEMBER_NAME.equals(name)) {
                throw UnsupportedMessageException.create();
            }
            return invocable.execute(args);
        }

        @SuppressWarnings("unused")
        @ExportMessage
        public Object readMember(String name) throws UnsupportedMessageException {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static class ExecutableTO implements TruffleObject {
        @ExportMessage
        public boolean isExecutable() {
            return true;
        }

        @SuppressWarnings("unused")
        @ExportMessage
        public Object execute(Object... args) {
            return 42;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static class Array implements TruffleObject {
        private final Object[] a;

        public Array(Object... a) {
            this.a = a;
        }

        @ExportMessage
        public boolean hasArrayElements() {
            return true;
        }

        @ExportMessage
        public long getArraySize() {
            return a.length;
        }

        @ExportMessage
        public boolean isArrayElementReadable(long i) {
            return i <= a.length;
        }

        @ExportMessage
        public Object readArrayElement(long i) {
            return a[(int) i];
        }

    }
}
