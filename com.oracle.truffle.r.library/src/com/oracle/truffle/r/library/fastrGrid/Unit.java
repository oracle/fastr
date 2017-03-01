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

import static com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.INCH_TO_POINTS_FACTOR;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.library.fastrGrid.UnitFactory.UnitElementAtNodeGen;
import com.oracle.truffle.r.library.fastrGrid.UnitFactory.UnitLengthNodeGen;
import com.oracle.truffle.r.library.fastrGrid.UnitFactory.UnitToInchesNodeGen;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.nodes.helpers.InheritsCheckNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;

/**
 * Note: internally in FastR Grid everything is in inches. However, some lists that are exposed to
 * the R code should contain values in centimeters, we convert such values immediatelly once they
 * enter our system.
 */
public class Unit {
    private static final String VALID_UNIT_ATTR = "valid.unit";

    private static final int NPC = 0;
    public static final int CM = 1;
    public static final int INCHES = 2;
    private static final int LINES = 3;
    private static final int NATIVE = 4;
    public static final int NULL = 5; /* only used in layout specifications (?) */
    public static final int SNPC = 6;
    public static final int MM = 7;
    /*
     * Some units based on TeX's definition thereof
     */
    public static final int POINTS = 8; /* 72.27 pt = 1 in */
    public static final int PICAS = 9; /* 1 pc = 12 pt */
    public static final int BIGPOINTS = 10; /* 72 bp = 1 in */
    public static final int DIDA = 11; /* 1157 dd = 1238 pt */
    public static final int CICERO = 12; /* 1 cc = 12 dd */
    public static final int SCALEDPOINTS = 13; /* 65536 sp = 1pt */
    /*
     * Some units which require an object to query for a value.
     */
    public static final int STRINGWIDTH = 14;
    public static final int STRINGHEIGHT = 15;
    public static final int STRINGASCENT = 16;
    public static final int STRINGDESCENT = 17;
    /*
     * public static final int LINES now means multiples of the line height. This is multiples of
     * the font size.
     */
    public static final int CHAR = 18;
    public static final int GROBX = 19;
    public static final int GROBY = 20;
    public static final int GROBWIDTH = 21;
    public static final int GROBHEIGHT = 22;
    public static final int GROBASCENT = 23;
    public static final int GROBDESCENT = 24;
    /*
     * No longer used
     */
    private static final int MYLINES = 103;
    public static final int MYCHAR = 104;
    public static final int MYSTRINGWIDTH = 105;
    public static final int MYSTRINGHEIGHT = 106;

    private static final double CM_IN_INCH = 2.54;

    public static double inchesToCm(double inches) {
        return inches * CM_IN_INCH;
    }

    public static double cmToInches(double cm) {
        return cm / CM_IN_INCH;
    }

    public static UnitLengthNode createLengthNode() {
        return UnitLengthNode.create();
    }

    public static UnitToInchesNode createToInchesNode() {
        return UnitToInchesNode.create();
    }

    public abstract static class UnitNodeBase extends Node {
        @Child private InheritsCheckNode inheritsCheckNode = new InheritsCheckNode("unit.arithmetic");

        boolean isArithmetic(Object obj) {
            return obj instanceof RList && inheritsCheckNode.execute(obj);
        }
    }

    /**
     * A unit object can represent more or fewer values that the number of elements underlying list
     * or vector. This node gives the length if the unit in a sense of the upper limit on what can
     * be used as an index for {@link UnitElementAtNode}.
     */
    public abstract static class UnitLengthNode extends UnitNodeBase {
        public static UnitLengthNode create() {
            return UnitLengthNodeGen.create();
        }

        public abstract int execute(RAbstractContainer vector);

        @Specialization(guards = "!isArithmetic(value)")
        int doNormal(RAbstractContainer value) {
            return value.getLength();
        }

        @Specialization(guards = "isArithmetic(list)")
        int doArithmetic(RList list) {
            throw RInternalError.unimplemented("Length for arithmetic units");
        }
    }

