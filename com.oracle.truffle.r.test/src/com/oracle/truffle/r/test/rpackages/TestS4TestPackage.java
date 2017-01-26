/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
public class TestS4TestPackage extends TestRPackages {

    private static final String[] TEST_PACKAGES = new String[]{"tests4"};

    @BeforeClass
    public static void setupInstallMyTestPackages() {
        setupInstallTestPackages(TEST_PACKAGES);
    }

    @AfterClass
    public static void tearDownUninstallMyTestPackages() {
        tearDownUninstallTestPackages();
    }

    @Test
    public void testS4Load() {
        assertEval(TestBase.template("{ library(\"tests4\", lib.loc = \"%0\"); detach(\"package:tests4\"); unloadNamespace(\"tests4\") }",
                        new String[]{TestRPackages.libLoc()}));
    }

    @Test
    public void testS4Execute() {
        assertEval(TestBase.template(
                        "{ library(\"tests4\", lib.loc = \"%0\"); r<-print(tests4:::inspect.vehicle(new(\"Car\"), new(\"Inspector\"))); detach(\"package:tests4\"); unloadNamespace(\"tests4\"); r }",
                        new String[]{TestRPackages.libLoc()}));
        assertEval(TestBase.template(
                        "{ library(\"tests4\", lib.loc = \"%0\"); r<-print(tests4:::inspect.vehicle(new(\"Truck\"), new(\"Inspector\"))); detach(\"package:tests4\"); unloadNamespace(\"tests4\"); r }",
                        new String[]{TestRPackages.libLoc()}));
        assertEval(TestBase.template(
                        "{ library(\"tests4\", lib.loc = \"%0\"); r<-print(tests4:::inspect.vehicle(new(\"Car\"), new(\"StateInspector\"))); detach(\"package:tests4\"); unloadNamespace(\"tests4\"); r }",
                        new String[]{TestRPackages.libLoc()}));
        assertEval(TestBase.template(
                        "{ library(\"tests4\", lib.loc = \"%0\"); r<-print(tests4:::inspect.vehicle(new(\"Truck\"), new(\"StateInspector\"))); detach(\"package:tests4\"); unloadNamespace(\"tests4\"); r }",
                        new String[]{TestRPackages.libLoc()}));
    }
}
