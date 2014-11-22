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
public class TestrGenBuiltinisOpen extends TestBase {

    @Test
    @Ignore
    public void testisOpen1() {
        assertEval("argv <- list(structure(2L, class = c(\'terminal\', \'connection\')), 0L); .Internal(isOpen(argv[[1]], argv[[2]]))");
    }

    @Test
    @Ignore
    public void testisOpen3() {
        assertEval("argv <- list(FALSE, 2L); .Internal(isOpen(argv[[1]], argv[[2]]))");
    }
}

