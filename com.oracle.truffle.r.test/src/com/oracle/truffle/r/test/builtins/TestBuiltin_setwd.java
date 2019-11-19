/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.test.builtins;

import com.oracle.truffle.r.runtime.Utils;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;
import com.oracle.truffle.r.test.generate.FastRSession;
import java.io.File;
import java.io.IOException;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;

public class TestBuiltin_setwd extends TestBase {

    protected static org.graalvm.polyglot.Context context;
    private static String beforeWD;

    @BeforeClass
    public static void before() {
        context = FastRSession.getContextBuilder("R", "llvm").build();
        context.eval("R", "1"); // initialize context
        context.enter();
        beforeWD = gewtWD();
    }

    @AfterClass
    public static void after() {
        context.eval(getSource("setwd('" + beforeWD + "')"));
        context.leave();
        context.close();
        Utils.resetWD();

    }

    private static final String testWDDir = "com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/builtins/data/";

    @Test
    public void testSetWDvsUserDir() throws IOException {
        String wd = gewtWD();

        // 1.) current java user.dir is {fastr_home} and it contains a file README.md
        assertTrue("no README.md file in the {fastr_home}, try to use some another file for this test then.", new File(wd + "README.md").exists());
        // 2.) lets create a test folder containg a directory with the same name - README.md
        new File(wd + "com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/builtins/data/README.md").mkdirs();
        File testFile = new File(wd + "com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/builtins/data/README.md/test");
        if (!testFile.exists()) {
            testFile.createNewFile();
        }
        // 3.) and setwd to {testdir} and try to access that README.md/test
        Value result = context.eval(getSource("setwd('" + wd + testWDDir + "'); .Call(tools:::C_Rmd5, 'README.md/test')"));
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", result.asString());

        // 1.) current java user.dir is {fastr_home} and it contains a dir 'bin' containig a file R
        assertTrue("no bin/R file in the {fastr_home}, try ot use some another file for this test then.", new File(wd + "bin/R").exists());
        // 2.) lets create a test folder containg a path 'bin/R/test';
        // note that {fastr_home}/bin/R points to a file, while {testdir}/bin/R is a directory
        new File(wd + "com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/builtins/data/bin/R").mkdirs();
        testFile = new File(wd + "com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/builtins/data/bin/R/test");
        if (!testFile.exists()) {
            testFile.createNewFile();
        }
        // 3.) and setwd to {testdir} and try to access that bin/R/test
        result = context.eval(getSource("setwd('" + wd + testWDDir + "'); .Call(tools:::C_Rmd5, 'bin/R/test');"));
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", result.asString());
    }

    private static String dirPathWD = "com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/simple/data";

    @Test
    public void testSetWD() {
        String wd = gewtWD();

        try {
            context.eval(getSource("{ setwd() }"));
        } catch (PolyglotException pe) {
            assertEquals(pe.getMessage(), "Error in setwd() : argument \"dir\" is missing, with no default");
        }

        try {
            context.eval(getSource("{ setwd('') }"));
        } catch (PolyglotException pe) {
            assertEquals(pe.getMessage(), "Error in setwd(\"\") : cannot change working directory");
        }

        Value result = context.eval(getSource("{ setwd('" + wd + dirPathWD + "'); getwd(); }"));
        assertEquals(wd + dirPathWD, result.asString());

        result = context.eval(getSource("{ setwd('" + wd + dirPathWD + "'); setwd('tree1'); getwd(); }"));
        assertEquals(wd + dirPathWD + "/tree1", result.asString());

        try {
            context.eval(getSource("{ setwd('" + wd + dirPathWD + "'); setwd('does-not-exist'); getwd(); }"));
        } catch (PolyglotException pe) {
            assertEquals(pe.getMessage(), "Error in setwd(\"does-not-exist\") : cannot change working directory");
        }

        result = context.eval(getSource("{ setwd('" + wd + dirPathWD + "/tree1/..'); getwd(); }"));
        assertEquals(wd + dirPathWD, result.asString());
    }

    private static Source getSource(String srcTxt) {
        return Source.newBuilder("R", srcTxt, "<testExternalPointerMR>").internal(true).buildLiteral();
    }

    private static String gewtWD() {
        return System.getProperty("user.dir") + "/";
    }

}
