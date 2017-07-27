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
import java.util.Objects;

import com.oracle.truffle.r.test.packages.analyzer.Location;
import com.oracle.truffle.r.test.packages.analyzer.Problem;
import com.oracle.truffle.r.test.packages.analyzer.RPackageTestRun;
import com.oracle.truffle.r.test.packages.analyzer.parser.DiffParser.ChangeCommand;
import com.oracle.truffle.r.test.packages.analyzer.parser.DiffParser.DiffChunk;

public class DiffDetector extends Detector<List<DiffChunk>> {

    public static final DiffDetector INSTANCE = new DiffDetector();

    protected DiffDetector() {
        super(null);
    }

    protected DiffDetector(Detector<?> parent) {
        super(parent);
    }

    @Override
    public String getName() {
        return "Diff detector";
    }

    @Override
    public Collection<Problem> detect(RPackageTestRun pkg, Location startLineLocation, List<DiffChunk> body) {
        Collection<Problem> problems = new LinkedList<>();
        for (DiffChunk diffChunk : body) {
            String summary;
            if (isMinorDifference(diffChunk)) {
                summary = "Minor difference in test output";
            } else {
                summary = "Major difference in test output";
            }
            problems.add(new DiffProblem(pkg, diffChunk.getLocation(), summary, diffChunk));
        }
        return problems;
    }

    /**
     * TODO In general, one-line changes can be classified as minor differences.
     */
    private static boolean isMinorDifference(DiffChunk diffChunk) {
        ChangeCommand cmd = diffChunk.getCmd();
        return cmd.cmd == 'c' && cmd.lFrom == cmd.rFrom && cmd.lTo == -1 && cmd.rTo == -1;
    }

    public static class DiffProblem extends Problem {

        private final String summary;
        private final DiffChunk diffChunk;

        protected DiffProblem(RPackageTestRun pkg, Location location, String summary, DiffChunk diffChunk) {
            super(pkg, location);
            this.summary = Objects.requireNonNull(summary);
            this.diffChunk = Objects.requireNonNull(diffChunk);
        }

        @Override
        public String getSummary() {
            return summary;
        }

        @Override
        public String getDetails() {
            return diffChunk.toString();
        }

        @Override
        public String toString() {
            return String.format("%s: %s", diffChunk.getLocation(), summary);
        }

    }

}
