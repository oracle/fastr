/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base.printer;

import java.io.IOException;

import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;

final class RawVectorPrinter extends VectorPrinter<RAbstractRawVector> {

    static final RawVectorPrinter INSTANCE = new RawVectorPrinter();

    private RawVectorPrinter() {
        // singleton
    }

    @Override
    protected RawVectorPrintJob createJob(RAbstractRawVector vector, int indx, PrintContext printCtx) {
        return new RawVectorPrintJob(vector, indx, printCtx);
    }

    private final class RawVectorPrintJob extends VectorPrintJob {

        protected RawVectorPrintJob(RAbstractRawVector vector, int indx, PrintContext printCtx) {
            super(vector, indx, printCtx);
        }

        @Override
        protected String elementTypeName() {
            return "raw";
        }

        @Override
        protected FormatMetrics formatVector(int offs, int len) {
            return new FormatMetrics(2);
        }

        @Override
        protected void printElement(int i, FormatMetrics fm) throws IOException {
            String rs = vector.getDataAt(i).toString();
            int gap = fm.maxWidth - 2;
            String fmt = "%" + Utils.asBlankArg(gap) + "s%s";
            String s = String.format(fmt, "", rs);
            printCtx.output().print(s);
        }

        @Override
        protected void printCell(int i, FormatMetrics fm) throws IOException {
            printElement(i, fm);
        }

        @Override
        protected void printEmptyVector() throws IOException {
            printCtx.output().print("raw(0)");
        }
    }
}
