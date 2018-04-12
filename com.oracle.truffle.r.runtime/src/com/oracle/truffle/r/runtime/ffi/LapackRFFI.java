/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * Collection of statically typed Lapack methods that are used in the {@code base} package. The
 * signatures match the Fortran definition with the exception that the "info" value is returned as
 * the result of the call.
 *
 * The documentation for individual functions can be found in the
 * <a href="http://www.netlib.org/lapack/explore-html">spec</a>.
 */
public final class LapackRFFI {
    private final DownCallNodeFactory downCallNodeFactory;

    public LapackRFFI(DownCallNodeFactory downCallNodeFactory) {
        this.downCallNodeFactory = downCallNodeFactory;
    }

    public static final class IlaverNode extends NativeCallNode {
        public static IlaverNode create() {
            return RFFIFactory.getLapackRFFI().createIlaverNode();
        }

        private IlaverNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.ilaver));
        }

        public void execute(int[] version) {
            call(version);
        }
    }

    public static final class DgeevNode extends NativeCallNode {

        public static DgeevNode create() {
            return RFFIFactory.getLapackRFFI().createDgeevNode();
        }

        private DgeevNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.dgeev));
        }

        public int execute(char jobVL, char jobVR, int n, double[] a, int lda, double[] wr, double[] wi, double[] vl, int ldvl, double[] vr, int ldvr, double[] work, int lwork) {
            return (int) call(jobVL, jobVR, n, a, lda, wr, wi, vl, ldvl, vr, ldvr, work, lwork);
        }
    }

    public static final class Dgeqp3Node extends NativeCallNode {

        public static Dgeqp3Node create() {
            return RFFIFactory.getLapackRFFI().createDgeqp3Node();
        }

        private Dgeqp3Node(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.dgeqp3));
        }

        public int execute(int m, int n, double[] a, int lda, int[] jpvt, double[] tau, double[] work, int lwork) {
            return (int) call(m, n, a, lda, jpvt, tau, work, lwork);
        }
    }

    public static final class DormqrNode extends NativeCallNode {

        public static DormqrNode create() {
            return RFFIFactory.getLapackRFFI().createDormqrNode();
        }

        private DormqrNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.dormq));
        }

        public int execute(char side, char trans, int m, int n, int k, double[] a, int lda, double[] tau, double[] c, int ldc, double[] work, int lwork) {
            return (int) call(side, trans, m, n, k, a, lda, tau, c, ldc, work, lwork);
        }
    }

    public static final class DtrtrsNode extends NativeCallNode {

        public static DtrtrsNode create() {
            return RFFIFactory.getLapackRFFI().createDtrtrsNode();
        }

        private DtrtrsNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.dtrtrs));
        }

        public int execute(char uplo, char trans, char diag, int n, int nrhs, double[] a, int lda, double[] b, int ldb) {
            return (int) call(uplo, trans, diag, n, nrhs, a, lda, b, ldb);
        }
    }

    public static final class DgetrfNode extends NativeCallNode {

        public static DgetrfNode create() {
            return RFFIFactory.getLapackRFFI().createDgetrfNode();
        }

        private DgetrfNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.dgetrf));
        }

        public int execute(int m, int n, double[] a, int lda, int[] ipiv) {
            return (int) call(m, n, a, lda, ipiv);
        }
    }

    public static final class DpotrfNode extends NativeCallNode {

        public static DpotrfNode create() {
            return RFFIFactory.getLapackRFFI().createDpotrfNode();
        }

        private DpotrfNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.dpotrf));
        }

        public int execute(char uplo, int n, double[] a, int lda) {
            return (int) call(uplo, n, a, lda);
        }
    }

    public static final class DpotriNode extends NativeCallNode {

        public static DpotriNode create() {
            return RFFIFactory.getLapackRFFI().createDpotriNode();
        }

        private DpotriNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.dpotri));
        }

        public int execute(char uplo, int n, double[] a, int lda) {
            return (int) call(uplo, n, a, lda);
        }
    }

    public static final class DpstrfNode extends NativeCallNode {

        public static DpstrfNode create() {
            return RFFIFactory.getLapackRFFI().createDpstrfNode();
        }

        private DpstrfNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.dpstrf));
        }

        public int execute(char uplo, int n, double[] a, int lda, int[] piv, int[] rank, double tol, double[] work) {
            return (int) call(uplo, n, a, lda, piv, rank, tol, work);
        }
    }

    public static final class DgesvNode extends NativeCallNode {

        public static DgesvNode create() {
            return RFFIFactory.getLapackRFFI().createDgesvNode();
        }

        private DgesvNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.dgesv));
        }

        public int execute(int n, int nrhs, double[] a, int lda, int[] ipiv, double[] b, int ldb) {
            return (int) call(n, nrhs, a, lda, ipiv, b, ldb);
        }
    }

    public static final class DgesddNode extends NativeCallNode {

        public static DgesddNode create() {
            return RFFIFactory.getLapackRFFI().createDgesddNode();
        }

        private DgesddNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.dgesdd));
        }

        public int execute(char jobz, int m, int n, double[] a, int lda, double[] s, double[] u, int ldu, double[] vt, int ldtv, double[] work, int lwork, int[] iwork) {
            return (int) call(jobz, m, n, a, lda, s, u, ldu, vt, ldtv, work, lwork, iwork);
        }
    }

    public static final class DlangeNode extends NativeCallNode {

        public static DlangeNode create() {
            return RFFIFactory.getLapackRFFI().createDlangeNode();
        }

        private DlangeNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.dlange));
        }

        public double execute(char norm, int m, int n, double[] a, int lda, double[] work) {
            return (double) call(norm, m, n, a, lda, work);
        }
    }

    public static final class DgeconNode extends NativeCallNode {

        public static DgeconNode create() {
            return RFFIFactory.getLapackRFFI().createDgeconNode();
        }

        private DgeconNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.dgecon));
        }

        public int execute(char norm, int n, double[] a, int lda, double anorm, double[] rcond, double[] work, int[] iwork) {
            return (int) call(norm, n, a, lda, anorm, rcond, work, iwork);
        }
    }

    public static final class DsyevrNode extends NativeCallNode {

        public static DsyevrNode create() {
            return RFFIFactory.getLapackRFFI().createDsyevrNode();
        }

        private DsyevrNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.dsyevr));
        }

        public int execute(char jobz, char range, char uplo, int n, double[] a, int lda, double vl, double vu, int il, int iu, double abstol, int[] m, double[] w, double[] z, int ldz, int[] isuppz,
                        double[] work, int lwork, int[] iwork, int liwork) {
            return (int) call(jobz, range, uplo, n, a, lda, vl, vu, il, iu, abstol, m, w, z, ldz, isuppz, work, lwork, iwork, liwork);
        }
    }

    public static final class ZunmqrNode extends NativeCallNode {

        public static ZunmqrNode create() {
            return RFFIFactory.getLapackRFFI().createZunmqrNode();
        }

        private ZunmqrNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.zunmqr));
        }

        public int execute(String side, String trans, int m, int n, int k, double[] a, int lda, double[] tau, double[] c, int ldc, double[] work, int lwork) {
            return (int) call(side, trans, m, n, k, a, lda, tau, c, ldc, work, lwork);
        }

    }

    public static final class ZtrtrsNode extends NativeCallNode {

        public static ZtrtrsNode create() {
            return RFFIFactory.getLapackRFFI().createZtrtrsNode();
        }

        private ZtrtrsNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.ztrtrs));
        }

        public int execute(String uplo, String trans, String diag, int n, int nrhs, double[] a, int lda, double[] b, int ldb) {
            return (int) call(uplo, trans, diag, n, nrhs, a, lda, b, ldb);
        }
    }

    public static final class DtrsmNode extends NativeCallNode {

        public static DtrsmNode create() {
            return RFFIFactory.getLapackRFFI().createDtrsmNode();
        }

        private DtrsmNode(DownCallNodeFactory factory) {
            super(factory.createDownCallNode(NativeFunction.dtrsm));
        }

        public void execute(String side, String uplo, String transa, String diag, int m, int n, double alpha, double[] a, int lda, double[] b, int ldb) {
            call(side, uplo, transa, diag, m, n, alpha, a, lda, b, ldb);
        }

    }

    public IlaverNode createIlaverNode() {
        return new IlaverNode(downCallNodeFactory);
    }

    public DgeevNode createDgeevNode() {
        return new DgeevNode(downCallNodeFactory);
    }

    public Dgeqp3Node createDgeqp3Node() {
        return new Dgeqp3Node(downCallNodeFactory);
    }

    public DormqrNode createDormqrNode() {
        return new DormqrNode(downCallNodeFactory);
    }

    public DtrtrsNode createDtrtrsNode() {
        return new DtrtrsNode(downCallNodeFactory);
    }

    public DgetrfNode createDgetrfNode() {
        return new DgetrfNode(downCallNodeFactory);
    }

    public DpotrfNode createDpotrfNode() {
        return new DpotrfNode(downCallNodeFactory);
    }

    public DpotriNode createDpotriNode() {
        return new DpotriNode(downCallNodeFactory);
    }

    public DpstrfNode createDpstrfNode() {
        return new DpstrfNode(downCallNodeFactory);
    }

    public DgesvNode createDgesvNode() {
        return new DgesvNode(downCallNodeFactory);
    }

    public DgesddNode createDgesddNode() {
        return new DgesddNode(downCallNodeFactory);
    }

    public DlangeNode createDlangeNode() {
        return new DlangeNode(downCallNodeFactory);
    }

    public DgeconNode createDgeconNode() {
        return new DgeconNode(downCallNodeFactory);
    }

    public DsyevrNode createDsyevrNode() {
        return new DsyevrNode(downCallNodeFactory);
    }

    public ZunmqrNode createZunmqrNode() {
        return new ZunmqrNode(downCallNodeFactory);
    }

    public ZtrtrsNode createZtrtrsNode() {
        return new ZtrtrsNode(downCallNodeFactory);
    }

    public DtrsmNode createDtrsmNode() {
        return new DtrsmNode(downCallNodeFactory);
    }
}
