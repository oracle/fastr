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
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asDoubleVector;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asIntVector;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asList;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asListOrNull;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.fmax;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.fmin;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.getDataAtMod;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.getDoubleAt;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.hasRClass;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.sum;
import static com.oracle.truffle.r.library.fastrGrid.device.DrawingContext.INCH_TO_POINTS_FACTOR;

import java.util.function.BiFunction;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.library.fastrGrid.UnitFactory.UnitLengthNodeGen;
import com.oracle.truffle.r.library.fastrGrid.UnitFactory.UnitToInchesNodeGen;
import com.oracle.truffle.r.library.fastrGrid.ViewPortTransform.GetViewPortTransformNode;
import com.oracle.truffle.r.library.fastrGrid.device.DrawingContext;
import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.nodes.helpers.InheritsCheckNode;
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
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Note: internally in FastR Grid everything is in inches. However, some lists that are exposed to
 * the R code should contain values in centimeters, we convert such values immediately once they
 * enter our system.
 */
public final class Unit {
    static final String VALID_UNIT_ATTR = "valid.unit";

    public static final int NPC = 0;
    public static final int CM = 1;
    public static final int INCHES = 2;
    public static final int LINES = 3;
    public static final int NATIVE = 4;
    public static final int NULL = 5; /* only used in layout specifications */
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
     * LINES now means multiples of the line height. This is multiples of the font size.
     */
    public static final int CHAR = 18;
    public static final int GROBX = 19;
    public static final int GROBY = 20;
    public static final int GROBWIDTH = 21;
    public static final int GROBHEIGHT = 22;
    public static final int GROBASCENT = 23;
    public static final int GROBDESCENT = 24;
    private static final int LAST_NORMAL_UNIT = GROBDESCENT;
    /*
     * No longer used
     */
    public static final int MYLINES = 103;
    public static final int MYCHAR = 104;
    public static final int MYSTRINGWIDTH = 105;
    public static final int MYSTRINGHEIGHT = 106;

    // null layout arithmetic mode
    private static final int L_adding = 1;
    private static final int L_subtracting = 2;
    private static final int L_summing = 3;
    private static final int L_plain = 4;
    private static final int L_maximising = 5;
    private static final int L_minimising = 6;
    private static final int L_multiplying = 7;

    // attributes in the unit object and unit classes
    private static final String UNIT_ATTR_DATA = "data";
    private static final String UNIT_ATTR_UNIT_ID = "valid.unit";
    private static final String UNIT_CLASS = "unit";
    private static final String UNIT_ARITHMETIC_CLASS = "unit.arithmetic";
    private static final String UNIT_LIST_CLASS = "unit.list";

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
        result.setClassAttr(RDataFactory.createStringVectorFromScalar(UNIT_CLASS));
        result.setAttr(UNIT_ATTR_UNIT_ID, unitId);
        result.setAttr(UNIT_ATTR_DATA, RNull.instance);
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
            case PICAS:
                return value / 12 * INCH_TO_POINTS_FACTOR;
            case BIGPOINTS:
                return value * 72;
            case DIDA:
                return value / 1238 * 1157 * INCH_TO_POINTS_FACTOR;
            case CICERO:
                return value / 1238 * 1157 * INCH_TO_POINTS_FACTOR / 12;
            case SCALEDPOINTS:
                return value * 65536 * INCH_TO_POINTS_FACTOR;
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

