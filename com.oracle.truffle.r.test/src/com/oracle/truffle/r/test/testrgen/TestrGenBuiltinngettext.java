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
public class TestrGenBuiltinngettext extends TestBase {

    @Test
    public void testngettext1() {
        assertEval("argv <- list(1L, '%s is not TRUE', '%s are not all TRUE', NULL); .Internal(ngettext(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testngettext2() {
        assertEval("argv <- list(2L, '%s is not TRUE', '%s are not all TRUE', NULL); .Internal(ngettext(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }
}
