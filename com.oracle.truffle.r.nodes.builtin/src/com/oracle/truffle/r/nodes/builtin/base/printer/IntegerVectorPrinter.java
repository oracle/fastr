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

import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.indexWidth;

import java.io.IOException;

import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

//Transcribed from GnuR, src/main/printutils.c, src/main/format.c

public final class IntegerVectorPrinter extends VectorPrinter<RAbstractIntVector> {

    public static final IntegerVectorPrinter INSTANCE = new IntegerVectorPrinter();

    @Override
    protected IntegerVectorPrintJob createJob(RAbstractIntVector vector, int indx, boolean quote, PrintContext printCtx) {
        return new IntegerVectorPrintJob(vector, indx, quote, printCtx);
    }

    private final class IntegerVectorPrintJob extends VectorPrintJob {

        protected IntegerVectorPrintJob(RAbstractIntVector vector, int indx, boolean quote, PrintContext printCtx) {
            super(vector, indx, quote, printCtx);
        }

        @Override
        protected String elementTypeName() {
            return "integer";
        }

        @Override
        protected FormatMetrics formatVector(int offs, int len) {
            return formatIntVector(vector, offs, len, printCtx.parameters().getNaWidth());
        }

        @Override
        protected void printElement(int i, FormatMetrics fm) throws IOException {
            String v = IntegerPrinter.encodeInteger(vector.getDataAt(i), fm.maxWidth, printCtx.parameters());
            out.print(v);
        }

        @Override
        protected void printCell(int i, FormatMetrics fm) throws IOException {
            printElement(i, fm);
        }

        @Override
        protected void printEmptyVector() throws IOException {
            out.println("integer(0)");
        }

    }

    public static FormatMetrics formatIntVector(RAbstractIntVector x, int offs, int n, int naWidth) {
        int xmin = RRuntime.INT_MAX_VALUE;
        int xmax = RRuntime.INT_MIN_VALUE;
        boolean naflag = false;
        int l;
        int fieldwidth;

        for (int i = 0; i < n; i++) {
            int xi = x.getDataAt(offs + i);
            if (xi == RRuntime.INT_NA) {
                naflag = true;
            } else {
                if (xi < xmin) {
                    xmin = xi;
                }
                if (xi > xmax) {
                    xmax = xi;
                }
            }
        }

        if (naflag) {
            fieldwidth = naWidth;
        } else {
            fieldwidth = 1;
        }

        if (xmin < 0) {
            l = indexWidth(-xmin) + 1; /* +1 for sign */
            if (l > fieldwidth) {
                fieldwidth = l;
            }
        }
        if (xmax > 0) {
            l = indexWidth(xmax);
            if (l > fieldwidth) {
                fieldwidth = l;
            }
        }

        return new FormatMetrics(fieldwidth);
    }

}
