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

public class TestBuiltin_rep_len extends TestBase {

    @Test
    public void testRepLen() {
        assertEval("{ rep_len(1, 2) }");
        assertEval("{ rep_len(3.14159, 3) }");
        assertEval("{ rep_len(\"RepeatTest\", 5) }");
        assertEval("{ rep_len(2+6i, 4) }");
        assertEval("{ rep_len(TRUE, 2) }");
        assertEval("{ x<-as.raw(16); rep_len(x, 2) }");

        assertEval("{ rep_len(1:4, 10) }");
        assertEval("{ rep_len(1:4, 3) }");
        assertEval("{ rep_len(1:4, 4) }");
        assertEval("{ rep_len(c(3.1415, 0.8), 1) }");
        assertEval("{ rep_len(c(2i+3, 4+2i), 4) }");
        assertEval("{ x<-as.raw(16); y<-as.raw(5); rep_len(c(x, y), 5) }");
        // cases with named arguments:
        assertEval("{rep_len(x=1:2, length.out=4)}");
        assertEval("{rep_len(length.out=4, x=1:2)}");
        assertEval("{rep_len(length.out=4, \"text\")}");
        assertEval("{rep_len(4, x=\"text\")}");
        assertEval("{x<-\"text\"; length.out<-4; rep_len(x=x, length.out=length.out)}");
        assertEval("{x<-\"text\"; length.out<-4; rep_len(length.out=length.out, x=x)}");
        // test string vector argument
        assertEval("{rep_len(c(\"abcd\", \"efg\"), 7)}");
        assertEval("{rep_len(c(\"abcd\", \"efg\"), 14)}");
        assertEval("{rep_len(c(\"abcd\", \"efg\"), 8)}");
        assertEval("{rep_len(c(\"abcd\", \"efg\"), 0)}");
        assertEval("{rep_len(c(\"abcd\", \"efg\"), 1)}");
        assertEval("{rep_len(c(\"abcd\", \"efg\"), 2)}");

        assertEval(Output.IgnoreErrorContext, "{ rep_len(function() 42, 7) }");
        assertEval("{ rep_len(7, \"7\") }");
        assertEval("{ rep_len(7, integer()) }");
        assertEval("{ rep_len(7, NA) }");
        assertEval("{ rep_len(7, NULL) }");
        assertEval("{ rep_len(7, c(7, 42)) }");

    }
}
