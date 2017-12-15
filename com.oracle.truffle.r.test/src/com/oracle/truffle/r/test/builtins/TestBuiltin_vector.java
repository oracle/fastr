/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012-2014, Purdue University
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
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

        assertEval("{ vector(character()) }");
        assertEval("{ vector(c(\"numeric\", \"numeric\")) }");
        assertEval("{  vector(\"numeric\", integer()) }");
        assertEval("{  vector(\"numeric\", c(7, 42)) }");
    }

    @Test
    public void testVectorWithPairlist() {
        assertEval("vector('pairlist', 0)");
        assertEval("vector('pairlist', 3)");
    }

    @Test
    public void testVectorNASubscript() {
        assertEval("v <- as.integer(c(1, 2)); v[1]<-NA_integer_; v");
        assertEval("v <- c(1, 2); v[1]<-NA_real_; v");
        assertEval("v <- c('a', 'b'); v[1]<-NA_character_; v");
        assertEval("v <- c(1, 2); v[1]<-NA_integer_; v");

        assertEval("v <- as.integer(c(1, 2, 3, 4)); dim(v)<-c(2,2); v[1, 1]<-NA_integer_; v");
        assertEval("v <- c(1, 2, 3, 4); dim(v)<-c(2,2); v[1, 1]<-NA_real_; v");
        assertEval("v <- c('a', 'b', 'c', 'd'); dim(v)<-c(2,2); v[1, 1]<-NA_character_; v");
        assertEval("v <- c(1, 2, 3, 4); dim(v)<-c(2,2); v[1, 1]<-NA_integer_; v");
    }
}
