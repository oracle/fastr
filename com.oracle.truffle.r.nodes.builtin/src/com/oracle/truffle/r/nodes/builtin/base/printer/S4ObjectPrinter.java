/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base.printer;

import java.io.IOException;
import java.io.PrintWriter;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributesLayout;
import com.oracle.truffle.r.runtime.data.RAttributesLayout.RAttribute;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RS4Object;

final class S4ObjectPrinter implements ValuePrinter<RS4Object> {

    static final S4ObjectPrinter INSTANCE = new S4ObjectPrinter();

    private S4ObjectPrinter() {
        // singleton
    }

    @Override
    public void print(RS4Object object, PrintContext printCtx) throws IOException {
        final PrintWriter out = printCtx.output();
        out.print("<S4 Type Object>");
        if (object.getAttributes() != null) {
            for (RAttribute attr : RAttributesLayout.asIterable(object.getAttributes())) {
                printAttribute(attr, printCtx);
            }
        }
    }

    private static void printAttribute(RAttributesLayout.RAttribute attr, PrintContext printCtx) throws IOException {
        final PrintWriter out = printCtx.output();
        out.println();
        out.print("attr(,\"");
        out.print(attr.getName());
        out.println("\")");
        ValuePrinters.INSTANCE.print(attr.getValue(), printCtx);
    }

    static void printS4(PrintContext printCtx, Object o) {
        Frame frame = com.oracle.truffle.r.runtime.Utils.getActualCurrentFrame();
        RContext.getEngine().evalFunction(createShowFunction(frame), null, null, true, null, o);
        // The show function prints an additional new line character. The following attribute
        // instructs the ValuePrinter.println method not to print the new line since it was
        // already printed.
        printCtx.setAttribute(DONT_PRINT_NL_ATTR, true);
    }

    private static RFunction createShowFunction(Frame frame) {
        return ReadVariableNode.lookupFunction("show", frame);
    }
}
