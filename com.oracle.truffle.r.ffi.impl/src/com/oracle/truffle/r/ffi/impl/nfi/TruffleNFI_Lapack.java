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

import com.oracle.truffle.r.runtime.ffi.LapackRFFI;

public class TruffleNFI_Lapack implements LapackRFFI {

    private static class TruffleNFI_IlaverNode extends TruffleNFI_DownCallNode implements IlaverNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.ilaver;
        }

        @Override
        public void execute(int[] version) {
            call(version);
        }
    }

    private static class TruffleNFI_DgeevNode extends TruffleNFI_DownCallNode implements DgeevNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.dgeev;
        }

        @Override
        public int execute(char jobVL, char jobVR, int n, double[] a, int lda, double[] wr, double[] wi, double[] vl, int ldvl, double[] vr, int ldvr, double[] work, int lwork) {
            return (int) call(jobVL, jobVR, n, a, lda, wr, wi, vl, ldvl, vr, ldvr, work, lwork);
        }
    }

    private static class TruffleNFI_Dgeqp3Node extends TruffleNFI_DownCallNode implements Dgeqp3Node {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.dgeqp3;
        }

        @Override
        public int execute(int m, int n, double[] a, int lda, int[] jpvt, double[] tau, double[] work, int lwork) {
            return (int) call(m, n, a, lda, jpvt, tau, work, lwork);
        }
    }

    private static class TruffleNFI_DormqrNode extends TruffleNFI_DownCallNode implements DormqrNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.dormq;
        }

        @Override
        public int execute(char side, char trans, int m, int n, int k, double[] a, int lda, double[] tau, double[] c, int ldc, double[] work, int lwork) {
            return (int) call(side, trans, m, n, k, a, lda, tau, c, ldc, work, lwork);
        }
    }

    private static class TruffleNFI_DtrtrsNode extends TruffleNFI_DownCallNode implements DtrtrsNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.dtrtrs;
        }

        @Override
        public int execute(char uplo, char trans, char diag, int n, int nrhs, double[] a, int lda, double[] b, int ldb) {
            return (int) call(uplo, trans, diag, n, nrhs, a, lda, b, ldb);
        }
    }

    private static class TruffleNFI_DgetrfNode extends TruffleNFI_DownCallNode implements DgetrfNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.dgetrf;
        }

        @Override
        public int execute(int m, int n, double[] a, int lda, int[] ipiv) {
            return (int) call(m, n, a, lda, ipiv);
        }
    }

    private static class TruffleNFI_DpotrfNode extends TruffleNFI_DownCallNode implements DpotrfNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.dpotrf;
        }

        @Override
        public int execute(char uplo, int n, double[] a, int lda) {
            return (int) call(uplo, n, a, lda);
        }
    }

    private static class TruffleNFI_DpotriNode extends TruffleNFI_DownCallNode implements DpotriNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.dpotri;
        }

        @Override
        public int execute(char uplo, int n, double[] a, int lda) {
            return (int) call(uplo, n, a, lda);
        }
    }

    private static class TruffleNFI_DpstrfNode extends TruffleNFI_DownCallNode implements DpstrfNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.dpstrf;
        }

        @Override
        public int execute(char uplo, int n, double[] a, int lda, int[] piv, int[] rank, double tol, double[] work) {
            return (int) call(uplo, n, a, lda, piv, rank, tol, work);
        }
    }

    private static class TruffleNFI_DgesvNode extends TruffleNFI_DownCallNode implements DgesvNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.dgesv;
        }

        @Override
        public int execute(int n, int nrhs, double[] a, int lda, int[] ipiv, double[] b, int ldb) {
            return (int) call(n, nrhs, a, lda, ipiv, b, ldb);
        }
    }

    private static class TruffleNFI_DgesddNode extends TruffleNFI_DownCallNode implements DgesddNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.dgesdd;
        }

        @Override
        public int execute(char jobz, int m, int n, double[] a, int lda, double[] s, double[] u, int ldu, double[] vt, int ldtv, double[] work, int lwork, int[] iwork) {
            return (int) call(jobz, m, n, a, lda, s, u, ldu, vt, ldtv, work, lwork, iwork);
        }
    }

    private static class TruffleNFI_DlangeNode extends TruffleNFI_DownCallNode implements DlangeNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.dlange;
        }

        @Override
        public double execute(char norm, int m, int n, double[] a, int lda, double[] work) {
            return (double) call(norm, m, n, a, lda, work);
        }
    }

    private static class TruffleNFI_DgeconNode extends TruffleNFI_DownCallNode implements DgeconNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.dgecon;
        }

        @Override
        public int execute(char norm, int n, double[] a, int lda, double anorm, double[] rcond, double[] work, int[] iwork) {
            return (int) call(norm, n, a, lda, anorm, rcond, work, iwork);
        }
    }

    private static class TruffleNFI_DsyevrNode extends TruffleNFI_DownCallNode implements DsyevrNode {
        @Override
        protected NativeFunction getFunction() {
            return NativeFunction.dsyevr;
        }

        @Override
        public int execute(char jobz, char range, char uplo, int n, double[] a, int lda, double vl, double vu, int il, int iu, double abstol, int[] m, double[] w, double[] z, int ldz, int[] isuppz,
                        double[] work, int lwork, int[] iwork, int liwork) {
            return (int) call(jobz, range, uplo, n, a, lda, vl, vu, il, iu, abstol, m, w, z, ldz, isuppz, work, lwork, iwork, liwork);
        }
    }

    @Override
    public IlaverNode createIlaverNode() {
        return new TruffleNFI_IlaverNode();
    }

    @Override
    public DgeevNode createDgeevNode() {
        return new TruffleNFI_DgeevNode();
    }

    @Override
    public Dgeqp3Node createDgeqp3Node() {
        return new TruffleNFI_Dgeqp3Node();
    }

    @Override
    public DormqrNode createDormqrNode() {
        return new TruffleNFI_DormqrNode();
    }

    @Override
    public DtrtrsNode createDtrtrsNode() {
        return new TruffleNFI_DtrtrsNode();
    }

    @Override
    public DgetrfNode createDgetrfNode() {
        return new TruffleNFI_DgetrfNode();
    }

    @Override
    public DpotrfNode createDpotrfNode() {
        return new TruffleNFI_DpotrfNode();
    }

    @Override
    public DpotriNode createDpotriNode() {
        return new TruffleNFI_DpotriNode();
    }

    @Override
    public DpstrfNode createDpstrfNode() {
        return new TruffleNFI_DpstrfNode();
    }

    @Override
    public DgesvNode createDgesvNode() {
        return new TruffleNFI_DgesvNode();
    }

    @Override
    public DgesddNode createDgesddNode() {
        return new TruffleNFI_DgesddNode();
    }

    @Override
    public DlangeNode createDlangeNode() {
        return new TruffleNFI_DlangeNode();
    }

    @Override
    public DgeconNode createDgeconNode() {
        return new TruffleNFI_DgeconNode();
    }

    @Override
    public DsyevrNode createDsyevrNode() {
        return new TruffleNFI_DsyevrNode();
    }
}
