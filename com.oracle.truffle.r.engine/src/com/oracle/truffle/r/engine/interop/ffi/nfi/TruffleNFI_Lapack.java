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
package com.oracle.truffle.r.engine.interop.ffi.nfi;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.LapackRFFI;

public class TruffleNFI_Lapack implements LapackRFFI {
    enum Function {
        ilaver("([sint32]): void"),
        dgeev("(uint8, uint8, sint32, [double], sint32, [double], [double], [double], sint32, [double], sint32, [double], sint32) : sint32"),
        dgeqp3("(sint32, sint32, [double], sint32, [sint32], [double], [double], sint32) : sint32"),
        dormq("(uint8, uint8, sint32, sint32, sint32, [double], sint32, [double], [double], sint32, [double], sint32) : sint32"),
        dtrtrs("(uint8, uint8, uint8, sint32, sint32, [double], sint32, [double], sint32) : sint32"),
        dgetr("(sint32, sint32, [double], sint32, [sint32]) : sint32"),
        dpotrf("(uint8, sint32, [double], sint32) : sint32"),
        dpotri("(uint8, sint32, [double], sint32) : sint32"),
        dpstrf("uint8, sint32, [double], sint32, [sint32], [sint32], double, [double]) : sint32"),
        dgesv("(sint32, sint32, [double], sint32, [sint32], [double], sint32) : sint32"),
        dlange("(uint8, sint32, sint32, [double], sint32, [double]) : double"),
        dgecon("(uint8, sint32, [double], sint32, double, [double], [double], [sint32]) : sint32"),
        dsyevr("(uint8, uint8, uint8, sint32, [double], sint32, double, double, sint32, sint32, double, [sint32], [double], [double], sint32, [sint32], [double], sint32, [sint32], sint32) : sint32");

        private final int argCount;
        private final String signature;
        @CompilationFinal private Node executeNode;
        @CompilationFinal private TruffleObject function;

        Function(String signature) {
            this.argCount = TruffleNFI_Utils.getArgCount(signature);
            this.signature = signature;
        }

        private void initialize() {
            if (executeNode == null) {
                executeNode = Message.createExecute(argCount).createNode();
            }
            if (function == null) {
                function = TruffleNFI_Utils.lookupAndBind("call_" + name(), false, signature);
            }
        }
    }

    private static class TruffleNFI_IlaverNode extends IlaverNode {

        @Override
        public void execute(int[] version) {
            Function.ilaver.initialize();
            try {
                ForeignAccess.sendExecute(Function.ilaver.executeNode, Function.ilaver.function, JavaInterop.asTruffleObject(version));
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }

        }
    }

    private static class TruffleNFI_DgeevNode extends DgeevNode {

