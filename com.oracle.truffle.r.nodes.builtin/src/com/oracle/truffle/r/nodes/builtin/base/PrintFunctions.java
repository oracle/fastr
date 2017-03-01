/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notIntNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetClassAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.printer.PrintParameters;
import com.oracle.truffle.r.nodes.builtin.base.printer.ValuePrinterNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RTypedValue;

public class PrintFunctions {

    @RBuiltin(name = "print.default", visibility = OFF, kind = INTERNAL, parameterNames = {"x", "digits", "quote", "na.print", "print.gap", "right", "max", "useSource", "noOpt"}, behavior = IO)
    public abstract static class PrintDefault extends RBuiltinNode {

        @Child private GetClassAttributeNode getClassNode = GetClassAttributeNode.create();

        @Child private ValuePrinterNode valuePrinter = new ValuePrinterNode();

        static {
            Casts casts = new Casts(PrintDefault.class);
            casts.arg("digits").allowNull().asIntegerVector().findFirst().mustBe(notIntNA()).mustBe(gte(Format.R_MIN_DIGITS_OPT).and(lte(Format.R_MAX_DIGITS_OPT)));

            casts.arg("quote").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());

            casts.arg("na.print").defaultError(RError.Message.INVALID_NA_PRINT_SPEC).allowNull().mustBe(stringValue()).asStringVector().findFirst();

            casts.arg("print.gap").defaultError(RError.Message.GAP_MUST_BE_NON_NEGATIVE).allowNull().asIntegerVector().findFirst().mustBe(notIntNA()).mustBe(gte(0));

            casts.arg("right").defaultError(RError.Message.INVALID_ARGUMENT, "right").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());

            casts.arg("max").allowNull().asIntegerVector().findFirst().mustBe(notIntNA()).mustBe(gte(0));

            casts.arg("useSource").defaultError(RError.Message.INVALID_ARGUMENT, "useSource").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());

            casts.arg("noOpt").defaultError(RError.Message.GENERIC, "invalid 'tryS4' internal argument").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
        }

        @Specialization(guards = "!isS4(o)")
        protected Object printDefault(Object o, Object digits, boolean quote, Object naPrint, Object printGap, boolean right, Object max, boolean useSource, boolean noOpt) {
            valuePrinter.execute(o, digits, quote, naPrint, printGap, right, max, useSource, noOpt);
            return o;
        }

        protected static RFunction createShowFunction(VirtualFrame frame) {
            return ReadVariableNode.lookupFunction("show", frame);
        }

        @Specialization(guards = "isS4(o)")
        protected Object printDefaultS4(@SuppressWarnings("unused") VirtualFrame frame, RTypedValue o, Object digits, boolean quote, Object naPrint, Object printGap, boolean right, Object max,
                        boolean useSource, boolean noOpt,
                        @Cached("createShowFunction(frame)") RFunction showFunction) {
            if (noOpt) {
                // S4 should only be called in case noOpt is true
                RContext.getEngine().evalFunction(showFunction, null, null, null, o);
            } else {
                printDefault(showFunction, digits, quote, naPrint, printGap, right, max, useSource, noOpt);
            }
            return o;
        }

        protected boolean isS4(Object o) {
            return o instanceof RAttributable && ((RAttributable) o).isS4() && getClassNode.getClassAttr(o) != null;
        }
    }

    @RBuiltin(name = "print.function", visibility = OFF, kind = INTERNAL, parameterNames = {"x", "useSource", "..."}, behavior = IO)
    public abstract static class PrintFunction extends RBuiltinNode {

        @Child private ValuePrinterNode valuePrinter = new ValuePrinterNode();

        static {
            Casts casts = new Casts(PrintFunction.class);
            casts.arg("x").mustBe(instanceOf(RFunction.class));

            casts.arg("useSource").defaultError(RError.Message.INVALID_ARGUMENT, "useSource").asLogicalVector().findFirst(RRuntime.LOGICAL_FALSE).map(toBoolean());
        }

        @Specialization
        protected RFunction printFunction(RFunction x, boolean useSource, @SuppressWarnings("unused") RArgsValuesAndNames extra) {
            valuePrinter.execute(x, PrintParameters.getDefaultDigits(), true, RRuntime.STRING_NA, 1, false, PrintParameters.getDefaultMaxPrint(), useSource, false);
            return x;
        }
    }
}
