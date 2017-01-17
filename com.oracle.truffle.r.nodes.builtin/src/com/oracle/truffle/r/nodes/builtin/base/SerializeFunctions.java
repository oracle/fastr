/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asIntegerVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.findFirst;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.rawValue;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

public class SerializeFunctions {

    public abstract static class Adapter extends RBuiltinNode {
        @TruffleBoundary
        protected Object doUnserializeFromConnBase(int connIndex, @SuppressWarnings("unused") REnvironment refhook) {
            try (RConnection openConn = RConnection.fromIndex(connIndex).forceOpen("rb")) {
                if (!openConn.canRead()) {
                    throw RError.error(this, RError.Message.CONNECTION_NOT_OPEN_READ);
                }
                return RSerialize.unserialize(openConn);
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
            }
        }

        @TruffleBoundary
        protected Object doUnserializeFromRaw(RAbstractRawVector data, @SuppressWarnings("unused") REnvironment refhook) {
            return RSerialize.unserialize(data);
        }

        @TruffleBoundary
        protected Object doSerializeToConnBase(Object object, int connIndex, int type, @SuppressWarnings("unused") byte xdrLogical, @SuppressWarnings("unused") RNull version,
                        @SuppressWarnings("unused") RNull refhook) {
            // xdr is only relevant if ascii is false
            try (RConnection openConn = RConnection.fromIndex(connIndex).forceOpen(type != RSerialize.XDR ? "wt" : "wb")) {
                if (!openConn.canWrite()) {
                    throw RError.error(this, RError.Message.CONNECTION_NOT_OPEN_WRITE);
                }
                if (type == RSerialize.XDR && openConn.isTextMode()) {
                    throw RError.error(this, RError.Message.BINARY_CONNECTION_REQUIRED);
                }
                RSerialize.serialize(openConn, object, type, RSerialize.DEFAULT_VERSION, null);
                return RNull.instance;
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
            }
        }

        protected void connection(CastBuilder casts) {
            casts.arg("con").mustNotBeNull().mustBe(integerValue()).asIntegerVector().findFirst();
        }
    }

    @RBuiltin(name = "unserializeFromConn", kind = INTERNAL, parameterNames = {"con", "refhook"}, behavior = IO)
    public abstract static class UnserializeFromConn extends Adapter {
        @Override
        protected void createCasts(CastBuilder casts) {
            connection(casts);
        }

        @Specialization
        protected Object doUnserializeFromConn(int conn, @SuppressWarnings("unused") RNull refhook) {
            return doUnserializeFromConnBase(conn, null);
        }

        @Specialization
        protected Object doUnserializeFromConn(int conn, @SuppressWarnings("unused") REnvironment refhook) {
            // TODO figure out what this really means?
            return doUnserializeFromConnBase(conn, null);
        }
    }

    @RBuiltin(name = "serializeToConn", visibility = OFF, kind = INTERNAL, parameterNames = {"object", "con", "ascii", "version", "refhook"}, behavior = IO)
    public abstract static class SerializeToConn extends Adapter {
        @Override
        protected void createCasts(CastBuilder casts) {
            connection(casts);
            casts.arg("ascii").mustBe(logicalValue(), RError.Message.ASCII_NOT_LOGICAL);
            casts.arg("version").allowNull().mustBe(integerValue());
        }

        @Specialization
        protected Object doSerializeToConn(Object object, int conn, byte asciiLogical, RNull version, RNull refhook) {
            int type;
            if (asciiLogical == RRuntime.LOGICAL_NA) {
                type = RSerialize.ASCII_HEX;
            } else if (asciiLogical == RRuntime.LOGICAL_TRUE) {
                type = RSerialize.ASCII;
            } else {
                type = RSerialize.XDR;
            }
            return doSerializeToConnBase(object, conn, type, RRuntime.LOGICAL_NA, version, refhook);
        }
    }

    @RBuiltin(name = "unserialize", kind = INTERNAL, parameterNames = {"con", "refhook"}, behavior = IO)
    public abstract static class Unserialize extends Adapter {
        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("con").defaultError(Message.INVALID_CONNECTION).mustNotBeNull().mustBe(integerValue().or(rawValue())).mapIf(integerValue(),
                            asIntegerVector().setNext(findFirst().integerElement()));
        }

        @Specialization
        protected Object unSerialize(int conn, @SuppressWarnings("unused") RNull refhook) {
            return doUnserializeFromConnBase(conn, null);
        }

        @Specialization
        protected Object unSerialize(RAbstractRawVector data, @SuppressWarnings("unused") RNull refhook) {
            return doUnserializeFromRaw(data, null);
        }
    }

    @RBuiltin(name = "serialize", kind = INTERNAL, parameterNames = {"object", "con", "type", "version", "refhook"}, behavior = IO)
    public abstract static class Serialize extends Adapter {
        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("con").allowNull().mustBe(integerValue()).asIntegerVector().findFirst();
            casts.arg("type").asIntegerVector().findFirst();
        }

        @Specialization
        protected Object serialize(Object object, int conn, int type, RNull version, RNull refhook) {
            return doSerializeToConnBase(object, conn, type, RRuntime.LOGICAL_NA, version, refhook);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object serialize(Object object, RNull conn, int type, RNull version, RNull refhook) {
            byte[] data = RSerialize.serialize(object, type, RSerialize.DEFAULT_VERSION, null);
            return RDataFactory.createRawVector(data);
        }
    }

    @RBuiltin(name = "serializeb", kind = INTERNAL, parameterNames = {"object", "con", "xdr", "version", "refhook"}, behavior = IO)
    public abstract static class SerializeB extends Adapter {
        @Override
        protected void createCasts(CastBuilder casts) {
            connection(casts);
            casts.arg("xdr").asLogicalVector().findFirst();
        }

        @Specialization
        protected Object serializeB(Object object, int conn, byte xdrLogical, RNull version, RNull refhook) {
            if (!RRuntime.fromLogical(xdrLogical)) {
                throw RError.nyi(this, "xdr==FALSE");
            }
            return doSerializeToConnBase(object, conn, RRuntime.LOGICAL_FALSE, xdrLogical, version, refhook);
        }
    }
}
