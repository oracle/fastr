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

import static com.oracle.truffle.r.nodes.builtin.base.printer.IntegerVectorPrinter.NB;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.snprintf;

import java.io.IOException;

import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;

//Transcribed from GnuR, src/main/printutils.c, src/main/format.c

final class LogicalVectorPrinter extends VectorPrinter<RAbstractLogicalVector> {

    static final LogicalVectorPrinter INSTANCE = new LogicalVectorPrinter();

    private LogicalVectorPrinter() {
        // singleton
    }

    @Override
    protected VectorPrinter<RAbstractLogicalVector>.VectorPrintJob createJob(RAbstractLogicalVector vector, int indx, PrintContext printCtx) {
        return new LogicalVectorPrintJob(vector, indx, printCtx);
    }

    private final class LogicalVectorPrintJob extends VectorPrintJob {

        protected LogicalVectorPrintJob(RAbstractLogicalVector vector, int indx, PrintContext printCtx) {
            super(vector, indx, printCtx);
        }

        @Override
        protected String elementTypeName() {
            return "logical";
        }

        @Override
        protected FormatMetrics formatVector(int offs, int len) {
            return formatLogicalVector(vector, offs, len, printCtx.parameters().getNaWidth());
        }

        @Override
        protected void printElement(int i, FormatMetrics fm) throws IOException {
            String v = encodeLogical(vector.getDataAt(i), fm.maxWidth, printCtx.parameters());
            out.print(v);
        }

        @Override
        protected void printCell(int i, FormatMetrics fm) throws IOException {
            printElement(i, fm);
        }

        @Override
        protected void printEmptyVector() throws IOException {
            out.print("logical(0)");
        }
    }

    static FormatMetrics formatLogicalVector(RAbstractLogicalVector x, int offs, int n, int naWidth) {
        int fieldwidth = 1;
        for (int i = 0; i < n; i++) {
            byte xi = x.getDataAt(offs + i);
            if (xi == RRuntime.LOGICAL_NA) {
                if (fieldwidth < naWidth) {
                    fieldwidth = naWidth;
                }
            } else if (xi != 0 && fieldwidth < 4) {
                fieldwidth = 4;
            } else if (xi == 0 && fieldwidth < 5) {
                fieldwidth = 5;
                break;
                /* this is the widest it can be, so stop */
            }
        }
        return new FormatMetrics(fieldwidth);
    }

    static String encodeLogical(byte x, int w, PrintParameters pp) {
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
