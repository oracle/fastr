/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.engine.shell;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.shadowed.org.jline.reader.Candidate;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.launcher.JLineConsoleCompleter;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.test.generate.FastRSession;

public class TestJLineConsoleCompleter {

    private Context context;
    private JLineConsoleCompleter consoleCompleter;

    private static final String TEST_PATH = "_TestJLineConsoleCompleter";

    private static final Source GET_COMPLETION_ENV = Source.newBuilder("R", "utils:::.CompletionEnv", "<completion>").internal(true).buildLiteral();
    private static final Source GET_FUNCTION = Source.newBuilder("R", "`$`", "<completion>").internal(true).buildLiteral();

    @Before
    public void before() {
        JLineConsoleCompleter.testingMode();
        context = FastRSession.getContextBuilder("R", "llvm").build();
        context.enter();

        context.eval("R", "1"); // initialize context
        context.eval("R", "l <- list(foo=1, bar=2)");
        context.eval("R", "setClass('testclass', representation(foo = 'character', bar = 'numeric')); tc <- new('testclass', foo = 'what', bar = 42)");
        TruffleObject tobj = (TruffleObject) RContext.getInstance().getEnv().asGuestValue(new TestJavaObject());
        context.getPolyglotBindings().putMember("tjo", tobj);
        context.eval("R", "tjo <- import('tjo')");

        consoleCompleter = new JLineConsoleCompleter(context);
    }

    @After
    public void dispose() {
        if (context != null) {
            context.leave();
            context.close();
        }
        String testPath = getTestPath();
        if (testPath != null) {
            File f = new File(testPath);
            if (f.exists()) {
                delete(f);
            }
        }
    }

