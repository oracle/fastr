/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.constant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notIntNA;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullConstant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.runtime.data.nodes.attributes.SpecialAttributesFunctions.GetClassAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.printer.ValuePrinterNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPairList;

public class PrintFunctions {

    /**
     * {@code print.default} pre R 3.6.0 took all the arguments as actual arguments and not in a
     * pairlist. The current implementation of {@code print.default} unrolls the pairlist and passes
     * it to this node, which allows us to keep the cast pipeline and specializations.
     */
    public abstract static class OldPrintDefault extends RBuiltinNode.Arg8 {

        @Child private GetClassAttributeNode getClassNode = GetClassAttributeNode.create();

        @Child private ValuePrinterNode valuePrinter = new ValuePrinterNode();

        static {
            Casts casts = new Casts(OldPrintDefault.class);
            casts.arg(1).mapMissing(nullConstant()).allowNull().defaultError(RError.Message.INVALID_ARGUMENT, "digits").asIntegerVector().findFirst().mustBe(notIntNA()).mustBe(
                            gte(Format.R_MIN_DIGITS_OPT).and(lte(Format.R_MAX_DIGITS_OPT)));
            casts.arg(2).mapMissing(constant(RRuntime.LOGICAL_TRUE)).asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
            casts.arg(3).mapMissing(nullConstant()).defaultError(RError.Message.INVALID_NA_PRINT_SPEC).allowNull().mustBe(stringValue()).asStringVector().findFirst();
            casts.arg(4).mapMissing(nullConstant()).defaultError(RError.Message.GAP_MUST_BE_NON_NEGATIVE).allowNull().asIntegerVector().findFirst().mustBe(notIntNA()).mustBe(gte(0));
            casts.arg(5).mapMissing(constant(RRuntime.LOGICAL_FALSE)).defaultError(RError.Message.INVALID_ARGUMENT, "right").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
            casts.arg(6).mapMissing(nullConstant()).allowNull().asIntegerVector().findFirst().mustBe(notIntNA()).mustBe(gte(0));
            casts.arg(7).mapMissing(constant(RRuntime.LOGICAL_TRUE)).defaultError(RError.Message.INVALID_ARGUMENT, "useSource").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
        }

        @Specialization(guards = "!isS4(o)")
        protected Object printDefault(Object o, Object digits, boolean quote, Object naPrint, Object printGap, boolean right, Object max, boolean useSource) {
            valuePrinter.execute(o, digits, quote, naPrint, printGap, right, max, useSource);
            return o;
        }

        protected static RFunction createShowFunction(VirtualFrame frame) {
            return ReadVariableNode.lookupFunction("show", frame);
        }

        @Specialization(guards = "isS4(o)")
        protected Object printDefaultS4(@SuppressWarnings("unused") VirtualFrame frame, RBaseObject o, Object digits, boolean quote, Object naPrint, Object printGap, boolean right, Object max,
                        boolean useSource,
                        @Cached("createShowFunction(frame)") RFunction showFunction) {
            // TODO: the same as above
            boolean noOpt = true;
            if (noOpt) {
                // S4 should only be called in case noOpt is true
                RContext.getEngine().evalFunction(showFunction, null, null, true, null, o);
            } else {
                printDefault(showFunction, digits, quote, naPrint, printGap, right, max, useSource);
            }
            return o;
        }

        protected boolean isS4(Object o) {
            return o instanceof RAttributable && ((RAttributable) o).isS4() && getClassNode.getClassAttr((RAttributable) o) != null;
        }
    }

    @RBuiltin(name = "print.default", visibility = OFF, kind = INTERNAL, parameterNames = {"x", "args", "missing"}, behavior = IO)
    public abstract static class PrintDefault extends RBuiltinNode.Arg3 {
        private static final int OLD_PRINT_ARGS_SIZE = 7;

        static {
            Casts casts = new Casts(PrintDefault.class);
            casts.arg("args").mustBe(RPairList.class);
            casts.arg("missing").mustBe(logicalValue()).asLogicalVector();
        }

        @Specialization
        @ExplodeLoop
        protected Object print(VirtualFrame frame, Object x, RPairList argsIn, RLogicalVector missing,
                        @Cached OldPrintDefault oldPrintDefault) {
            // convert the pairlist to array, check missing too
            Object[] argsArr = new Object[OLD_PRINT_ARGS_SIZE];
            Object args = argsIn;
            int i = 0;
            while (!RRuntime.isNull(args)) {
                RPairList nextNode = (RPairList) args;
                argsArr[i] = RRuntime.fromLogical(missing.getDataAt(i)) ? RMissing.instance : nextNode.car();
                args = nextNode.cdr();
                i++;
                if (i >= OLD_PRINT_ARGS_SIZE) {
                    // From the documentation: further arguments in "..." are ignored
                    break;
                }
            }

            padArgsWithMissing(argsArr, i);

            return oldPrintDefault.call(frame, x, argsArr[0], argsArr[1], argsArr[2], argsArr[3], argsArr[4], argsArr[5], argsArr[6]);
        }

        private static void padArgsWithMissing(Object[] argsArr, int i) {
            // Pad with missing (noOpt seems to be now optional passed in ...)
            for (int j = i; j < OLD_PRINT_ARGS_SIZE; j++) {
                argsArr[j] = RMissing.instance;
            }
        }
    }
}
