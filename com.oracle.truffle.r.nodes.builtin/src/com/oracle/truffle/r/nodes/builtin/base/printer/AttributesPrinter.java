/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2013,  The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base.printer;

import java.io.IOException;
import java.io.PrintWriter;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.r.nodes.function.ClassHierarchyNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;

//Transcribed from GnuR, src/main/print.c

final class AttributesPrinter implements ValuePrinter<RAttributable> {

    static final AttributesPrinter INSTANCE = new AttributesPrinter(false);

    private final boolean useSlots;

    private AttributesPrinter(boolean useSlots) {
        this.useSlots = useSlots;
    }

    @Override
    @TruffleBoundary
    public void print(RAttributable value, PrintContext printCtx) throws IOException {
        DynamicObject attrs = value.getAttributes();
        if (attrs == null) {
            return;
        }

        final StringBuilder savedBuffer = printCtx.getTagBuffer();
        printCtx.resetTagBuffer();
        for (RAttributesLayout.RAttribute a : RAttributesLayout.asIterable(attrs)) {
            if (useSlots && RRuntime.CLASS_SYMBOL.equals(a.getName())) {
                continue;
            }
            ValuePrinterNode utils = printCtx.printerNode();
            if (utils.isArray(value) || utils.isList(value)) {
                if (RRuntime.DIM_ATTR_KEY.equals(a.getName()) || RRuntime.DIMNAMES_ATTR_KEY.equals(a.getName())) {
                    continue;
                }
            }
            if (utils.inherits(value, RRuntime.CLASS_FACTOR)) {
                if (RRuntime.LEVELS_ATTR_KEY.equals(a.getName())) {
                    continue;
                }
                if (RRuntime.CLASS_ATTR_KEY.equals(a.getName())) {
                    continue;
                }
            }
            if (ClassHierarchyNode.hasClass(value, RRuntime.CLASS_DATA_FRAME)) {
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
            out.println();

            final String tag;
            if (useSlots) {
                tag = String.format("Slot \"%s\":", a.getName());
            } else {
                tag = String.format("attr(,\"%s\")", a.getName());
            }
            out.println(tag);

            StringBuilder buff = printCtx.getOrCreateTagBuffer();
            int origLen = buff.length();
            buff.append(tag);

            if (RContext.getInstance().isMethodTableDispatchOn() && utils.isS4(a.getValue())) {
                S4ObjectPrinter.printS4(printCtx, a.getValue());
                // throw new UnsupportedOperationException("TODO");
            } else {
                if (a.getValue() instanceof RAttributable && ((RAttributable) a.getValue()).isObject()) {
                    RContext.getEngine().printResult(a.getValue());
                } else {
                    ValuePrinters.INSTANCE.print(a.getValue(), printCtx);
                }
            }

            // restore tag buffer to its original value
            buff.setLength(origLen);
        }

        printCtx.setTagBuffer(savedBuffer);
    }
}
