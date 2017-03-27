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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.printer.AnyVectorToStringVectorWriter;
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

    protected final BranchProfile errorProfile = BranchProfile.create();

    public static final int R_MAX_DIGITS_OPT = 22;
    public static final int R_MIN_DIGITS_OPT = 0;

    private static Config printConfig;

    private static Config setPrintDefaults() {
        if (printConfig == null) {
            printConfig = new Config();
        }
        printConfig.width = (int) RContext.getInstance().stateROptions.getValue("width");
        printConfig.naWidth = RRuntime.STRING_NA.length();
        printConfig.naWidthNoQuote = RRuntime.NA_HEADER.length();
        printConfig.digits = 7 /* default */;
        printConfig.scipen = 0 /* default */;
        printConfig.gap = 1;
        printConfig.quote = 1;
        printConfig.right = Adjustment.LEFT;
        printConfig.max = 99999 /* default */;
        printConfig.naString = RRuntime.STRING_NA;
        printConfig.naStringNoQuote = RRuntime.NA_HEADER;
        printConfig.useSource = 8 /* default */;
        printConfig.cutoff = 60;
        return printConfig;
    }

    private static Config getConfig() {
        return setPrintDefaults();
    }

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
        casts.arg("justify").asIntegerVector().findFirst(RRuntime.INT_NA).mustBe(intNA().or(gte(0).and(lte(3))));
        casts.arg("na.encode").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).mustNotBeNA().map(toBoolean());
        casts.arg("scientific").asIntegerVector().findFirst();
        casts.arg("decimal.mark").asStringVector().findFirst();
    }

    @Specialization
    protected RStringVector format(RAbstractLogicalVector value, boolean trim, int digits, int nsmall, int width, int justify, boolean naEncode, int scientific,
                    String decimalMark) {
        return (RStringVector) valuePrinter.prettyPrint(value, AnyVectorToStringVectorWriter::new);
    }

    @Specialization
    protected RStringVector format(RAbstractIntVector value, boolean trim, int digits, int nsmall, int width, int justify, boolean naEncode, int scientific,
                    String decimalMark) {
        return (RStringVector) valuePrinter.prettyPrint(value, AnyVectorToStringVectorWriter::new);
    }

    // TODO: even though format's arguments are not used at this point, their processing mirrors
    // what GNU R does

    private int computeSciArg(RAbstractVector sciVec) {
        assert sciVec.getLength() > 0;
        int tmp = castInteger(sciVec).getDataAt(0);
        int ret;
        if (sciVec instanceof RAbstractLogicalVector) {
            if (RRuntime.isNA(tmp)) {
                ret = tmp;
            } else {
                ret = tmp > 0 ? -100 : 100;
            }
        } else {
            ret = tmp;
        }
        if (!RRuntime.isNA(ret)) {
            getConfig().scipen = ret;
        }
        return ret;
    }

    @Specialization
    protected RStringVector format(RAbstractDoubleVector value, boolean trim, int digits, int nsmall, int width, int justify, boolean naEncode, int scientific,
                    String decimalMark) {
        return (RStringVector) valuePrinter.prettyPrint(value, AnyVectorToStringVectorWriter::new);
    }

    @Specialization
    protected RStringVector format(RAbstractComplexVector value, boolean trim, int digits, int nsmall, int width, int justify, boolean naEncode, int scientific,
                    String decimalMark) {
        return (RStringVector) valuePrinter.prettyPrint(value, AnyVectorToStringVectorWriter::new);
    }

    @Specialization
    protected RStringVector format(REnvironment value, boolean trim, int digits, int nsmall, int width, int justify, boolean naEncode, int scientific,
                    String decimalMark) {
        return RDataFactory.createStringVector(value.getPrintName());
    }

    @Specialization
    protected RStringVector format(RAbstractStringVector value, boolean trim, int digits, int nsmall, int width, int justify, boolean naEncode, int scientific, String decimalMark) {
        // TODO: implement full semantics
        return value.materialize();
    }

    private static class Config {
        public int width;
        public int naWidth;
        public int naWidthNoQuote;
        public int digits;
        public int scipen;
        public int gap;
        public int quote;
        public Adjustment right;
        public int max;
        public String naString;
        public String naStringNoQuote;
        public int useSource;
        public int cutoff;
    }

    private enum Adjustment {
        LEFT,
        RIGHT,
        CENTRE,
        NONE;
    }
}
