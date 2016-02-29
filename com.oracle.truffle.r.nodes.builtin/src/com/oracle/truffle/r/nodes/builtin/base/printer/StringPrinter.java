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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;

import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.conn.RConnection;

//Transcribed from GnuR, src/main/printutils.c

public final class StringPrinter extends AbstractValuePrinter<String> {

    public static final StringPrinter INSTANCE = new StringPrinter();

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
        return encode(s, w, pp.getRight() ? PrintJustification.right : PrintJustification.left);
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

    public static void printString(String s, int w, PrintContext printCtx) {
        String outS = encode(s, w, printCtx.parameters());
        printCtx.output().print(outS);
    }

    @Override
    protected void printValue(String value, PrintContext printCtx) throws IOException {
        PrintWriter out = printCtx.output();
        out.print("[1] ");
        printString(value, value.length(), printCtx);
        out.println();
    }

}