    // TODO: NewRVersionMigration
    @Ignore
    @Test
    public void testCompl() {
        assertCompl("", 0, "", 0);
        assertCompl("", 1, "", 0);
        assertCompl(" ", 1, "", 0);

        assertCompl("(", 0, "", 0);
        assertCompl("(", 1, "", 0);
        assertCompl("=", 1, "=", 1);
        assertCompl("$", 1, "$", 1);

        assertCompl("strt", 4, "strt", 4, "strtoi", "strtrim");
        assertCompl("strt", 5, "", 0);
        assertCompl("strt", 6, "", 0);

        assertCompl("strto", 5, "strto", 5, "strtoi");
        assertCompl("strtoi", 5, "strto", 5, "strtoi");
        assertCompl("strtoi", 4, "strto", 4, "strtoi", "strtrim");
        assertCompl("strto ", 6, "", 0);
        assertCompl("strt blabla", 4, "strt", 4, "strtoi", "strtrim");
        assertCompl("strt  blabla", 4, "strt", 4, "strtoi", "strtrim");
        assertCompl("strt,,blabla", 4, "strt", 4, "strtoi", "strtrim");
        assertCompl("strt, blabla", 4, "strt", 4, "strtoi", "strtrim");
        assertCompl("strto blabla", 4, "strto", 4, "strtoi", "strtrim");
        assertCompl("blabla,strt", 11, "strt", 4, "strtoi", "strtrim");
        assertCompl("blabla strt", 11, "strt", 4, "strtoi", "strtrim");
        assertCompl("blabla,,strt", 12, "strt", 4, "strtoi", "strtrim");
        assertCompl("blabla  strt", 12, "strt", 4, "strtoi", "strtrim");
        assertCompl("blabla, strt", 12, "strt", 4, "strtoi", "strtrim");
        // Checkstyle: stop
        assertCompl("blabla ,strt", 12, "strt", 4, "strtoi", "strtrim");
        // Checkstyle: resume

        assertCompl("source('a')", 10, "a", 1);
        assertCompl("source('a')", 11, "", 0);
        assertCompl("source('a') ", 12, "", 0);
        assertCompl("source('a') ", 13, "", 0);

        assertCompl("base::strt", 10, "base::strt", 10, "base::strtoi", "base::strtrim");
        assertCompl("base:::strt", 11, "base:::strt", 11, "base:::strtoi", "base:::strtrim");
        assertCompl("base:::strttrt", 14, "base:::strttrt", 14, "base:::");

        assertCompl("strt(", 4, "strt", 4, "strtoi", "strtrim");
        assertCompl("strt(", 5, "", 5);
        assertCompl("f(strt", 6, "strt", 4, "strtoi", "strtrim");
        assertCompl("f(base::strt", 12, "base::strt", 10, "base::strtoi", "base::strtrim");
        assertCompl("f(strt(trt", 6, "strt", 4, "strtoi", "strtrim");
        assertCompl("f(strt(trt", 10, "trt", 3);
        assertCompl("f(strt(strto", 11, "strto", 4, "strtoi", "strtrim");
        assertCompl("f(strt(strto", 12, "strto", 5, "strtoi");

        assertCompl("grep(", 5, "", 5, "fixed=", "ignore.case=", "invert=", "pattern=", "perl=", "useBytes=", "value=", "x=");
        assertCompl("grep(pattern=\"a\",", 17, "", 0, "fixed=", "ignore.case=", "invert=", "pattern=", "perl=", "useBytes=", "value=", "x=");
        assertCompl("grep(pattern=\"a\"", 16, "pattern=\"a\"", 11);
        assertCompl("grep(pattern=\"a\", fixe", 22, "fixe", 4, "fixed=");

        assertCompl("grep (patt", 10, "patt", 4, "pattern=");
        assertCompl("grep,(patt", 10, "patt", 4, "pattern=");
        assertCompl("grep  (patt", 11, "patt", 4, "pattern=");
        assertCompl("grep,,(patt", 11, "patt", 4, "pattern=");
        // Checkstyle: stop
        assertCompl("grep ,(patt", 11, "patt", 4, "pattern=");
        // Checkstyle: resume
        assertCompl("grep, (patt", 11, "patt", 4, "pattern=");

        // this are ridiculos, but still, lets see if we feed R completion properly
        assertCompl("grep(patt ", 10, "", 4, "fixed=", "ignore.case=", "invert=", "pattern=", "perl=", "useBytes=", "value=", "x=");
        assertCompl("grep (patt ", 11, "", 4, "fixed=", "ignore.case=", "invert=", "pattern=", "perl=", "useBytes=", "value=", "x=");
        assertCompl("grep (patt  ", 12, "", 4, "fixed=", "ignore.case=", "invert=", "pattern=", "perl=", "useBytes=", "value=", "x=");

        // show only arguments for 'cor', and not also those for 'cor.test', 'cor.test.name' etc.
        assertCompl("cor(", 3, "cor", 3, "cor", "cor.test");
        assertCompl("cor(", 4, "", 0, "method=", "use=", "x=", "y=");
        assertCompl("cor(", 5, "", 0, "method=", "use=", "x=", "y=");
        assertCompl("cor( ", 5, "", 0, "method=", "use=", "x=", "y=");

        String noName = "_f_f_f_";
        assertCompl(noName + ".", 7, noName + ".", 7);
        assertCompl(noName + ".", 8, noName + ".", 8);
        assertCompl(noName + "." + File.separator, 9, noName + "." + File.separator, 9);
        assertCompl(noName + "'", 7, noName + "'", 7);
        assertCompl(noName + "'", 8, noName + "'", 8, NOT_EMPTY);
        // assertCompl(noName + "'." + File.separator, 8, noName + "'." + File.separator, 8,
        // NOT_EMPTY);
        // assertCompl(noName + "'." + File.separator, 9, NOT_EMPTY);
        assertCompl(noName + "'." + File.separator, 10, noName + "'." + File.separator, 10, NOT_EMPTY);
        // assertCompl(noName + "\"." + File.separator, 8, NOT_EMPTY);

        // named list
        assertCompl("l$", 2, "l$", 2, "l$bar", "l$foo");
        assertCompl("l$b", 3, "l$b", 3, "l$bar");
        // s4 object
        assertCompl("tc@", 3, "tc@", 3, "tc@bar", "tc@foo");
        assertCompl("tc@b", 4, "tc@b", 4, "tc@bar");

        // java object
        assertCompl("tjo$", 4, "tjo$", 4, "tjo$class", "tjo$name");
        assertCompl("tjo$n", 5, "tjo$n", 5, "tjo$name");
        assertCompl("tjo@", 4, "tjo@", 4, "tjo@class", "tjo@name");
        assertCompl("tjo@n", 5, "tjo@n", 5, "tjo@name");
    }

