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
