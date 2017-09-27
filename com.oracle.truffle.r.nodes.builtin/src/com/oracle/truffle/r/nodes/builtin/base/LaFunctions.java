/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.dimEq;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.dimGt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.emptyDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.matrix;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.missingValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.not;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.or;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.squareMatrix;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.READS_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.lang.ref.SoftReference;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.attributes.CopyAttributesNode;
import com.oracle.truffle.r.nodes.attributes.SetFixedAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetDimNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetNamesAttributeNode;
import com.oracle.truffle.r.nodes.attributes.UnaryCopyAttributesNode;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts.Casts;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleNodeGen;
import com.oracle.truffle.r.runtime.RAccuracyInfo;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.data.nodes.GetDataCopy;
import com.oracle.truffle.r.runtime.data.nodes.GetReadonlyData;
import com.oracle.truffle.r.runtime.ffi.LapackRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

/*
 * Logic derived from GNU-R, src/modules/lapack/Lapack.c
 */

/**
 * Lapack builtins.
 */
public class LaFunctions {

    @RBuiltin(name = "La_version", kind = INTERNAL, parameterNames = {}, behavior = READS_STATE)
    public abstract static class Version extends RBuiltinNode.Arg0 {
        @Child LapackRFFI.IlaverNode ilaverNode = RFFIFactory.getLapackRFFI().createIlaverNode();

        @Specialization
        @TruffleBoundary
        protected String doVersion() {
            int[] version = new int[3];
            ilaverNode.execute(version);
            return version[0] + "." + version[1] + "." + version[2];
        }
    }

    protected static final String[] NAMES = new String[]{"values", "vectors"};

    protected static Casts createCasts(Class<? extends RBuiltinNode> extClass) {
        Casts casts = new Casts(extClass);
        casts.arg("matrix").asDoubleVector(false, true, false).mustBe(squareMatrix(), Message.MUST_BE_SQUARE_NUMERIC, "x");
        casts.arg("onlyValues").defaultError(Message.INVALID_ARGUMENT, "only.values").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
        return casts;
    }

    @RBuiltin(name = "La_rg", kind = INTERNAL, parameterNames = {"matrix", "onlyValues"}, behavior = PURE)
    public abstract static class Rg extends RBuiltinNode.Arg2 {

        private final ConditionProfile hasComplexValues = ConditionProfile.createBinaryProfile();

        static {
            createCasts(Rg.class);
        }
        @Child private LapackRFFI.DgeevNode dgeevNode = LapackRFFI.DgeevNode.create();

        @Specialization
        protected Object doRg(RAbstractDoubleVector matrix, boolean onlyValues,
                        @Cached("create()") GetDimAttributeNode getDimsNode) {
            int[] dims = getDimsNode.getDimensions(matrix);
            // copy array component of matrix as Lapack destroys it
            int n = dims[0];
            double[] a = matrix.materialize().getDataCopy();
            char jobVL = 'N';
            char jobVR = 'N';
            boolean vectors = !onlyValues;
            double[] left = null;
            double[] right = null;
            if (vectors) {
                jobVR = 'V';
                right = new double[n * n];
            }
            double[] wr = new double[n];
            double[] wi = new double[n];
            double[] work = new double[1];
            // ask for optimal size of work array
            int info = dgeevNode.execute(jobVL, jobVR, n, a, n, wr, wi, left, n, right, n, work, -1);
            if (info != 0) {
                throw error(Message.LAPACK_ERROR, info, "dgeev");
            }
            // now allocate work array and make the actual call
            int lwork = (int) work[0];
            work = new double[lwork];
            info = dgeevNode.execute(jobVL, jobVR, n, a, n, wr, wi, left, n, right, n, work, lwork);
            if (info != 0) {
                throw error(Message.LAPACK_ERROR, info, "dgeev");
            }
            // result is a list containing "values" and "vectors" (unless only.values is TRUE)
            boolean complexValues = false;
            for (int i = 0; i < n; i++) {
                if (Math.abs(wi[i]) > 10 * RAccuracyInfo.get().eps * Math.abs(wr[i])) {
                    complexValues = true;
                    break;
                }
            }
            RVector<?> values = null;
            Object vectorValues = RNull.instance;
            if (hasComplexValues.profile(complexValues)) {
                double[] data = new double[n * 2];
                for (int i = 0; i < n; i++) {
                    int ix = 2 * i;
                    data[ix] = wr[i];
                    data[ix + 1] = wi[i];
                }
                values = RDataFactory.createComplexVector(data, RDataFactory.COMPLETE_VECTOR);
                if (vectors) {
                    vectorValues = unscramble(wi, n, right);
                }
            } else {
                values = RDataFactory.createDoubleVector(wr, RDataFactory.COMPLETE_VECTOR);
                if (vectors) {
                    double[] val = new double[n * n];
                    for (int i = 0; i < n * n; i++) {
                        val[i] = right[i];
                    }
                    vectorValues = RDataFactory.createDoubleVector(val, RDataFactory.COMPLETE_VECTOR, new int[]{n, n});
                }
            }
            RStringVector names = RDataFactory.createStringVector(NAMES, RDataFactory.COMPLETE_VECTOR);
            RList result = RDataFactory.createList(new Object[]{values, vectorValues}, names);
            return result;
        }

