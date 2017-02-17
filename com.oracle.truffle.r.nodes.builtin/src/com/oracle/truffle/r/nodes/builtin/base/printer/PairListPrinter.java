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

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
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

final class PairListPrinter extends AbstractValuePrinter<RPairList> {

    static final PairListPrinter INSTANCE = new PairListPrinter();

    private PairListPrinter() {
        // singleton
    }

    @Override
    @TruffleBoundary
    protected void printValue(RPairList s, PrintContext printCtx) throws IOException {
        final RAbstractIntVector dims = Utils.<RAbstractIntVector> castTo(
                        s.getAttr(RRuntime.DIM_ATTR_KEY));

        final int ns = s.getLength();

        if (dims != null && dims.getLength() > 1) {
            String[] t = new String[ns];
            for (int i = 0; i < ns; i++) {
                Object tmp = RRuntime.asAbstractVector(s.getDataAtAsObject(i));
                final String pbuf;
                if (tmp == null || tmp == RNull.instance) {
                    pbuf = RRuntime.NULL;
                } else if (tmp instanceof RAbstractLogicalVector) {
                    pbuf = "Logical," + ((RAbstractContainer) tmp).getLength();
                } else if (tmp instanceof RAbstractIntVector) {
                    pbuf = "Integer," + ((RAbstractContainer) tmp).getLength();
                } else if (tmp instanceof RAbstractDoubleVector) {
                    pbuf = "Numeric," + ((RAbstractContainer) tmp).getLength();
                } else if (tmp instanceof RAbstractComplexVector) {
                    pbuf = "Complex," + ((RAbstractContainer) tmp).getLength();
                } else if (tmp instanceof RAbstractStringVector) {
                    pbuf = "Character," + ((RAbstractContainer) tmp).getLength();
                } else if (tmp instanceof RAbstractRawVector) {
                    pbuf = "Raw," + ((RAbstractContainer) tmp).getLength();
                } else if (tmp instanceof RAbstractListVector) {
                    pbuf = "List," + ((RAbstractContainer) tmp).getLength();
                } else if (tmp instanceof RLanguage) {
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
