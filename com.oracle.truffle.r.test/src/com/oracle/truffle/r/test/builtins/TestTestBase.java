/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Ignore;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

/**
 * Documents and tests the testing API.
 */
public class TestTestBase extends TestBase {
    @Test
    public void testTraits() {
        assertEval(Output.IgnoreErrorMessage, getCode("cat('Error in foo(42): FastR error message')", "cat('Error in foo(42): GnuR error message is different')"));
        assertEval(Output.IgnoreErrorMessage, getCode("cat('Error in foo(44): IgnoreErrorMessage with different ctx')", "cat('Error in foo(42): IgnoreErrorMessage with different ctx')"));
        assertEval(Output.IgnoreErrorMessage, getCode("cat('Error in foo(42): IgnoreErrorMessage starts with newline')", "cat('Error in foo(42):\\nIgnoreErrorMessage starts with newline')"));

        assertEval(Output.IgnoreErrorContext, getCode("cat('Error in .Internal(foo(44)): IgnoreErrorContext')", "cat('Error in foo(42): IgnoreErrorContext')"));
        assertEval(Output.IgnoreErrorContext, getCode("cat('Error in foo(42)   : IgnoreErrorContext extra spaces')", "cat('Error in foo(42): IgnoreErrorContext extra spaces')"));

        assertEval(Output.IgnoreWarningContext, getCode("cat('Warning message: In .Internal(foo(42)) : IgnoreWarningContext')", "cat('Warning message: In foo(42) : IgnoreWarningContext')"));
        assertEval(Output.IgnoreWarningContext,
                        getCode("cat('Warning message: In .Internal(foo(42)) : IgnoreWarningContext extra newline')", "cat('Warning message: In foo(42) : \\nIgnoreWarningContext extra newline')"));

        assertEval(Output.IgnoreWhitespace, getCode("cat('Error in foo(42) : IgnoreWhitespace extra spaces')", "cat('Error  in foo(42): \\nIgnoreWhitespace extra spaces')"));
    }

    @Test
    @Ignore // these tests should fail
    public void negativeTestTraits() {
        assertEval(Output.IgnoreErrorContext, getCode("cat('Error in foo(44): IgnoreErrorContext diff message')", "cat('Error in foo(42): IgnoreErrorContext should fail')"));
        assertEval(Output.IgnoreWarningContext,
                        getCode("cat('Warning message: In .Internal(foo(42)): IgnoreWarningContext diff message')", "cat('Warning message in foo(42): IgnoreWarningContext should fail')"));
    }

    private static String getCode(String fastr, String gnur) {
        return "if (exists('.fastr.identity')) { " + fastr + " } else { " + gnur + " }";
    }
}
