/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.engine.shell;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.LinkedList;

import org.graalvm.polyglot.Context;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.oracle.truffle.r.launcher.JLineConsoleCompleter;
import org.junit.Ignore;

public class TestJLineConsoleCompleter {

    private Context context;
    private JLineConsoleCompleter consoleCompleter;

    @Before
    public void before() {
        JLineConsoleCompleter.testingMode();
        context = Context.create();
        consoleCompleter = new JLineConsoleCompleter(context);
    }

    @After
    public void dispose() {
        if (context != null) {
            context.close();
        }
    }

    // disabled because it uses Engine, which clashes with other tests that use PolyglotEngine
    @Ignore
    @Test
    public void testCompl() {
        assertCompl("", 0);
        assertCompl("", 1);
        assertCompl(" ", 1);

        assertCompl("(", 0);
        assertCompl("(", 1);
        assertCompl("=", 1);
        assertCompl("$", 1);

        assertCompl("strt", 4, "strtoi", "strtrim");
        assertCompl("strt", 5);
        assertCompl("strt", 6);

        assertCompl("strto", 5, "strtoi");
        assertCompl("strtoi", 5, "strtoi");
        assertCompl("strtoi", 4, "strtoi", "strtrim");
        assertCompl("strto ", 6);
        assertCompl("strto,", 6);
        assertCompl("blabla,strt", 11, "strtoi", "strtrim");
        assertCompl("blabla strt", 11, "strtoi", "strtrim");
        assertCompl("blabla,,strt", 12, "strtoi", "strtrim");
        assertCompl("blabla  strt", 12, "strtoi", "strtrim");
        assertCompl("blabla, strt", 12, "strtoi", "strtrim");
        // Checkstyle: stop
        assertCompl("blabla ,strt", 12, "strtoi", "strtrim");
        // Checkstyle: resume

        assertCompl("source('a')", 10);
        assertCompl("source('a')", 11);
        assertCompl("source('a') ", 12);
        assertCompl("source('a') ", 13);

        assertCompl("base::strt", 10, "base::strtoi", "base::strtrim");
        assertCompl("base:::strt", 11, "base:::strtoi", "base:::strtrim");
        assertCompl("base:::strttrt", 14, "base:::");

        assertCompl("strt(", 4, "strtoi", "strtrim");
        assertCompl("strt(", 5);
        assertCompl("f(strt", 6, "strtoi", "strtrim");
        assertCompl("f(base::strt", 12, "base::strtoi", "base::strtrim");
        assertCompl("f(strt(trt", 6, "strtoi", "strtrim");
        assertCompl("f(strt(trt", 10);
        assertCompl("f(strt(strto", 11, "strtoi", "strtrim");
        assertCompl("f(strt(strto", 12, "strtoi");

        assertCompl("grep(", 5, "fixed=", "ignore.case=", "invert=", "pattern=", "perl=", "useBytes=", "value=", "x=");
        assertCompl("grep(pattern=\"a\",", 17, "fixed=", "ignore.case=", "invert=", "pattern=", "perl=", "useBytes=", "value=", "x=");
        assertCompl("grep(pattern=\"a\"", 16);
        assertCompl("grep(pattern=\"a\", fixe", 22, "fixed=");

        assertCompl("grep (patt", 10, "pattern=");
        assertCompl("grep,(patt", 10, "pattern=");
        assertCompl("grep  (patt", 11, "pattern=");
        assertCompl("grep,,(patt", 11, "pattern=");
        // Checkstyle: stop
        assertCompl("grep ,(patt", 11, "pattern=");
        // Checkstyle: resume
        assertCompl("grep, (patt", 11, "pattern=");

        assertCompl("grep(patt ", 10, "fixed=", "ignore.case=", "invert=", "pattern=", "perl=", "useBytes=", "value=", "x=");
        assertCompl("grep (patt ", 11, "fixed=", "ignore.case=", "invert=", "pattern=", "perl=", "useBytes=", "value=", "x=");
        assertCompl("grep (patt  ", 12, "fixed=", "ignore.case=", "invert=", "pattern=", "perl=", "useBytes=", "value=", "x=");

        // show only arguments for 'cor', and not also those for 'cor.test', 'cor.test.name' etc.
        assertCompl("cor(", 3, "cor", "cor.test");
        assertCompl("cor(", 4, "method=", "use=", "x=", "y=");
        assertCompl("cor(", 5, "method=", "use=", "x=", "y=");
        assertCompl("cor( ", 5, "method=", "use=", "x=", "y=");

        String noName = "_f_f_f_";
        assertCompl(noName + ".", 7);
        assertCompl(noName + ".", 8);
        assertCompl(noName + "." + File.separator, 9);
        assertCompl(noName + "'", 7);
        assertCompl(noName + "'", 8, NOT_EMPTY);
        assertCompl(noName + "'." + File.separator, 8, NOT_EMPTY);
        assertCompl(noName + "'." + File.separator, 9, NOT_EMPTY);
        assertCompl(noName + "'." + File.separator, 10, NOT_EMPTY);
        assertCompl(noName + "\"." + File.separator, 8, NOT_EMPTY);
    }

    // e.g. check if the file path completion returned at least something
    private static final String NOT_EMPTY = "_@_Only.Check.If.Result.Not.Empty_@_";

    private void assertCompl(String buffer, int cursor, String... expected) {
        LinkedList<CharSequence> l = new LinkedList<>();
        consoleCompleter.complete(buffer, cursor, l);

        if (expected == null || expected.length == 0) {
            assertTrue(l.isEmpty());
        } else if (expected.length == 1 && NOT_EMPTY.equals(expected[0])) {
            assertFalse(l.isEmpty());
        } else {
            Assert.assertArrayEquals(expected, l.toArray(new CharSequence[l.size()]));
        }
    }
}
