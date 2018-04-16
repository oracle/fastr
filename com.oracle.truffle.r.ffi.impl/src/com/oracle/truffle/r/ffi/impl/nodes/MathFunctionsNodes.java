/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.nodes.GetReadonlyData;
import com.oracle.truffle.r.runtime.ffi.interop.UnsafeAdapter;
import com.oracle.truffle.r.runtime.interop.ForeignArray2R;
import com.oracle.truffle.r.runtime.nmath.BesselFunctions;
import com.oracle.truffle.r.runtime.nmath.Beta;
import com.oracle.truffle.r.runtime.nmath.Choose;
import com.oracle.truffle.r.runtime.nmath.GammaFunctions;
import com.oracle.truffle.r.runtime.nmath.LBeta;
import com.oracle.truffle.r.runtime.nmath.MathConstants;
import com.oracle.truffle.r.runtime.nmath.RMath;
import com.oracle.truffle.r.runtime.nmath.distr.Logis.PLogis;
import com.oracle.truffle.r.runtime.nmath.distr.Pnorm.PnormBoth;

public final class MathFunctionsNodes {

    public abstract static class RfPnormBothNode extends FFIUpCallNode.Arg5 {

        @Child private Node cumIsPointerNode = Message.IS_POINTER.createNode();
        @Child private Node cumAsPointerNode;
        @Child private Node cumRead;
        @Child private Node cumWrite;
        @Child private Node ccumIsPointerNode = Message.IS_POINTER.createNode();
        @Child private Node ccumAsPointerNode;
        @Child private Node ccumWrite;

        @Specialization
        protected Object evaluate(double x, Object cum, Object ccum, int lowerTail, int logP) {
            // cum is R/W double* with size==1 and ccum is Writeonly double* with size==1
            double[] cumArr = new double[1];
            double[] ccumArr = new double[1];
            long cumPtr;
            long ccumPtr;
            TruffleObject cumTO = (TruffleObject) cum;
            if (ForeignAccess.sendIsPointer(cumIsPointerNode, cumTO)) {
                if (cumAsPointerNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    cumAsPointerNode = insert(Message.AS_POINTER.createNode());
                }
                try {
                    cumPtr = ForeignAccess.sendAsPointer(cumAsPointerNode, cumTO);
                    cumArr[0] = UnsafeAdapter.UNSAFE.getDouble(cumPtr);
                } catch (UnsupportedMessageException e) {
                    throw RInternalError.shouldNotReachHere("IS_POINTER message returned true, AS_POINTER should not fail");
                }
            } else {
                cumPtr = 0L;
                if (cumRead == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    cumRead = Message.READ.createNode();
                    cumWrite = Message.WRITE.createNode();
                }
                try {
                    cumArr[0] = ((Number) ForeignAccess.sendRead(cumRead, cumTO, 0)).doubleValue();
                } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }

            TruffleObject ccumTO = (TruffleObject) ccum;
            if (ForeignAccess.sendIsPointer(ccumIsPointerNode, ccumTO)) {
                if (ccumAsPointerNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    ccumAsPointerNode = insert(Message.AS_POINTER.createNode());
                }
                try {
                    ccumPtr = ForeignAccess.sendAsPointer(ccumAsPointerNode, ccumTO);
                } catch (UnsupportedMessageException e) {
                    throw RInternalError.shouldNotReachHere("IS_POINTER message returned true, AS_POINTER should not fail");
                }
            } else {
                ccumPtr = 0L;
                if (ccumWrite == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    ccumWrite = Message.WRITE.createNode();
                }
            }

            PnormBoth.evaluate(x, cumArr, ccumArr, lowerTail != 0, logP != 0);
            if (cumPtr != 0L) {
                UnsafeAdapter.UNSAFE.putDouble(cumPtr, cumArr[0]);
            } else {
                try {
                    ForeignAccess.sendWrite(cumWrite, cumTO, 0, cumArr[0]);
                } catch (InteropException e) {
                    throw RInternalError.shouldNotReachHere("WRITE message support expected");
                }
            }
            if (ccumPtr != 0L) {
                UnsafeAdapter.UNSAFE.putDouble(ccumPtr, ccumArr[0]);
            } else {
                try {
                    ForeignAccess.sendWrite(ccumWrite, ccumTO, 0, ccumArr[0]);
                } catch (InteropException e) {
                    throw RInternalError.shouldNotReachHere("WRITE message support expected");
                }
            }
            return RNull.instance;
        }

