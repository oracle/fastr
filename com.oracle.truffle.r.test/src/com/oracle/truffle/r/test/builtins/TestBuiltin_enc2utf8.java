/*
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates
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
