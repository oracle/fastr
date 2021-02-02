/*
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2013,  The R Core Team
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates
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

import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.indexWidth;

import java.io.IOException;

import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;

//Transcribed from GnuR, src/main/printutils.c, src/main/format.c

public final class IntegerVectorPrinter extends VectorPrinter<RIntVector> {

    static final IntegerVectorPrinter INSTANCE = new IntegerVectorPrinter();

    private IntegerVectorPrinter() {
        // singleton
    }

    @Override
    protected IntegerVectorPrintJob createJob(RIntVector vector, int indx, PrintContext printCtx) {
        return new IntegerVectorPrintJob(vector, indx, printCtx);
    }

    private final class IntegerVectorPrintJob extends VectorPrintJob {

        protected IntegerVectorPrintJob(RIntVector vector, int indx, PrintContext printCtx) {
            super(vector, indx, printCtx);
        }

        @Override
        protected String elementTypeName() {
            return "integer";
        }

        @Override
        protected FormatMetrics formatVector(int offs, int len) {
            return new FormatMetrics(formatIntVectorInternal(iterator, access, offs, len, printCtx.parameters().getNaWidth()));
        }

        @Override
        protected void printElement(int i, FormatMetrics fm) throws IOException {
            String v = encodeInteger(access.getInt(iterator, i), fm.maxWidth, printCtx.parameters());
            out.print(v);
        }

        @Override
        protected void printCell(int i, FormatMetrics fm) throws IOException {
            printElement(i, fm);
        }

        @Override
        protected void printEmptyVector() throws IOException {
            out.print("integer(0)");
        }
    }

    static int formatIntVectorInternal(RandomIterator iter, VectorAccess access, int offs, int n, int naWidth) {
        int xmin = RRuntime.INT_MAX_VALUE;
        int xmax = RRuntime.INT_MIN_VALUE;
        boolean naflag = false;
        int l;
        int fieldwidth;

        for (int i = 0; i < n; i++) {
            int xi = access.getInt(iter, offs + i);
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
        return fieldwidth;
    }

    private static final int[][] DECIMAL_VALUES = new int[10][10];
    private static final int[] DECIMAL_WEIGHTS = new int[10];

    static {
        for (int i = 0; i < DECIMAL_WEIGHTS.length; i++) {
            DECIMAL_WEIGHTS[i] = (int) Math.pow(10, i);
        }
        for (int i = 0; i < DECIMAL_VALUES.length; i++) {
            for (int i2 = 0; i2 < 10; i2++) {
                DECIMAL_VALUES[i][i2] = (int) (Math.pow(10, i) * i2);
            }
        }
    }

    public static String encodeInteger(int initialX, int w, PrintParameters pp) {
        StringBuilder str = new StringBuilder(w);

        int x = initialX;
        if (RRuntime.isNA(x)) {
            String id = pp.getNaString();
            for (int i = w - id.length(); i > 0; i--) {
                str.append(' ');
            }
            str.append(id);
        } else {
            boolean negated = false;
            if (x < 0) {
                negated = true;
                x = -x;
            }
            int log10 = x == 0 ? 0 : (int) Math.log10(x);
            int blanks = w // target width
                            - (negated ? 1 : 0) // "-"
                            - (log10 + 1); // digits

            for (int i = 0; i < blanks; i++) {
                str.append(' ');
            }
            if (negated) {
                str.append('-');
            }
            for (int i = log10; i >= 0; i--) {
                x = appendDigit(x, i, str);
            }
        }
        return str.toString();
    }

    private static int appendDigit(int x, int digit, StringBuilder str) {
        int c = x / DECIMAL_WEIGHTS[digit];
        assert c >= 0 && c <= 9;
        str.append((char) ('0' + c));
        return x - DECIMAL_VALUES[digit][c];
    }

    public static String[] format(RIntVector value, boolean trim, int width, PrintParameters pp) {
        VectorAccess access = value.slowPathAccess();
        RandomIterator iter = access.randomAccess(value);
        int w;
        int length = access.getLength(iter);
        if (trim) {
            w = 1;
        } else {
            w = formatIntVectorInternal(iter, access, 0, length, pp.getNaWidth());
        }
        w = Math.max(w, width);

        String[] result = new String[length];
        for (int i = 0; i < length; i++) {
            result[i] = encodeInteger(access.getInt(iter, i), w, pp);
        }
        return result;
    }
}
