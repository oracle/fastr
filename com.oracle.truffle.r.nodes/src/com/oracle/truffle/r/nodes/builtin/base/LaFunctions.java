/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.ffi.*;

/*
 * Logic derived from GNU-R, src/modules/lapack/Lapack.c
 */

/**
 * Lapack builtins.
 */
public class LaFunctions {

    @RBuiltin(name = "La_version", kind = INTERNAL)
    public abstract static class Version extends RBuiltinNode {
        @Specialization
        @SlowPath
        public String doVersion() {
            int[] version = new int[3];
            RFFIFactory.getRFFI().getLapackRFFI().ilaver(version);
            return version[0] + "." + version[1] + "." + version[2];
        }
    }

    public abstract static class LaHelper extends RBuiltinNode {

        protected void error(String msg) throws RError {
            CompilerDirectives.transferToInterpreter();
            throw RError.getGenericError(getEncapsulatingSourceSection(), msg);
        }

        protected void lapackError(String func, int info) {
            error("error code " + info + " from Lapack routine '" + func + "'");
        }
    }

    @RBuiltin(name = "La_rg", kind = INTERNAL)
    public abstract static class Rg extends LaHelper {

        private static final String[] NAMES = new String[]{"values", "vectors"};

        @Specialization
        public Object doRg(RDoubleVector matrix, byte onlyValues) {
            controlVisibility();
            if (!matrix.isMatrix()) {
                error("'x' must be a square numeric matrix");
            }
            int[] dims = matrix.getDimensions();
            if (onlyValues == RRuntime.LOGICAL_NA) {
                error("invalid \"only.values\" argument");
            }
            // copy array component of matrix as Lapack destroys it
            int n = dims[0];
            double[] a = matrix.getDataCopy();
            char jobVL = 'N';
            char jobVR = 'N';
            boolean vectors = onlyValues == RRuntime.LOGICAL_FALSE;
            if (vectors) {
                // TODO fix
                error("\"only.values == FALSE\" not implemented");
            }
            double[] left = null;
            double[] right = null;
            if (vectors) {
                jobVR = 'V';
                right = new double[a.length];
            }
            double[] wr = new double[n];
            double[] wi = new double[n];
            double[] work = new double[1];
            // ask for optimal size of work array
            int info = RFFIFactory.getRFFI().getLapackRFFI().dgeev(jobVL, jobVR, n, a, n, wr, wi, left, n, right, n, work, -1);
            if (info != 0) {
                lapackError("dgeev", info);
            }
            // now allocate work array and make the actual call
            int lwork = (int) work[0];
            work = new double[lwork];
            info = RFFIFactory.getRFFI().getLapackRFFI().dgeev(jobVL, jobVR, n, a, n, wr, wi, left, n, right, n, work, lwork);
            if (info != 0) {
                lapackError("dgeev", info);
            }
            // result is a list containing "values" and "vectors" (unless only.values is TRUE)
            boolean complexValues = false;
            for (int i = 0; i < n; i++) {
                if (Math.abs(wi[i]) > 10 * RAccuracyInfo.get().eps * Math.abs(wr[i])) {
                    complexValues = true;
                }
            }
            RVector values = null;
            Object vectorValues = RNull.instance;
            if (complexValues) {
                double[] data = new double[n * 2];
                for (int i = 0; i < n; i++) {
                    int ix = 2 * i;
                    data[ix] = wr[i];
                    data[ix + 1] = wi[i];
                }
                values = RDataFactory.createComplexVector(data, RDataFactory.COMPLETE_VECTOR);
                if (vectors) {
                    // TODO
                }
            } else {
                values = RDataFactory.createDoubleVector(wr, RDataFactory.COMPLETE_VECTOR);
                if (vectors) {
                    // TODO
                }
            }
            RStringVector names = RDataFactory.createStringVector(NAMES, RDataFactory.COMPLETE_VECTOR);
            RList result = RDataFactory.createList(new Object[]{values, vectorValues}, names);
            return result;
        }

    }

    @RBuiltin(name = "La_qr", kind = INTERNAL)
    public abstract static class Qr extends LaHelper {
        private static final String[] NAMES = new String[]{"qr", "rank", "qraux", "pivot"};

        @Specialization
        public RList doQr(RAbstractVector aIn) {
            // This implementation is sufficient for B25 matcal-5.
            if (!aIn.isMatrix()) {
                error("'a' must be a numeric matrix");
            }
            if (!(aIn instanceof RDoubleVector)) {
                error("non-real vectors not supported (yet)");
            }
            RDoubleVector daIn = (RDoubleVector) aIn;
            int[] dims = daIn.getDimensions();
            // copy array component of matrix as Lapack destroys it
            int n = dims[0];
            int m = dims[1];
            double[] a = daIn.getDataCopy();
            int[] jpvt = new int[n];
            double[] tau = new double[m < n ? m : n];
            double[] work = new double[1];
            // ask for optimal size of work array
            int info = RFFIFactory.getRFFI().getLapackRFFI().dgeqp3(m, n, a, m, jpvt, tau, work, -1);
            if (info < 0) {
                lapackError("dgeqp3", info);
            }
            int lwork = (int) work[0];
            work = new double[lwork];
            info = RFFIFactory.getRFFI().getLapackRFFI().dgeqp3(m, n, a, m, jpvt, tau, work, lwork);
            if (info < 0) {
                lapackError("dgeqp3", info);
            }
            Object[] data = new Object[4];
            // TODO check complete
            RDoubleVector ra = RDataFactory.createDoubleVector(a, RDataFactory.COMPLETE_VECTOR);
            // TODO check pivot
            ra.setDimensions(dims);
            data[0] = ra;
            data[1] = m < n ? m : n;
            data[2] = RDataFactory.createDoubleVector(tau, RDataFactory.COMPLETE_VECTOR);
            data[3] = RDataFactory.createIntVector(jpvt, RDataFactory.COMPLETE_VECTOR);
            return RDataFactory.createList(data, RDataFactory.createStringVector(NAMES, RDataFactory.COMPLETE_VECTOR));
        }
    }

}
