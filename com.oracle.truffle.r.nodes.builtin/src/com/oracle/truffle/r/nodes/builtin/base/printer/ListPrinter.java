/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1997-2013,  The R Core Team
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
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
import com.oracle.truffle.r.runtime.data.RComplex;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

//Transcribed from GnuR, src/main/print.c

final class ListPrinter extends AbstractValuePrinter<RAbstractListVector> {

    static final ListPrinter INSTANCE = new ListPrinter();

    private ListPrinter() {
        // singleton
    }

    private static int TAGBUFLEN = 256;

    @Override
    @TruffleBoundary
    protected void printValue(RAbstractListVector s, PrintContext printCtx) throws IOException {
        RAbstractIntVector dims = Utils.<RAbstractIntVector> castTo(
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
            Object tmp = RRuntime.asAbstractVector(s.getDataAtAsObject(i));
            final String pbuf;
            if (tmp == null || tmp == RNull.instance) {
                pbuf = RRuntime.NULL;
            } else if (tmp instanceof RAbstractLogicalVector) {
                RAbstractLogicalVector lv = (RAbstractLogicalVector) tmp;
                if (lv.getLength() == 1) {
                    FormatMetrics fm = LogicalVectorPrinter.formatLogicalVector(lv, 0, 1, pp.getNaWidth());
                    pbuf = LogicalVectorPrinter.encodeLogical(lv.getDataAt(0), fm.maxWidth, pp);
                } else {
                    pbuf = "Logical," + lv.getLength();
                }
            } else if (tmp instanceof RAbstractIntVector) {
                RAbstractIntVector iv = (RAbstractIntVector) tmp;
                if (printCtx.printerNode().inherits(iv, RRuntime.CLASS_FACTOR)) {
                    /* factors are stored as integers */
                    pbuf = "factor," + iv.getLength();
                } else {
                    if (iv.getLength() == 1) {
                        FormatMetrics fm = IntegerVectorPrinter.formatIntVector(iv, 0, 1, pp.getNaWidth());
                        pbuf = IntegerVectorPrinter.encodeInteger(iv.getDataAt(0), fm.maxWidth, pp);
                    } else {
                        pbuf = "Integer," + iv.getLength();
                    }
                }
            } else if (tmp instanceof RAbstractDoubleVector) {
                RAbstractDoubleVector dv = (RAbstractDoubleVector) tmp;
                if (dv.getLength() == 1) {
                    DoubleVectorMetrics fm = DoubleVectorPrinter.formatDoubleVector(dv, 0, 1, 0, pp);
                    pbuf = DoubleVectorPrinter.encodeReal(dv.getDataAt(0), fm, pp);
                } else {
                    pbuf = "Numeric," + dv.getLength();
                }
            } else if (tmp instanceof RAbstractComplexVector) {
                RAbstractComplexVector cv = (RAbstractComplexVector) tmp;
                if (cv.getLength() == 1) {
                    RComplex x = cv.getDataAt(0);
                    if (RRuntime.isNA(x.getRealPart()) || RRuntime.isNA(x.getImaginaryPart())) {
                        /* formatReal(NA) --> w=R_print.na_width, d=0, e=0 */
                        pbuf = DoubleVectorPrinter.encodeReal(RRuntime.DOUBLE_NA, pp.getNaWidth(), 0, 0, '.', pp);
                    } else {
                        ComplexVectorMetrics cvm = ComplexVectorPrinter.formatComplexVector(x, 0, 1, 0, pp);
                        pbuf = ComplexVectorPrinter.encodeComplex(x, cvm, '.', pp);
                    }
                } else {
                    pbuf = "Complex," + cv.getLength();
                }
            } else if (tmp instanceof RAbstractStringVector) {
                RAbstractStringVector sv = (RAbstractStringVector) tmp;
                if (sv.getLength() == 1) {
                    String ctmp = RRuntime.escapeString(sv.getDataAt(0), true, true);
                    int len = ctmp.length();
                    if (len < 100) {
                        pbuf = ctmp;
                    } else {
                        pbuf = Utils.trimSize(101, ctmp) + "\" [truncated]";
                    }
                } else {
                    pbuf = "Character," + sv.getLength();
                }
            } else if (tmp instanceof RAbstractRawVector) {
                pbuf = "Raw," + ((RAbstractRawVector) (tmp)).getLength();
            } else if (tmp instanceof RAbstractListVector) {
                pbuf = "List," + ((RAbstractListVector) (tmp)).getLength();
            } else if (tmp instanceof RLanguage) {
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
    static void printNoDimList(RAbstractContainer s, PrintContext printCtx) throws IOException {
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
                        if (ss == RRuntime.STRING_NA) {
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
                Object si = s.getDataAtAsObject(i);
                if (si instanceof RAttributable && ((RAttributable) si).isObject()) {
                    RContext.getEngine().printResult(si);
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
