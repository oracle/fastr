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

public class TestBuiltin_substring extends TestBase {

    @Test
    public void testsubstring1() {
        assertEval("argv <- structure(list(text = c('NA', NA, 'BANANA'), first = 1,     last = 1), .Names = c('text', 'first', 'last'));do.call('substring', argv)");
    }

    @Test
    public void testsubstring2() {
        assertEval("argv <- structure(list(text = 'abcdef', first = 1:6, last = 1:6),     .Names = c('text', 'first', 'last'));do.call('substring', argv)");
    }

    @Test
    public void testSubstring() {
        assertEval("{ substring(\"123456\", first=2, last=4) }");
        assertEval("{ substring(\"123456\", first=2.8, last=4) }");
        assertEval("{ substring(c(\"hello\", \"bye\"), first=c(1,2,3), last=4) }");
        assertEval("{ substring(\"fastr\", first=NA, last=2) }");
    }
}
