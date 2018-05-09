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

public class TestBuiltin_isnaassign_default extends TestBase {

    @Test
    public void testisnaassign_default1() {
        assertEval("argv <- structure(list(x = 9L, value = TRUE), .Names = c('x',     'value'));do.call('is.na<-.default', argv)");
    }

    @Test
    public void testisnaassign_default2() {
        assertEval("argv <- structure(list(x = structure(c('A', '3', 'C'), class = 'AsIs'),     value = 2), .Names = c('x', 'value'));do.call('is.na<-.default', argv)");
    }

    @Test
    public void testIsNAAssign() {
        assertEval("{ x <- c(0:4); is.na(x) <- c(2, 4); x }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); is.na(x)<-1; x }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); is.na(x)<-2; x }");
        assertEval("{ x<-factor(c(\"a\", \"b\", \"a\")); is.na(x)<-c(1, 3); x }");
    }
}
