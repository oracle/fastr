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
package com.oracle.truffle.r.test.library.base;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check

public class TestEnvironments extends TestBase {

    @Test
    public void testLookup() {
        assertEval("{ f <- function() { assign(\"x\", 1) ; x } ; f() }");
        assertEval("{ f <- function() { x <- 2 ; g <- function() { x <- 3 ; assign(\"x\", 1, inherits=FALSE) ; x } ; g() } ; f() }");
        assertEval("{ f <- function() { x <- 2 ; g <- function() { assign(\"x\", 1, inherits=FALSE) } ; g() ; x } ; f() }");
        assertEval("{ f <- function() { x <- 2 ; g <- function() { assign(\"x\", 1, inherits=TRUE) } ; g() ; x } ; f() }");
        assertEval("{ f <- function() {  g <- function() { assign(\"x\", 1, inherits=TRUE) } ; g() } ; f() ; x }");
        assertEval("{ x <- 3 ; g <- function() { x } ; f <- function() { assign(\"x\", 2) ; g() } ; f() }");
        assertEval("{ x <- 3 ; f <- function() { assign(\"x\", 2) ; g <- function() { x } ; g() } ; f() }");
        assertEval("{ h <- function() { x <- 3 ; g <- function() { x } ; f <- function() { assign(\"x\", 2) ; g() } ; f() }  ; h() }");
        assertEval("{ h <- function() { x <- 3  ; f <- function() { assign(\"x\", 2) ; g <- function() { x } ; g() } ; f() }  ; h() }");
        assertEval("{ x <- 3 ; h <- function() { g <- function() { x } ; f <- function() { assign(\"x\", 2, inherits=TRUE) } ; f() ; g() }  ; h() }");
        assertEval("{ x <- 3 ; h <- function(s) { if (s == 2) { assign(\"x\", 2) } ; x }  ; h(1) ; h(2) }");
        assertEval("{ x <- 3 ; h <- function(s) { y <- x ; if (s == 2) { assign(\"x\", 2) } ; c(y,x) }  ; c(h(1),h(2)) }");
        assertEval("{ g <- function() { x <- 2 ; f <- function() { x ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ g <- function() { f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ g <- function() { f <- function() { if (FALSE) { x } ; assign(\"x\", 1) ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ g <- function() { if (FALSE) { x <- 2 } ; f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ g <- function() { if (FALSE) { x <- 2 } ; f <- function() { if (FALSE) { x } ; assign(\"x\", 2) ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ h <- function() { g <- function() { if (FALSE) { x <- 2 } ; f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() } ; h() }");
        assertEval("{ h <- function() { x <- 3 ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() } ; h() }");
        assertEval("{ f <- function(z) { exists(\"z\") } ; f() }");
        assertEval("{ f <- function() { x <- 3 ; exists(\"x\", inherits=FALSE) } ; f() }");
        assertEval("{ f <- function() { z <- 3 ; exists(\"x\", inherits=FALSE) } ; f() }");
        assertEval("{ f <- function() { if (FALSE) { x <- 3 } ; exists(\"x\", inherits=FALSE) } ; f() }");
        assertEval("{ f <- function() { assign(\"x\", 2) ; exists(\"x\", inherits=FALSE) } ; f() }");
        assertEval("{ g <- function() { x <- 2 ; f <- function() { if (FALSE) { x <- 3 } ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ g <- function() { x <- 2 ; f <- function() { x <- 5 ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ g <- function() { f <- function() { assign(\"x\", 3) ; if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ g <- function() { f <- function() { assign(\"z\", 3) ; if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ h <- function() { assign(\"x\", 1) ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() } ; h() }");
        assertEval("{ h <- function() { assign(\"z\", 1) ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() } ; h() }");
        assertEval("{ h <- function() { x <- 3 ; g <- function() { f <- function() { if (FALSE) { x } ; exists(\"x\") }  ; f() } ; g() } ; h() }");

        assertEval("{ x <- 3 ; f <- function() { exists(\"x\") } ; f() }");
        assertEval("{ x <- 3 ; f <- function() { exists(\"x\", inherits=FALSE) } ; f() }");

        assertEval("{ x <- 2 ; y <- 3 ; rm(\"y\") ; ls() }");
        assertEval("{ x <- 2 ; rm(\"x\") ; get(\"x\") }");
        assertEval("{ get(\"x\") }");

        assertEval("{ f <- function() { assign(\"x\", 1) ; y <- 2 ; ls() } ; sort(f()) }");
        assertEval("{ f <- function() { x <- 1 ; y <- 2 ; ls() } ; sort(f()) }");
        assertEval("{ f <- function() { assign(\"x\", 1) ; y <- 2 ; if (FALSE) { z <- 3 } ; ls() } ; sort(f()) }");
        assertEval("{ f <- function() { if (FALSE) { x <- 1 } ; y <- 2 ; ls() } ; f() }");
        // the actual elements are formatted differently from GNU-R, also in different order
        assertEval("{ f <- function() { for (i in rev(1:10)) { assign(as.character(i), i) } ; ls() } ; length(f()) }");

        // lookup
        assertEval("{ x <- 3 ; f <- function() { assign(\"x\", 4) ; h <- function(s=1) { if (s==2) { x <- 5 } ; x } ; h() } ; f() }");
        assertEval("{ x <- 3 ; f <- function() { assign(\"x\", 4) ; g <- function() { assign(\"y\", 3) ; h <- function(s=1) { if (s==2) { x <- 5 } ; x } ; h() } ; g()  } ; f() }");
        assertEval("{ f <- function() { assign(\"x\", 2, inherits=TRUE) ; assign(\"x\", 1) ; h <- function() { x } ; h() } ; f() }");
        assertEval("{ x <- 3 ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x } ; h() } ; f() } ; g() }");
        assertEval("{ x <- 3 ; gg <- function() {  g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x } ; h() } ; f() } ; g() } ; gg() }");
        assertEval("{ h <- function() { x <- 2 ; f <- function() { if (FALSE) { x <- 1 } ; g <- function() { x } ; g() } ; f() } ; h() }");
        assertEval("{ f <- function() { assign(\"x\", 3) ; g <- function() { x } ; g() } ; x <- 10 ; f() }");
        assertEval("{ f <- function() { assign(\"x\", 3) ; h <- function() { assign(\"z\", 4) ; g <- function() { x } ; g() } ; h() } ; x <- 10 ; f() }");
        assertEval("{ f <- function() { assign(\"x\", 3) ; h <- function() { g <- function() { x } ; g() } ; h() } ; x <- 10 ; f() }");
        assertEval("{ f <- function() { assign(\"x\", 1) ; g <- function() { assign(\"z\", 2) ; x } ; g() } ; f() }");
        assertEval("{ h <- function() { x <- 3 ; g <- function() { assign(\"z\", 2) ; x } ; f <- function() { assign(\"x\", 2) ; g() } ; f() }  ; h() }");
        assertEval("{ h <- function() { x <- 3 ; g <- function() { assign(\"x\", 5) ; x } ; f <- function() { assign(\"x\", 2) ; g() } ; f() }  ; h() }");
        assertEval("{ x <- 10 ; g <- function() { x <- 100 ; z <- 2 ; f <- function() { assign(\"z\", 1); x <- x ; x } ; f() } ; g() }");
        assertEval("{ g <- function() { if (FALSE) { x <- 2 ; y <- 3} ; f <- function() { if (FALSE) { x } ; assign(\"y\", 2) ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ g <- function() { if (FALSE) {y <- 3; x <- 2} ; f <- function() { assign(\"x\", 2) ; exists(\"x\") }  ; f() } ; g() }");
        assertEval("{ g <- function() { if (FALSE) {y <- 3; x <- 2} ; f <- function() { assign(\"x\", 2) ; h <- function() { exists(\"x\") } ; h() }  ; f() } ; g() }");
        assertEval("{ g <- function() { if (FALSE) {y <- 3; x <- 2} ; f <- function() { assign(\"y\", 2) ; h <- function() { exists(\"x\") } ; h() }  ; f() } ; g() }");
        assertEval("{ g <- function() { if (FALSE) {y <- 3; x <- 2} ; f <- function() { assign(\"x\", 2) ; gg <- function() { h <- function() { exists(\"x\") } ; h() } ; gg() } ; f() } ; g() }");
        assertEval("{ x <- 3 ; f <- function(i) { if (i == 1) { assign(\"x\", 4) } ; function() { x } } ; f1 <- f(1) ; f2 <- f(2) ; f1() }");
        assertEval("{ x <- 3 ; f <- function(i) { if (i == 1) { assign(\"x\", 4) } ; function() { x } } ; f1 <- f(1) ; f2 <- f(2) ; f2() ; f1() }");
        assertEval("{ f <- function() { x <- 2 ; g <- function() { if (FALSE) { x <- 2 } ; assign(\"x\", 1, inherits=TRUE) } ; g() ; x } ; f() }");
        assertEval("{ h <- function() { if (FALSE) { x <- 2 ; z <- 3 } ; g <- function() { assign(\"z\", 3) ; if (FALSE) { x <- 4 } ;  f <- function() { exists(\"x\") } ; f() } ; g() } ; h() }");
        assertEval("{ f <- function(x) { assign(x, 23) ; exists(x) } ; c(f(\"a\"),f(\"b\")) }");
        assertEval("{ f <- function() { x <- 2 ; get(\"x\") } ; f() }");
        assertEval("{ x <- 3 ; f <- function() { get(\"x\") } ; f() }");
        assertEval("{ x <- 3 ; f <- function() { x <- 2 ; get(\"x\") } ; f() }");
        assertEval("{ x <- 3 ; f <- function() { x <- 2; h <- function() {  get(\"x\") }  ; h() } ; f() }");
        assertEval("{ f <- function() { g <- function() { get(\"x\", inherits=TRUE) } ; g() } ; x <- 3 ; f() }");
        assertEval("{ f <- function() { assign(\"z\", 2) ; g <- function() { get(\"x\", inherits=TRUE) } ; g() } ; x <- 3 ; f() }");
        assertEval("{ f <- function() { x <- 22 ; get(\"x\", inherits=FALSE) } ; f() }");
        assertEval("{ x <- 33 ; f <- function() { assign(\"x\", 44) ; get(\"x\", inherits=FALSE) } ; f() }");
        assertEval("{ g <- function() { if (FALSE) {y <- 3; x <- 2} ; f <- function() { assign(\"x\", 2) ; gg <- function() { h <- function() { get(\"x\") } ; h() } ; gg() } ; f() } ; g() }");

        // lookup with function matching
        assertEval("{ x <- function(){3} ; f <- function() { assign(\"x\", function(){4}) ; h <- function(s=1) { if (s==2) { x <- 5 } ; x() } ; h() } ; f() }");
        assertEval("{ f <- function() { assign(\"x\", function(){2}, inherits=TRUE) ; assign(\"x\", function(){1}) ; h <- function() { x() } ; h() } ; f() }");
        assertEval("{ x <- function(){3} ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x() } ; h() } ; f() } ; g() }");
        assertEval("{ x <- function(){3} ; gg <- function() {  g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x() } ; h() } ; f() } ; g() } ; gg() }");
        assertEval("{ h <- function() { x <- function(){2} ; f <- function() { if (FALSE) { x <- 1 } ; g <- function() { x } ; g() } ; f() } ; z <- h() ; z() }");
        assertEval("{ h <- function() { g <- function() {4} ; f <- function() { if (FALSE) { g <- 4 } ; g() } ; f() } ; h() }");
        assertEval("{ h <- function() { assign(\"f\", function() {4}) ; f() } ; h() }");
        assertEval("{ f <- function() { 4 } ; h <- function() { assign(\"f\", 5) ; f() } ; h() }");
        assertEval("{ f <- function() { 4 } ; h <- function() { assign(\"z\", 5) ; f() } ; h() }");
        assertEval("{ gg <- function() {  assign(\"x\", function(){11}) ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x() } ; h() } ; f() } ; g() } ; gg() }");
        assertEval("{ x <- function(){3} ; gg <- function() { assign(\"x\", 4) ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x() } ; h() } ; f() } ; g() } ; gg() }");
        assertEval("{ h <- function() { x <- function() {3} ; g <- function() { assign(\"z\", 2) ; x } ; f <- function() { assign(\"x\", 2) ; g() } ; f() }  ; z <- h() ; z() }");
        assertEval("{ h <- function() { x <- function() {3} ; g <- function() { assign(\"x\", function() {5} ) ; x() } ; g() } ; h() }");
        assertEval("{ h <- function() { z <- 3 ; x <- function() {3} ; g <- function() { x <- 1 ; assign(\"z\", 5) ; x() } ; g() } ; h() }");
        assertEval("{ h <- function() { x <- function() {3} ; gg <- function() { assign(\"x\", 5) ; g <- function() { x() } ; g() } ; gg() } ; h() }");
        assertEval("{ h <- function() { z <- 2 ; x <- function() {3} ; gg <- function() { assign(\"z\", 5) ; g <- function() { x() } ; g() } ; gg() } ; h() }");
        assertEval("{ h <- function() { x <- function() {3} ; g <- function() { assign(\"x\", function() {4}) ; x() } ; g() } ; h() }");
        assertEval("{ h <- function() { z <- 2 ; x <- function() {3} ; g <- function() { assign(\"z\", 1) ; x() } ; g() } ; h() }");
        assertEval("{ x <- function() { 3 } ; h <- function() { if (FALSE) { x <- 2 } ;  z <- 2  ; g <- function() { assign(\"z\", 1) ; x() } ; g() } ; h() }");
        assertEval("{ x <- function() { 3 } ; h <- function() { g <- function() { f <- function() { x <- 1 ; x() } ; f() } ; g() } ; h() }");
        assertEval("{ h <- function() { myfunc <- function(i) { sum(i) } ; g <- function() { myfunc <- 2 ; f <- function() { myfunc(2) } ; f() } ; g() } ; h() }");
        assertEval("{ x <- function() {11} ; g <- function() { f <- function() { assign(\"x\", 2) ; x() } ; f() } ; g() }");
        assertEval("{ x <- function() {3} ; f <- function(i) { if (i == 1) { assign(\"x\", function() {4}) } ; function() { x() } } ; f1 <- f(1) ; f2 <- f(2) ; f1() }");
        assertEval("{ x <- function() {3} ; f <- function(i) { if (i == 1) { assign(\"x\", function() {4}) } ; function() { x() } } ; f1 <- f(1) ; f2 <- f(2) ; f2() ; f1() }");
        assertEval("{ x <- function() {3} ; f <- function(i) { if (i == 1) { assign(\"x\", function() {4}) } ; function() { x() } } ; f1 <- f(1) ; f2 <- f(2) ; f1() ; f2() }");

        // lookup with super assignment
        assertEval("{ fu <- function() { uu <<- 23 } ; fu() ; sort(ls(globalenv())) }");
        assertEval("{ x <- 3 ; g <- function() { if (FALSE) { x <- 2 } ; f <- function() { h <- function() { x ; hh <- function() { x <<- 4 } ; hh() } ; h() } ; f() } ; g() ; x }");
        assertEval("{ f <- function() { x <- 1 ; g <- function() { h <- function() { x <<- 2 } ; h() } ; g() ; x } ; f() }");
        assertEval("{ g <- function() { if (FALSE) { x <- 2 } ; f <- function() { assign(\"x\", 4) ; x <<- 3 } ; f() } ; g() ; x }");
        assertEval("{ g <- function() { if (FALSE) { x <- 2 ; z <- 3 } ; h <- function() { if (FALSE) { x <- 1 } ; assign(\"z\", 10) ; f <- function() { assign(\"x\", 4) ; x <<- 3 } ; f() } ; h() } ; g() ; x }");
        assertEval("{ gg <- function() { assign(\"x\", 100) ; g <- function() { if (FALSE) { x <- 2 ; z <- 3 } ; " +
                        "h <- function() { if (FALSE) { x <- 1 } ; assign(\"z\", 10) ; f <- function() { assign(\"x\", 4) ; x <<- 3 } ; f() } ; h() } ; g() } ; x <- 10 ; gg() ; x }");
        assertEval("{ gg <- function() { if (FALSE) { x <- 100 } ; g <- function() { if (FALSE) { x <- 100 } ; h <- function() { f <- function() { x <<- 3 } ; f() } ; h() } ; g() } ; x <- 10 ; gg() ; x }");
        assertEval("{ g <- function() { if (FALSE) { x <- 2 ; z <- 3 } ; h <- function() { assign(\"z\", 10) ; f <- function() { x <<- 3 } ; f() } ; h() } ; g() ; x }");
        assertEval("{ g <- function() { x <- 2 ; z <- 3 ; hh <- function() { assign(\"z\", 2) ; h <- function() { f <- function() { x <<- 3 } ; f() } ; h() } ; hh() } ; x <- 10 ; g() ; x }");
        assertEval("{ g <- function() { x <- 2 ; z <- 3 ; hh <- function() { assign(\"z\", 2) ; h <- function() { assign(\"x\", 1); f <- function() { x <<- 3 } ; f() } ; h() } ; hh() ; x } ; x <- 10 ; g() }");
        assertEval("{ x <- 3 ; f <- function(i) { if (i == 1) { assign(\"x\", 4) } ; function(v) { x <<- v} } ; f1 <- f(1) ; f2 <- f(2) ; f1(10) ; f2(11) ; x }");
        assertEval("{ x <- 3 ; f <- function(i) { if (i == 1) { assign(\"x\", 4) } ; function(v) { x <<- v} } ; f1 <- f(1) ; f2 <- f(2) ; f2(10) ; f1(11) ; x }");
        assertEval("{ x <- 3 ; f <- function() { assign(\"x\", 4) ; h <- function(s=1) { if (s==2) { x <- 5 } ; x <<- 6 } ; h() ; get(\"x\") } ; f() }");
        assertEval("{ x <- 3 ; f <- function() { assign(\"x\", 4) ; hh <- function() { if (FALSE) { x <- 100 } ; h <- function() { x <<- 6 } ; h() } ; hh() ; get(\"x\") } ; f() }");

        assertEval("{ assign(\"z\", 10, inherits=TRUE) ; z }");

        // top-level lookups
        assertEval("{ exists(\"sum\", inherits = FALSE) }");
        assertEval("{ x <- 1; exists(\"x\", inherits = FALSE) }");

        assertEval("{ f <- function(z) { exists(\"z\") } ; f(a) }"); // requires promises

        assertEval("{ exists(\"sum\") }");

        assertEval("{ g <- function() { assign(\"myfunc\", function(i) { sum(i) });  f <- function() { lapply(2, \"myfunc\") } ; f() } ; g() }");
        assertEval("{ myfunc <- function(i) { sum(i) } ; g <- function() { assign(\"z\", 1);  f <- function() { lapply(2, \"myfunc\") } ; f() } ; g() }");
        assertEval("{ g <- function() { f <- function() { assign(\"myfunc\", function(i) { sum(i) }); lapply(2, \"myfunc\") } ; f() } ; g() }");

        assertEval(Ignored.Unstable, "{ g <- function() { myfunc <- function(i) { i+i } ; f <- function() { lapply(2, \"myfunc\") } ; f() } ; g() }");
    }

