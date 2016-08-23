/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_isNamespaceEnv extends TestBase {

    @Test
    public void testisNamespaceEnv1() {
        assertEval("argv <- list(FALSE); .Internal(isNamespaceEnv(argv[[1]]))");
    }

    @Test
    public void testisNamespaceEnv2() {
        assertEval("argv <- list(structure(numeric(0), .Dim = c(0L, 0L))); .Internal(isNamespaceEnv(argv[[1]]))");
    }

    @Test
    public void testisNamespaceEnv3() {
        assertEval("argv <- list(structure(list(c0 = structure(integer(0), .Label = character(0), class = 'factor')), .Names = 'c0', row.names = character(0), class = 'data.frame')); .Internal(isNamespaceEnv(argv[[1]]))");
    }
}
