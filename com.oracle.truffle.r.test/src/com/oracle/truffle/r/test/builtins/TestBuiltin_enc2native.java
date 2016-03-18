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
public class TestBuiltin_enc2native extends TestBase {

    @Test
    public void testenc2native1() {
        assertEval("argv <- list(character(0));enc2native(argv[[1]]);");
    }

    @Test
    public void testenc2native3() {
        assertEval("argv <- list(structure(character(0), .Names = character(0)));enc2native(argv[[1]]);");
    }

    @Test
    public void testenc2native4() {
        assertEval("argv <- list('José Pinheiro [aut] (S version)');enc2native(argv[[1]]);");
    }
}
