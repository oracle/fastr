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

import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.canBeDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.canBeIntVector;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.canBeLogicalVector;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.canBeStringVector;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.snprintf;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.toComplexVector;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.toDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.toIntVector;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.toLogicalVector;
import static com.oracle.truffle.r.nodes.builtin.base.printer.Utils.toStringVector;

import java.io.IOException;

import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;

//Transcribed from GnuR, src/main/print.c

public final class PairListPrinter extends AbstractValuePrinter<RPairList> {

    public static final PairListPrinter INSTANCE = new PairListPrinter();

    private static RAttributeProfiles dummyAttrProfiles = RAttributeProfiles.create();

    @Override
    protected void printValue(RPairList s, PrintContext printCtx) throws IOException {
        final RAbstractIntVector dims = Utils.<RAbstractIntVector> castTo(
                        s.getAttr(dummyAttrProfiles, RRuntime.DIM_ATTR_KEY));

        final int ns = s.getLength();

        if (dims != null && dims.getLength() > 1) {
            String[] t = new String[ns];
            for (int i = 0; i < ns; i++) {
                Object tmp = s.getDataAtAsObject(i);
                final String pbuf;
                if (tmp == null || tmp == RNull.instance) {
                    pbuf = RRuntime.NULL;
                } else if (canBeLogicalVector(tmp)) {
                    pbuf = snprintf(115, "Logical,%d", toLogicalVector(tmp).getLength());
                } else if (canBeIntVector(tmp)) {
                    pbuf = snprintf(115, "Integer,%d", toIntVector(tmp).getLength());
                } else if (canBeDoubleVector(tmp)) {
                    pbuf = snprintf(115, "Numeric,%d", toDoubleVector(tmp).getLength());
                } else if (tmp instanceof RAbstractComplexVector) {
                    pbuf = snprintf(115, "Complex,%d", toComplexVector(tmp).getLength());
                } else if (canBeStringVector(tmp)) {
                    pbuf = snprintf(115, "Character,%d", toStringVector(tmp).getLength());
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

                RStringVector tt = RDataFactory.createStringVector(t, true, s.getDimensions());
                Object dimNames = s.getAttr(dummyAttrProfiles, RRuntime.DIMNAMES_ATTR_KEY);
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
