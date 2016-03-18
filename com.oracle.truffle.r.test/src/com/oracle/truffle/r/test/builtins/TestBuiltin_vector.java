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
public class TestBuiltin_vector extends TestBase {

    @Test
    public void testvector1() {
        assertEval("argv <- list('integer', 0L); .Internal(vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testvector2() {
        assertEval("argv <- list('double', 17.1); .Internal(vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testvector3() {
        assertEval("argv <- list('list', 1L); .Internal(vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testvector4() {
        assertEval("argv <- list('logical', 15L); .Internal(vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testvector5() {
        assertEval("argv <- list('double', 2); .Internal(vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testvector9() {
        assertEval("argv <- list('raw', 0L); .Internal(vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testvector10() {
        assertEval("argv <- list('list', structure(1L, .Names = '\\\\c')); .Internal(vector(argv[[1]], argv[[2]]))");
    }

    @Test
    public void testvector11() {
        assertEval("argv <- structure(list(mode = 'complex', length = 7), .Names = c('mode',     'length'));do.call('vector', argv)");
    }

    @Test
    public void testVectorConstructor() {
        assertEval("{ vector() }");
        assertEval("{ vector(\"integer\") }");
        assertEval("{ vector(\"numeric\") }");
        assertEval("{ vector(\"numeric\", length=4) }");
        assertEval("{ vector(length=3) }");
        assertEval("{ x<-as.vector(3); y<-vector(length=x) }");
    }
}
