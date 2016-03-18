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
public class TestBuiltin_filecreate extends TestBase {

    @Test
    public void testfilecreate1() {
        assertEval("argv <- list('codetools-manual.log', TRUE); .Internal(file.create(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testfilecreate2() {
        assertEval("argv <- list(character(0), TRUE); .Internal(file.create(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testfilecreate4() {
        assertEval("argv <- structure(list('foo1'), .Names = '');do.call('file.create', argv)");
    }
}
