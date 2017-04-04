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
import com.oracle.truffle.r.runtime.nmath.RMath;

public abstract class LCircleBounds extends RExternalBuiltinNode.Arg4 {
    @Child private Unit.UnitToInchesNode unitToInches = Unit.createToInchesNode();
    @Child private GetViewPortTransformNode getViewPortTransform = new GetViewPortTransformNode();

    static {
        Casts casts = new Casts(LCircleBounds.class);
        LCircle.addCircleCasts(casts);
        casts.arg(3).mustBe(numericValue()).asDoubleVector().findFirst();
    }

    public static LCircleBounds create() {
        return LCircleBoundsNodeGen.create();
    }

    @Specialization
    @TruffleBoundary
    Object doCircle(RAbstractVector xVec, RAbstractVector yVec, RAbstractVector radiusVec, double theta) {
        GridContext ctx = GridContext.getContext();
        GridDevice dev = ctx.getCurrentDevice();

        RList currentVP = ctx.getGridState().getViewPort();
        GPar gpar = GPar.create(ctx.getGridState().getGpar());
        ViewPortTransform vpTransform = getViewPortTransform.execute(currentVP, dev);
        ViewPortContext vpContext = ViewPortContext.fromViewPort(currentVP);
        UnitConversionContext conversionCtx = new UnitConversionContext(vpTransform.size, vpContext, dev, gpar);

        int length = GridUtils.maxLength(xVec, yVec, radiusVec);
        Bounds bounds = new Bounds();
        int count = 0;
        Point loc = null;  // we remember the last position and radius
        double radius = -1;
        for (int i = 0; i < length; i++) {
            Size radiusSizes = Size.fromUnits(unitToInches, radiusVec, radiusVec, i, conversionCtx);
            radius = RMath.fmin2(radiusSizes.getWidth(), radiusSizes.getHeight());
            loc = Point.fromUnits(unitToInches, xVec, yVec, i, conversionCtx);
            if (loc.isFinite() && Double.isFinite(radius)) {
                bounds.updateX(loc.x - radius, loc.x + radius);
                bounds.updateY(loc.y - radius, loc.y + radius);
                count++;
            }
        }

        if (count == 0) {
            return RNull.instance;
        }
        Point result;
        assert loc != null;
        if (count == 1) {
            result = EdgeDetection.circleEdge(loc, radius, theta);
        } else {
            result = EdgeDetection.rectEdge(bounds.minX, bounds.minY, bounds.maxX, bounds.maxY, theta);
        }

        double scale = ctx.getGridState().getScale();
        return GridUtils.createDoubleVector(result.x / scale, result.y / scale, bounds.getWidth() / scale, bounds.getHeight() / scale);
    }
}
