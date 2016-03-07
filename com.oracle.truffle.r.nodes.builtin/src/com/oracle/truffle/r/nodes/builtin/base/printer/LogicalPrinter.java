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

import static com.oracle.truffle.r.nodes.builtin.base.printer.IntegerPrinter.NB;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.snprintf;

import java.io.IOException;
import java.io.PrintWriter;

import com.oracle.truffle.r.nodes.builtin.base.printer.VectorPrinter.FormatMetrics;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RLogical;

//Transcribed from GnuR, src/main/printutils.c

public final class LogicalPrinter extends AbstractScalarValuePrinter<Byte> {

    public static final LogicalPrinter INSTANCE = new LogicalPrinter();

    @Override
    protected void printScalarValue(Byte value, PrintContext printCtx) throws IOException {
        FormatMetrics fm = LogicalVectorPrinter.formatLogicalVector(RLogical.valueOf(value),
                        0, 1, printCtx.parameters().getNaWidth());
        String s = encodeLogical(value, fm.maxWidth, printCtx.parameters());
        PrintWriter out = printCtx.output();
        out.print(s);
    }

    public static String encodeLogical(byte x, int w, PrintParameters pp) {
        final String fmt = "%" + Utils.asBlankArg(Math.min(w, (NB - 1))) + "s";
        if (x == RRuntime.LOGICAL_NA) {
            return snprintf(NB, fmt, pp.getNaString());
        } else if (x != 0) {
            return snprintf(NB, fmt, "TRUE");
        } else {
            return snprintf(NB, fmt, "FALSE");
        }
    }

}
