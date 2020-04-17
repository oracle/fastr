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

import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.indexWidth;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.snprintf;

import java.io.IOException;
import java.io.PrintWriter;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess;
import com.oracle.truffle.r.runtime.data.nodes.VectorAccess.RandomIterator;

//Transcribed from GnuR, src/main/print.c

final class ListPrinter extends AbstractValuePrinter<RAbstractListVector> {

    static final ListPrinter INSTANCE = new ListPrinter();

    private ListPrinter() {
        // singleton
    }

    private static final int TAGBUFLEN = 256;

    @Override
    @TruffleBoundary
    protected void printValue(RAbstractListVector s, PrintContext printCtx) throws IOException {
        RIntVector dims = Utils.<RIntVector> castTo(
                        s.getAttr(RRuntime.DIM_ATTR_KEY));

        if (dims != null && dims.getLength() > 1) {
            printDimList(s, printCtx);
        } else {
            // no dim()
            printNoDimList(s, printCtx);
        }
    }

    @TruffleBoundary
    private static void printDimList(RAbstractListVector s, PrintContext printCtx) throws IOException {
        final PrintParameters pp = printCtx.parameters();

        int ns = s.getLength();
        String[] t = new String[ns];
        for (int i = 0; i < ns; i++) {
            Object tmp = RRuntime.asAbstractVector(s.getDataAt(i));
            final String pbuf;
            if (tmp == null || tmp == RNull.instance) {
                pbuf = RRuntime.NULL;
            } else if (tmp instanceof RAbstractLogicalVector) {
                RAbstractLogicalVector lv = (RAbstractLogicalVector) tmp;
                if (lv.getLength() == 1) {
                    pbuf = LogicalVectorPrinter.format(lv, false, 0, pp)[0];
                } else {
                    pbuf = "Logical," + lv.getLength();
                }
            } else if (tmp instanceof RIntVector) {
                RIntVector iv = (RIntVector) tmp;
                if (printCtx.printerNode().inherits(iv, RRuntime.CLASS_FACTOR)) {
                    /* factors are stored as integers */
                    pbuf = "factor," + iv.getLength();
                } else {
                    if (iv.getLength() == 1) {
                        pbuf = IntegerVectorPrinter.format(iv, false, 0, pp)[0];
                    } else {
                        pbuf = "Integer," + iv.getLength();
                    }
                }
            } else if (tmp instanceof RDoubleVector) {
                RDoubleVector dv = (RDoubleVector) tmp;
                if (dv.getLength() == 1) {
                    pbuf = DoubleVectorPrinter.format(dv, false, 0, 0, '.', pp)[0];
                } else {
                    pbuf = "Numeric," + dv.getLength();
                }
            } else if (tmp instanceof RAbstractComplexVector) {
                RAbstractComplexVector cv = (RAbstractComplexVector) tmp;
                if (cv.getLength() == 1) {
                    pbuf = ComplexVectorPrinter.format(cv, false, 0, 0, '.', pp)[0];
                } else {
                    pbuf = "Complex," + cv.getLength();
                }
            } else if (tmp instanceof RAbstractStringVector) {
                RAbstractStringVector sv = (RAbstractStringVector) tmp;
                if (sv.getLength() == 1) {
                    String ctmp;
                    VectorAccess access = sv.slowPathAccess();
                    try (RandomIterator iter = access.randomAccess(sv)) {
                        ctmp = RRuntime.escapeString(access.getString(iter, 0), true, true);
                    }
                    if (ctmp.length() < 100) {
                        pbuf = ctmp;
                    } else {
                        pbuf = Utils.trimSize(101, ctmp) + "\" [truncated]";
                    }
                } else {
                    pbuf = "Character," + sv.getLength();
                }
            } else if (tmp instanceof RRawVector) {
                pbuf = "Raw," + ((RRawVector) (tmp)).getLength();
            } else if (tmp instanceof RAbstractListVector) {
                pbuf = "List," + ((RAbstractListVector) (tmp)).getLength();
            } else if ((tmp instanceof RPairList && ((RPairList) tmp).isLanguage())) {
                pbuf = "Expression";
            } else {
                pbuf = "?";
            }

            t[i] = pbuf;
        }

        RStringVector tt = RDataFactory.createStringVector(t, true, s.getDimensions());
        Object dimNames = s.getAttr(RRuntime.DIMNAMES_ATTR_KEY);
        tt.setAttr(RRuntime.DIMNAMES_ATTR_KEY, dimNames);

        PrintContext cc = printCtx.cloneContext();
        cc.parameters().setQuote(false);
        StringVectorPrinter.INSTANCE.print(tt, cc);
    }