    // TODO: NewRVersionMigration
    @Ignore
    @Test
    public void testPathCompl() {
        testPathCompl('"');
        testPathCompl('\'');
    }

    private void testPathCompl(char quote) {
        File testDir = getTestDir();
        String testDirPath = testDir.getAbsolutePath() + File.separator;

        // no files in {testDirPath}
        // >source("{testDirPath}/<TAB> => no compl
        String cli = "source(" + quote + testDirPath;
        assertCompl(cli, cli.length());

        // >source("{testDirPath}/pa<TAB> => no compl
        cli = "source(" + quote + testDirPath + "pa";
        assertCompl(cli, cli.length());

        // one file in {testDirPath}
        File path = new File(testDir, "path1");
        path.mkdirs();

        // >source("{testDirPath}/pa<TAB> => testDirPath}/path1
        cli = "source(" + quote + testDirPath + "pa";
        assertCompl(cli, cli.length(), testDir.getAbsolutePath() + File.separator + "path1");

        // >source("{testDirPath}/pa<TAB>" (in closed quotes) => testDirPath}/path1
        cli = "source(" + quote + testDirPath + "pa" + quote;
        assertCompl(cli, cli.length() - 1, testDir.getAbsolutePath() + File.separator + "path1");

        Value completionEnv = context.eval(GET_COMPLETION_ENV);
        Value getFunction = context.eval(GET_FUNCTION);
        Value token = getFunction.execute(completionEnv, "token");
        Assert.assertEquals(testDirPath + "pa", token.asString());

        // three files in {testDirPath}
        new File(testDir, "path2").mkdirs();
        new File(testDir, "path3").mkdirs();
        // >source("{testDirPath}/pa<TAB> => testDirPath}/path1-3
        cli = "source(" + quote + testDirPath + "pa";
        assertCompl(cli, cli.length(), testDirPath + "path1", testDirPath + "path2", testDirPath + "path3");
        token = getFunction.execute(completionEnv, "token");
        Assert.assertEquals(testDirPath + "pa", token.asString());

        // >source("{testDirPath}/pa<TAB>" (in closed quotes) => testDirPath}/path1-3
        cli = "source(" + quote + testDirPath + "pa" + quote;
        assertCompl(cli, cli.length() - 1, testDirPath + "path1", testDirPath + "path2", testDirPath + "path3");

        // >source("{testDirPath}/papa<TAB> => no compl
        cli = "source(" + quote + testDirPath + "papa";
        assertCompl(cli, cli.length());
    }

    // e.g. check if the file path completion returned at least something
    private static final String NOT_EMPTY = "_@_Only.Check.If.Result.Not.Empty_@_";

    private void assertCompl(String buffer, int cursor, String... expected) {
        assertCompl(buffer, cursor, "", 0, expected);
    }

    private void assertCompl(String buffer, int cursor, String word, int wordCursor, String... expected) {
        List<Candidate> l = new LinkedList<>();
        consoleCompleter.complete(buffer, cursor, word, wordCursor, l);

        if (expected == null || expected.length == 0) {
            assertTrue(l.isEmpty());
        } else if (expected.length == 1 && NOT_EMPTY.equals(expected[0])) {
            assertFalse(l.isEmpty());
        } else {
            String[] result = new String[l.size()];
            for (int i = 0; i < l.size(); i++) {
                result[i] = l.get(i).value();
            }
            Assert.assertArrayEquals(expected, result);
        }
    }

    private File getTestDir() {
        String testPath = getTestPath();
        if (testPath != null) {
            File f = new File(testPath);
            if (f.exists()) {
                delete(f);
            }
            f.mkdirs();
            return f;
        }
        return null;
    }

    private static String getTestPath() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (tmpDir != null) {
            return tmpDir + File.separator + TEST_PATH;
        }
        return null;
    }

    private void delete(File f) {
        if (f.exists()) {
            if (f.isDirectory()) {
                File[] files = f.listFiles();
                if (files != null) {
                    for (File child : files) {
                        delete(child);
                    }
                }
            }
            f.delete();
        }
    }

    public static final class TestJavaObject {
        public final String name = "abc";
    }
}
