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

public class TestBuiltin_allequal extends TestBase {

    @Test
    public void testallequal1() {
        assertEval("argv <- structure(list(target = 0.261799387799149, current = 6.54498469497874),     .Names = c('target', 'current'));do.call('all.equal', argv)");
    }

    @Test
    public void testAllEqual() {
        assertEval("{ all.equal(data.frame(list(1,2,3)), data.frame(list(1,2,3))) }");
    }
}