    @TruffleBoundary
    static void printNoDimList(RAbstractListVector s, PrintContext printCtx) throws IOException {
        final PrintParameters pp = printCtx.parameters();
        final PrintWriter out = printCtx.output();

        final StringBuilder tagbuf = printCtx.getOrCreateTagBuffer();
        // save the original length so that we can restore the original value
        int taglen = tagbuf.length();

        int ns = s.getLength();

        RAbstractStringVector names;
        names = Utils.castTo(RRuntime.asAbstractVector(s.getNames()));

        if (ns > 0) {
            int npr = (ns <= pp.getMax() + 1) ? ns : pp.getMax();
            /* '...max +1' ==> will omit at least 2 ==> plural in msg below */
            for (int i = 0; i < npr; i++) {
                if (i > 0) {
                    out.println();
                }
                String ss = names == null ? null : Utils.<String> getDataAt(names, i);
                if (ss != null && !ss.isEmpty()) {
                    /*
                     * Bug for L <- list(`a\\b` = 1, `a\\c` = 2) : const char *ss =
                     * translateChar(STRING_ELT(names, i));
                     */
                    if (taglen + ss.length() > TAGBUFLEN) {
                        if (taglen <= TAGBUFLEN) {
                            tagbuf.append("$...");
                        }
                    } else {
                        /*
                         * we need to distinguish character NA from "NA", which is a valid (if
                         * non-syntactic) name
                         */
                        if (RRuntime.isNA(ss)) {
                            tagbuf.append("$<NA>");
                        } else if (RDeparse.isValidName(ss)) {
                            tagbuf.append(String.format("$%s", ss));
                        } else {
                            tagbuf.append(String.format("$`%s`", ss));
                        }
                    }
                } else {
                    if (taglen + indexWidth(i) > TAGBUFLEN) {
                        if (taglen <= TAGBUFLEN) {
                            tagbuf.append("$...");
                        }
                    } else {
                        tagbuf.append(String.format("[[%d]]", i + 1));
                    }
                }

                out.println(tagbuf);
                Object si = s.getDataAt(i);
                if (si instanceof RAttributable && ((RAttributable) si).isObject()) {
                    RContext.getEngine().printResult(RContext.getInstance(), si);
                } else {
                    ValuePrinters.INSTANCE.print(si, printCtx);
                    ValuePrinters.printNewLine(printCtx);
                }
                tagbuf.setLength(taglen); // reset tag buffer to the original value
            }

            if (npr < ns) {
                out.printf("\n [ reached getOption(\"max.print\") -- omitted %d entries ]",
                                ns - npr);
            }
        } else {
            /* ns = length(s) == 0 */

            /* Formal classes are represented as empty lists */
            String className = null;
            if (printCtx.printerNode().isObject(s) && printCtx.printerNode().isMethodDispatchOn()) {
                RAbstractStringVector klass = Utils.castTo(RRuntime.asAbstractVector(s.getAttr(RRuntime.CLASS_ATTR_KEY)));
                if (klass != null && klass.getLength() == 1) {
                    String ss = klass.getDataAt(0);
                    String str = snprintf(200, ".__C__%s", ss);
                    Frame frame = com.oracle.truffle.r.runtime.Utils.getActualCurrentFrame();
                    if (ReadVariableNode.lookupAny(str, frame, false) != null) {
                        className = ss;
                    }
                }
            }
            if (className != null) {
                out.printf("An object of class \"%s\"", className);
            } else {
                if (names != null) {
                    out.print("named ");
                }
                out.print("list()");
            }
        }
    }
}
