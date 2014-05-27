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

    @RBuiltin(name = "La_rg", kind = INTERNAL)
    public abstract static class Rg extends RBuiltinNode {

        private static final String[] NAMES = new String[]{"values", "vectors"};

        @Specialization
        public Object doRg(RDoubleVector matrix, byte onlyValues) {
            controlVisibility();
            if (!matrix.isMatrix()) {
                CompilerDirectives.transferToInterpreter();
                throw RError.getGenericError(getEncapsulatingSourceSection(), "'x' must be a square numeric matrix");
            }
            int[] dims = matrix.getDimensions();
            if (onlyValues == RRuntime.LOGICAL_NA) {
                CompilerDirectives.transferToInterpreter();
                throw RError.getGenericError(getEncapsulatingSourceSection(), "invalid \"only.values\" argument");
            }
            // copy array component of matrix as Lapack destroys it
            int n = dims[0];
            double[] a = matrix.getDataCopy();
            char jobVL = 'N';
            char jobVR = 'N';
            boolean vectors = onlyValues == RRuntime.LOGICAL_FALSE;
            if (vectors) {
                // TODO fix
                CompilerDirectives.transferToInterpreter();
                throw RError.getGenericError(getEncapsulatingSourceSection(), "\"only.values == FALSE\" not implemented");
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
                dgeevError(info);
            }
            // now allocate work array and make the actual call
            int lwork = (int) work[0];
            work = new double[lwork];
            info = RFFIFactory.getRFFI().getLapackRFFI().dgeev(jobVL, jobVR, n, a, n, wr, wi, left, n, right, n, work, lwork);
            if (info != 0) {
                dgeevError(info);
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

        private void dgeevError(int info) {
            CompilerDirectives.transferToInterpreter();
            throw RError.getGenericError(getEncapsulatingSourceSection(), "error code " + info + " from Lapack routine 'dgeev'");
        }
    }

}
