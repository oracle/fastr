/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2013,  The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base.printer;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

//Transcribed from GnuR, src/include/Print.h

import com.oracle.truffle.r.nodes.builtin.base.Format;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RString;

public final class PrintParameters {
    @CompilationFinal private static int DefaultDigits = -1;

    private int width;
    private int naWidth;
    private int naWidthNoquote;
    private int digits;
    private int scipen;
    private int gap;
    private boolean quote;
    private boolean right;
    private int max;
    private String naString;
    private String naStringNoquote;
    private boolean useSource;
    private int cutoff; // for deparsed language objects
    /**
     * If set to true the vector printer does not print the index labels ([i]). This parameter is
     * not subject to cloning so that it does not propagate to nested printers.
     */
    private boolean suppressIndexLabels;

    public static int getDefaultDigits() {
        if (DefaultDigits == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            DefaultDigits = RRuntime.asInteger(RContext.getInstance().stateROptions.getValue("digits"));
        }
        return DefaultDigits;
    }

    public PrintParameters() {
        // default constructor
    }

    @TruffleBoundary
    PrintParameters(Object digits, boolean quote, Object naPrint,
                    Object printGap, boolean right, Object max, boolean useSource, @SuppressWarnings("unused") boolean noOpt) {

        setDefaults();

        if (digits != RNull.instance) {
            this.digits = RRuntime.asInteger(digits);
            if (this.digits == RRuntime.INT_NA ||
                            this.digits < Format.R_MIN_DIGITS_OPT ||
                            this.digits > Format.R_MAX_DIGITS_OPT) {
                throw new IllegalArgumentException(String.format("invalid '%s' argument", "digits"));
            }
        }

        this.quote = quote;

        if (naPrint != RNull.instance) {
            // TODO: Although the original code in print.c at line 253 contains the following
            // condition, the GnuR application ignores that condition. It was revealed when running
            // test com.oracle.truffle.r.test.builtins.TestBuiltin_printdefault, which fails if the
            // condition is present complaining about an invalid na.print specification.
            // if (!(naPrint instanceof RString) || ((RString) naPrint).getValue().length() < 1)
            // throw new
            // IllegalArgumentException(String.format("invalid 'na.print' specification"));
            String nav = naPrint.toString();
            if (!"".equals(nav)) {
                this.naString = this.naStringNoquote = ((RString) naPrint).getValue();
                this.naWidth = this.naWidthNoquote = this.naString.length();
            }
        }

        if (printGap != RNull.instance) {
            this.gap = RRuntime.asInteger(printGap);
            if (this.gap == RRuntime.INT_NA || this.gap < 0) {
                throw new IllegalArgumentException(String.format("'gap' must be non-negative integer"));
            }
        }

        this.right = right;

        if (max != RNull.instance) {
            this.max = RRuntime.asInteger(max);
            if (this.max == RRuntime.INT_NA || this.max < 0) {
                throw new IllegalArgumentException(String.format("invalid '%s' argument", "max"));
            } else if (this.max == RRuntime.INT_MAX_VALUE) {
                this.max--; // so we can add
            }
        }

        this.useSource = useSource;
    }

    PrintParameters cloneParameters() {
        PrintParameters cloned = new PrintParameters();
        cloned.naString = this.naString;
        cloned.naStringNoquote = this.naStringNoquote;
        cloned.naWidth = this.naWidth;
        cloned.naWidthNoquote = this.naStringNoquote.length();
        cloned.quote = this.quote;
        cloned.right = this.right;
        cloned.digits = this.digits;
        cloned.scipen = this.scipen;
        cloned.max = this.max;
        cloned.gap = this.gap;
        cloned.width = this.width;
        cloned.useSource = this.useSource;
        cloned.cutoff = this.cutoff;
        // the suppression of index labels flag is not inherited
        cloned.suppressIndexLabels = false;
        return cloned;
    }

    public static int getDefaultMaxPrint() {
        int max = RRuntime.asInteger(RContext.getInstance().stateROptions.getValue("max.print"));
        if (max == RRuntime.INT_NA || max < 0) {
            max = 99999;
        } else if (max == RRuntime.INT_NA) {
            max--; // so we can add
        }
        return max;
    }

    public void setDefaults() {
        this.naString = RRuntime.STRING_NA;
        this.naStringNoquote = "<NA>";
        this.naWidth = this.naString.length();
        this.naWidthNoquote = this.naStringNoquote.length();
        this.quote = true;
        this.right = false;
        this.digits = getDefaultDigits();
        this.scipen = RRuntime.asInteger(RContext.getInstance().stateROptions.getValue("scipen"));
        if (this.scipen == RRuntime.INT_NA) {
            this.scipen = 0;
        }
        this.max = getDefaultMaxPrint();
        this.gap = 1;
        this.width = RRuntime.asInteger(RContext.getInstance().stateROptions.getValue("width"));
        this.useSource = true;
        this.cutoff = RRuntime.asInteger(RContext.getInstance().stateROptions.getValue("deparse.cutoff"));
        this.suppressIndexLabels = false;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getNaWidth() {
        return naWidth;
    }

    public void setNaWidth(int naWidth) {
        this.naWidth = naWidth;
    }

    public int getNaWidthNoquote() {
        return naWidthNoquote;
    }

    public void setNaWidthNoquote(int naWidthNoquote) {
        this.naWidthNoquote = naWidthNoquote;
    }

    public int getDigits() {
        return digits;
    }

    public void setDigits(int digits) {
        this.digits = digits;
    }

    public int getScipen() {
        return scipen;
    }

    public void setScipen(int scipen) {
        this.scipen = scipen;
    }

    public int getGap() {
        return gap;
    }

    public void setGap(int gap) {
        this.gap = gap;
    }

    public boolean getQuote() {
        return quote;
    }

    public void setQuote(boolean quote) {
        this.quote = quote;
    }

    public boolean getRight() {
        return right;
    }

    public void setRight(boolean right) {
        this.right = right;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public String getNaString() {
        return naString;
    }

    public void setNaString(String naString) {
        this.naString = naString;
    }

    public String getNaStringNoquote() {
        return naStringNoquote;
    }

    public void setNaStringNoquote(String naStringNoquote) {
        this.naStringNoquote = naStringNoquote;
    }

    public boolean getUseSource() {
        return useSource;
    }

    public void setUseSource(boolean useSource) {
        this.useSource = useSource;
    }

    public int getCutoff() {
        return cutoff;
    }

    public void setCutoff(int cutoff) {
        this.cutoff = cutoff;
    }

    public boolean getSuppressIndexLabels() {
        return suppressIndexLabels;
    }

    public void setSuppressIndexLabels(boolean suppressIndexLabels) {
        this.suppressIndexLabels = suppressIndexLabels;
    }
}
