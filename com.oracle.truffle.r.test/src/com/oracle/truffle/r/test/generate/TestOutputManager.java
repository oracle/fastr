/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.generate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.test.TestBase.Context;
import com.oracle.truffle.r.test.TestBase.Ignored;
import com.oracle.truffle.r.test.TestBase.Output;
import com.oracle.truffle.r.test.TestTrait;
import com.oracle.truffle.r.test.WhiteList;

/**
 * Supports the management of expected test output.
 *
 * In the normal mode of operation (i.e. during unit testing) an existing file of expected outputs
 * is read and an optimized lookup map is created for use by {@link #getOutput(String)}.
 *
 */
public class TestOutputManager {

    /**
     * Tests are recorded in a map keyed by the test input. An instance of this class is the value
     * associated with the key.
     */
    public static class TestInfo {
        /**
         * Name of the element containing the test, i,e., the method annotated with {@code @Test}.
         */
        private String elementName;
        /**
         * The output, i.e., as produced by the associated R session.
         */
        public final String output;
        /**
         * This is initially set to {@code false} when reading an existing expected output file.
         * When analyzing the test classes, if a test matches, this value is set to {@true}.
         * Therefore, after test class analysis, any tests with {@code inCode == false}, must have
         * been deleted, and will be removed from the updated file.
         */
        private boolean inCode;

        /**
         * The {@link TestTrait}s associated with this test.
         */
        private TestTrait[] traits;

        public boolean inCode() {
            return inCode;
        }

        void setInCode() {
            inCode = true;
        }

        public String elementName() {
            return elementName;
        }

        void setElementName(String elementName) {
            this.elementName = elementName;
        }

        public TestTrait[] testTraits() {
            return traits;
        }

        void setTestTraits(TestTrait[] traits) {
            this.traits = traits;
        }

        public TestInfo(String elementName, String expected, final boolean inCode, TestTrait... traits) {
            this.elementName = elementName;
            this.output = expected;
            this.inCode = inCode;
            this.traits = traits;
        }
    }

    public static final String TEST_EXPECTED_OUTPUT_FILE = "ExpectedTestOutput.test";
    public static final String TEST_FASTR_OUTPUT_FILE = "FastRTestOutput.test";
    public static final String TEST_DIFF_OUTPUT_FILE = "DiffTestOutput.test";

    /**
     * Maps inputs to expected outputs, used during generation.
     */
    private final SortedMap<String, SortedMap<String, TestInfo>> testMaps = new TreeMap<>();
    /**
     * A fast lookup map used at runtime to located the expected output.
     */
    private Map<String, String> runtimeTestMap;
    /**
     * Represents a remote GNU R session.
     */
    private RSession rSession;

    /**
     * Used to get the (expected) output for a test in generation mode,may be {@code null}.
     */
    private String rSessionName;

    /**
     * The file containing the associated test output (being read or being generated).
     */
    public final File outputFile;

    public TestOutputManager(File outputFile) {
        this.outputFile = outputFile;
    }

    protected void setRSession(RSession session) {
        this.rSession = session;
        this.rSessionName = session.name();
    }

    public RSession getRSession() {
        return rSession;
    }

    /**
     * Associates a name with the manager, for the case where {@link #rSession} is unset.
     */
    protected void setRSessionName(String name) {
        this.rSessionName = name;
    }

    /**
     * Lookup the expected output in the fast map. If either the fast map does not exist or the
     * lookup fails, return {@code null}.
     */
    public String getOutput(String input) {
        if (runtimeTestMap != null) {
            return runtimeTestMap.get(input);
        } else {
            return null;
        }
    }

    /**
     * Create a fast lookup map from an existing set of maps read from a file.
     */
    private Map<String, String> createFastLookupMap() {
        runtimeTestMap = new HashMap<>();
        for (Map<String, TestInfo> testMap : testMaps.values()) {
            for (Map.Entry<String, TestInfo> entrySet : testMap.entrySet()) {
                TestInfo testInfo = entrySet.getValue();
                runtimeTestMap.put(entrySet.getKey(), testInfo.output);
            }
        }
        return runtimeTestMap;
    }

    private static class SaveBufferedReader extends BufferedReader {
        StringBuffer save;

        SaveBufferedReader(Reader in, StringBuffer save) {
            super(in);
            this.save = save;
        }

        @Override
        public String readLine() throws IOException {
            String line = super.readLine();
            if (line != null) {
                save.append(line).append('\n');
            }
            return line;
        }
    }

    private static String escapeTestInput(String input) {
        return input.replace("\n", "<<<NEWLINE>>>");
    }

    private static String unescapeTestInput(String input) {
        return input.replace("<<<NEWLINE>>>", "\n");
    }

