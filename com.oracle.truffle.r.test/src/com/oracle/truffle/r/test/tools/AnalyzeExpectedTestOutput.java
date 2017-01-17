/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;

import com.oracle.truffle.r.test.TestBase.Output;
import com.oracle.truffle.r.test.TestTrait;
import com.oracle.truffle.r.test.WhiteList;
import com.oracle.truffle.r.test.generate.TestOutputManager;
import com.oracle.truffle.r.test.generate.TestOutputManager.TestInfo;
import com.oracle.truffle.r.test.library.base.TestSimpleValues;

/**
 * Compares several "expected" test output files and reports differences. Typically used to
 * determine differences between operating system environments or R versions.
 *
 * When {@code --arith-whitelist} is set, the first file is taken as providing the definitive test
 * output that will be used in unit tests, and the second file is assumed to be the FastR output.
 * This is intended to handle differences in binary arithmetic operations and generates a list of
 * entries of the form: "test, file 0 output, file 1 output" that can be plugged into
 * {@link TestSimpleValues}.
 */
public class AnalyzeExpectedTestOutput {

    public static void main(String[] args) {
        if (args.length < 2) {
            usage();
        }
        ArrayList<String> fileList = new ArrayList<>();
        boolean arithWhiteList = false;
        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                // options maybe
                if (arg.equals("--arith-whitelist")) {
                    arithWhiteList = true;
                }
            } else {
                fileList.add(arg);
            }
            i++;
        }
        try {
            String[] files = new String[fileList.size()];
            fileList.toArray(files);
            TestOutputManager[] toms = new TestOutputManager[files.length];
            for (i = 0; i < files.length; i++) {
                toms[i] = new TestOutputManager(new File(files[i]));
                toms[i].readTestOutputFile();
            }
            // compare and report differences
            ArrayList<WhiteListInfo> whiteListInfo = compare(toms, files);

            if (arithWhiteList) {
                TestOutputManager fastrTOM = toms[1];
                Map<String, String> fastrMap = fastrTOM.getRuntimeMap();
                for (WhiteListInfo info : whiteListInfo) {
                    String expected = info.expected.replace("\n", "\\n");
                    String fastr = fastrMap.get(info.test);
                    if (fastr != null && info.name.equals("arithmetic") && !expected.contains("Error")) {
                        fastr = fastr.replace("\n", "\\n");
                        if (!fastr.equals(expected)) {
                            String fastrExpected = fastrMap.get(info.test).replace("\n", "\\n");
                            System.out.printf("WHITELIST.add(\"%s\", \"%s\", \"%s\");%n", info.test, fastrExpected, expected);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    private static class WhiteListInfo {
        private final String name;
        private final String test;
        private final String expected;

        WhiteListInfo(String name, String test, String expected) {
            this.name = name;
            this.test = test;
            this.expected = expected;
        }
    }

    private static void usage() {
        System.out.println("usage: [-arith-whitelist] file1 file2");
        System.exit(1);
    }

    private static ArrayList<WhiteListInfo> compare(TestOutputManager[] toms, String[] files) {
        ArrayList<WhiteListInfo> misMatchedTests = new ArrayList<>();
        // assume the maps ran on the same set of inputs and use toms[0] as iterator
        SortedMap<String, SortedMap<String, TestInfo>> m0 = toms[0].getTestMaps();
        ArrayList<SortedMap<String, SortedMap<String, TestInfo>>> subMaps = new ArrayList<>();
        System.out.printf("file 0: %s\n", files[0]);
        for (int i = 1; i < toms.length; i++) {
            subMaps.add(toms[i].getTestMaps());
            System.out.printf("file %d: %s\n", i, files[i]);
        }
        System.out.println();

        for (Map.Entry<String, SortedMap<String, TestInfo>> m1Entry : m0.entrySet()) {
            String m0EntryKey = m1Entry.getKey();
            SortedMap<String, TestInfo> m0Value = m1Entry.getValue();

            for (Map.Entry<String, TestInfo> entrySet : m0Value.entrySet()) {
                String test = entrySet.getKey();
                TestInfo testInfo = entrySet.getValue();
                boolean misMatch = false;
                WhiteList[] whiteLists = TestTrait.collect(testInfo.testTraits(), WhiteList.class);
                for (int i = 0; i < subMaps.size(); i++) {
                    Map<String, TestInfo> subMap = subMaps.get(i).get(m0EntryKey);
                    if (subMap == null) {
                        System.out.printf("testMap %s not found in file %d%n", m0EntryKey, i + 1);
                        continue;
                    }
                    TestInfo subMapTestInfo = subMap.get(test);
                    if (subMapTestInfo == null) {
                        System.out.printf("test %s not found in file %d%n", test, i + 1);
                        continue;
                    }
                    String subMapExpected = subMapTestInfo.output;
                    if (mismatch(testInfo, subMapTestInfo)) {
                        // if already seen a mismatch (> 2 files) don't log test info
                        if (!misMatch) {
                            System.out.printf("##%s#", m0EntryKey);
                            for (TestTrait trait : testInfo.testTraits()) {
                                System.out.printf("%s.%s#", trait.getClass().getSimpleName(), trait.getName());
                            }
                            System.out.println();
                            System.out.printf("#%s%n", test);
                            System.out.println("file 0:");
                            System.out.println(testInfo.output);
                            // record the difference
                            if (whiteLists.length != 0 && whiteLists[0].getName().equals("arithmetic")) {
                                misMatchedTests.add(new WhiteListInfo("arithmetic", test, testInfo.output));
                            }
                            misMatch = true;
                        }
                        System.out.printf("file %d:%n", i + 1);
                        System.out.println(subMapExpected);
                    }
                }
            }
        }
        return misMatchedTests;
    }

    private static boolean mismatch(TestInfo expectedInfo, TestInfo actualInfo) {
        // some things are never equal
        if (TestTrait.contains(expectedInfo.testTraits(), Output.ContainsReferences)) {
            return false;
        }
        return !expectedInfo.output.equals(actualInfo.output);
    }
}
