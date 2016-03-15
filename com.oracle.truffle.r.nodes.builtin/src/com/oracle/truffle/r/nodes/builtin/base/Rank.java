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

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.OrderNodeGen.CmpNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.OrderNodeGen.OrderVector1NodeGen;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "rank", kind = INTERNAL, parameterNames = {"x", "len", "ties.method"})
public abstract class Rank extends RBuiltinNode {
    @Child Order.OrderVector1Node orderVector1Node;
    @Child Order.CmpNode orderCmpNode;

    private static final Object rho = new Object();

    private enum TiesKind {
        AVERAGE,
        MAX,
        MIN;
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toInteger(1);
    }

    Order.OrderVector1Node initOrderVector1() {
        if (orderVector1Node == null) {
            orderVector1Node = insert(OrderVector1NodeGen.create());
        }
        return orderVector1Node;
    }

    Order.CmpNode initOrderCmp() {
        if (orderCmpNode == null) {
            orderCmpNode = insert(CmpNodeGen.create());
        }
        return orderCmpNode;
    }

    @Specialization
    protected Object rank(RAbstractVector xa, int n, RAbstractStringVector tiesMethod) {
        if (n < 0 || RRuntime.isNA(n)) {
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "length(xx)");
        }
        if (xa instanceof RRawVector) {
            throw RError.error(this, RError.Message.RAW_SORT);
        }
        TiesKind tiesKind;
        switch (tiesMethod.getDataAt(0)) {
            case "average":
                tiesKind = TiesKind.AVERAGE;
                break;

            case "max":
                tiesKind = TiesKind.MAX;
                break;

            case "min":
                tiesKind = TiesKind.MIN;
                break;
            default:
                throw RError.error(this, RError.Message.GENERIC, "invalid ties.method for rank() [should never happen]");

        }
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

}
