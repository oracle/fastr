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
package com.oracle.truffle.r.runtime.ffi.jni;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.ffi.LapackRFFI;

public class JNI_Lapack implements LapackRFFI {
    private static class JNI_IlaverNode extends IlaverNode {
        @Override
        @TruffleBoundary
        public void execute(int[] version) {
            native_ilaver(version);
        }
    }

    private static class JNI_DgeevNode extends DgeevNode {
        @Override
        @TruffleBoundary
        public int execute(char jobVL, char jobVR, int n, double[] a, int lda, double[] wr, double[] wi, double[] vl, int ldvl, double[] vr, int ldvr, double[] work, int lwork) {
            return native_dgeev(jobVL, jobVR, n, a, lda, wr, wi, vl, ldvl, vr, ldvr, work, lwork);
        }
    }

    private static class JNI_Dgeqp3Node extends Dgeqp3Node {
        @Override
        @TruffleBoundary
        public int execute(int m, int n, double[] a, int lda, int[] jpvt, double[] tau, double[] work, int lwork) {
            return native_dgeqp3(m, n, a, lda, jpvt, tau, work, lwork);
        }
    }

    private static class JNI_DormqrNode extends DormqrNode {
        @Override
        @TruffleBoundary
        public int execute(char side, char trans, int m, int n, int k, double[] a, int lda, double[] tau, double[] c, int ldc, double[] work, int lwork) {
            return native_dormqr(side, trans, m, n, k, a, lda, tau, c, ldc, work, lwork);
        }
    }

    private static class JNI_DtrtrsNode extends DtrtrsNode {
        @Override
        @TruffleBoundary
        public int execute(char uplo, char trans, char diag, int n, int nrhs, double[] a, int lda, double[] b, int ldb) {
            return native_dtrtrs(uplo, trans, diag, n, nrhs, a, lda, b, ldb);
        }
    }

    private static class JNI_DgetrfNode extends DgetrfNode {
        @Override
        @TruffleBoundary
        public int execute(int m, int n, double[] a, int lda, int[] ipiv) {
            return native_dgetrf(m, n, a, lda, ipiv);
        }
    }

    private static class JNI_DpotrfNode extends DpotrfNode {
        @Override
        @TruffleBoundary
        public int execute(char uplo, int n, double[] a, int lda) {
            return native_dpotrf(uplo, n, a, lda);
        }
    }

    private static class JNI_DpotriNode extends DpotriNode {
        @Override
        @TruffleBoundary
        public int execute(char uplo, int n, double[] a, int lda) {
            return native_dpotri(uplo, n, a, lda);
        }
    }

    private static class JNI_DpstrfNode extends DpstrfNode {
        @Override
        @TruffleBoundary
        public int execute(char uplo, int n, double[] a, int lda, int[] piv, int[] rank, double tol, double[] work) {
            return native_dpstrf(uplo, n, a, lda, piv, rank, tol, work);
        }
    }

    private static class JNI_DgesvNode extends DgesvNode {
        @Override
        @TruffleBoundary
        public int execute(int n, int nrhs, double[] a, int lda, int[] ipiv, double[] b, int ldb) {
            return native_dgesv(n, nrhs, a, lda, ipiv, b, ldb);
        }
    }

    private static class JNI_DlangeNode extends DlangeNode {
        @Override
        @TruffleBoundary
        public double execute(char norm, int m, int n, double[] a, int lda, double[] work) {
            return native_dlange(norm, m, n, a, lda, work);
        }
    }

    private static class JNI_DgeconNode extends DgeconNode {
        @Override
        @TruffleBoundary
        public int execute(char norm, int n, double[] a, int lda, double anorm, double[] rcond, double[] work, int[] iwork) {
            return native_dgecon(norm, n, a, lda, anorm, rcond, work, iwork);
        }
    }

    private static class JNI_DsyevrNode extends DsyevrNode {
        @Override
        public int execute(char jobz, char range, char uplo, int n, double[] a, int lda, double vl, double vu, int il, int iu, double abstol, int[] m,
                        double[] w, double[] z, int ldz, int[] isuppz, double[] work, int lwork, int[] iwork, int liwork) {
            return native_dsyevr(jobz, range, uplo, n, a, lda, vl, vu, il, iu, abstol, m, w, z, ldz, isuppz, work, lwork, iwork, liwork);
        }
    }

    @Override
    public IlaverNode createIlaverNode() {
        return new JNI_IlaverNode();
    }

    @Override
    public DgeevNode createDgeevNode() {
        return new JNI_DgeevNode();
    }

    @Override
    public Dgeqp3Node createDgeqp3Node() {
        return new JNI_Dgeqp3Node();
    }

    @Override
    public DormqrNode createDormqrNode() {
        return new JNI_DormqrNode();
    }

    @Override
    public DtrtrsNode createDtrtrsNode() {
        return new JNI_DtrtrsNode();
    }

    @Override
    public DgetrfNode createDgetrfNode() {
        return new JNI_DgetrfNode();
    }

    @Override
    public DpotrfNode createDpotrfNode() {
        return new JNI_DpotrfNode();
    }

    @Override
    public DpotriNode createDpotriNode() {
        return new JNI_DpotriNode();
    }

    @Override
    public DpstrfNode createDpstrfNode() {
        return new JNI_DpstrfNode();
    }

    @Override
    public DgesvNode createDgesvNode() {
        return new JNI_DgesvNode();
    }

    @Override
    public DlangeNode createDlangeNode() {
        return new JNI_DlangeNode();
    }

    @Override
    public DgeconNode createDgeconNode() {
        return new JNI_DgeconNode();
    }

    @Override
    public DsyevrNode createDsyevrNode() {
        return new JNI_DsyevrNode();
    }

    // Checkstyle: stop method name

    private static native void native_ilaver(int[] version);

    private static native int native_dgeev(char jobVL, char jobVR, int n, double[] a, int lda, double[] wr, double[] wi, double[] vl, int ldvl, double[] vr, int ldvr, double[] work, int lwork);

    private static native int native_dgeqp3(int m, int n, double[] a, int lda, int[] jpvt, double[] tau, double[] work, int lwork);

    private static native int native_dormqr(char side, char trans, int m, int n, int k, double[] a, int lda, double[] tau, double[] c, int ldc, double[] work, int lwork);

    private static native int native_dtrtrs(char uplo, char trans, char diag, int n, int nrhs, double[] a, int lda, double[] b, int ldb);

    private static native int native_dgetrf(int m, int n, double[] a, int lda, int[] ipiv);

    private static native int native_dpotrf(char uplo, int n, double[] a, int lda);

    private static native int native_dpotri(char uplo, int n, double[] a, int lda);

    private static native int native_dpstrf(char uplo, int n, double[] a, int lda, int[] piv, int[] rank, double tol, double[] work);

    private static native int native_dgesv(int n, int nrhs, double[] a, int lda, int[] ipiv, double[] b, int ldb);

    private static native double native_dlange(char norm, int m, int n, double[] a, int lda, double[] work);

    private static native int native_dgecon(char norm, int n, double[] a, int lda, double anorm, double[] rcond, double[] work, int[] iwork);

    private static native int native_dsyevr(char jobz, char range, char uplo, int n, double[] a, int lda, double vl, double vu, int il, int iu, double abstol, int[] m,
                    double[] w, double[] z, int ldz, int[] isuppz, double[] work, int lwork, int[] iwork, int liwork);

}
