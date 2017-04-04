/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 2001-3 Paul Murrell
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.fastrGrid;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.fastrGrid.EdgeDetection.Bounds;
import com.oracle.truffle.r.library.fastrGrid.Unit.UnitConversionContext;
import com.oracle.truffle.r.library.fastrGrid.ViewPortTransform.GetViewPortTransformNode;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public abstract class LLocnBounds extends RExternalBuiltinNode.Arg3 {
    @Child private Unit.UnitToInchesNode unitToInches = Unit.createToInchesNode();
    @Child private GetViewPortTransformNode getViewPortTransform = new GetViewPortTransformNode();

    static {
        Casts casts = new Casts(LLocnBounds.class);
        casts.arg(0).mustBe(abstractVectorValue());
        casts.arg(1).mustBe(abstractVectorValue());
        casts.arg(2).mustBe(numericValue()).asDoubleVector().findFirst();
    }

    public static LLocnBounds create() {
        return LLocnBoundsNodeGen.create();
    }

    @Specialization
    @TruffleBoundary
    Object doBounds(RAbstractVector xVec, RAbstractVector yVec, double theta) {
        GridContext ctx = GridContext.getContext();
        GridDevice dev = ctx.getCurrentDevice();

        RList currentVP = ctx.getGridState().getViewPort();
        GPar gpar = GPar.create(ctx.getGridState().getGpar());
        ViewPortTransform vpTransform = getViewPortTransform.execute(currentVP, dev);
        ViewPortContext vpContext = ViewPortContext.fromViewPort(currentVP);
        UnitConversionContext conversionCtx = new UnitConversionContext(vpTransform.size, vpContext, dev, gpar);

        int length = GridUtils.maxLength(xVec, yVec);
        if (length == 0) {
            return RNull.instance;
        }

        double[] xx = new double[length];
        double[] yy = new double[length];
        Bounds bounds = new Bounds();
        int count = 0;
        for (int i = 0; i < length; i++) {
            Point loc = Point.fromUnits(unitToInches, xVec, yVec, i, conversionCtx);
            xx[i] = loc.x;
            yy[i] = loc.y;
            if (loc.isFinite()) {
                bounds.update(loc);
                count++;
            }
        }

        if (count == 0) {
            return RNull.instance;
        }

        Point edge = EdgeDetection.hullEdge(ctx, xx, yy, theta);
        double scale = ctx.getGridState().getScale();
        return GridUtils.createDoubleVector(edge.x / scale, edge.y / scale, bounds.getWidth() / scale, bounds.getHeight() / scale);
    }
}
