/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

/**
 * Collection of statically typed Lapack methods that are used in the {@code base} package.
 */
public interface LapackRFFI extends RFFI {
    /**
     * Return version info, mjor, minor, patch, in {@code version}.
     */
    void ilaver(int[] version);

    /**
     * See <a href="http://www.netlib.no/netlib/lapack/double/dgeev.f">spec</a>. The {@code info}
     * arg in the Fortran spec is returned as result.
     */
    // @formatter:off
    int dgeev(char jobVL, char jobVR, int n, double[] a, int lda, double[] wr, double[] wi, double[] vl, int ldvl,
                    double[] vr, int ldvr, double[] work, int lwork);

    /**
     * See <a href="http://www.netlib.no/netlib/lapack/double/dgeqp3.f">spec</a>. The {@code info}
     * arg in the Fortran spec is returned as result.
     */
    // @formatter:off
    int dgeqp3(int m, int n, double[] a, int lda, int[] jpvt, double[] tau, double[] work, int lwork);
}
