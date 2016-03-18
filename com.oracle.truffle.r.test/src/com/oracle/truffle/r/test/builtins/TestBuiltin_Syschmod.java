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
public class TestBuiltin_Syschmod extends TestBase {

    @Test
    public void testSyschmod1() {
        assertEval(Ignored.SideEffects, "argv <- list(character(0), structure(integer(0), class = 'octmode'), TRUE); .Internal(Sys.chmod(argv[[1]], argv[[2]], argv[[3]]))");
    }
}
