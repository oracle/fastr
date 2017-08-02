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

public class UnsupportedSpecializationDetector extends LineDetector {

    public static final UnsupportedSpecializationDetector INSTANCE = new UnsupportedSpecializationDetector();

    private static final Pattern PATTERN = Pattern.compile(
                    "com\\.oracle\\.truffle\\.r\\.runtime\\.RInternalError: com\\.oracle\\.truffle\\.api\\.dsl\\.UnsupportedSpecializationException: (?<MSG>.*)");

    protected UnsupportedSpecializationDetector() {
        super(RInternalErrorDetector.INSTANCE);
    }

    @Override
    public String getName() {
        return "UnsupportedSpecializationException detector";
    }

    @Override
    public Collection<Problem> detect(RPackageTestRun pkg, Location startLocation, List<String> body) {
        Collection<Problem> problems = new LinkedList<>();
        String message = null;
        assert body.isEmpty() || startLocation != null;
        int lineNr = startLocation != null ? startLocation.lineNr : 0;
        int problemStartLine = lineNr;
        for (String line : body) {
            Matcher matcher = PATTERN.matcher(line);
            if (matcher.find()) {
                message = matcher.group("MSG");
                problemStartLine = lineNr;
                problems.add(new UnsupportedSpecializationProblem(pkg, this, new Location(startLocation.file, problemStartLine), message));
            }
            ++lineNr;
        }
        return problems;
    }

    private static class UnsupportedSpecializationProblem extends Problem {

        protected UnsupportedSpecializationProblem(RPackageTestRun pkg, UnsupportedSpecializationDetector detector, Location location, String message) {
            super(pkg, detector, location);
            this.message = message;
        }

        public final String message;

        @Override
        public String toString() {
            return getLocation() + ": com.oracle.truffle.api.dsl.UnsupportedSpecializationException: " + message;
        }

        @Override
        public String getSummary() {
            return "UnsupportedSpecializationException";
        }

        @Override
        public String getDetails() {
            return message;
        }

        @Override
        public int getSimilarityTo(Problem other) {
            if (other.getClass() == UnsupportedSpecializationProblem.class) {
                return Problem.computeLevenshteinDistance(getDetails().trim(), other.getDetails().trim());
            }
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isSimilarTo(Problem other) {
            return getSimilarityTo(other) < 5;
        }
    }

}
