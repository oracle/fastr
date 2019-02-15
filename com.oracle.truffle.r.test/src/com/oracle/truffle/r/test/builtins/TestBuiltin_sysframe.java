/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.test.builtins.TestBuiltin_sysparent.SYS_PARENT_SETUP;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestBuiltin_sysframe extends TestBase {
    @Test
    public void frameAccessCommonTest() {
        assertEval("{ foo <- function(x) lapply(1:7, function(x) ls(sys.frame(x)));" + SYS_PARENT_SETUP + "}");
    }

    @Test
    public void sysFrameWithPromises() {
        // Note: the parameter names help to identify the corresponding environments in the output
        assertEval(
                        "{ top <- function(vtop) vtop;" +
                                        "foo <- function(vfoo) top(vfoo);" +
                                        "boo <- function(vboo) foo(sys.frame(vboo));" +
                                        "bar <- function(vbar) do.call(boo, list(vbar), envir = parent.frame(2));" +
                                        "baz <- function(vbaz) bar(vbaz);" +
                                        "start <- function(vstart) baz(vstart);" +
                                        "lapply(lapply(0:8, function(i) start(i)), function(env) sort(tolower(ls(env)))); }");
    }

    private final String[] envirValues = {"-6", "-4", "-3", "-2", "-1", "0", "1", "2", "3", "4", "5", "6"};

    @Test
    public void sysFrameViaEval() {
        assertEval(template("{ f <- function() { xx <- 'xv'; f1 <- function() get('xx', envir = %0);  xy <- new.env(); xy$xx <- 'aa'; eval(parse(text='f1()'), envir=xy)}; f() }", envirValues));
        assertEval(template("{ f <- function() { xx <- 'xv'; f1 <- function() ls(sys.frame(%0));  eval(parse(text='f1()'), envir=environment())}; f() }", envirValues));
    }
}
