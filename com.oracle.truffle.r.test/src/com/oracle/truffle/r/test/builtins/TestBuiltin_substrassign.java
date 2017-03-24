/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_substrassign extends TestBase {

    @Test
    public void testsubstrassign1() {
        assertEval("argv <- list('(0,5]', 1L, 1L, '['); .Internal(`substr<-`(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testsubstrassign2() {
        assertEval("argv <- list(c('asfef', 'qwerty', 'yuiop[', 'b', 'stuff.blah.yech'), 2L, 1000000L, c('..', '+++')); .Internal(`substr<-`(argv[[1]], argv[[2]], argv[[3]], argv[[4]]))");
    }

    @Test
    public void testsubstrassign_1() {
        assertEval("argv <- structure(list(x = c('NA', NA, 'BANANA'), start = 1,     stop = 2, value = 'na'), .Names = c('x', 'start', 'stop',     'value'));do.call('substr<-', argv)");
    }

    @Test
    public void testsubstrassign_2() {
        assertEval("argv <- structure(list(x = 'abcde', start = NA, stop = 3, value = 'abc'),     .Names = c('x', 'start', 'stop', 'value'));do.call('substr<-', argv)");
    }

    @Test
    public void testsubstrassign() {
        assertEval("`substr<-`(c('asdfasdf', 'jfjfjf', 'ffff'), 2, 5, c('a', 'asdf'))");
        assertEval("`substr<-`(c('asdfasdf', 'jfjfjf', 'ffff'), c(1,10,-6), 5, c('a', 'asdf'))");
        assertEval("`substr<-`(c('asdfasdf', 'jfjfjf', 'ffff'), 2, c(5,15,-6), c('a', 'asdf'))");
        assertEval("`substr<-`(c('asdfasdf', 'jfjfjf', 'ffff'), 2, 5, NULL)");
        assertEval("`substr<-`(c('asdfasdf', 'jfjfjf', 'ffff'), 2, 5, 1)");

        String[] values = new String[]{"-2L", "-1L", "0L", "1L", "2L"};
        assertEval(template("`substr<-`('ffff', %0, %1, 'xyz')", values, values));
    }
}
