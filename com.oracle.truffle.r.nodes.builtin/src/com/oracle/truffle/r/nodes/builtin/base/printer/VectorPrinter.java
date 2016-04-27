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
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.indexWidth;

import java.io.IOException;
import java.io.PrintWriter;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

//Transcribed from GnuR, src/main/print.c, src/main/printarray.c, src/main/printvector.c

abstract class VectorPrinter<T extends RAbstractVector> extends AbstractValuePrinter<T> {

    private static RAttributeProfiles dummyAttrProfiles = RAttributeProfiles.create();

    @Override
    protected void printValue(T vector, PrintContext printCtx) throws IOException {
        printVector(vector, 1, printCtx);
    }

    public void printVector(T vector, int indx, PrintContext printCtx) throws IOException {
        createJob(vector, indx, printCtx).print();
    }

    protected abstract VectorPrintJob createJob(T vector, int indx, PrintContext printCtx);

    protected enum JobMode {
        nonEmpty,
        empty,
        named,
        namedEmpty,
        matrix,
        array
    }

    private static final int R_MIN_LBLOFF = 2;

    protected abstract class VectorPrintJob {

        protected final T vector;
        protected final int n;
        protected final int nPr;
        protected final int indx;
        protected final int labwidth;
        protected final boolean quote;
        protected final PrintContext printCtx;
        protected final PrettyPrintWriter out;
        protected final JobMode jobMode;
        protected final RAbstractStringVector names;
        protected final String title;
        protected final MatrixDimNames matrixDimNames;
        protected final RAbstractIntVector dims;
        protected final boolean supressIndexLabels;

        protected VectorPrintJob(T vector, int indx, PrintContext printCtx) {
            this.vector = vector;
            this.indx = indx;
            this.quote = printCtx.parameters().getQuote();

            MatrixDimNames mdn = null;

            Object dimAttr = vector.getAttr(dummyAttrProfiles, RRuntime.DIM_ATTR_KEY);
            if (dimAttr instanceof RAbstractIntVector) {
                dims = (RAbstractIntVector) dimAttr;
                if (dims.getLength() == 1) {
                    RList t = Utils.<RList> castTo(vector.getAttr(dummyAttrProfiles, RRuntime.DIMNAMES_ATTR_KEY));
                    if (t != null && t.getDataAt(0) != null) {
                        RAbstractStringVector nn = Utils.castTo(RRuntime.asAbstractVector(t.getAttr(dummyAttrProfiles, RRuntime.NAMES_ATTR_KEY)));

                        if (nn != null) {
                            title = nn.getDataAt(0);
                        } else {
                            title = null;
                        }

                        jobMode = vector.getLength() == 0 ? JobMode.namedEmpty : JobMode.named;
                        names = Utils.castTo(RRuntime.asAbstractVector(t.getDataAt(0)));
                    } else {
                        title = null;
                        names = null;
                        jobMode = vector.getLength() == 0 ? JobMode.empty : JobMode.nonEmpty;
                    }
                } else if (dims.getLength() == 2) {
                    mdn = new MatrixDimNames(vector);
                    title = null;
                    names = null;
                    jobMode = JobMode.matrix;
                } else {
                    mdn = new MatrixDimNames(vector);
                    title = null;
                    names = null;
                    jobMode = JobMode.array;
                }
            } else {
                dims = null;
                Object namesAttr = Utils.castTo(vector.getAttr(dummyAttrProfiles, RRuntime.NAMES_ATTR_KEY));
                if (namesAttr != null) {
                    if (vector.getLength() > 0) {
                        names = Utils.castTo(RRuntime.asAbstractVector(namesAttr));
                        jobMode = JobMode.named;
                    } else {
                        names = null;
                        jobMode = JobMode.namedEmpty;
                    }
                } else if (vector.getLength() > 0) {
                    jobMode = JobMode.nonEmpty;
                    names = null;
                } else {
                    jobMode = JobMode.empty;
                    names = null;
                }
                title = null;
            }

            this.printCtx = printCtx.cloneContext();
            this.supressIndexLabels = printCtx.parameters().getSuppressIndexLabels();
            if (jobMode == JobMode.named) {
                this.printCtx.parameters().setRight(true);
            }
            this.out = this.printCtx.output();
            this.n = vector.getLength();
            int max = printCtx.parameters().getMax();
            this.nPr = (n <= max + 1) ? n : max;
            this.labwidth = indexWidth(n) + 2;
            this.matrixDimNames = mdn;
        }

