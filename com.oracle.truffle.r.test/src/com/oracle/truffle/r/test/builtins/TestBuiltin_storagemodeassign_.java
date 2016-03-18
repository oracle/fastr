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

public class TestBuiltin_storagemodeassign_ extends TestBase {

    @Test
    public void teststoragemodeassign_1() {
        assertEval("argv <- structure(list(structure(c(0, 1, 2), .Dim = c(3L, 1L)),     value = 'integer'), .Names = c('', 'value'));do.call('storage.mode<-', argv)");
    }
}
