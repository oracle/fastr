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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.fastrGrid.Unit.UnitConversionContext;
import com.oracle.truffle.r.library.fastrGrid.ViewPortTransform.GetViewPortTransformNode;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * Gets (vectors of) 4 coordinates (two points) and draws a line between them, unlike {@link LLines}
 * which gets a vector of points and connects them all.
 */
public abstract class LSegments extends RExternalBuiltinNode.Arg5 {
    @Child private Unit.UnitToInchesNode unitToInches = Unit.createToInchesNode();
    @Child private Unit.UnitLengthNode unitLength = Unit.createLengthNode();
    @Child private GetViewPortTransformNode getViewPortTransform = new GetViewPortTransformNode();
    @Child private DrawArrowsNode drawArrowsNode = new DrawArrowsNode();

    static {
        Casts casts = new Casts(LSegments.class);
        casts.arg(0).mustBe(abstractVectorValue());
        casts.arg(1).mustBe(abstractVectorValue());
        casts.arg(2).mustBe(abstractVectorValue());
        casts.arg(3).mustBe(abstractVectorValue());
        casts.arg(4).allowNull().mustBe(RList.class);
    }

    public static LSegments create() {
        return LSegmentsNodeGen.create();
    }

    @Specialization
    Object doSegments(RAbstractVector x0, RAbstractVector y0, RAbstractVector x1, RAbstractVector y1, RNull arrow) {
        return doSegments(x0, y0, x1, y1, (RList) null);
    }

    @Specialization
    @TruffleBoundary
    Object doSegments(RAbstractVector x0, RAbstractVector y0, RAbstractVector x1, RAbstractVector y1, RList arrow) {
        GridContext ctx = GridContext.getContext();
        GridDevice dev = ctx.getCurrentDevice();

        RList currentVP = ctx.getGridState().getViewPort();
        DrawingContext drawingCtx = GPar.asDrawingContext(ctx.getGridState().getGpar());
        ViewPortTransform vpTransform = getViewPortTransform.execute(currentVP, dev);
        ViewPortContext vpContext = ViewPortContext.fromViewPort(currentVP);
        UnitConversionContext conversionCtx = new UnitConversionContext(vpTransform.size, vpContext, dev, drawingCtx);

        int length = GridUtils.maxLength(unitLength, x0, y0, x1, y1);
        double[] xx = new double[2];
        double[] yy = new double[2];
        for (int i = 0; i < length; i++) {
            Point loc1 = TransformMatrix.transLocation(Point.fromUnits(unitToInches, x0, y0, i, conversionCtx), vpTransform.transform);
            Point loc2 = TransformMatrix.transLocation(Point.fromUnits(unitToInches, x1, y1, i, conversionCtx), vpTransform.transform);
            if (!loc1.isFinite() || !loc2.isFinite()) {
                continue;
            }
            xx[0] = loc1.x;
            xx[1] = loc2.x;
            yy[0] = loc1.y;
            yy[1] = loc2.y;
            dev.drawPolyLines(drawingCtx, xx, yy, 0, 2);
            if (arrow != null) {
                drawArrowsNode.drawArrows(xx, yy, 0, 2, i, arrow, true, true, conversionCtx);
            }
        }
        return RNull.instance;
    }
}
