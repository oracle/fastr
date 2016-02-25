/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.profiles.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.context.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "format", kind = INTERNAL, parameterNames = {"x", "trim", "digits", "nsmall", "width", "justify", "na.encode", "scientific"})
public abstract class Format extends RBuiltinNode {

    @Child private CastIntegerNode castInteger;

    protected final BranchProfile errorProfile = BranchProfile.create();
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();
    /**
     * This is just a dummy object used to invoke getNames on RAbstractDoubleVector. The
     * RAbstractDoubleVector has no method for obtaining the names associated with the values in the
     * vector other than getNames(RAttributeProfiles).
     */
    private static final RAttributeProfiles dummyAttrProfiles = RAttributeProfiles.create();

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

    // TODO: handling of logical values has been derived from GNU R, with handling of other
    // types following suit at some point for compliance

    @TruffleBoundary
    private static RStringVector convertToString(RAbstractLogicalVector value) {
        int width = formatLogical(value);
        String[] data = new String[value.getLength()];
        for (int i = 0; i < data.length; i++) {
            data[i] = PrettyPrinterNode.prettyPrint(value.getDataAt(i), width);
        }
        // vector is complete because strings are created by string builder
        return RDataFactory.createStringVector(data, RDataFactory.COMPLETE_VECTOR);
    }

    @Specialization
    protected RStringVector format(RAbstractLogicalVector value, RLogicalVector trimVec, RIntVector digitsVec, RIntVector nsmallVec, RIntVector widthVec, RIntVector justifyVec,
                    RLogicalVector naEncodeVec, RAbstractVector sciVec) {
        checkArgs(trimVec, digitsVec, nsmallVec, widthVec, justifyVec, naEncodeVec, sciVec);
        if (value.getLength() == 0) {
            return RDataFactory.createEmptyStringVector();
        } else {
            return convertToString(value);
        }
    }

    private static int formatLogical(RAbstractLogicalVector value) {
        int width = 1;
        for (int i = 0; i < value.getLength(); i++) {
            byte val = value.getDataAt(i);
            if (RRuntime.isNA(val)) {
                width = getConfig().naWidth;
            } else if (val != RRuntime.LOGICAL_FALSE && width < 4) {
                width = 4;
            } else if (val == RRuntime.LOGICAL_FALSE && width < 5) {
                width = 5;
                break;
            }
        }
        return width;
    }

    // TODO: handling of other types re-uses our current formatting code in PrettyPrinterNode, which
    // should be sufficient for the time being but is likely not 100% compliant

    private static void addSpaces(String[] data, int width) {
        for (int i = 0; i < data.length; i++) {
            StringBuilder sb = new StringBuilder();
            data[i] = PrettyPrinterNode.spaces(sb, width - data[i].length()).append(data[i]).toString();
        }
    }

    @TruffleBoundary
    private static RStringVector convertToString(RAbstractIntVector value) {
        String[] data = new String[value.getLength()];
        int width = 0;
        int widthChanges = 0;
        boolean complete = RDataFactory.COMPLETE_VECTOR;
        for (int i = 0; i < data.length; i++) {
            data[i] = PrettyPrinterNode.prettyPrint(value.getDataAt(i));
            if (RRuntime.isNA(data[i])) {
                complete = RDataFactory.INCOMPLETE_VECTOR;
            }
            if (width < data[i].length()) {
                width = data[i].length();
                widthChanges++;
            }
        }
        if (widthChanges > 1) {
            addSpaces(data, width);
        }
        return RDataFactory.createStringVector(data, complete);
    }

