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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.nodes.binary.BoxPrimitiveNode;
import com.oracle.truffle.r.nodes.builtin.base.InheritsBuiltin;
import com.oracle.truffle.r.nodes.builtin.base.InheritsBuiltinNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.IsMethodsDispatchOn;
import com.oracle.truffle.r.nodes.builtin.base.IsMethodsDispatchOnNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.IsS4;
import com.oracle.truffle.r.nodes.builtin.base.IsS4NodeGen;
import com.oracle.truffle.r.nodes.builtin.base.IsTypeFunctions.IsArray;
import com.oracle.truffle.r.nodes.builtin.base.IsTypeFunctions.IsList;
import com.oracle.truffle.r.nodes.builtin.base.IsTypeFunctions.IsObject;
import com.oracle.truffle.r.nodes.builtin.base.IsTypeFunctionsFactory.IsArrayNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.IsTypeFunctionsFactory.IsListNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.IsTypeFunctionsFactory.IsObjectNodeGen;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public abstract class ValuePrinterNode extends RBaseNode {

    @Child private IsArray isArrayBuiltIn = IsArrayNodeGen.create(null);
    @Child private IsList isListBuiltIn = IsListNodeGen.create(null);
    @Child private InheritsBuiltin inheritsBuiltinBuiltIn = InheritsBuiltinNodeGen.create(null);
    @Child private IsS4 isS4BuiltIn = IsS4NodeGen.create(null);
    @Child private IsObject isObjectBuiltIn = IsObjectNodeGen.create(null);
    @Child private IsMethodsDispatchOn isMethodDispatchOnBuiltIn = IsMethodsDispatchOnNodeGen.create(null);
    @Child private CastStringNode castStringNode = CastStringNode.createNonPreserving();
    @Child private BoxPrimitiveNode boxPrimitiveNode = BoxPrimitiveNode.create();

    public boolean isArray(Object o) {
        return RRuntime.fromLogical(isArrayBuiltIn.execute(o));
    }

    public boolean isList(Object o) {
        return RRuntime.fromLogical(isListBuiltIn.execute(o));
    }

    public boolean inherits(Object o, Object what) {
        return RRuntime.fromLogical((Byte) inheritsBuiltinBuiltIn.execute(o, what, false));
    }

    public boolean isS4(Object o) {
        return RRuntime.fromLogical(isS4BuiltIn.execute(o));
    }

    public boolean isObject(Object o) {
        return RRuntime.fromLogical(isObjectBuiltIn.execute(o));
    }

    public boolean isMethodDispatchOn() {
        return RRuntime.fromLogical(isMethodDispatchOnBuiltIn.execute());
    }

    public String castString(Object o) {
        return (String) castStringNode.executeString(o);
    }

    public Object boxPrimitive(Object o) {
        return boxPrimitiveNode.execute(o);
    }

    public abstract Object executeString(Object o, Object digits, boolean quote, Object naPrint, Object printGap, boolean right, Object max, boolean useSource, boolean noOpt);

    @TruffleBoundary
    public Object prettyPrint(Object v, WriterFactory wf) {
        PrintParameters printParams = new PrintParameters();
        printParams.setDefaults();
        printParams.setSuppressIndexLabels(true);
        PrintContext printCtx = PrintContext.enter(this, printParams, wf);
        try {
            ValuePrinters.INSTANCE.print(v, printCtx);
            return printCtx.output().getPrintReport();
        } catch (IOException ex) {
            throw RError.ioError(this, ex);
        } finally {
            PrintContext.leave();
        }
    }

    @Specialization
    protected String prettyPrint(Object o, Object digits, boolean quote, Object naPrint, Object printGap, boolean right, Object max, boolean useSource, boolean noOpt) {
        try {
            prettyPrint(o, new PrintParameters(digits, quote, naPrint, printGap,
                            right, max, useSource, noOpt), RWriter::new);
            return null;
        } catch (IOException ex) {
            throw RError.ioError(this, ex);
        }
    }

    private String prettyPrint(Object o, PrintParameters printParams, WriterFactory wf)
                    throws IOException {
        PrintContext printCtx = PrintContext.enter(this, printParams, wf);
        try {
            prettyPrint(o, printCtx);
            return null;
        } finally {
            PrintContext.leave();
        }
    }

    @TruffleBoundary
    private static void prettyPrint(Object o, PrintContext printCtx) throws IOException {
        ValuePrinters.INSTANCE.println(o, printCtx);
    }

    public static String prettyPrint(final Object value) {
        return (String) Truffle.getRuntime().createCallTarget(new RootNode(TruffleLanguage.class, null, null) {

            @Child ValuePrinterNode valuePrinterNode = ValuePrinterNodeGen.create();

            @Override
            public Object execute(VirtualFrame frame) {
                return valuePrinterNode.prettyPrint(value, AnyVectorToStringVectorWriter::new);
            }
        }).call(value);
    }

}
