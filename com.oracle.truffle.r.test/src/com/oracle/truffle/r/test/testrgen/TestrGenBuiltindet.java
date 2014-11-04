/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 * 
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.testrgen;

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check
public class TestrGenBuiltindet extends TestBase {

    @Test
    @Ignore
    public void testdet1() {
        assertEval("argv <- list(structure(c(FALSE, TRUE, TRUE, FALSE), .Dim = c(2L, 2L), .Dimnames = list(c(\'A\', \'B\'), c(\'A\', \'B\'))), TRUE); .Internal(det_ge_real(argv[[1]], argv[[2]]))");
    }

    @Test
    @Ignore
    public void testdet2() {
        assertEval("argv <- list(structure(c(TRUE, TRUE, TRUE, TRUE), .Dim = c(2L, 2L), .Dimnames = list(c(\'A\', \'B\'), c(\'A\', \'B\'))), TRUE); .Internal(det_ge_real(argv[[1]], argv[[2]]))");
    }

    @Test
    @Ignore
    public void testdet3() {
        assertEval("argv <- list(structure(c(2, 1, 1, 2), .Dim = c(2L, 2L), .Dimnames = list(c(\'A\', \'B\'), c(\'A\', \'B\'))), TRUE); .Internal(det_ge_real(argv[[1]], argv[[2]]))");
    }
}
