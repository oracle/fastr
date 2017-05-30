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

public class TestBuiltin_oldClassassign extends TestBase {

    @Test
    public void testoldClassassign1() {
        assertEval("argv <- list(list(), NULL);`oldClass<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testoldClassassign2() {
        // FIXME UnsupportedSpecializationException: Unexpected values provided for oldClass<-: [],
        // []
        assertEval(Ignored.ImplementationError, "argv <- list(NULL, NULL);`oldClass<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testOldClassAssign() {
        assertEval("{ x<-1; oldClass(x)<-\"foo\"; class(x) }");

        assertEval("{ x<-1; oldClass(x)<-\"foo\"; oldClass(x) }");

        assertEval("{ x<-1; oldClass(x)<-\"integer\"; class(x) }");

        assertEval("{ x<-1; oldClass(x)<-\"integer\"; oldClass(x) }");

        assertEval("{ x<-1; oldClass(x)<-\"integer\"; class(x)<-\"integer\"; class(x) }");

        assertEval("{ x<-1; oldClass(x)<-\"integer\"; class(x)<-\"integer\"; oldClass(x) }");
    }
}
