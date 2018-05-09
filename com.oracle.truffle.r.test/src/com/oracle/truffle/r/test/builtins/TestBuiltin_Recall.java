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
public class TestBuiltin_Recall extends TestBase {

    @Test
    public void testRecall() {
        assertEval("{ f<-function(i) { if(i<=1) 1 else i*Recall(i-1) } ; f(10) }");
        assertEval("{ f<-function(i) { if(i<=1) 1 else i*Recall(i-1) } ; g <- f ; f <- sum ; g(10) }");
        assertEval("{ f<-function(i) { if (i==1) { 1 } else if (i==2) { 1 } else { Recall(i-1) + Recall(i-2) } } ; f(10) }");
        assertEval("{ Recall(10) }");
        // Recall with more then 1 argument
        assertEval("{ f <- function(tarDepth,curDepth) { if (tarDepth == curDepth) {curDepth} else {Recall(tarDepth,curDepth+1)}}; f(3,0) }");
    }
}
