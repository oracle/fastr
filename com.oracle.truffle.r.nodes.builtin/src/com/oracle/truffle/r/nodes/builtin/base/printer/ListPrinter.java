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

//Transcribed from GnuR, src/main/print.c

import com.oracle.truffle.api.frame.Frame;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.canBeDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.canBeIntVector;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.canBeLogicalVector;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.canBeStringVector;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.indexWidth;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.isValidName;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.snprintf;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.toComplexVector;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.toDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.toIntVector;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.toLogicalVector;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.toStringVector;

import java.io.IOException;
import java.io.PrintWriter;

import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.base.printer.ComplexVectorPrinter.ComplexVectorMetrics;
import com.oracle.truffle.r.nodes.builtin.base.printer.DoubleVectorPrinter.DoubleVectorMetrics;
import com.oracle.truffle.r.nodes.builtin.base.printer.VectorPrinter.FormatMetrics;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
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

public final class ListPrinter extends AbstractValuePrinter<RAbstractListVector> {

    public static final ListPrinter INSTANCE = new ListPrinter();

    private static RAttributeProfiles dummyAttrProfiles = RAttributeProfiles.create();

    private static int TAGBUFLEN = 256;

    @Override
    protected void printValue(RAbstractListVector s, PrintContext printCtx) throws IOException {
        RAbstractIntVector dims = Utils.<RAbstractIntVector> castTo(
                        s.getAttr(dummyAttrProfiles, RRuntime.DIM_ATTR_KEY));

        if (dims != null && dims.getLength() > 1) {
            printDimList(s, printCtx);
        } else {
            // no dim()
            printNoDimList(s, printCtx);
        }

    }

    private static void printDimList(RAbstractListVector s, PrintContext printCtx) throws IOException {
        final PrintParameters pp = printCtx.parameters();

        int ns = s.getLength();
        String[] t = new String[ns];
        for (int i = 0; i < ns; i++) {
            Object tmp = s.getDataAtAsObject(i);
            final String pbuf;
            if (tmp == null || tmp == RNull.instance) {
                pbuf = RRuntime.NULL;
            } else if (canBeLogicalVector(tmp)) {
                RAbstractLogicalVector lv = toLogicalVector(tmp);
                if (lv.getLength() == 1) {
                    FormatMetrics fm = LogicalVectorPrinter.formatLogicalVector(lv, 0, 1, pp.getNaWidth());
                    pbuf = snprintf(115, "%s",
                                    LogicalVectorPrinter.encodeLogical(lv.getDataAt(0), fm.maxWidth, pp));
                } else {
                    pbuf = snprintf(115, "Logical,%d", lv.getLength());
                }
            } else if (canBeIntVector(tmp)) {
                RAbstractIntVector iv = toIntVector(tmp);
                if (printCtx.printerNode().inherits(iv, "factor", RRuntime.LOGICAL_FALSE)) {
                    /* factors are stored as integers */
                    pbuf = snprintf(115, "factor,%d", iv.getLength());
                } else {
                    if (iv.getLength() == 1) {
                        FormatMetrics fm = IntegerVectorPrinter.formatIntVector(iv, 0, 1, pp.getNaWidth());
                        pbuf = snprintf(115, "%s",
                                        IntegerVectorPrinter.encodeInteger(iv.getDataAt(0), fm.maxWidth, pp));
                    } else {
                        pbuf = snprintf(115, "Integer,%d", iv.getLength());
                    }
                }
            } else if (canBeDoubleVector(tmp)) {
                RAbstractDoubleVector dv = toDoubleVector(tmp);
                if (dv.getLength() == 1) {
                    DoubleVectorMetrics fm = DoubleVectorPrinter.formatDoubleVector(dv, 0, 1, 0, pp);
                    pbuf = snprintf(115, "%s",
                                    DoubleVectorPrinter.encodeReal(dv.getDataAt(0), fm, pp));
                } else {
                    pbuf = snprintf(115, "Numeric,%d", dv.getLength());
                }
            } else if (tmp instanceof RAbstractComplexVector) {
                RAbstractComplexVector cv = toComplexVector(tmp);
                if (cv.getLength() == 1) {
                    RComplex x = cv.getDataAt(0);
                    if (RRuntime.isNA(x.getRealPart()) || RRuntime.isNA(x.getImaginaryPart())) {
                        /* formatReal(NA) --> w=R_print.na_width, d=0, e=0 */
                        pbuf = snprintf(115, "%s",
                                        DoubleVectorPrinter.encodeReal(RRuntime.DOUBLE_NA, pp.getNaWidth(), 0, 0, '.', pp));
                    } else {
                        ComplexVectorMetrics cvm = ComplexVectorPrinter.formatComplexVector(x, 0, 1, 0, pp);
                        pbuf = snprintf(115, "%s", ComplexVectorPrinter.encodeComplex(x, cvm, pp));
                    }
                } else {
                    pbuf = snprintf(115, "Complex,%d", cv.getLength());
                }
            } else if (canBeStringVector(tmp)) {
                RAbstractStringVector sv = toStringVector(tmp);
                if (sv.getLength() == 1) {
                    String ctmp = sv.getDataAt(0);
                    int len = ctmp.length();
                    if (len < 100) {
                        pbuf = snprintf(115, "\"%s\"", ctmp);
                    } else {
                        pbuf = snprintf(101, "\"%s", ctmp) + "\" [truncated]";
                    }
                } else {
                    pbuf = snprintf(115, "Character,%d", sv.getLength());
                }
            } else if (tmp instanceof RAbstractRawVector) {
                pbuf = snprintf(115, "Raw,%d", ((RAbstractRawVector) (tmp)).getLength());
            } else if (tmp instanceof RAbstractListVector) {
                pbuf = snprintf(115, "List,%d", ((RAbstractListVector) (tmp)).getLength());
            } else if (tmp instanceof RLanguage) {
                pbuf = snprintf(115, "Expression");
            } else {
                pbuf = snprintf(115, "?");
            }

            t[i] = pbuf;
        }

        RStringVector tt = RDataFactory.createStringVector(t, true, s.getDimensions());
        Object dimNames = s.getAttr(dummyAttrProfiles, RRuntime.DIMNAMES_ATTR_KEY);
        tt.setAttr(RRuntime.DIMNAMES_ATTR_KEY, dimNames);

        PrintContext cc = printCtx.cloneContext();
        cc.parameters().setQuote(false);
        StringVectorPrinter.INSTANCE.print(tt, cc);
    }

