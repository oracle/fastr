/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.nodes.builtin.base.GrepFunctions.GrepRaw.FixedPatternFinder;
import com.oracle.truffle.r.nodes.builtin.base.GrepFunctions.GrepRaw.HaystackDescriptor;
import com.oracle.truffle.r.nodes.builtin.base.GrepFunctions.GrepRaw.Range;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RRawVector;
import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.r.test.TestBase;

// Checkstyle: stop line length check
public class TestBuiltin_grepRaw extends TestBase {
    @Test
    public void testFixedNoValueNoAll() {
        assertEval("grepRaw('no match', 'textText', fixed=T)");
        assertEval("grepRaw('adf', 'adadfadfdfadadf', fixed=T)");
    }

    @Test
    public void testFixedNoValueNoAllOffset() {
        assertEval("grepRaw('no match', 'textText', fixed=T, offset=5)");
        assertEval("grepRaw('adf', 'adadfadfdfadadf', fixed=T, offset=7)");
        assertEval("grepRaw('X', 'aaXaa', fixed=T, offset=3)");
    }

    @Test
    public void testFixedValueNoAll() {
        assertEval("grepRaw('text', 'textText', fixed=T, value=T)");
        assertEval("grepRaw('no match', 'textText', fixed=T, value=T)");
        assertEval("grepRaw('X', 'aXaaaX', fixed=T, value=T)");
        assertEval("grepRaw('XX', 'aXXaaaX', fixed=T, value=T)");
    }

    @Test
    public void testFixedValueAll() {
        assertEval("grepRaw('X', 'aXaaaX', fixed=T, value=T, all=T)");
        assertEval("grepRaw('X', 'aXaaa', fixed=T, value=T, all=T)");
        assertEval("grepRaw('X', 'XXXXXXXX', fixed=T, value=T, all=T)");
        assertEval("grepRaw('two-matches', 'two-matches fff two-matches', fixed=T, value=T, all=T)");
        assertEval("grepRaw('no match', 'XXXXXXXX', fixed=T, value=T, all=T)");
    }

    @Test
    public void testFixedIgnoreInvert() {
        // From gnu-r help: invert option is ignored unless value=T.
        // And warning should be generated.
        assertEval("grepRaw('no match', 'XXXXXXXX', fixed=T, invert=T, value=F)");
    }

    @Test
    public void testFixedInvertNoAll() {
        assertEval("grepRaw('no match', 'XXXXXXXX', fixed=T, invert=T, value=T)");
        assertEval("grepRaw('two-matches', 'two-matches fff two-matches', fixed=T, invert=T, value=T)");
    }

    @Test
    public void testFixedInvertAll() {
        assertEval("grepRaw('two-matches', 'two-matches fff two-matches', fixed=T, invert=T, value=T, all=T)");
    }

    @Test
    public void testFixedInvertAllOffset() {
        assertEval("grepRaw('two-matches', 'two-matches fff two-matches', fixed=T, invert=T, value=T, all=T, offset=11)");
    }

    @Test
    public void testFixedEmpty() {
        assertEval("grepRaw('adf', '', fixed=TRUE)");
    }

    @Test
    public void testFixedFindOneMatch() {
        HaystackDescriptor haystackDescriptor = findFirst(new byte[]{'X'}, new byte[]{'a', 'X', 'a'});

        Assert.assertEquals(1, haystackDescriptor.getMatchedRangesCount());
        Assert.assertEquals(2, haystackDescriptor.getUnmatchedRangesCount());

        Range firstRange = haystackDescriptor.getRange(0);
        Range secondRange = haystackDescriptor.getRange(1);
        Range thirdRange = haystackDescriptor.getRange(2);
        Assert.assertEquals(0, firstRange.getFromIdx());
        Assert.assertEquals(1, firstRange.getToIdx());
        Assert.assertEquals(1, secondRange.getFromIdx());
        Assert.assertEquals(2, secondRange.getToIdx());
        Assert.assertEquals(2, thirdRange.getFromIdx());
        Assert.assertEquals(3, thirdRange.getToIdx());
    }

