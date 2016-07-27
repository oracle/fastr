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
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RTypedValue;

public class FastRInterop {
    @RBuiltin(name = ".fastr.interop.eval", visibility = RVisibility.OFF, kind = RBuiltinKind.PRIMITIVE, parameterNames = {"mimeType", "source"})
    public abstract static class Eval extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.firstStringWithError(0, Message.INVALID_ARGUMENT, "mimeType");
            casts.firstStringWithError(1, Message.INVALID_ARGUMENT, "source");
        }

        protected CallTarget parse(String mimeType, String source) {
            CompilerAsserts.neverPartOfCompilation();

            Source sourceObject = RSource.fromTextInternal(source, RSource.Internal.EVAL_WRAPPER, mimeType);
            try {
                emitIO();
                return RContext.getInstance().getEnv().parse(sourceObject);
            } catch (IOException e) {
                throw RError.error(this, Message.GENERIC, "Error while parsing: " + e.getMessage());
            }
        }

        protected DirectCallNode createCall(String mimeType, String source) {
            return Truffle.getRuntime().createDirectCallNode(parse(mimeType, source));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"cachedMimeType != null", "cachedMimeType.equals(mimeType)", "cachedSource != null", "cachedSource.equals(source)"})
        protected Object evalCached(VirtualFrame frame, String mimeType, String source, //
                        @Cached("mimeType") String cachedMimeType, //
                        @Cached("source") String cachedSource, //
                        @Cached("createCall(mimeType, source)") DirectCallNode call) {
            return call.call(frame, EMPTY_OBJECT_ARRAY);
        }

        @Specialization(contains = "evalCached")
        @TruffleBoundary
        protected Object eval(String mimeType, String source) {
            return parse(mimeType, source).call();
        }

        @SuppressWarnings("unused")
        private void emitIO() throws IOException {
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