    private static double convertToInches(double value, int index, int unitId, RList data, UnitConversionContext ctx, AxisOrDimension axisOrDim) {
        // Note: grob units are converted in a dedicated node
        double vpSize = ctx.getViewPortSize(axisOrDim);
        String str;
        String[] lines;
        double result = 0;
        switch (unitId) {
            case INCHES:
                return value;
            case NATIVE:
                double tmp = axisOrDim.isDimension() ? value : (value - ctx.getScaleMin(axisOrDim));
                return (tmp / (ctx.getScaleMax(axisOrDim) - ctx.getScaleMin(axisOrDim))) * vpSize;
            case NPC:
                return value * vpSize;
            case POINTS:
                return value / INCH_TO_POINTS_FACTOR;
            case CM:
                return value / CM_IN_INCH;
            case MM:
                return value / (CM_IN_INCH * 10);
            case PICAS:
                return (value * 12) / INCH_TO_POINTS_FACTOR;
            case BIGPOINTS:
                return value / 72;
            case DIDA:
                return value / 1157 * 1238 / INCH_TO_POINTS_FACTOR;
            case CICERO:
                return value * 12 / 1157 * 1238 / INCH_TO_POINTS_FACTOR;
            case SCALEDPOINTS:
                return value / 65536 / INCH_TO_POINTS_FACTOR;
            case CHAR:
            case MYCHAR:
                return (value * ctx.gpar.getDrawingContext(index).getFontSize()) / INCH_TO_POINTS_FACTOR;
            case LINES:
            case MYLINES:
                return (value * ctx.gpar.getDrawingContext(index).getFontSize() * ctx.gpar.getDrawingContext(index).getLineHeight()) / INCH_TO_POINTS_FACTOR;
            case STRINGWIDTH:
            case MYSTRINGWIDTH:
                str = RRuntime.asString(data.getDataAt(0));
                lines = str.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    result = Math.max(result, ctx.device.getStringWidth(ctx.gpar.getDrawingContext(index), lines[i]));
                }
                return value * result;
            case STRINGHEIGHT:
            case MYSTRINGHEIGHT:
                str = RRuntime.asString(data.getDataAt(0));
                lines = str.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    result += ctx.device.getStringHeight(ctx.gpar.getDrawingContext(index), lines[i]);
                }
                return value * result;
            case NULL:
                return evaluateNullUnit(value, vpSize, ctx.nullLayoutMode, ctx.nullArithmeticMode);
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
        return unit instanceof RList && hasRClass((RAttributable) unit, UNIT_LIST_CLASS);
    }

    static boolean isArithmeticUnit(Object unit) {
        return unit instanceof RList && hasRClass((RAttributable) unit, UNIT_ARITHMETIC_CLASS);
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

    public static boolean isSimpleUnit(RAbstractContainer unit) {
        RStringVector classAttr = unit.getClassAttr();
        if (classAttr == null || classAttr.getLength() == 0) {
            return true;
        }
        for (int i = 0; i < classAttr.getLength(); i++) {
            String x = classAttr.getDataAt(i);
            if (Unit.UNIT_ARITHMETIC_CLASS.equals(x) || Unit.UNIT_LIST_CLASS.equals(x)) {
                return false;
            }
        }
        return true;
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

    private abstract static class UnitVisitor<T> {
        public T visit(RAbstractContainer unit) {
            RStringVector clazz = unit.getClassAttr();
            if (clazz == null || clazz.getLength() == 0) {
                return visitSimpleUnit((RAbstractVector) unit);
            }
            for (int i = 0; i < clazz.getLength(); i++) {
                String className = clazz.getDataAt(i);
                if (UNIT_ARITHMETIC_CLASS.equals(className)) {
                    return visitArithmeticUnit(ArithmeticUnit.asArithmeticUnit(asList(unit)));
                }
                if (UNIT_LIST_CLASS.equals(className)) {
                    return visitListUnit(asList(unit));
                }
            }
            return visitSimpleUnit((RAbstractVector) unit);
        }

        protected abstract T visitListUnit(RList unit);

        protected abstract T visitArithmeticUnit(ArithmeticUnit unit);

        protected abstract T visitSimpleUnit(RAbstractVector unit);
    }

    private static final class UnitLengthVisitor extends UnitVisitor<Integer> {
        private static final UnitLengthVisitor INSTANCE = new UnitLengthVisitor();

        @Override
        protected Integer visitListUnit(RList unit) {
            return unit.getLength();
        }

        @Override
        protected Integer visitArithmeticUnit(ArithmeticUnit unit) {
            // length of aggregate functions, max, min, etc. is 1
            return unit.isBinary() ? Math.max(visit(unit.arg1), visit(unit.arg2)) : 1;
        }

        @Override
        protected Integer visitSimpleUnit(RAbstractVector unit) {
            return unit.getLength();
        }
    }

    public static int getLength(RAbstractContainer unit) {
        return UnitLengthVisitor.INSTANCE.visit(unit);
    }

    abstract static class UnitNodeBase extends RBaseNode {
        @Child private InheritsCheckNode inheritsArithmeticCheckNode = new InheritsCheckNode(UNIT_ARITHMETIC_CLASS);
        @Child private InheritsCheckNode inheritsUnitListCheckNode = new InheritsCheckNode(UNIT_LIST_CLASS);

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
     * Used to discriminate between x axis, y axis, width, and height when doing unit conversions.
     * The order should be the same as used in e.g. {@code L_convert}, which is 0 means x, 1 means
     * y, 2 means width, 3 means height.
     */
    public enum AxisOrDimension {
        X,
        Y,
        WIDTH,
        HEIGHT;

        private static final AxisOrDimension[] enumValues = values();

        static AxisOrDimension fromInt(int value) {
            assert value >= 0 && value < 4;
            return enumValues[value];
        }

        public boolean isHorizontal() {
            return this == X || this == WIDTH;
        }

        public boolean isDimension() {
            return this == WIDTH || this == HEIGHT;
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
        public final GPar gpar;
        public final GridDevice device;
        public final int nullLayoutMode;
        public final int nullArithmeticMode;

        public UnitConversionContext(Size viewPortSize, ViewPortContext viewPortContext, GridDevice device, GPar gpar) {
            this(viewPortSize, viewPortContext, device, gpar, 0, 0);
        }

        public UnitConversionContext(Size viewPortSize, ViewPortContext viewPortContext, GridDevice device, GPar gpar, int nullLMode, int nullAMode) {
            this.viewPortSize = viewPortSize;
            this.viewPortContext = viewPortContext;
            this.device = device;
            this.gpar = gpar;
            this.nullLayoutMode = nullLMode;
            this.nullArithmeticMode = nullAMode;
        }

        public double getViewPortSize(AxisOrDimension forAxisOrDim) {
            return forAxisOrDim.isHorizontal() ? viewPortSize.getWidth() : viewPortSize.getHeight();
        }

        public double getScaleMin(AxisOrDimension forAxisOrDim) {
            return forAxisOrDim.isHorizontal() ? viewPortContext.xscalemin : viewPortContext.yscalemin;
        }

        public double getScaleMax(AxisOrDimension forAxisOrDim) {
            return forAxisOrDim.isHorizontal() ? viewPortContext.xscalemax : viewPortContext.yscalemax;
        }
    }

    /**
     * Normalizes grid unit object to a double value in inches. For convenience the index is
     * interpreted as cyclic.
     */
    public abstract static class UnitToInchesNode extends UnitNodeBase {
        @Child GrobUnitToInches grobUnitToInches;

        public static UnitToInchesNode create() {
            return UnitToInchesNodeGen.create();
        }

        public double convertX(RAbstractContainer vector, int index, UnitConversionContext ctx) {
            return execute(vector, index, ctx, AxisOrDimension.X);
        }

        public double convertY(RAbstractContainer vector, int index, UnitConversionContext ctx) {
            return execute(vector, index, ctx, AxisOrDimension.Y);
        }

        public double convertWidth(RAbstractContainer vector, int index, UnitConversionContext ctx) {
            return execute(vector, index, ctx, AxisOrDimension.WIDTH);
        }

        public double convertHeight(RAbstractContainer vector, int index, UnitConversionContext ctx) {
            return execute(vector, index, ctx, AxisOrDimension.HEIGHT);
        }

        public double convertDimension(RAbstractContainer vector, int index, UnitConversionContext ctx, boolean isWidth) {
            return isWidth ? convertWidth(vector, index, ctx) : convertHeight(vector, index, ctx);
        }

        public abstract double execute(RAbstractContainer vector, int index, UnitConversionContext ctx, AxisOrDimension axisOrDim);

        @Specialization(guards = "isSimple(value)")
        double doNormal(RAbstractVector value, int index, UnitConversionContext ctx, AxisOrDimension axisOrDim) {
            int unitId = getDataAtMod(asIntVector(value.getAttr(UNIT_ATTR_UNIT_ID)), index);
            double scalarValue = getDoubleAt(value, index % value.getLength());
            if (isGrobUnit(unitId)) {
                RList grobList = asList(value.getAttr(UNIT_ATTR_DATA));
                return getGrobUnitToInchesNode().execute(scalarValue, unitId, grobList.getDataAt(index % grobList.getLength()), ctx);
            }
            return convertToInches(scalarValue, index, unitId, asListOrNull(value.getAttr(UNIT_ATTR_DATA)), ctx, axisOrDim);
        }

        @Specialization(guards = "isUnitList(value)")
        double doList(RList value, int index, UnitConversionContext ctx, AxisOrDimension axisOrDim,
                        @Cached("create()") UnitToInchesNode recursiveNode) {
            Object unwrapped = value.getDataAt(index % value.getLength());
            if (unwrapped instanceof RAbstractVector) {
                return recursiveNode.execute((RAbstractContainer) unwrapped, 0, ctx, axisOrDim);
            }
            throw error(Message.GENERIC, "Unexpected unit list with non-vector like element at index " + index);
        }

        @Specialization(guards = "isArithmetic(list)")
        double doArithmetic(RList list, int index, UnitConversionContext ctx, AxisOrDimension axisOrDim,
                        @Cached("create()") UnitLengthNode unitLengthNode,
                        @Cached("create()") UnitToInchesNode recursiveNode) {
            ArithmeticUnit expr = ArithmeticUnit.asArithmeticUnit(list);
            BiFunction<RAbstractContainer, Integer, Double> recursive = (x, newNullAMode) -> recursiveNode.execute(x, index, getNewCtx(ctx, axisOrDim, newNullAMode), axisOrDim);
            switch (expr.op) {
                case "+":
                    return recursive.apply(expr.arg1, L_adding) + recursive.apply(expr.arg2, L_adding);
                case "-":
                    return recursive.apply(expr.arg1, L_subtracting) - recursive.apply(expr.arg2, L_subtracting);
                case "*":
                    RAbstractDoubleVector left = asDoubleVector(expr.arg1);
                    return left.getDataAt(index % left.getLength()) * recursive.apply(expr.arg2, L_multiplying);
                default:
                    break;
            }

            // must be aggregate operation
            UnitConversionContext newCtx = getNewCtx(ctx, axisOrDim, getNullAMode(expr.op));
            int len = unitLengthNode.execute(expr.arg1);
            double[] values = new double[len];
            for (int i = 0; i < len; i++) {
                values[i] = recursiveNode.execute(expr.arg1, i, newCtx, axisOrDim);
            }

            switch (expr.op) {
                case "min":
                    return GridUtils.fmin(Double.MAX_VALUE, values);
                case "max":
                    return GridUtils.fmax(Double.MIN_VALUE, values);
                case "sum":
                    return GridUtils.sum(values);
                default:
                    throw RInternalError.shouldNotReachHere("The operation should have been validated in asArithmeticUnit method.");
            }
        }

        // Note the catch: newNullAMode is applied only if the axisOrDim is dimension
        private static UnitConversionContext getNewCtx(UnitConversionContext ctx, AxisOrDimension axisOrDim, int newNullAMode) {
            return new UnitConversionContext(ctx.viewPortSize, ctx.viewPortContext, ctx.device, ctx.gpar, ctx.nullLayoutMode,
                            axisOrDim.isDimension() ? newNullAMode : ctx.nullArithmeticMode);
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

        private GrobUnitToInches getGrobUnitToInchesNode() {
            if (grobUnitToInches == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                grobUnitToInches = insert(new GrobUnitToInches());
            }
            return grobUnitToInches;
        }
    }

    public static final class GrobUnitToInches extends Node {
        @Child private RGridCodeCall preDrawCode = new RGridCodeCall("grobConversionPreDraw");
        @Child private RGridCodeCall getUnitXY = new RGridCodeCall("grobConversionGetUnitXY");
        @Child private RGridCodeCall postDrawCode = new RGridCodeCall("grobConversionPostDraw");
        @Child private GetViewPortTransformNode getViewPortTransform = new GetViewPortTransformNode();

        @Child private IsRelativeUnitNode isRelativeUnit = new IsRelativeUnitNode();
        @Child private UnitToInchesNode unitToInchesNode;

        // transcribed from unit.c function evaluateGrobUnit

        public double execute(double value, int unitId, Object grob, UnitConversionContext conversionCtx) {
            GridContext ctx = GridContext.getContext();
            RList currentVP = ctx.getGridState().getViewPort();
            getViewPortTransform.execute(currentVP, conversionCtx.device);

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
            ViewPortTransform vpTransform = getViewPortTransform.execute(currentVP, conversionCtx.device);
            ViewPortContext vpContext = ViewPortContext.fromViewPort(currentVP);

            // getUnitXY returns a list with either one or two items
            RList unitxy = (RList) getUnitXY.execute(new RArgsValuesAndNames(new Object[]{updatedGrob, unitId}, ArgumentsSignature.empty(2)));
            double result;
            switch (unitId) {
                case GROBX:
                case GROBY:
                    if (unitId == GROBY && isRelativeUnit.execute(unitxy.getDataAt(1), 0)) {
                        double nullUnitValue = pureNullUnitValue((RAbstractContainer) unitxy.getDataAt(1), 0);
                        result = evaluateNullUnit(nullUnitValue, vpTransform.size.getHeight(), conversionCtx.nullLayoutMode, conversionCtx.nullArithmeticMode);
                    } else if (isRelativeUnit.execute(unitxy.getDataAt(0), 0)) {
                        double nullUnitValue = pureNullUnitValue((RAbstractContainer) unitxy.getDataAt(0), 0);
                        result = evaluateNullUnit(nullUnitValue, vpTransform.size.getWidth(), conversionCtx.nullLayoutMode, conversionCtx.nullArithmeticMode);
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
                        result = evaluateNullUnit(nullUnitValue, vpTransform.size.getWidth(), conversionCtx.nullLayoutMode, conversionCtx.nullArithmeticMode);
                    } else {
                        UnitConversionContext newConversionCtx = new UnitConversionContext(vpTransform.size, vpContext, conversionCtx.device, GPar.create(currentGP));
                        initUnitToInchesNode();
                        if (unitId == GROBWIDTH) {
                            result = unitToInchesNode.convertWidth((RAbstractContainer) unitxy.getDataAt(0), 0, newConversionCtx);
                        } else {
                            // Note: GnuR uses height transform for both grobascent, grobdescent and
                            // for height
                            result = unitToInchesNode.convertHeight((RAbstractContainer) unitxy.getDataAt(0), 0, newConversionCtx);
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
