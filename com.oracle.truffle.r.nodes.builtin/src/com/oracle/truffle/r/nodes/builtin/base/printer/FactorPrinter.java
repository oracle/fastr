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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RFactor;
import com.oracle.truffle.r.runtime.data.RVector;
import com.oracle.truffle.r.runtime.data.closures.RClosures;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

final class FactorPrinter extends AbstractValuePrinter<RAbstractIntVector> {

    static final FactorPrinter INSTANCE = new FactorPrinter();

    private FactorPrinter() {
        // singleton
    }

    @SuppressWarnings("unused") private static RAttributeProfiles dummyAttrProfiles = RAttributeProfiles.create();

    @Override
    @TruffleBoundary
    protected void printValue(RAbstractIntVector operand, PrintContext printCtx) throws IOException {
        // TODO: this should be handled by an S3 function. Should it? For example, in C code for
        // split, there is direct call to getAttrib. This should be refactored to use
        // AttributeAccess node or even Factor.GetLevels node. The same holds for the access
        RVector levels = RFactor.getLevels(operand);
        String[] strings;
        if (levels == null) {
            strings = new String[0];
        } else {
            strings = new String[levels.getLength()];
            for (int i = 0; i < levels.getLength(); i++) {
                strings[i] = printCtx.printerNode().castString(levels.getDataAtAsObject(i));
            }
        }

        RAbstractVector v = RClosures.createFactorToVector(operand, true, levels);
        PrintContext vectorPrintCtx = printCtx.cloneContext();
        vectorPrintCtx.parameters().setQuote(false);
        ValuePrinters.INSTANCE.println(v, vectorPrintCtx);

        final PrintWriter out = printCtx.output();
        out.print("Levels:");
        if (levels != null) {
            for (int i = 0; i < levels.getLength(); i++) {
                out.print(" ");
                out.print(strings[i]);
            }
        }
    }
}
