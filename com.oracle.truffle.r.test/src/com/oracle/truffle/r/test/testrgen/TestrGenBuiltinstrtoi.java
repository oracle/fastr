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

public class TestrGenBuiltinstrtoi extends TestBase {

    @Test
    @Ignore
    public void teststrtoi1() {
        assertEval("argv <- list(\'0777\', 8L); .Internal(strtoi(argv[[1]], argv[[2]]))");
    }

    @Test
    @Ignore
    public void teststrtoi2() {
        assertEval("argv <- list(\'700\', 8L); .Internal(strtoi(argv[[1]], argv[[2]]))");
    }

    @Test
    @Ignore
    public void teststrtoi3() {
        assertEval("argv <- list(c(\'0xff\', \'077\', \'123\'), 0L); .Internal(strtoi(argv[[1]], argv[[2]]))");
    }

    @Test
    @Ignore
    public void teststrtoi4() {
        assertEval("argv <- list(\'1.3\', 16L); .Internal(strtoi(argv[[1]], argv[[2]]))");
    }

    @Test
    @Ignore
    public void teststrtoi5() {
        assertEval("argv <- list(character(0), 8L); .Internal(strtoi(argv[[1]], argv[[2]]))");
    }
}
