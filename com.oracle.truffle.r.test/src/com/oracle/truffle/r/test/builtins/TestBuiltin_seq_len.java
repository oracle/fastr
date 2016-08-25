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

public class TestBuiltin_seq_len extends TestBase {

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
    }
}
