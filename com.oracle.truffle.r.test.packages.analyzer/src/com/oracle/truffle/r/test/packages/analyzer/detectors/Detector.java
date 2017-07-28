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

import com.oracle.truffle.r.test.packages.analyzer.Location;
import com.oracle.truffle.r.test.packages.analyzer.Problem;
import com.oracle.truffle.r.test.packages.analyzer.model.RPackageTestRun;

public abstract class Detector<T> {

    /** Semantics: If a child reports an error for a particular location, the parent doesn't. */
    protected Detector<?> parent;

    public Detector(Detector<?> parent) {
        this.parent = parent;
    }

    public Detector<?> getParent() {
        return parent;
    }

    public abstract String getName();

    /**
     * @param pkgTestRun The package test run any problems should be associated with.
     * @param startLineLocation The location of the first line, i.e., of body[0], or
     *            <code>null</code> if body is empty.
     * @param body The content to analyze (e.g. a list of lines in a file).
     * @return A list of detected problems (must not be {@code null}).
     */
    public abstract Collection<Problem> detect(RPackageTestRun pkgTestRun, Location startLineLocation, T body);

}