    @Test
    public void testEnvironment() {
        assertEval("{ h <- new.env() ; assign(\"abc\", \"yes\", h) ; exists(c(\"abc\", \"def\"), h) }");
        assertEval("{ h <- new.env() ; assign(\"abc\", \"yes\", h) ; exists(c(\"def\", \"abc\"), h) }");
        assertEval("{ h <- new.env() ; assign(c(\"a\"), 1, h) ; ls(h) }");
        assertEval("{ h <- new.env() ; assign(c(\"a\"), 1L, h) ; ls(h) }");

        assertEval("{ h <- new.env(parent=emptyenv()) ; assign(\"x\", 1, h) ; assign(\"y\", 2, h) ; ls(h) }");
        assertEval("{ h <- new.env(parent=emptyenv()) ; assign(\"y\", 1, h) ; assign(\"x\", 2, h) ; sort(ls(h)) }");

        assertEval("{ hh <- new.env() ; assign(\"z\", 3, hh) ; h <- new.env(parent=hh) ; assign(\"y\", 2, h) ; get(\"z\", h) }");

        // hashmaps
        assertEval("{ e<-new.env() ; ls(e) }");
        assertEval("{ e<-new.env() ; assign(\"x\",1,e) ; ls(e) }");
        assertEval("{ e<-new.env() ; assign(\"x\",1,e) ; get(\"x\",e) }");
        assertEval("{ h <- new.env() ; assign(\"x\", 1, h) ; assign(\"x\", 1, h) ; get(\"x\", h) }");
        assertEval("{ h <- new.env() ; assign(\"x\", 1, h) ; assign(\"x\", 2, h) ; get(\"x\", h) }");
        assertEval("{ h <- new.env() ; u <- 1 ; assign(\"x\", u, h) ; assign(\"x\", u, h) ; get(\"x\", h) }");
        assertEval("{ h <- new.env(parent=emptyenv()) ; assign(\"x\", 1, h) ; exists(\"x\", h) }");
        assertEval("{ h <- new.env(parent=emptyenv()) ; assign(\"x\", 1, h) ; exists(\"xx\", h) }");
        assertEval("{ hh <- new.env() ; assign(\"z\", 3, hh) ; h <- new.env(parent=hh) ; assign(\"y\", 2, h) ; exists(\"z\", h) }");
        assertEval("{ ph <- new.env() ; h <- new.env(parent=ph) ; assign(\"x\", 2, ph) ; assign(\"x\", 10, h, inherits=TRUE) ; get(\"x\", ph)}");

        // env queries
        assertEval("{ globalenv() }");
        assertEval("{ emptyenv() }");
        assertEval("{ baseenv() }");

        // ls
        assertEval("{ ls() }");
        assertEval("{ f <- function(x, y) { ls() }; f(1, 2) }");
        assertEval("{ x <- 1; ls(globalenv()) }");
        assertEval("{ x <- 1; .y <- 2; ls(globalenv()) }");

        assertEval("{ is.environment(globalenv()) }");
        assertEval("{ is.environment(1) }");

        // as.environment
        assertEval("{ as.environment(-1) }");
        assertEval("{ f <- function()  { as.environment(-1) } ; f() }");
        assertEval("{ as.environment(0) }");
        assertEval("{ as.environment(1) }");
        // GnuR has more packages loaded so can't test in the middle
        assertEval("{ as.environment(length(search())) }");
        assertEval("{ as.environment(length(search()) + 1) }");
        assertEval("{ as.environment(length(search()) + 2) }");

        assertEval("{ as.environment(\".GlobalEnv\") }");
        assertEval("{ as.environment(\"package:base\") }");

        // parent.env
        assertEval("{ identical(parent.env(baseenv()), emptyenv()) }");
        assertEval("{ e <- new.env(); `parent.env<-`(e, emptyenv()); identical(parent.env(e), emptyenv()) }");

        // environment
        assertEval("{ environment() }");
        assertEval("{ environment(environment) }");

        // environmentName
        assertEval("{ environmentName(baseenv()) }");
        assertEval("{ environmentName(globalenv()) }");
        assertEval("{ environmentName(emptyenv()) }");
        assertEval("{ environmentName(1) }");

        // locking
        assertEval("{ e<-new.env(); environmentIsLocked(e) }");
        assertEval("{ e<-new.env(); lockEnvironment(e); environmentIsLocked(e) }");
        assertEval("{ e<-new.env(); lockEnvironment(e); assign(\"a\", 1, e) }");
        assertEval("{ e<-new.env(); assign(\"a\", 1, e) ; lockEnvironment(e); assign(\"a\", 2, e) }");
        assertEval("{ e<-new.env(); assign(\"a\", 1, e) ; lockEnvironment(e, TRUE); assign(\"a\", 2, e) }");
        assertEval("{ e<-new.env(); assign(\"a\", 1, e); lockBinding(\"a\", e); assign(\"a\", 2, e) }");
        assertEval("{ e<-new.env(); assign(\"a\", 1, e) ; lockEnvironment(e, TRUE); unlockBinding(\"a\", e); assign(\"a\", 2, e) }");
        assertEval("{ e<-new.env(); assign(\"a\", 1, e); bindingIsLocked(\"a\", e) }");
        assertEval("{ e<-new.env(); assign(\"a\", 1, e); lockBinding(\"a\", e); bindingIsLocked(\"a\", e) }");

        // rm
        assertEval("{ rm(\"foo\", envir = baseenv()) }");
        assertEval("{ e<-new.env(); assign(\"a\", 1, e) ; lockEnvironment(e); rm(\"a\",envir = e); }");
        // ok to removed a locked binding
        assertEval("{ e<-new.env(); assign(\"a\", 1, e) ; lockBinding(\"a\", e); rm(\"a\",envir = e); ls() }");
        assertEval("{ e<-new.env(); assign(\"a\", 1, e) ; rm(\"a\",envir = e); ls() }");

        // get
        assertEval("{ e<-new.env(); get(\"x\", e) }");
        assertEval("{ e<-new.env(); x<-1; get(\"x\", e) }");
        assertEval("{ e<-new.env(); assign(\"x\", 1, e); get(\"x\", e) }");
        assertEval("{ e<-new.env(); x<-1; get(\"x\", e, inherits=FALSE) }");
        assertEval("{ e<-new.env(parent=emptyenv()); x<-1; get(\"x\", e) }");

        // misc
        assertEval("{ h <- new.env(parent=emptyenv()) ; assign(\"y\", 2, h) ; get(\"z\", h) }");
        assertEval("{ plus <- function(x) { function(y) x + y } ; plus_one <- plus(1) ; ls(environment(plus_one)) }");
        assertEval("{ ls(.GlobalEnv) }");
        assertEval("{ x <- 1 ; ls(.GlobalEnv) }");
        assertEval("{ ph <- new.env(parent=emptyenv()) ; h <- new.env(parent=ph) ; assign(\"x\", 10, h, inherits=TRUE) ; get(\"x\", ph)}");
        assertEval("{ ph <- new.env() ; h <- new.env(parent=ph) ; assign(\"x\", 2, h) ; assign(\"x\", 10, h, inherits=TRUE) ; get(\"x\", ph)}");
        assertEval("{ h <- new.env(parent=globalenv()) ; assign(\"x\", 10, h, inherits=TRUE) ; x }");
        assertEval("{ ph <- new.env() ; h <- new.env(parent=ph) ; assign(\"x\", 10, h, inherits=TRUE) ; x }");

        // Requires generic specialization
        assertEval("{ as.environment(as.environment) }");
    }

