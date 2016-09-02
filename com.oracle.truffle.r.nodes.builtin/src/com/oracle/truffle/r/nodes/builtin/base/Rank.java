/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte0;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.intNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.rawValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RError.NO_CALLER;
import static com.oracle.truffle.r.runtime.RError.SHOW_CALLER;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_TIES_FOR_RANK;
import static com.oracle.truffle.r.runtime.RError.Message.INVALID_VALUE;
import static com.oracle.truffle.r.runtime.RError.Message.RANK_LARGE_N;
import static com.oracle.truffle.r.runtime.RError.Message.UNIMPLEMENTED_TYPE_IN_GREATER;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.function.Function;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.OrderNodeGen.CmpNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.OrderNodeGen.OrderVector1NodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "rank", kind = INTERNAL, parameterNames = {"x", "len", "ties.method"}, behavior = PURE)
public abstract class Rank extends RBuiltinNode {

    @Child private Order.OrderVector1Node orderVector1Node;
    @Child private Order.CmpNode orderCmpNode;
    private final BranchProfile errorProfile = BranchProfile.create();

    private static final Object rho = new Object();

    private enum TiesKind {
        AVERAGE,
        MAX,
        MIN
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        // @formatter:off
        Function<Object, Object> typeFunc = x -> x.getClass().getSimpleName();
        casts.arg("x").mustBe(abstractVectorValue(), SHOW_CALLER, UNIMPLEMENTED_TYPE_IN_GREATER, typeFunc).
                mustBe(rawValue().not(), SHOW_CALLER, RError.Message.RAW_SORT);
        // Note: in the case of no long vector support, when given anything but integer as n, GnuR behaves as if n=1,
        // we allow ourselves to be bit inconsistent with GnuR in that.
        casts.arg("len").defaultError(NO_CALLER, INVALID_VALUE, "length(xx)").mustBe(numericValue()).
                asIntegerVector().
                mustBe(notEmpty()).
                findFirst().mustBe(intNA().not().and(gte0()));
        // Note: we parse ties.methods in the Specialization anyway, so the validation of the value is there
        casts.arg("ties.method").defaultError(NO_CALLER, INVALID_TIES_FOR_RANK).mustBe(stringValue()).asStringVector().findFirst();
        // @formatter:on
    }

    private Order.OrderVector1Node initOrderVector1() {
        if (orderVector1Node == null) {
            orderVector1Node = insert(OrderVector1NodeGen.create());
        }
        return orderVector1Node;
    }

    private Order.CmpNode initOrderCmp() {
        if (orderCmpNode == null) {
            orderCmpNode = insert(CmpNodeGen.create());
        }
        return orderCmpNode;
    }

    @Specialization
    protected Object rank(RAbstractVector xa, int inN, String tiesMethod) {
        int n = inN;
        if (n > xa.getLength()) {
            errorProfile.enter();
            n = xa.getLength();
            RError.warning(SHOW_CALLER, RANK_LARGE_N);
        }

        TiesKind tiesKind = getTiesKind(tiesMethod);
        int[] ik = null;
        double[] rk = null;
        if (tiesKind == TiesKind.AVERAGE) {
            rk = new double[n];
        } else {
            ik = new int[n];
        }
        int[] indx = new int[n];
        for (int i = 0; i < n; i++) {
            indx[i] = i;
        }
        RIntVector indxVec = RDataFactory.createIntVector(indx, RDataFactory.COMPLETE_VECTOR);
        RAbstractVector x = xa instanceof RAbstractLogicalVector ? RClosures.createLogicalToIntVector((RAbstractLogicalVector) xa) : xa;
        initOrderVector1().execute(indxVec, x, true, false, rho);
        initOrderCmp();
        int j;
        for (int i = 0; i < n; i = j + 1) {
            j = i;
            while ((j < n - 1) && orderCmpNode.executeInt(x, indx[j], indx[j + 1], false) == 0) {
                j++;
            }
            switch (tiesKind) {
                case AVERAGE:
                    for (int k = i; k <= j; k++) {
                        rk[indx[k]] = (i + j + 2) / 2.;
                    }
                    break;
                case MAX:
                    for (int k = i; k <= j; k++) {
                        ik[indx[k]] = j + 1;
                    }
                    break;
                case MIN:
                    for (int k = i; k <= j; k++) {
                        ik[indx[k]] = i + 1;
                    }
                    break;
            }
        }
        if (tiesKind == TiesKind.AVERAGE) {
            return RDataFactory.createDoubleVector(rk, RDataFactory.COMPLETE_VECTOR);
        } else {
            return RDataFactory.createIntVector(ik, RDataFactory.COMPLETE_VECTOR);
        }
    }

    private TiesKind getTiesKind(String tiesMethod) {
        switch (tiesMethod) {
            case "average":
                return TiesKind.AVERAGE;
            case "max":
                return TiesKind.MAX;
            case "min":
                return TiesKind.MIN;
            default:
                errorProfile.enter();
                throw RError.error(NO_CALLER, RError.Message.GENERIC, "invalid ties.method for rank() [should never happen]");
        }
    }
}
