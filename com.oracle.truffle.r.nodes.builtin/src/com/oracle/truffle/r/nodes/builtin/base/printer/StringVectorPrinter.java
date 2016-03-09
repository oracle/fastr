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

import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.asBlankArg;

import java.io.IOException;
import java.util.Arrays;

import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

//Transcribed from GnuR, src/main/format.c

public final class StringVectorPrinter extends VectorPrinter<RAbstractStringVector> {

    public static final StringVectorPrinter INSTANCE = new StringVectorPrinter();

    @Override
    protected VectorPrinter<RAbstractStringVector>.VectorPrintJob createJob(RAbstractStringVector vector, int indx, PrintContext printCtx) {
        return new StringVectorPrintJob(vector, indx, printCtx);
    }

    private final class StringVectorPrintJob extends VectorPrintJob {

        protected StringVectorPrintJob(RAbstractStringVector vector, int indx, PrintContext printCtx) {
            super(vector, indx, printCtx);
        }

        @Override
        protected FormatMetrics formatVector(int offs, int len) {
            int w = formatString(vector, offs, len, quote, printCtx.parameters());
            return new FormatMetrics(w);
        }

        @Override
        protected void printElement(int i, FormatMetrics fm) throws IOException {
            String s = vector.getDataAt(i);
            StringVectorPrinter.printString(s, fm.maxWidth, printCtx);
        }

        @Override
        protected void printCell(int i, FormatMetrics fm) throws IOException {
            String s = vector.getDataAt(i);
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
        protected void printMatrixColumnLabels(RAbstractStringVector cl, int jmin, int jmax, FormatMetrics[] w) {
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

    public static int formatString(RAbstractStringVector x, int offs, int n, boolean quote, PrintParameters pp) {
        int xmax = 0;
        int l;

        // output argument
        int fieldwidth;

        for (int i = 0; i < n; i++) {
            String s = x.getDataAt(offs + i);
            String xi = quote ? RRuntime.quoteString(s) : s;

            if (xi == RRuntime.STRING_NA) {
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

    public static void printString(String s, int w, PrintContext printCtx) {
        String outS = encode(s, w, printCtx.parameters());
        printCtx.output().print(outS);
    }

    public static String encode(String s, int w, PrintJustification justify) {
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

    public static String encode(String value, int w, PrintParameters pp) {
        final boolean quote = pp.getQuote();
        final String s;
        if (quote) {
            if (RRuntime.isNA(value)) {
                s = pp.getNaString();
            } else {
                s = RRuntime.quoteString(value);
            }
        } else {
            if (RRuntime.isNA(value)) {
                s = pp.getNaStringNoquote();
            } else {
                s = value;
            }
        }
        return StringVectorPrinter.encode(s, w, pp.getRight() ? PrintJustification.right : PrintJustification.left);
    }

}
