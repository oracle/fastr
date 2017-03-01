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

import static com.oracle.truffle.r.library.fastrGrid.GridUtils.getDataAtMod;
import static com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.INCH_TO_POINTS_FACTOR;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.fastrGrid.Unit.UnitConversionContext;
import com.oracle.truffle.r.library.fastrGrid.ViewPortContext.VPContextFromVPNode;
import com.oracle.truffle.r.library.fastrGrid.ViewPortTransform.GetViewPortTransformNode;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * Note: the third parameter contains sequences {@code 1:max(length(x),length(y))}, where the
 * 'length' dispatches to S3 method giving us unit length like {@link Unit.UnitLengthNode}.
 */
public abstract class LText extends RExternalBuiltinNode.Arg7 {
    @Child private Unit.UnitToInchesNode unitToInches = Unit.createToInchesNode();
    @Child private Unit.UnitLengthNode unitLength = Unit.createLengthNode();
    @Child private GetViewPortTransformNode getViewPortTransform = new GetViewPortTransformNode();
    @Child private VPContextFromVPNode vpContextFromVP = new VPContextFromVPNode();

    static {
        Casts casts = new Casts(LText.class);
        // TODO: expressions and maybe other types should have special handling, not only simple
        // String coercion
        casts.arg(0).asStringVector();
        casts.arg(1).mustBe(abstractVectorValue());
        casts.arg(2).mustBe(abstractVectorValue());
        casts.arg(3).mustBe(numericValue()).asDoubleVector();
        casts.arg(4).mustBe(numericValue()).asDoubleVector();
        casts.arg(5).mustBe(numericValue()).asDoubleVector();
    }

    public static LText create() {
        return LTextNodeGen.create();
    }

    @Specialization
    Object drawText(RAbstractStringVector text, RAbstractVector x, RAbstractVector y, RAbstractDoubleVector hjust, RAbstractDoubleVector vjust, RAbstractDoubleVector rotation,
                    Object checkOverlapIgnored) {
        if (text.getLength() == 0) {
            return RNull.instance;
        }

        GridContext ctx = GridContext.getContext();
        GridDevice dev = ctx.getCurrentDevice();

        RList currentVP = ctx.getGridState().getViewPort();
        DrawingContext drawingCtx = GPar.asDrawingContext(ctx.getGridState().getGpar());
        ViewPortTransform vpTransform = getViewPortTransform.execute(currentVP);
        ViewPortContext vpContext = vpContextFromVP.execute(currentVP);
        UnitConversionContext conversionCtx = new UnitConversionContext(vpTransform.size, vpContext, drawingCtx);

        int length = GridUtils.maxLength(unitLength, x, y);
        for (int i = 0; i < length; i++) {
            Point loc = TransformMatrix.transLocation(Point.fromUnits(unitToInches, x, y, i, conversionCtx), vpTransform.transform);
            text(loc.x, loc.y, text.getDataAt(i % text.getLength()), getDataAtMod(hjust, i), getDataAtMod(vjust, i), getDataAtMod(rotation, i), drawingCtx, dev);
        }
        return RNull.instance;
    }

    // transcribed from engine.c

    private void text(double x, double y, String text, double xadjIn, double yadj, double rotation, DrawingContext drawingCtx, GridDevice device) {
        if (!Double.isFinite(yadj)) {
            throw new RuntimeException("Not implemented: 'exact' vertical centering, see engine.c:1700");
        }
        double xadj = Double.isFinite(xadjIn) ? xadjIn : 0.5;

        double radRotation = Math.toRadians(rotation);
        double cosRot = Math.cos(radRotation);
        double sinRot = Math.sin(radRotation);
        String[] lines = text.split("\n");
        for (int lineIdx = 0; lineIdx < lines.length; lineIdx++) {
            double xoff;
            double yoff;
            if (lines.length == 1) {
                // simplification for single line
                xoff = x;
                yoff = y;
            } else {
                yoff = (1 - yadj) * (lines.length - 1) - lineIdx;
                // TODO: in the original the following formula uses "dd->dev->cra[1]"
                yoff *= (drawingCtx.getFontSize() * drawingCtx.getLineHeight()) / INCH_TO_POINTS_FACTOR;
                xoff = -yoff * sinRot;
                yoff = yoff * cosRot;
                xoff = x + xoff;
                yoff = y + yoff;
            }

            double xleft = xoff;
            double ybottom = yoff;
            // now determine bottom-left for THIS line
            if (xadj != 0.0 || yadj != 0.0) {
                // otherwise simply the initial values for xleft and ybottom are OK
                double width = device.getStringWidth(drawingCtx, lines[lineIdx]);
                double height = device.getStringHeight(drawingCtx, lines[lineIdx]);
                xleft = xoff - (xadj) * width * cosRot + yadj * height * sinRot;
                ybottom = yoff - (xadj) * width * sinRot - yadj * height * cosRot;
            }

            device.drawString(drawingCtx, xleft, ybottom, rotation, lines[lineIdx]);
        }
    }
}
