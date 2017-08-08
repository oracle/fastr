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

import com.oracle.truffle.r.ffi.impl.nfi.NativeFunction;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.RApplRFFI;

/**
 * See the comments in {@link TruffleLLVM_Lapack} regarding argument passing.
 */
public class TruffleLLVM_RAppl implements RApplRFFI {

    private static final class TruffleLLVM_Dqrdc2Node extends TruffleLLVM_DownCallNode implements Dqrdc2Node {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.dqrdc2;
        }

        @Override
        public void execute(double[] x, int ldx, int n, int p, double tol, int[] rank, double[] qraux, int[] pivot, double[] work) {
            call(x, ldx, n, p, tol, rank, qraux, pivot, work);
        }
    }

    private static final class TruffleLLVM_DqrcfNode extends TruffleLLVM_DownCallNode implements DqrcfNode {

        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.dqrcf;
        }

        @Override
        public void execute(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] b, int[] info) {
            call(x, n, k, qraux, y, ny, b, info);
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
    public Dqrdc2Node createDqrdc2Node() {
        return new TruffleLLVM_Dqrdc2Node();
    }

    @Override
    public DqrcfNode createDqrcfNode() {
        return new TruffleLLVM_DqrcfNode();
    }

    @Override
    public DqrlsNode createDqrlsNode() {
        return new TruffleLLVM_DqrlsNode();
    }

    @Override
    public DqrqtyNode createDqrqtyNode() {
        throw RInternalError.unimplemented();
    }

    @Override
    public DqrqyNode createDqrqyNode() {
        throw RInternalError.unimplemented();
    }

    @Override
    public DqrrsdNode createDqrrsdNode() {
        throw RInternalError.unimplemented();
    }

    @Override
    public DqrxbNode createDqrxbNode() {
        throw RInternalError.unimplemented();
    }
}
