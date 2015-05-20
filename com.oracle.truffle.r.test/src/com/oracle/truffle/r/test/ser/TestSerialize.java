/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.ser;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

import org.junit.*;

import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.test.*;

public class TestSerialize extends TestBase {

    private static final class OutputDir {
        private final Path outputDirPath;

        OutputDir() {
            Path rpackages = Paths.get(REnvVars.rHome(), "com.oracle.truffle.r.test");
            outputDirPath = TestBase.relativize(rpackages.resolve("ser.save"));
            if (!outputDirPath.toFile().exists()) {
                outputDirPath.toFile().mkdir();
            }
        }

        Path outputFile(String p) {
            return outputDirPath.resolve(p);
        }

    }

    private static Map<String, Path> paths = new HashMap<>();

    private static String[] wrap(Path p) {
        return new String[]{p.toString()};
    }

    private static String[] wrap(String p) {
        return new String[]{p};
    }

    @BeforeClass
    public static void init() {
        // collect the input files used by the input tests
        try {
            InputStream is = ResourceHandlerFactory.getHandler().getResourceAsStream(TestSerialize.class, "data");
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.endsWith(".rds") || line.endsWith(".bin")) {
                        String key = line.trim();
                        final String rResource = "data/" + key;
                        URL url = ResourceHandlerFactory.getHandler().getResource(TestSerialize.class, rResource);
                        paths.put(key, TestBase.relativize(Paths.get(url.getPath())));
                    }
                }
            }
        } catch (IOException ex) {
            Assert.fail("error loading serialization data for " + TestSerialize.class.getSimpleName() + " : " + ex);
        }
        // create a temporary directory for output tests
        outputDir = new OutputDir();
        paths.put(SAVE_RDS, outputDir.outputFile(SAVE_RDS));
    }

    @AfterClass
    public static void teardownTestDir() {
        if (!deleteDir(outputDir.outputDirPath)) {
            throw new AssertionError();
        }
    }

    private static OutputDir outputDir;

    @Test
    public void testVectors() {
        runUnserializeFromConn("vector1.rds");
        runUnserializeFromConn("list2.rds");
        checkDataFrame("dataframe1.rds");
        checkFactor("factor1.rds");
    }

    @Test
    public void readChar() {
        readCharTests("testchar.bin", new String[]{"c(3, 10, 3)", "c(1, 8, 3)", "c(4, 9, 3)"});
    }

    private static final String SAVE_RDS = "save.rds";
    private static final String DEPARSE = "c%% <- textConnection(\"c%%\", open=\"w\"); writeLines(deparse(f%%), c%%)";
    private static final String[] SAVE_READ = wrap("saveRDS(f1, \"save.rds\"); f2 <- readRDS(\"save.rds\")");
    private static final String[] IDENTICAL = wrap("identical(c1,c2)");
    // @formatter:off
    private static final String[] SAVE_FUNCTIONS = new String[]{
        "f1 <- function() { }",
        "f1 <- function(a,b) a + b",
        "f1 <- function(a) if (a) a else -a",
        "f1 <- function(x, i) x[i]", "f1 <- function(x, i) x$i",
        "f1 <- function(a,b,c) g(a,b,c)"
    };

    private static String[] deparse(String arg) {
        return wrap(DEPARSE.replace("%%", arg));
    }

    private static String[] composeFunctions(String[] functions) {
        ArrayList<String> list = new ArrayList<>();
        for (String function : functions) {
            String[] expand = TestBase.template("{%0; %1; %2; %3; %4 }", wrap(function), SAVE_READ, deparse("1"), deparse("2"), IDENTICAL);
            list.add(expand[0]);
        }
        String[] result = new String[list.size()];
        list.toArray(result);
        return result;
    }

    @Test
    public void testSerializeFunction() {
        assertEval(composeFunctions(SAVE_FUNCTIONS));
   }

    private void runUnserializeFromConn(String fileName) {
        assertEval(TestBase.template("{ print(.Internal(unserializeFromConn(gzfile(\"%0\"), NULL))) }", wrap(paths.get(fileName))));
    }

    private void checkDataFrame(String fileName) {
        assertEval(TestBase.template("{ x <- .Internal(unserializeFromConn(gzfile(\"%0\"), NULL)); is.data.frame(x) }", wrap(paths.get(fileName))));
    }

    private void checkFactor(String fileName) {
        assertEval(TestBase.template("{ x <- .Internal(unserializeFromConn(gzfile(\"%0\"), NULL)); is.factor(x) }", wrap(paths.get(fileName))));
    }

    private void readCharTests(String fileName, String[] nchars) {
        assertEval(TestBase.template("{ zz <- file(\"%0\", \"rb\"); nc<-%1; readChar(zz, nc) }", wrap(paths.get(fileName)), nchars));
    }

}
