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

import com.oracle.truffle.r.runtime.REnvVars;

/*
 *  Base class for all Java test suites (in the sense of JUnit Java files)
 *  that want to run R tests stored in the file system as R sources.
 *  It is expected that R test source files will be stored in the R sub-directory
 *  of the directory containing a given JUnit Java file.
 */

public class TestRBase extends TestBase {

    /*
     * Each test suite that wants to run R tests from R sources needs to override this method.
     */
    protected String getTestDir() {
        return null;
    }

    /*
     * This method needs to be called by each test suite to run the actual tests.
     */
    protected void runRSourceTests() {
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
            try {
                BufferedReader bf = new BufferedReader(new FileReader(files[i]));
                StringBuffer sb = new StringBuffer("{");
                TestTrait testTrait = null;
                while (true) {
                    String l = bf.readLine();
                    if (l != null) {
                        l = l.trim();
                        if (l.startsWith("#")) {
                            if (sb.length() == 0) {
                                // check only when (most likely) on the first line
                                if (l.contains("ContainsError")) {
                                    testTrait = Output.ContainsError;
                                } else if (l.contains("ContainsWarning")) {
                                    testTrait = Output.ContainsWarning;
                                }
                                // TODO: add other options if need be
                            }
                        } else {
                            sb.append(l);
                            if (l.length() > 0 && !l.endsWith(";")) {
                                sb.append(";");
                            }
                        }
                    } else {
                        break;
                    }
                }
                sb.append("}");
                bf.close();
                if (testTrait == null) {
                    assertEval(sb.toString());
                } else {
                    assertEval(testTrait, sb.toString());
                }
            } catch (IOException x) {
                Assert.fail("error reading: " + files[i].getPath() + ": " + x);
            }
        }
    }

}
