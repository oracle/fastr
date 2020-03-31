/*
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2003-2015, The R Foundation
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gt0;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.isFinite;
import static com.oracle.truffle.r.runtime.RRuntime.EPSILON;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.DBL_MAX;
import static com.oracle.truffle.r.runtime.nmath.TOMS708.fabs;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.function.call.RExplicitCallNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RIntVector;

public abstract class Zeroin2 extends RExternalBuiltinNode.Arg7 {

    @Child private RExplicitCallNode callNode = RExplicitCallNode.create();

    public static Zeroin2 create() {
        return Zeroin2NodeGen.create();
    }

    static {
        /* zeroin2(f, ax, bx, f.ax, f.bx, tol, int maxiter) */
        Casts casts = new Casts(Zeroin2.class);
        casts.arg(0).mustBe(instanceOf(RFunction.class), Message.MINIMIZE_NON_FUNCTION);
        casts.arg(1).asDoubleVector().findFirst().mustBe(isFinite(), Message.INVALID_VALUE, "xmin");
        casts.arg(2).asDoubleVector().findFirst().mustBe(isFinite(), Message.INVALID_VALUE, "xmax");
        casts.arg(3).asDoubleVector().findFirst().mustNotBeNA(Message.NA_NOT_ALLOWED, "f.lower");
        casts.arg(4).asDoubleVector().findFirst().mustNotBeNA(Message.NA_NOT_ALLOWED, "f.upper");
        casts.arg(5).asDoubleVector().findFirst().mustBe(isFinite(), Message.INVALID_VALUE, "tol");
        casts.arg(6).asIntegerVector().findFirst().mustBe(gt0(), Message.MUST_BE_POSITIVE, "maxiter");
    }

    @Specialization
    protected RDoubleVector zeroin2(RFunction f, double xmin, double xmax, double faxArg, double fbxArg, double tolArg, int maxIterArg) {
        double fax = faxArg;
        double fbx = fbxArg;
        double tol = tolArg;
        int maxIter = maxIterArg;
        if (xmin >= xmax) {
            error(Message.NOT_LESS_THAN, "xmin", "xmax");
        }
        double a = xmin;
        double b = xmax;
        double c = a;
        double fc = fax;
        double res;
        int maxit = maxIter + 1;

        finished: while (true) {
            /* First test if we have found a root at an endpoint */
            if (fax == 0.0) {
                tol = 0.0;
                maxIter = 0;
                res = a;
                break finished;
            }
            if (fbx == 0.0) {
                tol = 0.0;
                maxIter = 0;
                res = b;
                break finished;
            }

            while (maxit-- > 0) { // Main iteration loop
                double prevStep = b - a; /*
                                          * Distance from the last but one to the last approximation
                                          */
                double tolAct; /* Actual tolerance */
                double p; /* Interpolation step is calcu- */
                double q; /*
                           * lated in the form p/q; divi- sion operations is delayed until the last
                           * moment
                           */
                double newStep; /* Step at this iteration */

                if (fabs(fc) < fabs(fbx)) { /* Swap data for b to be the */
                    a = b;
                    b = c;
                    c = a; /* best approximation */
                    fax = fbx;
                    fbx = fc;
                    fc = fax;
                }
                tolAct = 2 * EPSILON * fabs(b) + tol / 2;
                newStep = (c - b) / 2;

                if (fabs(newStep) <= tolAct || fbx == 0.0d) {
                    maxIter -= maxit;
                    tol = fabs(c - b);
                    res = b;
                    break finished;
                }

                /* Decide if the interpolation can be tried */
                if (fabs(prevStep) >= tolAct /* If prev_step was large enough */
                                && fabs(fax) > fabs(fbx)) { /*
                                                             * and was in true direction,
                                                             * Interpolation may be tried
                                                             */
                    double t1;
                    double t2;
                    double cb = c - b;
                    if (Utils.identityEquals(a, c)) { /*
                                                       * If we have only two distinct points linear
                                                       * interpolation
                                                       */
                        t1 = fbx / fax; /* can only be applied */
                        p = cb * t1;
                        q = 1.0 - t1;
                    } else { // Quadric inverse interpolation

                        q = fax / fc;
                        t1 = fbx / fc;
                        t2 = fbx / fax;
                        p = t2 * (cb * q * (q - t1) - (b - a) * (t1 - 1.0));
                        q = (q - 1.0) * (t1 - 1.0) * (t2 - 1.0);
                    }
                    if (p > 0.0d) { /* p was calculated with the */
                        q = -q; /* opposite sign; make p positive */
                    } else { /* and assign possible minus to */
                        p = -p; /* q */
                    }

                    if (p < (0.75 * cb * q - fabs(tolAct * q) / 2) /*
                                                                    * If b+p/q falls in [b,c]
                                                                    */
                                    && p < fabs(prevStep * q / 2)) { /* and isn't too large */
                        newStep = p / q; /*
                                          * it is accepted If p/q is too large then the bisection
                                          * procedure can reduce [b,c] range to more extent
                                          */
                    }
                }

                if (fabs(newStep) < tolAct) { /* Adjust the step to be not less */
                    if (newStep > 0.0d) { /* than tolerance */
                        newStep = tolAct;
                    } else {
                        newStep = -tolAct;
                    }
                }
                a = b;
                fax = fbx; /* Save the previous approx. */
                b += newStep; /* Do step to a new approxim. */

                RArgsValuesAndNames args = new RArgsValuesAndNames(new Object[]{b}, ArgumentsSignature.empty(1));
                Object fRes = callNode.call(f.getEnclosingFrame(), f, args);
                if (fRes instanceof RIntVector) {
                    RIntVector vec = (RIntVector) fRes;
                    if (vec.getLength() == 1) {
                        fRes = vec.getDataAt(0);
                    }
                } else if (fRes instanceof RDoubleVector) {
                    RDoubleVector vec = (RDoubleVector) fRes;
                    if (vec.getLength() == 1) {
                        fRes = vec.getDataAt(0);
                    }
                }

                if (fRes instanceof Integer) {
                    int i = (Integer) fRes;
                    if (i == RRuntime.INT_NA) {
                        warning(Message.NA_REPLACED);
                        fbx = DBL_MAX;
                    } else {
                        fbx = i;
                    }
                } else if (fRes instanceof Double) {
                    double d = (Double) fRes;
                    if (!RRuntime.isFinite(d)) {
                        if (d == Double.NEGATIVE_INFINITY) {
                            warning(Message.MINUS_INF_REPLACED);
                            fbx = -DBL_MAX;
                        } else {
                            warning(Message.NA_INF_REPLACED);
                            fbx = DBL_MAX;
                        }
                    } else {
                        fbx = d;
                    }
                } else {
                    error(Message.INVALID_FUNCTION_VALUE, "zeroin");
                    fbx = 0.0;
                }

                if ((fbx > 0 && fc > 0) || (fbx < 0 && fc < 0)) {
                    /* Adjust c for it to have a sign opposite to that of b */
                    c = a;
                    fc = fax;
                }

            }
            /* failed! */
            tol = fabs(c - b);
            maxIter = -1;
            res = b;
            break finished;
        }
        return RDataFactory.createDoubleVector(new double[]{res, maxIter, tol}, true);
    }

}
