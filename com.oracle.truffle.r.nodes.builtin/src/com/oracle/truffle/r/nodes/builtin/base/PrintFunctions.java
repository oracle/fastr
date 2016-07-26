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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.printer.PrintParameters;
import com.oracle.truffle.r.nodes.builtin.base.printer.ValuePrinterNode;
import com.oracle.truffle.r.nodes.builtin.base.printer.ValuePrinterNodeGen;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.RTypedValue;

public class PrintFunctions {

    @RBuiltin(name = "print.default", visibility = RVisibility.OFF, kind = INTERNAL, parameterNames = {"x", "digits", "quote", "na.print", "print.gap", "right", "max", "useSource", "noOpt"})
    public abstract static class PrintDefault extends RBuiltinNode {

        private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

        @Child private ValuePrinterNode valuePrinter = ValuePrinterNodeGen.create();

        @Override
        protected void createCasts(CastBuilder casts) {
            super.createCasts(casts);
            casts.firstBoolean(2);
            casts.firstBoolean(5);
            casts.firstBoolean(7);
            casts.firstBoolean(8);
        }

        @TruffleBoundary
        @Specialization(guards = "!isS4(o)")
        protected Object printDefault(Object o, Object digits, boolean quote, Object naPrint, Object printGap, boolean right, Object max, boolean useSource, boolean noOpt) {
            valuePrinter.executeString(o, digits, quote, naPrint, printGap, right, max, useSource, noOpt);
            return o;
        }

        protected static RFunction createShowFunction(VirtualFrame frame) {
            return ReadVariableNode.lookupFunction("show", frame, false);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isS4(o)")
        protected Object printDefaultS4(VirtualFrame frame, RTypedValue o, Object digits, boolean quote, Object naPrint, Object printGap, boolean right, Object max, boolean useSource, boolean noOpt, //
                        @Cached("createShowFunction(frame)") RFunction showFunction) {
            if (noOpt) {
                // S4 should only be called in case noOpt is true
                RContext.getEngine().evalFunction(showFunction, null, null, null, o);
            } else {
                printDefault(showFunction, digits, quote, naPrint, printGap, right, max, useSource, noOpt);
            }
            return null;
        }

        protected boolean isS4(Object o) {
            return o instanceof RAttributable && ((RAttributable) o).isS4() && ((RAttributable) o).getClassAttr(attrProfiles) != null;
        }
    }

    @RBuiltin(name = "print.function", visibility = RVisibility.OFF, kind = INTERNAL, parameterNames = {"x", "useSource", "..."})
    public abstract static class PrintFunction extends RBuiltinNode {

        @Child private ValuePrinterNode valuePrinter = ValuePrinterNodeGen.create();

        @SuppressWarnings("unused")
        @Specialization
        protected RFunction printFunction(RFunction x, byte useSource, RArgsValuesAndNames extra) {
            valuePrinter.executeString(x, PrintParameters.getDefaultDigits(), true, RString.valueOf(RRuntime.STRING_NA), 1, false, PrintParameters.getDefaultMaxPrint(), true, false);
            return x;
        }
    }
}