        private static RComplexVector unscramble(double[] imaginary, int n, double[] vecs) {
            double[] s = new double[2 * (n * n)];
            int j = 0;
            while (j < n) {
                if (imaginary[j] != 0) {
                    int j1 = j + 1;
                    for (int i = 0; i < n; i++) {
                        s[(i + n * j) << 1] = s[(i + n * j1) << 1] = vecs[i + j * n];
                        s[((i + n * j1) << 1) + 1] = -(s[((i + n * j) << 1) + 1] = vecs[i + j1 * n]);
                    }
                    j = j1;
                } else {
                    for (int i = 0; i < n; i++) {
                        s[(i + n * j) << 1] = vecs[i + j * n];
                        s[((i + n * j) << 1) + 1] = 0.0;
                    }
                }
                j++;
            }
            return RDataFactory.createComplexVector(s, RDataFactory.COMPLETE_VECTOR, new int[]{n, n});
        }
    }

    @RBuiltin(name = "La_rs", kind = INTERNAL, parameterNames = {"matrix", "onlyValues"}, behavior = PURE)
    public abstract static class Rs extends RBuiltinNode.Arg2 {

        static {
            createCasts(Rs.class);
        }
        @Child private LapackRFFI.DsyevrNode dsyevrNode = LapackRFFI.DsyevrNode.create();

        @Specialization
        protected Object doRs(RAbstractDoubleVector matrix, boolean onlyValues,
                        @Cached("create()") GetDimAttributeNode getDimsNode) {
            int[] dims = getDimsNode.getDimensions(matrix);
            int n = dims[0];
            char jobv = onlyValues ? 'N' : 'V';
            char uplo = 'L';
            char range = 'A';
            double vl = 0.0;
            double vu = 0.0;
            int il = 0;
            int iu = 0;
            double abstol = 0.0;
            double[] x = matrix.materialize().getDataCopy();

            double[] values = new double[n];

            double[] z = null;
            if (!onlyValues) {
                z = new double[n * n];
            }
            int lwork = -1;
            int liwork = -1;
            int[] m = new int[n];
            int[] isuppz = new int[2 * n];
            double[] work = new double[1];
            int[] iwork = new int[1];
            int info = dsyevrNode.execute(jobv, range, uplo, n, x, n, vl, vu, il, iu, abstol, m, values, z, n, isuppz, work, lwork, iwork, liwork);
            if (info != 0) {
                throw error(Message.LAPACK_ERROR, info, "dysevr");
            }
            lwork = (int) work[0];
            liwork = iwork[0];
            work = new double[lwork];
            iwork = new int[liwork];
            info = dsyevrNode.execute(jobv, range, uplo, n, x, n, vl, vu, il, iu, abstol, m, values, z, n, isuppz, work, lwork, iwork, liwork);
            if (info != 0) {
                throw error(Message.LAPACK_ERROR, info, "dysevr");
            }
            Object[] data = new Object[onlyValues ? 1 : 2];
            RStringVector names;
            data[0] = RDataFactory.createDoubleVector(values, RDataFactory.COMPLETE_VECTOR);
            if (!onlyValues) {
                data[1] = RDataFactory.createDoubleVector(z, RDataFactory.COMPLETE_VECTOR, new int[]{n, n});
                names = RDataFactory.createStringVector(NAMES, RDataFactory.COMPLETE_VECTOR);
            } else {
                names = RDataFactory.createStringVectorFromScalar(NAMES[0]);
            }
            return RDataFactory.createList(data, names);

        }
    }

    @RBuiltin(name = "La_qr", kind = INTERNAL, parameterNames = {"in"}, behavior = PURE)
    public abstract static class Qr extends RBuiltinNode.Arg1 {

