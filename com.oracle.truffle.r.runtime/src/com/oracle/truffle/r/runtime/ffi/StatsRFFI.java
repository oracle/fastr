/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.ffi;

/**
 * Interface to native (C) methods provided by the {@code stats} package that are used to implement
 * {@code .Call(C_fft)}. The implementation is split into a Java part which calls the
 * {@code fft_factor} and {@code fft_work}. functions from the GNU R C code.
 */
public final class StatsRFFI {
    private final DownCallNodeFactory downCallNodeFactory;

    public StatsRFFI(DownCallNodeFactory downCallNodeFactory) {
        this.downCallNodeFactory = downCallNodeFactory;
    }

    public static final class FactorNode extends NativeCallNode {
        private FactorNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode());
        }

        public void execute(int n, int[] pmaxf, int[] pmaxp) {
            call(NativeFunction.fft_factor, n, pmaxf, pmaxp);
        }

        public static FactorNode create() {
            return RFFIFactory.getStatsRFFI().createFactorNode();
        }
    }

    public static final class SetupWorkNode extends NativeCallNode {
        private SetupWorkNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode());
        }

        public int execute(double[] a, int nseg, int n, int nspn, int isn, double[] work, int[] iwork) {
            return (int) call(NativeFunction.fft_setup_work, a, nseg, n, nspn, isn, work, iwork);
        }

        public static SetupWorkNode create() {
            return RFFIFactory.getStatsRFFI().createSetupWorkNode();
        }
    }

    public static final class LminflNode extends NativeCallNode {
        private LminflNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode());
        }

        public void execute(double[] x, int ldx, int n, int k, int docoef, double[] qraux, double[] resid, double[] hat, double[] coef, double[] sigma, double tol) {
            call(NativeFunction.lminfl, x, ldx, n, k, docoef, qraux, resid, hat, coef, sigma, tol);
        }

        public static LminflNode create() {
            return RFFIFactory.getStatsRFFI().createLminflNode();
        }
    }

    public FactorNode createFactorNode() {
        return new FactorNode(downCallNodeFactory);
    }

    public SetupWorkNode createSetupWorkNode() {
        return new SetupWorkNode(downCallNodeFactory);
    }

    public LminflNode createLminflNode() {
        return new LminflNode(downCallNodeFactory);
    }
}
