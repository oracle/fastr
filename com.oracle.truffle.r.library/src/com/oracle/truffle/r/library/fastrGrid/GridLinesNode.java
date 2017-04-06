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

import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asIntVector;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.library.fastrGrid.Unit.UnitConversionContext;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * Common code shared between {@code L_lines} and {@code L_polygon} externals. Both draw a series of
 * lines, but only the later connects the last point with the first point and only the former draws
 * arrows (which is not implemented yet). Note: the third parameter contains sequences
 * {@code 1:max(length(x),length(y))}, where the 'length' dispatches to S3 method giving us unit
 * length like {@link com.oracle.truffle.r.library.fastrGrid.Unit#getLength(RAbstractContainer)}.
 * This means that we do not have to use the
 * {@link com.oracle.truffle.r.library.fastrGrid.Unit#getLength(RAbstractContainer)} to get the
 * length.
 */
public abstract class GridLinesNode extends Node {
    public static GridLinesNode createLines() {
        return new GridLinesImpl();
    }

    public static GridLinesNode createPolygon() {
        return new GridLinesPolygon();
    }

    @TruffleBoundary
    void execute(RAbstractVector x, RAbstractVector y, RList lengths, RList arrow) {
        GridContext ctx = GridContext.getContext();
        GridDevice dev = ctx.getCurrentDevice();

        RList currentVP = ctx.getGridState().getViewPort();
        GPar gpar = GPar.create(ctx.getGridState().getGpar());
        ViewPortTransform vpTransform = ViewPortTransform.get(currentVP, dev);
        ViewPortContext vpContext = ViewPortContext.fromViewPort(currentVP);
        UnitConversionContext conversionCtx = new UnitConversionContext(vpTransform.size, vpContext, dev, gpar);

        // Convert the list of vectors of indexes to type-safe array and calculate the max length of
        // the vectors.
        RAbstractIntVector[] unitIndexesList = new RAbstractIntVector[lengths.getLength()];
        int maxIndexesLen = 0;
        for (int i = 0; i < lengths.getLength(); i++) {
            unitIndexesList[i] = asIntVector(lengths.getDataAt(i));
            maxIndexesLen = Math.max(maxIndexesLen, unitIndexesList[i].getLength());
        }

        double[] xx = new double[maxIndexesLen + 1];    // plus one for polygons
        double[] yy = new double[maxIndexesLen + 1];
        for (int unitIndexesListIdx = 0; unitIndexesListIdx < unitIndexesList.length; unitIndexesListIdx++) {
            RAbstractIntVector unitIndexes = unitIndexesList[unitIndexesListIdx];
            DrawingContext drawingCtx = gpar.getDrawingContext(unitIndexesListIdx);
            boolean oldIsFinite = false;
            int start = 0;
            int unitIndexesLen = unitIndexes.getLength();
            // following loop finds series of valid points (finite x and y values) and draws each
            // such series as a polyline
            for (int i = 0; i < unitIndexesLen; i++) {
                int unitIndex = unitIndexes.getDataAt(i) - 1;   // converting R's 1-based index
                Point origLoc = Point.fromUnits(x, y, unitIndex, conversionCtx);
                Point loc = TransformMatrix.transLocation(origLoc, vpTransform.transform);
                xx[i] = loc.x;
                yy[i] = loc.y;
                boolean currIsFinite = loc.isFinite();
                boolean lastIter = i == (unitIndexesLen - 1);
                if (currIsFinite && !oldIsFinite) {
                    start = i; // start a new series
                } else if (oldIsFinite && (!currIsFinite || lastIter)) {
                    // draw the previous points series because
                    // (1) current is invalid point. Note: in (one of) the next iteration(s), the
                    // oldIsFinite will be false and we will update the start and start a new series
                    // (2) we are in the last iteration
                    int length = i - start;
                    if (currIsFinite) {
                        // the length includes the last point only if the point is finite
                        length++;
                    }
                    if (length > 1) {
                        drawPolylines(dev, drawingCtx, yy, xx, start, length);
                        if (arrow != null) {
                            // Can draw an arrow at the start if the points include the first point.
                            // Draw an arrow at the end only if this is the last series
                            Arrows.drawArrows(xx, yy, start, length, unitIndex, arrow, start == 0, lastIter, conversionCtx);
                        }
                    }
                }
                oldIsFinite = currIsFinite;
            }
        }
    }

    abstract void drawPolylines(GridDevice dev, DrawingContext drawingCtx, double[] yy, double[] xx, int start, int length);

    private static final class GridLinesImpl extends GridLinesNode {
        @Override
        void drawPolylines(GridDevice dev, DrawingContext drawingCtx, double[] yy, double[] xx, int start, int length) {
            dev.drawPolyLines(drawingCtx, xx, yy, start, length);
        }
    }

    private static final class GridLinesPolygon extends GridLinesNode {
        @Override
        void drawPolylines(GridDevice dev, DrawingContext drawingCtx, double[] yy, double[] xx, int start, int length) {
            xx[start + length] = xx[start];
            yy[start + length] = yy[start];
            dev.drawPolygon(drawingCtx, xx, yy, start, length + 1);
        }
    }
}
