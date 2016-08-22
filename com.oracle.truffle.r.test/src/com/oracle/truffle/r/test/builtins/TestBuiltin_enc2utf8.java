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
public class TestBuiltin_enc2utf8 extends TestBase {

    @Test
    public void testenc2utf81() {
        assertEval("argv <- list('Add Text to a Plot');enc2utf8(argv[[1]]);");
    }

    @Test
    public void testenc2utf82() {
        assertEval("argv <- list('Modes');enc2utf8(argv[[1]]);");
    }

    @Test
    public void testenc2utf83() {
        assertEval("argv <- list(c('', '(De)compress I/O Through Connections'));enc2utf8(argv[[1]]);");
    }

    @Test
    public void testenc2utf84() {
        assertEval("argv <- list(character(0));enc2utf8(argv[[1]]);");
    }

    @Test
    public void testenc2utf86() {
        assertEval("argv <- list(NA_character_);do.call('enc2utf8', argv)");
    }

    @Test
    public void testInvalidArguments() {
        // Note: GnuR has typo in the message
        assertEval(Output.IgnoreErrorMessage, "enc2utf8(42);");
    }
}
