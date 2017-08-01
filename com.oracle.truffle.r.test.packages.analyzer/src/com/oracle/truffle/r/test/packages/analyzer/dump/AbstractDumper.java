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
package com.oracle.truffle.r.test.packages.analyzer.dump;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.oracle.truffle.r.test.packages.analyzer.Problem;
import com.oracle.truffle.r.test.packages.analyzer.detectors.Detector;
import com.oracle.truffle.r.test.packages.analyzer.model.RPackage;
import com.oracle.truffle.r.test.packages.analyzer.model.RPackageTestRun;
import com.oracle.truffle.r.test.packages.analyzer.parser.LogFileParseException;

public abstract class AbstractDumper {

    public abstract void dump(Collection<RPackage> problems, Collection<LogFileParseException> collection);

    protected Map<Class<? extends Problem>, List<Problem>> groupByType(Collection<Problem> problems) {
        return problems.stream().collect(Collectors.groupingBy(p -> p.getClass()));
    }

    protected Map<RPackage, List<Problem>> groupByPkg(Collection<Problem> problems) {
        return problems.stream().collect(Collectors.groupingBy(p -> p.getPackageTestRun().getPackage()));
    }

    protected Map<RPackageTestRun, List<Problem>> groupByTestRuns(Collection<RPackageTestRun> allTestRuns, Collection<Problem> problems) {
        Map<RPackageTestRun, List<Problem>> groupedByTestRun = problems.stream().collect(Collectors.groupingBy(p -> p.getPackageTestRun()));
        // insert test runs don't having any problems
        for (RPackageTestRun testRun : allTestRuns) {
            if (!groupedByTestRun.containsKey(testRun)) {
                groupedByTestRun.put(testRun, Collections.emptyList());
            }
        }
        return groupedByTestRun;
    }

    protected Map<ProblemContent, List<Problem>> groupByProblemContent(Collection<Problem> problems) {
        return problems.stream().collect(Collectors.groupingBy(p -> new ProblemContent(p)));
    }

    protected static Collection<Problem> eliminateRedundantProblems(Collection<Problem> problems) {
        // build detector hierarchy
        Set<Detector<?>> collect = problems.stream().map(p -> p.getDetector()).collect(Collectors.toSet());
        Map<Detector<?>, Collection<Detector<?>>> hierarchy = new HashMap<>();
        for (Detector<?> detector : collect) {
            hierarchy.put(detector, collectChildrenRecursive(detector));
        }

        Map<Detector<?>, List<Problem>> collect2 = problems.stream().collect(Collectors.groupingBy(p -> p.getDetector()));

        return problems.stream().filter(p -> isIncluded(p, hierarchy, collect2)).collect(Collectors.toList());
    }

    /**
     * Returns {@code false} if any of the problem detector's children has reported a problem in the
     * same location. Returns {@code true} otherwise.
     */
    private static boolean isIncluded(Problem p, Map<Detector<?>, Collection<Detector<?>>> hierarchy, Map<Detector<?>, List<Problem>> collect2) {
        Collection<Detector<?>> children = hierarchy.get(p.getDetector());
        for (Detector<?> childDetector : children) {
            for (Problem childProblem : collect2.get(childDetector)) {
                if (childProblem.getLocation().equals(p.getLocation())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Collection<Detector<?>> collectChildrenRecursive(Detector<?> detector) {
        Collection<Detector<?>> all = new LinkedList<>();
        all.addAll(detector.getChildren());
        detector.getChildren().stream().forEach(c -> all.addAll(collectChildrenRecursive(c)));
        return all;
    }

    protected static class ProblemContent {

        final Problem representitive;

        protected ProblemContent(Problem representitive) {
            this.representitive = representitive;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((representitive == null) ? 0 : representitive.getClass().hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ProblemContent other = (ProblemContent) obj;
            return representitive.isSimilarTo(other.representitive);
        }

        @Override
        public String toString() {
            return representitive.toString();
        }

    }

}
