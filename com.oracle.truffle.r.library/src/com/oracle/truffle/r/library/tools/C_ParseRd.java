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
package com.oracle.truffle.r.library.tools;

import java.io.IOException;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.ffi.RFFIFactory;

public abstract class C_ParseRd extends RExternalBuiltinNode.Arg9 {

    @Specialization
    protected Object parseRd(RConnection con, REnvironment srcfile, String encoding, byte verboseL, RAbstractStringVector basename, byte fragmentL, byte warningCallsL,
                    byte macrosL, byte warndupsL) {
        return doParseRd(con, srcfile, encoding, verboseL, basename, fragmentL, warningCallsL, RDataFactory.createLogicalVectorFromScalar(macrosL), warndupsL);
    }

    @Specialization
    protected Object parseRd(RConnection con, REnvironment srcfile, String encoding, byte verboseL, RAbstractStringVector basename, byte fragmentL, byte warningCallsL,
                    REnvironment macros, byte warndupsL) {
        return doParseRd(con, srcfile, encoding, verboseL, basename, fragmentL, warningCallsL, macros, warndupsL);
    }

    private Object doParseRd(RConnection con, REnvironment srcfile, @SuppressWarnings("unused") String encoding, byte verboseL, RAbstractStringVector basename, byte fragmentL, byte warningCallsL,
                    Object macros, byte warndupsL) {
        if (RRuntime.isNA(warningCallsL)) {
            throw RError.error(this, RError.Message.INVALID_ARGUMENT, "warningCalls");
        }

        try (RConnection openConn = con.forceOpen("r")) {
            // @formatter:off
            return RFFIFactory.getRFFI().getToolsRFFI().parseRd(openConn, srcfile,
                            RDataFactory.createLogicalVectorFromScalar(verboseL),
                            RDataFactory.createLogicalVectorFromScalar(fragmentL),
                            RDataFactory.createStringVectorFromScalar(basename.getDataAt(0)),
                            RDataFactory.createLogicalVectorFromScalar(warningCallsL),
                            macros,
                            RDataFactory.createLogicalVectorFromScalar(warndupsL));
            // @formatter:on
        } catch (IOException ex) {
            throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
        } catch (Throwable ex) {
            throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
        }
    }

    @SuppressWarnings("unused")
    @Fallback
    public Object parseRd(Object con, Object srcfile, Object encoding, Object verbose, Object basename, Object fragment, Object warningCalls,
                    Object macros, Object warndupsL) {
        throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
    }
}
