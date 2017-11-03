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
package com.oracle.truffle.r.nodes.builtin.base.printer;

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
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
import com.oracle.truffle.r.runtime.interop.TruffleObjectConverter;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public final class ValuePrinterNode extends RBaseNode {

    @Child private IsArray isArrayBuiltIn = IsArrayNodeGen.create();
    @Child private IsList isListBuiltIn = IsListNodeGen.create();
    @Child private InheritsBuiltin inheritsBuiltinBuiltIn = InheritsBuiltinNodeGen.create();
    @Child private IsS4 isS4BuiltIn = IsS4NodeGen.create();
    @Child private IsObject isObjectBuiltIn = IsObjectNodeGen.create();
    @Child private IsMethodsDispatchOn isMethodDispatchOnBuiltIn = IsMethodsDispatchOnNodeGen.create();
    @Child private CastStringNode castStringNode = CastStringNode.createNonPreserving();
    @Child private BoxPrimitiveNode boxPrimitiveNode = BoxPrimitiveNode.create();

    @Child private ConvertTruffleObjectNode convertTruffleObject;

    /**
     * This node inspects non-R {@link TruffleObject}s and tries to create wrappers for them that
     * mimic R data structures.
     */
    public static final class ConvertTruffleObjectNode extends Node {

        private final TruffleObjectConverter converter;

        public ConvertTruffleObjectNode() {
            converter = new TruffleObjectConverter();
            insert(converter.getSubNodes());
        }

        @TruffleBoundary
        public Object convert(TruffleObject obj) {
            return converter.convert(obj);
        }

    }

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

    private Object convertTruffleObject(Object o) {
        if (RRuntime.isForeignObject(o)) {
            if (convertTruffleObject == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                convertTruffleObject = insert(new ConvertTruffleObjectNode());
            }
            return convertTruffleObject.convert((TruffleObject) o);
        }
        return null;
    }

    public String execute(Object o, Object digits, boolean quote, Object naPrint, Object printGap, boolean right, Object max, boolean useSource, boolean noOpt) {
        try {
            PrintParameters printParams = new PrintParameters(digits, quote, naPrint, printGap, right, max, useSource, noOpt);

            PrintContext printCtx = PrintContext.enter(this, printParams, RWriter::new);
            try {
                prettyPrint(o, printCtx);
                Object foreignObjectWrapper = convertTruffleObject(o);
                if (foreignObjectWrapper != null) {
                    prettyPrint(foreignObjectWrapper, printCtx);
                }
            } finally {
                PrintContext.leave();
            }
            return null;
        } catch (IOException ex) {
            throw RError.ioError(this, ex);
        }
    }

    @TruffleBoundary
    private static void prettyPrint(Object o, PrintContext printCtx) throws IOException {
        ValuePrinters.INSTANCE.print(o, printCtx);
        ValuePrinters.printNewLine(printCtx);
    }

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
}