        @CompilationFinal(dimensions = 1) private static final String[] NAMES = new String[]{"qr", "rank", "qraux", "pivot"};

        static {
            Casts casts = new Casts(Qr.class);
            casts.arg("in").asDoubleVector(false, true, false).mustBe(matrix(), Message.MUST_BE_NUMERIC_MATRIX, "a");
        }
        @Child private LapackRFFI.Dgeqp3Node dgeqp3Node = LapackRFFI.Dgeqp3Node.create();

        @Specialization
        protected RList doQr(RAbstractDoubleVector aIn,
                        @Cached("create()") GetDimAttributeNode getDimsNode,
                        @Cached("create()") SetDimAttributeNode setDimsNode) {
            // This implementation is sufficient for B25 matcal-5.
            int[] dims = getDimsNode.getDimensions(aIn);
            // copy array component of matrix as Lapack destroys it
            int n = dims[0];
            int m = dims[1];
            double[] a = aIn.materialize().getDataCopy();
            int[] jpvt = new int[n];
            double[] tau = new double[m < n ? m : n];
            double[] work = new double[1];
            // ask for optimal size of work array
            int info = dgeqp3Node.execute(m, n, a, m, jpvt, tau, work, -1);
            if (info < 0) {
                throw error(Message.LAPACK_ERROR, info, "dgeqp3");
            }
            int lwork = (int) work[0];
            work = new double[lwork];
            info = dgeqp3Node.execute(m, n, a, m, jpvt, tau, work, lwork);
            if (info < 0) {
                throw error(Message.LAPACK_ERROR, info, "dgeqp3");
            }
            Object[] data = new Object[4];
            // TODO check complete
            RDoubleVector ra = RDataFactory.createDoubleVector(a, RDataFactory.COMPLETE_VECTOR);
            // TODO check pivot
            setDimsNode.setDimensions(ra, dims);
            data[0] = ra;
            data[1] = m < n ? m : n;
            data[2] = RDataFactory.createDoubleVector(tau, RDataFactory.COMPLETE_VECTOR);
            data[3] = RDataFactory.createIntVector(jpvt, RDataFactory.COMPLETE_VECTOR);
            return RDataFactory.createList(data, RDataFactory.createStringVector(NAMES, RDataFactory.COMPLETE_VECTOR));
        }
    }

    @RBuiltin(name = "qr_coef_real", kind = INTERNAL, parameterNames = {"q", "b"}, behavior = PURE)
    public abstract static class QrCoefReal extends RBuiltinNode.Arg2 {

        private static final char SIDE = 'L';
        private static final char TRANS = 'T';

        static {
            Casts casts = new Casts(QrCoefReal.class);
            casts.arg("q").mustBe(instanceOf(RList.class));
            casts.arg("b").asDoubleVector(false, true, false).mustBe(matrix(), Message.MUST_BE_NUMERIC_MATRIX, "b");
        }

        @Child private LapackRFFI.DormqrNode dormqrNode = LapackRFFI.DormqrNode.create();
        @Child private LapackRFFI.DtrtrsNode dtrtrsNode = LapackRFFI.DtrtrsNode.create();

        @Specialization
        protected RDoubleVector doQrCoefReal(RList qIn, RAbstractDoubleVector b,
                        @Cached("create()") GetReadonlyData.Double qrToArrayNode,
                        @Cached("create()") GetReadonlyData.Double tauToArrayNode,
                        @Cached("create()") GetDimAttributeNode getBDimsNode,
                        @Cached("create()") GetDimAttributeNode getQDimsNode) {
            RDoubleVector qr = (RDoubleVector) qIn.getDataAt(0);

            RDoubleVector tau = (RDoubleVector) qIn.getDataAt(2);
            int k = tau.getLength();

            int[] bDims = getBDimsNode.getDimensions(b);
            int[] qrDims = getQDimsNode.getDimensions(qr);
            int n = qrDims[0];
            if (bDims[0] != n) {
                throw error(Message.RHS_SHOULD_HAVE_ROWS, n, bDims[0]);
            }
            int nrhs = bDims[1];
            double[] work = new double[1];
            // qr and tau do not really need copying
            double[] qrData = qrToArrayNode.execute(qr);
            double[] tauData = tauToArrayNode.execute(tau);
            // this will be the result, we are going to modify this array
            double[] bData = b.materialize().getDataCopy();
            // ask for optimal size of work array
            int info = dormqrNode.execute(SIDE, TRANS, n, nrhs, k, qrData, n, tauData, bData, n, work, -1);
            if (info < 0) {
                throw error(Message.LAPACK_ERROR, info, "dormqr");
            }
            int lwork = (int) work[0];
            work = new double[lwork];
            info = dormqrNode.execute(SIDE, TRANS, n, nrhs, k, qrData, n, tauData, bData, n, work, lwork);
            if (info < 0) {
                throw error(Message.LAPACK_ERROR, info, "dormqr");
            }
            info = dtrtrsNode.execute('U', 'N', 'N', k, nrhs, qrData, n, bData, n);
            if (info < 0) {
                throw error(Message.LAPACK_ERROR, info, "dtrtrs");
            }
            // TODO check complete
            return RDataFactory.createDoubleVector(bData, RDataFactory.INCOMPLETE_VECTOR);
        }
    }

