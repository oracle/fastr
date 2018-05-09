/*
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2013,  The R Core Team
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
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
        if (printCtx.attrDepth() == 0) {
            printCtx.resetTagBuffer();
        }
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
            if (RRuntime.COMMENT_ATTR_KEY.equals(a.getName()) || RRuntime.R_SOURCE.equals(a.getName()) ||
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

            StringBuilder buff = printCtx.getOrCreateTagBuffer();
            int origLen = buff.length();
            buff.append(tag);
            out.println(buff);
            printCtx.updateAttrDepth(+1);
            try {
                RContext ctx = RContext.getInstance();
                if (ctx.isMethodTableDispatchOn() && utils.isS4(a.getValue())) {
                    S4ObjectPrinter.printS4(printCtx, a.getValue());
                } else {
                    if (a.getValue() instanceof RAttributable && ((RAttributable) a.getValue()).isObject()) {
                        RContext.getEngine().printResult(ctx, a.getValue());
                    } else {
                        ValuePrinters.INSTANCE.print(a.getValue(), printCtx);
                    }
                }
            } finally {
                printCtx.updateAttrDepth(-1);
            }

            // restore tag buffer to its original value
            buff.setLength(origLen);
        }

        printCtx.setTagBuffer(savedBuffer);
    }
}
