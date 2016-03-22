/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.profiles.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.printer.AnyVectorToStringVectorWriter;
import com.oracle.truffle.r.nodes.builtin.base.printer.ValuePrinterNode;
import com.oracle.truffle.r.nodes.builtin.base.printer.ValuePrinterNodeGen;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "format", kind = INTERNAL, parameterNames = {"x", "trim", "digits", "nsmall", "width", "justify", "na.encode", "scientific", "decimal.mark"})
public abstract class Format extends RBuiltinNode {

    @Child private CastIntegerNode castInteger;
    @Child protected ValuePrinterNode valuePrinter = ValuePrinterNodeGen.create(null, null, null, null, null, null, null, null, null);

    protected final BranchProfile errorProfile = BranchProfile.create();

    public static final int R_MAX_DIGITS_OPT = 22;
    public static final int R_MIN_DIGITS_OPT = 0;

    private static Config printConfig;

    public static Config setPrintDefaults() {
        if (printConfig == null) {
            printConfig = new Config();
        }
        printConfig.width = RContext.getInstance().getConsoleHandler().getWidth();
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

    public static Config getConfig() {
        return setPrintDefaults();
    }

    private RAbstractIntVector castInteger(Object operand) {
        if (castInteger == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castInteger = insert(CastIntegerNodeGen.create(true, false, false));
        }
        return (RAbstractIntVector) castInteger.execute(operand);
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toVector(1).toLogical(1);
        casts.toVector(2).toInteger(2);
        casts.toVector(3).toInteger(3);
        casts.toVector(4).toInteger(4);
        casts.toVector(5).toInteger(5);
        casts.toVector(6).toLogical(6);
        casts.toVector(7);
    }

    @Specialization
    protected RStringVector format(RAbstractLogicalVector value, RLogicalVector trimVec, RIntVector digitsVec, RIntVector nsmallVec, RIntVector widthVec, RIntVector justifyVec,
                    RLogicalVector naEncodeVec, RAbstractVector sciVec, RAbstractStringVector decimalMark) {
        checkArgs(trimVec, digitsVec, nsmallVec, widthVec, justifyVec, naEncodeVec, sciVec, decimalMark);
        return (RStringVector) valuePrinter.prettyPrint(value, AnyVectorToStringVectorWriter::new);
    }

    @Specialization
    protected RStringVector format(RAbstractIntVector value, RLogicalVector trimVec, RIntVector digitsVec, RIntVector nsmallVec, RIntVector widthVec, RIntVector justifyVec,
                    RLogicalVector naEncodeVec, RAbstractVector sciVec, RAbstractStringVector decimalMark) {
        checkArgs(trimVec, digitsVec, nsmallVec, widthVec, justifyVec, naEncodeVec, sciVec, decimalMark);
        return (RStringVector) valuePrinter.prettyPrint(value, AnyVectorToStringVectorWriter::new);
    }

    // TODO: even though format's arguments are not used at this point, their processing mirrors
    // what GNU R does

    private int computeSciArg(RAbstractVector sciVec) {
        assert sciVec.getLength() > 0;
        int tmp = castInteger(sciVec).getDataAt(0);
        int ret;
        if (sciVec.getElementClass() == RLogical.class) {
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
    protected RStringVector format(RAbstractDoubleVector value, RLogicalVector trimVec, RIntVector digitsVec, RIntVector nsmallVec, RIntVector widthVec, RIntVector justifyVec,
                    RLogicalVector naEncodeVec, RAbstractVector sciVec, RAbstractStringVector decimalMark) {
        checkArgs(trimVec, digitsVec, nsmallVec, widthVec, justifyVec, naEncodeVec, sciVec, decimalMark);
        return (RStringVector) valuePrinter.prettyPrint(value, AnyVectorToStringVectorWriter::new);
    }

    @Specialization
    protected RStringVector format(RAbstractComplexVector value, RLogicalVector trimVec, RIntVector digitsVec, RIntVector nsmallVec, RIntVector widthVec, RIntVector justifyVec,
                    RLogicalVector naEncodeVec, RAbstractVector sciVec, RAbstractStringVector decimalMark) {
        checkArgs(trimVec, digitsVec, nsmallVec, widthVec, justifyVec, naEncodeVec, sciVec, decimalMark);
        return (RStringVector) valuePrinter.prettyPrint(value, AnyVectorToStringVectorWriter::new);
    }

    @SuppressWarnings("unused")
    private void processArguments(RLogicalVector trimVec, RIntVector digitsVec, RIntVector nsmallVec, RIntVector widthVec, RIntVector justifyVec, RLogicalVector naEncodeVec, RAbstractVector sciVec,
                    RAbstractStringVector decimalMark) {
        byte trim = trimVec.getLength() > 0 ? trimVec.getDataAt(0) : RRuntime.LOGICAL_NA;
        int digits = digitsVec.getLength() > 0 ? digitsVec.getDataAt(0) : RRuntime.INT_NA;
        getConfig().digits = digits;
        int nsmall = nsmallVec.getLength() > 0 ? nsmallVec.getDataAt(0) : RRuntime.INT_NA;
        int width = widthVec.getLength() > 0 ? widthVec.getDataAt(0) : 0;
        int justify = justifyVec.getLength() > 0 ? justifyVec.getDataAt(0) : RRuntime.INT_NA;
        byte naEncode = naEncodeVec.getLength() > 0 ? naEncodeVec.getDataAt(0) : RRuntime.LOGICAL_NA;
        int sci = computeSciArg(sciVec);
        String myOutDec = decimalMark.getDataAt(0);
        if (RRuntime.isNA(myOutDec)) {
            myOutDec = ".";
        }
    }

    @Specialization
    protected RStringVector format(RAbstractStringVector value, RLogicalVector trimVec, RIntVector digitsVec, RIntVector nsmallVec, RIntVector widthVec, RIntVector justifyVec,
                    RLogicalVector naEncodeVec, RAbstractVector sciVec, RAbstractStringVector decimalMark) {
        checkArgs(trimVec, digitsVec, nsmallVec, widthVec, justifyVec, naEncodeVec, sciVec, decimalMark);
        // TODO: implement full semantics
        return value.materialize();
    }

    @Specialization
    protected RStringVector format(RFactor value, RLogicalVector trimVec, RIntVector digitsVec, RIntVector nsmallVec, RIntVector widthVec, RIntVector justifyVec, RLogicalVector naEncodeVec,
                    RAbstractVector sciVec, RAbstractStringVector decimalMark) {
        return format(value.getVector(), trimVec, digitsVec, nsmallVec, widthVec, justifyVec, naEncodeVec, sciVec, decimalMark);
    }

    // TruffleDSL bug - should not need multiple guards here
    protected void checkArgs(RLogicalVector trimVec, RIntVector digitsVec, RIntVector nsmallVec, RIntVector widthVec, RIntVector justifyVec, RLogicalVector naEncodeVec, RAbstractVector sciVec,
                    RAbstractStringVector decimalMark) {
        if (trimVec.getLength() > 0 && RRuntime.isNA(trimVec.getDataAt(0))) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "trim");
        }
        if (digitsVec.getLength() > 0 && (RRuntime.isNA(digitsVec.getDataAt(0)) || digitsVec.getDataAt(0) < R_MIN_DIGITS_OPT || digitsVec.getDataAt(0) > R_MAX_DIGITS_OPT)) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "digits");
        }
        if (nsmallVec.getLength() > 0 && (RRuntime.isNA(nsmallVec.getDataAt(0)) || nsmallVec.getDataAt(0) < 0 || nsmallVec.getDataAt(0) > 20)) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "nsmall");
        }
        if (widthVec.getLength() > 0 && RRuntime.isNA(widthVec.getDataAt(0))) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "width");
        }
        if (justifyVec.getLength() > 0 && (RRuntime.isNA(justifyVec.getDataAt(0)) || justifyVec.getDataAt(0) < 0 || nsmallVec.getDataAt(0) > 3)) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "justify");
        }
        if (naEncodeVec.getLength() > 0 && RRuntime.isNA(naEncodeVec.getDataAt(0))) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "na.encode");
        }
        if (sciVec.getLength() != 1 || (sciVec.getElementClass() != RLogical.class && sciVec.getElementClass() != RInteger.class && sciVec.getElementClass() != RDouble.class)) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "scientific");
        }
        if (decimalMark.getLength() != 1) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "decmial.mark");
        }
    }

    public static class Config {
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

    public enum Adjustment {
        LEFT,
        RIGHT,
        CENTRE,
        NONE;
    }
}
