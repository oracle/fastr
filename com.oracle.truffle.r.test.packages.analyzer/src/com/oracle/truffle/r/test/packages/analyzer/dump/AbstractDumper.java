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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.oracle.truffle.r.test.packages.analyzer.Problem;
import com.oracle.truffle.r.test.packages.analyzer.RPackage;
import com.oracle.truffle.r.test.packages.analyzer.RPackageTestRun;

public abstract class AbstractDumper {

    public abstract void dump(Collection<Problem> problems);

    protected Map<Class<? extends Problem>, List<Problem>> groupByType(Collection<Problem> problems) {
        return problems.stream().collect(Collectors.groupingBy(p -> p.getClass()));
    }

    protected Map<RPackage, List<Problem>> groupByPkg(Collection<Problem> problems) {
        return problems.stream().collect(Collectors.groupingBy(p -> p.getPackageTestRun().getPackage()));
    }

    protected Map<RPackageTestRun, List<Problem>> groupByTestRuns(Collection<Problem> problems) {
        return problems.stream().collect(Collectors.groupingBy(p -> p.getPackageTestRun()));
    }

    protected Map<ProblemContent, List<Problem>> groupByProblemContent(Collection<Problem> problems) {
        final Map<ProblemContent, ProblemContent> table = new HashMap<>();
        return problems.stream().collect(Collectors.groupingBy(
                        p -> getOrAdd(table, new ProblemContent(p.getSummary(), p.getDetails()))));
    }

    private static <T> T getOrAdd(Map<T, T> cache, T val) {
        T t = cache.get(val);
        if (t == null) {
            cache.put(val, val);
            t = val;
        }
        return t;
    }

    protected static class ProblemContent {

        final String summary;
        final String details;

        protected ProblemContent(String summary, String details) {
            this.summary = summary;
            this.details = details;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((details == null) ? 0 : details.hashCode());
            result = prime * result + ((summary == null) ? 0 : summary.hashCode());
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
            if (details == null) {
                if (other.details != null) {
                    return false;
                }
            } else if (!details.equals(other.details)) {
                return false;
            }
            if (summary == null) {
                if (other.summary != null) {
                    return false;
                }
            } else if (!summary.equals(other.summary)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            int indexOf = details.indexOf("\n");
            return summary + ": " + details.substring(0, Math.max(indexOf, Math.min(details.length(), 150)));
        }

    }

}
