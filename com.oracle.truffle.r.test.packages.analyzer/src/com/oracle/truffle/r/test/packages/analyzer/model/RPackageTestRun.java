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
package com.oracle.truffle.r.test.packages.analyzer.model;

import java.util.Collection;
import java.util.Collections;

import com.oracle.truffle.r.test.packages.analyzer.Problem;

/**
 * Represents one test run of a package.
 */
public class RPackageTestRun {

    private final RPackage pkg;
    private final int nr;
    private Collection<Problem> problems;

    /** The overall outcome of the package test run as reported. */
    private boolean success;

    public RPackageTestRun(RPackage pkg, int nr) {
        this.pkg = pkg;
        this.nr = nr;
    }

    public RPackage getPackage() {
        return pkg;
    }

    public int getNumber() {
        return nr;
    }

    public void setProblems(Collection<Problem> p) {
        problems = p;
    }

    public Collection<Problem> getProblems() {
        if (problems != null) {
            return problems;
        }
        return Collections.emptyList();
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return pkg + "/" + nr;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + nr;
        result = prime * result + ((pkg == null) ? 0 : pkg.hashCode());
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
        RPackageTestRun other = (RPackageTestRun) obj;
        if (nr != other.nr) {
            return false;
        }
        if (pkg == null) {
            if (other.pkg != null) {
                return false;
            }
        } else if (!pkg.equals(other.pkg)) {
            return false;
        }
        return true;
    }

}
