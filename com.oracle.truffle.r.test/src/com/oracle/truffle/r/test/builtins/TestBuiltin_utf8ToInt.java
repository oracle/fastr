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
public class TestBuiltin_utf8ToInt extends TestBase {

    @Test
    public void testutf8ToInt1() {
        assertEval(Ignored.Unknown, "argv <- list('lasy'); .Internal(utf8ToInt(argv[[1]]))");
    }

    @Test
    public void testutf8ToInt3() {
        assertEval(Ignored.Unknown, "argv <- structure(list(x = NA_character_), .Names = 'x');do.call('utf8ToInt', argv)");
    }
}