        @Override
        public int execute(char jobVL, char jobVR, int n, double[] a, int lda, double[] wr, double[] wi, double[] vl, int ldvl, double[] vr, int ldvr, double[] work, int lwork) {
            Function.dgeev.initialize();
            try {
                return (int) ForeignAccess.sendExecute(Function.dgeev.executeNode, Function.dgeev.function, jobVL, jobVR, n, JavaInterop.asTruffleObject(a), lda,
                                JavaInterop.asTruffleObject(wr), JavaInterop.asTruffleObject(wi), JavaInterop.asTruffleObject(vl), ldvl,
                                JavaInterop.asTruffleObject(vr), ldvr, JavaInterop.asTruffleObject(work), lwork);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_Dgeqp3Node extends Dgeqp3Node {

        @Override
        public int execute(int m, int n, double[] a, int lda, int[] jpvt, double[] tau, double[] work, int lwork) {
            Function.dgeqp3.initialize();
            try {
                return (int) ForeignAccess.sendExecute(Function.dgeqp3.executeNode, Function.dgeqp3.function, m, n, JavaInterop.asTruffleObject(a), lda, JavaInterop.asTruffleObject(jpvt),
                                JavaInterop.asTruffleObject(tau), JavaInterop.asTruffleObject(work), lwork);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DormqrNode extends DormqrNode {

        @Override
        public int execute(char side, char trans, int m, int n, int k, double[] a, int lda, double[] tau, double[] c, int ldc, double[] work, int lwork) {
            Function.dormq.initialize();
            try {
                return (int) ForeignAccess.sendExecute(Function.dormq.executeNode, Function.dormq.function, side, trans, m, n, k, JavaInterop.asTruffleObject(a), lda,
                                JavaInterop.asTruffleObject(tau), JavaInterop.asTruffleObject(c), ldc, JavaInterop.asTruffleObject(work), lwork);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DtrtrsNode extends DtrtrsNode {

        @Override
        public int execute(char uplo, char trans, char diag, int n, int nrhs, double[] a, int lda, double[] b, int ldb) {
            Function.dtrtrs.initialize();
            try {
                return (int) ForeignAccess.sendExecute(Function.dtrtrs.executeNode, Function.dtrtrs.function, uplo, trans, diag, n, nrhs, JavaInterop.asTruffleObject(a), lda,
                                JavaInterop.asTruffleObject(b), ldb);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DgetrfNode extends DgetrfNode {

        @Override
        public int execute(int m, int n, double[] a, int lda, int[] ipiv) {
            Function.dgetr.initialize();
            try {
                return (int) ForeignAccess.sendExecute(Function.dgetr.executeNode, Function.dgetr.function, m, n, JavaInterop.asTruffleObject(a), lda, JavaInterop.asTruffleObject(ipiv));
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DpotrfNode extends DpotrfNode {

        @Override
        public int execute(char uplo, int n, double[] a, int lda) {
            Function.dpotrf.initialize();
            try {
                return (int) ForeignAccess.sendExecute(Function.dpotrf.executeNode, Function.dpotrf.function, uplo, n, JavaInterop.asTruffleObject(a), lda);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DpotriNode extends DpotriNode {

        @Override
        public int execute(char uplo, int n, double[] a, int lda) {
            Function.dpotri.initialize();
            try {
                return (int) ForeignAccess.sendExecute(Function.dpotri.executeNode, Function.dpotrf.function, uplo, n, JavaInterop.asTruffleObject(a), lda);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DpstrfNode extends DpstrfNode {

        @Override
        public int execute(char uplo, int n, double[] a, int lda, int[] piv, int[] rank, double tol, double[] work) {
            Function.dpstrf.initialize();
            try {
                return (int) ForeignAccess.sendExecute(Function.dpstrf.executeNode, Function.dpstrf.function, uplo, n, JavaInterop.asTruffleObject(a), lda,
                                JavaInterop.asTruffleObject(piv), JavaInterop.asTruffleObject(rank), tol, JavaInterop.asTruffleObject(work));
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DgesvNode extends DgesvNode {

        @Override
        public int execute(int n, int nrhs, double[] a, int lda, int[] ipiv, double[] b, int ldb) {
            Function.dgesv.initialize();
            try {
                return (int) ForeignAccess.sendExecute(Function.dgesv.executeNode, Function.dgesv.function, n, nrhs, JavaInterop.asTruffleObject(a), lda, JavaInterop.asTruffleObject(ipiv),
                                JavaInterop.asTruffleObject(b), ldb);
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DlangeNode extends DlangeNode {

        @Override
        public double execute(char norm, int m, int n, double[] a, int lda, double[] work) {
            Function.dlange.initialize();
            try {
                return (double) ForeignAccess.sendExecute(Function.dlange.executeNode, Function.dlange.function, norm, m, n, JavaInterop.asTruffleObject(a), lda,
                                JavaInterop.asTruffleObject(work));
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DgeconNode extends DgeconNode {

        @Override
        public int execute(char norm, int n, double[] a, int lda, double anorm, double[] rcond, double[] work, int[] iwork) {
            Function.dgecon.initialize();
            try {
                return (int) ForeignAccess.sendExecute(Function.dgecon.executeNode, Function.dgecon.function, norm, n, JavaInterop.asTruffleObject(a), lda, anorm, JavaInterop.asTruffleObject(rcond),
                                JavaInterop.asTruffleObject(work), JavaInterop.asTruffleObject(iwork));
            } catch (InteropException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }
    }

    private static class TruffleNFI_DsyevrNode extends DsyevrNode {

        @Override
        public int execute(char jobz, char range, char uplo, int n, double[] a, int lda, double vl, double vu, int il, int iu, double abstol, int[] m, double[] w, double[] z, int ldz, int[] isuppz,
                        double[] work, int lwork, int[] iwork, int liwork) {
            Function.dsyevr.initialize();
            try {
                return (int) ForeignAccess.sendExecute(Function.dsyevr.executeNode, Function.dsyevr.function, jobz, range, uplo, n, JavaInterop.asTruffleObject(a),
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