    /**
     * @see UnitLengthNode
     */
    public abstract static class UnitElementAtNode extends UnitNodeBase {
        @Child private CastNode castToDouble = newCastBuilder().asDoubleVector().buildCastNode();

        public static UnitElementAtNode create() {
            return UnitElementAtNodeGen.create();
        }

        public abstract double execute(RAbstractContainer vector, int index);

        @Specialization(guards = "!isArithmetic(value)")
        double doNormal(RAbstractContainer value, int index) {
            return ((RAbstractDoubleVector) castToDouble.execute(value)).getDataAt(index);
        }

        @Specialization(guards = "isArithmetic(list)")
        double doArithmetic(RList list, int index) {
            throw RInternalError.unimplemented("UnitElementAt for arithmetic units");
        }
    }

    /**
     * Wraps the data necessary for converting a unit to another unit.
     */
    public static final class UnitConversionContext {
        public final Size viewPortSize;
        public final ViewPortContext viewPortContext;
        public final DrawingContext drawingContext;

        public UnitConversionContext(Size viewPortSize, ViewPortContext viewPortContext, DrawingContext drawingContext) {
            this.viewPortSize = viewPortSize;
            this.viewPortContext = viewPortContext;
            this.drawingContext = drawingContext;
        }
    }

    /**
     * Normalizes grid unit object to a double value in inches. For convenience the index is
     * interpreted as cyclic unlike in {@link UnitElementAtNode}.
     */
    public abstract static class UnitToInchesNode extends UnitNodeBase {
        @Child private CastNode castUnitId = newCastBuilder().mustBe(numericValue()).asIntegerVector().findFirst().buildCastNode();
        @Child private UnitElementAtNode elementAtNode = UnitElementAtNode.create();

        public static UnitToInchesNode create() {
            return UnitToInchesNodeGen.create();
        }

        public double convertX(RAbstractContainer vector, int index, UnitConversionContext conversionCtx) {
            return execute(vector, index, conversionCtx.viewPortSize.getWidth(), conversionCtx.viewPortContext.xscalemin, conversionCtx.viewPortContext.xscalemax, conversionCtx.drawingContext);
        }

        public double convertY(RAbstractContainer vector, int index, UnitConversionContext conversionCtx) {
            return execute(vector, index, conversionCtx.viewPortSize.getHeight(), conversionCtx.viewPortContext.yscalemin, conversionCtx.viewPortContext.yscalemax, conversionCtx.drawingContext);
        }

        public abstract double execute(RAbstractContainer vector, int index, double vpSize, double scalemin, double scalemax, DrawingContext drawingCtx);

        @Specialization(guards = "!isArithmetic(value)")
        double doNormal(RAbstractContainer value, int index, double vpSize, double scalemin, double scalemax, DrawingContext drawingCtx) {
            int unitId = (Integer) castUnitId.execute(value.getAttr(VALID_UNIT_ATTR));
            return convert(elementAtNode.execute(value, index % value.getLength()), unitId, vpSize, scalemin, scalemax, drawingCtx);
        }

        @Specialization(guards = "isArithmetic(list)")
        double doArithmetic(RList list, int index, double vpSize, double scalemin, double scalemax, DrawingContext drawingCtx) {
            throw RInternalError.unimplemented("UnitToInches for arithmetic units");
        }

        private static double convert(double value, int unitId, double vpSize, double scalemin, double scalemax, DrawingContext drawingCtx) {
            switch (unitId) {
                case NATIVE:
                    return ((value - scalemin) / (scalemax - scalemin)) * vpSize;
                case NPC:
                    return value * vpSize;
                case LINES:
                case MYLINES:
                    return (value * drawingCtx.getFontSize() * drawingCtx.getLineHeight()) / INCH_TO_POINTS_FACTOR;

                default:
                    throw RInternalError.unimplemented("unit type " + unitId + " in UnitToInches");
            }
        }
    }
}
