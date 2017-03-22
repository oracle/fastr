/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 2001-3 Paul Murrell
 * Copyright (c) 1998-2015, The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.fastrGrid;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.fastrGrid.Unit.UnitConversionContext;
import com.oracle.truffle.r.library.fastrGrid.Unit.UnitLengthNode;
import com.oracle.truffle.r.library.fastrGrid.Unit.UnitToInchesNode;
import com.oracle.truffle.r.library.fastrGrid.ViewPortTransform.GetViewPortTransformNode;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridColor;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public abstract class LPoints extends RExternalBuiltinNode.Arg4 {
    private static final double SMALL = 0.25;
    private static final double RADIUS = 0.375;
    private static final double SQRC = 0.88622692545275801364; /* sqrt(pi / 4) */
    private static final double DMDC = 1.25331413731550025119; /* sqrt(pi / 4) * sqrt(2) */
    private static final double TRC0 = 1.55512030155621416073; /* sqrt(4 * pi/(3 * sqrt(3))) */
    private static final double TRC1 = 1.34677368708859836060; /* TRC0 * sqrt(3) / 2 */
    private static final double TRC2 = 0.77756015077810708036; /* TRC0 / 2 */

    @Child private GetViewPortTransformNode getViewPortTransform = new GetViewPortTransformNode();

    @Child private UnitLengthNode unitLength = Unit.createLengthNode();
    @Child private UnitToInchesNode unitToInches = Unit.createToInchesNode();

    static {
        Casts casts = new Casts(LPoints.class);
        casts.arg(0).mustBe(abstractVectorValue());
        casts.arg(1).mustBe(abstractVectorValue());
        casts.arg(2).mustBe(numericValue(), Message.GENERIC, "grid.points: pch argument not implemented for characters yet").asIntegerVector();
        casts.arg(3).mustBe(abstractVectorValue());
    }

    public static LPoints create() {
        return LPointsNodeGen.create();
    }

    @Specialization
    @TruffleBoundary
    public Object doPoints(RAbstractVector xVec, RAbstractVector yVec, RAbstractIntVector pchVec, RAbstractVector sizeVec) {
        GridContext ctx = GridContext.getContext();
        GridDevice dev = ctx.getCurrentDevice();

        RList currentVP = ctx.getGridState().getViewPort();
        RList gpar = ctx.getGridState().getGpar();
        DrawingContext drawingCtx = GPar.asDrawingContext(gpar);
        double cex = GPar.getCex(gpar);
        ViewPortTransform vpTransform = getViewPortTransform.execute(currentVP, dev);
        ViewPortContext vpContext = ViewPortContext.fromViewPort(currentVP);
        UnitConversionContext conversionCtx = new UnitConversionContext(vpTransform.size, vpContext, dev, drawingCtx);

        // Note: unlike in other drawing primitives, we only consider length of x
        int length = unitLength.execute(xVec);
        for (int i = 0; i < length; i++) {
            Point loc = TransformMatrix.transLocation(Point.fromUnits(unitToInches, xVec, yVec, i, conversionCtx), vpTransform.transform);
            double size = unitToInches.convertWidth(sizeVec, i, conversionCtx);
            if (loc.isFinite() && Double.isFinite(size)) {
                drawSymbol(drawingCtx, dev, cex, pchVec.getDataAt(i % pchVec.getLength()), size, loc.x, loc.y);
            }
        }
        return RNull.instance;
    }

    // transcribed from engine.c function GESymbol

    private void drawSymbol(DrawingContext drawingCtx, GridDevice dev, double cex, int pch, double size, double x, double y) {
        // pch 0 - 25 are interpreted as geometrical shapes, pch from ascii code of ' ' are
        // interpreted as corresponding ascii character, which should be drawn
        switch (pch) {
            case 46:
                drawDot(drawingCtx, dev, cex, x, y);
                break;
            case 1:
                drawOctahedron(drawingCtx, dev, GridColor.TRANSPARENT, size, x, y);
                break;
            case 16:
                drawOctahedron(drawingCtx, dev, drawingCtx.getColor(), size, x, y);
                break;
            default:
                throw RInternalError.unimplemented("grid.points unimplemented symbol " + pch);
        }
    }

    private static void drawOctahedron(DrawingContext drawingCtx, GridDevice dev, GridColor fill, double size, double x, double y) {
        GridColor originalFill = drawingCtx.getFillColor();
        drawingCtx.setFillColor(fill);
        dev.drawCircle(drawingCtx, x, y, RADIUS * size);
        drawingCtx.setFillColor(originalFill);
    }

    private static void drawDot(DrawingContext drawingCtx, GridDevice dev, double cex, double x, double y) {
        // NOTE: we are *filling* a rect with the current colour (we are not drawing the border AND
        // we are not using the current fill colour)
        GridColor originalFill = drawingCtx.getFillColor();
        drawingCtx.setFillColor(drawingCtx.getColor());
        drawingCtx.setColor(GridColor.TRANSPARENT);

        /*
         * The idea here is to use a 0.01" square, but to be of at least one device unit in each
         * direction, assuming that corresponds to pixels. That may be odd if pixels are not square,
         * but only on low resolution devices where we can do nothing better.
         *
         * For this symbol only, size is cex (see engine.c).
         *
         * Prior to 2.1.0 the offsets were always 0.5.
         */
        double xc = cex * 0.005;
        double yc = cex * 0.005;
        if (cex > 0 && xc < 0.5) {
            xc = 0.5;
        }
        if (cex > 0 && yc < 0.5) {
            yc = 0.5;
        }
        dev.drawRect(drawingCtx, x - xc, y - yc, x + xc, y + yc);

        drawingCtx.setColor(drawingCtx.getFillColor());
        drawingCtx.setFillColor(originalFill);
    }
}
