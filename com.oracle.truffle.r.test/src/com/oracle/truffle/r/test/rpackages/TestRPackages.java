/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

import com.oracle.truffle.r.runtime.FastROptions;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.test.TestBase;

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
    protected static final class PackagePaths {
        /**
         * The path containing the package distributions as tar files. These are built in the
         * {@code com.oracle.truffle.r.test.native} project in the {@code packages} directory.
         */
        private final Path rpackagesDists;
        /**
         * The path to where the package will be installed (R_LIBS_USER).
         */
        protected final Path rpackagesLibs;

        private PackagePaths() {
            Path rpackages = Paths.get(REnvVars.rHome(), "com.oracle.truffle.r.test", "rpackages");
            rpackagesLibs = TestBase.relativize(rpackages.resolve("testrlibs_user"));
            // Empty it in case of failure that didn't clean up
            if (rpackagesLibs.toFile().exists()) {
                try {
                    Files.walkFileTree(rpackagesLibs, new SimpleFileVisitor<Path>() {

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                            if (e == null) {
                                Files.delete(dir);
                                return FileVisitResult.CONTINUE;
                            } else {
                                // directory iteration failed
                                throw e;
                            }
                        }

                    });
                } catch (IOException e) {
                    assert false;
                }
            }

            rpackagesLibs.toFile().mkdirs();
            rpackagesDists = Paths.get(REnvVars.rHome(), "com.oracle.truffle.r.test.native", "packages");
        }

        protected boolean installPackage(String packageName) {
            Path packagePath = rpackagesDists.resolve(packageName).resolve("lib").resolve(packageName + ".tar");
            String[] cmds = new String[4];
            if (generatingExpected()) {
                // use GnuR
                cmds[0] = "R";
            } else {
                // use FastR
                cmds[0] = FileSystems.getDefault().getPath(REnvVars.rHome(), "bin", "R").toString();
            }
            cmds[1] = "CMD";
            cmds[2] = "INSTALL";
            cmds[cmds.length - 1] = packagePath.toString();
            ProcessBuilder pb = new ProcessBuilder(cmds);
            Map<String, String> env = pb.environment();
            env.put("R_LIBS_USER", rpackagesLibs.toString());
            if (!generatingExpected()) {
                env.put("R_INSTALL_TAR", RContext.getInstance().stateREnvVars.get("TAR"));
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

        protected boolean uninstallPackage(String packageName) {
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

    protected static final PackagePaths packagePaths = new PackagePaths();

}
