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
package com.oracle.truffle.r.test.packages.analyzer;

import java.util.Comparator;
import java.util.Objects;

import com.oracle.truffle.r.test.packages.analyzer.model.RPackage;
import com.oracle.truffle.r.test.packages.analyzer.model.RPackageTestRun;

/**
 * Abstract class denoting any kind of problem occurred during a package test.
 */
public abstract class Problem {

    private final RPackageTestRun pkgTestRun;
    private final Location location;

    protected Problem(RPackageTestRun pkg, Location location) {
        this.pkgTestRun = Objects.requireNonNull(pkg);
        this.location = Objects.requireNonNull(location);
    }

    public RPackage getPackage() {
        return pkgTestRun.getPackage();
    }

    public RPackageTestRun getPackageTestRun() {
        return pkgTestRun;
    }

    public Location getLocation() {
        return location;
    }

    /**
     * Returns a one-line summary of the problem (e.g.
     * <q>RuntimeException occurred</q>).
     */
    public abstract String getSummary();

    /**
     * Returns detailed content of the problem excluding the summary (e.g. a stack trace).
     */
    public abstract String getDetails();

    public abstract int getSimilarityTo(Problem other);

    public abstract boolean isSimilarTo(Problem other);

    public Comparator<Problem> getSimilarityComparator() {
        return (a, b) -> a.isSimilarTo(b) ? 0 : 1;
    }

    private static int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    public static int computeLevenshteinDistance(CharSequence lhs, CharSequence rhs) {
        int[][] distance = new int[lhs.length() + 1][rhs.length() + 1];

        for (int i = 0; i <= lhs.length(); i++) {
            distance[i][0] = i;
        }
        for (int j = 1; j <= rhs.length(); j++) {
            distance[0][j] = j;
        }

        for (int i = 1; i <= lhs.length(); i++) {
            for (int j = 1; j <= rhs.length(); j++) {
                distance[i][j] = min(
                                distance[i - 1][j] + 1,
                                distance[i][j - 1] + 1,
                                distance[i - 1][j - 1] + ((lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1));
            }
        }

        return distance[lhs.length()][rhs.length()];
    }

}
