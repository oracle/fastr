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
import org.junit.BeforeClass;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

/**
 * Tests related to the loading, etc. of R packages.
 */
public class TestRFFIPackage extends TestRPackages {

    private static final String[] TEST_PACKAGES = new String[]{"testrffi"};

    @BeforeClass
    public static void setupInstallMyTestPackages() {
        setupInstallTestPackages(TEST_PACKAGES);
    }

    @AfterClass
    public static void tearDownUninstallMyTestPackages() {
        tearDownUninstallTestPackages(TEST_PACKAGES);
    }

    @Test
    public void testLoadTestRFFISimple() {
        assertEval(TestBase.template(
                        "{ library(\"testrffi\", lib.loc = \"%0\"); r1 <- rffi.addInt(2L, 3L); r2 <- rffi.addDouble(2, 3); r3 <- rffi.populateIntVector(5); " +
                                        "r4 <- rffi.populateLogicalVector(5); detach(\"package:testrffi\"); list(r1, r2, r3, r4) }",
                        new String[]{TestRPackages.libLoc()}));
        assertEval(TestBase.template(
                        "{ library(\"testrffi\", lib.loc = \"%0\"); r1 <- rffi.mkStringFromChar(); r2 <- rffi.mkStringFromBytes(); r3 <- rffi.null(); " +
                                        "r4 <-rffi.isRString(character(0)); detach(\"package:testrffi\"); list(r1, r2, r3, r4) }",
                        new String[]{TestRPackages.libLoc()}));
        assertEval(TestBase.template(
                        "{ library(\"testrffi\", lib.loc = \"%0\"); c3 <- c(1L,2L,3L); r1 <- rffi.iterate_iarray(c3); r2 <- rffi.iterate_iptr(c3); " +
                                        "detach(\"package:testrffi\"); list(r1, r2) }",
                        new String[]{TestRPackages.libLoc()}));
    }

    @Test
    public void testLoadTestRFFIDotC() {
        assertEval(TestBase.template(
                        "{ library(\"testrffi\", lib.loc = \"%0\"); r1 <- rffi.dotCModifiedArguments(c(0,1,2,3)); r1 }",
                        new String[]{TestRPackages.libLoc()}));
    }

    @Test
    public void testLoadTestRFFIExternal() {
        assertEval(TestBase.template(
                        "{ library(\"testrffi\", lib.loc = \"%0\"); r1 <- rffi.dotExternalAccessArgs(1L, 3, c(1,2,3), c('a', 'b'), 'b', TRUE, as.raw(12)); detach(\"package:testrffi\"); r1 }",
                        new String[]{TestRPackages.libLoc()}));
    }

    @Test
    public void testLoadTestRFFIExternalWithNames() {
        assertEval(TestBase.template(
                        "{ library(\"testrffi\", lib.loc = \"%0\"); r1 <- rffi.dotExternalAccessArgs(x=1L, 3, c(1,2,3), y=c('a', 'b'), 'b', TRUE, as.raw(12)); detach(\"package:testrffi\"); r1 }",
                        new String[]{TestRPackages.libLoc()}));
    }

    @Test
    public void testLoadTestRFFIManyArgs() {
        assertEval(TestBase.template(
                        "{ library(\"testrffi\", lib.loc = \"%0\"); r1 <- rffi.invoke12(); detach(\"package:testrffi\"); r1 }",
                        new String[]{TestRPackages.libLoc()}));
    }

}
