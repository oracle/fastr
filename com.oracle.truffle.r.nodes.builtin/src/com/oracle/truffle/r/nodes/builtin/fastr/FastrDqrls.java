/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastDoubleNode;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.ffi.RApplRFFI;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

/**
 * FastR specific internal used in R code of Cdqrls, which is in GunR implemented in C and invokes
 * directly this Fortran routine.
 */
@RBuiltin(name = ".fastr.dqrls", visibility = OFF, kind = PRIMITIVE, parameterNames = {"x", "n", "p", "y", "ny", "tol", "coeff"}, behavior = PURE)
public abstract class FastrDqrls extends RBuiltinNode.Arg7 {
    @Child private RApplRFFI.DqrlsNode dqrlsNode = RFFIFactory.getRFFI().getRApplRFFI().createDqrlsNode();

    private static final String[] NAMES = new String[]{"qr", "coefficients", "residuals", "effects", "rank", "pivot", "qraux", "tol", "pivoted"};
    private static RStringVector namesVector = null;

    static {
        Casts casts = new Casts(FastrDqrls.class);
        casts.arg("x").asDoubleVector(true, true, true);
        casts.arg("n").asIntegerVector().findFirst();
        casts.arg("p").asIntegerVector().findFirst();
        casts.arg("y").asDoubleVector(true, true, true);
        casts.arg("ny").asIntegerVector().findFirst();
        casts.arg("tol").asDoubleVector().findFirst();
        casts.arg("coeff").asDoubleVector(true, true, true);

    }

    @Specialization
    public RList doDouble(RAbstractDoubleVector xVec, int n, int p, RAbstractDoubleVector yVec, int ny, double tol, RAbstractDoubleVector coeffVec) {
        return call(xVec, xVec.materialize(), n, p, yVec, yVec.materialize(), ny, tol, coeffVec);
    }

    @Specialization(replaces = "doDouble")
    public RList doOther(RAbstractVector xVec, int n, int p, RAbstractVector yVec, int ny, double tol, RAbstractDoubleVector coeffVec, @Cached(value = "create()") CastDoubleNode castNode) {
        return call(xVec, ((RAbstractDoubleVector) castNode.execute(xVec)).materialize(), n, p, yVec, ((RAbstractDoubleVector) castNode.execute(yVec)).materialize(), ny, tol, coeffVec);
    }

    private RList call(RAbstractVector originalXVec, RDoubleVector xVec, int n, int p, RAbstractVector originalYVec, RDoubleVector yVec, int ny, double tol, RAbstractDoubleVector coeffVec) {
        double[] x = xVec.getDataTemp();
        double[] y = yVec.getDataTemp();
        double[] coeff = coeffVec.materialize().getDataTemp();
        double[] residuals = Arrays.copyOf(y, y.length);
        double[] effects = Arrays.copyOf(y, y.length);
        double[] qraux = new double[p];
        double[] work = new double[2 * p];
        int[] rank = new int[]{0};
        int[] pivot = new int[p];
        for (int i = 0; i < p; ++i) {
            pivot[i] = i + 1;
        }

        dqrlsNode.execute(x, n, p, y, ny, tol, coeff, residuals, effects, rank, pivot, qraux, work);

        RDoubleVector resultCoeffVect = RDataFactory.createDoubleVector(coeff, RDataFactory.COMPLETE_VECTOR);
        resultCoeffVect.copyAttributesFrom(coeffVec);

        RDoubleVector resultX = RDataFactory.createDoubleVector(x, RDataFactory.COMPLETE_VECTOR);
        resultX.copyAttributesFrom(originalXVec);

        RDoubleVector resultResiduals = RDataFactory.createDoubleVector(residuals, RDataFactory.COMPLETE_VECTOR);
        resultResiduals.copyAttributesFrom(originalYVec);

        Object[] data = new Object[]{
                        resultX,
                        resultCoeffVect,
                        resultResiduals,
                        RDataFactory.createDoubleVector(effects, RDataFactory.COMPLETE_VECTOR),
                        RDataFactory.createIntVectorFromScalar(rank[0]),
                        RDataFactory.createIntVector(pivot, RDataFactory.COMPLETE_VECTOR),
                        RDataFactory.createDoubleVector(qraux, RDataFactory.COMPLETE_VECTOR),
                        tol,
                        RDataFactory.createIntVectorFromScalar(0) // pivoted
        };

        return RDataFactory.createList(data, getNamesVector());
    }

    private static RStringVector getNamesVector() {
        if (namesVector == null) {
            namesVector = RDataFactory.createStringVector(NAMES, RDataFactory.COMPLETE_VECTOR);
        }
        return namesVector;
    }
}
