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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.data.RNull;

public class SinkFunctions {
    @RBuiltin(name = "sink", visibility = OFF, kind = INTERNAL, parameterNames = {"file", "closeOnExit", "type", "split"}, behavior = IO)
    public abstract static class Sink extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("file").mustBe(instanceOf(RConnection.class).or(integerValue()));
            casts.arg("closeOnExit").asLogicalVector().findFirst().notNA().map(toBoolean());
            casts.arg("type").asLogicalVector().findFirst().notNA().map(toBoolean());
            casts.arg("split").asLogicalVector().findFirst().notNA().map(toBoolean());
        }

        @Specialization
        @TruffleBoundary
        protected RNull sink(RConnection conn, boolean closeOnExit, boolean errcon, boolean split) {
            if (split) {
                // TODO
                throw RError.nyi(this, "split");
            }
            if (errcon) {
                StdConnections.divertErr(conn);
            } else {
                StdConnections.pushDivertOut(conn, closeOnExit);
            }
            return RNull.instance;
        }

        @SuppressWarnings("unused")
        @Specialization
        @TruffleBoundary
        protected RNull sink(int conn, boolean closeOnExit, boolean isMessage, boolean split) {
            try {
                StdConnections.popDivertOut();
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
            }
            return RNull.instance;
        }

    }

    @RBuiltin(name = "sink.number", kind = INTERNAL, parameterNames = {"type"}, behavior = IO)
    public abstract static class SinkNumber extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("type").asLogicalVector().findFirst().notNA().map(toBoolean());
        }

        @Specialization
        @TruffleBoundary
        protected int sinkNumber(boolean isOutput) {
            if (isOutput) {
                return StdConnections.stdoutDiversions();
            } else {
                return StdConnections.stderrDiversion();
            }
        }
    }
}
