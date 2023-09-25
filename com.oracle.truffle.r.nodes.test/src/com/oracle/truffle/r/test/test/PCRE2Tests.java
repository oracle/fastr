/*
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.test.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.r.runtime.ffi.PCRE2RFFI;
import com.oracle.truffle.r.runtime.ffi.PCRE2RFFI.IndexRange;
import com.oracle.truffle.r.runtime.ffi.PCRE2RFFI.MatchData;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

@RunWith(Theories.class)
public class PCRE2Tests extends TestBase {
    private TestRootNode testRootNode;
    private PCRE2RFFI.CompileNode compileNode;
    private PCRE2RFFI.MatchNode matchNode;
    private PCRE2RFFI.MemoryReleaseNode memoryReleaseNode;
    private PCRE2RFFI.GetCaptureNamesNode captureNamesNode;
    private PCRE2RFFI.GetCaptureCountNode captureCountNode;
    private InteropLibrary interop;

    private static class TestData {
        public String pattern;
        public String subject;
        public int[] expectedMatchIndexes;
        private final Map<Integer, List<IndexRange>> expectedCaptureMatches = new HashMap<>();
        private String[] expectedCaptureNames;

        static Builder builder() {
            return new Builder();
        }

        boolean hasCaptures() {
            return expectedCaptureMatches.size() > 1;
        }

        @Override
        public String toString() {
            return "TestData{" +
                            "pattern='" + pattern + '\'' +
                            ", subject='" + subject + '\'' +
                            '}';
        }

        private static class Builder {
            private final TestData testData = new TestData();

            public TestData build() {
                assert testData.pattern != null;
                assert testData.subject != null;
                assert testData.expectedMatchIndexes != null;
                if (testData.hasCaptures()) {
                    assert testData.expectedCaptureNames != null;
                    Set<Integer> captureIndexes = testData.expectedCaptureMatches.keySet();
                    for (int i = 0; i < testData.expectedCaptureNames.length; i++) {
                        assert captureIndexes.contains(i);
                    }
                    int captureMatchesSize = 0;
                    for (List<IndexRange> captureMatches : testData.expectedCaptureMatches.values()) {
                        if (captureMatchesSize == 0) {
                            captureMatchesSize = captureMatches.size();
                        }
                        // All matches of all the captures should have the same size.
                        assert captureMatches.size() == captureMatchesSize;
                    }
                }
                return testData;
            }

            public Builder pattern(String pattern) {
                testData.pattern = pattern;
                return this;
            }

            public Builder subject(String subject) {
                testData.subject = subject;
                return this;
            }

            public Builder expectedMatchIndexes(int[] matchIndexes) {
                testData.expectedMatchIndexes = matchIndexes;
                return this;
            }

            /**
             * @param captureIdx Capture indexes as tracked by PCRE. Captures are numbered from 0 to
             *            N-1 where N is the total capture count (as returned by
             *            {@link com.oracle.truffle.r.runtime.ffi.PCRE2RFFI.GetCaptureCountNode}.
             * @param captureIndexes Indexes of the capture matches.
             */
            public Builder expectedCaptureIndexes(int captureIdx, int[] captureIndexes) {
                assert captureIndexes.length % 2 == 0;
                if (!testData.expectedCaptureMatches.containsKey(captureIdx)) {
                    testData.expectedCaptureMatches.put(captureIdx, new ArrayList<>());
                }
                List<IndexRange> captureMatches = testData.expectedCaptureMatches.get(captureIdx);
                for (int i = 0; i < captureIndexes.length; i += 2) {
                    IndexRange indexRange = new IndexRange(captureIndexes[i], captureIndexes[i + 1]);
                    captureMatches.add(indexRange);
                }
                return this;
            }

            public Builder expectedCaptureNames(String[] captureNames) {
                testData.expectedCaptureNames = captureNames;
                return this;
            }
        }
    }

    private static class TestRootNode extends RootNode {
        TestRootNode() {
            super(TruffleRLanguage.getCurrentLanguage());
        }

        void insertChildren(Node[] children) {
            insert(children);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            throw new AssertionError("should not reach here");
        }
    }

    /**
     * Expected data roughly corresponds to the output of some regular expression tester, like
     * <a href="https://regex101.com/">regex101</a>.
     */
    // @formatter:off
    // Checkstyle: stop
    @DataPoints
    public static TestData[] testData = {
            TestData.builder().pattern("X").subject("aaa").expectedMatchIndexes(new int[]{}).build(),
            TestData.builder().pattern("X").subject("aXa").expectedMatchIndexes(new int[]{1, 2}).build(),
            TestData.builder().pattern("[a-z]+").subject("abc123").expectedMatchIndexes(new int[]{0, 3}).build(),
            TestData.builder().pattern("([a-z]+)").subject("abc123").
                    expectedMatchIndexes(new int[]{0, 3}).
                    expectedCaptureIndexes(0, new int[]{0, 3}).
                    expectedCaptureNames(new String[]{null}).
                    build(),
            TestData.builder().pattern(".*").subject("X")
                    .expectedMatchIndexes(new int[]{0,1, 1, 1})
                    .build(),
            TestData.builder().pattern("(?P<word>[a-z]+)").subject("abc123").
                    expectedMatchIndexes(new int[]{0, 3}).
                    expectedCaptureIndexes(0, new int[]{0, 3}).
                    expectedCaptureNames(new String[]{"word"}).
                    build(),
            TestData.builder().pattern("(?P<word>[a-z]+)(?P<num>[0-9]+)").subject("abc123").
                    expectedMatchIndexes(new int[]{0, 6}).
                    expectedCaptureIndexes(0, new int[]{0, 3}).
                    expectedCaptureIndexes(1, new int[]{3, 6}).
                    expectedCaptureNames(new String[]{"word", "num"}).
                    build(),
            TestData.builder().pattern("(?<first>[[:upper:]][[:lower:]]+) (?<last>[[:upper:]][[:lower:]]+)").
                    subject("  Ben Franklin and Jefferson Davis").
                    expectedMatchIndexes(new int[]{2, 14, 19, 34}).
                    expectedCaptureIndexes(0, new int[]{
                            2, 5, // "Ben"
                            19, 28  // "Jefferson"
                    }).
                    expectedCaptureIndexes(1, new int[]{
                            6, 14, // "Franklin"
                            29, 34 // "Davis"
                    }).
                    expectedCaptureNames(new String[]{"first", "last"}).
                    build(),
            TestData.builder().pattern("^((.*))$").subject("A1").
                    expectedMatchIndexes(new int[]{0, 2}).
                                    // Two captures at the same indexes.
                    expectedCaptureIndexes(0, new int[]{0, 2}).
                    expectedCaptureIndexes(1, new int[]{0, 2}).
                    expectedCaptureNames(new String[]{null, null}).
                    build(),
            TestData.builder().pattern("((?P<word>[a-z]+))").subject("abc").
                    expectedMatchIndexes(new int[]{0, 3}).
                    expectedCaptureIndexes(0, new int[]{0, 3}).
                    expectedCaptureIndexes(1, new int[]{0, 3}).
                    expectedCaptureNames(new String[]{null, "word"}).
                    build(),
            TestData.builder().pattern("(a)(?P<capt1>b)").subject("ab").
                    expectedMatchIndexes(new int[]{0, 2}).
                    expectedCaptureIndexes(0, new int[]{0, 1}).
                    expectedCaptureIndexes(1, new int[]{1, 2}).
                    expectedCaptureNames(new String[]{null, "capt1"}).
                    build(),
            TestData.builder().pattern("([a-z])").subject("ab").
                    expectedMatchIndexes(new int[]{0, 1, 1, 2}).
                    expectedCaptureIndexes(0, new int[]{0, 1, 1, 2}).
                    expectedCaptureNames(new String[]{null}).
                    build(),
            // UTF-8 tests
            TestData.builder().pattern(".*").subject("á").
                    expectedMatchIndexes(new int[]{0, 1, 1, 1}).
                    build(),
            TestData.builder().pattern("(a)|(b)").subject("b").
                    expectedMatchIndexes(new int[]{0, 1}).
                    // PCRE2 does not report the first capture at all, so we have to replace it with (0,0).
                    // i.e. the capture with index 0 does not have any matches.
                    expectedCaptureIndexes(0, new int[]{0, 0}).
                    expectedCaptureIndexes(1, new int[]{0, 1}).
                    expectedCaptureNames(new String[]{null, null}).
                    build(),
            TestData.builder().pattern("(a)|(b)").subject("a").
                    expectedMatchIndexes(new int[]{0, 1}).
                    expectedCaptureIndexes(0, new int[]{0, 1}).
                    expectedCaptureIndexes(1, new int[]{0, 0}).
                    expectedCaptureNames(new String[]{null, null}).
                    build(),
            // Both pattern and subject are Unicode characters
            TestData.builder().pattern("[⚽]").subject("─")
                    .expectedMatchIndexes(new int[]{})
                    .build(),
            TestData.builder().pattern("[⚽]").subject("a")
                    .expectedMatchIndexes(new int[]{})
                    .build(),
    };
    // @formatter:on
    // Checkstyle: resume

    @Before
    public void init() {
        execInContext(() -> {
            compileNode = RFFIFactory.getPCRE2RFFI().createCompileNode();
            matchNode = RFFIFactory.getPCRE2RFFI().createMatchNode();
            memoryReleaseNode = RFFIFactory.getPCRE2RFFI().createMemoryReleaseNode();
            captureNamesNode = RFFIFactory.getPCRE2RFFI().createGetCaptureNamesNode();
            captureCountNode = RFFIFactory.getPCRE2RFFI().createGetCaptureCountNode();
            interop = InteropLibrary.getUncached();
            // Some of the nodes that we initialize in this method have to be adopted. Therefore,
            // we adopt them into this artificial root node.
            testRootNode = new TestRootNode();
            testRootNode.insertChildren(new Node[]{compileNode, matchNode, memoryReleaseNode, captureNamesNode, captureCountNode});
            return null;
        });
    }

    @Theory
    public void test(TestData testingData) {
        execInContext(() -> {
            Object compiledPattern = compilePattern(testingData.pattern);
            int captureCount = captureCountNode.execute(compiledPattern);
            String[] captureNames = captureNamesNode.execute(compiledPattern, captureCount);
            assertEquals(captureCount, captureNames.length);
            MatchData matchData = matchNode.execute(compiledPattern, testingData.subject, 0, false, captureCount);
            assertMatchIndexesEqual(testingData.expectedMatchIndexes, matchData);
            if (testingData.hasCaptures()) {
                assert testingData.expectedCaptureNames != null;
                assertCapturesEqual(testingData.expectedCaptureMatches, matchData);
                assertCaptureNamesEqual(testingData.expectedCaptureNames, captureNames);
            }
            freePattern(compiledPattern);
            return null;
        });
    }

    @Test
    public void testFailedPatternCompilationStar() {
        execInContext(() -> {
            String pattern = ".**";
            PCRE2RFFI.CompileResult compileResult = compileNode.execute(pattern, 0);
            assertNotEquals(0, compileResult.errorCode);
            assertNotNull(compileResult.errorMessage);
            assertEquals(2, compileResult.errOffset);
            return null;
        });
    }

    @Test
    public void testFailedPatternCompilationNoClosingBracket() {
        execInContext(() -> {
            String pattern = "abc[";
            PCRE2RFFI.CompileResult compileResult = compileNode.execute(pattern, 0);
            assertNotEquals(0, compileResult.errorCode);
            assertNotNull(compileResult.errorMessage);
            assertEquals(4, compileResult.errOffset);
            return null;
        });
    }

    /**
     * @param expected Start indexes and end indexes are intertwined ([start_idx1, end_idx1,
     *            start_idx2, end_idx2,...])
     */
    private static void assertMatchIndexesEqual(int[] expected, MatchData matchData) {
        assert expected.length % 2 == 0;
        List<IndexRange> matches = matchData.getMatches();
        assertEquals(matches.size(), matchData.getMatchCount());
        assertEquals(expected.length, matches.size() * 2);
        int expectedArrayIdx = 0;
        for (IndexRange match : matches) {
            assertEquals(expected[expectedArrayIdx], match.startIdx);
            assertEquals(expected[expectedArrayIdx + 1], match.endIdx);
            expectedArrayIdx += 2;
        }
    }

    private static void assertCapturesEqual(Map<Integer, List<IndexRange>> expectedCaptures, MatchData matchData) {
        Map<Integer, List<IndexRange>> captures = matchData.getCaptures();
        assertEquals(expectedCaptures.size(), captures.size());
        for (Map.Entry<Integer, List<IndexRange>> entry : expectedCaptures.entrySet()) {
            int captureIdx = entry.getKey();
            List<IndexRange> expectedCaptureMatches = entry.getValue();
            List<IndexRange> captureMatches = captures.get(captureIdx);
            Assert.assertArrayEquals(expectedCaptureMatches.toArray(), captureMatches.toArray());
        }
    }

    private static void assertCaptureNamesEqual(String[] expectedNames, String[] actualNames) {
        Assert.assertArrayEquals(expectedNames, actualNames);
    }

    private Object compilePattern(String pattern) {
        final Object[] compiledPattern = {null};
        execInContext(() -> {
            PCRE2RFFI.CompileResult compileResult = compileNode.execute(pattern, 0);
            Assert.assertFalse(interop.isNull(compileResult.compiledPattern));
            compiledPattern[0] = compileResult.compiledPattern;
            return null;
        });
        return compiledPattern[0];
    }

    private void freePattern(Object compiledPattern) {
        execInContext(() -> {
            memoryReleaseNode.execute(compiledPattern);
            return null;
        });
    }
}
