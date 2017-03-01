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
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.fastrGrid.Unit.UnitConversionContext;
import com.oracle.truffle.r.library.fastrGrid.ViewPortContext.VPContextFromVPNode;
import com.oracle.truffle.r.library.fastrGrid.ViewPortTransform.GetViewPortTransformNode;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

/**
 * Note: the third parameter contains sequences {@code 1:max(length(x),length(y))}, where the
 * 'length' dispatches to S3 method giving us unit length like
 * {@link com.oracle.truffle.r.library.fastrGrid.Unit.UnitLengthNode}.
 */
public abstract class LLines extends RExternalBuiltinNode.Arg4 {
    @Child private CastNode toIntVector = newCastBuilder().mustBe(integerValue()).boxPrimitive().asIntegerVector().buildCastNode();
    @Child private Unit.UnitToInchesNode unitToInches = Unit.createToInchesNode();
    @Child private GetViewPortTransformNode getViewPortTransform = new GetViewPortTransformNode();
    @Child private VPContextFromVPNode vpContextFromVP = new VPContextFromVPNode();

    static {
        Casts casts = new Casts(LLines.class);
        casts.arg(0).mustBe(abstractVectorValue());
        casts.arg(1).mustBe(abstractVectorValue());
        casts.arg(2).mustBe(RList.class);
    }

    public static LLines create() {
        return LLinesNodeGen.create();
    }

    @Specialization
    Object doLines(RAbstractVector x, RAbstractVector y, RList lengths, Object arrowIgnored) {
        GridContext ctx = GridContext.getContext();
        GridDevice dev = ctx.getCurrentDevice();

        RList currentVP = ctx.getGridState().getViewPort();
        DrawingContext drawingCtx = GPar.asDrawingContext(ctx.getGridState().getGpar());
        ViewPortTransform vpTransform = getViewPortTransform.execute(currentVP);
        ViewPortContext vpContext = vpContextFromVP.execute(currentVP);
        UnitConversionContext conversionCtx = new UnitConversionContext(vpTransform.size, vpContext, drawingCtx);

        // Convert the list of vectors of indexes to type-safe array and calculate the max length of
        // the vectors.
        RAbstractIntVector[] unitIndexesList = new RAbstractIntVector[lengths.getLength()];
        int maxIndexesLen = 0;
        for (int i = 0; i < lengths.getLength(); i++) {
            unitIndexesList[i] = (RAbstractIntVector) toIntVector.execute(lengths.getDataAt(i));
            maxIndexesLen = Math.max(maxIndexesLen, unitIndexesList[i].getLength());
        }

        double[] xx = new double[maxIndexesLen];
        double[] yy = new double[maxIndexesLen];
        for (RAbstractIntVector unitIndexes : unitIndexesList) {
            boolean oldIsFinite = false;
            int start = 0;
            int unitIndexesLen = unitIndexes.getLength();
            // following loop finds series of valid points (finite x and y values) and draws each
            // such series as a polyline
            for (int i = 0; i < unitIndexesLen; i++) {
                int unitIndex = unitIndexes.getDataAt(i) - 1;   // coverting R's 1-based index
                Point origLoc = Point.fromUnits(unitToInches, x, y, unitIndex, conversionCtx);
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
                    if (lastIter || i - start > 1) {
                        // we draw only if the previous series of points was at least of length 3 or
                        // it's last iteration. This seems slightly weird, but that's how GnuR seems
                        // to work
                        dev.drawPolyLines(drawingCtx, xx, yy, start, (i - start) + 1);
                    }
                }
                oldIsFinite = currIsFinite;
            }
        }

        return RNull.instance;
    }
}
