/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.asIntegerVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.eq;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.findFirst;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.rawValue;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.io.IOException;
import java.util.function.Function;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.NodeWithArgumentCasts.Casts;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.RSerialize.VersionInfo;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.RConnection;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public class SerializeFunctions {

    private static void checkCanRead(RConnection openConn, RBaseNode node) {
        if (!openConn.canRead()) {
            throw node.error(Message.CONNECTION_NOT_OPEN_READ);
        }
    }

    @TruffleBoundary
    protected static Object doUnserializeFromConnBase(RBaseNode node, int connIndex, @SuppressWarnings("unused") REnvironment refhook) {
        try (RConnection openConn = RConnection.fromIndex(connIndex).forceOpen("rb")) {
            checkCanRead(openConn, node);
            return RSerialize.unserialize(openConn);
        } catch (IOException ex) {
            throw node.error(RError.Message.GENERIC, ex.getMessage());
        }
    }

    @TruffleBoundary
    protected static VersionInfo doUnserializeInfoFromConn(RBaseNode node, int connIndex) {
        try (RConnection openConn = RConnection.fromIndex(connIndex).forceOpen("rb")) {
            checkCanRead(openConn, node);
            return RSerialize.unserializeInfo(openConn);
        } catch (IOException ex) {
            throw node.error(RError.Message.GENERIC, ex.getMessage());
        }
    }

    @TruffleBoundary
    protected static Object doUnserializeFromRaw(RAbstractRawVector data, @SuppressWarnings("unused") REnvironment refhook) {
        return RSerialize.unserialize(data);
    }

    @TruffleBoundary
    protected static Object doSerializeToConnBase(RBaseNode node, Object object, int connIndex, int type, int version) {
        // xdr is only relevant if ascii is false
        try (RConnection openConn = RConnection.fromIndex(connIndex).forceOpen(type != RSerialize.XDR ? "wt" : "wb")) {
            if (!openConn.canWrite()) {
                throw node.error(RError.Message.CONNECTION_NOT_OPEN_WRITE);
            }
            if (type == RSerialize.XDR && openConn.isTextMode()) {
                throw node.error(RError.Message.BINARY_CONNECTION_REQUIRED);
            }
            RSerialize.serialize(RContext.getInstance(), openConn, object, type, version, null);
            return RNull.instance;
        } catch (IOException ex) {
            throw node.error(RError.Message.GENERIC, ex.getMessage());
        }
    }

    protected static void connection(Casts casts) {
        casts.arg("con").mustBe(integerValue()).asIntegerVector().findFirst();
    }

    private static void version(Casts casts) {
        // must be either default NULL or 2 or 3
        casts.arg("version").allowNull().asIntegerVector().findFirst().mustBe(eq(2).or(eq(3)), Message.VERSION_N_NOT_SUPPORTED, (Function<Object, Object>) n -> n);
    }

    @RBuiltin(name = "unserializeFromConn", kind = INTERNAL, parameterNames = {"con", "refhook"}, behavior = IO)
    public abstract static class UnserializeFromConn extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(UnserializeFromConn.class);
            connection(casts);
        }

        @Specialization
        protected Object doUnserializeFromConn(int conn, @SuppressWarnings("unused") RNull refhook) {
            return doUnserializeFromConnBase(this, conn, null);
        }

        @Specialization
        protected Object doUnserializeFromConn(int conn, @SuppressWarnings("unused") REnvironment refhook) {
            // TODO figure out what this really means?
            return doUnserializeFromConnBase(this, conn, null);
        }
    }

    @RBuiltin(name = "serializeInfoFromConn", kind = INTERNAL, parameterNames = {"con"}, behavior = IO)
    public abstract static class SerializeInfoFromConn extends RBuiltinNode.Arg1 {

        static {
            Casts casts = new Casts(SerializeInfoFromConn.class);
            connection(casts);
        }

        @Specialization
        protected Object getSerializeInfoFromConn(int conn) {
            return doUnserializeInfoFromConn(this, conn).toVector();
        }
    }

    @RBuiltin(name = "serializeToConn", visibility = OFF, kind = INTERNAL, parameterNames = {"object", "con", "ascii", "version", "refhook"}, behavior = IO)
    public abstract static class SerializeToConn extends RBuiltinNode.Arg5 {

        static {
            Casts casts = new Casts(SerializeToConn.class);
            casts.arg("object").mustNotBeMissing();
            connection(casts);
            casts.arg("ascii").mustBe(logicalValue(), RError.Message.ASCII_NOT_LOGICAL).asLogicalVector().findFirst();
            version(casts);
            casts.arg("refhook").mustNotBeMissing();
        }

        @Specialization
        protected Object doSerializeToConn(Object object, int conn, byte asciiLogical, int version, @SuppressWarnings("unused") RNull refhook) {
            return serialize(asciiLogical, object, conn, version);
        }

        @Specialization
        protected Object doSerializeToConn(Object object, int conn, byte asciiLogical, @SuppressWarnings("unused") RNull version, @SuppressWarnings("unused") RNull refhook) {
            return serialize(asciiLogical, object, conn, RSerialize.DEFAULT_VERSION);
        }

        private Object serialize(byte asciiLogical, Object object, int conn, int version) {
            int type;
            if (asciiLogical == RRuntime.LOGICAL_NA) {
                type = RSerialize.ASCII_HEX;
            } else if (asciiLogical == RRuntime.LOGICAL_TRUE) {
                type = RSerialize.ASCII;
            } else {
                type = RSerialize.XDR;
            }
            return doSerializeToConnBase(this, object, conn, type, version);
        }
    }

    @RBuiltin(name = "unserialize", kind = INTERNAL, parameterNames = {"con", "refhook"}, behavior = IO)
    public abstract static class Unserialize extends RBuiltinNode.Arg2 {

        static {
            Casts casts = new Casts(Unserialize.class);
            casts.arg("con").defaultError(Message.INVALID_CONNECTION).mustBe(integerValue().or(rawValue())).mapIf(integerValue(),
                            asIntegerVector().setNext(findFirst().integerElement()));
        }

        @Specialization
        protected Object unSerialize(int conn, @SuppressWarnings("unused") RNull refhook) {
            return doUnserializeFromConnBase(this, conn, null);
        }

        @Specialization
        protected Object unSerialize(RAbstractRawVector data, @SuppressWarnings("unused") RNull refhook) {
            return doUnserializeFromRaw(data, null);
        }
    }

    @RBuiltin(name = "serialize", kind = INTERNAL, parameterNames = {"object", "con", "type", "version", "refhook"}, behavior = IO)
    public abstract static class Serialize extends RBuiltinNode.Arg5 {

        static {
            Casts casts = new Casts(Serialize.class);
            casts.arg("con").allowNull().mustBe(integerValue()).asIntegerVector().findFirst();
            casts.arg("type").asIntegerVector().findFirst();
            version(casts);
        }

        @Specialization
        protected Object serialize(Object object, int conn, int type, @SuppressWarnings("unused") int version, @SuppressWarnings("unused") RNull refhook) {
            return doSerializeToConnBase(this, object, conn, type, version);
        }

        @Specialization
        protected Object serialize(Object object, int conn, int type, @SuppressWarnings("unused") RNull version, @SuppressWarnings("unused") RNull refhook) {
            return doSerializeToConnBase(this, object, conn, type, RSerialize.DEFAULT_VERSION);
        }

        @Specialization
        protected Object serialize(Object object, @SuppressWarnings("unused") RNull conn, int type, int version, @SuppressWarnings("unused") RNull refhook) {
            return serialize(object, type, version);
        }

        @Specialization
        protected Object serialize(Object object, @SuppressWarnings("unused") RNull conn, int type, @SuppressWarnings("unused") RNull version, @SuppressWarnings("unused") RNull refhook) {
            return serialize(object, type, RSerialize.DEFAULT_VERSION);
        }

        private static Object serialize(Object object, int type, int version) {
            byte[] data = RSerialize.serialize(RContext.getInstance(), object, type, version, null);
            return RDataFactory.createRawVector(data);
        }
    }

    @RBuiltin(name = "serializeb", kind = INTERNAL, parameterNames = {"object", "con", "xdr", "version", "refhook"}, behavior = IO)
    public abstract static class SerializeB extends RBuiltinNode.Arg5 {

        static {
            Casts casts = new Casts(SerializeB.class);
            connection(casts);
            casts.arg("xdr").asLogicalVector().findFirst();
            version(casts);
        }

        @Specialization
        protected Object serializeB(Object object, int conn, byte xdrLogical, int version, @SuppressWarnings("unused") RNull refhook) {
            return serialize(xdrLogical, object, conn, version);
        }

        @Specialization
        protected Object serializeB(Object object, int conn, byte xdrLogical, @SuppressWarnings("unused") RNull version, @SuppressWarnings("unused") RNull refhook) {
            return serialize(xdrLogical, object, conn, RSerialize.DEFAULT_VERSION);
        }

        private Object serialize(byte xdrLogical, Object object, int conn, int version) throws RError {
            if (!RRuntime.fromLogical(xdrLogical)) {
                throw RError.nyi(this, "xdr==FALSE");
            }
            return doSerializeToConnBase(this, object, conn, RRuntime.LOGICAL_FALSE, version);
        }
    }
}