    @RBuiltin(name = "det_ge_real", kind = INTERNAL, parameterNames = {"a", "uselog"}, behavior = PURE)
    public abstract static class DetGeReal extends RBuiltinNode.Arg2 {

        private static final RStringVector NAMES_VECTOR = RDataFactory.createStringVector(new String[]{"modulus", "sign"}, RDataFactory.COMPLETE_VECTOR);
        private static final RStringVector DET_CLASS = RDataFactory.createStringVector(new String[]{"det"}, RDataFactory.COMPLETE_VECTOR);

        private final ConditionProfile infoGreaterZero = ConditionProfile.createBinaryProfile();
        private final ConditionProfile doUseLog = ConditionProfile.createBinaryProfile();
        private final NACheck naCheck = NACheck.create();

        @Child private SetFixedAttributeNode setLogAttrNode = SetFixedAttributeNode.create("logarithm");

        static {
            Casts casts = new Casts(DetGeReal.class);
            casts.arg("a").asDoubleVector(false, true, false).mustBe(matrix(), Message.MUST_BE_NUMERIC_MATRIX, "a").mustBe(squareMatrix(), Message.MUST_BE_SQUARE_MATRIX, "a");

            casts.arg("uselog").defaultError(Message.MUST_BE_LOGICAL, "logarithm").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
        }

        @Child private LapackRFFI.DgetrfNode dgetrfNode = LapackRFFI.DgetrfNode.create();

        @Specialization
        protected RList doDetGeReal(RAbstractDoubleVector aIn, boolean useLog,
                        @Cached("create()") GetReadonlyData.Double vectorToArrayNode,
                        @Cached("create()") GetDimAttributeNode getDimsNode) {
            RDoubleVector a = (RDoubleVector) aIn.copy();
            int[] aDims = getDimsNode.getDimensions(aIn);
            int n = aDims[0];
            int[] ipiv = new int[n];
            double modulus = 0;
            double[] aData = vectorToArrayNode.execute(a);
            int info = dgetrfNode.execute(n, n, aData, n, ipiv);
            int sign = 1;
            if (info < 0) {
                throw error(Message.LAPACK_ERROR, info, "dgetrf");
            } else if (infoGreaterZero.profile(info > 0)) {
                modulus = useLog ? Double.NEGATIVE_INFINITY : 0;
            } else {
                for (int i = 0; i < n; i++) {
                    if (ipiv[i] != (i + 1)) {
                        sign = -sign;
                    }
                }
                // Note: Lapack may change NA to NaN, so we need to check the original vector
                naCheck.enable(aIn);
                if (doUseLog.profile(useLog)) {
                    modulus = 0.0;
                    int n1 = n + 1;
                    for (int i = 0; i < n; i++) {
                        double dii = aData[i * n1]; /* ith diagonal element */
                        if (naCheck.check(aIn.getDataAt(i * n1))) {
                            modulus = RRuntime.DOUBLE_NA;
                            break;
                        }
                        modulus += Math.log(dii < 0 ? -dii : dii);
                        if (dii < 0) {
                            sign = -sign;
                        }
                    }
                } else {
                    modulus = 1.0;
                    int n1 = n + 1;
                    for (int i = 0; i < n; i++) {
                        modulus *= aData[i * n1];
                        if (naCheck.check(aIn.getDataAt(i * n1))) {
                            modulus = RRuntime.DOUBLE_NA;
                            break;
                        }
                    }
                    if (modulus < 0 && !RRuntime.isNA(modulus)) {
                        modulus = -modulus;
                        sign = -sign;
                    }
                }
            }
            RDoubleVector modulusVec = RDataFactory.createDoubleVectorFromScalar(modulus);
            setLogAttrNode.execute(modulusVec, RRuntime.asLogical(useLog));
            RList result = RDataFactory.createList(new Object[]{modulusVec, sign}, NAMES_VECTOR);
            RVector.setVectorClassAttr(result, DET_CLASS);
            return result;
        }
    }

