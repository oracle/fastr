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
package com.oracle.truffle.r.test.packages.analyzer.parser;

import java.util.regex.Matcher;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.r.test.packages.analyzer.parser.DiffParser.ChangeCommand;

public class PatternTest {

    private static void assertMatch(String s, ChangeCommand expected) {
        Matcher matcher = DiffParser.CHANGE_CMD_PATTERN.matcher(s);

        Assert.assertTrue(matcher.matches());

        String cmdStr = matcher.group("CMD");
        Assert.assertEquals(1, cmdStr.length());

        char cmd = cmdStr.charAt(0);

        String lFromStr = matcher.group(1);
        String lToStr = matcher.group(3);
        String rFromStr = matcher.group(5);
        String rToStr = matcher.group(7);

        Assert.assertEquals(expected, new ChangeCommand(atoi(lFromStr), atoi(lToStr), cmd, atoi(rFromStr), atoi(rToStr)));
    }

    private static int atoi(String rToStr) {
        if (rToStr != null && !rToStr.isEmpty()) {
            return Integer.parseInt(rToStr);
        }
        return -1;
    }

    @Test
    public void testSingleDigitLine() {
        assertMatch("1a1", new ChangeCommand(1, -1, 'a', 1, -1));
        assertMatch("1c1", new ChangeCommand(1, -1, 'c', 1, -1));
        assertMatch("1d1", new ChangeCommand(1, -1, 'd', 1, -1));
    }

    @Test
    public void testMultipleDigitsLine() {
        assertMatch("10a1", new ChangeCommand(10, -1, 'a', 1, -1));
        assertMatch("100c1", new ChangeCommand(100, -1, 'c', 1, -1));
        assertMatch("12345d1", new ChangeCommand(12345, -1, 'd', 1, -1));
        assertMatch("1a10", new ChangeCommand(1, -1, 'a', 10, -1));
        assertMatch("1c100", new ChangeCommand(1, -1, 'c', 100, -1));
        assertMatch("1d12345", new ChangeCommand(1, -1, 'd', 12345, -1));
    }

    @Test
    public void testRangeSingleDigit() {
        assertMatch("1,2a1", new ChangeCommand(1, 2, 'a', 1, -1));
        assertMatch("1,2a1,2", new ChangeCommand(1, 2, 'a', 1, 2));
        assertMatch("1a1,2", new ChangeCommand(1, -1, 'a', 1, 2));
    }

    @Test
    public void testRangeMultipleDigits() {
        assertMatch("10,25a1", new ChangeCommand(10, 25, 'a', 1, -1));
        assertMatch("10,25a10,25", new ChangeCommand(10, 25, 'a', 10, 25));
        assertMatch("1a10,25", new ChangeCommand(1, -1, 'a', 10, 25));
    }

}
