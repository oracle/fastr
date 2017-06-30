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
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridColor;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public abstract class LPoints extends RExternalBuiltinNode.Arg4 {
    private static final double TRC0 = 1.55512030155621416073; /* sqrt(4 * pi/(3 * sqrt(3))) */
    private static final double TRC1 = 1.34677368708859836060; /* TRC0 * sqrt(3) / 2 */
    private static final double TRC2 = 0.77756015077810708036; /* TRC0 / 2 */

    // empiracally chosen to match GNU R look better
    private static final double TRIANGLE_SIZE_FACTOR = 1.15;

    // empirically chosen factor to visually approx match GNU R
    private static final double SIZE_FACTOR = 0.375;

    // we assume at leat 72 points per inch
    private static final double PIXEL_SIZE = 1. / 72.;

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
        ViewPortTransform vpTransform = ViewPortTransform.get(currentVP, dev);
        ViewPortContext vpContext = ViewPortContext.fromViewPort(currentVP);
        UnitConversionContext conversionCtx = new UnitConversionContext(vpTransform.size, vpContext, dev, gpar);

        // Note: unlike in other drawing primitives, we only consider length of x
        int length = Unit.getLength(xVec);
        ContextCache contextCache = new ContextCache(null);
        for (int i = 0; i < length; i++) {
            Point loc = TransformMatrix.transLocation(Point.fromUnits(xVec, yVec, i, conversionCtx), vpTransform.transform);
            double size = Unit.convertWidth(sizeVec, i, conversionCtx);
            if (loc.isFinite() && Double.isFinite(size)) {
                contextCache = contextCache.from(gpar.getDrawingContext(i));
                drawSymbol(contextCache, dev, cex, pchVec.getDataAt(i % pchVec.getLength()), size * SIZE_FACTOR, loc.x, loc.y);
            }
        }
        return RNull.instance;
    }

    private static void drawSymbol(ContextCache ctxCache, GridDevice dev, double cex, int pch, double halfSize, double x, double y) {
        // pch 0 - 25 are interpreted as geometrical shapes, pch from ascii code of ' ' are
        // interpreted as corresponding ascii character, which should be drawn
        // the coordinates should be interpreted as the center of the symbol
        double fullSize = halfSize * 2;
        DrawingContext emptyFill = ctxCache.getTransparentFill();
        switch (pch) {
            case 0:
                drawSquare(emptyFill, dev, halfSize, x, y);
                break;
            case 1:
                dev.drawCircle(emptyFill, x, y, halfSize);
                break;
            case 2: // triangle up
                triangleUp(emptyFill, dev, halfSize * TRIANGLE_SIZE_FACTOR, x, y);
                break;
            case 3: /* S plus */
                drawPlus(emptyFill, dev, halfSize, x, y);
                break;
            case 4: // S times
                drawTimes(emptyFill, dev, halfSize, x, y);
                break;
            case 5: // S diamond
                drawDiamond(emptyFill, dev, halfSize, fullSize, x, y);
                break;
            case 6: // S triangle point down
                triangleDown(emptyFill, dev, halfSize * TRIANGLE_SIZE_FACTOR, x, y);
                break;
            case 7: // S square and times superimposed
                drawSquare(emptyFill, dev, halfSize, x, y);
                drawTimes(emptyFill, dev, halfSize, x, y);
                break;
            case 8: // S times and plus superimposed
                drawPlus(emptyFill, dev, halfSize, x, y);
                drawTimes(emptyFill, dev, halfSize, x, y);
                break;
            case 9: // S diamond and plus superimposed
                drawPlus(emptyFill, dev, halfSize, x, y);
                drawDiamond(emptyFill, dev, halfSize, fullSize, x, y);
                break;
            case 10: // S circle and plus
                dev.drawCircle(emptyFill, x, y, halfSize);
                drawPlus(emptyFill, dev, halfSize, x, y);
                break;
            case 11: // S superimposed triangles
                triangleUp(emptyFill, dev, halfSize * TRIANGLE_SIZE_FACTOR, x, y);
                triangleDown(emptyFill, dev, halfSize * TRIANGLE_SIZE_FACTOR, x, y);
                break;
            case 12: // S square and plus superimposed
                drawSquare(emptyFill, dev, halfSize, x, y);
                drawPlus(emptyFill, dev, halfSize, x, y);
                break;
            case 13: // S circle and times
                dev.drawCircle(emptyFill, x, y, halfSize);
                drawTimes(ctxCache.original, dev, halfSize, x, y);
                break;
            case 14: // S rectangle with triangle up
                dev.drawRect(emptyFill, x - halfSize, y - halfSize, fullSize, fullSize, 0);
                drawConnected(ctxCache.getTransparentFill(), dev, x - halfSize, y - halfSize, x + halfSize, y - halfSize, x, y + halfSize);
                break;
            case 15: // S filled square
            case 22: // S filled (with different color) square
                dev.drawRect(ctxCache.getFilled(), x - halfSize, y - halfSize, fullSize, fullSize, 0);
                break;
            case 16: // S filled circle (should be 'octagon')
            case 19: // S filled circle
            case 21: // S filled (with different color) circle
                dev.drawCircle(ctxCache.getFilled(), x, y, halfSize);
                break;
            case 17: // S filled triangle up
            case 24: // S filled (with different color) triangle up
                triangleUp(ctxCache.getFilled(), dev, halfSize * TRIANGLE_SIZE_FACTOR, x, y);
                break;
            case 18: // S filled diamond
            case 23: // S filled (with different color) diamond
                drawDiamond(ctxCache.getFilled(), dev, halfSize, fullSize, x, y);
                break;
            case 20: // S smaller filled circle
                dev.drawCircle(ctxCache.getFilled(), x, y, halfSize * .6);
                break;
            case 25: // S triangle down filled
                triangleDown(ctxCache.getFilled(), dev, halfSize * TRIANGLE_SIZE_FACTOR, x, y);
                break;
            case 46: // small dot
                // we assume at leat 72 points per inch
                dev.drawRect(ctxCache.getFilled(), x - PIXEL_SIZE / 2, y - PIXEL_SIZE / 2, PIXEL_SIZE, PIXEL_SIZE, 0);
                break;
            default:
                drawTextSymbol(ctxCache, dev, x, y, new String(new char[]{(char) pch}));
        }
    }

    private static void drawDiamond(DrawingContext ctx, GridDevice dev, double halfSize, double fullSize, double x, double y) {
        dev.drawRect(ctx, x - halfSize, y - halfSize, fullSize, fullSize, 1.75 * Math.PI);
    }

    private static void drawSquare(DrawingContext ctx, GridDevice dev, double halfSize, double x, double y) {
        double fullSize = halfSize * 2.;
        dev.drawRect(ctx, x - halfSize, y - halfSize, fullSize, fullSize, 0);
    }

    private static void drawTimes(DrawingContext ctx, GridDevice dev, double halfSize, double x, double y) {
        drawLine(ctx, dev, x - halfSize, y + halfSize, x + halfSize, y - halfSize);
        drawLine(ctx, dev, x + halfSize, y + halfSize, x - halfSize, y - halfSize);
    }

    private static void drawPlus(DrawingContext ctx, GridDevice dev, double halfSize, double x, double y) {
        drawLine(ctx, dev, x - halfSize, y, x + halfSize, y);
        drawLine(ctx, dev, x, y + halfSize, x, y - halfSize);
    }

    private static void triangleDown(DrawingContext ctx, GridDevice dev, double halfSize, double x, double y) {
        double yc = halfSize * TRC2;
        double xc = halfSize * TRC1;
        drawConnected(ctx, dev, x, y - halfSize * TRC0, x - xc, y + yc, x + xc, y + yc);
    }

    private static void triangleUp(DrawingContext ctx, GridDevice dev, double halfSize, double x, double y) {
        double yc = halfSize * TRC2;
        double xc = halfSize * TRC1;
        drawConnected(ctx, dev, x, y + halfSize * TRC0, x - xc, y - yc, x + xc, y - yc);
    }

    private static void drawTextSymbol(ContextCache ctxCache, GridDevice dev, double x, double y, String symbols) {
        double height = dev.getStringHeight(ctxCache.getSymbol(), symbols);
        double width = dev.getStringWidth(ctxCache.getSymbol(), symbols);
        dev.drawString(ctxCache.getSymbol(), x - width / 2, y - height / 2, 0, symbols);
    }

    /**
     * Simpler to use by hand version of drawPolyline. Points are expected to be in format [x1, y1,
     * x2, y2, ...].
     */
    private static void drawConnected(DrawingContext ctx, GridDevice dev, double... points) {
        assert points.length % 2 == 0 && points.length > 0;
        double[] x = new double[(points.length / 2) + 1];
        double[] y = new double[(points.length / 2) + 1];
        x[x.length - 1] = points[0];
        y[y.length - 1] = points[1];
        for (int i = 0; i < x.length - 1; i++) {
            x[i] = points[i * 2];
            y[i] = points[(i * 2) + 1];
        }
        dev.drawPolygon(ctx, x, y, 0, y.length);
    }

    private static void drawLine(DrawingContext ctx, GridDevice dev, double x1, double y1, double x2, double y2) {
        dev.drawPolyLines(ctx, new double[]{x1, x2}, new double[]{y1, y2}, 0, 2);
    }

    private static final class ContextCache {
        public final DrawingContext original;
        private DrawingContext filled;
        private DrawingContext transprentFill;
        private DrawingContext symbol;

        private ContextCache(DrawingContext original) {
            this.original = original;
        }

        ContextCache from(DrawingContext newOriginal) {
            if (original == newOriginal) {
                return this;
            }
            return new ContextCache(newOriginal);
        }

        /**
         * Context with fill color set to the normal color of the original context.
         */
        DrawingContext getFilled() {
            if (filled == null) {
                filled = new PointDrawingContext(original, original.getColor(), original.getColor(), 1);
            }
            return filled;
        }

        DrawingContext getTransparentFill() {
            if (transprentFill == null) {
                transprentFill = new PointDrawingContext(original, original.getColor(), GridColor.TRANSPARENT, 1);
            }
            return transprentFill;
        }

        DrawingContext getSymbol() {
            if (symbol == null) {
                symbol = new PointDrawingContext(original, original.getColor(), original.getFillColor(), 1.4);
            }
            return symbol;
        }
    }

    /**
     * Context that has the same parameters as the given context except for the color and fill color
     * and multiplication factor for font size, which are given explicitly.
     */
    private static final class PointDrawingContext implements DrawingContext {
        private final DrawingContext inner;
        private final GridColor color;
        private final GridColor fillColor;
        private final double fontsizeFactor;

        private PointDrawingContext(DrawingContext inner, GridColor color, GridColor fillColor, double fontsizeFactor) {
            this.inner = inner;
            this.color = color;
            this.fillColor = fillColor;
            this.fontsizeFactor = fontsizeFactor;
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
            return inner.getFontSize() * fontsizeFactor;
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
