/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RVisibility.CUSTOM;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RInterfaceCallbacks;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.ffi.REmbedRFFI.ReadConsoleNode;
import com.oracle.truffle.r.runtime.ffi.REmbedRFFI.WriteConsoleNode;
import com.oracle.truffle.r.runtime.ffi.REmbedRFFI.WriteErrConsoleNode;

/**
 * Internal builtins used from the C embedding mode.
 */
public class FastREmbedded {
    @RBuiltin(name = ".fastr.rinterface.callback.is.overridden", visibility = CUSTOM, kind = PRIMITIVE, parameterNames = {"name"}, behavior = PURE)
    public abstract static class RCallbackIsOverridden extends RBuiltinNode.Arg1 {
        static {
            Casts casts = new Casts(RCallbackIsOverridden.class);
            casts.arg("name").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        }

        @Specialization
        @TruffleBoundary
        protected Object doIt(String name) {
            return RInterfaceCallbacks.valueOf(name).isOverridden();
        }
    }

    @RBuiltin(name = ".fastr.rinterface.read.console", visibility = CUSTOM, kind = PRIMITIVE, parameterNames = {"prompt"}, behavior = PURE)
    public abstract static class ReadConsole extends RBuiltinNode.Arg1 {
        static {
            Casts casts = new Casts(ReadConsole.class);
            casts.arg("prompt").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        }

        @Specialization
        protected String doIt(String prompt,
                        @Cached ReadConsoleNode readConsoleNode) {
            return readConsoleNode.execute(prompt);
        }
    }

    @RBuiltin(name = ".fastr.rinterface.write.console", visibility = CUSTOM, kind = PRIMITIVE, parameterNames = {"text"}, behavior = PURE)
    public abstract static class WriteConsole extends RBuiltinNode.Arg1 {
        static {
            Casts casts = new Casts(WriteConsole.class);
            casts.arg("text").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        }

        @Specialization
        protected Object doIt(String text,
                        @Cached WriteConsoleNode writeConsoleNode) {
            writeConsoleNode.execute(text);
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".fastr.rinterface.write.err.console", visibility = CUSTOM, kind = PRIMITIVE, parameterNames = {"text"}, behavior = PURE)
    public abstract static class WriteErrConsole extends RBuiltinNode.Arg1 {
        static {
            Casts casts = new Casts(WriteErrConsole.class);
            casts.arg("text").mustBe(stringValue()).asStringVector().mustBe(singleElement()).findFirst();
        }

        @Specialization
        protected Object doIt(String text,
                        @Cached WriteErrConsoleNode writeConsoleNode) {
            writeConsoleNode.execute(text);
            return RNull.instance;
        }
    }
}
