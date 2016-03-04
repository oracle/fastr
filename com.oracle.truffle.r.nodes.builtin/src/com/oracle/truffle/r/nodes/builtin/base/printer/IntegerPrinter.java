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

import com.oracle.truffle.r.nodes.builtin.base.printer.VectorPrinter.FormatMetrics;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RInteger;

//Transcribed from GnuR, src/main/printutils.c

public final class IntegerPrinter extends AbstractValuePrinter<Integer> {

    public static final IntegerPrinter INSTANCE = new IntegerPrinter();

    @Override
    protected void printValue(Integer value, PrintContext printCtx) throws IOException {
        FormatMetrics fm = IntegerVectorPrinter.formatIntVector(RInteger.valueOf(value),
                        0, 1, printCtx.parameters().getNaWidth());
        String s = encodeInteger(value, fm.maxWidth, printCtx.parameters());

        PrintWriter out = printCtx.output();
        out.print("[1] ");
        out.println(s);
    }

    /*
     * There is no documented (or enforced) limit on 'w' here, so use snprintf
     */
    public static int NB = 1000;

    public static String encodeInteger(int x, int w, PrintParameters pp) {
        if (x == RRuntime.INT_NA) {
            return Utils.snprintf(NB, "%" + Utils.asBlankArg(Math.min(w, (NB - 1))) + "s", pp.getNaString());
        } else {
            return Utils.snprintf(NB, "%" + Utils.asBlankArg(Math.min(w, (NB - 1))) + "d", x);
        }
    }

}