        public static RfPnormBothNode create() {
            return MathFunctionsNodesFactory.RfPnormBothNodeGen.create();
        }
    }

    public abstract static class Log1pmxNode extends FFIUpCallNode.Arg1 {

        @Specialization
        protected Object evaluate(double a) {
            return GammaFunctions.log1pmx(a);
        }

        public static Log1pmxNode create() {
            return MathFunctionsNodesFactory.Log1pmxNodeGen.create();
        }
    }

    public abstract static class Log1pexpNode extends FFIUpCallNode.Arg1 {

        @Specialization
        protected Object evaluate(double a) {
            return PLogis.log1pexp(a);
        }

        public static Log1pexpNode create() {
            return MathFunctionsNodesFactory.Log1pexpNodeGen.create();
        }
    }

    public abstract static class Lgamma1pNode extends FFIUpCallNode.Arg1 {

        @Specialization
        protected Object evaluate(double a) {
            return GammaFunctions.lgamma1p(a);
        }

        public static Lgamma1pNode create() {
            return MathFunctionsNodesFactory.Lgamma1pNodeGen.create();
        }
    }

    public abstract static class LogspaceAddNode extends FFIUpCallNode.Arg2 {

        @Specialization
        protected Object evaluate(double a, double b) {
            return MathConstants.logspaceAdd(a, b);
        }

        public static LogspaceAddNode create() {
            return MathFunctionsNodesFactory.LogspaceAddNodeGen.create();
        }
    }

    public abstract static class LogspaceSubNode extends FFIUpCallNode.Arg2 {

        @Specialization
        protected Object evaluate(double a, double b) {
            return MathConstants.logspaceSub(a, b);
        }

        public static LogspaceSubNode create() {
            return MathFunctionsNodesFactory.LogspaceSubNodeGen.create();
        }
    }

    public abstract static class GammafnNode extends FFIUpCallNode.Arg1 {

        @Specialization
        protected Object evaluate(double a) {
            return GammaFunctions.gammafn(a);
        }

        public static GammafnNode create() {
            return MathFunctionsNodesFactory.GammafnNodeGen.create();
        }
    }

    public abstract static class LGammafnNode extends FFIUpCallNode.Arg1 {

        @Specialization
        protected Object evaluate(double a) {
            return GammaFunctions.lgammafn(a);
        }

        public static LGammafnNode create() {
            return MathFunctionsNodesFactory.LGammafnNodeGen.create();
        }
    }

    public abstract static class LGammafnSignNode extends FFIUpCallNode.Arg2 {

        @Child private Node sgnIsPointerNode = Message.IS_POINTER.createNode();
        @Child private Node sgnAsPointerNode;
        @Child private Node sgnRead;
        @Child private Node sgnWrite;

        @Specialization
        protected Object evaluate(double a, Object sgn) {
            int[] sgnArr = new int[1];
            long sgnPtr;
            TruffleObject sgnTO = (TruffleObject) sgn;
            if (ForeignAccess.sendIsPointer(sgnIsPointerNode, sgnTO)) {
                if (sgnAsPointerNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    sgnAsPointerNode = insert(Message.AS_POINTER.createNode());
                }
                try {
                    sgnPtr = ForeignAccess.sendAsPointer(sgnAsPointerNode, sgnTO);
                    sgnArr[0] = UnsafeAdapter.UNSAFE.getInt(sgnPtr);
                } catch (UnsupportedMessageException e) {
                    throw RInternalError.shouldNotReachHere("IS_POINTER message returned true, AS_POINTER should not fail");
                }
            } else {
                sgnPtr = 0L;
                if (sgnRead == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    sgnRead = Message.READ.createNode();
                    sgnWrite = Message.WRITE.createNode();
                }
                try {
                    sgnArr[0] = ((Number) ForeignAccess.sendRead(sgnRead, sgnTO, 0)).intValue();
                } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }

            double result = GammaFunctions.lgammafnSign(a, sgnArr);
            if (sgnPtr != 0L) {
                UnsafeAdapter.UNSAFE.putInt(sgnPtr, sgnArr[0]);
            } else {
                try {
                    ForeignAccess.sendWrite(sgnWrite, sgnTO, 0, sgnArr[0]);
                } catch (InteropException e) {
                    throw RInternalError.shouldNotReachHere("WRITE message support expected");
                }
            }
            return result;
        }