    static void printNoDimList(RAbstractContainer s, PrintContext printCtx) throws IOException {
        final PrintParameters pp = printCtx.parameters();
        final PrintWriter out = printCtx.output();

        class TagBuf {
            final int tagStartMark = buffer().length();

            private StringBuilder buffer() {
                StringBuilder buf = (StringBuilder) printCtx.getAttribute(ListPrinter.class.getName());
                if (buf == null) {
                    buf = new StringBuilder();
                    printCtx.setAttribute(ListPrinter.class.getName(), buf);
                }
                return buf;
            }

            int taglen() {
                return buffer().length();
            }

            void appendTag(String tag) {
                buffer().append(tag);
            }

            void removeTag() {
                buffer().delete(tagStartMark, buffer().length());
            }

            @Override
            public String toString() {
                return buffer().toString();
            }

        }
        final TagBuf tagbuf = new TagBuf();
        int taglen = tagbuf.taglen();

        int ns = s.getLength();

        RAbstractStringVector names;
        names = toStringVector(s.getAttr(dummyAttrProfiles, RRuntime.NAMES_ATTR_KEY));

        if (ns > 0) {
            int npr = (ns <= pp.getMax() + 1) ? ns : pp.getMax();
            /* '...max +1' ==> will omit at least 2 ==> plural in msg below */
            for (int i = 0; i < npr; i++) {
                if (i > 0) {
                    out.println();
                }
                String ss = names == null ? null : Utils.<String> getDataAt(names, i);
                if (ss != null && !"".equals(ss)) {
                    /*
                     * Bug for L <- list(`a\\b` = 1, `a\\c` = 2) : const char *ss =
                     * translateChar(STRING_ELT(names, i));
                     */
                    if (taglen + ss.length() > TAGBUFLEN) {
                        if (taglen <= TAGBUFLEN) {
                            tagbuf.appendTag("$...");
                        }
                    } else {
                        /*
                         * we need to distinguish character NA from "NA", which is a valid (if
                         * non-syntactic) name
                         */
                        if (ss == RRuntime.STRING_NA) {
                            tagbuf.appendTag("$<NA>");
                        } else if (isValidName(ss)) {
                            tagbuf.appendTag(String.format("$%s", ss));
                        } else {
                            tagbuf.appendTag(String.format("$`%s`", ss));
                        }
                    }

                } else {
                    if (taglen + indexWidth(i) > TAGBUFLEN) {
                        if (taglen <= TAGBUFLEN) {
                            tagbuf.appendTag("$...");
                        }
                    } else {
                        tagbuf.appendTag(String.format("[[%d]]", i + 1));
                    }
                }

                out.println(tagbuf);
                Object si = s.getDataAtAsObject(i);
                if (printCtx.printerNode().isObject(si)) {
                    ValuePrinters.INSTANCE.println(si, printCtx);
                } else {
                    ValuePrinters.INSTANCE.println(si, printCtx);
                }

                tagbuf.removeTag();
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
                RAbstractStringVector klass = toStringVector(s.getAttr(dummyAttrProfiles, RRuntime.CLASS_ATTR_KEY));
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