    @Test
    public void testEnvironmentAssignLocked() {
        assertEval(Ignored.Unknown, Output.IgnoreErrorContext, "{ x <- 1; lockBinding(\"x\", globalenv()); x <- 1 }");
    }

    @Test
    public void testFrames() {
        assertEval("{ t1 <- function() {  aa <- 1; t2 <- function() { cat(\"current frame is\", sys.nframe(), \"; \"); cat(\"parents are frame numbers\", sys.parents(), \"; \"); print(ls(envir = sys.frame(-1))) };  t2() }; t1() }");
    }

    @Test
    public void testAttach() {
        // tests must leave the search path unmodified
        assertEval("{ e <- new.env(); assign(\"x\", 1, e); attach(e, name = \"mine\"); r <- x; detach(\"mine\"); r }");
        assertEval("{ e <- new.env(); assign(\"x\", \"abc\", e); attach(e, 2); r <- x; detach(2); r }");
        assertEval("{ attach(.Platform, 2); r <- file.sep; detach(2); r }");
        assertEval("{ e <- new.env(); assign(\"x\", 1, e); attach(e, 2); r <- x; detach(2); r }");
        assertEval("{ e <- new.env(); assign(\"x\", 1, e); attach(e, 2); x; detach(2); x }");
        assertEval("{ detach(\"missing\"); x }");
    }

    @Test
    public void testFrameToEnv() {
        // Note: islistfactor is internal and should fail if it gets promise directly
        assertEval("{ makefun <- function(f) function(a) f(a); .Internal(islistfactor(environment(makefun(function(b) 2*b))$f, F)); }");
        // Turning frame into an environment should not evaluate all the promises:
        assertEval("{ makefun <- function(f,s) function(a) f(a); s <- function() cat('side effect'); .Internal(islistfactor(environment(makefun(function(b) 2*b, s()))$f, F)); }");
    }
}