        public static LGammafnSignNode create() {
            return MathFunctionsNodesFactory.LGammafnSignNodeGen.create();
        }
    }

    public abstract static class DpsiFnNode extends FFIUpCallNode.Arg7 {

        @Child private Node ansIsPointerNode = Message.IS_POINTER.createNode();
        @Child private Node ansAsPointerNode;
        @Child private Node ansRead;
        @Child private Node ansWrite;
        @Child private Node nzIsPointerNode = Message.IS_POINTER.createNode();
        @Child private Node nzAsPointerNode;
        @Child private Node nzRead;
        @Child private Node nzWrite;
        @Child private Node ierrIsPointerNode = Message.IS_POINTER.createNode();
        @Child private Node ierrAsPointerNode;
        @Child private Node ierrRead;
        @Child private Node ierrWrite;

        @Specialization
        protected Object evaluate(double x, int n, int kode, int m, Object ans, Object nz, Object ierr) {
            // ans is R/W double* size==1 and nz is R/W int* size==1
            // ierr is R/W int* size==1
            double ansIn;
            int nzIn;
            int ierrIn;
            TruffleObject ansTO = (TruffleObject) ans;
            long ansPtr;
            if (ForeignAccess.sendIsPointer(ansIsPointerNode, ansTO)) {
                if (ansAsPointerNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    ansAsPointerNode = insert(Message.AS_POINTER.createNode());
                }
                try {
                    ansPtr = ForeignAccess.sendAsPointer(ansAsPointerNode, ansTO);
                    ansIn = UnsafeAdapter.UNSAFE.getDouble(ansPtr);
                } catch (UnsupportedMessageException e) {
                    throw RInternalError.shouldNotReachHere("IS_POINTER message returned true, AS_POINTER should not fail");
                }
            } else {
                ansPtr = 0L;
                if (ansRead == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    ansRead = Message.READ.createNode();
                    ansWrite = Message.WRITE.createNode();
                }
                try {
                    ansIn = ((Number) ForeignAccess.sendRead(ansRead, ansTO, 0)).doubleValue();
                } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }

            TruffleObject nzTO = (TruffleObject) nz;
            long nzPtr;
            if (ForeignAccess.sendIsPointer(nzIsPointerNode, nzTO)) {
                if (nzAsPointerNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    nzAsPointerNode = insert(Message.AS_POINTER.createNode());
                }
                try {
                    nzPtr = ForeignAccess.sendAsPointer(nzAsPointerNode, nzTO);
                    nzIn = UnsafeAdapter.UNSAFE.getInt(nzPtr);
                } catch (UnsupportedMessageException e) {
                    throw RInternalError.shouldNotReachHere("IS_POINTER message returned true, AS_POINTER should not fail");
                }
            } else {
                nzPtr = 0L;
                if (nzRead == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    nzRead = Message.READ.createNode();
                    nzWrite = Message.WRITE.createNode();
                }
                try {
                    nzIn = ((Number) ForeignAccess.sendRead(nzRead, nzTO, 0)).intValue();
                } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }

            TruffleObject ierrTO = (TruffleObject) ierr;
            long ierrPtr;
            if (ForeignAccess.sendIsPointer(ierrIsPointerNode, ierrTO)) {
                if (ierrAsPointerNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    ierrAsPointerNode = insert(Message.AS_POINTER.createNode());
                }
                try {
                    ierrPtr = ForeignAccess.sendAsPointer(ierrAsPointerNode, ierrTO);
                    ierrIn = UnsafeAdapter.UNSAFE.getInt(ierrPtr);
                } catch (UnsupportedMessageException e) {
                    throw RInternalError.shouldNotReachHere("IS_POINTER message returned true, AS_POINTER should not fail");
                }
            } else {
                ierrPtr = 0L;
                if (ierrRead == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    ierrRead = Message.READ.createNode();
                    ierrWrite = Message.WRITE.createNode();
                }
                try {
                    ierrIn = ((Number) ForeignAccess.sendRead(ierrRead, ierrTO, 0)).intValue();
                } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                    throw RInternalError.shouldNotReachHere(e);
                }
            }

