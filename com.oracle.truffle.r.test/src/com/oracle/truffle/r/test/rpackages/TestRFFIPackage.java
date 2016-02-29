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

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

/**
 * Tests related to the loading, etc. of R packages.
 */
public class TestRFFIPackage extends TestRPackages {

    private static final String[] TEST_PACKAGES = new String[]{"testrffi"};

    @Override
    public void beforeTest() {
        // TODO Auto-generated method stub
        super.beforeTest();
        setupInstallTestPackages(TEST_PACKAGES);
    }

    @Override
    public void afterTest() {
        // TODO Auto-generated method stub
        super.afterTest();
        tearDownUninstallTestPackages(TEST_PACKAGES);
    }

    @BeforeClass
    public static void setupInstallMyTestPackages() {
        // setupInstallTestPackages(TEST_PACKAGES);
    }

    @AfterClass
    public static void tearDownUninstallMyTestPackages() {
        // tearDownUninstallTestPackages(TEST_PACKAGES);
    }

    @Test
    public void testLoadTestRFFICall() {
        assertEval(TestBase.template("{ library(\"testrffi\", lib.loc = \"%0\"); r1 <- rffi.addInt(2L, 3L);  detach(\"package:testrffi\"); list(r1) }",
                        new String[]{packagePaths.rpackagesLibs.toString()}));
        assertEval(TestBase.template(
                        "{ library(\"testrffi\", lib.loc = \"%0\"); r1 <- rffi.addInt(2L, 3L); r2 <- rffi.addDouble(2, 3); v <- rffi.populateIntVector(5); v2 <- rffi.dotCModifiedArguments(c(0,1,2,3)); "
                                        +
                                        "detach(\"package:testrffi\"); list(r1, r2, v, v2) }",
                        new String[]{packagePaths.rpackagesLibs.toString()}));
    }

    @Test
    public void testLoadTestRFFIExternal() {
        assertEval(TestBase.template(
                        "{ library(\"testrffi\", lib.loc = \"%0\"); r1 <- rffi.dotExternalAccessArgs(1L, 3, c(1,2,3), c('a', 'b'), 'b', TRUE, as.raw(12)); detach(\"package:testrffi\"); list(r1) }",
                        new String[]{packagePaths.rpackagesLibs.toString()}));
    }

    @Test
    public void testLoadTestRFFIExternalWithNames() {
        assertEval(TestBase.template(
                        "{ library(\"testrffi\", lib.loc = \"%0\"); r1 <- rffi.dotExternalAccessArgs(x=1L, 3, c(1,2,3), y=c('a', 'b'), 'b', TRUE, as.raw(12)); detach(\"package:testrffi\"); list(r1) }",
                        new String[]{packagePaths.rpackagesLibs.toString()}));
    }
}
