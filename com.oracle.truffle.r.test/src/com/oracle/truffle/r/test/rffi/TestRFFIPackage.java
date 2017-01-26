/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.test.TestTrait;
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
    private void assertEvalWithLibWithSetupAndTrait(TestTrait trait, String setup, String test) {
        String[] tests = TestBase.template("{ library(\"testrffi\", lib.loc = \"%0\"); " + setup + "x <- " + test + "; detach(\"package:testrffi\", unload=T); x }",
                        new String[]{TestRPackages.libLoc()});
        if (trait == null) {
            assertEval(tests);
        } else {
            assertEval(trait, tests);
        }
    }

    private void assertEvalWithLib(String test) {
        assertEvalWithLibWithSetupAndTrait(null, "", test);
    }

    private void assertEvalWithLibWithSetup(String setup, String test) {
        assertEvalWithLibWithSetupAndTrait(null, setup, test);
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

    private static final String[] AS_VALUES = new String[]{"1L", "2", "2.2", "T", "integer()", "numeric()", "logical()", "character()", "c(5,6)", "c(2.3, 3.4)", "c(T, F)",
                    "as.symbol(\"sym\")", "list()"};

    private static final String[] AS_FUNS = new String[]{"Char", "Integer", "Real", "Logical"};

    @Test
    public void testAsFunctions() {
        String[] asCalls = template("x <- append(x, rffi.as%0(%1)); ", AS_FUNS, AS_VALUES);
        assertEvalWithLibWithSetupAndTrait(Output.MayIgnoreWarningContext, "x <- list(); ", flatten(asCalls));
    }

    private static final String[] LIST_FUNS = new String[]{"CAR", "CDR"};

    private static final String[] LIST_VALUES = new String[]{"pairlist(1,2)", "pairlist(x=1L, y=2L)"};

    @Test
    public void testListFunctions() {
        String[] calls = template("x <- append(x, rffi.%0(%1)); ", LIST_FUNS, LIST_VALUES);
        assertEvalWithLibWithSetupAndTrait(Output.MayIgnoreErrorContext, "x <- list(); ", flatten(calls));

    }

    private static String flatten(String[] tests) {
        StringBuilder sb = new StringBuilder();
        for (String test : tests) {
            sb.append(test);
        }
        return sb.toString();
    }

    private static final String[] LENGTH_VALUES = new String[]{"1", "c(1,2,3)", "list(1,2,3)", "expression(1,2)"};

    // Checkstyle: stop method name check
    @Test
    public void TestLENGTH() {
        String[] calls = template("x <- append(x, rffi.LENGTH(%0)); ", LENGTH_VALUES);
        assertEvalWithLibWithSetupAndTrait(Output.MayIgnoreErrorContext, "x <- list(); ", flatten(calls));
    }

}
