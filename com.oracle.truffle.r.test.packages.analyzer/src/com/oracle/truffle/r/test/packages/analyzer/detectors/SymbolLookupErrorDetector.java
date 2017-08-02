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
package com.oracle.truffle.r.test.packages.analyzer.detectors;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oracle.truffle.r.test.packages.analyzer.Location;
import com.oracle.truffle.r.test.packages.analyzer.Problem;
import com.oracle.truffle.r.test.packages.analyzer.model.RPackageTestRun;

public final class SymbolLookupErrorDetector extends LineDetector {

    public static final SymbolLookupErrorDetector INSTANCE = new SymbolLookupErrorDetector();

    private static final Pattern PATTERN = Pattern.compile("(.*): symbol lookup error: (.*): (?<MSG>.*)");

    private SymbolLookupErrorDetector() {
    }

    @Override
    public String getName() {
        return "Symbol lookup error detector";
    }

    @Override
    public Collection<Problem> detect(RPackageTestRun pkgTestRun, Location startLocation, List<String> body) {
        Collection<Problem> problems = new LinkedList<>();
        int lineNr = startLocation != null ? startLocation.lineNr : 0;
        for (String line : body) {
            Matcher matcher = PATTERN.matcher(line);
            if (matcher.matches()) {
                String message = matcher.group("MSG");
                problems.add(new SymbolLookupErrorProblem(pkgTestRun, this, new Location(startLocation.file, lineNr), message));

            }
            ++lineNr;
        }
        return problems;
    }

    public static class SymbolLookupErrorProblem extends Problem {

        private final String message;

        protected SymbolLookupErrorProblem(RPackageTestRun pkg, SymbolLookupErrorDetector detector, Location location, String message) {
            super(pkg, detector, location);
            this.message = message;
        }

        @Override
        public String toString() {
            return getLocation() + ": symbol lookup error " + message;
        }

        @Override
        public String getSummary() {
            return "symbol lookup error";
        }

        @Override
        public String getDetails() {
            return message;
        }

        @Override
        public int getSimilarityTo(Problem other) {
            if (other.getClass() == SymbolLookupErrorProblem.class) {
                return Problem.computeLevenshteinDistance(getDetails().trim(), other.getDetails().trim());
            }
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isSimilarTo(Problem other) {
            return getSimilarityTo(other) < 30;
        }
    }

}
