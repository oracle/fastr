/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

public class TestBuiltin_colon extends TestBase {

    private static final String[] INT_VALUES = new String[]{"0L", "1L", "2L", "30L", "-1L", "-30L"};
    private static final String[] DOUBLE_VALUES = new String[]{"0.2", "1.1", "2.3", "2.999999999", "5", "33.3", "-0.2", "-1.1", "-2.999999999", "-5", "-33.3"};
    private static final String[] BOOL_VALUES = new String[]{"TRUE", "FALSE"};

    @Test
    public void testOne() {
        assertEval(template("%0:%1", INT_VALUES, INT_VALUES));
        assertEval(template("%0:%1", INT_VALUES, DOUBLE_VALUES));
        assertEval(template("%0:%1", INT_VALUES, BOOL_VALUES));

        assertEval(template("%0:%1", DOUBLE_VALUES, INT_VALUES));
        assertEval(template("%0:%1", DOUBLE_VALUES, DOUBLE_VALUES));
        assertEval(template("%0:%1", DOUBLE_VALUES, BOOL_VALUES));

        assertEval(template("%0:%1", BOOL_VALUES, BOOL_VALUES));
        assertEval(template("%0:%1", BOOL_VALUES, INT_VALUES));
        assertEval(template("%0:%1", BOOL_VALUES, DOUBLE_VALUES));
    }
}
