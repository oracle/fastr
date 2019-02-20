/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates
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
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.SuppressFBWarnings;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.conn.RConnection.ReadLineWarning;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public abstract class ReadTableHead extends RExternalBuiltinNode.Arg7 {

    static {
        Casts casts = new Casts(ReadTableHead.class);
        casts.arg(0).defaultError(Message.INVALID_CONNECTION).mustNotBeNull().asIntegerVector().findFirst();
        casts.arg(1).mustNotBeNull().asIntegerVector().findFirst();
        casts.arg(2).mustNotBeMissing().mustBe(Predef.stringValue()).asStringVector().findFirst();
        casts.arg(3).mustNotBeMissing().mustBe(Predef.logicalValue()).asLogicalVector().findFirst().map(Predef.toBoolean());
        casts.arg(4).mustNotBeMissing().mustBe(Predef.stringValue()).asStringVector().findFirst();
        casts.arg(5).mustNotBeMissing().mustBe(Predef.stringValue()).asStringVector().findFirst();
        casts.arg(6).mustNotBeMissing().mustBe(Predef.logicalValue()).asLogicalVector().findFirst().map(Predef.toBoolean());
    }

    @Specialization
    @TruffleBoundary
    public RAbstractStringVector read(int con, int nlines, String commentChar, boolean blankLinesSkip,
                    String quote, String sep, boolean skipNull) {
        // TODO This is quite incomplete and just uses readLines, which works for some inputs
        try (RConnection openConn = RConnection.fromIndex(con).forceOpen("r")) {
            ReadState readState = new ReadState(openConn, nlines,
                            (commentChar.length() > 0) ? commentChar.charAt(0) : -1, quote, skipNull);
            int sepChar = (sep.length() > 0) ? sep.charAt(0) : -1;
            int quoteChar = -1;
            StringBuilder lineBuilder = new StringBuilder();
            while (readState.resultLines.size() < nlines) {
                boolean empty = true;
                boolean skip = false;
                boolean firstNonWhite = true;
                int c;
                while ((c = readState.nextChar()) != -1) {
                    if (quoteChar != -1) {
                        if (sepChar != -1 && c == '\\') {
                            lineBuilder.append('\\');
                            c = readState.nextChar();
                            if (c == -1) {
                                error(Message.EOF_AFTER_BACKSLASH);
                            }
                            lineBuilder.append((char) c);
                            continue;
                        } else if (c == quoteChar) {
                            if (sepChar == -1) {
                                quoteChar = -1;
                            } else {
                                int c2 = readState.nextChar();
                                if (c2 == quoteChar) {
                                    lineBuilder.append((char) c);
                                } else {
                                    readState.pushBack(c2);
                                    quoteChar = -1;
                                }
                            }
                        }
                    } else if (!skip && firstNonWhite && quote.indexOf((char) c) != -1) {
                        quoteChar = c;
                    } else if (Character.isWhitespace((char) c) || c == sepChar) {
                        firstNonWhite = true;
                    } else {
                        firstNonWhite = false;
                    }
                    if (empty && !skip && c != '\n' && c != readState.commentChar) {
                        empty = false;
                    }
                    if (quoteChar == -1 && !skip && c == readState.commentChar) {
                        skip = true;
                    }
                    if (quoteChar != -1 || c != '\n') {
                        lineBuilder.append((char) c);
                    } else {
                        break;
                    }
                }
                if (!empty || (c != -1 && !blankLinesSkip)) {
                    readState.addResultLine(lineBuilder);
                }
                if (c == -1) {
                    break;
                }
            }
            return RDataFactory.createStringVector(readState.resultLines.toArray(new String[0]), RDataFactory.COMPLETE_VECTOR);
        } catch (IOException ex) {
            throw error(RError.Message.ERROR_READING_CONNECTION, ex.getMessage());
        }
    }

    private final class ReadState {

        private final RConnection openConn;

        private final int nlines;

        final int commentChar;

        private final boolean skipNull;

        final List<String> resultLines;

        private String[] readLines;

        private int readLineIndex;

        private int colIndex;

        private int pushBackChar = -1;

        private boolean inQuote;

        private int nullCnt;

        ReadState(RConnection openConn, int nlines, int commentChar, @SuppressWarnings("unused") String quotes, boolean skipNull) {
            this.openConn = openConn;
            this.nlines = nlines;
            this.commentChar = commentChar;
            this.skipNull = skipNull;
            this.resultLines = new ArrayList<>(nlines);
        }

        @SuppressFBWarnings(value = "UWF_UNWRITTEN_FIELD", justification = "incomplete implementation")
        int nextChar() throws IOException {
            int c;
            if (pushBackChar != -1) {
                c = pushBackChar;
                if (c == 0) {
                    nullCnt++;
                }
                pushBackChar = -1;
            } else {
                c = readChar();
            }
            if (!inQuote && commentChar != -1 && c == commentChar) {
                do {
                    c = readChar();
                } while (c != -1 && c != '\n');
            }

            // TODO if (false produces ecj "dead code" warnigs so commented the whole if branch
            // The allowEscapes flag is currently not propagated into table head reading which
            // causes e.g. a header line duplication - see tests
            // if (false && c == '\\') { // Assuming escapes never allowed
            // c = readChar();
            // if ('0' <= c && c <= '8') {
            // int octal = c - '0';
            // if ('0' <= (c = readChar()) && c <= '8') {
            // octal = 8 * octal + c - '0';
            // if ('0' <= (c = readChar()) && c <= '8') {
            // octal = 8 * octal + c - '0';
            // } else {
            // pushBack(c);
            // }
            // } else {
            // pushBack(c);
            // }
            // c = octal;
            // } else if (c == -1) {
            // c = '\\';
            // } else {
            // switch ((char) c) {
            // case 'a':
            // c = '\u0007';
            // break;
            // case 'b':
            // c = '\b';
            // break;
            // case 'f':
            // c = '\f';
            // break;
            // case 'n':
            // c = '\n';
            // break;
            // case 'r':
            // c = '\r';
            // break;
            // case 't':
            // c = '\t';
            // break;
            // case 'v':
            // c = '\u000B';
            // break;
            // case 'x':
            // int hex = 0;
            // int ext;
            // for (int i = 0; i < 2; i++) {
            // c = readChar();
            // if (c >= '0' && c <= '9') {
            // ext = c - '0';
            // } else if (c >= 'A' && c <= 'F') {
            // ext = c - 'A' + 10;
            // } else if (c >= 'a' && c <= 'f') {
            // ext = c - 'a' + 10;
            // } else {
            // pushBack(c);
            // break;
            // }
            // hex = 16 * hex + ext;
            // }
            // c = hex;
            // break;
            // default:
            // if (inQuote && quotes.indexOf(c) != -1) {
            // pushBack(c);
            // c = '\\';
            // }
            // break;
            // }
            // }
            // }
            return c;
        }

        int readChar() throws IOException {
            int c;
            do {
                if (readLines == null || readLineIndex >= readLines.length) {
                    readLineIndex = 0;
                    readLines = openConn.readLines(nlines - resultLines.size(), EnumSet.of(ReadLineWarning.INCOMPLETE_LAST_LINE), false);
                    if (readLines == null || readLines.length == 0) {
                        return -1;
                    }
                }
                String readLine = readLines[readLineIndex];
                if (colIndex < readLine.length()) {
                    c = readLine.charAt(colIndex++);
                } else {
                    c = '\n';
                    readLineIndex++;
                    colIndex = 0;
                }
            } while (c == 0 && skipNull);
            if (c == 0) {
                nullCnt++;
            }
            return c;
        }

        void pushBack(int c) {
            pushBackChar = c;
            if (c == 0) {
                nullCnt--;
            }
        }

        void addResultLine(StringBuilder lineBuilder) {
            resultLines.add(lineBuilder.toString());
            lineBuilder.setLength(0);
            if (nullCnt > 0) {
                nullCnt = 0;
                warning(Message.LINE_CONTAINS_EMBEDDED_NULLS, resultLines.size());
            }
        }

    }

}
