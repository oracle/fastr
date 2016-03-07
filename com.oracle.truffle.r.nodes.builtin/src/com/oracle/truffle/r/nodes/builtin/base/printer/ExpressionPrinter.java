/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;

public final class ExpressionPrinter extends AbstractValuePrinter<RExpression> {

    public static final ExpressionPrinter INSTANCE = new ExpressionPrinter();

    private static RAttributeProfiles dummyAttrProfiles = RAttributeProfiles.create();

    @Override
    protected void printValue(RExpression expr, PrintContext printCtx) throws IOException {
        final PrintWriter out = printCtx.output();
        final PrintContext valPrintCtx = printCtx.cloneContext();
        valPrintCtx.parameters().setSuppressIndexLabels(true);

        out.print("expression(");
        RList exprs = expr.getList();
        RStringVector names = (RStringVector) expr.getAttr(dummyAttrProfiles, RRuntime.NAMES_ATTR_KEY);
        for (int i = 0; i < exprs.getLength(); i++) {
            if (i != 0) {
                out.print(", ");
            }
            if (names != null && names.getDataAt(i) != null) {
                out.print(names.getDataAt(i));
                out.print(" = ");
            }
            ValuePrinters.INSTANCE.print(exprs.getDataAt(i), valPrintCtx);
        }
        out.print(')');
    }

}
