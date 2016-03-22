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

import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RExternalPtr;
import com.oracle.truffle.r.runtime.data.RFactor;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RS4Object;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractComplexVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public final class ValuePrinters implements ValuePrinter<Object> {

    private final Map<Class<?>, ValuePrinter<?>> printers = new HashMap<>();

    public static final ValuePrinters INSTANCE = new ValuePrinters();

    private ValuePrinters() {
        printers.put(RNull.class, NullPrinter.INSTANCE);
        printers.put(RSymbol.class, SymbolPrinter.INSTANCE);
        printers.put(RFunction.class, FunctionPrinter.INSTANCE);
        printers.put(RExpression.class, ExpressionPrinter.INSTANCE);
        printers.put(RLanguage.class, LanguagePrinter.INSTANCE);
        printers.put(RExternalPtr.class, ExternalPtrPrinter.INSTANCE);
        printers.put(RS4Object.class, S4ObjectPrinter.INSTANCE);
        printers.put(RPairList.class, PairListPrinter.INSTANCE);
        printers.put(RFactor.class, FactorPrinter.INSTANCE);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void print(Object v, PrintContext printCtx) throws IOException {
        RInternalError.guarantee(v != null, "Unexpected null value");

        if (v == RNull.instance) {
            NullPrinter.INSTANCE.print(null, printCtx);
        } else {
            // Try to box a scalar primitive value to the respective vector
            Object x = printCtx.printerNode().boxPrimitive(v);
            ValuePrinter printer = printers.get(x.getClass());
            if (printer == null) {
                if (x instanceof RAbstractStringVector) {
                    printer = StringVectorPrinter.INSTANCE;
                } else if (x instanceof RAbstractDoubleVector) {
                    printer = DoubleVectorPrinter.INSTANCE;
                } else if (x instanceof RAbstractIntVector) {
                    printer = IntegerVectorPrinter.INSTANCE;
                } else if (x instanceof RAbstractLogicalVector) {
                    printer = LogicalVectorPrinter.INSTANCE;
                } else if (x instanceof RAbstractComplexVector) {
                    printer = ComplexVectorPrinter.INSTANCE;
                } else if (x instanceof RAbstractRawVector) {
                    printer = RawVectorPrinter.INSTANCE;
                } else if (x instanceof RAbstractListVector) {
                    printer = ListPrinter.INSTANCE;
                } else if (x instanceof REnvironment) {
                    printer = EnvironmentPrinter.INSTANCE;
                } else {
                    RInternalError.shouldNotReachHere();
                }
            }
            printer.print(x, printCtx);
        }
    }

}