            GammaFunctions.DpsiFnResult result = GammaFunctions.dpsifn(x, n, kode, ansIn, nzIn, ierrIn);
            if (ansPtr != 0L) {
                UnsafeAdapter.UNSAFE.putDouble(ansPtr, result.ans);
            } else {
                try {
                    ForeignAccess.sendWrite(ansWrite, ansTO, 0, result.ans);
                } catch (InteropException e) {
                    throw RInternalError.shouldNotReachHere("WRITE message support expected");
                }
            }
            if (nzPtr != 0L) {
                UnsafeAdapter.UNSAFE.putInt(nzPtr, result.nz);
            } else {
                try {
                    ForeignAccess.sendWrite(nzWrite, nzTO, 0, result.nz);
                } catch (InteropException e) {
                    throw RInternalError.shouldNotReachHere("WRITE message support expected");
                }
            }
            if (ierrPtr != 0L) {
                UnsafeAdapter.UNSAFE.putInt(ierrPtr, result.ierr);
            } else {
                try {
                    ForeignAccess.sendWrite(ierrWrite, ierrTO, 0, result.ierr);
                } catch (InteropException e) {
                    throw RInternalError.shouldNotReachHere("WRITE message support expected");
                }
            }
            return RNull.instance;
        }

        public static DpsiFnNode create() {
            return MathFunctionsNodesFactory.DpsiFnNodeGen.create();
        }
    }

    public abstract static class PsiGammaNode extends FFIUpCallNode.Arg2 {

        @Specialization
        protected Object evaluate(double x, double deriv) {
            return GammaFunctions.psigamma(x, deriv);
        }

        public static PsiGammaNode create() {
            return MathFunctionsNodesFactory.PsiGammaNodeGen.create();
        }
    }

    public abstract static class DiGammaNode extends FFIUpCallNode.Arg1 {

        @Specialization
        protected Object evaluate(double x) {
            return GammaFunctions.digamma(x);
        }

        public static DiGammaNode create() {
            return MathFunctionsNodesFactory.DiGammaNodeGen.create();
        }
    }

    public abstract static class TriGammaNode extends FFIUpCallNode.Arg1 {

        @Specialization
        protected Object evaluate(double x) {
            return GammaFunctions.trigamma(x);
        }

        public static TriGammaNode create() {
            return MathFunctionsNodesFactory.TriGammaNodeGen.create();
        }
    }

    public abstract static class TetraGammaNode extends FFIUpCallNode.Arg1 {

        @Specialization
        protected Object evaluate(double x) {
            return GammaFunctions.tetragamma(x);
        }

        public static TetraGammaNode create() {
            return MathFunctionsNodesFactory.TetraGammaNodeGen.create();
        }
    }

    public abstract static class PentaGammaNode extends FFIUpCallNode.Arg1 {

        @Specialization
        protected Object evaluate(double x) {
            return GammaFunctions.pentagamma(x);
        }

        public static PentaGammaNode create() {
            return MathFunctionsNodesFactory.PentaGammaNodeGen.create();
        }
    }

    public abstract static class BetaNode extends FFIUpCallNode.Arg2 {

        @Specialization
        protected Object evaluate(double a, double b) {
            return Beta.beta(a, b);
        }

        public static BetaNode create() {
            return MathFunctionsNodesFactory.BetaNodeGen.create();
        }
    }

    public abstract static class LBetaNode extends FFIUpCallNode.Arg2 {

        @Specialization
        protected Object evaluate(double a, double b) {
            return LBeta.lbeta(a, b);
        }

        public static LBetaNode create() {
            return MathFunctionsNodesFactory.LBetaNodeGen.create();
        }
    }

    public abstract static class ChooseNode extends FFIUpCallNode.Arg2 {

        @Specialization
        protected Object evaluate(double n, double k) {
            return Choose.choose(n, k);
        }

        public static ChooseNode create() {
            return MathFunctionsNodesFactory.ChooseNodeGen.create();
        }
    }

    public abstract static class LChooseNode extends FFIUpCallNode.Arg2 {

        @Specialization
        protected Object evaluate(double n, double k) {
            return Choose.lchoose(n, k);
        }

        public static LChooseNode create() {
            return MathFunctionsNodesFactory.LChooseNodeGen.create();
        }
    }

    public abstract static class BesselINode extends FFIUpCallNode.Arg3 {

        @Specialization
        protected Object evaluate(double a, double b, double c) {
            return BesselFunctions.bessel_i(a, b, c);
        }

        public static BesselINode create() {
            return MathFunctionsNodesFactory.BesselINodeGen.create();
        }
    }

    public abstract static class BesselJNode extends FFIUpCallNode.Arg2 {

        @Specialization
        protected Object evaluate(double a, double b) {
            return BesselFunctions.bessel_j(a, b);
        }

        public static BesselJNode create() {
            return MathFunctionsNodesFactory.BesselJNodeGen.create();
        }
    }

    public abstract static class BesselKNode extends FFIUpCallNode.Arg3 {

        @Specialization
        protected Object evaluate(double a, double b, double c) {
            return BesselFunctions.bessel_k(a, b, c);
        }

        public static BesselKNode create() {
            return MathFunctionsNodesFactory.BesselKNodeGen.create();
        }
    }

    public abstract static class BesselYNode extends FFIUpCallNode.Arg2 {

        @Specialization
        protected Object evaluate(double a, double b) {
            return BesselFunctions.bessel_y(a, b);
        }

        public static BesselYNode create() {
            return MathFunctionsNodesFactory.BesselYNodeGen.create();
        }
    }

    public abstract static class BesselIExNode extends FFIUpCallNode.Arg4 {

        @Specialization
        protected Object evaluate(final double a, final double b, final double c, Object d,
                        @Cached("create()") BesselExNode besselEx) {
            return besselEx.execute(new BesselExCaller() {
                @Override
                public double call(double[] arr) {
                    return BesselFunctions.bessel_i_ex(a, b, c, arr);
                }

                @Override
                public int arrLen() {
                    return (int) Math.floor(b) + 1;
                }
            }, d);
        }

        public static BesselIExNode create() {
            return MathFunctionsNodesFactory.BesselIExNodeGen.create();
        }
    }

    public abstract static class BesselJExNode extends FFIUpCallNode.Arg3 {

        @Specialization
        protected Object evaluate(final double a, final double b, Object c,
                        @Cached("create()") BesselExNode besselEx) {
            return besselEx.execute(new BesselExCaller() {
                @Override
                public double call(double[] arr) {
                    return BesselFunctions.bessel_j_ex(a, b, arr);
                }

                @Override
                public int arrLen() {
                    return (int) Math.floor(b) + 1;
                }
            }, c);
        }

        public static BesselJExNode create() {
            return MathFunctionsNodesFactory.BesselJExNodeGen.create();
        }
    }

    public abstract static class BesselKExNode extends FFIUpCallNode.Arg4 {

        @Specialization
        protected Object evaluate(double a, double b, double c, Object d,
                        @Cached("create()") BesselExNode besselEx) {
            return besselEx.execute(new BesselExCaller() {
                @Override
                public double call(double[] arr) {
                    return BesselFunctions.bessel_k_ex(a, b, c, arr);
                }

                @Override
                public int arrLen() {
                    return (int) Math.floor(b) + 1;
                }
            }, d);
        }

        public static BesselKExNode create() {
            return MathFunctionsNodesFactory.BesselKExNodeGen.create();
        }
    }

    public abstract static class BesselYExNode extends FFIUpCallNode.Arg3 {

        @Specialization
        protected Object evaluate(double a, double b, Object c,
                        @Cached("create()") BesselExNode besselEx) {
            return besselEx.execute(new BesselExCaller() {
                @Override
                public double call(double[] arr) {
                    return BesselFunctions.bessel_y_ex(a, b, arr);
                }

                @Override
                public int arrLen() {
                    return (int) Math.floor(b) + 1;
                }
            }, c);
        }

        public static BesselYExNode create() {
            return MathFunctionsNodesFactory.BesselYExNodeGen.create();
        }
    }

    interface BesselExCaller {

        double call(double[] arr);

        int arrLen();

    }

    abstract static class BesselExNode extends Node {

        @Child private Node bIsPointerNode = Message.IS_POINTER.createNode();
        @Child private Node bAsPointerNode;
        @Child private ForeignArray2R bForeignArray2R;

        public abstract double execute(BesselExCaller caller, Object b);

        @Specialization
        protected double besselEx(BesselExCaller caller, Object b,
                        @Cached("create()") GetReadonlyData.Double bReadonlyData) {
            RAbstractDoubleVector bVec;
            TruffleObject bTO = (TruffleObject) b;
            if (ForeignAccess.sendIsPointer(bIsPointerNode, bTO)) {
                if (bAsPointerNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    bAsPointerNode = insert(Message.AS_POINTER.createNode());
                }
                long addr;
                try {
                    addr = ForeignAccess.sendAsPointer(bAsPointerNode, bTO);
                    bVec = RDataFactory.createDoubleVectorFromNative(addr, caller.arrLen());
                } catch (UnsupportedMessageException e) {
                    throw RInternalError.shouldNotReachHere("IS_POINTER message returned true, AS_POINTER should not fail");
                }
            } else {
                if (bForeignArray2R == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    bForeignArray2R = ForeignArray2R.create();
                }
                bVec = (RAbstractDoubleVector) bForeignArray2R.convert(bTO);
            }

            return caller.call(bReadonlyData.execute(bVec.materialize()));
        }

        public static BesselExNode create() {
            return MathFunctionsNodesFactory.BesselExNodeGen.create();
        }

    }

    public abstract static class SignNode extends FFIUpCallNode.Arg1 {

        @Specialization
        protected Object evaluate(double x) {
            return RMath.sign(x);
        }

        public static SignNode create() {
            return MathFunctionsNodesFactory.SignNodeGen.create();
        }
    }

    public abstract static class FPrecNode extends FFIUpCallNode.Arg2 {

        @Specialization
        protected Object evaluate(double x, double digits) {
            return RMath.fprec(x, digits);
        }

        public static FPrecNode create() {
            return MathFunctionsNodesFactory.FPrecNodeGen.create();
        }
    }

    public abstract static class SinpiNode extends FFIUpCallNode.Arg1 {

        @Specialization
        protected Object evaluate(double x) {
            return RMath.sinpi(x);
        }

        public static SinpiNode create() {
            return MathFunctionsNodesFactory.SinpiNodeGen.create();
        }
    }

    public abstract static class CospiNode extends FFIUpCallNode.Arg1 {

        @Specialization
        protected Object evaluate(double x) {
            return RMath.cospi(x);
        }

        public static CospiNode create() {
            return MathFunctionsNodesFactory.CospiNodeGen.create();
        }
    }

    public abstract static class TanpiNode extends FFIUpCallNode.Arg1 {

        @Specialization
        protected Object evaluate(double x) {
            return RMath.tanpi(x);
        }

        public static TanpiNode create() {
            return MathFunctionsNodesFactory.TanpiNodeGen.create();
        }
    }

}
