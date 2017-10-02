/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.llvm;

import com.oracle.truffle.r.runtime.ffi.MiscRFFI;
import com.oracle.truffle.r.runtime.ffi.NativeFunction;

public class TruffleLLVM_Misc implements MiscRFFI {

    private static class TruffleLLVM_ExactSumNode extends TruffleLLVM_DownCallNode implements ExactSumNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.exactSumFunc;
        }

        @Override
        public double execute(double[] values, boolean hasNa, boolean naRm) {
            return (double) call(values, values.length, hasNa ? 1 : 0, naRm ? 1 : 0);
        }
    }

    private static final class TruffleLLVM_DqrlsNode extends TruffleLLVM_DownCallNode implements DqrlsNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.dqrls;
        }

        @Override
        public void execute(double[] x, int n, int p, double[] y, int ny, double tol, double[] b, double[] rsd, double[] qty, int[] k, int[] jpvt, double[] qraux, double[] work) {
            call(x, n, p, y, ny, tol, b, rsd, qty, k, jpvt, qraux, work);
        }
    }

    @Override
    public ExactSumNode createExactSumNode() {
        return new TruffleLLVM_ExactSumNode();
    }

    @Override
    public DqrlsNode createDqrlsNode() {
        return new TruffleLLVM_DqrlsNode();
    }

}
