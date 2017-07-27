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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.nodes.NodeInterface;

/**
 * Collection of statically typed Lapack methods that are used in the {@code base} package. The
 * signatures match the Fortran definition with the exception that the "info" value is returned as
 * the result of the call.
 */
public interface LapackRFFI {
    interface IlaverNode extends NodeInterface {
        /**
         * Return version info, mjor, minor, patch, in {@code version}.
         */
        void execute(int[] version);

        static IlaverNode create() {
            return RFFIFactory.getLapackRFFI().createIlaverNode();
        }
    }

    interface DgeevNode extends NodeInterface {
        /**
         * See <a href="http://www.netlib.org/lapack/explore-html/d9/d28/dgeev_8f.html">spec</a>.
         */
        int execute(char jobVL, char jobVR, int n, double[] a, int lda, double[] wr, double[] wi, double[] vl, int ldvl, double[] vr, int ldvr, double[] work, int lwork);

        static DgeevNode create() {
            return RFFIFactory.getLapackRFFI().createDgeevNode();
        }
    }

    interface Dgeqp3Node extends NodeInterface {
        /**
         * See <a href="http://www.netlib.org/lapack/explore-html/db/de5/dgeqp3_8f.html">spec</a>.
         */
        int execute(int m, int n, double[] a, int lda, int[] jpvt, double[] tau, double[] work, int lwork);

        static Dgeqp3Node create() {
            return RFFIFactory.getLapackRFFI().createDgeqp3Node();
        }
    }

    interface DormqrNode extends NodeInterface {
        /**
         * See <a href="http://www.netlib.org/lapack/explore-html/da/d82/dormqr_8f.html">spec</a>.
         */
        int execute(char side, char trans, int m, int n, int k, double[] a, int lda, double[] tau, double[] c, int ldc, double[] work, int lwork);

        static DormqrNode create() {
            return RFFIFactory.getLapackRFFI().createDormqrNode();
        }
    }

    interface DtrtrsNode extends NodeInterface {

        /**
         * See <a href="http://www.netlib.org/lapack/explore-html/d6/d6f/dtrtrs_8f.html">spec</a>.
         */
        int execute(char uplo, char trans, char diag, int n, int nrhs, double[] a, int lda, double[] b, int ldb);

        static DtrtrsNode create() {
            return RFFIFactory.getLapackRFFI().createDtrtrsNode();
        }
    }

    interface DgetrfNode extends NodeInterface {

        /**
         * See <a href="http://www.netlib.org/lapack/explore-html/d3/d6a/dgetrf_8f.html">spec</a>.
         */
        int execute(int m, int n, double[] a, int lda, int[] ipiv);

        static DgetrfNode create() {
            return RFFIFactory.getLapackRFFI().createDgetrfNode();
        }
    }

    interface DpotrfNode extends NodeInterface {

        /**
         * See <a href="http://www.netlib.org/lapack/explore-html/d0/d8a/dpotrf_8f.html">spec</a>.
         */
        int execute(char uplo, int n, double[] a, int lda);

        static DpotrfNode create() {
            return RFFIFactory.getLapackRFFI().createDpotrfNode();
        }
    }

    interface DpotriNode extends NodeInterface {

        /**
         * See <a href="http://www.netlib.org/lapack/explore-html/d0/d8a/dpotri_8f.html">spec</a>.
         */
        int execute(char uplo, int n, double[] a, int lda);

        static DpotriNode create() {
            return RFFIFactory.getLapackRFFI().createDpotriNode();
        }
    }

    interface DpstrfNode extends NodeInterface {
        /**
         * See <a href="http://www.netlib.org/lapack/explore-html/dd/dad/dpstrf_8f.html">spec</a>.
         */
        int execute(char uplo, int n, double[] a, int lda, int[] piv, int[] rank, double tol, double[] work);

        static DpstrfNode create() {
            return RFFIFactory.getLapackRFFI().createDpstrfNode();
        }
    }

    interface DgesvNode extends NodeInterface {
        /**
         * See <a href="http://www.netlib.org/lapack/explore-html/d8/d72/dgesv_8f.html">spec</a>.
         */
        int execute(int n, int nrhs, double[] a, int lda, int[] ipiv, double[] b, int ldb);

        static DgesvNode create() {
            return RFFIFactory.getLapackRFFI().createDgesvNode();
        }
    }

    interface DlangeNode extends NodeInterface {

        /**
         * See <a href="http://www.netlib.org/lapack/explore-html/dc/d09/dlange_8f.html">spec</a>.
         */
        double execute(char norm, int m, int n, double[] a, int lda, double[] work);

        static DlangeNode create() {
            return RFFIFactory.getLapackRFFI().createDlangeNode();
        }
    }

    interface DgeconNode extends NodeInterface {

        /**
         * See <a href="http://www.netlib.org/lapack/explore-html/db/de4/dgecon_8f.html">spec</a>.
         */
        int execute(char norm, int n, double[] a, int lda, double anorm, double[] rcond, double[] work, int[] iwork);

        static DgeconNode create() {
            return RFFIFactory.getLapackRFFI().createDgeconNode();
        }
    }

    interface DsyevrNode extends NodeInterface {

        int execute(char jobz, char range, char uplo, int n, double[] a, int lda, double vl, double vu, int il, int iu, double abstol, int[] m, double[] w, double[] z, int ldz, int[] isuppz,
                        double[] work, int lwork, int[] iwork, int liwork);

        static DsyevrNode create() {
            return RFFIFactory.getLapackRFFI().createDsyevrNode();
        }
    }

    IlaverNode createIlaverNode();

    DgeevNode createDgeevNode();

    Dgeqp3Node createDgeqp3Node();

    DormqrNode createDormqrNode();

    DtrtrsNode createDtrtrsNode();

    DgetrfNode createDgetrfNode();

    DpotrfNode createDpotrfNode();

    DpotriNode createDpotriNode();

    DpstrfNode createDpstrfNode();

    DgesvNode createDgesvNode();

    DlangeNode createDlangeNode();

    DgeconNode createDgeconNode();

    DsyevrNode createDsyevrNode();

}
