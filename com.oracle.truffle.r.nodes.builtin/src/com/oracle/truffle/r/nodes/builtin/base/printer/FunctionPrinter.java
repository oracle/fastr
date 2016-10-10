/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.io.PrintWriter;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.nodes.RRootNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.function.FormalArguments;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.builtins.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RNode;

final class FunctionPrinter extends AbstractValuePrinter<RFunction> {

    static final FunctionPrinter INSTANCE = new FunctionPrinter();

    private FunctionPrinter() {
        // singleton
    }

    @Override
    @TruffleBoundary
    protected void printValue(RFunction operand, PrintContext printCtx) throws IOException {
        final PrintWriter out = printCtx.output();
        final PrintContext valPrintCtx = printCtx.cloneContext();
        // suppress the "[1] "
        valPrintCtx.parameters().setSuppressIndexLabels(true);

        if (operand.isBuiltin()) {
            RBuiltinDescriptor rBuiltin = operand.getRBuiltin();
            RRootNode node = (RRootNode) operand.getTarget().getRootNode();
            FormalArguments formals = node.getFormalArguments();
            out.print("function (");
            ArgumentsSignature signature = formals.getSignature();
            for (int i = 0; i < signature.getLength(); i++) {
                RNode defaultArg = formals.getDefaultArgument(i);
                out.print(signature.getName(i));
                if (defaultArg != null) {
                    out.print(" = ");
                    Object value = ((ConstantNode) defaultArg).getValue();
                    ValuePrinters.INSTANCE.print(value, valPrintCtx);
                }
                if (i != signature.getLength() - 1) {
                    out.print(", ");
                }
            }
            out.print(")  .Primitive(\"");
            out.print(rBuiltin.getName());
            out.print("\")");
        } else {
            final boolean useSource = printCtx.parameters().getUseSource();
            String source = ((RRootNode) operand.getTarget().getRootNode()).getSourceCode();
            if (source == null || !useSource) {
                source = RDeparse.deparse(operand);
            }
            REnvironment env = RArguments.getEnvironment(operand.getEnclosingFrame());
            if (env != null && env.isNamespaceEnv()) {
                source += "\n" + env.getPrintName();
            }
            out.print(source);
        }
    }
}
