/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

/**
 * Test the installation of the "recommended" packages that come with GnuR. N.B. There are no
 * specific tests beyond install/load as that is handled separately in the package testing
 * framework. We are primarily concerned with detecting installation regressions.
 *
 * N.B. The package 'tgz' files have been copied to the com.oracle.truffle.r.test project output
 * directory by the com.oracle.truffle.r.test.native Makefile. to allow them to be packaged into a
 * distribution and avoid any dependency on source paths.
 */
public class TestRecommendedPackages extends TestRPackages {
    private static final String[] DEFAULT_PACKAGES = new String[]{"MASS", "boot", "class", "cluster", "codetools", "lattice", "nnet", "spatial", "survival", "KernSmooth", "Matrix", "foreign", "nlme",
                    "rpart"};
    private static String[] packages = DEFAULT_PACKAGES;

    /**
     * Allows an external agent to ignore certain packages that are known to fail.
     */
    public static void ignorePackages(String[] ignoredPackages) {
        packages = new String[DEFAULT_PACKAGES.length - ignoredPackages.length];
        int k = 0;
        for (int i = 0; i < DEFAULT_PACKAGES.length; i++) {
            for (int j = 0; j < ignoredPackages.length; j++) {
                if (DEFAULT_PACKAGES[i].equals(ignoredPackages[j])) {
                    continue;
                }
                ignoredPackages[k++] = DEFAULT_PACKAGES[i];
            }
        }
    }

    @BeforeClass
    public static void setupInstallMyTestPackages() {
        setupInstallTestPackages(packages, new Resolver() {
            @Override
            Path getPath(String p) {
                return TestBase.getNativeProjectFile(Paths.get("packages")).resolve("recommended").resolve(p + ".tgz");
            }
        });
    }

    @AfterClass
    public static void tearDownUninstallMyTestPackages() {
        tearDownUninstallTestPackages(packages);
    }

    @Test
    public void testLoad() {
        // This is perhaps redundant as package installation tests whether the package will load.
        assertEval(Ignored.OutputFormatting, Context.NonShared, Context.LongTimeout,
                        TestBase.template("{ library(%1, lib.loc = \"%0\"); detach(\"package:%1\"); }", new String[]{TestRPackages.libLoc()}, packages));
    }
}
