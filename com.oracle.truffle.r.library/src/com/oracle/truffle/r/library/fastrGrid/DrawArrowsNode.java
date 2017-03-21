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

import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asAbstractContainer;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asDouble;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asInt;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.library.fastrGrid.Unit.UnitConversionContext;
import com.oracle.truffle.r.library.fastrGrid.Unit.UnitToInchesNode;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

public class DrawArrowsNode extends Node {
    // Structure of an arrow description
    public static final int ARROWANGLE = 0;
    public static final int ARROWLENGTH = 1;
    public static final int ARROWENDS = 2;
    public static final int ARROWTYPE = 3;
    // known values of ARROWTYPE
    public static final int ARROWTYPE_LINES = 1;
    public static final int ARROWTYPE_POLYGON = 2;

    @Child private UnitToInchesNode unitToInches = Unit.createToInchesNode();

    /**
     * Draws arrows at the start and end of given lines.
     *
     * @param x x-positions of the line(s)
     * @param y y-positions of the line(s)
     * @param startIndex consider arrays x,y to start from this index
     * @param length consider arrays x,y to have this length
     * @param parentIndex the index of the line we are drawing, this is used for choosing the right
     *            index into vectors extracted from arrow
     * @param arrow list with various attributes of the arrow
     * @param start should we draw start arrow if the arrow list says so. Otherwise never draw it.
     * @param end should we draw end arrow if the arrow list says so. Otherwise never draw it.
     * @param conversionCtx needed for unit conversions.
     */
    public void drawArrows(double[] x, double[] y, int startIndex, int length, int parentIndex, RList arrow, boolean start, boolean end, UnitConversionContext conversionCtx) {
        assert x.length == y.length;
        int endsVal = asInt(arrow.getDataAt(ARROWENDS), parentIndex);
        boolean first = endsVal != 2;
        boolean last = endsVal != 1;
        if ((!first || !start) && (!last || !end)) {
            // if we are not going to draw any arrow anyway, just finish
            return;
        }
        // extract angle, length in inches and arrow type from 'arrow'
        double angle = asDouble(arrow.getDataAt(ARROWANGLE), parentIndex);
        int arrowType = asInt(arrow.getDataAt(ARROWTYPE), parentIndex);
        RAbstractContainer lengthVec = asAbstractContainer(arrow.getDataAt(ARROWLENGTH));
        double arrowLength = unitToInches.convertHeight(lengthVec, parentIndex, conversionCtx);
        arrowLength = Math.max(arrowLength, unitToInches.convertWidth(lengthVec, parentIndex, conversionCtx));
        // draw the arrows
        GridDevice device = conversionCtx.device;
        DrawingContext drawingCtx = conversionCtx.drawingContext;
        if (first && start) {
            drawArrow(drawingCtx, device, arrowType, x[startIndex], y[startIndex], x[startIndex + 1], y[startIndex + 1], angle, arrowLength);
        }
        if (last && end) {
            int n = startIndex + length;
            drawArrow(drawingCtx, device, arrowType, x[n - 1], y[n - 1], x[n - 2], y[n - 2], angle, arrowLength);
        }
    }

    private void drawArrow(DrawingContext drawingCtx, GridDevice device, int arrowType, double x0, double y0, double x1, double y1, double angle, double length) {
        double a = Math.toRadians(angle);
        double xc = x1 - x0;
        double yc = y1 - y0;
        double rot = Math.atan2(yc, xc);
        double[] vertx = new double[3];
        double[] verty = new double[3];
        vertx[0] = x0 + length * Math.cos(rot + a);
        verty[0] = y0 + length * Math.sin(rot + a);
        vertx[1] = x0;
        verty[1] = y0;
        vertx[2] = x0 + length * Math.cos(rot - a);
        verty[2] = y0 + length * Math.sin(rot - a);
        if (arrowType == ARROWTYPE_LINES) {
            device.drawPolyLines(drawingCtx, vertx, verty, 0, 3);
        } else if (arrowType == ARROWTYPE_POLYGON) {
            device.drawPolyLines(drawingCtx, vertx, verty, 0, 3);
            // TODO: real polygon
        }
    }
}
