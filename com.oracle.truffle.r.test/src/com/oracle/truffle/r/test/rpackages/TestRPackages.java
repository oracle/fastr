/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.*;
import java.util.*;

import org.junit.*;

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
     * Currently we are using GnuR to do the install of the FastR-compiled package. The install
     * environment is handled in the Makefile using environment variables set in
     * {@link #installPackage(String)}.
     */
    private class PackagePaths {
        private final Path rpackagesDists;
        private final Path rpackagesLibs;

        private PackagePaths() {
            Path cwd = Paths.get(System.getProperty("user.dir"));
            Path rpackages = Paths.get(REnvVars.rHome(), "com.oracle.truffle.r.test", "rpackages");
            rpackagesLibs = cwd.relativize(rpackages.resolve("testrlibs_user"));
            if (!rpackagesLibs.toFile().exists()) {
                rpackagesLibs.toFile().mkdir();
            }
            rpackagesDists = rpackages.resolve("distributions");
        }

        private boolean installPackage(String packageZip) {
            Path vanilla = rpackagesDists.resolve(packageZip);
            // install the package (using GnuR for now)
            ProcessBuilder pb = new ProcessBuilder("R", "CMD", "INSTALL", vanilla.toString());
            Map<String, String> env = pb.environment();
            env.put("R_LIBS_USER", rpackagesLibs.toString());
            String installKind = generatingExpected() ? "GNUR" : "FASTR";
            env.put("FASTR_INSTALL", installKind);
            env.put("FASTR_HOME", REnvVars.rHome());
            String javaHome = System.getenv("JAVA_HOME");
            // GnuR INSTALL sets JAVA_HOME to a 1.6 JRE
            env.put("FASTR_JAVA_HOME", javaHome);
            try {
                // Uncomment the following to actually see the INSTALL output (debugging)
                // pb.inheritIO();
                Process install = pb.start();
                int rc = install.waitFor();
                return rc == 0;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    @Test
    public void testLoadVanilla() {
        PackagePaths packagePaths = new PackagePaths();
        assertTrue(packagePaths.installPackage("vanilla_1.0.tar.gz"));
        assertTemplateEval(TestBase.template("{ library(\"vanilla\", lib.loc = \"%0\"); vanilla() }", new String[]{packagePaths.rpackagesLibs.toString()}));
    }

    @Test
    public void testLoadTestRFFI() {
        PackagePaths packagePaths = new PackagePaths();
        assertTrue(packagePaths.installPackage("testrffi_1.0.tar.gz"));
        assertTemplateEval(TestBase.template("{ library(\"testrffi\", lib.loc = \"%0\"); add_int(2L, 3L) }", new String[]{packagePaths.rpackagesLibs.toString()}));
        assertTemplateEval(TestBase.template("{ library(\"testrffi\", lib.loc = \"%0\"); add_double(2, 3) }", new String[]{packagePaths.rpackagesLibs.toString()}));
        assertTemplateEval(TestBase.template("{ library(\"testrffi\", lib.loc = \"%0\"); v <- createIntVector(2); v[1] <- 1; v[2] <- 2; v }", new String[]{packagePaths.rpackagesLibs.toString()}));
    }

}