        public void print() throws IOException {
            switch (jobMode) {
                case empty:
                    printEmptyVector();
                    break;
                case nonEmpty:
                    printNonEmptyVector();
                    break;
                case named:
                    printNamedVector();
                    break;
                case namedEmpty:
                    printNamedEmptyVector();
                    break;
                case matrix:
                    printMatrix();
                    break;
                case array:
                    printArray();
            }
        }

        private void printNamedEmptyVector() throws IOException {
            out.print("named ");
            printEmptyVector();
        }

        private void printNonEmptyVector() throws IOException {
            final int gap = supressIndexLabels ? 0 : printCtx.parameters().getGap();
            final int totalWidth = printCtx.parameters().getWidth();

            int width = 0;

            width = doLab(0);

            FormatMetrics fm = formatVector(0, n);
            final int w = fm.maxWidth;

            for (int i = 0; i < nPr; i++) {
                if (i > 0 && width + w + gap > totalWidth) {
                    out.println();
                    width = doLab(i);
                }
                out.printf("%" + asBlankArg(gap) + "s", "");
                printElementAndNotify(i, fm);
                width += w + gap;
            }
            if (nPr < n) {
                out.printf("\n [ reached getOption(\"max.print\") -- omitted %d entries ]", n - nPr);
            }
        }

        private void printNamedVector() throws IOException {
            if (title != null) {
                out.println(title);
            }

            int i;
            int j;
            int k;
            int nlines;
            int nperline;
            int wn;

            FormatMetrics fm = formatVector(0, n);

            PrintParameters pp = printCtx.parameters();

            wn = StringVectorPrinter.formatString(names, 0, n, false, pp);
            if (fm.maxWidth < wn) {
                fm.maxWidth = wn;
            }

            final int w = fm.maxWidth;

            nperline = pp.getWidth() / (w + pp.getGap());
            if (nperline <= 0) {
                nperline = 1;
            }
            nlines = n / nperline;
            if (n % nperline != 0) {
                nlines += 1;
            }

            int gap = pp.getGap();
            PrintContext namesPrintCtx = printCtx.cloneContext();
            namesPrintCtx.parameters().setQuote(false);
            namesPrintCtx.parameters().setRight(true);
            for (i = 0; i < nlines; i++) {
                if (i > 0) {
                    out.println();
                }
                for (j = 0; j < nperline && (k = i * nperline + j) < n; j++) {
                    StringVectorPrinter.printString(names.getDataAt(k), w, namesPrintCtx);
                    out.printf("%" + asBlankArg(gap) + "s", "");
                }
                out.println();
                for (j = 0; j < nperline && (k = i * nperline + j) < n; j++) {
                    printElementAndNotify(k, fm);
                    out.printf("%" + asBlankArg(gap) + "s", "");
                }
            }
        }

        private void printMatrix() throws IOException {
            printMatrix(0, true);
        }