    @Specialization
    protected RStringVector format(RAbstractIntVector value, RLogicalVector trimVec, RIntVector digitsVec, RIntVector nsmallVec, RIntVector widthVec, RIntVector justifyVec,
                    RLogicalVector naEncodeVec, RAbstractVector sciVec) {
        checkArgs(trimVec, digitsVec, nsmallVec, widthVec, justifyVec, naEncodeVec, sciVec);
        if (value.getLength() == 0) {
            return RDataFactory.createEmptyStringVector();
        } else {
            RStringVector result = convertToString(value);
            if (value.getDimensions() != null) {
                result.setDimensions(value.getDimensions());
                result.setDimNames(value.getDimNames(attrProfiles));
            } else {
                result.setNames(value.getNames(attrProfiles));
            }
            return result;
        }
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

    @TruffleBoundary
    private static RStringVector convertToString(RAbstractDoubleVector value) {
        String[] data = new String[value.getLength()];
        int width = 0;
        int widthChanges = 0;
        boolean complete = RDataFactory.COMPLETE_VECTOR;
        for (int i = 0; i < data.length; i++) {
            data[i] = PrettyPrinterNode.prettyPrint(value.getDataAt(i));
            if (RRuntime.isNA(data[i])) {
                complete = RDataFactory.INCOMPLETE_VECTOR;
            }
        }
        PrettyPrinterNode.padTrailingDecimalPointAndZeroesIfRequired(data);
        for (int i = 0; i < data.length; i++) {
            if (width < data[i].length()) {
                width = data[i].length();
                widthChanges++;
            }
        }
        if (widthChanges > 1) {
            addSpaces(data, width);
        }
        // vector is complete because strings are created by string builder
        return RDataFactory.createStringVector(data, complete, value.getNames(dummyAttrProfiles));
    }

    @Specialization
    protected RStringVector format(RAbstractDoubleVector value, RLogicalVector trimVec, RIntVector digitsVec, RIntVector nsmallVec, RIntVector widthVec, RIntVector justifyVec,
                    RLogicalVector naEncodeVec, RAbstractVector sciVec) {
        checkArgs(trimVec, digitsVec, nsmallVec, widthVec, justifyVec, naEncodeVec, sciVec);
        if (value.getLength() == 0) {
            return RDataFactory.createEmptyStringVector();
        } else {
            return convertToString(value);
        }
    }

    @SuppressWarnings("unused")
    private void processArguments(RLogicalVector trimVec, RIntVector digitsVec, RIntVector nsmallVec, RIntVector widthVec, RIntVector justifyVec, RLogicalVector naEncodeVec, RAbstractVector sciVec) {
        byte trim = trimVec.getLength() > 0 ? trimVec.getDataAt(0) : RRuntime.LOGICAL_NA;
        int digits = digitsVec.getLength() > 0 ? digitsVec.getDataAt(0) : RRuntime.INT_NA;
        getConfig().digits = digits;
        int nsmall = nsmallVec.getLength() > 0 ? nsmallVec.getDataAt(0) : RRuntime.INT_NA;
        int width = widthVec.getLength() > 0 ? widthVec.getDataAt(0) : 0;
        int justify = justifyVec.getLength() > 0 ? justifyVec.getDataAt(0) : RRuntime.INT_NA;
        byte naEncode = naEncodeVec.getLength() > 0 ? naEncodeVec.getDataAt(0) : RRuntime.LOGICAL_NA;
        int sci = computeSciArg(sciVec);
    }

    @Specialization
    protected RStringVector format(RAbstractStringVector value, RLogicalVector trimVec, RIntVector digitsVec, RIntVector nsmallVec, RIntVector widthVec, RIntVector justifyVec,
                    RLogicalVector naEncodeVec, RAbstractVector sciVec) {
        checkArgs(trimVec, digitsVec, nsmallVec, widthVec, justifyVec, naEncodeVec, sciVec);
        // TODO: implement full semantics
        return value.materialize();
    }

    @Specialization
    protected RStringVector format(RFactor value, RLogicalVector trimVec, RIntVector digitsVec, RIntVector nsmallVec, RIntVector widthVec, RIntVector justifyVec, RLogicalVector naEncodeVec,
                    RAbstractVector sciVec) {
        return format(value.getVector(), trimVec, digitsVec, nsmallVec, widthVec, justifyVec, naEncodeVec, sciVec);
    }

    // TruffleDSL bug - should not need multiple guards here
    protected void checkArgs(RLogicalVector trimVec, RIntVector digitsVec, RIntVector nsmallVec, RIntVector widthVec, RIntVector justifyVec, RLogicalVector naEncodeVec, RAbstractVector sciVec) {
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
