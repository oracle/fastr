/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.nodes.Node;

/**
 * Collection of statically typed methods (from Linpack and elsewhere) that are built in to a GnuR
 * implementation and factored out into a separate library in FastR. This corresponds to the
 * {@code libappl} library in GnuR.
 */
public interface RApplRFFI {
    abstract class RApplRFFINode extends Node {
        // Linpack
        public abstract void dqrdc2(double[] x, int ldx, int n, int p, double tol, int[] rank, double[] qraux, int[] pivot, double[] work);

        public abstract void dqrcf(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] b, int[] info);

        public abstract void dqrls(double[] x, int n, int p, double[] y, int ny, double tol, double[] b, double[] rsd, double[] qty, int[] k, int[] jpvt, double[] qraux, double[] work);
    }

    RApplRFFINode rApplRFFINode();

}
