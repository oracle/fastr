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

import java.io.IOException;
import java.io.PrintWriter;

import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributes;
import com.oracle.truffle.r.runtime.data.RAttributes.RAttribute;
import com.oracle.truffle.r.runtime.data.RDataFrame;

//Transcribed from GnuR, src/main/print.c

public class AttributesPrinter implements ValuePrinter<RAttributable> {

    public static final AttributesPrinter INSTANCE = new AttributesPrinter(false);

    private final boolean useSlots;

    public AttributesPrinter(boolean useSlots) {
        super();
        this.useSlots = useSlots;
    }

    public void print(RAttributable value, PrintContext printCtx) throws IOException {
        RAttributes attrs = value.getAttributes();
        if (attrs == null) {
            return;
        }

        for (RAttribute a : attrs) {
            if (useSlots && RRuntime.CLASS_SYMBOL.equals(a.getName())) {
                continue;
            }
            ValuePrinterNode utils = printCtx.printerNode();
            if (utils.isArray(value) || utils.isList(value)) {
                if (RRuntime.DIM_ATTR_KEY.equals(a.getName()) || RRuntime.DIMNAMES_ATTR_KEY.equals(a.getName())) {
                    continue;
                }
            }
            if (utils.inherits(value, "factor", RRuntime.LOGICAL_FALSE)) {
                if (RRuntime.LEVELS_ATTR_KEY.equals(a.getName())) {
                    continue;
                }
                if (RRuntime.CLASS_ATTR_KEY.equals(a.getName())) {
                    continue;
                }
            }
            if (value instanceof RDataFrame) {
                if (RRuntime.ROWNAMES_ATTR_KEY.equals(a.getName())) {
                    continue;
                }
            }
            if (!utils.isArray(value)) {
                if (RRuntime.NAMES_ATTR_KEY.equals(a.getName())) {
                    continue;
                }
            }
            if (RRuntime.R_COMMENT.equals(a.getName()) || RRuntime.R_SOURCE.equals(a.getName()) ||
                            RRuntime.R_SRCREF.equals(a.getName()) || RRuntime.R_WHOLE_SRCREF.equals(a.getName()) ||
                            RRuntime.R_SRCFILE.equals(a.getName())) {
                continue;
            }

            final PrintWriter out = printCtx.output();
            final String tag;
            if (useSlots) {
                tag = String.format("Slot \"%s\":", a.getName());
            } else {
                tag = String.format("attr(,\"%s\")", a.getName());
            }
            out.println(tag);

            if (RRuntime.ROWNAMES_ATTR_KEY.equals(a.getName())) {
                /* need special handling AND protection */
                Object val = a.getValue();
                ValuePrinters.printValue(val, printCtx);
                continue;
            }
            if (RContext.getInstance().isMethodTableDispatchOn() && utils.isS4(value)) {
                throw new UnsupportedOperationException("TODO");
            }
            if (utils.isObject(value)) {
                throw new UnsupportedOperationException("TODO");
            }

            ValuePrinters.printValue(a.getValue(), printCtx);
        }
    }

}
