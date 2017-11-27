/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2012, The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.doubleValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetDimAttributeNode;
import com.oracle.truffle.r.nodes.attributes.UnaryCopyAttributesNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder;
import com.oracle.truffle.r.nodes.helpers.AccessListField;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDataFactory.VectorFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.nodes.GetReadonlyData;
import com.oracle.truffle.r.runtime.ffi.StatsRFFI.LminflNode;

public abstract class Influence extends RExternalBuiltinNode.Arg4 {

    static {
        Casts casts = new Casts(Influence.class);
        casts.arg(0).mustBe(RList.class);
        casts.arg(1).asLogicalVector().findFirst().map(toBoolean());
        casts.arg(2).mustBe(doubleValue()).asDoubleVector();
        casts.arg(3).asDoubleVector().findFirst();
    }

    public static Influence create() {
        return InfluenceNodeGen.create();
    }

    @CompilationFinal private RStringVector namesWithCoef;
    @CompilationFinal private RStringVector names;
    @Child private LminflNode lminflNode = LminflNode.create();

    @Specialization
    RList doInfluence(RList mqr, boolean doCoef, RAbstractDoubleVector resid, double tol,
                    @Cached("create()") GetDimAttributeNode getDimAttribute,
                    @Cached("create()") GetReadonlyData.Double getReadonlyData,
                    @Cached("create()") AccessListField accessQrField,
                    @Cached("create()") AccessListField accessQrauxField,
                    @Cached("create()") AccessListField accessRankField,
                    @Cached("create()") UnaryCopyAttributesNode copyResidAttrs,
                    @Cached("create()") VectorFactory vectorFactory,
                    @Cached("create()") VectorFactory resultVectorFactory,
                    @Cached("createIntScalarCast()") CastNode scalarIntCast) {
        RAbstractDoubleVector qr = getDoubleField(mqr, accessQrField, "qr");
        RAbstractDoubleVector qraux = getDoubleField(mqr, accessQrauxField, "qraux");
        int n = getDimAttribute.nrows(qr);
        int k = (int) scalarIntCast.doCast(accessRankField.execute(mqr, "rank"));
        double[] hat = new double[n];
        double[] coefficients = doCoef ? new double[n * k] : new double[0];
        double[] sigma = new double[n];
        double[] residData = getReadonlyData.execute(resid.materialize());
        double[] qrData = getReadonlyData.execute(qr.materialize());
        double[] qrauxData = getReadonlyData.execute(qraux.materialize());
        // Note: it is OK to override data in "e" regardless of its sharing status, GNUR does it too
        lminflNode.execute(qrData, n, n, k, doCoef ? 1 : 0, qrauxData, residData, hat, coefficients, sigma, tol);
        for (int i = 0; i < n; i++) {
            if (hat[i] > 1. - tol) {
                hat[i] = 1.;
            }
        }
        Object[] ans = new Object[doCoef ? 4 : 3];
        int idx = 0;
        ans[idx++] = vectorFactory.createDoubleVector(hat, RDataFactory.COMPLETE_VECTOR);
        if (doCoef) {
            ans[idx++] = vectorFactory.createDoubleVector(coefficients, RDataFactory.COMPLETE_VECTOR, new int[]{n, k});
        }
        ans[idx++] = vectorFactory.createDoubleVector(sigma, RDataFactory.COMPLETE_VECTOR);
        RDoubleVector residResult = vectorFactory.createDoubleVector(residData, RDataFactory.COMPLETE_VECTOR);
        copyResidAttrs.execute(residResult, resid);
        ans[idx] = residResult;
        return resultVectorFactory.createList(ans, getNames(doCoef));
    }

    protected static CastNode createIntScalarCast() {
        return CastNodeBuilder.newCastBuilder().asIntegerVector().findFirst().buildCastNode();
    }

    private RStringVector getNames(boolean withCoef) {
        if (withCoef) {
            if (namesWithCoef == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                namesWithCoef = RDataFactory.getPermanent().createStringVector(new String[]{"hat", "coefficients", "sigma", "wt.res"}, RDataFactory.COMPLETE_VECTOR);
            }
            return namesWithCoef;
        }
        if (names == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            names = RDataFactory.getPermanent().createStringVector(new String[]{"hat", "sigma", "wt.res"}, RDataFactory.COMPLETE_VECTOR);
        }
        return names;
    }

    private RAbstractDoubleVector getDoubleField(RList mqr, AccessListField access, String name) {
        Object obj = access.execute(mqr, name);
        if (!(obj instanceof RAbstractDoubleVector)) {
            throw error(Message.INVALID_TYPE, "numeric", name, "numeric");
        }
        return (RAbstractDoubleVector) obj;
    }
}
