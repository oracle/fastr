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
import com.oracle.truffle.r.test.packages.analyzer.RPackageTestRun;

public class UnsupportedSpecializationDetector extends LineDetector {

    public static final UnsupportedSpecializationDetector INSTANCE = new UnsupportedSpecializationDetector();
    private static final Pattern PATTERN = Pattern.compile(
                    "com\\.oracle\\.truffle\\.r\\.runtime\\.RInternalError: com\\.oracle\\.truffle\\.api\\.dsl\\.UnsupportedSpecializationException: (?<MSG>.*)");
    private static final String TRACE_END = "Frame(d=0)";

    protected UnsupportedSpecializationDetector() {
    }

    @Override
    public String getName() {
        return "UnsupportedSpecializationException detector";
    }

    @Override
    public Collection<Problem> detect(RPackageTestRun pkg, Location startLocation, List<String> body) {
        Collection<Problem> problems = new LinkedList<>();
        StringBuilder stackTrace = new StringBuilder();
        int state = 0;
        String message = null;
        assert body.isEmpty() || startLocation != null;
        int lineNr = startLocation != null ? startLocation.lineNr : 0;
        for (String line : body) {
            switch (state) {
                case 0:
                    Matcher matcher = PATTERN.matcher(line);
                    if (matcher.find()) {
                        message = matcher.group("MSG");
                        state = 1;
                    }
                    break;
                case 1:
                    if (line.contains(TRACE_END)) {
                        problems.add(new UnsupportedSpecializationProblem(pkg, new Location(startLocation.file, lineNr), message, stackTrace.toString()));
                        stackTrace.setLength(0);
                        message = null;
                        state = 0;
                    } else {
                        stackTrace.append(line).append(System.lineSeparator());
                    }
                    break;
            }
            ++lineNr;
        }
        return problems;
    }

    private static class UnsupportedSpecializationProblem extends Problem {

        protected UnsupportedSpecializationProblem(RPackageTestRun pkg, Location location, String message, String stackTrace) {
            super(pkg, location);
            this.message = message;
            this.stackTrace = stackTrace;
        }

        public final String message;
        public final String stackTrace;

        @Override
        public String toString() {
            return getLocation() + ": com.oracle.truffle.api.dsl.UnsupportedSpecializationException: " + message;
        }

        @Override
        public String getSummary() {
            return "com.oracle.truffle.api.dsl.UnsupportedSpecializationException: " + message;
        }

        @Override
        public String getDetails() {
            return stackTrace;
        }

    }

}
