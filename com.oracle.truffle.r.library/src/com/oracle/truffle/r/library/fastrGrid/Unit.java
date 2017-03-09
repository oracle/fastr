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
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asIntVector;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asList;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.fmax;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.fmin;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.hasRClass;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.sum;
import static com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.INCH_TO_POINTS_FACTOR;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;

import java.util.function.BiFunction;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.library.fastrGrid.UnitFactory.UnitLengthNodeGen;
import com.oracle.truffle.r.library.fastrGrid.UnitFactory.UnitToInchesNodeGen;
import com.oracle.truffle.r.library.fastrGrid.ViewPortContext.VPContextFromVPNode;
import com.oracle.truffle.r.library.fastrGrid.ViewPortTransform.GetViewPortTransformNode;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.nodes.attributes.GetFixedAttributeNode;
import com.oracle.truffle.r.nodes.helpers.InheritsCheckNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
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
    private static final int CM = 1;
    public static final int INCHES = 2;
    private static final int LINES = 3;
    public static final int NATIVE = 4;
    private static final int NULL = 5; /* only used in layout specifications */
    private static final int SNPC = 6;
    private static final int MM = 7;
    /*
     * Some units based on TeX's definition thereof
     */
    private static final int POINTS = 8; /* 72.27 pt = 1 in */
    public static final int PICAS = 9; /* 1 pc = 12 pt */
    public static final int BIGPOINTS = 10; /* 72 bp = 1 in */
    public static final int DIDA = 11; /* 1157 dd = 1238 pt */
    public static final int CICERO = 12; /* 1 cc = 12 dd */
    public static final int SCALEDPOINTS = 13; /* 65536 sp = 1pt */
    /*
     * Some units which require an object to query for a value.
     */
    private static final int STRINGWIDTH = 14;
    private static final int STRINGHEIGHT = 15;
    public static final int STRINGASCENT = 16;
    public static final int STRINGDESCENT = 17;
    /*
     * LINES now means multiples of the line height. This is multiples of the font size.
     */
    private static final int CHAR = 18;
    private static final int GROBX = 19;
    private static final int GROBY = 20;
    private static final int GROBWIDTH = 21;
    private static final int GROBHEIGHT = 22;
    public static final int GROBASCENT = 23;
    private static final int GROBDESCENT = 24;
    private static final int LAST_NORMAL_UNIT = GROBDESCENT;
    /*
     * No longer used
     */
    private static final int MYLINES = 103;
    private static final int MYCHAR = 104;
    private static final int MYSTRINGWIDTH = 105;
    private static final int MYSTRINGHEIGHT = 106;

    // null layout arithmetic mode
    private static final int L_adding = 1;
    private static final int L_subtracting = 2;
    private static final int L_summing = 3;
    private static final int L_plain = 4;
    private static final int L_maximising = 5;
    private static final int L_minimising = 6;
    private static final int L_multiplying = 7;

    private static final double CM_IN_INCH = 2.54;

    public static double inchesToCm(double inches) {
        return inches * CM_IN_INCH;
    }

    public static double cmToInches(double cm) {
        return cm / CM_IN_INCH;
    }

    public static RAbstractDoubleVector newUnit(double value, int unitId) {
        assert unitId > 0 && unitId <= LAST_NORMAL_UNIT;
        RDoubleVector result = RDataFactory.createDoubleVector(new double[]{value}, RDataFactory.COMPLETE_VECTOR);
        result.setClassAttr(RDataFactory.createStringVectorFromScalar("unit"));
        result.setAttr("valid.unit", unitId);
        result.setAttr("data", RNull.instance);
        return result;
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
                double addition = isDimension ? 0 : scalemin;
                return addition + (value / vpSize) * (scalemax - scalemin);
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

    private static double convertToInches(double value, int unitId, double vpSize, double scalemin, double scalemax, int nullLMode, int nullAMode, boolean isDimension, DrawingContext drawingCtx) {
        switch (unitId) {
            case INCHES:
                return value;
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
            case CHAR:
            case MYCHAR:
                return (value * drawingCtx.getFontSize()) / INCH_TO_POINTS_FACTOR;
            case LINES:
            case MYLINES:
                return (value * drawingCtx.getFontSize() * drawingCtx.getLineHeight()) / INCH_TO_POINTS_FACTOR;
            case NULL:
                return evaluateNullUnit(value, vpSize, nullLMode, nullAMode);
            default:
                throw RInternalError.unimplemented("unit type " + unitId + " in convertToInches");
        }
    }

    private static double evaluateNullUnit(double value, double vpSize, int nullLMode, int nullAMode) {
        if (nullLMode > 0) {
            return value;
        }
        switch (nullAMode) {
            case L_plain:
            case L_adding:
            case L_subtracting:
            case L_summing:
            case L_multiplying:
            case L_maximising:
                return 0;
            case L_minimising:
                return vpSize;  // Note: GnuR has vpSize in "cm", so this will be in "cm"
                                // too, does it matter?
        }
        return value;
    }

    static boolean isListUnit(Object unit) {
        return unit instanceof RList && hasRClass((RAttributable) unit, "unit.list");
    }

    static boolean isArithmeticUnit(Object unit) {
        return unit instanceof RList && hasRClass((RAttributable) unit, "unit.arithmetic");
    }

    private static boolean isGrobUnit(int unitId) {
        return unitId >= GROBX && unitId <= GROBDESCENT;
    }

    static double pureNullUnitValue(RAbstractContainer unit, int index) {
        // TODO: convert to unit visitor
        if (unit instanceof RAbstractDoubleVector) {
            RAbstractDoubleVector simpleUnit = (RAbstractDoubleVector) unit;
            return simpleUnit.getDataAt(index % simpleUnit.getLength());
        } else if (isListUnit(unit)) {
            return pureNullUnitValue((RAbstractContainer) ((RList) unit).getDataAt(index % unit.getLength()), 0);
        } else if (isArithmeticUnit(unit)) {
            ArithmeticUnit expr = ArithmeticUnit.asArithmeticUnit((RList) unit);
            switch (expr.op) {
                case "+":
                    return pureNullUnitValue(expr.arg1, index) + pureNullUnitValue(expr.arg2, index);
                case "-":
                    return pureNullUnitValue(expr.arg1, index) - pureNullUnitValue(expr.arg2, index);
                case "*":
                    return asDouble(expr.arg1) * pureNullUnitValue(expr.arg2, index);
                case "min":
                case "max":
                case "sum":
                    double[] values = new double[expr.arg1.getLength()];
                    for (int i = 0; i < values.length; i++) {
                        values[i] = pureNullUnitValue(expr.arg1, i);
                    }
                    switch (expr.op) {
                        case "min":
                            return fmin(Double.MAX_VALUE, values);
                        case "max":
                            return fmax(Double.MIN_VALUE, values);
                        case "sum":
                            return sum(values);
                        default:
                            throw RInternalError.shouldNotReachHere("unexpected arithmetic unit operation");
                    }
            }

        }
        throw RInternalError.shouldNotReachHere("unexpected arithmetic unit type");
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

        static ArithmeticUnit asArithmeticUnit(RList unit) {
            if (unit.getLength() <= 1) {
                throw RError.error(RError.NO_CALLER, Message.GENERIC, "Invalid arithmetic unit (length <= 1).");
            }
            // Note: the operator is usually of type character, however in the R code, grid compares
            // it to symbols, e.g. `x`. Our implementation here should work with symbols too thanks
            // to the asStringVector() conversion.
            String op = RRuntime.asString(unit.getDataAt(0));
            RAbstractContainer arg1 = asAbstractContainer(unit.getDataAt(1));
            if (op.equals("+") || op.equals("-") || op.equals("*")) {
                if (unit.getLength() != 3) {
                    throw RError.error(RError.NO_CALLER, Message.GENERIC, "Invalid arithmetic unit with binary operator and missing operand.");
                }
                return new ArithmeticUnit(op, arg1, asAbstractContainer(unit.getDataAt(2)));
            }
            if (op.equals("max") || op.equals("min") || op.equals("sum")) {
                return new ArithmeticUnit(op, arg1, null);
            }
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "Unexpected unit operator " + op);
        }

        public boolean isBinary() {
            return arg2 != null;
        }
    }

    abstract static class UnitNodeBase extends RBaseNode {
        @Child private InheritsCheckNode inheritsArithmeticCheckNode = new InheritsCheckNode("unit.arithmetic");
        @Child private InheritsCheckNode inheritsUnitListCheckNode = new InheritsCheckNode("unit.list");

        boolean isSimple(Object obj) {
            return !inheritsArithmeticCheckNode.execute(obj) && !inheritsUnitListCheckNode.execute(obj);
        }

        boolean isArithmetic(Object obj) {
            return inheritsArithmeticCheckNode.execute(obj);
        }

        boolean isUnitList(Object obj) {
            return inheritsUnitListCheckNode.execute(obj);
        }
    }

    public static final class UnitUnitIdNode extends Node {
        @Child private GetFixedAttributeNode getValidUnitsAttr = GetFixedAttributeNode.create(VALID_UNIT_ATTR);

        public int execute(RAbstractContainer unit, int index) {
            RAbstractIntVector validUnits = asIntVector(getValidUnitsAttr.execute(unit));
            return validUnits.getDataAt(index % validUnits.getLength());
        }

        public static UnitUnitIdNode create() {
            return new UnitUnitIdNode();
        }
    }

    public static final class IsRelativeUnitNode extends UnitNodeBase {
        @Child private RGridCodeCall isPureNullCall = new RGridCodeCall("isPureNullUnit");

        public boolean execute(Object unit, int index) {
            // Note: converting 0-based java index to 1-based R index
            Object result = isPureNullCall.execute(new RArgsValuesAndNames(new Object[]{unit, index + 1}, ArgumentsSignature.empty(2)));
            byte resultByte;
            if (result instanceof Byte) {
                resultByte = (Byte) result;
            } else if (result instanceof RAbstractLogicalVector) {
                resultByte = ((RAbstractLogicalVector) result).getDataAt(0);
            } else {
                throw RInternalError.shouldNotReachHere("unexpected result type form isPuteNullUnit");
            }
            return RRuntime.fromLogical(resultByte);
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
            ArithmeticUnit arithmeticUnit = ArithmeticUnit.asArithmeticUnit(list);
            if (arithmeticUnit.isBinary()) {
                return Math.max(recursiveLen.execute(arithmeticUnit.arg1), recursiveLen.execute(arithmeticUnit.arg2));
            }
            return 1;   // op is max, min, sum
        }
    }

    /**
     * Wraps the data necessary for converting a unit to another unit. Note: {@code nullLMode} and
     * {@code nullAMode} is only used for converting 'NULL' units and is only explicitly set when
     * calculating layout. When e.g. drawing or calculating bounds, both should have default zero
     * value.
     */
    public static final class UnitConversionContext {
        public final Size viewPortSize;
        public final ViewPortContext viewPortContext;
        public final DrawingContext drawingContext;
        public final int nullLayoutMode;
        public final int nullArithmeticMode;

        public UnitConversionContext(Size viewPortSize, ViewPortContext viewPortContext, DrawingContext drawingContext) {
            this(viewPortSize, viewPortContext, drawingContext, 0, 0);
        }

        public UnitConversionContext(Size viewPortSize, ViewPortContext viewPortContext, DrawingContext drawingContext, int nullLMode, int nullAMode) {
            this.viewPortSize = viewPortSize;
            this.viewPortContext = viewPortContext;
            this.drawingContext = drawingContext;
            this.nullLayoutMode = nullLMode;
            this.nullArithmeticMode = nullAMode;
        }
    }

    /**
     * Normalizes grid unit object to a double value in inches. For convenience the index is
     * interpreted as cyclic.
     */
    public abstract static class UnitToInchesNode extends UnitNodeBase {
        @Child GrobUnitToInches grobUnitToInches = new GrobUnitToInches();

        public static UnitToInchesNode create() {
            return UnitToInchesNodeGen.create();
        }

        public double convertX(RAbstractContainer vector, int index, UnitConversionContext ctx) {
            return execute(vector, index, ctx.viewPortSize.getWidth(), ctx.viewPortContext.xscalemin, ctx.viewPortContext.xscalemax, ctx.nullLayoutMode, ctx.nullArithmeticMode, false,
                            ctx.drawingContext);
        }

        public double convertY(RAbstractContainer vector, int index, UnitConversionContext ctx) {
            return execute(vector, index, ctx.viewPortSize.getHeight(), ctx.viewPortContext.yscalemin, ctx.viewPortContext.yscalemax, ctx.nullLayoutMode, ctx.nullArithmeticMode, false,
                            ctx.drawingContext);
        }

        public double convertWidth(RAbstractContainer vector, int index, UnitConversionContext ctx) {
            return execute(vector, index, ctx.viewPortSize.getWidth(), ctx.viewPortContext.xscalemin, ctx.viewPortContext.xscalemax, ctx.nullLayoutMode, ctx.nullArithmeticMode, true,
                            ctx.drawingContext);
        }

        public double convertHeight(RAbstractContainer vector, int index, UnitConversionContext ctx) {
            return execute(vector, index, ctx.viewPortSize.getHeight(), ctx.viewPortContext.yscalemin, ctx.viewPortContext.yscalemax, ctx.nullLayoutMode, ctx.nullArithmeticMode, true,
                            ctx.drawingContext);
        }

        public double convertDimension(RAbstractContainer vector, int index, UnitConversionContext ctx, boolean isWidth) {
            return isWidth ? convertWidth(vector, index, ctx) : convertHeight(vector, index, ctx);
        }

        public abstract double execute(RAbstractContainer vector, int index, double vpSize, double scalemin, double scalemax, int nullLMode, int nullAMode, boolean isDimension,
                        DrawingContext drawingCtx);

        @Specialization(guards = "isSimple(value)")
        double doNormal(RAbstractContainer value, int index, double vpSize, double scalemin, double scalemax, int nullLMode, int nullAMode, boolean isDimension, DrawingContext drawingCtx,
                        @Cached("createAsDoubleCast()") CastNode asDoubleCast,
                        @Cached("create()") UnitUnitIdNode unitUnitId) {
            int unitId = unitUnitId.execute(value, index);
            RAbstractDoubleVector vector = (RAbstractDoubleVector) asDoubleCast.execute(value);
            double scalarValue = vector.getDataAt(index % vector.getLength());
            if (isGrobUnit(unitId)) {
                RList grobList = asList(vector.getAttr("data"));
                return grobUnitToInches.execute(scalarValue, unitId, grobList.getDataAt(index % grobList.getLength()), nullLMode, nullAMode);
            }
            return convertToInches(scalarValue, unitId, vpSize, scalemin, scalemax, nullLMode, nullAMode, isDimension, drawingCtx);
        }

        @Specialization(guards = "isUnitList(value)")
        double doList(RList value, int index, double vpSize, double scalemin, double scalemax, int nullLMode, int nullAMode, boolean isDimension, DrawingContext drawingCtx,
                        @Cached("create()") UnitToInchesNode recursiveNode) {
            Object unwrapped = value.getDataAt(index % value.getLength());
            if (unwrapped instanceof RAbstractVector) {
                return recursiveNode.execute((RAbstractContainer) unwrapped, 0, vpSize, scalemin, scalemax, nullLMode, nullAMode, isDimension, drawingCtx);
            }
            throw error(Message.GENERIC, "Unexpected unit list with non-vector like element at index " + index);
        }

        @Specialization(guards = "isArithmetic(list)")
        double doArithmetic(RList list, int index, double vpSize, double scalemin, double scalemax, int nullLMode, int nullAMode, boolean isDimension, DrawingContext drawingCtx,
                        @Cached("create()") UnitLengthNode unitLengthNode,
                        @Cached("create()") UnitToInchesNode recursiveNode) {
            ArithmeticUnit expr = ArithmeticUnit.asArithmeticUnit(list);
            // Note the catch: newNullAMode is applied only if isDimension == true
            BiFunction<RAbstractContainer, Integer, Double> recursive = (x, newNullAMode) -> recursiveNode.execute(x, index, vpSize, scalemin, scalemax, nullLMode,
                            isDimension ? newNullAMode : nullAMode, isDimension, drawingCtx);
            switch (expr.op) {
                case "+":
                    return recursive.apply(expr.arg1, L_adding) + recursive.apply(expr.arg2, L_adding);
                case "-":
                    return recursive.apply(expr.arg1, L_subtracting) - recursive.apply(expr.arg2, L_subtracting);
                case "*":
                    RAbstractDoubleVector left = GridUtils.asDoubleVector(expr.arg1);
                    return left.getDataAt(index % left.getLength()) * recursive.apply(expr.arg2, L_multiplying);
                default:
                    break;
            }

            // must be aggregate operation
            int newNullAMode = isDimension ? getNullAMode(expr.op) : nullAMode;
            int len = unitLengthNode.execute(expr.arg1);
            double[] values = new double[len];
            for (int i = 0; i < len; i++) {
                values[i] = recursiveNode.execute(expr.arg1, i, vpSize, scalemin, scalemax, nullLMode, newNullAMode, isDimension, drawingCtx);
            }

            switch (expr.op) {
                case "min":
                    return GridUtils.fmin(Double.MAX_VALUE, values);
                case "max":
                    return GridUtils.fmax(Double.MAX_VALUE, values);
                case "sum":
                    return GridUtils.sum(values);
                default:
                    throw RInternalError.shouldNotReachHere("The operation should have been validated in asArithmeticUnit method.");
            }
        }

        private static int getNullAMode(String op) {
            switch (op) {
                case "min":
                    return L_minimising;
                case "max":
                    return L_maximising;
                case "sum":
                    return L_summing;
            }
            return L_plain;
        }

        static CastNode createAsDoubleCast() {
            return newCastBuilder().mustBe(numericValue()).asDoubleVector().buildCastNode();
        }
    }

    public static final class GrobUnitToInches extends Node {
        @Child private RGridCodeCall preDrawCode = new RGridCodeCall("grobConversionPreDraw");
        @Child private RGridCodeCall getUnitXY = new RGridCodeCall("grobConversionGetUnitXY");
        @Child private RGridCodeCall postDrawCode = new RGridCodeCall("grobConversionPostDraw");
        @Child private GetViewPortTransformNode getViewPortTransform = new GetViewPortTransformNode();
        @Child private VPContextFromVPNode vpContextFromVP = new VPContextFromVPNode();
        @Child private IsRelativeUnitNode isRelativeUnit = new IsRelativeUnitNode();
        @Child private UnitToInchesNode unitToInchesNode;

        // transcribed from unit.c function evaluateGrobUnit

        public double execute(double value, int unitId, Object grob, int nullLMode, int nullAMode) {
            GridContext ctx = GridContext.getContext();
            RList currentVP = ctx.getGridState().getViewPort();
            getViewPortTransform.execute(currentVP);

            RList savedGPar = ctx.getGridState().getGpar();
            Object savedGrob = ctx.getGridState().getCurrentGrob();

            Object updatedGrob = preDrawCode.call(grob);

            /*
             * The call to preDraw may have pushed viewports and/or enforced gpar settings, SO we
             * need to re-establish the current viewport and gpar settings before evaluating the
             * width unit.
             */
            currentVP = ctx.getGridState().getViewPort();
            RList currentGP = ctx.getGridState().getGpar();
            ViewPortTransform vpTransform = getViewPortTransform.execute(currentVP);
            ViewPortContext vpContext = vpContextFromVP.execute(currentVP);

            // getUnitXY returns a list with either one or two items
            RList unitxy = (RList) getUnitXY.execute(new RArgsValuesAndNames(new Object[]{updatedGrob, unitId}, ArgumentsSignature.empty(2)));
            double result;
            switch (unitId) {
                case GROBX:
                case GROBY:
                    if (unitId == GROBY && isRelativeUnit.execute(unitxy.getDataAt(1), 0)) {
                        double nullUnitValue = pureNullUnitValue((RAbstractContainer) unitxy.getDataAt(1), 0);
                        result = evaluateNullUnit(nullUnitValue, vpTransform.size.getHeight(), nullLMode, nullAMode);
                    } else if (isRelativeUnit.execute(unitxy.getDataAt(0), 0)) {
                        double nullUnitValue = pureNullUnitValue((RAbstractContainer) unitxy.getDataAt(0), 0);
                        result = evaluateNullUnit(nullUnitValue, vpTransform.size.getWidth(), nullLMode, nullAMode);
                    } else {
                        throw RInternalError.unimplemented("GrobUnitToInches from unit.c: 610");
                    }
                    break;
                default:
                    // should still be GROB_SOMETHING unit
                    if (isRelativeUnit.execute(unitxy.getDataAt(0), 0)) {
                        // Note: GnuR uses equivalent of vpTransform.size.getWidth() even for
                        // GROBHEIGHT, bug?
                        double nullUnitValue = pureNullUnitValue((RAbstractContainer) unitxy.getDataAt(0), 0);
                        result = evaluateNullUnit(nullUnitValue, vpTransform.size.getWidth(), nullLMode, nullAMode);
                    } else {
                        UnitConversionContext conversionCtx = new UnitConversionContext(vpTransform.size, vpContext, GPar.asDrawingContext(currentGP));
                        initUnitToInchesNode();
                        if (unitId == GROBWIDTH) {
                            result = unitToInchesNode.convertWidth((RAbstractContainer) unitxy.getDataAt(0), 0, conversionCtx);
                        } else {
                            // Note: GnuR uses height transform for both grobascent, grobdescent and
                            // for height
                            result = unitToInchesNode.convertHeight((RAbstractContainer) unitxy.getDataAt(0), 0, conversionCtx);
                        }
                    }
                    break;
            }

            postDrawCode.call(updatedGrob);
            ctx.getGridState().setGpar(savedGPar);
            ctx.getGridState().setCurrentGrob(savedGrob);
            return value * result;
        }

        private void initUnitToInchesNode() {
            if (unitToInchesNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                unitToInchesNode = UnitToInchesNode.create();
            }
        }
    }
}
