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

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.LapackRFFI;

public class TruffleNFI_Lapack implements LapackRFFI {

    private static class TruffleNFI_IlaverNode extends IlaverNode {
        @Child private Node message = NFIFunction.ilaver.createMessage();

        @Override
        public void execute(int[] version) {
            try {
                ForeignAccess.sendExecute(message, NFIFunction.ilaver.getFunction(), JavaInterop.asTruffleObject(version));
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DgeevNode extends DgeevNode {
        @Child private Node message = NFIFunction.dgeev.createMessage();

        @Override
        public int execute(char jobVL, char jobVR, int n, double[] a, int lda, double[] wr, double[] wi, double[] vl, int ldvl, double[] vr, int ldvr, double[] work, int lwork) {
            try {
                return (int) ForeignAccess.sendExecute(message, NFIFunction.dgeev.getFunction(), jobVL, jobVR, n, JavaInterop.asTruffleObject(a), lda,
                                JavaInterop.asTruffleObject(wr), JavaInterop.asTruffleObject(wi), JavaInterop.asTruffleObject(vl), ldvl,
                                JavaInterop.asTruffleObject(vr), ldvr, JavaInterop.asTruffleObject(work), lwork);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_Dgeqp3Node extends Dgeqp3Node {
        @Child private Node message = NFIFunction.dgeqp3.createMessage();

        @Override
        public int execute(int m, int n, double[] a, int lda, int[] jpvt, double[] tau, double[] work, int lwork) {
            try {
                return (int) ForeignAccess.sendExecute(message, NFIFunction.dgeqp3.getFunction(), m, n, JavaInterop.asTruffleObject(a), lda, JavaInterop.asTruffleObject(jpvt),
                                JavaInterop.asTruffleObject(tau), JavaInterop.asTruffleObject(work), lwork);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DormqrNode extends DormqrNode {
        @Child private Node message = NFIFunction.dormq.createMessage();

        @Override
        public int execute(char side, char trans, int m, int n, int k, double[] a, int lda, double[] tau, double[] c, int ldc, double[] work, int lwork) {
            try {
                return (int) ForeignAccess.sendExecute(message, NFIFunction.dormq.getFunction(), side, trans, m, n, k, JavaInterop.asTruffleObject(a), lda,
                                JavaInterop.asTruffleObject(tau), JavaInterop.asTruffleObject(c), ldc, JavaInterop.asTruffleObject(work), lwork);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DtrtrsNode extends DtrtrsNode {
        @Child private Node message = NFIFunction.dtrtrs.createMessage();

        @Override
        public int execute(char uplo, char trans, char diag, int n, int nrhs, double[] a, int lda, double[] b, int ldb) {
            try {
                return (int) ForeignAccess.sendExecute(message, NFIFunction.dtrtrs.getFunction(), uplo, trans, diag, n, nrhs, JavaInterop.asTruffleObject(a), lda,
                                JavaInterop.asTruffleObject(b), ldb);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DgetrfNode extends DgetrfNode {
        @Child private Node message = NFIFunction.dgetrf.createMessage();

        @Override
        public int execute(int m, int n, double[] a, int lda, int[] ipiv) {
            try {
                return (int) ForeignAccess.sendExecute(message, NFIFunction.dgetrf.getFunction(), m, n, JavaInterop.asTruffleObject(a), lda, JavaInterop.asTruffleObject(ipiv));
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DpotrfNode extends DpotrfNode {
        @Child private Node message = NFIFunction.dpotrf.createMessage();

        @Override
        public int execute(char uplo, int n, double[] a, int lda) {
            try {
                return (int) ForeignAccess.sendExecute(message, NFIFunction.dpotrf.getFunction(), uplo, n, JavaInterop.asTruffleObject(a), lda);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DpotriNode extends DpotriNode {
        @Child private Node message = NFIFunction.dpotri.createMessage();

        @Override
        public int execute(char uplo, int n, double[] a, int lda) {
            try {
                return (int) ForeignAccess.sendExecute(message, NFIFunction.dpotri.getFunction(), uplo, n, JavaInterop.asTruffleObject(a), lda);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DpstrfNode extends DpstrfNode {
        @Child private Node message = NFIFunction.dpstrf.createMessage();

        @Override
        public int execute(char uplo, int n, double[] a, int lda, int[] piv, int[] rank, double tol, double[] work) {
            try {
                return (int) ForeignAccess.sendExecute(message, NFIFunction.dpstrf.getFunction(), uplo, n, JavaInterop.asTruffleObject(a), lda,
                                JavaInterop.asTruffleObject(piv), JavaInterop.asTruffleObject(rank), tol, JavaInterop.asTruffleObject(work));
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DgesvNode extends DgesvNode {
        @Child private Node message = NFIFunction.dgesv.createMessage();

        @Override
        public int execute(int n, int nrhs, double[] a, int lda, int[] ipiv, double[] b, int ldb) {
            try {
                return (int) ForeignAccess.sendExecute(message, NFIFunction.dgesv.getFunction(), n, nrhs, JavaInterop.asTruffleObject(a), lda, JavaInterop.asTruffleObject(ipiv),
                                JavaInterop.asTruffleObject(b), ldb);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DlangeNode extends DlangeNode {
        @Child private Node message = NFIFunction.dlange.createMessage();

        @Override
        public double execute(char norm, int m, int n, double[] a, int lda, double[] work) {
            try {
                return (double) ForeignAccess.sendExecute(message, NFIFunction.dlange.getFunction(), norm, m, n, JavaInterop.asTruffleObject(a), lda,
                                JavaInterop.asTruffleObject(work));
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DgeconNode extends DgeconNode {
        @Child private Node message = NFIFunction.dgecon.createMessage();

        @Override
        public int execute(char norm, int n, double[] a, int lda, double anorm, double[] rcond, double[] work, int[] iwork) {
            try {
                return (int) ForeignAccess.sendExecute(message, NFIFunction.dgecon.getFunction(), norm, n, JavaInterop.asTruffleObject(a), lda, anorm, JavaInterop.asTruffleObject(rcond),
                                JavaInterop.asTruffleObject(work), JavaInterop.asTruffleObject(iwork));
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DsyevrNode extends DsyevrNode {
        @Child private Node message = NFIFunction.dsyevr.createMessage();

        @Override
        public int execute(char jobz, char range, char uplo, int n, double[] a, int lda, double vl, double vu, int il, int iu, double abstol, int[] m, double[] w, double[] z, int ldz, int[] isuppz,
                        double[] work, int lwork, int[] iwork, int liwork) {
            try {
                return (int) ForeignAccess.sendExecute(message, NFIFunction.dsyevr.getFunction(), jobz, range, uplo, n, JavaInterop.asTruffleObject(a),
                                lda, vl, vu, il, iu, abstol, JavaInterop.asTruffleObject(m), JavaInterop.asTruffleObject(w), JavaInterop.asTruffleObject(z), ldz,
                                JavaInterop.asTruffleObject(isuppz), JavaInterop.asTruffleObject(work), lwork, JavaInterop.asTruffleObject(iwork), liwork);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
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