    public String readTestOutputFile() throws IOException {
        if (!outputFile.exists()) {
            return null;
        }
        StringBuffer content = new StringBuffer();
        try (SaveBufferedReader in = new SaveBufferedReader(new FileReader(outputFile), content)) {
            // line format for element name: ##elementName
            // line format for input lines: #input
            // output lines do not start with ##
            String line = in.readLine();
            while (true) {
                if (line == null) {
                    break;
                }
                if (!line.startsWith("##")) {
                    throw new IOException("expected line to start with ##");
                }
                String[] elementNameAndTraits = line.substring(2).split("#");
                String elementName = elementNameAndTraits[0];
                TestTrait[] traits = new TestTrait[0];
                if (elementNameAndTraits.length > 1) {
                    traits = new TestTrait[elementNameAndTraits.length - 1];
                    for (int i = 1; i < elementNameAndTraits.length; i++) {
                        String traitClass = elementNameAndTraits[i];
                        String[] traitParts = traitClass.split("\\.");
                        // no reflection (AOT)
                        TestTrait trait = null;
                        switch (traitParts[0]) {
                            case "Ignored":
                                trait = Ignored.valueOf(traitParts[1]);
                                break;
                            case "Output":
                                trait = Output.valueOf(traitParts[1]);
                                break;
                            case "Context":
                                trait = Context.valueOf(traitParts[1]);
                                break;
                            case "WhiteList":
                                trait = WhiteList.create(traitParts[1]);
                                break;
                            default:
                                System.err.println("unrecognized TestTrait: " + traitClass);
                        }
                        traits[i - 1] = trait;
                    }
                }
                line = in.readLine();
                if (!line.startsWith("#")) {
                    throw new IOException("expected line to start with #");
                }
                String input = unescapeTestInput(line.substring(1));
                StringBuilder output = new StringBuilder();
                while (true) {
                    line = in.readLine();
                    if (line == null || line.startsWith("##")) {
                        break;
                    }
                    output.append(line).append('\n');
                }
                output.deleteCharAt(output.length() - 1);
                Map<String, TestInfo> testMap = getTestMap(elementName);
                testMap.put(input, new TestInfo(elementName, output.toString(), false, traits));
            }
        }
        createFastLookupMap();
        return content.toString();
    }

