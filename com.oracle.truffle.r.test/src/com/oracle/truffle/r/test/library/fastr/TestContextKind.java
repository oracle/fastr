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

package com.oracle.truffle.r.test.library.fastr;

import com.oracle.truffle.r.test.TestBase;
import com.oracle.truffle.r.test.TestTrait;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests internal working of {@link com.oracle.truffle.r.runtime.context.RContext.ContextKind}.
 */
public class TestContextKind extends TestBase {
    @Test
    @Ignore
    public void test1() {
        assertEval(Context.NonShared, "{ Sys.setenv(MY_ENV_VAR = '1') }");
    }

    @Test
    @Ignore
    public void test2() {
        assertEval(Context.NonShared, "{ Sys.getenv('MY_ENV_VAR') }");
    }

    @Test
    @Ignore
    public void testAddSymbolToGlobalEnv() {
        // .GlobalEnv should be cleaned up after every test in shared context.
        assertEval("x <- 42");
        assertEval("'x' %in% names(.GlobalEnv)");
    }

    @Test
    @Ignore
    public void testDigitsOption() {
        // options should be reset
        assertEval("{ options(digits = 3); getOption('digits') }");
        assertEval("{ getOption('digits') }");
        assertEval("{ 23.123456789 }");
    }

    /**
     * Option 'error' cannot be reset in gnur, so we should run these tests in a non-shared context.
     */
    @Test
    @Ignore
    public void testErrorOption() {
        assertEval("{ options(error = quote(cat('Err occured\n'))); non_existing_var }");
        assertEval("{ non_existing_var }");
    }

    @Test
    public void testResetSeed() {
        assertEval("{ set.seed(11, 'Marsaglia-Multicarry') }");
        assertEval("{ set.seed(42); rnorm(5) }");
    }
}
