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

public class TestBuiltin_ifelse extends TestBase {

    @Test
    public void testifelse1() {
        assertEval("argv <- structure(list(test = c(TRUE, TRUE, FALSE, TRUE, FALSE),     yes = 'True', no = 'False'), .Names = c('test', 'yes', 'no'));do.call('ifelse', argv)");
    }

    @Test
    public void testIfelse() {
        assertEval("{ ifelse(TRUE,1,0) }");
        assertEval("{ ifelse(FALSE,1,0) }");
        assertEval("{ ifelse(NA,1,0) }");
    }
}
