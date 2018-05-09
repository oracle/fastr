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
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_seq_len extends TestBase {
    @Test
    public void testseq5() {
        assertEval("argv <- list(FALSE);do.call('seq_len', argv);");
    }

    @Test
    public void testseq13() {
        assertEval("argv <- list(structure(3.14159265358979, class = structure('3.14159265358979', class = 'testit')));do.call('seq_len', argv);");
    }

    @Test
    public void testseq26() {
        assertEval("argv <- list(structure(2, .Names = 'Ind'));do.call('seq_len', argv);");
    }

    @Test
    public void testseq27() {
        assertEval(Output.IgnoreWarningContext, "argv <- list(c(2L, 2L));do.call('seq_len', argv)");
    }

    @Test
    public void testSeqLen() {
        assertEval("{ seq_len(10) }");
        assertEval("{ seq_len(5L) }");
        assertEval("{ seq_len(1:2) }");
        assertEval("{ seq_len(integer()) }");

        assertEval("{ seq_len(NA) }");
        assertEval("{ seq_len(-1) }");
        assertEval("{ seq_len(NULL) }");
        assertEval(Output.IgnoreWarningContext, "{ seq_len(\"foo\") }");
        // Note: tests conversion of empty integer sequence to empty double sequence
        assertEval("{ seq_len(0) + 1.1; }");
    }
}
