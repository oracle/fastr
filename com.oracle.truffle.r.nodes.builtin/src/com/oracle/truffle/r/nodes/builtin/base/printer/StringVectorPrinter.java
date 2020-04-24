/*
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2013,  The R Core Team
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates
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

import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.asBlankArg;

import java.io.IOException;
import java.util.Arrays;

import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;

//Transcribed from GnuR, src/main/format.c

final class StringVectorPrinter extends VectorPrinter<RStringVector> {

    static final StringVectorPrinter INSTANCE = new StringVectorPrinter();

    private StringVectorPrinter() {
        // singleton
    }

    @Override
    protected VectorPrinter<RStringVector>.VectorPrintJob createJob(RStringVector vector, int indx, PrintContext printCtx) {
        return new StringVectorPrintJob(vector, indx, printCtx);
    }

    private final class StringVectorPrintJob extends VectorPrintJob {

        protected StringVectorPrintJob(RStringVector vector, int indx, PrintContext printCtx) {
            super(vector, indx, printCtx);
        }

        @Override
        protected FormatMetrics formatVector(int offs, int len) {
            int w = formatString(iterator, access, offs, len, quote, printCtx.parameters());
            return new FormatMetrics(w);
        }

        @Override
        protected void printElement(int i, FormatMetrics fm) throws IOException {
            String s = access.getString(iterator, i);
            StringVectorPrinter.printString(s, fm.maxWidth, printCtx);
        }

        @Override
        protected void printCell(int i, FormatMetrics fm) throws IOException {
            String s = access.getString(iterator, i);
            String outS = StringVectorPrinter.encode(s, fm.maxWidth, printCtx.parameters());
            int g = printCtx.parameters().getGap();
            String fmt = "%" + asBlankArg(g) + "s%s";
            printCtx.output().printf(fmt, "", outS);
        }

        @Override
        protected void printEmptyVector() throws IOException {
            out.print("character(0)");
        }

        @Override
        protected void printMatrixColumnLabels(RStringVector cl, int jmin, int jmax, FormatMetrics[] w) {
            if (printCtx.parameters().getRight()) {
                for (int j = jmin; j < jmax; j++) {
                    rightMatrixColumnLabel(cl, j, w[j].maxWidth);
                }
            } else {
                for (int j = jmin; j < jmax; j++) {
                    leftMatrixColumnLabel(cl, j, w[j].maxWidth);
                }
            }
        }

        @Override
        protected int matrixColumnWidthCorrection1() {
            return 0;
        }

        @Override
        protected int matrixColumnWidthCorrection2() {
            return printCtx.parameters().getGap();
        }

        @Override
        protected String elementTypeName() {
            return "character";
        }
    }

    static int formatString(RandomIterator iter, VectorAccess access, int offs, int n, boolean quote, PrintParameters pp) {
        int xmax = 0;
        int l;

        // output argument
        int fieldwidth;

        for (int i = 0; i < n; i++) {
            String s = access.getString(iter, offs + i);
            String xi = RRuntime.escapeString(s, false, quote);

            if (RRuntime.isNA(xi)) {
                l = quote ? pp.getNaWidth() : pp.getNaWidthNoquote();
            } else {
                l = xi.length();
            }
            if (l > xmax) {
                xmax = l;
            }
        }

        fieldwidth = xmax;

        return fieldwidth;
    }

    static void printString(String s, int w, PrintContext printCtx) {
        String outS = encode(s, w, printCtx.parameters());
        printCtx.output().print(outS);
    }

    static String encode(String s, int w, PrintJustification justify) {
        // justification
        final int b = w - s.length(); // total amount of blanks
        int bl = 0; // left blanks
        int br = 0; // right blanks

        switch (justify) {
            case left:
                br = b;
                break;
            case center:
                bl = b / 2;
                br = b - bl;
                break;
            case right:
                bl = b;
                break;
            case none:
                break;
        }

        StringBuilder sb = new StringBuilder();

        if (bl > 0) {
            char[] sp = new char[bl];
            Arrays.fill(sp, ' ');
            sb.append(sp);
        }

        sb.append(s);

        if (br > 0) {
            char[] sp = new char[br];
            Arrays.fill(sp, ' ');
            sb.append(sp);
        }

        return sb.toString();
    }

    static String encode(String value, int w, PrintParameters pp) {
        String s;
        if (RRuntime.isNA(value)) {
            s = pp.getQuote() ? pp.getNaString() : pp.getNaStringNoquote();
        } else {
            s = RRuntime.escapeString(value, false, pp.getQuote());
        }
        return StringVectorPrinter.encode(s, w, pp.getRight() ? PrintJustification.right : PrintJustification.left);
    }
}
