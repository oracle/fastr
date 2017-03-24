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
        RList gparList = ctx.getGridState().getGpar();
        GPar gpar = GPar.create(gparList);
        double cex = GPar.getCex(gparList);
        ViewPortTransform vpTransform = getViewPortTransform.execute(currentVP, dev);
        ViewPortContext vpContext = ViewPortContext.fromViewPort(currentVP);
        UnitConversionContext conversionCtx = new UnitConversionContext(vpTransform.size, vpContext, dev, gpar);

        // Note: unlike in other drawing primitives, we only consider length of x
        int length = unitLength.execute(xVec);
        DrawingContext initialDrawingCtx = gpar.getDrawingContext(0);
        PointDrawingContext pointDrawingCtx = new PointDrawingContext(initialDrawingCtx, initialDrawingCtx.getFillColor(), initialDrawingCtx.getFillColor());
        for (int i = 0; i < length; i++) {
            Point loc = TransformMatrix.transLocation(Point.fromUnits(unitToInches, xVec, yVec, i, conversionCtx), vpTransform.transform);
            double size = unitToInches.convertWidth(sizeVec, i, conversionCtx);
            if (loc.isFinite() && Double.isFinite(size)) {
                pointDrawingCtx = pointDrawingCtx.update(gpar.getDrawingContext(i));
                pointDrawingCtx = drawSymbol(pointDrawingCtx, dev, cex, pchVec.getDataAt(i % pchVec.getLength()), size, loc.x, loc.y);
            }
        }
        return RNull.instance;
    }

    // transcribed from engine.c function GESymbol

    private PointDrawingContext drawSymbol(PointDrawingContext drawingCtx, GridDevice dev, double cex, int pch, double size, double x, double y) {
        // pch 0 - 25 are interpreted as geometrical shapes, pch from ascii code of ' ' are
        // interpreted as corresponding ascii character, which should be drawn
        switch (pch) {
            case 46:
                return drawDot(drawingCtx, dev, cex, x, y);
            case 1:
                return drawOctahedron(drawingCtx, dev, GridColor.TRANSPARENT, size, x, y);
            case 16:
                return drawOctahedron(drawingCtx, dev, drawingCtx.getWrapped().getColor(), size, x, y);
            default:
                throw RInternalError.unimplemented("grid.points unimplemented symbol " + pch);
        }
    }

    private static PointDrawingContext drawOctahedron(PointDrawingContext drawingCtxIn, GridDevice dev, GridColor fill, double size, double x, double y) {
        PointDrawingContext drawingCtx = drawingCtxIn.update(drawingCtxIn.getWrapped().getColor(), fill);
        dev.drawCircle(drawingCtx, x, y, RADIUS * size);
        return drawingCtx;
    }

    private static PointDrawingContext drawDot(PointDrawingContext drawingCtxIn, GridDevice dev, double cex, double x, double y) {
        // NOTE: we are *filling* a rect with the current colour (we are not drawing the border AND
        // we are not using the current fill colour)
        PointDrawingContext drawingCtx = drawingCtxIn.update(GridColor.TRANSPARENT, drawingCtxIn.getWrapped().getColor());
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
        return drawingCtx;
    }

    private static final class PointDrawingContext implements DrawingContext {
        private final DrawingContext inner;
        private final GridColor color;
        private final GridColor fillColor;

        private PointDrawingContext(DrawingContext inner, GridColor color, GridColor fillColor) {
            this.inner = inner;
            this.color = color;
            this.fillColor = fillColor;
        }

        // This allows to re-use the existing instance if it would have the same parameters. The
        // assumption is that the users will actually draw many points in a row with the same
        // parameters.
        private PointDrawingContext update(GridColor color, GridColor fillColor) {
            if (this.color.equals(color) && this.fillColor.equals(fillColor)) {
                return this;
            }
            return new PointDrawingContext(inner, color, fillColor);
        }

        private PointDrawingContext update(DrawingContext inner) {
            if (this.inner == inner) {
                return this;
            }
            return new PointDrawingContext(inner, this.color, this.fillColor);
        }

        @Override
        public byte[] getLineType() {
            return inner.getLineType();
        }

        @Override
        public double getLineWidth() {
            return inner.getLineWidth();
        }

        @Override
        public GridLineJoin getLineJoin() {
            return inner.getLineJoin();
        }

        @Override
        public GridLineEnd getLineEnd() {
            return inner.getLineEnd();
        }

        @Override
        public double getLineMitre() {
            return inner.getLineMitre();
        }

        @Override
        public GridColor getColor() {
            return color;
        }

        @Override
        public double getFontSize() {
            return inner.getFontSize();
        }

        @Override
        public GridFontStyle getFontStyle() {
            return inner.getFontStyle();
        }

        @Override
        public String getFontFamily() {
            return inner.getFontFamily();
        }

        @Override
        public double getLineHeight() {
            return inner.getLineHeight();
        }

        @Override
        public GridColor getFillColor() {
            return fillColor;
        }

        private DrawingContext getWrapped() {
            return inner;
        }
    }
}