    @RBuiltin(name = "La_chol", kind = INTERNAL, parameterNames = {"a", "pivot", "tol"}, behavior = PURE)
    public abstract static class LaChol extends RBuiltinNode.Arg3 {

        private final ConditionProfile noPivot = ConditionProfile.createBinaryProfile();

        @Child private SetFixedAttributeNode setPivotAttrNode = SetFixedAttributeNode.create("pivot");
        @Child private SetFixedAttributeNode setRankAttrNode = SetFixedAttributeNode.create("rank");

        static {
            Casts casts = new Casts(LaChol.class);
            casts.arg("a").asDoubleVector(false, true, false).mustBe(matrix(), Message.MUST_BE_NUMERIC_MATRIX, "a").mustBe(squareMatrix(), Message.MUST_BE_SQUARE_MATRIX, "a").mustBe(
                            dimGt(1, 0), Message.DIMS_GT_ZERO, "a");

            casts.arg("pivot").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());

            casts.arg("tol").asDoubleVector().findFirst(RRuntime.DOUBLE_NA);
        }

        @Child private LapackRFFI.DpotrfNode dpotrfNode = LapackRFFI.DpotrfNode.create();
        @Child private LapackRFFI.DpstrfNode dpstrfNode = LapackRFFI.DpstrfNode.create();

