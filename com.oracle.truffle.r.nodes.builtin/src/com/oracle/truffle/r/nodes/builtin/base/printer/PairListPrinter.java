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

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

//Transcribed from GnuR, src/main/print.c

final class PairListPrinter extends AbstractValuePrinter<RPairList> {

    static final PairListPrinter INSTANCE = new PairListPrinter();

    private PairListPrinter() {
        // singleton
    }

    @Override
    @TruffleBoundary
    protected void printValue(RPairList s, PrintContext printCtx) throws IOException {
        final RIntVector dims = Utils.<RIntVector> castTo(
                        s.getAttr(RRuntime.DIM_ATTR_KEY));

        final int ns = s.getLength();

        if (dims != null && dims.getLength() > 1) {
            String[] t = new String[ns];
            for (int i = 0; i < ns; i++) {
                Object tmp = RRuntime.asAbstractVector(s.getDataAtAsObject(i));
                final String pbuf;
                if (tmp == null || tmp == RNull.instance) {
                    pbuf = RRuntime.NULL;
                } else if (tmp instanceof RLogicalVector) {
                    pbuf = "Logical," + ((RAbstractContainer) tmp).getLength();
                } else if (tmp instanceof RIntVector) {
                    pbuf = "Integer," + ((RAbstractContainer) tmp).getLength();
                } else if (tmp instanceof RDoubleVector) {
                    pbuf = "Numeric," + ((RAbstractContainer) tmp).getLength();
                } else if (tmp instanceof RComplexVector) {
                    pbuf = "Complex," + ((RAbstractContainer) tmp).getLength();
                } else if (tmp instanceof RAbstractStringVector) {
                    pbuf = "Character," + ((RAbstractContainer) tmp).getLength();
                } else if (tmp instanceof RRawVector) {
                    pbuf = "Raw," + ((RAbstractContainer) tmp).getLength();
                } else if (tmp instanceof RAbstractListVector) {
                    pbuf = "List," + ((RAbstractContainer) tmp).getLength();
                } else if ((tmp instanceof RPairList && ((RPairList) tmp).isLanguage())) {
                    pbuf = "Expression";
                } else {
                    pbuf = "?";
                }

                t[i] = pbuf;

                RStringVector tt = RDataFactory.createStringVector(t, true, s.getDimensions());
                Object dimNames = s.getAttr(RRuntime.DIMNAMES_ATTR_KEY);
                tt.setAttr(RRuntime.DIMNAMES_ATTR_KEY, dimNames);

                PrintContext cc = printCtx.cloneContext();
                cc.parameters().setQuote(false);
                StringVectorPrinter.INSTANCE.print(tt, cc);
            }
        } else {
            // no dim()
            ListPrinter.printNoDimList(s.toRList(), printCtx);
        }
    }
}
