/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_Sysgetenv extends TestBase {

    @Test
    public void testSysgetenv1() {
        assertEval("argv <- list('EDITOR', ''); .Internal(Sys.getenv(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testSysgetenv2() {
        assertEval("argv <- list('SWEAVE_OPTIONS', NA_character_); .Internal(Sys.getenv(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testEnvVars() {
        assertEval("{ Sys.setenv(\"a\") } ");
        assertEval("{ Sys.setenv(FASTR_A=\"a\"); Sys.getenv(\"FASTR_A\"); } ");
        assertEval("{ Sys.setenv(FASTR_A=\"a\", FASTR_B=\"b\"); Sys.getenv(c(\"FASTR_A\", \"FASTR_B\"));  } ");
        assertEval("{ Sys.getenv(\"FASTR_A\") } ");
        assertEval("{ Sys.getenv(\"FASTR_A\", unset=\"UNSET\") } ");
    }
}
