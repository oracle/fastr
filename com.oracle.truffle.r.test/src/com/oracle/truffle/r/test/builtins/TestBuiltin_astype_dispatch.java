/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Tests internal dispatch of 'as.{type}' functions.
 */
public class TestBuiltin_astype_dispatch extends TestBase {

    private static final String[] AS_FUNC_NAMES = new String[]{"as.character", "as.integer", "as.double", "as.complex", "as.logical", "as.raw"};

    @Test
    public void testAsFunctionInternalDispatch() {
        assertEval(template("{ %0.myclass <- function(x) 42L; %0(structure(TRUE, class='myclass')); }", AS_FUNC_NAMES));
    }

    @Test
    public void testExtraArgumentsArePassedToOverload() {
        assertEval("as.integer.myclass <- function(x, extra, ...) list(x=x, extra=extra, varargs=list(...)); as.integer(structure(TRUE, class='myclass'), my=TRUE, extra=42L, args='hello');");
    }

    @Test
    public void testWithBuiltinFunction() {
        // taken from grDevices package
        assertEval("as.numeric(diff(structure(c(-154401120, 1503191520), class = c('POSIXct', 'POSIXt'), tzone = 'GMT')), units='secs')");
    }
}
