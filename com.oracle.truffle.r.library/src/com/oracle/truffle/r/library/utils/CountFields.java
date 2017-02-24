/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.utils;

import java.io.IOException;
import java.io.InputStream;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;

// Transcribed from GnuR, library/utils/src/io.c

// Checkstyle: stop
public abstract class CountFields extends RExternalBuiltinNode.Arg6 {

    private static final int R_EOF = -1;
    private static final int SCAN_BLOCKSIZE = 1000;

    @SuppressWarnings("unused")
    private static class LocalData {
        Object NAstrings;
        boolean quiet;
        int sepchar; /* = 0 *//* This gets compared to ints */
        char decchar = '.'; /* = '.' *//* This only gets compared to chars */
        String quoteset; /* = NULL */
        int comchar = 100000; /* = NO_COMCHAR */
        boolean ttyflag; /* = 0 */
        RConnection con; /* = NULL */
        boolean wasopen; /* = FALSE */
        boolean escapes; /* = FALSE */
        int save; /* = 0; */
        boolean isLatin1; /* = FALSE */
        boolean isUTF8; /* = FALSE */
        boolean skipNul;
        byte[] convbuf = new byte[100];
        InputStream is;

    }

    static {
        Casts casts = new Casts(CountFields.class);
        casts.arg(0, "conn").asIntegerVector().findFirst();
        casts.arg(1, "sep").allowNull().mustBe(Predef.stringValue()).asStringVector().findFirst();
        casts.arg(2, "quote").allowNull().mustBe(Predef.stringValue()).asStringVector().findFirst();
        casts.arg(3, "nskip").asIntegerVector().findFirst();
        casts.arg(4, "blskip").asLogicalVector().findFirst().replaceNA(RRuntime.LOGICAL_TRUE).map(Predef.toBoolean());
        casts.arg(5, "commend.char").mustBe(Predef.stringValue()).asStringVector().findFirst().mustBe(Predef.length(1));
    }

    @Specialization
    protected Object count(int conn, Object sep, Object quote, int nskipArg, boolean blskip, String commentCharArg) {
        char comChar;
        if (!(commentCharArg != null && commentCharArg.length() == 1)) {
            throw error(RError.Message.INVALID_ARGUMENT, "comment.char");
        } else {
            comChar = commentCharArg.charAt(0);
        }

        int nskip;
        if (nskipArg < 0 || nskipArg == RRuntime.INT_NA) {
            nskip = 0;
        } else {
            nskip = nskipArg;
        }

        char sepChar;
        if (sep instanceof RNull) {
            sepChar = 0;
        } else {
            String s = (String) sep;
            if (s == null) {
                throw error(RError.Message.INVALID_ARGUMENT, "sep");
            } else {
                sepChar = s.length() == 0 ? 0 : s.charAt(0);
            }
        }
        String quoteSet;
        if (quote instanceof RNull) {
            quoteSet = "";
        } else {
            quoteSet = (String) quote;
        }
        try (RConnection openConn = RConnection.fromIndex(conn).forceOpen("r")) {
            return countFields(openConn, sepChar, quoteSet, nskip, blskip, comChar);
        } catch (IllegalStateException | IOException ex) {
            throw error(RError.Message.GENERIC, ex.getMessage());
        }
    }

