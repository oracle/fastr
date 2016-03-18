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

public class TestBuiltin_proptable extends TestBase {

    @Test
    public void testproptable1() {
        assertEval("argv <- structure(list(x = structure(c(15L, 37L, 30L, 18L, 12L,     30L, 64L, 44L), .Dim = c(4L, 2L), .Dimnames = structure(list(Evaluation = c('very good',     'good', 'bad', 'very bad'), Location = c('city centre', 'suburbs')),     .Names = c('Evaluation', 'Location'))), margin = 2), .Names = c('x',     'margin'));"
                        + "do.call('prop.table', argv)");
    }
}
