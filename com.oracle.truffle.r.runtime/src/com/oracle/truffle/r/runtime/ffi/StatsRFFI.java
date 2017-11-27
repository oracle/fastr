/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.nodes.NodeInterface;

/**
 * Interface to native (C) methods provided by the {@code stats} package that are used to implement
 * {@code.Call(C_fft)}. The implementation is split into a Java part which calls the
 * {@code fft_factor} and {@code fft_work}. functions from the GNU R C code.
 */
public interface StatsRFFI {
    interface FactorNode extends NodeInterface {
        void execute(int n, int[] pmaxf, int[] pmaxp);

        static FactorNode create() {
            return RFFIFactory.getStatsRFFI().createFactorNode();
        }
    }

    interface WorkNode extends NodeInterface {
        int execute(double[] a, int nseg, int n, int nspn, int isn, double[] work, int[] iwork);

        static WorkNode create() {
            return RFFIFactory.getStatsRFFI().createWorkNode();
        }
    }

    interface LminflNode extends NodeInterface {
        void execute(double[] x, int ldx, int n, int k, int docoef, double[] qraux, double[] resid, double[] hat, double[] coef, double[] sigma, double tol);

        static LminflNode create() {
            return RFFIFactory.getStatsRFFI().createLminflNode();
        }
    }

    FactorNode createFactorNode();

    WorkNode createWorkNode();

    LminflNode createLminflNode();
}
