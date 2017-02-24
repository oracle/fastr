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
package com.oracle.truffle.r.library.tools;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.ffi.AsIntegerNode;
import com.oracle.truffle.r.nodes.ffi.AsLogicalNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;
import com.oracle.truffle.r.runtime.ffi.ToolsRFFI;

public abstract class C_ParseRd extends RExternalBuiltinNode.Arg9 {
    @Child ToolsRFFI.ParseRdNode parseRdNode = RFFIFactory.getRFFI().getToolsRFFI().createParseRdNode();

    static {
        Casts casts = new Casts(C_ParseRd.class);
        /*
         * Most arguments require coercion using, e.g., asLogical; N.B. GNU R doesn't check
         * everything, e.g., srcfile. Since this is "internal" code we do not really expect argument
         * errors.
         */
        casts.arg(1, "srcfile").mustBe(instanceOf(REnvironment.class));
        casts.arg(3, "verbose").mustBe(logicalValue()).asLogicalVector();
        casts.arg(4, "basename").mustBe(stringValue()).asStringVector();
    }

    @TruffleBoundary
    @Specialization
    protected Object parseRd(Object conObj, REnvironment srcfile, @SuppressWarnings("unused") Object encoding, RAbstractLogicalVector verbose, RAbstractStringVector basename, Object fragmentObj,
                    Object warningCallsObj, Object macros, Object warndupsObj,
                    @Cached("create()") AsIntegerNode conAsInteger,
                    @Cached("create()") AsLogicalNode fragmentAsLogical,
                    @Cached("create()") AsLogicalNode warningCallsAsLogical,
                    @Cached("create()") AsLogicalNode warnDupsAsLogical) {
        int con = conAsInteger.execute(conObj);
        int warningCalls = warningCallsAsLogical.execute(warningCallsObj);
        if (RRuntime.isNA(warningCalls)) {
            throw error(RError.Message.INVALID_VALUE, "warningCalls");
        }
        int fragment = fragmentAsLogical.execute(fragmentObj);
        int warndups = warnDupsAsLogical.execute(warndupsObj);

        // fromIndex checks validity of con and throws error if not
        try (RConnection openConn = RConnection.fromIndex(con).forceOpen("r")) {
            // @formatter:off
            return parseRdNode.execute(openConn, srcfile,
                            verbose.materialize(),
                            RDataFactory.createLogicalVectorFromScalar((byte) fragment),
                            basename.materialize(),
                            RDataFactory.createLogicalVectorFromScalar((byte) warningCalls),
                            macros,
                            RDataFactory.createLogicalVectorFromScalar((byte) warndups));
            // @formatter:on
        } catch (Throwable ex) {
            throw error(RError.Message.GENERIC, ex.getMessage());
        }
    }

    @SuppressWarnings("unused")
    @Fallback
    public Object parseRd(Object con, Object srcfile, Object encoding, Object verbose, Object basename, Object fragment, Object warningCalls,
                    Object macros, Object warndupsL) {
        throw error(RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
    }
}
