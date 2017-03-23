/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.test.library.base;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestSimpleLists extends TestBase {

    @Test
    public void testListCreation() {
        assertEval("{ list() }");
        assertEval("{ list(list(),list()) }");
        assertEval("{ list(1,NULL,list()) }");
    }

    @Test
    public void testListAccess() {
        assertEval("{ l <- list(c(1,2,3),\"eep\") ; l[[1]] }");
        assertEval("{ l <- list(c(1,2,3),\"eep\") ; l[[2]] }");

        assertEval("{ l <- list(1,2,3) ; l[5] }");
        assertEval("{ l <- list(1,2,3) ; typeof(l[5]) }");
        assertEval("{ l <- list(1,2,3) ; l[[5]] }");

        assertEval("{ l <- list(1,2,3) ; l[0] }");
        assertEval(Output.IgnoreErrorContext, "{ l <- list(1,2,3) ; l[[0]] }");

        assertEval("{ l <- list(1,2,3) ; l[[NA]] }");
        assertEval("{ l <- list(1,2,3) ; l[[NaN]] }");
        assertEval("{ l <- list(1,2,3) ; typeof(l[[NA]]) }");
        assertEval("{ l <- list(1,2,3) ; typeof(l[[NaN]]) }");

        assertEval("{ l <- list(1,2,3) ; l[NA] }");
        assertEval("{ l <- list(1,2,3) ; l[NaN] }");
        assertEval("{ l <- list(1,2,3) ; typeof(l[NA]) }");
        assertEval("{ l <- list(1,2,3) ; typeof(l[NaN]) }");

        assertEval("{ l <- list(1,2,3) ; l[-2] }");
        assertEval("{ l <- list(1,2,3) ; typeof(l[-2]) }");

        assertEval(Output.IgnoreErrorContext, "{ l <- list(1,2,3) ; l[[-2]] }");

        assertEval("{ l <- list(1,2,3) ; l[-5] }");
        assertEval(Output.IgnoreErrorContext, "{ l <- list(1,2,3) ; l[[-5]] }");

        assertEval("{ a <- list(1,NULL,list()) ; a[3] }");
        assertEval("{ a <- list(1,NULL,list()) ; a[[3]] }");
        assertEval("{ a <- list(1,NULL,list()) ; typeof(a[3]) }");
        assertEval("{ a <- list(1,NULL,list()) ; typeof(a[[3]]) }");

        assertEval("{ a <- list(1,2,3) ; x <- integer() ; a[x] }");
        assertEval(Output.IgnoreErrorContext, "{ a <- list(1,2,3) ; x <- integer() ; a[[x]] }");
    }

    @Test
    public void testListUpdate() {
        assertEval("{ l <- list(c(1,2,3),c(4,5,6)) ; l[[1]] <- c(7,8,9) ; l[[1]] }");
        assertEval("{ l <- list(42); l[1][1] <- 7; l }");
        assertEval("{ l <- list(c(42)); l[1][1] <- 7; l }");
        assertEval("{ l <- list(c(42, 43)); l[[1]][1] <- 7; l }");
        assertEval("{ l <- list(c(42)); idx <- TRUE; l[idx] <- list(c(1,2,3)); l }");
    }

    @Test
    public void testListCombine() {
        assertEval("{ a <- c(list(1)) ; typeof(a) }");
        assertEval("{ a <- c(list(1)) ; typeof(a[1]) }");
        assertEval("{ a <- c(list(1)) ; typeof(a[[1]]) }");
        assertEval("{ a <- c(1,2,list(3,4),5) ; a }");
        assertEval("{ a <- c(1,2,list(3,4),5) ; typeof(a) }");
        assertEval("{ a <- c(1,2,list(3,4),5) ; a[3] }");
        assertEval("{ a <- c(1,2,list(3,4),5) ; typeof(a[3]) }");
        assertEval("{ a <- c(1,2,list(3,4),5) ; a[[3]] }");
        assertEval("{ a <- c(1,2,list(3,4),5) ; typeof(a[[3]]) }");
    }

    @Test
    public void testListArgumentEvaluation() {
        assertEval("{ a <- c(0,0,0) ; f <- function() { g <- function() { a[2] <<- 9 } ; g() } ; u <- function() { a <- c(1,1,1) ; f() ; a } ; list(a,u()) }");
    }

    @Test
    public void testListRefcounting() {
        // (LS) more tests like this are needed...
        assertEval("a <- list(x=c(1,2,3)); b <- a; a$x[[1]] <- 4; b");
        assertEval("a <- list(); a$x <- c(1,2,3); b <- a; a$x[[1]] <- 4; b");
        assertEval("a <- list(); a$y <- 'dummy'; a$x <- c(1,2,3); b <- a; a$x[[1]] <- 4; b");
        assertEval("a <- list(); a$y <- 'dummy'; b <- a; a$x <- c(1,2,3); a$x[[1]] <- 4; b");
        assertEval("a <- list(x=c(1,2,10)); b <- list(); b$x <- c(1,42); swap <- a; a <- b; b <- swap; a$x[[2]] <- 3; list(a=a, b=b)");
        assertEval("z <- c(1,4,8); a<-list(); a$x <- z; a$x[[1]] <- 42; list(a=a, z=z)");

        // with a scalar
        assertEval("a <- list(x=1); b <- a; a$x[[1]] <- 42; list(a=a,b=b)");

        // recursive
        assertEval("a <- list(x=list(y=c(1,2,4))); b <- a; b$x$y[[1]] <- 42; list(a=a,b=b)");
        assertEval("a <- list(x=list(y=c(1,2,4))); b <- a$x; b$y[[1]] <- 42; list(a=a,b=b)");
        assertEval("a <- list(x=list(y=c(1,2,4))); b <- a$x; c <- b; c$y[[1]] <- 42; list(a=a,b=b,c=c)");
        assertEval("l <- list(k=list()); k <- list(x=c(1,10)); l$k <- k; l$k$x[1] <- 42; list(l_k=l$k$x, k=k$x);");

        // parameters
        assertEval("f <- function(l) l$x[[1]]<-42; a <- list(x=c(1,2,4)); b <- a; f(b); list(a=a, b=b)");
        assertEval("f <- function(l) l$x[[1]]<-42; a <- list(x=c(1,2,4)); f(a); a");
        assertEval("f <- function(l) l; a <- list(x=c(1,2,4)); b <- f(a); b$x[[1]] <- 42; list(a=a, b=b)");

        // lists returned from built-ins
        assertEval("r <- split(1,1); r[[1]] / 2; r;");
        assertEval("r <- split(1,1); x <- r; r[[1]] <- 42; x;");
        assertEval("x <- c(1,2,3); l <- list(x); x[[1]] <- 42; l;");

        // (potential) cycles
        assertEval(Ignored.Unimplemented, "l <- list(list(list())); l[[1]][[1]] <- l; l");
        assertEval("l <- list(k=list()); k <- list(l=list()); l$k <- k; k$l <- l; k$l$x <- 42; list(k=k, l=l)");
        assertEval("l <- list(x=list()); v <- list(y=42,x=list()); v$x <- l; l$x <- v; l$y <- 44; k <- v; k$y <- 45; list(l=l,v=v,k=k);");
    }

    @Test
    public void testListDuplication() {
        assertEvalFastR("l <- list(x=c(3,4,5)); id <- .fastr.identity(l); l$x[2] <- 10; id == .fastr.identity(l)", "TRUE");
        assertEvalFastR("l <- list(x=c(3,4,5)); id <- .fastr.identity(l$x); l$x[2] <- 10; id == .fastr.identity(l$x)", "TRUE");
        assertEvalFastR("l <- list(x=c(3,4,5)); id <- .fastr.identity(l$x); l$x <- 1:10; id == .fastr.identity(l$x)", "FALSE");
    }

    @Test
    public void testAvoidingCopying() {
        // these tests should not print copying related output
        assertEval("a<-list(); a$x <- c(1,2,3); invisible(tracemem(a$x)); a$x[[1]] <- a$x[[2]] * 3; a$x");
        assertEval("a<-list(); a$x <- c(1,2,3); invisible(tracemem(a$x)); a$y <- 'dummy'; a$x[[1]] <- a$x[[2]] * 3; a$x");
        assertEval("a<-list(); a$x <- c(1,2,3); ident <- function(q)q; invisible(tracemem(a$x)); a$x[[1]] <- ident(a$x[[2]]); a$x");
        assertEval("z <- c(1,4,8); invisible(tracemem(z)); a<-list(); a$x <- z;");
        assertEval("l <- list(x=c(NA,NA)); l$x[1] <- 42; invisible(tracemem(l$x)); l$x[2] <- 42;");
        assertEval("l <- list(); l$x <- c(NA); length(l$x) <- 10; l$x[1] <- 42; invisible(tracemem(l$x)); l$x[2:9] <- 42;");
        assertEval("x <- list(c(1,2,3)); invisible(tracemem(x)); x[[1]] <- 42;");
    }

    @Test
    public void testNullListAssignment() {
        assertEval("a<-NULL; a$b<-42L; dput(a)");
        assertEval("a<-NULL; a$b<-print; dput(a)");
        assertEval("a<- NULL; a <- `$<-`(a, \"a\", 1); dput(a)");
        assertEval("a<- NULL; a <- `[[<-`(a, \"a\", 1); dput(a)");
        assertEval("a<- NULL; a <- `[[<-`(a, 1, 1); dput(a)");
        // FastR produces a better error context
        assertEval(Output.IgnoreErrorContext, "a<- NULL; a <- `$<-`(a, 1, 1); dput(a)");
    }
}
