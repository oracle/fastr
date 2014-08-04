/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 * 
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 * All rights reserved.
 */
package com.oracle.truffle.r.test.testrgen;

import org.junit.*;

import com.oracle.truffle.r.test.*;

public class TestrGenBuiltinbitwiseAnd extends TestBase {

    @Test
    public void testbitwiseAnd1() {
        assertEval("argv <- list(structure(c(420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 493L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 420L, 493L, 493L, 493L, 493L, 493L, 493L, 493L, 493L, 493L, 493L, 493L), class = \'octmode\'), structure(256L, class = \'octmode\')); .Internal(bitwiseAnd(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testbitwiseAnd2() {
        assertEval("argv <- list(structure(integer(0), class = \'hexmode\'), structure(integer(0), class = \'hexmode\')); .Internal(bitwiseAnd(argv[[1]], argv[[2]]))");
    }
}