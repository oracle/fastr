/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RCaller;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;

/**
 * Prints a "stack trace" of RCallers, i.e., iterates over frames and prints RCaller from each
 * frame. Note that this is different than {@link FastRStackTrace} because {@link FastRStackTrace}
 * outputs "system stack trace" that should be equivalent to sys.frames().
 */
@RBuiltin(name = ".fastr.rcallertrace", visibility = OFF, kind = PRIMITIVE, behavior = COMPLEX, parameterNames = {})
public abstract class FastRRCallerTrace extends RBuiltinNode.Arg0 {
    public static FastRRCallerTrace create() {
        return FastRRCallerTraceNodeGen.create();
    }

    @Child InteropLibrary interopLibrary = InteropLibrary.getUncached();

    @Specialization
    protected RNull printRCallerTrace(VirtualFrame frame) {
        assert RArguments.isRFrame(frame);
        RCaller caller = RArguments.getCall(frame);
        RContext.ConsoleIO console = RContext.getInstance().getConsole();
        console.println(rcallerToNestedString(caller, 0));
        return RNull.instance;
    }

    @TruffleBoundary
    private String rcallerToNestedString(RCaller caller, int indentDepth) {
        StringBuilder sb = new StringBuilder();
        sb.append(createIndentString(indentDepth));
        sb.append(String.format("RCaller (@%s):\n", Integer.toHexString(caller.hashCode())));
        sb.append(createIndentString(indentDepth));
        sb.append("depth = ").append(caller.getDepth()).append("\n");
        sb.append(createIndentString(indentDepth));
        sb.append("visibility = ").append(caller.getVisibility()).append("\n");
        sb.append(createIndentString(indentDepth));
        sb.append("isPromise = ").append(caller.isPromise()).append("\n");
        sb.append(createIndentString(indentDepth));
        sb.append("isValidCaller = ").append(caller.isValidCaller()).append("\n");

        sb.append(createIndentString(indentDepth));
        sb.append("promise = ");
        RPromise promise = caller.getPromise();
        if (promise == null) {
            sb.append("null");
        } else {
            Object displayStringObject = interopLibrary.toDisplayString(promise, false);
            assert displayStringObject instanceof String;
            sb.append((String) displayStringObject);
        }
        sb.append("\n");

        sb.append(createIndentString(indentDepth));
        sb.append("payload = ");
        if (caller.isPromise()) {
            RCaller parent = caller.getLogicalParent();
            if (parent == null) {
                sb.append("null");
            } else {
                sb.append("\n");
                sb.append(rcallerToNestedString(parent, indentDepth + 1));
            }
        } else {
            if (caller.isValidCaller()) {
                RSyntaxElement syntaxElement = caller.getSyntaxNode();
                assert syntaxElement != null;
                sb.append(syntaxElementToString(syntaxElement));
            } else {
                sb.append("null");
            }
        }
        sb.append("\n");

        sb.append(createIndentString(indentDepth));
        sb.append("previous = ");
        RCaller previous = caller.getPrevious();
        if (previous == null) {
            sb.append("null");
        } else {
            sb.append("\n");
            sb.append(rcallerToNestedString(previous, indentDepth + 1));
        }

        return sb.toString();
    }

    private static String createIndentString(int indentDepth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indentDepth; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }

    private static String syntaxElementToString(RSyntaxElement syntaxElement) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        String syntaxNodeText = syntaxElement.toString();
        if (syntaxNodeText.contains("\n")) {
            // Append only the first line
            int firstNewLineIdx = syntaxNodeText.indexOf("\n");
            assert firstNewLineIdx > 0;
            sb.append(syntaxNodeText, 0, firstNewLineIdx);
            sb.append(" ...");
        } else {
            sb.append(syntaxNodeText);
        }
        sb.append("}");
        return sb.toString();
    }
}
