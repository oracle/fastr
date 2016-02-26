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

//Transcribed from GnuR, src/include/Print.h

import com.oracle.truffle.r.nodes.builtin.base.Format;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RString;

public class PrintParameters {
    private int width;
    private int na_width;
    private int na_width_noquote;
    private int digits;
    private int scipen;
    private int gap;
    private boolean quote;
    private boolean right;
    private int max;
    private String na_string;
    private String na_string_noquote;
    private boolean useSource;
    private int cutoff; // for deparsed language objects

    public PrintParameters() {
    }

    public PrintParameters(Object digits, boolean quote, Object naPrint,
                    Object printGap, boolean right, Object max, boolean useSource, boolean noOpt) {

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
            // throw new IllegalArgumentException(String.format("invalid 'na.print'
            // specification"));
            String nav = naPrint.toString();
            if (!"".equals(nav)) {
                this.na_string = this.na_string_noquote = ((RString) naPrint).getValue();
                this.na_width = this.na_width_noquote = this.na_string.length();
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

    public PrintParameters cloneParameters() {
        PrintParameters cloned = new PrintParameters();
        cloned.na_string = this.na_string;
        cloned.na_string_noquote = this.na_string_noquote;
        cloned.na_width = this.na_width;
        cloned.na_width_noquote = this.na_string_noquote.length();
        cloned.quote = this.quote;
        cloned.right = this.right;
        cloned.digits = this.digits;
        cloned.scipen = this.scipen;
        cloned.max = this.max;
        cloned.gap = this.gap;
        cloned.width = this.width;
        cloned.useSource = this.useSource;
        cloned.cutoff = this.cutoff;
        return cloned;
    }

    private void setDefaults() {
        this.na_string = RRuntime.STRING_NA;
        this.na_string_noquote = "<NA>";
        this.na_width = this.na_string.length();
        this.na_width_noquote = this.na_string_noquote.length();
        this.quote = true;
        this.right = false;
        this.digits = RRuntime.asInteger(RContext.getInstance().stateROptions.getValue("digits"));
        this.scipen = RRuntime.asInteger(RContext.getInstance().stateROptions.getValue("scipen"));
        if (this.scipen == RRuntime.INT_NA) {
            this.scipen = 0;
        }
        this.max = RRuntime.asInteger(RContext.getInstance().stateROptions.getValue("max.print"));
        if (this.max == RRuntime.INT_NA || this.max < 0) {
            this.max = 99999;
        } else if (this.max == RRuntime.INT_NA) {
            this.max--; // so we can add
        }
        this.gap = 1;
        this.width = RRuntime.asInteger(RContext.getInstance().stateROptions.getValue("width"));
        this.useSource = true;
        this.cutoff = RRuntime.asInteger(RContext.getInstance().stateROptions.getValue("deparse.cutoff"));
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getNa_width() {
        return na_width;
    }

    public void setNa_width(int na_width) {
        this.na_width = na_width;
    }

    public int getNa_width_noquote() {
        return na_width_noquote;
    }

    public void setNa_width_noquote(int na_width_noquote) {
        this.na_width_noquote = na_width_noquote;
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

    public String getNa_string() {
        return na_string;
    }

    public void setNa_string(String na_string) {
        this.na_string = na_string;
    }

    public String getNa_string_noquote() {
        return na_string_noquote;
    }

    public void setNa_string_noquote(String na_string_noquote) {
        this.na_string_noquote = na_string_noquote;
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

}
