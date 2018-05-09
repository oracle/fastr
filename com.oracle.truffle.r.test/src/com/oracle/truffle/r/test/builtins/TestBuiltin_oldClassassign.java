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

public class TestBuiltin_oldClassassign extends TestBase {

    @Test
    public void testoldClassassign1() {
        assertEval("argv <- list(list(), NULL);`oldClass<-`(argv[[1]],argv[[2]]);");
    }

    @Test
    public void testoldClassassign2() {
        assertEval("argv <- list(NULL, NULL);`oldClass<-`(argv[[1]],argv[[2]]);");
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