    /**
     * Writes the contents of {@link #testMaps} to {link #testOutputFile}.
     *
     * @param oldContent can be passed in to avoid the file update if the content would not change
     * @param checkOnly does not update the file but returns {@code true} if it would be updated. In
     *            this case, {@code oldContent} must be provided.
     *
     * @return {@code true} if the file was updated
     */
    public boolean writeTestOutputFile(String oldContent, boolean checkOnly) throws IOException {
        StringWriter swr = new StringWriter();
        PrintWriter prSwr = new PrintWriter(swr);
        for (Map<String, TestInfo> testMap : testMaps.values()) {
            for (Map.Entry<String, TestInfo> entrySet : testMap.entrySet()) {
                TestInfo testInfo = entrySet.getValue();
                if (testInfo.inCode) {
                    prSwr.printf("##%s#", testInfo.elementName);
                    for (TestTrait trait : testInfo.traits) {
                        prSwr.printf("%s.%s#", trait.getClass().getSimpleName(), trait.getName());
                    }
                    prSwr.println();
                    prSwr.printf("#%s%n", escapeTestInput(entrySet.getKey()));
                    prSwr.println(testInfo.output);
                }
            }
        }
        String newContent = swr.getBuffer().toString();
        if (oldContent == null || !newContent.equals(oldContent)) {
            if (!checkOnly) {
                try (BufferedWriter wr = new BufferedWriter(new FileWriter(outputFile))) {
                    wr.write(newContent);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Similar to {@link #writeTestOutputFile} but writes the differences between data in two
     * managers, the first of which is the "expected" map. The expectation is that the second map
     * contains the same keys but with possibly different values, and may have some entries missing,
     * but never any keys not in the "expected" map. Returns true if there are no differences.
     */
    public static boolean writeDiffsTestOutputFile(File testOutputFile, TestOutputManager a, TestOutputManager b) throws IOException {
        StringWriter swr = new StringWriter();
        PrintWriter prSwr = new PrintWriter(swr);
        boolean matches = true;
        for (Map<String, TestInfo> aTestMap : a.testMaps.values()) {
            for (Map.Entry<String, TestInfo> entrySet : aTestMap.entrySet()) {
                TestInfo aTestInfo = entrySet.getValue();
                TestInfo bTestInfo = b.find(aTestInfo.elementName, entrySet.getKey());
                if (bTestInfo != null && aTestInfo.output.equals(bTestInfo.output)) {
                    continue;
                }
                matches = false;
                prSwr.printf("##%s%n", aTestInfo.elementName);
                prSwr.printf("#%s%n", escapeTestInput(entrySet.getKey()));
                prSwr.printf("#%s%n", a.rSessionName);
                prSwr.println(aTestInfo.output);
                prSwr.printf("#%s%n", b.rSessionName);
                if (bTestInfo != null) {
                    prSwr.println(bTestInfo.output);
                } else {
                    prSwr.println("MISSING");
                }
            }
        }
        if (!matches) {
            try (BufferedWriter wr = new BufferedWriter(new FileWriter(testOutputFile))) {
                wr.write(swr.getBuffer().toString());
            }
        }
        return matches;
    }

    /**
     * Helper method to locate a test with key {@code testId} in a given element map.
     */
    private TestInfo find(String elementName, String testId) {
        Map<String, TestInfo> testMap = testMaps.get(elementName);
        if (testMap != null) {
            TestInfo bTestInfo = testMap.get(testId);
            if (bTestInfo != null) {
                // found element
                return bTestInfo;
            }
        }
        return null;
    }

    /**
     * May be called at runtime to add a test result.
     *
     * @param keepTrailingWhiteSpace TODO
     */
    public void addTestResult(String testElementName, String input, String result, boolean keepTrailingWhiteSpace) {
        SortedMap<String, TestInfo> testMap = getTestMap(testElementName);
        testMap.put(input, new TestInfo(testElementName, prepareResult(result, keepTrailingWhiteSpace), true));
    }

    public interface DiagnosticHandler {
        void warning(String msg);

        void note(String msg);

        void error(String msg);
    }

    /**
     * Generate a test result using GnuR.
     *
     * @param testElementName identification of the annotated test element, i.e.,
     *            {@code class.testmethod}.
     * @param test R test string
     * @param d handler for diagnostics
     * @param checkOnly if {@code true} do not invoke GnuR, just update map
     * @param keepTrailingWhiteSpace if {@code true} preserve trailing white space, otherwise trim
     * @return the GnuR output
     */
    public String genTestResult(String testElementName, String test, DiagnosticHandler d, boolean checkOnly, boolean keepTrailingWhiteSpace, TestTrait... traits) {
        Map<String, TestInfo> testMap = getTestMap(testElementName);
        TestInfo testInfo = testMap.get(test);
        if (testInfo != null) {
            if (testInfo.inCode()) {
                // we have already seen this test - duplicates are harmless but we warn about it
                d.warning("test '" + test + "' is duplicated in " + testInfo.elementName() + " and " + testElementName);
            }
            testInfo.setInCode();
            testInfo.setTestTraits(traits);
            testInfo.setElementName(testElementName);
            return testInfo.output;
        } else {
            d.note("test file does not contain: " + test);
            String expected = null;
            if (!checkOnly) {
                try {
                    expected = rSession.eval(null, test, null, false);
                } catch (Throwable e) {
                    throw RInternalError.shouldNotReachHere("unexpected exception thrown by GNUR session: " + e);
                }
                expected = prepareResult(expected, keepTrailingWhiteSpace);
            }
            testMap.put(test, new TestInfo(testElementName, expected, true, traits));
            return expected;
        }
    }

    public SortedMap<String, TestInfo> getTestMap(String elementName) {
        SortedMap<String, TestInfo> testMap = testMaps.get(elementName);
        if (testMap == null) {
            testMap = new TreeMap<>();
            testMaps.put(elementName, testMap);
        }
        return testMap;
    }

    public SortedMap<String, SortedMap<String, TestInfo>> getTestMaps() {
        return testMaps;
    }

    public Map<String, String> getRuntimeMap() {
        return runtimeTestMap;
    }

    /**
     * Join {@code arrays} removing duplicates.
     */
    public static String[] join(String[]... arrays) {
        Set<String> set = new HashSet<>();
        for (String[] s : arrays) {
            set.addAll(Arrays.asList(s));
        }
        return set.toArray(new String[set.size()]);
    }

    public static String[] template(String template, String[]... parameters) {
        int resultLength = 1;
        for (String[] param : parameters) {
            resultLength *= param.length;
        }
        String[] result = new String[resultLength];
        int index = 0;
        int[] positions = new int[parameters.length];
        while (index < result.length) {
            String currentString = template;
            for (int i = 0; i < parameters.length; i++) {
                int currentPos = positions[i];
                currentString = currentString.replace("%" + i, parameters[i][currentPos]);
            }
            result[index] = currentString;
            index++;

            for (int i = 0; i < parameters.length; i++) {
                positions[i]++;
                if (positions[i] == parameters[i].length) {
                    positions[i] = 0;
                } else {
                    break;
                }
            }
        }

        return result;
    }

    public static String prepareResult(String s, boolean keepTrailingWhiteSpace) {
        if (keepTrailingWhiteSpace) {
            return s;
        }
        int len = s.length();
        if (len == 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        int ix = 0;
        while (ix < len) {
            int ixnl = s.indexOf('\n', ix);

            int ixns = ixnl < 0 ? len - 1 : ixnl - 1;
            if (ixns >= ix) {
                while (ixns >= ix) {
                    if (s.charAt(ixns) != ' ') {
                        break;
                    }
                    ixns--;
                }
                sb.append(s.substring(ix, ixns + 1));
            }
            if (ixnl >= 0) {
                sb.append('\n');
                ixnl++;
                ix = ixnl;
            } else {
                ix = len;
                break;
            }
        }
        sb.append(s.substring(ix));
        return sb.toString();
    }
}