        @Specialization
        protected RDoubleVector doDetGeReal(RAbstractDoubleVector aIn, boolean piv, double tol,
                        @Cached("create()") UnaryCopyAttributesNode copyAttributesNode,
                        @Cached("create()") GetDimAttributeNode getDimsNode,
                        @Cached("create()") SetDimNamesAttributeNode setDimNamesNode,
                        @Cached("create()") GetDimNamesAttributeNode getDimNamesNode) {
            double[] aData = aIn.materialize().getDataCopy();
            int[] aDims = getDimsNode.getDimensions(aIn);
            int n = aDims[0];
            int m = aDims[1];
            /* zero the lower triangle */
            for (int j = 0; j < n; j++) {
                for (int i = j + 1; i < n; i++) {
                    aData[i + n * j] = 0;
                }
            }

            int info;
            if (noPivot.profile(!piv)) {
                info = dpotrfNode.execute('U', m, aData, m);
                if (info != 0) {
                    CompilerDirectives.transferToInterpreter();
                    if (info > 0) {
                        throw error(Message.LAPACK_CHOL_NOT_POSITIVE_DEFINITE, info);
                    } else {
                        throw error(Message.LAPACK_ERROR, info, "dpotrf");
                    }
                }
                return (RDoubleVector) copyAttributesNode.execute(RDataFactory.createDoubleVector(aData, RDataFactory.INCOMPLETE_VECTOR), aIn);
            }

            int[] ipiv = new int[m];
            double[] work = new double[2 * m];
            int[] rank = new int[1];
            info = dpstrfNode.execute('U', n, aData, n, ipiv, rank, tol, work);
            if (info != 0) {
                CompilerDirectives.transferToInterpreter();
                if (info > 0) {
                    throw error(Message.LAPACK_CHOL_RANK_DEF_OR_INDEF, info);
                } else {
                    throw error(Message.LAPACK_ERROR, info, "dpotrf");
                }
            }

            RDoubleVector result = (RDoubleVector) copyAttributesNode.execute(RDataFactory.createDoubleVector(aData, RDataFactory.INCOMPLETE_VECTOR), aIn);
            setPivotAttrNode.execute(result, RRuntime.asLogical(piv));
            setRankAttrNode.execute(result, rank[0]);
            RList dn = getDimNamesNode.getDimNames(aIn);
            if (dn != null && dn.getDataAt(0) != null) {
                Object[] dn2 = new Object[m];
                // need to pivot the colnames
                for (int i = 0; i < m; i++) {
                    dn2[i] = dn.getDataAt(ipiv[i] - 1);
                }
                setDimNamesNode.setDimNames(result, RDataFactory.createList(dn2));
            }
            return result;
        }
    }

    @RBuiltin(name = "La_chol2inv", kind = INTERNAL, parameterNames = {"a", "size"}, behavior = PURE)
    public abstract static class LaChol2Inv extends RBuiltinNode.Arg2 {

        @Child private SetFixedAttributeNode setPivotAttrNode = SetFixedAttributeNode.create("pivot");
        @Child private SetFixedAttributeNode setRankAttrNode = SetFixedAttributeNode.create("rank");

        static {
            Casts casts = new Casts(LaChol2Inv.class);
            casts.arg("a").asDoubleVector(false, true, false).mustBe(matrix(), Message.MUST_BE_NUMERIC_MATRIX, "a");
            casts.arg("size").asIntegerVector().mustBe(notEmpty()).findFirst().mustBe(gt(0), Message.MUST_BE_POSITIVE_INT);
        }

        @Child private LapackRFFI.DpotriNode dpotriNode = LapackRFFI.DpotriNode.create();

        @Specialization
        protected RDoubleVector chol2inv(RAbstractDoubleVector a, int size,
                        @Cached("create()") GetDimAttributeNode getDimsNode) {

            int[] aDims = getDimsNode.getDimensions(a);
            int m = aDims[0];
            int n = aDims[1];

            if (size > n) {
                throw error(Message.CANNOT_EXCEED_X, "size", "ncol", n);
            }
            if (size > m) {
                throw error(Message.CANNOT_EXCEED_X, "size", "nrow", n);
            }
            double[] result = new double[size * size];
            for (int j = 0; j < size; j++) {
                for (int i = 0; i <= j; i++) {
                    result[i + j * size] = a.getDataAt(i + j * m);
                }
            }
            int info = dpotriNode.execute('U', size, result, size);
            if (info != 0) {
                if (info > 0) {
                    throw error(Message.LAPACK_ZERO_INVERSE, info, info);
                }
                throw error(Message.LAPACK_INVALID_VALUE, -info, "dpotri");
            }
            for (int j = 0; j < size; j++) {
                for (int i = j + 1; i < size; i++) {
                    result[i + j * size] = result[j + i * size];
                }
            }
            return RDataFactory.createDoubleVector(result, true, new int[]{size, size});
        }
    }

    private static final class NativeArrayCache {
        private final ThreadLocal<SoftReference<double[]>> cache = new ThreadLocal<>();

        @TruffleBoundary
        private double[] get(int minLength) {
            SoftReference<double[]> cached = cache.get();
            double[] array;
            if (cached != null) {
                array = cached.get();
                if (array != null && array.length >= minLength) {
                    return array;
                }
            }
            array = new double[minLength];
            cache.set(new SoftReference<>(array));
            return array;
        }
    }

    @RBuiltin(name = "La_solve", kind = INTERNAL, parameterNames = {"a", "bin", "tolin"}, behavior = PURE)
    public abstract static class LaSolve extends RBuiltinNode.Arg3 {
        @Child private CastDoubleNode castDouble = CastDoubleNodeGen.create(false, false, false);

        private static final NativeArrayCache aCache = new NativeArrayCache();

        private static Function<RAbstractDoubleVector, Object> getDimVal(int dim) {
            return vec -> vec.getDimensions()[dim];
        }

        static {
            Casts casts = new Casts(LaSolve.class);
            casts.arg("a").mustBe(numericValue()).asVector().mustBe(matrix(), Message.MUST_BE_NUMERIC_MATRIX, "a").mustBe(not(dimEq(0, 0)),
                            Message.GENERIC, "'a' is 0-diml").mustBe(squareMatrix(), Message.MUST_BE_SQUARE_MATRIX_SPEC, "a", getDimVal(0), getDimVal(1));

            casts.arg("bin").returnIf(missingValue().or(nullValue()), emptyDoubleVector()).asDoubleVector(false, true, false).mustBe(or(not(matrix()), not(dimEq(1, 0))), Message.GENERIC,
                            "no right-hand side in 'b'");

            casts.arg("tolin").asDoubleVector().findFirst(RRuntime.DOUBLE_NA);
        }

        @Child private LapackRFFI.DgesvNode dgesvNode = LapackRFFI.DgesvNode.create();
        @Child private LapackRFFI.DgeconNode dgeconNode = LapackRFFI.DgeconNode.create();
        @Child private LapackRFFI.DlangeNode dlangeNode = LapackRFFI.DlangeNode.create();

        @Specialization
        protected RDoubleVector laSolve(RAbstractVector a, RAbstractDoubleVector bin, double tol,
                        @Cached("create()") GetDimAttributeNode getADimsNode,
                        @Cached("create()") GetDimAttributeNode getBinDimsNode,
                        @Cached("create()") SetDimAttributeNode setBDimsNode,
                        @Cached("create()") SetDimNamesAttributeNode setBDimNamesNode,
                        @Cached("create()") GetDimNamesAttributeNode getADimNamesNode,
                        @Cached("create()") GetDimNamesAttributeNode getBinDimNamesNode,
                        @Cached("create()") SetNamesAttributeNode setNamesNode) {
            int[] aDims = getADimsNode.getDimensions(a);
            int n = aDims[0];
            if (n == 0) {
                throw error(Message.GENERIC, "'a' is 0-diml");
            }
            int n2 = aDims[1];
            if (n2 != n) {
                throw error(Message.MUST_BE_SQUARE, "a", n, n2);
            }
            RList aDn = getADimNamesNode.getDimNames(a);
            int p;
            double[] bData;
            RDoubleVector b;
            int[] bDims = getBinDimsNode.getDimensions(bin);
            if (GetDimAttributeNode.isMatrix(bDims)) {
                p = bDims[1];
                if (p == 0) {
                    throw error(Message.GENERIC, "no right-hand side in 'b'");
                }
                int p2 = bDims[0];
                if (p2 != n) {
                    throw error(Message.MUST_BE_SQUARE_COMPATIBLE, "b", p2, p, "a", n, n);
                }
                if (bin.getLength() == n * p) {
                    bData = bin.materialize().getDataNonShared();
                } else {
                    bData = new double[n];
                    // TODO: length for arraycopy is n*p, but bData is new double[n] ?? Should be
                    // rewritten to manually copy using getDataAt, or using a new node in
                    // c.o.t.r.runtime.data.nodes (the same in the "else" branch)
                    System.arraycopy(bin.materialize().getReadonlyData(), 0, bData, 0, n * p);
                }
                b = RDataFactory.createDoubleVector(bData, RDataFactory.COMPLETE_VECTOR);
                setBDimsNode.setDimensions(b, new int[]{n, p});
                RList binDn = getBinDimNamesNode.getDimNames(bin);
                // This is somewhat odd, but Matrix relies on dropping NULL dimnames
                if (aDn != null || binDn != null) {
                    // rownames(ans) = colnames(A), colnames(ans) = colnames(Bin)
                    if (aDn != null || binDn != null) {
                        Object[] bDnData = new Object[2];
                        bDnData[0] = aDn == null ? RNull.instance : aDn.getDataAt(1);
                        bDnData[1] = binDn == null ? RNull.instance : binDn.getDataAt(1);
                        setBDimNamesNode.setDimNames(b, RDataFactory.createList(bDnData));
                    }
                }
            } else {
                p = 1;
                if (bin.getLength() != n) {
                    throw error(Message.MUST_BE_SQUARE_COMPATIBLE, "b", bin.getLength(), p, "a", n, n);
                }
                if (bin.getLength() == n) {
                    bData = bin.materialize().getDataNonShared();
                } else {
                    bData = new double[n];
                    System.arraycopy(bin.materialize().getReadonlyData(), 0, bData, 0, n * p);
                }
                b = RDataFactory.createDoubleVector(bData, RDataFactory.COMPLETE_VECTOR);
                if (aDn != null) {
                    setNamesNode.setNames(b, RDataFactory.createStringVector((String) aDn.getDataAt(1)));
                }
            }

            int[] ipiv = new int[n];
            // work on a copy of A
            RDoubleVector aDouble;
            if (a instanceof RAbstractDoubleVector) {
                aDouble = ((RAbstractDoubleVector) a).materialize();
            } else {
                aDouble = (RDoubleVector) castDouble.doCast(a);
            }
            double[] avals;
            if (aDouble.isShared()) {
                avals = aCache.get(aDouble.getLength());
                // TODO: fixme more efficient copying
                System.arraycopy(aDouble.getDataCopy(), 0, avals, 0, n * p);
            } else {
                avals = aDouble.getDataCopy();
            }
            int info = dgesvNode.execute(n, p, avals, n, ipiv, bData, n);
            if (info < 0) {
                throw error(Message.LAPACK_INVALID_VALUE, -info, "dgesv");
            }
            if (info > 0) {
                throw error(Message.LAPACK_EXACTLY_SINGULAR, "dgesv", info, info);
            }
            if (tol > 0) {
                double anorm = dlangeNode.execute('1', n, n, avals, n, null);
                double[] work = new double[4 * n];
                double[] rcond = new double[1];
                dgeconNode.execute('1', n, avals, n, anorm, rcond, work, ipiv);
                if (rcond[0] < tol) {
                    throw error(Message.SYSTEM_COMP_SINGULAR, rcond[0]);
                }
            }
            return b;
        }
    }

    @RBuiltin(name = "La_svd", kind = INTERNAL, parameterNames = {"jobu", "x", "s", "u", "vt"}, behavior = PURE)
    public abstract static class Svd extends RBuiltinNode.Arg5 {

        static {
            Casts casts = new Casts(Svd.class);
            casts.arg("jobu").defaultError(Message.MUST_BE_STRING, "jobu").mustNotBeNull().mustBe(stringValue()).asStringVector().findFirst();
            casts.arg("x").mustNotBeNull().mustBe(doubleValue()).asDoubleVectorClosure(true, true, true);
            casts.arg("s").mustNotBeNull().mustBe(doubleValue()).asDoubleVector(true, true, true);
            casts.arg("u").mustNotBeNull().mustBe(doubleValue()).asDoubleVector(true, true, true);
            casts.arg("vt").mustNotBeNull().mustBe(doubleValue()).asDoubleVector(true, true, true);
        }

        @Child private LapackRFFI.DgesddNode dgesddNode = LapackRFFI.DgesddNode.create();

        @Specialization
        protected Object doSvd(String ju, RAbstractDoubleVector x, RAbstractDoubleVector s, RAbstractDoubleVector u, RAbstractDoubleVector vt,
                        @Cached("create()") GetDataCopy.Double getDataCopyNode,
                        @Cached("createCopyAllAttributes()") CopyAttributesNode copyAttrNode,
                        @Cached("create()") GetDimAttributeNode getDimsNode) {

            int[] xdims = getDimsNode.getDimensions(x);
            int n = xdims[0];
            int p = xdims[1];

            int[] udims = getDimsNode.getDimensions(u);
            int ldu = udims[0];

            int[] vtdims = getDimsNode.getDimensions(vt);
            int ldvt = vtdims[0];

            int[] iwork = new int[8 * Math.min(n, p)];

            RDoubleVector xMaterialized = x.materialize();
            RDoubleVector sMaterialized = s.materialize();
            RDoubleVector uMaterialized = u.materialize();
            RDoubleVector vtMaterialized = vt.materialize();

            double[] xvals = getDataCopyNode.execute(xMaterialized);
            double[] sdata = getDataCopyNode.execute(sMaterialized);
            double[] udata = getDataCopyNode.execute(uMaterialized);
            double[] vtdata = getDataCopyNode.execute(vtMaterialized);
            double[] tmp = new double[1];

// F77_CALL(dgesdd)(ju, &n, &p, xvals, &n, REAL(s), REAL(u), &ldu, REAL(vt), &ldvt, &tmp, &lwork,
// iwork, &info);
            int info = dgesddNode.execute(ju.charAt(0), n, p, xvals, n, sdata, udata, ldu, vtdata, ldvt, tmp, -1, iwork);
            if (info != 0) {
                error(Message.LAPACK_ERROR, info, "dgesdd");
            }

            int lwork = (int) tmp[0];
            double[] work = new double[lwork];
// F77_CALL(dgesdd)(ju, &n, &p, xvals, &n, REAL(s), REAL(u), &ldu, REAL(vt), &ldvt, work, &lwork,
// iwork, &info);
            dgesddNode.execute(ju.charAt(0), n, p, xvals, n, sdata, udata, ldu, vtdata, ldvt, work, lwork, iwork);
            if (info != 0) {
                error(Message.LAPACK_ERROR, info, "dgesdd");
            }

            RStringVector nm = RDataFactory.createStringVector(new String[]{"d", "u", "vt"}, true);
            Object[] val = new Object[3];
            RDoubleVector sResult = RDataFactory.createDoubleVector(sdata, false);
            RDoubleVector uResult = RDataFactory.createDoubleVector(udata, false);
            RDoubleVector vtResult = RDataFactory.createDoubleVector(vtdata, false);

            copyAttrNode.execute(sResult, sResult, sResult.getLength(), sMaterialized, sMaterialized.getLength());
            copyAttrNode.execute(uResult, uResult, uResult.getLength(), uMaterialized, uMaterialized.getLength());
            copyAttrNode.execute(vtResult, vtResult, vtResult.getLength(), vtMaterialized, vtMaterialized.getLength());

            val[0] = sResult;
            val[1] = uResult;
            val[2] = vtResult;

            return RDataFactory.createList(val, nm);
        }
    }
}
