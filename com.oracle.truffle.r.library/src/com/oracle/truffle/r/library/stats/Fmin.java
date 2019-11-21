/*
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 2003-2004, The R Core Team
 * Copyright (c) 1998-2014, The R Foundation
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.isFinite;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notDoubleNA;
import static com.oracle.truffle.r.runtime.nmath.MathConstants.DBL_EPSILON;
import static com.oracle.truffle.r.runtime.nmath.TOMS708.fabs;
import static java.lang.Math.sqrt;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.function.call.RExplicitCallNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.RIntVector;

public final class Fmin extends RExternalBuiltinNode.Arg4 {

    static {
        Casts casts = new Casts(Fmin.class);
        casts.arg(0).defaultError(Message.GENERIC, "attempt to minimize non-function").mustBe(RFunction.class);
        casts.arg(1).asDoubleVector().findFirst().mustBe(isFinite().and(notDoubleNA()), Message.INVALID_VALUE, "xmin");
        casts.arg(2).asDoubleVector().findFirst().mustBe(isFinite().and(notDoubleNA()), Message.INVALID_VALUE, "xmax");
        casts.arg(3).defaultError(Message.INVALID_VALUE, "tol").asDoubleVector().findFirst().mustBe(isFinite().and(gt(0.0).and(notDoubleNA())));
    }

    public static Fmin create() {
        return new Fmin();
    }

    @Child private RExplicitCallNode explicitCallNode;
    @Child private BoxPrimitiveNode boxPrimitiveNode;
    @CompilationFinal private ConditionProfile isIntegerProfile;

    @Override
    public Object execute(Object arg1, Object arg2, Object arg3, Object arg4) {
        throw RInternalError.shouldNotReachHere(); // we override the 'call' method
    }

    @Override
    public Object call(VirtualFrame frame, RArgsValuesAndNames args) {
        checkLength(args, 4);
        return fmin(frame, (RFunction) castArg(args, 0), (Double) castArg(args, 1), (Double) castArg(args, 2), (Double) castArg(args, 3));
    }

    private Object fmin(VirtualFrame frame, RFunction fun, double xmin, double xmax, double tol) {
        if (xmin >= xmax) {
            throw error(Message.NOT_LESS_THAN, "xmin", "xmax");
        }
        return Brent_fmin(frame, xmin, xmax, fun, tol);
    }

    private double callFun(VirtualFrame frame, RFunction fun, double x) {
        Object result = box(explicitCall(frame, fun, x));
        if (getIsIntegerProfile().profile(result instanceof RIntVector)) {
            if (((RIntVector) result).getLength() != 1) {
                throw badValueError();
            }
            return ((RIntVector) result).getDataAt(0);
        } else if (result instanceof RAbstractDoubleVector) {
            if (((RAbstractDoubleVector) result).getLength() != 1) {
                throw badValueError();
            }
            return ((RAbstractDoubleVector) result).getDataAt(0);
        }
        throw badValueError();
    }

    private RError badValueError() {
        throw error(Message.INVALID_FUNCTION_VALUE, "optimize");
    }

    // Transcribed from GNU-R
    // Checkstyle: stop
    private double Brent_fmin(VirtualFrame frame, double ax, double bx, RFunction fun, double tol) {
        /* c is the squared inverse of the golden ratio */
        final double c = (3. - sqrt(5.)) * .5;

        /* Local variables */
        double a, b, d, e, p, q, r, u, v, w, x;
        double t2, fu, fv, fw, fx, xm, eps, tol1, tol3;

        /* eps is approximately the square root of the relative machine precision. */
        eps = DBL_EPSILON;
        tol1 = eps + 1.;/* the smallest 1.000... > 1 */
        eps = sqrt(eps);

        a = ax;
        b = bx;
        v = a + c * (b - a);
        w = v;
        x = v;

        d = 0.;/* -Wall */
        e = 0.;
        fx = callFun(frame, fun, x);
        fv = fx;
        fw = fx;
        tol3 = tol / 3.;

        /* main loop starts here ----------------------------------- */

        for (;;) {
            xm = (a + b) * .5;
            tol1 = eps * fabs(x) + tol3;
            t2 = tol1 * 2.;

            /* check stopping criterion */

            if (fabs(x - xm) <= t2 - (b - a) * .5)
                break;
            p = 0.;
            q = 0.;
            r = 0.;
            if (fabs(e) > tol1) { /* fit parabola */

                r = (x - w) * (fx - fv);
                q = (x - v) * (fx - fw);
                p = (x - v) * q - (x - w) * r;
                q = (q - r) * 2.;
                if (q > 0.)
                    p = -p;
                else
                    q = -q;
                r = e;
                e = d;
            }

            if (fabs(p) >= fabs(q * .5 * r) ||
                            p <= q * (a - x) || p >= q * (b - x)) { /* a golden-section step */

                if (x < xm)
                    e = b - x;
                else
                    e = a - x;
                d = c * e;
            } else { /* a parabolic-interpolation step */

                d = p / q;
                u = x + d;

                /* f must not be evaluated too close to ax or bx */

                if (u - a < t2 || b - u < t2) {
                    d = tol1;
                    if (x >= xm)
                        d = -d;
                }
            }

            /* f must not be evaluated too close to x */

            if (fabs(d) >= tol1)
                u = x + d;
            else if (d > 0.)
                u = x + tol1;
            else
                u = x - tol1;
            fu = callFun(frame, fun, u);

            /* update a, b, v, w, and x */

            if (fu <= fx) {
                if (u < x)
                    b = x;
                else
                    a = x;
                v = w;
                w = x;
                x = u;
                fv = fw;
                fw = fx;
                fx = fu;
            } else {
                if (u < x)
                    a = u;
                else
                    b = u;
                if (fu <= fw || Utils.identityEquals(w, x)) {
                    v = w;
                    fv = fw;
                    w = u;
                    fw = fu;
                } else if (fu <= fv || Utils.identityEquals(v, x) || Utils.identityEquals(v, w)) {
                    v = u;
                    fv = fu;
                }
            }
        }
        /* end of main loop */

        return x;
    }
    // Checkstyle: resume

    private ConditionProfile getIsIntegerProfile() {
        if (isIntegerProfile == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            isIntegerProfile = ConditionProfile.createBinaryProfile();
        }
        return isIntegerProfile;
    }

    private Object box(Object value) {
        if (boxPrimitiveNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            boxPrimitiveNode = insert(BoxPrimitiveNode.create());
        }
        return boxPrimitiveNode.execute(value);
    }

    private Object explicitCall(VirtualFrame frame, RFunction fun, double x) {
        if (explicitCallNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            explicitCallNode = insert(RExplicitCallNode.create());
        }
        return explicitCallNode.call(frame, fun, new RArgsValuesAndNames(new Object[]{x}, ArgumentsSignature.empty(1)));
    }
}