        private void printMatrix(int offset, boolean printij) throws IOException {
            PrintParameters pp = printCtx.parameters();

            RAbstractStringVector rl = matrixDimNames.rl;
            RAbstractStringVector cl = matrixDimNames.cl;
            String rn = matrixDimNames.rn;
            String cn = matrixDimNames.cn;
            int r = dims.getDataAt(0);
            int c = dims.getDataAt(1);
            int rpr;

            /* PR#850 */
            if (rl != null && r > rl.getLength()) {
                throw RError.error(printCtx.printerNode(), RError.Message.GENERIC, "too few row labels");
            }
            if (cl != null && c > cl.getLength()) {
                throw RError.error(printCtx.printerNode(), RError.Message.GENERIC, "too few column labels");
            }
            if (r == 0 && c == 0) { // FIXME? names(dimnames(.)) :
                out.print("<0 x 0 matrix>");
                return;
            }
            rpr = r;
            if (c > 0 && pp.getMax() / c < r) {
                /* using floor(), not ceil(), since 'c' could be huge: */
                rpr = pp.getMax() / c;
            }

            printMatrix(offset, rpr, r, c, rl, cl, rn, cn, printij);

            if (rpr < r) {
                out.printf("\n [ reached getOption(\"max.print\") -- omitted %d rows ]", r - rpr);
            }

        }

        private void printMatrix(int offset, int rpr, int r, int c,
                        RAbstractStringVector rl, RAbstractStringVector cl, String rn, String cn,
                        boolean printij) throws IOException {
            // _PRINT_INIT_rl_rn

            PrintParameters pp = printCtx.parameters();

            FormatMetrics[] w = new FormatMetrics[c];
            int width;
            int rlabw = -1;
            int clabw = -1;
            int i;
            int j;
            int jmin = 0;
            int jmax = 0;
            int lbloff = 0;

            if (rl != null) {
                rlabw = StringVectorPrinter.formatString(rl, 0, r, false, pp);
            } else {
                rlabw = indexWidth(r + 1) + 3;
            }

            if (rn != null) {
                int rnw = rn.length();
                if (rnw < rlabw + R_MIN_LBLOFF) {
                    lbloff = R_MIN_LBLOFF;
                } else {
                    lbloff = rnw - rlabw;
                }

                rlabw += lbloff;
            }

            // define _COMPUTE_W2_(_FORMAT_j_, _LAST_j_)
            /* compute w[j] = column-width of j(+1)-th column : */
            for (j = 0; j < c; j++) {
                if (printij) {
                    w[j] = formatVector(offset + j * r, r);
                } else {
                    w[j] = formatVector(0, 0);
                }

                if (cl != null) {
                    String clj = cl.getDataAt(j);
                    if (RRuntime.isNA(clj)) {
                        clabw = pp.getNaWidthNoquote();
                    } else {
                        clabw = clj.length();
                    }
                } else {
                    clabw = indexWidth(j + 1) + 3;
                }

                if (w[j].maxWidth < clabw) {
                    w[j].maxWidth = clabw;
                }

                w[j].maxWidth += matrixColumnWidthCorrection1();
            }

            // _PRINT_MATRIX_(_W_EXTRA_, DO_COLUMN_LABELS, ENCODE_I_J)

            int wExtra = matrixColumnWidthCorrection2();
            if (c == 0) {
                printMatrixRowLab(cn, rn, rlabw);
                for (i = 0; i < r; i++) {
                    matrixRowLabel(rl, i, rlabw, lbloff);
                }
            } else {
                while (jmin < c) {
                    /* print columns jmin:(jmax-1) where jmax has to be determined first */

                    width = rlabw;
                    /* initially, jmax = jmin */
                    do {
                        width += w[jmax].maxWidth + wExtra;
                        jmax++;
                    } while (jmax < c && width + w[jmax].maxWidth + wExtra < pp.getWidth());

                    printMatrixRowLab(cn, rn, rlabw);

                    printMatrixColumnLabels(cl, jmin, jmax, w);

                    for (i = 0; i < rpr; i++) {
                        matrixRowLabel(rl, i, rlabw, lbloff); /* starting with an "\n" */
                        if (printij) {
                            for (j = jmin; j < jmax; j++) {
                                printCellAndNotify(offset + i + j * r, w[j]);
                            }
                        }
                    }
                    jmin = jmax;

                    if (jmin < c) {
                        out.println();
                    }
                }
            }
        }

