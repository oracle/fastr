/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.library.utils;

import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

public class TestTypeConvert extends TestBase {
    @Test
    public void basicTests() {
        assertEval("type.convert('42')");
        assertEval("type.convert(c('42', '42.42'))"); // convert to double vector
        assertEval("type.convert(c('NA', 'NA'))");
        assertEval("type.convert(c('NA', 'TRUE'))");
        assertEval("type.convert(c('NA', '42'))");
        assertEval("type.convert(c('NA', '44.5'))");
        assertEval("type.convert(c(NA, '44.5'))");  // string NA
        // looks like integer, but is double (because it would be INT_NA)
        assertEval("type.convert('-2147483648')");
    }

    private static final String[] LIT_VALUES = new String[]{"0xFFF", "0xFFFFFFFFFFF", "123", "2147483648"};

    @Test
    public void testConvertLiterals() {
        for (String suf : new String[]{"", "L"}) {
            for (String sign : new String[]{"", "-", "+"}) {
                String l = sign + "%0" + suf;
                assertEval(template("type.convert('" + l + "')", LIT_VALUES));
                assertEval(template("typeof(type.convert('" + l + "'))", LIT_VALUES));
            }
        }
    }

    @Test
    public void testFirstTypeMustBeOfModeTest() {
        assertEval("type.convert('NA', 1)");
    }

    @Test
    public void typeConvertExternal2Test() {
        assertEval(".External2(utils:::C_typeconvert, c('1'), 'NA', FALSE, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c('1'), 'NA', NA, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c('1'), 'NA', NULL, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c('1'), 'NA', 'TRUE', '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c('1'), 'NA', 'abc', '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c('1'), 'NA', list('abc'), '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c('1'), 'NA', list(), '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c('1'), 'NA', c(), '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c('1'), 'NA', c(NULL), '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c('1'), 'NA', 1, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c('1'), 'NA', 2, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c('1'), 'NA', environment(), '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c('1'), 'NA', function() {}, '.', 'allow.loss')");

        assertEval(".External2(utils:::C_typeconvert, NULL, 'NA', FALSE, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, NA, 'NA', FALSE, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c(), 'NA', FALSE, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c('1'), NULL, FALSE, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c('1'), NA, FALSE, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c('1'), c(), FALSE, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c(1), c(1), FALSE, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c(1, TRUE), c(1), FALSE, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c(1, 'TRUE'), c(1), FALSE, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c(1, 'TRUE', 'x'), c(1), FALSE, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c(1, '1'), c(1), FALSE, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c(1, 'x'), c(1), FALSE, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c('1'), c(1), FALSE, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, list(1), list(1), FALSE, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, list('1'), list('1'), FALSE, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, list('1'), list(1), FALSE, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, environment(), 'NA', FALSE, '.', 'allow.loss')");
        assertEval(".External2(utils:::C_typeconvert, c('1'), environment(), FALSE, '.', 'allow.loss')");
    }

}
