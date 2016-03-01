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

import org.junit.*;

import com.oracle.truffle.r.test.*;

// Checkstyle: stop line length check

public class TestBuiltin_setdiff extends TestBase {

    @Test
    public void testsetdiff1() {
        assertEval("argv <- structure(list(x = c('bibtex', 'tex'), y = '.svn'), .Names = c('x',     'y'));do.call('setdiff', argv)");
    }

    @Test
    public void setdiff() {
        assertEval("x <- c('a', 'b', 'x'); y <- c('a', 'y', 'z', 'x'); setdiff(x, y)");
    }

}
