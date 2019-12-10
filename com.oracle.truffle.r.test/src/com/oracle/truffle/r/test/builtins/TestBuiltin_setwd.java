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

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;
import java.io.File;
import java.io.IOException;
import static org.junit.Assert.assertTrue;

public class TestBuiltin_setwd extends TestBase {

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
        assertEvalOneShot("wd <- getwd(); setwd('" + wd + testWDDir + "'); .Call(tools:::C_Rmd5, 'README.md/test'); setwd(wd)");

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
        assertEvalOneShot("wd <- getwd(); setwd('" + wd + testWDDir + "'); .Call(tools:::C_Rmd5, 'bin/R/test'); setwd(wd)");
    }

    private static String dirPathWD = "com.oracle.truffle.r.test/src/com/oracle/truffle/r/test/simple/data";

    @Test
    public void testSetWD() {
        String wd = gewtWD();
        assertEvalOneShot("{ wd <- getwd(); setwd(); setwd(wd) }");
        assertEvalOneShot("{ wd <- getwd(); setwd(''); setwd(wd)}");
        assertEvalOneShot("{ wd <- getwd(); setwd('" + wd + dirPathWD + "'); print(getwd()); setwd(wd)}");
        assertEvalOneShot("{ wd <- getwd(); setwd('" + wd + dirPathWD + "'); setwd('tree1'); print(getwd()); setwd(wd) }");
        assertEvalOneShot("{ wd <- getwd(); tryCatch({setwd('" + wd + dirPathWD + "'); setwd('does-not-exist') }, error=function(e) print(e$message)); setwd(wd) }");
        assertEvalOneShot("{ wd <- getwd(); setwd('" + wd + dirPathWD + "/tree1/..'); print(getwd()); setwd(wd) }");

    }

    private static String gewtWD() {
        return System.getProperty("user.dir") + "/";
    }

}
