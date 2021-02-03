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
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestBuiltin_pairlist extends TestBase {

    @Test
    public void testpairlist1() {
        assertEval("argv <- list();do.call('pairlist', argv)");
    }

    @Test
    public void testPairList() {
        assertEval("{ x<-7; y<-c(foo=42); z<-pairlist(x, y); list(z, typeof(z)) }");
    }

    @Test
    public void testPairListAssignment() {
        assertEval("{ l1 <- pairlist(a=1); l2 <- pairlist(a=42); l1['a'] <- l2 }");
        assertEval("{ l1 <- pairlist(a=1); l2 <- pairlist(a=42); l1['a'] <- l2['a'] }");
        assertEval("{ l1 <- pairlist(a=1, b=2); l2 <- pairlist(a=41, b=42); l1[c('a', 'b')] <- l2 }");
    }
}
