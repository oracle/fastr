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

import static com.oracle.truffle.r.library.fastrGrid.Unit.NATIVE;
import static com.oracle.truffle.r.library.fastrGrid.Unit.NPC;
import static com.oracle.truffle.r.library.fastrGrid.Unit.isArithmeticUnit;
import static com.oracle.truffle.r.library.fastrGrid.Unit.isListUnit;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.fastrGrid.Unit.UnitConversionContext;
import com.oracle.truffle.r.library.fastrGrid.ViewPortContext.VPContextFromVPNode;
import com.oracle.truffle.r.library.fastrGrid.ViewPortTransform.GetViewPortTransformNode;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

public abstract class LConvert extends RExternalBuiltinNode.Arg4 {
    @Child private Unit.UnitLengthNode unitLength = Unit.createLengthNode();
    @Child private Unit.UnitToInchesNode unitToInches = Unit.createToInchesNode();
    @Child private GetViewPortTransformNode getViewPortTransform = new GetViewPortTransformNode();
    @Child private VPContextFromVPNode vpContextFromVP = new VPContextFromVPNode();

    static {
        Casts casts = new Casts(LConvert.class);
        casts.arg(0).mustBe(abstractVectorValue());
        casts.arg(1).mustBe(numericValue()).asIntegerVector();
        casts.arg(2).mustBe(numericValue()).asIntegerVector();
        casts.arg(3).mustBe(numericValue()).asIntegerVector();
    }

    public static LConvert create() {
        return LConvertNodeGen.create();
    }

    @Specialization
    Object doConvert(RAbstractVector units, RAbstractIntVector axisFromVec, RAbstractIntVector axisToVec, RAbstractIntVector unitToVec) {

        GridContext ctx = GridContext.getContext();
        GridDevice dev = ctx.getCurrentDevice();

        RList currentVP = ctx.getGridState().getViewPort();
        DrawingContext drawingCtx = GPar.asDrawingContext(ctx.getGridState().getGpar());
        ViewPortTransform vpTransform = getViewPortTransform.execute(currentVP);
        ViewPortContext vpContext = vpContextFromVP.execute(currentVP);
        UnitConversionContext conversionCtx = new UnitConversionContext(vpTransform.size, vpContext, drawingCtx);

        int length = unitLength.execute(units);
        double[] result = new double[length];

        RAbstractIntVector unitIds = GridUtils.asIntVector(units.getAttr(Unit.VALID_UNIT_ATTR));
        boolean fromUnitIsSimple = !isArithmeticUnit(units) && !isListUnit(units);

        for (int i = 0; i < length; i++) {
            // scalar values used in current iteration
            int axisFrom = axisFromVec.getDataAt(i % axisFromVec.getLength());
            int axisTo = axisToVec.getDataAt(i % axisToVec.getLength());
            boolean compatibleAxes = axisFrom == axisTo ||
                            (axisFrom == 0 && axisTo == 2) ||
                            (axisFrom == 2 && axisTo == 0) ||
                            (axisFrom == 1 && axisTo == 3) ||
                            (axisFrom == 3 && axisTo == 1);
            double vpToSize = isXAxis(axisTo) ? vpTransform.size.getWidth() : vpTransform.size.getHeight();
            double vpFromSize = isXAxis(axisFrom) ? vpTransform.size.getWidth() : vpTransform.size.getHeight();
            int unitTo = unitToVec.getDataAt(i % unitToVec.getLength());
            int fromUnitId = unitIds.getDataAt(i % unitIds.getLength());

            // actual conversion:
            // if the units are both relative, we are converting compatible axes and the vpSize for
            // 'from' axis is small, we will not convert through inches, but directly to avoid
            // divide by zero, but still do something useful
            boolean bothRelative = isRelative(unitTo) && isRelative(fromUnitId);
            boolean realativeConversion = bothRelative && fromUnitIsSimple && compatibleAxes && vpFromSize < 1e-6;
            if (realativeConversion) {
                // if the unit is not "unit.arithmetic" or "unit.list", it must be double vector
                RAbstractDoubleVector simpleUnits = (RAbstractDoubleVector) units;
                double fromValue = simpleUnits.getDataAt(i % simpleUnits.getLength());
                result[i] = transformFromNPC(tranfromToNPC(fromValue, fromUnitId, axisFrom, vpContext), unitTo, axisTo, vpContext);
            } else {
                double inches = toInches(units, i, axisFrom, conversionCtx);
                boolean isX = isXAxis(axisTo);
                double scalemin = isX ? vpContext.xscalemin : vpContext.yscalemin;
                double scalemax = isX ? vpContext.xscalemax : vpContext.yscalemax;
                result[i] = Unit.convertFromInches(inches, unitTo, vpToSize, scalemin, scalemax, isDimension(axisTo), drawingCtx);
            }
        }

        return RDataFactory.createDoubleVector(result, RDataFactory.COMPLETE_VECTOR);
    }

    private double toInches(RAbstractVector units, int index, int axisFrom, UnitConversionContext conversionCtx) {
        double inches;
        if (isXAxis(axisFrom)) {
            if (isDimension(axisFrom)) {
                inches = unitToInches.convertWidth(units, index, conversionCtx);
            } else {
                inches = unitToInches.convertX(units, index, conversionCtx);
            }
        } else {
            if (isDimension(axisFrom)) {
                inches = unitToInches.convertHeight(units, index, conversionCtx);
            } else {
                inches = unitToInches.convertY(units, index, conversionCtx);
            }
        }
        return inches;
    }

    private static double tranfromToNPC(double value, int fromUnitId, int axisFrom, ViewPortContext vpContext) {
        if (fromUnitId == Unit.NPC) {
            return value;
        }
        assert fromUnitId == Unit.NATIVE : "relative conversion should only happen when units are NPC or NATIVE";
        boolean isX = isXAxis(axisFrom);
        double min = isX ? vpContext.xscalemin : vpContext.yscalemin;
        double max = isX ? vpContext.xscalemax : vpContext.yscalemax;
        if (isDimension(axisFrom)) {
            return value / (max - min);
        } else {
            return (value - min) / (max - min);
        }
    }

    private static double transformFromNPC(double value, int unitTo, int axisTo, ViewPortContext vpContext) {
        if (unitTo == Unit.NPC) {
            return value;
        }
        assert unitTo == Unit.NATIVE : "relative conversion should only happen when units are NPC or NATIVE";
        boolean isX = isXAxis(axisTo);
        double min = isX ? vpContext.xscalemin : vpContext.yscalemin;
        double max = isX ? vpContext.xscalemax : vpContext.yscalemax;
        if (isDimension(axisTo)) {
            return value * (max - min);
        } else {
            return min + value * (max - min);
        }
    }

    // Note: this is not relative in the same sense as IsRelativeUnitNode. The later checks for
    // special NULL unit used only in layouting.
    private static boolean isRelative(int unitId) {
        return unitId == NPC || unitId == NATIVE;
    }

    // what = 0 means x, 1 means y, 2 means width, 3 means height
    private static boolean isXAxis(int what) {
        return what % 2 == 0;
    }

    private static boolean isDimension(int what) {
        return what >= 2;
    }
}
