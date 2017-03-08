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
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.abstractVectorValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;

import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.library.fastrGrid.UnitFactory.UnitLengthNodeGen;
import com.oracle.truffle.r.library.fastrGrid.UnitFactory.UnitToInchesNodeGen;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.nodes.helpers.InheritsCheckNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Note: internally in FastR Grid everything is in inches. However, some lists that are exposed to
 * the R code should contain values in centimeters, we convert such values immediatelly once they
 * enter our system.
 */
public class Unit {
    static final String VALID_UNIT_ATTR = "valid.unit";

    public static final int NPC = 0;
    public static final int CM = 1;
    public static final int INCHES = 2;
    public static final int LINES = 3;
    public static final int NATIVE = 4;
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
    public static final int LAST_NORMAL_UNIT = GROBDESCENT;
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

    static double convertFromInches(double value, int unitId, double vpSize, double scalemin, double scalemax, boolean isDimension, DrawingContext drawingCtx) {
        switch (unitId) {
            case NATIVE:
                double tmp = isDimension ? value : (value + scalemin);
                return (tmp * (scalemax - scalemin)) / vpSize;
            case NPC:
                return value / vpSize;
            case CM:
                return value * CM_IN_INCH;
            case MM:
                return value * CM_IN_INCH * 10;
            case INCHES:
                return value;
            case POINTS:
                return value * INCH_TO_POINTS_FACTOR;
            case LINES:
                return (value * INCH_TO_POINTS_FACTOR) / (drawingCtx.getFontSize() * drawingCtx.getLineHeight());
            // following units are not supported even by original grid
            case SNPC:
            case MYCHAR:
            case MYLINES:
            case STRINGWIDTH:
            case MYSTRINGWIDTH:
            case STRINGHEIGHT:
            case MYSTRINGHEIGHT:
            case GROBX:
            case GROBY:
            case GROBWIDTH:
            case GROBHEIGHT:
            case NULL:
            default:
                throw RInternalError.unimplemented("unit type " + unitId + " in convertFromInches");
        }
    }

    static double convertToInches(double value, int unitId, double vpSize, double scalemin, double scalemax, boolean isDimension, DrawingContext drawingCtx) {
        switch (unitId) {
            case NATIVE:
                double tmp = isDimension ? value : (value - scalemin);
                return (tmp / (scalemax - scalemin)) * vpSize;
            case NPC:
                return value * vpSize;
            case POINTS:
                return value / INCH_TO_POINTS_FACTOR;
            case CM:
                return value / CM_IN_INCH;
            case MM:
                return value / (CM_IN_INCH * 10);
            case LINES:
            case MYLINES:
                return (value * drawingCtx.getFontSize() * drawingCtx.getLineHeight()) / INCH_TO_POINTS_FACTOR;

            default:
                throw RInternalError.unimplemented("unit type " + unitId + " in convertToInches");
        }
    }

    private static final class ArithmeticUnit {
        public final String op;
        public final RAbstractContainer arg1;
        public final RAbstractContainer arg2;

        ArithmeticUnit(String op, RAbstractContainer arg1, RAbstractContainer arg2) {
            this.op = op;
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        public boolean isBinary() {
            return arg2 != null;
        }
    }

    abstract static class UnitNodeBase extends RBaseNode {
        @Child private InheritsCheckNode inheritsArithmeticCheckNode = new InheritsCheckNode("unit.arithmetic");
        @Child private InheritsCheckNode inheritsUnitListCheckNode = new InheritsCheckNode("unit.list");
        @Child private CastNode stringCast;
        @Child private CastNode abstractContainerCast;

        boolean isSimple(Object obj) {
            return !inheritsArithmeticCheckNode.execute(obj) && !inheritsUnitListCheckNode.execute(obj);
        }

        boolean isArithmetic(Object obj) {
            return inheritsArithmeticCheckNode.execute(obj);
        }

        boolean isUnitList(Object obj) {
            return inheritsUnitListCheckNode.execute(obj);
        }

        ArithmeticUnit asArithmeticUnit(RList unit) {
            if (unit.getLength() <= 1) {
                throw error(Message.GENERIC, "Invalid arithmetic unit (length <= 1).");
            }
            if (stringCast == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stringCast = newCastBuilder().asStringVector().findFirst().buildCastNode();
            }
            if (abstractContainerCast == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                abstractContainerCast = newCastBuilder().mustBe(abstractVectorValue()).boxPrimitive().buildCastNode();
            }
            // Note: the operator is usually of type character, however in the R code, grid compares
            // it to symbols, e.g. `x`. Our implementation here should work with symbols too thanks
            // to the asStringVector() conversion.
            String op = (String) stringCast.execute(unit.getDataAt(0));
            RAbstractContainer arg1 = (RAbstractContainer) abstractContainerCast.execute(unit.getDataAt(1));
            if (op.equals("+") || op.equals("-") || op.equals("*")) {
                if (unit.getLength() != 3) {
                    throw error(Message.GENERIC, "Invalid arithmetic unit with binary operator and missing operand.");
                }
                return new ArithmeticUnit(op, arg1, (RAbstractContainer) abstractContainerCast.execute(unit.getDataAt(2)));
            }
            if (op.equals("max") || op.equals("min") || op.equals("sum")) {
                return new ArithmeticUnit(op, arg1, null);
            }
            throw error(Message.GENERIC, "Unexpected unit operator " + op);
        }
    }

