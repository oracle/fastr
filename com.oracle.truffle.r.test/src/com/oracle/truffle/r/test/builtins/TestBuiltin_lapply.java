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

public class TestBuiltin_lapply extends TestBase {

    @Test
    public void testLapply() {
        assertEval("{ lapply(1:3, function(x) { 2*x }) }");
        assertEval("{ lapply(1:3, function(x,y) { x*y }, 2) }");
        assertEval("{ x<-c(1,3,4);attr(x,\"names\")<-c(\"a\",\"b\",\"c\");lapply(x, function(x,y) { as.character(x*y) }, 2) }");
        assertEval("{ f <- function() { lapply(c(X=\"a\",Y=\"b\"), function(x) { c(a=x) })  } ; f() }");
        assertEval("{ lapply(1:3, function(x,y,z) { as.character(x*y+z) }, 2,7) }");
        assertEval("{ f <- function(x) 2 * x ; lapply(1:3, f) }");
        assertEval("{ f <- function(x, y) x * y ; lapply(1:3, f, 2) }");
        assertEval("{ lapply(1:3, sum) }");
        assertEval("{ lapply(1:3, sum, 2) }");
        assertEval("{ x <- list(a=1:10, b=1:20) ; lapply(x, sum) }");
        assertEval("{ l <- list(list(1),list(2),list(3)); f <- function(a) { lapply(a, function(x) lapply(x, function(y) print(y))) }; f(l)}");
    }

    @Test
    public void testTapply() {
        assertEval("{ ind <- list(c(1, 2, 2), c(\"A\", \"A\", \"B\")) ; tapply(1:3, ind) }");
        assertEval("{ n <- 17 ; fac <- factor(rep(1:3, length = n), levels = 1:5) ; tapply(1:n, fac, sum) }");
        assertEval("{ ind <- list(c(1, 2, 2), c(\"A\", \"A\", \"B\")) ; tapply(1:3, ind, sum) }");
    }

    @Test
    public void testSapply() {
        assertEval("{ f <- function() { sapply(1:3,function(x){x*2L}) }; f() + f() }");
        assertEval("{ f <- function() { sapply(c(1,2,3),function(x){x*2}) }; f() + f() }");

        assertEval("{ h <- new.env() ; assign(\"a\",1,h) ; assign(\"b\",2,h) ; sa <- sapply(ls(h), function(k) get(k,h,inherits=FALSE)) ; names(sa) }");

        assertEval("{ sapply(1:3, function(x) { if (x==1) { list(1) } else if (x==2) { list(NULL) } else { list() } }) }");
        assertEval("{ f<-function(g) { sapply(1:3, g) } ; f(function(x) { x*2 }) }");
        assertEval("{ f<-function() { x<-2 ; sapply(1, function(i) { x }) } ; f() }");

        assertEval("{ sapply(1:3,function(x){x*2L}) }");
        assertEval("{ sapply(c(1,2,3),function(x){x*2}) }");
        assertEval("{ sapply(1:3, length) }");
        assertEval("{ f<-length; sapply(1:3, f) }");
        assertEval("{ sapply(list(1,2,3),function(x){x*2}) }");

        assertEval("{ sapply(1:3, function(x) { if (x==1) { 1 } else if (x==2) { integer() } else { TRUE } }) }");

        assertEval("{ f<-function(g) { sapply(1:3, g) } ; f(function(x) { x*2 }) ; f(function(x) { TRUE }) }");
        assertEval("{ sapply(1:2, function(i) { if (i==1) { as.raw(0) } else { 5+10i } }) }");
        assertEval("{ sapply(1:2, function(i) { if (i==1) { as.raw(0) } else { as.raw(10) } }) }");
        assertEval("{ sapply(1:2, function(i) { if (i==1) { as.raw(0) } else { \"hello\" }} ) } ");
        assertEval("{ sapply(1:3, function(x) { if (x==1) { list(1) } else if (x==2) { list(NULL) } else { list(2) } }) }");

        assertEval("{ sapply(1:3, `-`, 2) }");
        assertEval("{ sapply(1:3, \"-\", 2) }");

        // matrix support
        assertEval("{ sapply(1:3, function(i) { list(1,2) }) }");
        assertEval("{ sapply(1:3, function(i) { if (i < 3) { list(1,2) } else { c(11,12) } }) }");
        assertEval("{ sapply(1:3, function(i) { if (i < 3) { c(1+1i,2) } else { c(11,12) } }) }");

        // names
        assertEval("{ (sapply(1:3, function(i) { if (i < 3) { list(xxx=1) } else {list(zzz=2)} })) }");
        assertEval("{ (sapply(1:3, function(i) { list(xxx=1:i) } )) }");
        assertEval("{ sapply(1:3, function(i) { if (i < 3) { list(xxx=1) } else {list(2)} }) }");
        assertEval("{ (sapply(1:3, function(i) { if (i < 3) { c(xxx=1) } else {c(2)} })) }");
        assertEval("{ f <- function() { sapply(c(1,2), function(x) { c(a=x) })  } ; f() }");
        assertEval("{ f <- function() { sapply(c(X=1,Y=2), function(x) { c(a=x) })  } ; f() }");
        assertEval("{ f <- function() { sapply(c(\"a\",\"b\"), function(x) { c(a=x) })  } ; f() }");
        assertEval("{ f <- function() { sapply(c(X=\"a\",Y=\"b\"), function(x) { c(a=x) })  } ; f() }");
        assertEval("{ sapply(c(\"a\",\"b\",\"c\"), function(x) { x }) }");

        assertEval("{ f <- function(v) { sapply(1:3, function(k) v)}; f(1); f(2) }");
    }
}
