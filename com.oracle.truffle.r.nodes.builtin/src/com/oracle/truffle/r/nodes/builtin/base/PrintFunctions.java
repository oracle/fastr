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

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.RInvisibleBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.Utils;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RTypedValue;

public class PrintFunctions {
    public abstract static class PrintAdapter extends RInvisibleBuiltinNode {
        @Child protected PrettyPrinterNode prettyPrinter = PrettyPrinterNodeGen.create(null, null, null, null, false);

        @TruffleBoundary
        protected void printHelper(String string) {
            try {
                StdConnections.getStdout().writeString(string, true);
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
            }
        }

    }

    @RBuiltin(name = "print.default", kind = INTERNAL, parameterNames = {"x", "digits", "quote", "na.print", "print.gap", "right", "max", "useSource", "noOpt"})
    public abstract static class PrintDefault extends PrintAdapter {

        private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

        @SuppressWarnings("unused")
        @Specialization(guards = "!isS4(o)")
        protected Object printDefault(Object o, Object digits, byte quote, Object naPrint, Object printGap, byte right, Object max, Object useSource, Object noOpt) {
            String s = (String) prettyPrinter.executeString(o, null, quote, right);
            if (s != null && !s.isEmpty()) {
                printHelper(s);
            }
            controlVisibility();
            return o;
        }

        ReadVariableNode createShowFind() {
            return ReadVariableNode.createFunctionLookup(null, "show");
        }

        RFunction createShowFunction(VirtualFrame frame, ReadVariableNode showFind) {
            return (RFunction) showFind.execute(frame);
        }

        DirectCallNode createCallNode(RFunction f) {
            return Truffle.getRuntime().createDirectCallNode(f.getTarget());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isS4(o)")
        protected Object printDefaultS4(VirtualFrame frame, RTypedValue o, Object digits, byte quote, Object naPrint, Object printGap, byte right, Object max, Object useSource, Object noOpt,
                        @Cached("createShowFind()") ReadVariableNode showFind, @Cached("createShowFunction(frame, showFind)") RFunction showFunction) {
            RContext.getEngine().evalFunction(showFunction, null, o);
            return null;
        }

        protected boolean isS4(Object o) {
            // chacking for class attribute is a bit of a hack but GNU R has a hack in place here as
            // well to avoid recursively calling show via print in showDefault (we just can't use
            // the same hack at this point - for details see definition of showDefault in show.R)
            return o instanceof RAttributable && ((RAttributable) o).isS4() && ((RAttributable) o).getClassAttr(attrProfiles) != null;
        }

    }

    @RBuiltin(name = "print.function", kind = INTERNAL, parameterNames = {"x", "useSource", "..."})
    public abstract static class PrintFunction extends PrintAdapter {
        @SuppressWarnings("unused")
        @Specialization
        protected RFunction printFunction(VirtualFrame frame, RFunction x, byte useSource, RArgsValuesAndNames extra) {
            String s = prettyPrinter.prettyPrintFunction(x, null, RRuntime.LOGICAL_FALSE, RRuntime.LOGICAL_FALSE, useSource == RRuntime.LOGICAL_TRUE);
            if (s != null && !s.isEmpty()) {
                printHelper(s);
            }
            controlVisibility();
            return x;
        }
    }
}
