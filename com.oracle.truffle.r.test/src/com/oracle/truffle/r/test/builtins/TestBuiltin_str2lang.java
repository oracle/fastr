/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

public class TestBuiltin_str2lang extends TestBase {
    /**
     * str2lang builtin takes only character vector of length one, and does not coerce.
     */
    @Test
    public void testWrongArguments() {
        assertEval(Output.IgnoreErrorMessage, "{ str2lang(1) }");
        assertEval(Output.IgnoreErrorMessage, "{ str2lang(1:3) }");
        assertEval(Output.IgnoreErrorMessage, "{ str2lang(c('a', 'b')) }");
        assertEval(Output.IgnoreErrorMessage, "{ str2lang(NULL) }");
        assertEval(Output.IgnoreErrorMessage, "{ str2lang(NA) }");
    }

    @Test
    public void testSimpleCalls() {
        assertEval("{ str2lang('1 + 1') }");
        assertEval("{ str2lang('abs(1 + 1)') }");
        assertEval("{ str2lang('x[3] <- 1+4') }");
        assertEval("{ str2lang('log(y)') }");
        assertEval("{ str2lang('abc') }");
    }

    @Test
    public void testReturnValue() {
        assertEval("{ qa <- str2lang('abc'); is.symbol(qa) }");
        assertEval("{ qa <- str2lang('abc'); !is.call(qa) }");
        // Should be just a number, not a call
        assertEval("{ num <- str2lang('1.375'); typeof(num) }");
        assertEval("{ qa <- str2lang('log(y)'); is.call(qa) }");
    }
}
