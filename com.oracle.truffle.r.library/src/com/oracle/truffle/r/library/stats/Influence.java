/*
 * Copyright (c) 2012, The R Core Team
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder;
import com.oracle.truffle.r.nodes.helpers.AccessListField;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.nodes.GetReadonlyData;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.runtime.ffi.StatsRFFI.LminflNode;
import com.oracle.truffle.r.runtime.nodes.unary.CastNode;

/**
 * Transcribed from GNU-R's src/library/stats/src/influence.c
 */
public abstract class Influence extends RExternalBuiltinNode.Arg3 {

    static {
        Casts casts = new Casts(Influence.class);
        casts.arg(0).mustBe(RList.class);
        casts.arg(1).mustBe(doubleValue()).asDoubleVector();
        casts.arg(2).asDoubleVector().findFirst();
    }

    public static Influence create() {
        return InfluenceNodeGen.create();
    }

    @CompilationFinal private RStringVector names;
    @Child private LminflNode lminflNode = LminflNode.create();

    @Specialization
    RList doInfluence(RList mqr, RDoubleVector resid, double tol,
                    @Cached("create()") GetDimAttributeNode getDimAttribute,
                    @Cached("create()") GetReadonlyData.Double getReadonlyData,
                    @Cached("create()") AccessListField accessQrField,
                    @Cached("create()") AccessListField accessQrauxField,
                    @Cached("create()") AccessListField accessRankField,
                    @Cached("create()") VectorFactory vectorFactory,
                    @Cached("create()") VectorFactory resultVectorFactory,
                    @Cached("createIntScalarCast()") CastNode scalarIntCast) {
        RDoubleVector qr = getDoubleField(mqr, accessQrField, "qr");
        RDoubleVector qraux = getDoubleField(mqr, accessQrauxField, "qraux");
        int n = getDimAttribute.nrows(qr);
        int k = (int) scalarIntCast.doCast(accessRankField.execute(mqr, "rank"));
        int q = getDimAttribute.ncols(resid);
        double[] hat = new double[n];
        double[] sigma = new double[n];
        double[] residData = getReadonlyData.execute(resid.materialize());
        double[] qrData = getReadonlyData.execute(qr.materialize());
        double[] qrauxData = getReadonlyData.execute(qraux.materialize());
        // Note: it is OK to override data in "e" ("residData") regardless of its sharing status, GNUR does it too
        lminflNode.execute(qrData, n, n, k, q, qrauxData, residData, hat, sigma, tol);
        for (int i = 0; i < n; i++) {
            if (hat[i] > 1. - tol) {
                hat[i] = 1.;
            }
        }
        Object[] ans = new Object[2];
        ans[0] = vectorFactory.createDoubleVector(hat, RDataFactory.COMPLETE_VECTOR);
        ans[1] = vectorFactory.createDoubleVector(sigma, RDataFactory.COMPLETE_VECTOR);
        return resultVectorFactory.createList(ans, getNames());
    }

    protected static CastNode createIntScalarCast() {
        return CastNodeBuilder.newCastBuilder().asIntegerVector().findFirst().buildCastNode();
    }

    private RStringVector getNames() {
        if (names == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            names = RDataFactory.getPermanent().createStringVector(new String[]{"hat", "sigma"}, RDataFactory.COMPLETE_VECTOR);
        }
        return names;
    }

    private RDoubleVector getDoubleField(RList mqr, AccessListField access, String name) {
        Object obj = access.execute(mqr, name);
        if (!(obj instanceof RDoubleVector)) {
            throw error(Message.INVALID_TYPE, "numeric", name, "numeric");
        }
        return (RDoubleVector) obj;
    }
}
