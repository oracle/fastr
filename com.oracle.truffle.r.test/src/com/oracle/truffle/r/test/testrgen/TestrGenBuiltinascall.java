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

public class TestrGenBuiltinascall extends TestBase {

    @Test
    @Ignore
    public void testascall1() {
        assertEval("argv <- list(list(quote(quote), c(0.568, 1.432, -1.08, 1.08)));as.call(argv[[1]]);");
    }

    @Test
    @Ignore
    public void testascall2() {
        assertEval("argv <- list(list(quote(quote), FALSE));as.call(argv[[1]]);");
    }

    @Test
    @Ignore
    public void testascall3() {
        assertEval("argv <- list(list(quote(quote), list(NULL, c(\'time\', \'status\'))));as.call(argv[[1]]);");
    }

    @Test
    @Ignore
    public void testascall4() {
        assertEval("argv <- list(structure(expression(data.frame, check.names = TRUE, stringsAsFactors = TRUE), .Names = c(\'\', \'check.names\', \'stringsAsFactors\')));as.call(argv[[1]]);");
    }

    @Test
    @Ignore
    public void testascall5() {
        assertEval("argv <- list(list(quote(quote), 80L));as.call(argv[[1]]);");
    }

    @Test
    @Ignore
    public void testascall6() {
        assertEval("argv <- list(list(quote(quote), NA));as.call(argv[[1]]);");
    }
}
