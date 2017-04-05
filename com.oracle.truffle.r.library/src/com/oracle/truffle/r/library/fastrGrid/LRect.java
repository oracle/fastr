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
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.fastrGrid.Unit.UnitConversionContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public abstract class LRect extends RExternalBuiltinNode.Arg6 {

    static {
        Casts casts = new Casts(LRect.class);
        addRectCasts(casts);
    }

    static void addRectCasts(Casts casts) {
        casts.arg(0).mustBe(abstractVectorValue());
        casts.arg(1).mustBe(abstractVectorValue());
        casts.arg(2).mustBe(abstractVectorValue());
        casts.arg(3).mustBe(abstractVectorValue());
        casts.arg(4).mustBe(numericValue()).asDoubleVector();
        casts.arg(5).mustBe(numericValue()).asDoubleVector();
    }

    public static LRect create() {
        return LRectNodeGen.create();
    }

    @Specialization
    @TruffleBoundary
    public Object execute(RAbstractVector xVec, RAbstractVector yVec, RAbstractVector wVec, RAbstractVector hVec, RAbstractDoubleVector hjust, RAbstractDoubleVector vjust) {
        GridContext ctx = GridContext.getContext();
        GridDevice dev = ctx.getCurrentDevice();

        RList currentVP = ctx.getGridState().getViewPort();
        GPar gpar = GPar.create(ctx.getGridState().getGpar());
        ViewPortTransform vpTransform = ViewPortTransform.get(currentVP, dev);
        ViewPortContext vpContext = ViewPortContext.fromViewPort(currentVP);
        UnitConversionContext conversionCtx = new UnitConversionContext(vpTransform.size, vpContext, dev, gpar);

        int length = GridUtils.maxLength(xVec, yVec, wVec, hVec);
        for (int i = 0; i < length; i++) {
            Size size = Size.fromUnits(wVec, hVec, i, conversionCtx);
            Point origLoc = Point.fromUnits(xVec, yVec, i, conversionCtx);
            Point transLoc = TransformMatrix.transLocation(origLoc, vpTransform.transform);
            Point loc = transLoc.justify(size, getDataAtMod(hjust, i), getDataAtMod(vjust, i));
            dev.drawRect(gpar.getDrawingContext(i), loc.x, loc.y, size.getWidth(), size.getHeight(), Math.toRadians(vpTransform.rotationAngle));
        }
        return RNull.instance;
    }
}
