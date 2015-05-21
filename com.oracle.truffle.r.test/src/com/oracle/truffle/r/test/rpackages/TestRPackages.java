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
package com.oracle.truffle.r.test.rpackages;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

import org.junit.*;

import com.oracle.truffle.r.options.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.test.*;

/**
 * Tests related to the loading, etc. of R packages.
 */
public class TestRPackages extends TestBase {

    /**
     * Create {@link Path}s to needed folders. N.B. The same directory is used when generating
     * expected output with GnuR, and running FastR, to keep the {@code lib_loc} argument the same
     * in the test string. So the install is destructive, but ok as there is never a clash.
     *
     */
    private static final class PackagePaths {
        /**
         * The path containing the package distributions as tar files. These are built in the
         * {@code com.oracle.truffle.r.test.native} project in the {@code packages} directory.
         */
        private final Path rpackagesDists;
        /**
         * The path to where the package will be installed (R_LIBS_USER).
         */
        private final Path rpackagesLibs;

        private PackagePaths() {
            Path rpackages = Paths.get(REnvVars.rHome(), "com.oracle.truffle.r.test", "rpackages");
            rpackagesLibs = TestBase.relativize(rpackages.resolve("testrlibs_user"));
            if (!rpackagesLibs.toFile().exists()) {
                rpackagesLibs.toFile().mkdir();
            }
            rpackagesDists = Paths.get(REnvVars.rHome(), "com.oracle.truffle.r.test.native", "packages");
        }

        private boolean installPackage(String packageName) {
            Path packagePath = rpackagesDists.resolve(packageName).resolve("lib").resolve(packageName + ".tar");
            String[] cmds;
            if (generatingExpected()) {
                // use GnuR
                cmds = new String[4];
                cmds[0] = "R";
            } else {
                // use FastR
                cmds = new String[5];
                cmds[0] = FileSystems.getDefault().getPath(REnvVars.rHome(), "bin", "R").toString();
                // TODO remove --no-help limitation when markdown parser functioning
                cmds[3] = "--no-help";
            }
            cmds[1] = "CMD";
            cmds[2] = "INSTALL";
            cmds[cmds.length - 1] = packagePath.toString();
            ProcessBuilder pb = new ProcessBuilder(cmds);
            Map<String, String> env = pb.environment();
            env.put("R_LIBS_USER", rpackagesLibs.toString());
            if (!generatingExpected()) {
                env.put("R_INSTALL_TAR", "/usr/bin/tar");
            }
            try {
                if (FastROptions.debugMatches("TestRPackages")) {
                    pb.inheritIO();
                }
                Process install = pb.start();
                int rc = install.waitFor();
                if (rc == 0) {
                    return true;
                } else {
                    BufferedReader out = new BufferedReader(new InputStreamReader(install.getInputStream(), StandardCharsets.UTF_8));
                    BufferedReader err = new BufferedReader(new InputStreamReader(install.getErrorStream(), StandardCharsets.UTF_8));
                    try {
                        StringBuilder str = new StringBuilder();
                        String line;
                        while ((line = out.readLine()) != null) {
                            str.append(line).append('\n');
                        }
                        while ((line = err.readLine()) != null) {
                            str.append(line).append('\n');
                        }
                        System.out.println(str);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            } catch (Exception ex) {
                return false;
            }
        }

        private boolean uninstallPackage(String packageName) {
            Path packageDir = rpackagesLibs.resolve(packageName);
            try {
                deleteDir(packageDir);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

    }

    private static final PackagePaths packagePaths = new PackagePaths();

    private static final String[] TEST_PACKAGES = new String[]{"vanilla", "testrffi"};

    @BeforeClass
    public static void setupInstallTestPackages() {
        for (String p : TEST_PACKAGES) {
            if (!packagePaths.installPackage(p)) {
                throw new AssertionError();
            }
        }
    }

    @AfterClass
    public static void tearDownUninstallTestPackages() {
        for (String p : TEST_PACKAGES) {
            if (!packagePaths.uninstallPackage(p)) {
                throw new AssertionError();
            }
        }
    }

    @Test
    public void testLoadVanilla() {
        assertEval(TestBase.template("{ library(\"vanilla\", lib.loc = \"%0\"); r <- vanilla(); detach(\"package:vanilla\"); r }", new String[]{packagePaths.rpackagesLibs.toString()}));
    }

    @Test
    public void testLoadTestRFFI() {
        assertEval(TestBase.template(
                        "{ library(\"testrffi\", lib.loc = \"%0\"); r1 <- add_int(2L, 3L); r2 <- add_double(2, 3); v <- createIntVector(2); v[1] <- 1; v[2] <- 2; detach(\"package:testrffi\"); list(r1, r2, v) }",
                        new String[]{packagePaths.rpackagesLibs.toString()}));
    }

}
