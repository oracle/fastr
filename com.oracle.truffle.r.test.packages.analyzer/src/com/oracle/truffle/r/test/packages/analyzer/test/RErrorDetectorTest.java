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
package com.oracle.truffle.r.test.packages.analyzer.test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.r.test.packages.analyzer.Location;
import com.oracle.truffle.r.test.packages.analyzer.Problem;
import com.oracle.truffle.r.test.packages.analyzer.detectors.RErrorDetector;
import com.oracle.truffle.r.test.packages.analyzer.model.RPackage;
import com.oracle.truffle.r.test.packages.analyzer.model.RPackageTestRun;

public class RErrorDetectorTest {

    private static final RPackage pkg;
    private static final RPackageTestRun pkgTestRun;

    static {
        pkg = new RPackage("fastr", "0.27");
        pkg.setLocation(Paths.get("fastr"));
        pkgTestRun = new RPackageTestRun(pkg, 1);
    }

    @Test
    public void testMultiLine() {
        List<String> lines = Arrays.asList(new String[]{"Error in check(options) : ",
                        "ERROR: installing Rd objects failed for package ‘RUnit’"});

        Collection<Problem> detect = RErrorDetector.INSTANCE.detect(pkgTestRun, loc(), lines);
        Assert.assertEquals(1, detect.size());

        Optional<Problem> findFirst = detect.stream().findFirst();
        Assert.assertEquals("Error in check(options)", findFirst.orElse(null).getSummary().trim());
        Assert.assertEquals("ERROR: installing Rd objects failed for package ‘RUnit’", findFirst.orElse(null).getDetails().trim());

    }

    @Test
    public void testSingleLine0() {
        List<String> lines = Collections.singletonList("Error in check(options) : invalid value for 'label' ");

        Collection<Problem> detect = RErrorDetector.INSTANCE.detect(pkgTestRun, loc(), lines);
        Assert.assertEquals(1, detect.size());

        Optional<Problem> findFirst = detect.stream().findFirst();
        Assert.assertEquals("Error in check(options)", findFirst.orElse(null).getSummary().trim());
        Assert.assertEquals("invalid value for 'label'", findFirst.orElse(null).getDetails().trim());

    }

    @Test
    public void testSingleLine1() {
        List<String> lines = Collections.singletonList("Error in check : invalid value for 'label' ");

        Collection<Problem> detect = RErrorDetector.INSTANCE.detect(pkgTestRun, loc(), lines);
        Assert.assertEquals(1, detect.size());

        Optional<Problem> findFirst = detect.stream().findFirst();
        Assert.assertEquals("Error in check", findFirst.orElse(null).getSummary().trim());
        Assert.assertEquals("invalid value for 'label'", findFirst.orElse(null).getDetails().trim());

    }

    @Test
    public void testSingleLineMultipleColon() {
        List<String> lines = Collections.singletonList("Error in check(options) : invalid value for 'label' : ");

        Collection<Problem> detect = RErrorDetector.INSTANCE.detect(pkgTestRun, loc(), lines);
        Assert.assertEquals(1, detect.size());

        Optional<Problem> findFirst = detect.stream().findFirst();
        Assert.assertEquals("Error in check(options)", findFirst.orElse(null).getSummary().trim());
        Assert.assertEquals("invalid value for 'label' :", findFirst.orElse(null).getDetails().trim());

    }

    @Test
    public void testSingleLineWithoutCallstring0() {
        List<String> lines = Collections.singletonList("Error: invalid value for 'label' ");

        Collection<Problem> detect = RErrorDetector.INSTANCE.detect(pkgTestRun, loc(), lines);
        Assert.assertEquals(1, detect.size());

        Optional<Problem> findFirst = detect.stream().findFirst();
        Assert.assertEquals("Error", findFirst.orElse(null).getSummary().trim());
        Assert.assertEquals("invalid value for 'label'", findFirst.orElse(null).getDetails().trim());

    }

    @Test
    public void testSingleLineWithoutCallstring1() {
        List<String> lines = Collections.singletonList("Error: invalid value for 'label' : ");

        Collection<Problem> detect = RErrorDetector.INSTANCE.detect(pkgTestRun, loc(), lines);
        Assert.assertEquals(1, detect.size());

        Optional<Problem> findFirst = detect.stream().findFirst();
        Assert.assertEquals("Error", findFirst.orElse(null).getSummary().trim());
        Assert.assertEquals("invalid value for 'label' :", findFirst.orElse(null).getDetails().trim());
    }

    @Test
    public void testRInternalError0() {
        List<String> lines = Collections.singletonList("RInternalError: invalid value for 'label'");

        Collection<Problem> detect = RErrorDetector.INSTANCE.detect(pkgTestRun, loc(), lines);
        Assert.assertEquals(0, detect.size());
    }

    @Test
    public void testRInternalError1() {
        List<String> lines = Collections.singletonList("> RInternalError: invalid value for 'label'");

        Collection<Problem> detect = RErrorDetector.INSTANCE.detect(pkgTestRun, loc(), lines);
        Assert.assertEquals(0, detect.size());
    }

    @Test
    public void testRInternalError2() {
        List<String> lines = Collections.singletonList("  RInternalError: invalid value for 'label'");

        Collection<Problem> detect = RErrorDetector.INSTANCE.detect(pkgTestRun, loc(), lines);
        Assert.assertEquals(0, detect.size());
    }

    @Test
    public void testWithLinePrefix() {
        List<String> lines = Collections.singletonList("> Error: invalid value for 'label'");

        Collection<Problem> detect = RErrorDetector.INSTANCE.detect(pkgTestRun, loc(), lines);
        Assert.assertEquals(1, detect.size());

        Optional<Problem> findFirst = detect.stream().findFirst();
        Assert.assertEquals("Error", findFirst.orElse(null).getSummary().trim());
        Assert.assertEquals("invalid value for 'label'", findFirst.orElse(null).getDetails().trim());
    }

    @Test
    public void testCallstringWithNamesAndValues0() {
        List<String> lines = Arrays.asList(new String[]{"Error in grep(pattern, all.names, value = TRUE) : ",
                        "  invalid regular expression '*': Dangling meta character '*' near index 0"});

        Collection<Problem> detect = RErrorDetector.INSTANCE.detect(pkgTestRun, loc(), lines);
        Assert.assertEquals(1, detect.size());

        Optional<Problem> findFirst = detect.stream().findFirst();
        Assert.assertEquals("Error in grep(pattern, all.names, value = TRUE)", findFirst.orElse(null).getSummary().trim());
        Assert.assertEquals("invalid regular expression '*': Dangling meta character '*' near index 0", findFirst.orElse(null).getDetails().trim());
    }

    @Test
    public void testCallstringWithNamesAndValues1() {
        List<String> lines = Arrays.asList(new String[]{"Error in grep(pattern, all.names, value = \":\") : ",
                        "  invalid regular expression '*': Dangling meta character '*' near index 0"});

        Collection<Problem> detect = RErrorDetector.INSTANCE.detect(pkgTestRun, loc(), lines);
        Assert.assertEquals(1, detect.size());

        Optional<Problem> findFirst = detect.stream().findFirst();
        Assert.assertEquals("Error in grep(pattern, all.names, value = \":\")", findFirst.orElse(null).getSummary().trim());
        Assert.assertEquals("invalid regular expression '*': Dangling meta character '*' near index 0", findFirst.orElse(null).getDetails().trim());
    }

    private static Location loc() {
        return new Location(pkg.getLocation(), 0);
    }

}
