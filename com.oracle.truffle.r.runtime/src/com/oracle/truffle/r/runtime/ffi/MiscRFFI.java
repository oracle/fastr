/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * Miscellaneous methods implemented in native code.
 *
 */
public final class MiscRFFI {
    private final DownCallNodeFactory downCallNodeFactory;

    public MiscRFFI(DownCallNodeFactory downCallNodeFactory) {
        this.downCallNodeFactory = downCallNodeFactory;
    }

    public static final class ExactSumNode extends NativeCallNode {
        private ExactSumNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode());
        }

        public double execute(double[] values, boolean hasNa, boolean naRm) {
            return (double) call(NativeFunction.exactSumFunc, values, values.length, hasNa ? 1 : 0, naRm ? 1 : 0);
        }

        public static ExactSumNode create() {
            return RFFIFactory.getMiscRFFI().createExactSumNode();
        }
    }

    public static final class DqrlsNode extends NativeCallNode {
        private DqrlsNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode());
        }

        public void execute(double[] x, int n, int p, double[] y, int ny, double tol, double[] b, double[] rsd, double[] qty, int[] k, int[] jpvt, double[] qraux, double[] work) {
            call(NativeFunction.dqrls, x, n, p, y, ny, tol, b, rsd, qty, k, jpvt, qraux, work);
        }

        public static DqrlsNode create() {
            return RFFIFactory.getMiscRFFI().createDqrlsNode();
        }
    }

    public ExactSumNode createExactSumNode() {
        return new ExactSumNode(downCallNodeFactory);
    }

    public DqrlsNode createDqrlsNode() {
        return new DqrlsNode(downCallNodeFactory);
    }
}
