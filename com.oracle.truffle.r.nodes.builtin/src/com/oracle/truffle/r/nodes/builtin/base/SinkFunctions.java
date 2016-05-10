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
package com.oracle.truffle.r.nodes.builtin.base;

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RVisibility;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

public class SinkFunctions {
    @RBuiltin(name = "sink", visibility = RVisibility.OFF, kind = RBuiltinKind.INTERNAL, parameterNames = {"file", "closeOnExit", "isMessage", "split"})
    public abstract static class Sink extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected RNull sink(RConnection conn, byte closeOnExit, byte isMessage, @SuppressWarnings("unused") byte split) {
            if (RRuntime.fromLogical(isMessage)) {
                // TODO
            } else {
                StdConnections.pushDivertOut(conn, RRuntime.fromLogical(closeOnExit));
            }
            controlVisibility();
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected RNull sink(RAbstractIntVector conn, byte closeOnExit, byte isMessage, byte split) {
            try {
                StdConnections.popDivertOut();
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
            }
            controlVisibility();
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Fallback
        protected RNull sink(Object conn, Object closeOnExit, Object isMessage, Object split) {
            throw RError.error(this, RError.Message.INVALID_UNNAMED_ARGUMENTS);
        }
    }

    @RBuiltin(name = "sink.number", kind = RBuiltinKind.INTERNAL, parameterNames = {"type"})
    public abstract static class SinkNumber extends RBuiltinNode {
        @Specialization
        @TruffleBoundary
        protected int sinkNumber(byte isOutput) {
            if (RRuntime.fromLogical(isOutput)) {
                return StdConnections.stdoutDiversions();
            } else {
                return StdConnections.stderrDiversion();
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        protected RNull sinkNumbner(Object type) {
            throw RError.error(this, RError.Message.INVALID_ARGUMENT);
        }
    }
}
