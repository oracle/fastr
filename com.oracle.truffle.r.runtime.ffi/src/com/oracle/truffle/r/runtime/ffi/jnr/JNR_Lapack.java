/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi.jnr;

import static com.oracle.truffle.r.runtime.ffi.RFFIUtils.wrapChar;
import static com.oracle.truffle.r.runtime.ffi.RFFIUtils.wrapDouble;
import static com.oracle.truffle.r.runtime.ffi.RFFIUtils.wrapInt;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.ffi.LapackRFFI;

import jnr.ffi.LibraryLoader;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;

public class JNR_Lapack implements LapackRFFI {
    /**
     * Fortran does call by reference for everything, which we handle with arrays. Evidently, this
     * is not as efficient as it could be. This implementation assumes a single-threaded
     * environment.
     */
    public interface Lapack {
        // Checkstyle: stop method name
        void ilaver_(@Out int[] major, @Out int[] minor, @Out int[] patch);

        void dgeev_(@In byte[] jobVL, @In byte[] jobVR, @In int[] n, @In double[] a, @In int[] lda, @Out double[] wr, @Out double[] wi, @Out double[] vl, @In int[] ldvl, @Out double[] vr,
                        @In int[] ldvr, @Out double[] work, @In int[] lwork, @Out int[] info);

        void dgeqp3_(@In int[] m, @In int[] n, double[] a, @In int[] lda, int[] jpvt, @Out double[] tau, @Out double[] work, @In int[] lwork, @Out int[] info);

        void dormqr_(@In byte[] side, @In byte[] trans, @In int[] m, @In int[] n, @In int[] k, @In double[] a, @In int[] lda, @In double[] tau, double[] c, @In int[] ldc, @Out double[] work,
                        @In int[] lwork, @Out int[] info);

        void dtrtrs_(@In byte[] uplo, @In byte[] trans, @In byte[] diag, @In int[] n, @In int[] nrhs, @In double[] a, @In int[] lda, double[] b, @In int[] ldb, @Out int[] info);

        void dgetrf_(@In int[] m, @In int[] n, double[] a, @In int[] lda, @Out int[] ipiv, @Out int[] info);

        void dpotrf_(@In byte[] uplo, @In int[] n, double[] a, @In int[] lda, @Out int[] info);

        void dpstrf_(@In byte[] uplo, @In int[] n, double[] a, @In int[] lda, @Out int[] piv, @Out int[] rank, @In double[] tol, @Out double[] work, @Out int[] info);

        void dgesv_(@In int[] n, @In int[] nrhs, double[] a, @In int[] lda, @Out int[] ipiv, double[] b, @In int[] ldb, @Out int[] info);

        double dlange_(@In byte[] norm, @In int[] m, @In int[] n, @In double[] a, @In int[] lda, @Out double[] work);

        void dgecon_(@In byte[] norm, @In int[] n, @In double[] a, @In int[] lda, @In double[] anorm, @Out double[] rcond, @Out double[] work, @Out int[] iwork, @Out int[] info);

    }

    private static class LapackProvider {
        private static Lapack lapack;

        @TruffleBoundary
        private static Lapack createAndLoadLib() {
            return LibraryLoader.create(Lapack.class).load("Rlapack");
        }

        static Lapack lapack() {
            if (lapack == null) {
                lapack = createAndLoadLib();
            }
            return lapack;
        }
    }

    public static Lapack lapack() {
        return LapackProvider.lapack();
    }

    @Override
    @TruffleBoundary
    public void ilaver(int[] version) {
        int[] major = new int[1];
        int[] minor = new int[1];
        int[] patch = new int[1];
        lapack().ilaver_(major, minor, patch);
        version[0] = major[0];
        version[1] = minor[0];
        version[2] = patch[0];
    }

    @Override
    @TruffleBoundary
    public int dgeev(char jobVL, char jobVR, int n, double[] a, int lda, double[] wr, double[] wi, double[] vl, int ldvl, double[] vr, int ldvr, double[] work, int lwork) {
        int[] info = new int[1];
        lapack().dgeev_(wrapChar(jobVL), wrapChar(jobVR), wrapInt(n), a, wrapInt(lda), wr, wi, vl, wrapInt(ldvl), vr, wrapInt(ldvr), work, wrapInt(lwork), info);
        return info[0];
    }

    @Override
    @TruffleBoundary
    public int dgeqp3(int m, int n, double[] a, int lda, int[] jpvt, double[] tau, double[] work, int lwork) {
        int[] info = new int[1];
        lapack().dgeqp3_(wrapInt(m), wrapInt(n), a, wrapInt(lda), jpvt, tau, work, wrapInt(lwork), info);
        return info[0];
    }

    @Override
    @TruffleBoundary
    public int dormqr(char side, char trans, int m, int n, int k, double[] a, int lda, double[] tau, double[] c, int ldc, double[] work, int lwork) {
        int[] info = new int[1];
        lapack().dormqr_(wrapChar(side), wrapChar(trans), wrapInt(m), wrapInt(n), wrapInt(k), a, wrapInt(lda), tau, c, wrapInt(ldc), work, wrapInt(lwork), info);
        return info[0];
    }

    @Override
    @TruffleBoundary
    public int dtrtrs(char uplo, char trans, char diag, int n, int nrhs, double[] a, int lda, double[] b, int ldb) {
        int[] info = new int[1];
        lapack().dtrtrs_(wrapChar(uplo), wrapChar(trans), wrapChar(diag), wrapInt(n), wrapInt(nrhs), a, wrapInt(lda), b, wrapInt(ldb), info);
        return info[0];
    }

    @Override
    @TruffleBoundary
    public int dgetrf(int m, int n, double[] a, int lda, int[] ipiv) {
        int[] info = new int[1];
        lapack().dgetrf_(wrapInt(m), wrapInt(n), a, wrapInt(lda), ipiv, info);
        return info[0];
    }

    @Override
    @TruffleBoundary
    public int dpotrf(char uplo, int n, double[] a, int lda) {
        int[] info = new int[1];
        lapack().dpotrf_(wrapChar(uplo), wrapInt(n), a, wrapInt(lda), info);
        return info[0];
    }

    @Override
    @TruffleBoundary
    public int dpstrf(char uplo, int n, double[] a, int lda, int[] piv, int[] rank, double tol, double[] work) {
        int[] info = new int[1];
        lapack().dpstrf_(wrapChar(uplo), wrapInt(n), a, wrapInt(lda), piv, rank, wrapDouble(tol), work, info);
        return info[0];
    }

    @Override
    @TruffleBoundary
    public int dgesv(int n, int nrhs, double[] a, int lda, int[] ipiv, double[] b, int ldb) {
        int[] info = new int[1];
        lapack().dgesv_(wrapInt(n), wrapInt(nrhs), a, wrapInt(lda), ipiv, b, wrapInt(ldb), info);
        return info[0];
    }

    @Override
    @TruffleBoundary
    public double dlange(char norm, int m, int n, double[] a, int lda, double[] work) {
        return lapack().dlange_(wrapChar(norm), wrapInt(m), wrapInt(n), a, wrapInt(lda), work);
    }

    @Override
    @TruffleBoundary
    public int dgecon(char norm, int n, double[] a, int lda, double anorm, double[] rcond, double[] work, int[] iwork) {
        int[] info = new int[1];
        lapack().dgecon_(wrapChar(norm), wrapInt(n), a, wrapInt(lda), wrapDouble(anorm), rcond, work, iwork, info);
        return info[0];
    }
}