        protected void printMatrixColumnLabels(RAbstractStringVector cl, int jmin, int jmax, FormatMetrics[] w) {
            // define STD_ColumnLabels
            for (int j = jmin; j < jmax; j++) {
                matrixColumnLabel(cl, j, w[j].maxWidth);
            }
        }

        private void printMatrixRowLab(String cn, String rn, int rlabw) {
            // _PRINT_ROW_LAB
            if (cn != null) {
                String fmt = "%" + asBlankArg(rlabw) + "s%s\n";
                out.printf(fmt, "", cn);
            }
            if (rn != null) {
                String fmt = "%" + asBlankArg(-rlabw) + "s";
                out.printf(fmt, rn);
            } else {
                String fmt = "%" + asBlankArg(rlabw) + "s";
                out.printf(fmt, "");
            }
        }

        private void matrixColumnLabel(RAbstractStringVector cl, int j, int w) {
            PrintParameters pp = printCtx.parameters();

            if (cl != null) {
                String tmp = cl.getDataAt(j);
                int l = (RRuntime.isNA(tmp)) ? pp.getNaWidthNoquote() : tmp.length();
                int gap = w - l;
                String fmt = "%" + asBlankArg(gap) + "s%s";

                PrintParameters pp2 = printCtx.parameters().cloneParameters();
                pp2.setQuote(false);
                pp2.setRight(false);
                out.printf(fmt, "", StringVectorPrinter.encode(tmp, l, pp2));
            } else {
                int gap = w - indexWidth(j + 1) - 3;
                String fmt = "%" + asBlankArg(gap) + "s[,%d]";
                out.printf(fmt, "", j + 1);
            }
        }

        protected void rightMatrixColumnLabel(RAbstractStringVector cl, int j, int w) {
            PrintParameters pp = printCtx.parameters();

            if (cl != null) {
                String tmp = cl.getDataAt(j);
                int l = (RRuntime.isNA(tmp)) ? pp.getNaWidthNoquote() : tmp.length();
                /*
                 * This does not work correctly at least on FC3 Rprintf("%*s", R_print.gap+w,
                 * EncodeString(tmp, l, 0, Rprt_adj_right));
                 */
                int g = pp.getGap() + w - l;
                String fmt = "%" + asBlankArg(g) + "s%s";

                PrintParameters pp2 = printCtx.parameters().cloneParameters();
                pp2.setQuote(false);
                pp2.setRight(true);
                out.printf(fmt, "", StringVectorPrinter.encode(tmp, l, pp2));
            } else {
                String g1 = asBlankArg(pp.getGap());
                String g2 = asBlankArg(w - indexWidth(j + 1) - 3);
                String fmt = "%" + g1 + "s[,%d]%" + g2 + "s";
                out.printf(fmt, "", j + 1, "");
            }
        }

        protected void leftMatrixColumnLabel(RAbstractStringVector cl, int j, int w) {
            PrintParameters pp = printCtx.parameters();

            if (cl != null) {
                String tmp = cl.getDataAt(j);
                int l = (RRuntime.isNA(tmp)) ? pp.getNaWidthNoquote() : tmp.length();
                String g1 = asBlankArg(pp.getGap());
                String g2 = asBlankArg(w - l);
                String fmt = "%" + g1 + "s%s%" + g2 + "s";

                PrintParameters pp2 = printCtx.parameters().cloneParameters();
                pp2.setQuote(false);
                pp2.setRight(false);
                out.printf(fmt, "", StringVectorPrinter.encode(tmp, l, pp2), "");
            } else {
                String g1 = asBlankArg(pp.getGap());
                String g2 = asBlankArg(w - indexWidth(j + 1) - 3);
                String fmt = "%" + g1 + "s[,%d]%" + g2 + "s";
                out.printf(fmt, "", j + 1, "");
            }
        }

