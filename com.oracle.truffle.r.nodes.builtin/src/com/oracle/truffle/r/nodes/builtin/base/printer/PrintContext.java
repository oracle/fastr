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

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RInternalError;

final class PrintContext {
    private final ValuePrinterNode pn;
    private final PrintParameters params;
    private final PrettyPrintWriter out;
    private final Map<String, Object> attrs = new HashMap<>();
    private StringBuilder tagbuf;

    private static final ThreadLocal<ArrayDeque<PrintContext>> printCtxTL = new ThreadLocal<>();

    private PrintContext(ValuePrinterNode printerNode, PrintParameters parameters, PrettyPrintWriter output) {
        this.pn = printerNode;
        this.params = parameters;
        this.out = output;
    }

    /**
     * TagBuffer is part of {@link PrintContext} and represents a prefix that should be printed
     * before any value. This value reflects global variable {@code tagbuf} in GnuR. In FastR we can
     * have more parallel buffers.
     */
    public StringBuilder getOrCreateTagBuffer() {
        if (tagbuf == null) {
            tagbuf = new StringBuilder();
        }
        return tagbuf;
    }

    /**
     * Version of {@link #getOrCreateTagBuffer()} that does not create the buffer if it is
     * {@code null}.
     */
    public StringBuilder getTagBuffer() {
        return tagbuf;
    }

    public void resetTagBuffer() {
        tagbuf = null;
    }

    public void setTagBuffer(StringBuilder buffer) {
        tagbuf = buffer;
    }

    public PrintParameters parameters() {
        return params;
    }

    public ValuePrinterNode printerNode() {
        return pn;
    }

    public PrettyPrintWriter output() {
        return out;
    }

    public Object getAttribute(String attrName) {
        return attrs.get(attrName);
    }

    public void setAttribute(String attrName, Object attrValue) {
        attrs.put(attrName, attrValue);
    }

    public PrintContext cloneContext() {
        PrintContext cloned = new PrintContext(pn, params.cloneParameters(), out);
        cloned.attrs.putAll(attrs);
        return cloned;
    }

    @TruffleBoundary
    static PrintContext enter(ValuePrinterNode printerNode, PrintParameters parameters, WriterFactory wf) {
        ArrayDeque<PrintContext> ctxStack = printCtxTL.get();
        if (ctxStack == null) {
            ctxStack = new ArrayDeque<>();
            printCtxTL.set(ctxStack);
            PrintContext ctx = new PrintContext(printerNode, parameters, new PrettyPrintWriter(wf.createWriter()));
            ctxStack.push(ctx);
            return ctx;
        } else {
            PrintContext parentCtx = ctxStack.peek();
            PrintContext ctx = new PrintContext(printerNode, parameters, parentCtx.output());
            ctx.attrs.putAll(parentCtx.attrs);
            ctxStack.push(ctx);
            return ctx;
        }
    }

    @TruffleBoundary
    static PrintContext leave() {
        ArrayDeque<PrintContext> ctxStack = printCtxTL.get();

        RInternalError.guarantee(ctxStack != null, "No pretty-printer context stack");
        RInternalError.guarantee(!ctxStack.isEmpty(), "Pretty-printer context stack is empty");

        PrintContext ctx = ctxStack.pop();

        if (ctxStack.isEmpty()) {
            ctx.out.close();
            printCtxTL.remove();
        }

        return ctx;
    }
}
