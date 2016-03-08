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
import java.io.PrintWriter;
import java.io.Writer;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.r.nodes.builtin.base.Inherits;
import com.oracle.truffle.r.nodes.builtin.base.InheritsNodeGen;
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
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.nodes.RNode;

@SuppressWarnings("unused")
@NodeChildren({@NodeChild(value = "operand", type = RNode.class), @NodeChild(value = "digits", type = RNode.class), @NodeChild(value = "quote", type = RNode.class),
                @NodeChild(value = "naPrint", type = RNode.class), @NodeChild(value = "printGap", type = RNode.class), @NodeChild(value = "right", type = RNode.class),
                @NodeChild(value = "max", type = RNode.class), @NodeChild(value = "useSource", type = RNode.class), @NodeChild(value = "noOpt", type = RNode.class),
                @NodeChild(value = "max", type = RNode.class)
})
public abstract class ValuePrinterNode extends RNode {

    @Child IsArray isArrayBuiltIn = IsArrayNodeGen.create(null, null, null);
    @Child IsList isListBuiltIn = IsListNodeGen.create(null, null, null);
    @Child Inherits inheritsBuiltIn = InheritsNodeGen.create(null, null, null);
    @Child IsS4 isS4BuiltIn = IsS4NodeGen.create(null, null, null);
    @Child IsObject isObjectBuiltIn = IsObjectNodeGen.create(null, null, null);
    @Child IsMethodsDispatchOn isMethodDispatchOnBuiltIn = IsMethodsDispatchOnNodeGen.create(null, null, null);
    @Child CastStringNode castStringNode = CastStringNode.createNonPreserving();

    public boolean isArray(Object o) {
        return RRuntime.fromLogical(isArrayBuiltIn.execute(o));
    }

    public boolean isList(Object o) {
        return RRuntime.fromLogical(isListBuiltIn.execute(o));
    }

    public boolean inherits(Object o, Object what, byte which) {
        return RRuntime.fromLogical((Byte) inheritsBuiltIn.execute(o, what, which));
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

    public abstract Object executeString(VirtualFrame frame, Object o, Object digits, boolean quote, Object naPrint, Object printGap, boolean right, Object max, boolean useSource, boolean noOpt);

    // TODO: More specializations should be added

    @Specialization
    protected String prettyPrint(VirtualFrame frame, Object o, Object digits, boolean quote, Object naPrint, Object printGap, boolean right, Object max, boolean useSource, boolean noOpt) {
        // Until the new code is fully functional we have to use RBufferedWriter. In case
        // an exception is thrown by the new code, the content accumulated in the
        // RBufferedWriter is not printed and the old code is invoked to print the value. When
        // the new code stabilizes the RBufferedWriter will be replaced by RWriter.
        try (RWriter rw = new RWriter(); PrintWriter out = new PrintWriter(rw)) {
            prettyPrint(o, new PrintParameters(digits, quote, naPrint, printGap,
                            right, max, useSource, noOpt), out, frame);
            out.flush();
            // rw.commit();
            return null;
        } catch (IOException ex) {
            throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
        }

    }

    private String prettyPrint(Object o, PrintParameters printParams, PrintWriter out, VirtualFrame frame)
                    throws IOException {
        PrintContext printCtx = PrintContext.enter(this, printParams, out, frame);
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

}
