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
package com.oracle.truffle.r.test.rffi;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;
import com.oracle.truffle.r.test.rpackages.TestRPackages;

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

    /**
     * This is somewhat expensive to do per test, but the only alternative is to put all the
     * micro-tests in one big test. It might be that this should be switched to an R file-based
     * approach as the number of tests increase.
     */
    private void assertEvalWithLibWithSetup(String setup, String test) {
        assertEval(TestBase.template("{ library(\"testrffi\", lib.loc = \"%0\"); " + setup + "x <- " + test + "; detach(\"package:testrffi\", unload=T); x }", new String[]{TestRPackages.libLoc()}));
    }

    private void assertEvalWithLib(String test) {
        assertEvalWithLibWithSetup("", test);
    }

    @Test
    public void testRFFI1() {
        assertEvalWithLib("rffi.addInt(2L, 3L)");
    }

    @Test
    public void testRFFI2() {
        assertEvalWithLib("rffi.addDouble(2, 3)");
    }

    @Test
    public void testRFFI3() {
        assertEvalWithLib("rffi.populateIntVector(5)");
    }

    @Test
    public void testRFFI4() {
        assertEvalWithLib("rffi.populateLogicalVector(5)");
    }

    @Test
    public void testRFFI5() {
        assertEvalWithLib("rffi.mkStringFromChar()");
    }

    @Test
    public void testRFFI6() {
        assertEvalWithLib("rffi.mkStringFromBytes()");
    }

    @Test
    public void testRFFI7() {
        assertEvalWithLib("rffi.null()");
    }

    @Test
    public void testRFFI7E() {
        assertEvalWithLib("rffi.null.E()");
    }

    @Test
    public void testRFFI7C() {
        assertEvalWithLib("rffi.null.C()");
    }

    @Test
    public void testRFFI8() {
        assertEvalWithLib("rffi.isRString(character(0))");
    }

    @Test
    public void testRFFI9() {
        assertEvalWithLibWithSetup("a <- c(1L,2L,3L); ", "rffi.iterate_iarray(a)");
    }

    @Test
    public void testRFFI10() {
        assertEvalWithLibWithSetup("a <- c(1L,2L,3L); ", "rffi.iterate_iptr(a)");
    }

    @Test
    public void testRFFI11() {
        assertEvalWithLib("rffi.dotCModifiedArguments(c(0,1,2,3))");
    }

    @Test
    public void testRFFI12() {
        assertEvalWithLib("rffi.dotExternalAccessArgs(1L, 3, c(1,2,3), c('a', 'b'), 'b', TRUE, as.raw(12))");
    }

    @Test
    public void testRFFI13() {
        assertEvalWithLib("rffi.dotExternalAccessArgs(x=1L, 3, c(1,2,3), y=c('a', 'b'), 'b', TRUE, as.raw(12))");
    }

    @Test
    public void testRFFI14() {
        assertEvalWithLib("rffi.invoke12()");
    }

    @Test
    public void testRFFI15() {
        assertEvalWithLib("rffi.TYPEOF(3L)");
    }

    @Test
    public void testRFFI16() {
        assertEvalWithLib("rffi.isRString(\"hello\")");
    }

    @Test
    public void testRFFI17() {
        assertEvalWithLib("rffi.isRString(NULL)");
    }

    @Test
    public void testRFFI18() {
        assertEvalWithLib("rffi.interactive()");
    }

    @Test
    public void testRFFI19() {
        assertEvalWithLibWithSetup("x <- 1; ", "rffi.findvar(\"x\", globalenv())");
    }

    @Test
    public void testRFFI20() {
        assertEvalWithLibWithSetup("x <- \"12345\"; ", "rffi.char_length(x)");
    }
}
