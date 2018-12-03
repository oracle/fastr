/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import com.oracle.truffle.r.runtime.ResourceHandlerFactory;

/**
 * Base class for all Java test suites (in the sense of JUnit Java files) that want to run R tests
 * stored in the file system as R sources. It is expected that R test source files will be stored in
 * the R sub-directory of the {@code com.oracle.truffle.r.test} project.
 *
 * The first line of the file may contain some configuration information (as an R comment). At this
 * point, two keywords are recognized - ContainsError and ContansWarning. Including any of them on
 * in the first line will cause appropriate execution method to be chosen (if both are present then
 * ContainsError has precedence).
 *
 * The R files are sourced, so any test results have to be explicitly printed.
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
        Map<String, String> rFiles = ResourceHandlerFactory.getHandler().getRFiles(this.getClass(), testDirName);
        for (Entry<String, String> entry : rFiles.entrySet()) {
            String entryName = entry.getKey();
            String entryValue = entry.getValue();
            explicitTestContext = entryName;
            String[] lines = entryValue.split("\n");
            TestTrait testTrait = getTestTrait(lines);
            try {
                Path dir = TestBase.createTestDir(testDirName);
                Path p = dir.resolve(Paths.get(entryName).getFileName());
                Files.write(p, entryValue.getBytes());
                if (testTrait == null) {
                    assertEval(30000, TestBase.template("{ source(\"%0\") }", new String[]{p.toString()}));
                } else {
                    assertEval(30000, testTrait, TestBase.template("{ source(\"%0\") }", new String[]{p.toString()}));
                }
            } catch (IOException ex) {
                assert false;
            }
            explicitTestContext = null;
        }
    }

    private static TestTrait getTestTrait(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i].trim();
            if (!l.startsWith("#")) {
                return null;
            }
            // check the first line for configuration options
            if (l.contains("IgnoreErrorContext")) {
                return Output.IgnoreErrorContext;
            } else if (l.contains("IgnoreWarningContext")) {
                return Output.IgnoreWarningContext;
            } else if (l.contains("IgnoreWarningMessage")) {
                return Output.IgnoreWarningMessage;
            } else if (l.contains("Ignored")) {
                for (Ignored ignoredType : Ignored.values()) {
                    if (l.contains("Ignored." + ignoredType.name())) {
                        return ignoredType;
                    }
                }
                return Ignored.Unknown; // Retain old way for compatibility
            }
        }
        return null;
    }
}