        protected void matrixRowLabel(RAbstractStringVector rl, int i, int rlabw, int lbloff) {
            PrintParameters pp = printCtx.parameters();

            if (rl != null) {
                String tmp = rl.getDataAt(i);
                int l = (RRuntime.isNA(tmp)) ? pp.getNaWidthNoquote() : tmp.length();
                String gap = asBlankArg(rlabw - l - lbloff);
                String fmt = "\n%" + asBlankArg(lbloff) + "s%s%" + gap + "s";

                PrintParameters pp2 = printCtx.parameters().cloneParameters();
                pp2.setQuote(false);
                pp2.setRight(false);
                String s = StringVectorPrinter.encode(tmp, l, pp2);
                out.printf(fmt, "", s, "");
            } else {
                String gap = asBlankArg(rlabw - 3 - indexWidth(i + 1));
                String fmt = "\n%" + gap + "s[%d,]";
                out.printf(fmt, "", i + 1);
            }
        }

        private void printArray() throws IOException {
            PrintParameters pp = printCtx.parameters();

            MatrixDimNames mdn = this.matrixDimNames;
            int ndim = dims.getLength();

            int i;
            int j;
            int nb;
            int nbpr;
            int nrlast;
            int nr = dims.getDataAt(0);
            int nc = dims.getDataAt(1);
            int b = nr * nc;
            boolean maxreached;
            boolean hasdnn = mdn.axisNames != null;

            RAbstractStringVector dn;
            RAbstractStringVector dnn = mdn.axisNames;

            /*
             * nb := #{entries} in a slice such as x[1,1,..] or equivalently, the number of matrix
             * slices x[ , , *, ..] which are printed as matrices -- if options("max.print") allows
             */
            for (i = 2, nb = 1; i < ndim; i++) {
                nb *= dims.getDataAt(i);
            }
            maxreached = (b > 0 && pp.getMax() / b < nb);
            if (maxreached) { /* i.e., also b > 0, nr > 0, nc > 0, nb > 0 */
                /* nb_pr := the number of matrix slices to be printed */
                nbpr = (int) Math.ceil((double) pp.getMax() / b);
                /*
                 * for the last, (nb_pr)th matrix slice, use only nr_last rows; using floor(), not
                 * ceil(), since 'nc' could be huge:
                 */
                nrlast = (pp.getMax() - b * (nbpr - 1)) / nc;
                if (nrlast == 0) {
                    nbpr--;
                    nrlast = nr;
                }
            } else {
                nbpr = (nb > 0) ? nb : 1; // do print *something* when dim = c(a,b,0)
                nrlast = nr;
            }

            for (i = 0; i < nbpr; i++) {
                boolean doij = nb > 0;
                boolean ilast = (i == nbpr - 1); /* for the last slice */
                int usenr = ilast ? nrlast : nr;
                if (doij) {
                    int k = 1;
                    out.print(", ");
                    for (j = 2; j < ndim; j++) {
                        int l = (i / k) % dims.getDataAt(j) + 1;
                        if (mdn.hasDimNames &&
                                        ((dn = mdn.getDimNamesAt(j)) != null)) {
                            if (hasdnn) {
                                out.printf(", %s = %s",
                                                dnn.getDataAt(j),
                                                dn.getDataAt(l - 1));
                            } else {
                                out.printf(", %s", dn.getDataAt(l - 1));
                            }
                        } else {
                            out.printf(", %d", l);
                        }
                        k *= dims.getDataAt(j);
                    }
                    out.print("\n\n");
                } else { // nb == 0 -- e.g. <2 x 3 x 0 array of logical>
                    for (i = 0; i < ndim; i++) {
                        out.printf("%s%d", (i == 0) ? "<" : " x ", dims.getDataAt(i));
                    }
                    out.printf(" array of %s>\n", elementTypeName());
                }

                // int offset, int rpr, int r, int c,
                // RAbstractStringVector rl, RAbstractStringVector cl, String rn, String cn,
                // boolean printij
                printMatrix(i * b, usenr, nr, nc, mdn.rl, mdn.cl, mdn.rn, mdn.cn, doij);
                out.println();

                if (i + 1 < nbpr) {
                    out.println();
                }
            }
        }

