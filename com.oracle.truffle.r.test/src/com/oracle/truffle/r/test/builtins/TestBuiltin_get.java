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
public class TestBuiltin_get extends TestBase {

    @Test
    public void testGet() {
        assertEval("{y<-function(){y<-2;get(\"y\",mode=\"integer\")};y();}");
        assertEval("{y<-function(){y<-2;get(\"y\",mode=\"closure\")};y();}");
        assertEval("{y<-function(){y<-2;get(\"y\",mode=\"integer\",inherits=FALSE);get(\"y\",mode=\"integer\",inherits=FALSE)};y();}");
        assertEval("{y<-function(){y<-2;get(\"y\",mode=\"double\")};y();}");
        assertEval("{y<-function(){y<-2;get(\"y\",mode=\"double\",inherits=FALSE)};y();}");
        assertEval("{ get(\"dummy\") }");
        assertEval("{ x <- 33 ; f <- function() { if (FALSE) { x <- 22  } ; get(\"x\", inherits = FALSE) } ; f() }");
        assertEval("{ x <- 33 ; f <- function() { get(\"x\", inherits = FALSE) } ; f() }");
        assertEval("{ get(\".Platform\", globalenv())$endian }");
        assertEval("{ get(\".Platform\")$endian }");
        assertEval("{y<-function(){y<-2;get(\"y\",mode=\"closure\",inherits=FALSE);};y();}");

        // behavior specific to RS4Object as environment:
        assertEval("setClass('foo', representation(x='numeric')); f <- new('foo'); e <- new.env(); e$x <- 1; attr(f, '.xData') <- e; get('x', envir=f)");
        assertEval("setClass('foo', representation(x='numeric')); f <- new('foo'); e <- new.env(); e$x <- 1; attr(f, '.Data') <- e; get('x', envir=f)");

        assertEval("{x <- 1L; get('x', mode='numeric'); }");
        assertEval("{x <- 1L; get('x', mode='double'); }");
    }
}
