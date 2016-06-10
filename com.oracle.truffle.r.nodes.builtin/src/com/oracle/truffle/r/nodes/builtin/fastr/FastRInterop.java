/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import java.io.IOException;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.context.Engine;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RTypedValue;

public class FastRInterop {
    @RBuiltin(name = ".fastr.interop.eval", visibility = RVisibility.OFF, kind = RBuiltinKind.PRIMITIVE, parameterNames = {"mimeType", "source"})
    public abstract static class Eval extends RBuiltinNode {

        @Specialization
        @TruffleBoundary
        protected Object interopEval(Object mimeType, Object source) {
            Source sourceObject = Source.fromText(RRuntime.asString(source), Engine.EVAL_FUNCTION_NAME).withMimeType(RRuntime.asString(mimeType));

            CallTarget callTarget;

            try {
                callTarget = RContext.getInstance().getEnv().parse(sourceObject);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return callTarget.call();
        }
    }

    @RBuiltin(name = ".fastr.interop.export", visibility = RVisibility.OFF, kind = RBuiltinKind.PRIMITIVE, parameterNames = {"name", "value"})
    public abstract static class Export extends RBuiltinNode {

        @Specialization
        @TruffleBoundary
        protected Object exportSymbol(Object name, RTypedValue value) {
            String stringName = RRuntime.asString(name);
            if (stringName == null) {
                throw RError.error(this, RError.Message.INVALID_ARG_TYPE, "name");
            }
            RContext.getInstance().getExportedSymbols().put(stringName, value);
            return RNull.instance;
        }
    }

    @RBuiltin(name = ".fastr.interop.import", visibility = RVisibility.OFF, kind = RBuiltinKind.PRIMITIVE, parameterNames = {"name"})
    public abstract static class Import extends RBuiltinNode {

        @Specialization
        @TruffleBoundary
        protected Object importSymbol(Object name) {
            String stringName = RRuntime.asString(name);
            if (stringName == null) {
                throw RError.error(this, RError.Message.INVALID_ARG_TYPE, "name");
            }
            Object object = RContext.getInstance().getEnv().importSymbol(stringName);
            if (object == null) {
                throw RError.error(this, RError.Message.NO_IMPORT_OBJECT, stringName);
            }
            return object;
        }
    }

}
