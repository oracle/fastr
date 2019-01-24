/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_env2list extends TestBase {

    @Test
    public void testEnv2list() {
        // simple values are copied to the list
        assertEval("{ e <- new.env(); e$lst <- list(1,'a'); e$vec <- c(1,2,4); r <- as.list.environment(e); e$lst[[1]] <- 42; e$vec[[1]] <- 42; list(r$lst, r$vec, e$lst, e$vec) }");
        // ... is copied and contains promises, no deep copies
        assertEval(" { r <- (function(a,b,...) .Internal(env2list(environment(),TRUE,TRUE)))(list(1,'a'), c(1,2,4), cat('[should not be executed]')); list(r$a, r$b, sort(names(r))) }");
        // normal arguments are evaluated and copied
        assertEval("{ vec <- c(1,2,4); r <- (function(a) .Internal(env2list(environment(),TRUE,TRUE)))({ cat('[side effect]'); vec }); cat('[result created]'); r$a[[1]] <- 42; list(vec, r$a) }");
        // environment inside environment
        assertEval("{ e2 <- new.env(); e2$vec <- c(1,2,4); e <- new.env(); e$a <- e2; l <- .Internal(env2list(e,TRUE,TRUE)); l$a$vec[[1]] <- 42; list(e$a$vec, e2$vec, l$a$vec) }");
        // explicit promise
        assertEval("{ vec <- c(1,2,4); e <- new.env(); delayedAssign('a', {cat('[evaluating a]'); vec}, assign.env=e); l <- .Internal(env2list(e,TRUE,TRUE)); cat('[result created]'); l$a[[1]] <- 42; list(l$a, vec) }");
        // pairlist
        assertEval("{ e<-new.env(); e$x=pairlist(1:3); l<-.Internal(env2list(e, TRUE, TRUE)); print(l$x); typeof(l$x) }");
    }

    @Test
    public void testEnv2listArgValidation() {
        // first argument must be an environment
        assertEval(".Internal(env2list(list(1,2,3),TRUE,TRUE))");
        // the remaining two logical arguments are silently converted no matter what type they have
        assertEval(".Internal(env2list(new.env(),'string',list()))");
    }
}
