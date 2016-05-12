/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.r.runtime.REnvVars;

/*
 *  Base class for all Java test suites (in the sense of JUnit Java files)
 *  that want to run R tests stored in the file system as R sources.
 *  It is expected that R test source files will be stored in the R sub-directory
 *  of a directory stored in com.oracle.truffle.r.test/src/com/oracle/truffle/r/test
 *
 *  The first line of the file may contain some configuration information (as an R comment).
 *  At this point, two keywords are recognized - ContainsError and ContansWarning. Including
 *  any of them on in the first line will cause appropriate execution method to be chosen
 *  (if both are present then ContainsError has precedence).
 *
 *  The R files are sourced, so any test results have to be explicitly printed.
 */

public class TestRBase extends TestBase {

    /*
     * Each test suite that wants to run R tests from R sources needs to override this method.
     */
    protected String getTestDir() {
        return null;
    }

    @Test
    public void runRSourceTests() {
        String testDirName = getTestDir();
        if (testDirName == null) {
            return;
        }
        Path testDirPath = Paths.get(REnvVars.rHome(), "com.oracle.truffle.r.test", "src", "com", "oracle", "truffle", "r", "test", testDirName, "R");
        if (!Files.exists(testDirPath) || !Files.isDirectory(testDirPath)) {
            return;
        }
        File testDir = testDirPath.toFile();
        File[] files = testDir.listFiles((dir, name) -> name.endsWith(".R"));
        if (files == null) {
            return;
        }
        for (int i = 0; i < files.length; i++) {
            explicitTestContext = testDirName + "/R/" + files[i].getName();
            try {
                BufferedReader bf = new BufferedReader(new FileReader(files[i]));
                TestTrait testTrait = null;
                String l = bf.readLine();
                if (l != null) {
                    l = l.trim();
                    if (l.startsWith("#")) {
                        // check the first line for configuration options
                        if (l.contains("ContainsError")) {
                            testTrait = Output.ContainsError;
                        } else if (l.contains("ContainsWarning")) {
                            testTrait = Output.ContainsWarning;
                        }
                    }
                }
                bf.close();
                String testFilePath = TestBase.relativize(testDirPath.resolve(files[i].getName())).toString();
                if (testTrait == null) {
                    assertEval(TestBase.template("{ source(\"%0\") }", new String[]{testFilePath}));
                } else {
                    assertEval(testTrait, TestBase.template("{ source(\"%0\") }", new String[]{testFilePath}));
                }
            } catch (IOException x) {
                Assert.fail("error reading: " + files[i].getPath() + ": " + x);
            } finally {
                explicitTestContext = null;
            }
        }
    }
}
