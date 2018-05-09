/*
 * Copyright (C) 2001-3 Paul Murrell
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.library.fastrGrid;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.fastrGrid.Unit.UnitConversionContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nmath.RMath;

public abstract class LCircle extends RExternalBuiltinNode.Arg3 {

    static {
        Casts casts = new Casts(LCircle.class);
        addCircleCasts(casts);
    }

    static void addCircleCasts(Casts casts) {
        casts.arg(0).mustBe(abstractVectorValue());
        casts.arg(1).mustBe(abstractVectorValue());
        casts.arg(2).mustBe(abstractVectorValue());
    }

    public static LCircle create() {
        return LCircleNodeGen.create();
    }

    @Specialization
    @TruffleBoundary
    Object doCircle(RAbstractVector xVec, RAbstractVector yVec, RAbstractVector radiusVec) {
        GridContext ctx = GridContext.getContext();
        GridDevice dev = ctx.getCurrentDevice();

        RList currentVP = ctx.getGridState().getViewPort();
        GPar gpar = GPar.create(ctx.getGridState().getGpar());
        ViewPortTransform vpTransform = ViewPortTransform.get(currentVP, dev);
        ViewPortContext vpContext = ViewPortContext.fromViewPort(currentVP);
        UnitConversionContext conversionCtx = new UnitConversionContext(vpTransform.size, vpContext, dev, gpar);

        int length = GridUtils.maxLength(xVec, yVec, radiusVec);
        for (int i = 0; i < length; i++) {
            Size radiusSizes = Size.fromUnits(radiusVec, radiusVec, i, conversionCtx);
            double radius = RMath.fmin2(radiusSizes.getWidth(), radiusSizes.getHeight());
            Point origLoc = Point.fromUnits(xVec, yVec, i, conversionCtx);
            Point loc = TransformMatrix.transLocation(origLoc, vpTransform.transform);
            dev.drawCircle(gpar.getDrawingContext(i), loc.x, loc.y, radius);
        }
        return RNull.instance;
    }
}
