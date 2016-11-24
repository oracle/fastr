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
