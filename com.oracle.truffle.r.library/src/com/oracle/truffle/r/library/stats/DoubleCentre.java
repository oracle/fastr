/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995--2015, The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;

public abstract class DoubleCentre extends RExternalBuiltinNode.Arg1 {

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg(0).asDoubleVector();
    }

    @Specialization
    protected RDoubleVector doubleCentre(RAbstractDoubleVector aVecAbs) {
        RDoubleVector aVec = aVecAbs.materialize();
        int n = RRuntime.nrows(aVec);
        double[] a = aVec.getDataWithoutCopying(); // does not copy

        for (int i = 0; i < n; i++) {
            double sum = 0;
            for (int j = 0; j < n; j++) {
                sum += a[i + j * n];
            }
            sum /= n;
            for (int j = 0; j < n; j++) {
                a[i + j * n] -= sum;
            }
        }
        for (int j = 0; j < n; j++) {
            double sum = 0;
            for (int i = 0; i < n; i++) {
                sum += a[i + j * n];
            }
            sum /= n;
            for (int i = 0; i < n; i++) {
                a[i + j * n] -= sum;
            }
        }
        return aVec;
    }
}