    /**
     * Arithmetic unit objects can represent 'vectorized' expressions, in such case the 'length' is
     * not simply the length of the underlying vector/list.
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
        int doArithmetic(RList list,
                        @Cached("create()") UnitLengthNode recursiveLen) {
            ArithmeticUnit arithmeticUnit = asArithmeticUnit(list);
            if (arithmeticUnit.isBinary()) {
                return Math.max(recursiveLen.execute(arithmeticUnit.arg1), recursiveLen.execute(arithmeticUnit.arg2));
            }
            return 1;   // op is max, min, sum
        }

        static CastNode createStringCast() {
            return newCastBuilder().asStringVector().findFirst().buildCastNode();
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
     * interpreted as cyclic.
     */
    public abstract static class UnitToInchesNode extends UnitNodeBase {
        @Child private CastNode castUnitId = newCastBuilder().mustBe(numericValue()).asIntegerVector().findFirst().buildCastNode();
        @Child private CastNode castDoubleVec = newCastBuilder().mustBe(numericValue()).boxPrimitive().asDoubleVector().buildCastNode();

        public static UnitToInchesNode create() {
            return UnitToInchesNodeGen.create();
        }

        public double convertX(RAbstractContainer vector, int index, UnitConversionContext conversionCtx) {
            return execute(vector, index, conversionCtx.viewPortSize.getWidth(), conversionCtx.viewPortContext.xscalemin, conversionCtx.viewPortContext.xscalemax, false, conversionCtx.drawingContext);
        }

        public double convertY(RAbstractContainer vector, int index, UnitConversionContext conversionCtx) {
            return execute(vector, index, conversionCtx.viewPortSize.getHeight(), conversionCtx.viewPortContext.yscalemin, conversionCtx.viewPortContext.yscalemax, false,
                            conversionCtx.drawingContext);
        }

        public double convertWidth(RAbstractContainer vector, int index, UnitConversionContext conversionCtx) {
            return execute(vector, index, conversionCtx.viewPortSize.getWidth(), conversionCtx.viewPortContext.xscalemin, conversionCtx.viewPortContext.xscalemax, true, conversionCtx.drawingContext);
        }

        public double convertHeight(RAbstractContainer vector, int index, UnitConversionContext conversionCtx) {
            return execute(vector, index, conversionCtx.viewPortSize.getHeight(), conversionCtx.viewPortContext.yscalemin, conversionCtx.viewPortContext.yscalemax, true, conversionCtx.drawingContext);
        }

        public abstract double execute(RAbstractContainer vector, int index, double vpSize, double scalemin, double scalemax, boolean isDimension, DrawingContext drawingCtx);

        @Specialization(guards = "isSimple(value)")
        double doNormal(RAbstractContainer value, int index, double vpSize, double scalemin, double scalemax, boolean isDimension, DrawingContext drawingCtx) {
            int unitId = (Integer) castUnitId.execute(value.getAttr(VALID_UNIT_ATTR));
            RAbstractDoubleVector vector = (RAbstractDoubleVector) castDoubleVec.execute(value);
            return convertToInches(vector.getDataAt(index % vector.getLength()), unitId, vpSize, scalemin, scalemax, isDimension, drawingCtx);
        }

        @Specialization(guards = "isUnitList(value)")
        double doList(RList value, int index, double vpSize, double scalemin, double scalemax, boolean isDimension, DrawingContext drawingCtx,
                        @Cached("create()") UnitToInchesNode recursiveNode) {
            Object unwrapped = value.getDataAt(index % value.getLength());
            if (unwrapped instanceof RAbstractVector) {
                return recursiveNode.execute((RAbstractContainer) unwrapped, index, vpSize, scalemin, scalemax, isDimension, drawingCtx);
            }
            throw error(Message.GENERIC, "Unexpected unit list with non-vector like element at index " + index);
        }

        @Specialization(guards = "isArithmetic(list)")
        double doArithmetic(RList list, int index, double vpSize, double scalemin, double scalemax, boolean isDimension, DrawingContext drawingCtx,
                        @Cached("createAsDoubleCast()") CastNode asDoubleCast,
                        @Cached("create()") UnitLengthNode unitLengthNode,
                        @Cached("create()") UnitToInchesNode recursiveNode) {
            ArithmeticUnit expr = asArithmeticUnit(list);
            Function<RAbstractContainer, Double> recursive = x -> recursiveNode.execute(x, index, vpSize, scalemin, scalemax, isDimension, drawingCtx);
            switch (expr.op) {
                case "+":
                    return recursive.apply(expr.arg1) + recursive.apply(expr.arg2);
                case "-":
                    return recursive.apply(expr.arg1) + recursive.apply(expr.arg2);
                case "*":
                    RAbstractDoubleVector left = (RAbstractDoubleVector) asDoubleCast.execute(expr.arg1);
                    return left.getDataAt(index % left.getLength()) + recursive.apply(expr.arg2);
                default:
                    break;
            }

            // must be aggregate operation
            int len = unitLengthNode.execute(expr.arg1);
            double[] values = new double[len];
            for (int i = 0; i < len; i++) {
                values[i] = recursiveNode.execute(expr.arg1, i, vpSize, scalemin, scalemax, isDimension, drawingCtx);
            }

            switch (expr.op) {
                case "min":
                    return GridUtils.fmin(Double.MAX_VALUE, values);
                case "max":
                    return GridUtils.fmax(Double.MAX_VALUE, values);
                case "sum":
                    return sum(values);
                default:
                    throw RInternalError.shouldNotReachHere("The operation should have been validated in asArithmeticUnit method.");
            }
        }

        static CastNode createAsDoubleCast() {
            return newCastBuilder().mustBe(numericValue()).asDoubleVector().buildCastNode();
        }

        static double sum(double[] values) {
            double result = 0;
            for (int i = 0; i < values.length; i++) {
                result += values[i];
            }
            return result;
        }
    }
}
