/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.test.builtins;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestBuiltin_rm extends TestBase {
    @Test
    public void basicTests() {
        assertEval("tmp <- 42; rm(tmp); tmp");
        assertEval("tmp <- 42; rm(list='tmp'); tmp");
        assertEval(" e <- new.env(); e$a <- 42; rm(list='a', envir=e); e$a");
        assertEval("tmp <- 42; f <- function() rm(list='tmp',inherits=T); f(); tmp");
        assertEval("{ env0 <- new.env(); env0$a <- 123L; env1 <- new.env(parent=env0); env1$b <- 456L; rm('a', envir=env1, inherits=T); lapply(c(env0, env1), function(x) ls(x)) }");
        assertEval("{ env0 <- new.env(); env0$a <- 123L; env1 <- new.env(parent=env0); env1$b <- 456L; rm('a', envir=env1, inherits=F); lapply(c(env0, env1), function(x) ls(x)) }");
        assertEval("{ env0 <- new.env(); env0$b <- 123L; env1 <- new.env(parent=env0); env1$b <- 456L; rm('b', envir=env1, inherits=F); lapply(c(env0, env1), function(x) ls(x)) }");
        assertEval("{ rm(list=ls(baseenv(), all.names=TRUE), envir=baseenv()) }");
        assertEval("{ e <- new.env(parent=baseenv()); rm('c', envir=e, inherits=T) }");
        assertEval("{ e <- new.env(parent=baseenv()); e$a <- 1234L; rm(c('c', 'a'), envir=e, inherits=T); ls(e) }");
        assertEval("{ e <- new.env(); e$a <- 1234L; rm(list=c('c', 'a'), envir=e); ls(e) }");
        assertEval("{ e <- new.env(); e$a <- 1234L; lockEnvironment(e); rm(list='a', envir=e); ls(e) }");
        assertEval("{ e <- new.env(); e$a <- 1234L; lockBinding('a', e); rm(list='a', envir=e); ls(e) }");
        assertEval("{ rm(list='a', envir=emptyenv()) }");
        assertEval("{ rm(list=ls(emptyenv()), envir=emptyenv()) }");
    }

    @Test
    public void testArgsCasting() {
        assertEval("tmp <- 42; rm(tmp, inherits='asd')");
        assertEval(".Internal(remove(list=33, environment(), F))");
        assertEval("tmp <- 42; rm(tmp, envir=NULL)");
        assertEval("tmp <- 42; rm(tmp, envir=42)");
    }
}
