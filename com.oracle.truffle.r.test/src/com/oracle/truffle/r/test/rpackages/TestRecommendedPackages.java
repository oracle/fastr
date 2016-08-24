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

import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RVersionNumber;
import com.oracle.truffle.r.test.TestBase;

/**
 * Test the installation of the "recommended" packages that come with GnuR. N.B. There are no
 * specific tests beyond install/load as that is handled separately in the package testing
 * framework. We are primarily concerned with detecting installation regressions.
 */
public class TestRecommendedPackages extends TestRPackages {
    private static final String[] OK_PACKAGES = new String[]{"MASS", "boot", "class", "cluster", "codetools", "lattice", "nnet", "spatial", "survival", "KernSmooth", "Matrix", "foreign", "nlme",
                    "rpart"};
    @SuppressWarnings("unused") private static final String[] PROBLEM_PACKAGES = new String[]{};

    private static Path getRecommendedPath() {
        return Paths.get(REnvVars.rHome(), "com.oracle.truffle.r.native", "gnur", RVersionNumber.R_HYPHEN_FULL, "src", "library", "Recommended");
    }

    @BeforeClass
    public static void setupInstallMyTestPackages() {
        setupInstallTestPackages(OK_PACKAGES, new Resolver() {
            @Override
            Path getPath(String p) {
                return getRecommendedPath().resolve(p + ".tgz");
            }
        });
    }

    @AfterClass
    public static void tearDownUninstallMyTestPackages() {
        tearDownUninstallTestPackages(OK_PACKAGES);
    }

    @Test
    public void testLoad() {
        assertEval(Ignored.OutputFormatting, Context.NonShared, Context.LongTimeout,
                        TestBase.template("{ library(%1, lib.loc = \"%0\"); detach(\"package:%1\"); }", new String[]{TestRPackages.libLoc()}, OK_PACKAGES));
    }
}