    @TruffleBoundary
    private static Object countFields(RConnection file, char sepChar, String quoteSet, @SuppressWarnings("unused") int nskip, boolean blskip, char comChar) throws IOException {
        LocalData data = new LocalData();
        data.sepchar = sepChar;
        data.comchar = comChar;
        data.quoteset = quoteSet;

        if (file == StdConnections.getStdin()) {
            data.ttyflag = true;
            throw new IOException("count.fields not implemented for stdin");
        } else {
            data.ttyflag = false;
        }

        data.wasopen = file.isOpen();

        data.save = 0;
        int quote = 0;
        int inquote = 0;
        int nfields = 0;
        int nlines = 0;
        int blocksize = SCAN_BLOCKSIZE;
        int[] ans = new int[blocksize];

        try (RConnection openConn = file.forceOpen("r")) {
            data.is = openConn.getInputStream();
            while (true) {
                int c = scanchar(inquote, data);
                if (c == R_EOF) {
                    if (nfields != 0) {
                        ans[nlines] = nfields;
                    } else {
                        nlines--;
                    }
                    break;
                } else if (c == '\n') {
                    if (inquote != 0) {
                        ans[nlines] = RRuntime.INT_NA;
                        nlines++;
                    } else if (nfields > 0 || !blskip) {
                        ans[nlines] = nfields;
                        nlines++;
                        nfields = 0;
                        inquote = 0;
                    }
                    if (nlines == blocksize) {
                        int[] bns = ans;
                        blocksize = 2 * blocksize;
                        ans = new int[blocksize];
                        System.arraycopy(bns, 0, ans, 0, bns.length);
                    }
                    continue;
                } else if (data.sepchar != 0) {
                    if (nfields == 0)
                        nfields++;
                    if (inquote != 0 && c == R_EOF) {
                        throw new IllegalStateException("quoted string on line " + inquote + " terminated by EOF");
                    }
                    if (inquote != 0 && c == quote)
                        inquote = 0;
                    else if (data.quoteset.indexOf(c) > 0) {
                        inquote = nlines + 1;
                        quote = c;
                    }
                    if (c == data.sepchar && inquote == 0)
                        nfields++;
                } else if (!Rspace(c)) {
                    if (data.quoteset.indexOf(c) > 0) {
                        quote = c;
                        inquote = nlines + 1;
                        while ((c = scanchar(inquote, data)) != quote) {
                            if (c == R_EOF) {
                                throw new IllegalStateException("quoted string on line " + inquote + " terminated by EOF");
                            } else if (c == '\n') {
                                ans[nlines] = RRuntime.INT_NA;
                                nlines++;
                                if (nlines == blocksize) {
                                    int[] bns = ans;
                                    blocksize = 2 * blocksize;
                                    ans = new int[blocksize];
                                    System.arraycopy(bns, 0, ans, 0, bns.length);
                                }
                            }
                        }
                        inquote = 0;
                    } else {
                        do {
                            // if (dbcslocale && btowc(c) == WEOF)
                            // scanchar2(&data);
                            c = scanchar(0, data);
                        } while (!Rspace(c) && c != R_EOF);
                        if (c == R_EOF)
                            c = '\n';
                        unscanchar(c, data);
                    }
                    nfields++;
                }
            }
        }
        /*
         * we might have a character that was unscanchar-ed. So pushback if possible
         */
        if (data.save != 0 && !data.ttyflag && data.wasopen) {
            // TODO use more primitive method when available
            file.pushBack(RDataFactory.createStringVectorFromScalar(new String(new char[]{(char) data.save})), false);
        }

        if (nlines < 0) {
            return RNull.instance;
        }
        if (nlines == blocksize) {
            return RDataFactory.createIntVector(ans, RDataFactory.COMPLETE_VECTOR);
        }

        int[] bns = new int[nlines + 1];
        for (int i = 0; i <= nlines; i++) {
            bns[i] = ans[i];
        }
        return RDataFactory.createIntVector(bns, RDataFactory.COMPLETE_VECTOR);

    }

    private static int scanchar_raw(LocalData d) throws IOException {
        int c = (d.ttyflag) ? -1 : d.is.read();
        if (c == 0) {
            if (d.skipNul) {
                do {
                    c = (d.ttyflag) ? -1 : d.is.read();
                } while (c == 0);
            }
        }
        return c;
    }

    private static void unscanchar(int c, LocalData d) {
        d.save = c;
    }

    private static int scanchar(int inQuote, LocalData d) throws IOException {
        int next;
        if (d.save != 0) {
            next = d.save;
            d.save = 0;
        } else {
            next = scanchar_raw(d);
        }
        if (next == d.comchar && inQuote != 0) {
            do {
                next = scanchar_raw(d);
            } while (next != '\n' && next != R_EOF);
        }
        if (next == '\\' && d.escapes) {
            next = scanchar_raw(d);
            if ('0' <= next && next <= '8') {
                int octal = next - '0';
                if ('0' <= (next = scanchar_raw(d)) && next <= '8') {
                    octal = 8 * octal + next - '0';
                    if ('0' <= (next = scanchar_raw(d)) && next <= '8') {
                        octal = 8 * octal + next - '0';
                    } else {
                        unscanchar(next, d);
                    }
                } else {
                    unscanchar(next, d);
                }
                next = octal;
            } else {
                switch (next) {
                    case 'a':
                        next = 7;
                        break;
                    case 'b':
                        next = '\b';
                        break;
                    case 'f':
                        next = '\f';
                        break;
                    case 'n':
                        next = '\n';
                        break;
                    case 'r':
                        next = '\r';
                        break;
                    case 't':
                        next = '\t';
                        break;
                    case 'v':
                        next = 11;
                        break;
                    case 'x': {
                        int val = 0;
                        int i;
                        int ext;
                        for (i = 0; i < 2; i++) {
                            next = scanchar_raw(d);
                            if (next >= '0' && next <= '9') {
                                ext = next - '0';
                            } else if (next >= 'A' && next <= 'F') {
                                ext = next - 'A' + 10;
                            } else if (next >= 'a' && next <= 'f') {
                                ext = next - 'a' + 10;
                            } else {
                                unscanchar(next, d);
                                break;
                            }
                            val = 16 * val + ext;
                        }
                        next = val;
                    }
                        break;

                    default:
                        /*
                         * Any other char and even EOF escapes to itself, but we need to preserve \"
                         * etc inside quotes.
                         */
                        if (inQuote != 0 && d.quoteset.indexOf(next) >= 0) {
                            unscanchar(next, d);
                            next = '\\';
                        }
                        break;
                }
            }
        }
        return next;
    }

    private static boolean Rspace(int c) {
        if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
            return true;
        }
        // TODO locale
        return false;
    }
}