        /**
         * See TypeTable in util.c.
         *
         * @return the R-name of the element type
         */
        protected abstract String elementTypeName();

        /**
         * @param offs the beginning offset in the internal store
         * @param len the number of elements to involve in formatting
         * @return the format metrics containing the width of the widest vector element and possibly
         *         other data type specific metrics
         */
        protected abstract FormatMetrics formatVector(int offs, int len);

        /**
         * Prints the i-th vector element.
         *
         * @param i the element index (zero-based)
         * @param fm the format metrics produced by the corresponding <code>formatVector</code>
         *            invocation
         * @throws IOException
         */
        protected abstract void printElement(int i, FormatMetrics fm) throws IOException;

        private void printElementAndNotify(int i, FormatMetrics fm) throws IOException {
            out.beginElement(vector, i, fm);
            printElement(i, fm);
            out.endElement(vector, i, fm);
        }

        /**
         * Prints the matrix cell at position (i,j).
         *
         * @param i the index (zero-based) of the cell in the raw data store
         * @param fm the format metrics produced by the corresponding <code>formatVector</code>
         *            invocation
         * @throws IOException
         */
        protected abstract void printCell(int i, FormatMetrics fm) throws IOException;

        private void printCellAndNotify(int i, FormatMetrics fm) throws IOException {
            out.beginElement(vector, i, fm);
            printCell(i, fm);
            out.endElement(vector, i, fm);
        }

        protected int matrixIndividualCellColumnWidthCorrection() {
            return 0;
        }

        protected int matrixColumnWidthCorrection1() {
            return printCtx.parameters().getGap();
        }

        protected int matrixColumnWidthCorrection2() {
            return 0;
        }

        private int doLab(int i) {
            if (indx > 0 && !supressIndexLabels) {
                printVectorIndex(i + 1, labwidth, out);
                return labwidth;
            } else {
                return 0;
            }
        }

        protected abstract void printEmptyVector() throws IOException;

    }

    private static void printVectorIndex(int i, int w, PrintWriter out) {
        /* print index label "[`i']" , using total width `w' (left filling blanks) */
        // out.printf("%*s[%ld]", w - indexWidth(i) - 2, "", i);
        String blanks = asBlankArg(w - indexWidth(i) - 2);
        String fmt = "%" + blanks + "s[%d]";
        out.printf(fmt, "", i);
    }

    private static final class MatrixDimNames {
        final RList dimnames;
        final RAbstractStringVector rl;
        final RAbstractStringVector cl;
        final String rn;
        final String cn;
        final boolean hasDimNames;
        final RAbstractStringVector axisNames;

        MatrixDimNames(RAbstractVector x) {
            dimnames = Utils.<RList> castTo(x.getAttr(dummyAttrProfiles, RRuntime.DIMNAMES_ATTR_KEY));

            if (dimnames == null) {
                rl = null;
                cl = null;
                rn = null;
                cn = null;
                hasDimNames = false;
                axisNames = null;
            } else {
                rl = getDimNamesAt(0);
                cl = getDimNamesAt(1);
                axisNames = Utils.<RAbstractStringVector> castTo(dimnames.getAttr(dummyAttrProfiles, RRuntime.NAMES_ATTR_KEY));
                if (axisNames == null) {
                    rn = null;
                    cn = null;
                } else {
                    rn = axisNames.getDataAt(0);
                    cn = axisNames.getDataAt(1);
                }
                hasDimNames = true;
            }

        }

        RAbstractStringVector getDimNamesAt(int dimLevel) {
            return dimLevel < dimnames.getLength() ? Utils.castTo(RRuntime.asAbstractVector(dimnames.getDataAt(dimLevel))) : null;
        }
    }
}
