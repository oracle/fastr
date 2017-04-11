/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2014,  The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.and;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gt;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte0;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.isFinite;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.PrettyIntevals;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;

@RBuiltin(name = "pretty", kind = INTERNAL, parameterNames = {"l", "u", "n", "min.n", "shrink.sml", "hi", "eps.correct"}, behavior = PURE)
public abstract class Pretty extends RBuiltinNode.Arg7 {

    private static final RStringVector NAMES = RDataFactory.createStringVector(new String[]{"l", "u", "n"}, RDataFactory.COMPLETE_VECTOR);

    static {
        Casts casts = new Casts(Pretty.class);
        casts.arg("l").asDoubleVector().findFirst().mustBe(isFinite());
        casts.arg("u").asDoubleVector().findFirst().mustBe(isFinite());
        casts.arg("n").asIntegerVector().findFirst().mustNotBeNA().mustBe(gte0());
        casts.arg("min.n").asIntegerVector().findFirst().mustNotBeNA().mustBe(gte0());
        casts.arg("shrink.sml").asDoubleVector().findFirst().mustBe(and(isFinite(), gt(0.0)));
        casts.arg("hi").asDoubleVector().mustBe(size(2));
        casts.arg("eps.correct").defaultError(RError.Message.GENERIC, "'eps.correct' must be 0, 1, or 2").asIntegerVector().findFirst().mustNotBeNA().mustBe(and(gte0(), lte(2)));
    }

    public abstract RList execute(double l, double u, int n, int minN, double shrinkSml, RAbstractDoubleVector hi, int epsCorrect);

    @Specialization
    protected RList pretty(double l, double u, int n, int minN, double shrinkSml, RAbstractDoubleVector hi, int epsCorrect) {
        double hi0 = hi.getDataAt(0);
        double hi1 = hi.getDataAt(1);

        if (!RRuntime.isFinite(hi0) || hi0 < 0.0) {
            throw error(RError.Message.INVALID_ARGUMENT, "high.u.bias");
        }
        if (!RRuntime.isFinite(hi1) || hi1 < 0.0) {
            throw error(RError.Message.INVALID_ARGUMENT, "u5.bias");
        }
        double[] lo = new double[]{l};
        double[] up = new double[]{u};
        int[] ndiv = new int[]{n};
        PrettyIntevals.pretty(getErrorContext(), lo, up, ndiv, minN, shrinkSml, hi.materialize().getDataWithoutCopying(), epsCorrect, true);
        Object[] data = new Object[3];
        data[0] = lo[0];
        data[1] = up[0];
        data[2] = ndiv[0];
        return RDataFactory.createList(data, NAMES);
    }
}
