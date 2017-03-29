/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.intNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.printer.AnyVectorToStringVectorWriter;
import com.oracle.truffle.r.nodes.builtin.base.printer.ComplexVectorPrinter;
import com.oracle.truffle.r.nodes.builtin.base.printer.DoubleVectorMetrics;
import com.oracle.truffle.r.nodes.builtin.base.printer.DoubleVectorPrinter;
import com.oracle.truffle.r.nodes.builtin.base.printer.IntegerVectorPrinter;
import com.oracle.truffle.r.nodes.builtin.base.printer.LogicalVectorPrinter;
import com.oracle.truffle.r.nodes.builtin.base.printer.PrintParameters;
import com.oracle.truffle.r.nodes.builtin.base.printer.ValuePrinterNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNode;
import com.oracle.truffle.r.nodes.unary.CastIntegerNodeGen;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

@SuppressWarnings("unused")
@RBuiltin(name = "format", kind = INTERNAL, parameterNames = {"x", "trim", "digits", "nsmall", "width", "justify", "na.encode", "scientific", "decimal.mark"}, behavior = PURE)
public abstract class Format extends RBuiltinNode {

    @Child private CastIntegerNode castInteger;
    @Child protected ValuePrinterNode valuePrinter = new ValuePrinterNode();

    public static final int R_MIN_DIGITS_OPT = 0;
    public static final int R_MAX_DIGITS_OPT = 22;

    public static final int JUSTIFY_LEFT = 0;
    public static final int JUSTIFY_RIGHT = 1;
    public static final int JUSTIFY_CENTER = 2;
    public static final int JUSTIFY_NONE = 3;

    private RAbstractIntVector castInteger(Object operand) {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInteger = insert(CastIntegerNodeGen.create(true, false, false));
        }
        return (RAbstractIntVector) castInteger.execute(operand);
    }

    static {
        Casts casts = new Casts(Format.class);
        casts.arg("x");
        casts.arg("trim").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).mustNotBeNA().map(toBoolean());
        casts.arg("digits").asIntegerVector().findFirst(RRuntime.INT_NA).mustBe(intNA().or(gte(R_MIN_DIGITS_OPT).and(lte(R_MAX_DIGITS_OPT))));
        casts.arg("nsmall").asIntegerVector().findFirst(RRuntime.INT_NA).mustBe(intNA().or(gte(0).and(lte(20))));
        casts.arg("width").asIntegerVector().findFirst(0).mustNotBeNA();
        casts.arg("justify").asIntegerVector().findFirst(RRuntime.INT_NA).mustBe(intNA().or(gte(JUSTIFY_LEFT).and(lte(JUSTIFY_NONE))));
        casts.arg("na.encode").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).mustNotBeNA().map(toBoolean());
        casts.arg("scientific").asIntegerVector().findFirst();
        casts.arg("decimal.mark").asStringVector().findFirst();
    }

    private static char getDecimalMark(String decimalMark) {
        return decimalMark.length() == 0 ? '.' : decimalMark.charAt(0);
    }

    private static PrintParameters getParameters(int digits, int scientific) {
        PrintParameters pp = new PrintParameters();
        pp.setDefaults();
        if (!RRuntime.isNA(digits)) {
            pp.setDigits(digits);
        }
        if (!RRuntime.isNA(scientific)) {
            pp.setScipen(scientific);
        }
        return pp;
    }

    private static RStringVector createResult(RAbstractVector value, String[] array) {
        RStringVector result = RDataFactory.createStringVector(array, value.isComplete(), value.getDimensions(), value.getNames());
        result.setDimNames(value.getDimNames());
        return result;
    }

    @Specialization
    @TruffleBoundary
    protected RStringVector format(RAbstractLogicalVector value, boolean trim, int digits, int nsmall, int width, int justify, boolean naEncode, int scientific, String decimalMark) {
        String[] result = LogicalVectorPrinter.format(value, trim, width, getParameters(digits, scientific));
        return createResult(value, result);
    }

    @Specialization
    @TruffleBoundary
    protected RStringVector format(RAbstractIntVector value, boolean trim, int digits, int nsmall, int width, int justify, boolean naEncode, int scientific, String decimalMark) {
        String[] result = IntegerVectorPrinter.format(value, trim, width, getParameters(digits, scientific));
        return createResult(value, result);
    }

    @Specialization
    @TruffleBoundary
    protected RStringVector format(RAbstractDoubleVector value, boolean trim, int digits, int nsmall, int width, int justify, boolean naEncode, int scientific, String decimalMark) {
        String[] result = DoubleVectorPrinter.format(value, trim, nsmall, width, getDecimalMark(decimalMark), getParameters(digits, scientific));
        return createResult(value, result);
    }

    @Specialization
    @TruffleBoundary
    protected RStringVector format(RAbstractComplexVector value, boolean trim, int digits, int nsmall, int width, int justify, boolean naEncode, int scientific, String decimalMark) {
        String[] result = ComplexVectorPrinter.format(value, trim, nsmall, width, getDecimalMark(decimalMark), getParameters(digits, scientific));
        return createResult(value, result);
    }

    @Specialization
    protected RStringVector format(REnvironment value, boolean trim, int digits, int nsmall, int width, int justify, boolean naEncode, int scientific, String decimalMark) {
        return RDataFactory.createStringVector(value.getPrintName());
    }

    @Specialization
    @TruffleBoundary
    protected RStringVector format(RAbstractStringVector value, boolean trim, int digits, int nsmall, int width, int justify, boolean naEncode, int scientific, String decimalMark) {
        PrintParameters pp = getParameters(digits, scientific);
        int w;
        if (justify == JUSTIFY_NONE) {
            w = 0;
        } else {
            w = width;
            for (int i = 0; i < value.getLength(); i++) {
                String element = value.getDataAt(i);
                if (RRuntime.isNA(element)) {
                    if (naEncode) {
                        w = Math.max(w, pp.getNaWidth());
                    }
                } else {
                    w = Math.max(w, element.length());
                }
            }
        }
        String[] result = new String[value.getLength()];
        for (int i = 0; i < value.getLength(); i++) {
            String element = value.getDataAt(i);
            if (!naEncode && RRuntime.isNA(element)) {
                result[i] = RRuntime.STRING_NA;
            } else {
                String s0;
                if (RRuntime.isNA(element)) {
                    element = pp.getNaString();
                }
                int il = element.length();
                int b = w - il;
                StringBuilder str = new StringBuilder(Math.max(w, il));
                if (b > 0 && justify != JUSTIFY_LEFT) {
                    int b0 = (justify == JUSTIFY_CENTER) ? b / 2 : b;
                    for (int j = 0; j < b0; j++) {
                        str.append(' ');
                    }
                    b -= b0;
                }
                str.append(element);
                if (b > 0 && justify != JUSTIFY_RIGHT) {
                    for (int j = 0; j < b; j++) {
                        str.append(' ');
                    }
                }
                result[i] = str.toString();
            }
        }
        return createResult(value, result);
    }
}