    @Test
    public void testFixedFindFirstNoMatch() {
        HaystackDescriptor haystackDescriptor = findFirst(new byte[]{'X'}, new byte[]{'a', 'a', 'a'});
        Assert.assertEquals(0, haystackDescriptor.getMatchedRangesCount());
        Assert.assertEquals(1, haystackDescriptor.getUnmatchedRangesCount());
    }

    @Test
    public void testFixedFindAllNoMatch() {
        HaystackDescriptor haystackDescriptor = findAll(new byte[]{'X'}, new byte[]{'a', 'a'});

        Assert.assertEquals(0, haystackDescriptor.getMatchedRangesCount());
        Assert.assertEquals(1, haystackDescriptor.getUnmatchedRangesCount());

        Range range = haystackDescriptor.getRange(0);
        Assert.assertEquals(0, range.getFromIdx());
        Assert.assertEquals(2, range.getToIdx());
    }

    @Test
    public void testFixedFindTwoMatches() {
        HaystackDescriptor haystackDescriptor = findAll(new byte[]{'X', 'X'},
                        new byte[]{'a', 'a', 'X', 'X', 'X', 'X', 'a'});

        Assert.assertEquals(2, haystackDescriptor.getMatchedRangesCount());
        Assert.assertEquals(2, haystackDescriptor.getUnmatchedRangesCount());

        Range firstMatched = haystackDescriptor.getRange(1);
        Assert.assertEquals(2, firstMatched.getFromIdx());
        Assert.assertEquals(4, firstMatched.getToIdx());

        Range secondMatched = haystackDescriptor.getRange(2);
        Assert.assertEquals(4, secondMatched.getFromIdx());
        Assert.assertEquals(6, secondMatched.getToIdx());
    }

    @Test
    public void testFixedFindMatchesAtEnds() {
        HaystackDescriptor haystackDescriptor = findAll(new byte[]{'X'}, new byte[]{'X', 'a', 'X'});

        Assert.assertEquals(2, haystackDescriptor.getMatchedRangesCount());
        Assert.assertEquals(1, haystackDescriptor.getUnmatchedRangesCount());
    }

    @Test
    public void testFixedFindOneMatchWithOffset() {
        RRawVector pattern = RDataFactory.createRawVector(new byte[]{'X'});
        RRawVector haystack = RDataFactory.createRawVector(new byte[]{'a', 'a', 'X', 'a', 'a'});
        FixedPatternFinder patternFinder = new FixedPatternFinder(pattern, haystack, 4);

        HaystackDescriptor haystackDescriptor = patternFinder.findFirst();
        Assert.assertEquals(1, haystackDescriptor.getUnmatchedRangesCount());
        Assert.assertEquals(1, haystackDescriptor.getRangesCount());

        FixedPatternFinder allPatternFInder = new FixedPatternFinder(pattern, haystack, 4);
        haystackDescriptor = allPatternFInder.findAll();
        Assert.assertEquals(1, haystackDescriptor.getUnmatchedRangesCount());
        Assert.assertEquals(1, haystackDescriptor.getRangesCount());

        patternFinder = new FixedPatternFinder(pattern, haystack, 3);
        haystackDescriptor = patternFinder.findAll();
        Assert.assertEquals(3, haystackDescriptor.getRangesCount());
        Assert.assertEquals(2, haystackDescriptor.getUnmatchedRangesCount());
        Assert.assertEquals(1, haystackDescriptor.getMatchedRangesCount());
    }

    private static HaystackDescriptor findFirst(byte[] patternData, byte[] haystackData) {
        RRawVector haystack = RDataFactory.createRawVector(haystackData);
        RRawVector pattern = RDataFactory.createRawVector(patternData);
        FixedPatternFinder patternFinder = new FixedPatternFinder(pattern, haystack);
        return patternFinder.findFirst();
    }

    private static HaystackDescriptor findAll(byte[] patternData, byte[] haystackData) {
        RRawVector haystack = RDataFactory.createRawVector(haystackData);
        RRawVector pattern = RDataFactory.createRawVector(patternData);
        FixedPatternFinder patternFinder = new FixedPatternFinder(pattern, haystack);
        return patternFinder.findAll();
    }
}
