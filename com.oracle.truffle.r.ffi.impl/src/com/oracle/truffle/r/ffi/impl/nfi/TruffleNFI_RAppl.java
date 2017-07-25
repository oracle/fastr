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
package com.oracle.truffle.r.ffi.impl.nfi;

import com.oracle.truffle.r.runtime.ffi.RApplRFFI;

public class TruffleNFI_RAppl implements RApplRFFI {

    private static class TruffleNFI_Dqrdc2Node extends TruffleNFI_DownCallNode implements Dqrdc2Node {
        @Override
        protected NFIFunction getFunction() {
            return NFIFunction.dqrdc2;
        }

        @Override
        public void execute(double[] x, int ldx, int n, int p, double tol, int[] rank, double[] qraux, int[] pivot, double[] work) {
            call(x, ldx, n, p, tol, rank, qraux, pivot, work);
        }
    }

    private static class TruffleNFI_DqrcfNode extends TruffleNFI_DownCallNode implements DqrcfNode {
        @Override
        protected NFIFunction getFunction() {
            return NFIFunction.dqrcf;
        }

        @Override
        public void execute(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] b, int[] info) {
            call(x, n, k, qraux, y, ny, b, info);
        }
    }

    private static class TruffleNFI_DqrlsNode extends TruffleNFI_DownCallNode implements DqrlsNode {
        @Override
        protected NFIFunction getFunction() {
            return NFIFunction.dqrls;
        }

        @Override
        public void execute(double[] x, int n, int p, double[] y, int ny, double tol, double[] b, double[] rsd, double[] qty, int[] k, int[] jpvt, double[] qraux, double[] work) {
            call(x, n, p, y, ny, tol, b, rsd, qty, k, jpvt, qraux, work);
        }
    }

    private static class TruffleNFI_DqrqtyNode extends TruffleNFI_DownCallNode implements DqrqtyNode {
        @Override
        protected NFIFunction getFunction() {
            return NFIFunction.dqrqty;
        }

        @Override
        public void execute(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] qty) {
            call(x, n, k, qraux, y, ny, qty);
        }
    }

    private static class TruffleNFI_DqrqyNode extends TruffleNFI_DownCallNode implements DqrqyNode {
        @Override
        protected NFIFunction getFunction() {
            return NFIFunction.dqrqy;
        }

        @Override
        public void execute(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] qy) {
            call(x, n, k, qraux, y, ny, qy);
        }
    }

    private static class TruffleNFI_DqrrsdNode extends TruffleNFI_DownCallNode implements DqrrsdNode {
        @Override
        protected NFIFunction getFunction() {
            return NFIFunction.dqrrsd;
        }

        @Override
        public void execute(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] rsd) {
            call(x, n, k, qraux, y, ny, rsd);
        }
    }

    private static class TruffleNFI_DqrxbNode extends TruffleNFI_DownCallNode implements DqrxbNode {
        @Override
        protected NFIFunction getFunction() {
            return NFIFunction.dqrxb;
        }

        @Override
        public void execute(double[] x, int n, int k, double[] qraux, double[] y, int ny, double[] xb) {
            call(x, n, k, qraux, y, ny, xb);
        }
    }

    @Override
    public Dqrdc2Node createDqrdc2Node() {
        return new TruffleNFI_Dqrdc2Node();
    }

    @Override
    public DqrcfNode createDqrcfNode() {
        return new TruffleNFI_DqrcfNode();
    }

    @Override
    public DqrlsNode createDqrlsNode() {
        return new TruffleNFI_DqrlsNode();
    }

    @Override
    public DqrqtyNode createDqrqtyNode() {
        return new TruffleNFI_DqrqtyNode();
    }

    @Override
    public DqrqyNode createDqrqyNode() {
        return new TruffleNFI_DqrqyNode();
    }

    @Override
    public DqrrsdNode createDqrrsdNode() {
        return new TruffleNFI_DqrrsdNode();
    }

    @Override
    public DqrxbNode createDqrxbNode() {
        return new TruffleNFI_DqrxbNode();
    }
}
