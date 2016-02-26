/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.io.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

public class SerializeFunctions {

    public abstract static class Adapter extends RInvisibleBuiltinNode {
        @TruffleBoundary
        protected Object doUnserializeFromConnBase(RConnection conn, @SuppressWarnings("unused") REnvironment refhook) {
            controlVisibility();
            try (RConnection openConn = conn.forceOpen("rb")) {
                if (!openConn.canRead()) {
                    throw RError.error(this, RError.Message.CONNECTION_NOT_OPEN_READ);
                }
                Object result = RSerialize.unserialize(openConn);
                return result;
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
            }
        }

        @TruffleBoundary
        protected Object doSerializeToConnBase(Object object, RConnection conn, byte asciiLogical, byte xdrLogical, @SuppressWarnings("unused") RNull version,
                        @SuppressWarnings("unused") RNull refhook) {
            controlVisibility();
            boolean ascii = RRuntime.fromLogical(asciiLogical);
            // xdr is only relevant if ascii is false
            try (RConnection openConn = conn.forceOpen(ascii ? "wt" : "wb")) {
                if (!openConn.canWrite()) {
                    throw RError.error(this, RError.Message.CONNECTION_NOT_OPEN_WRITE);
                }
                if (!ascii && openConn.isTextMode()) {
                    throw RError.error(this, RError.Message.BINARY_CONNECTION_REQUIRED);
                }
                RSerialize.serialize(openConn, object, ascii, RRuntime.fromLogical(xdrLogical), RSerialize.DEFAULT_VERSION, null);
                return RNull.instance;
            } catch (IOException ex) {
                throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
            }
        }
    }

    @RBuiltin(name = "unserializeFromConn", kind = INTERNAL, parameterNames = {"conn", "refhook"})
    public abstract static class UnserializeFromConn extends Adapter {
        @Specialization
        protected Object doUnserializeFromConn(RConnection conn, @SuppressWarnings("unused") RNull refhook) {
            return doUnserializeFromConnBase(conn, null);
        }

        @Specialization
        protected Object doUnserializeFromConn(RConnection conn, @SuppressWarnings("unused") REnvironment refhook) {
            // TODO figure out what this really means?
            return doUnserializeFromConnBase(conn, null);
        }
    }

    @RBuiltin(name = "serializeToConn", kind = INTERNAL, parameterNames = {"object", "conn", "ascii", "version", "refhook"})
    public abstract static class SerializeToConn extends Adapter {
        @Specialization
        protected Object doSerializeToConn(Object object, RConnection conn, byte asciiLogical, RNull version, RNull refhook) {
            return doSerializeToConnBase(object, conn, asciiLogical, RRuntime.LOGICAL_NA, version, refhook);
        }

    }

    @RBuiltin(name = "unserialize", kind = INTERNAL, parameterNames = {"conn", "refhook"})
    public abstract static class Unserialize extends Adapter {
        @SuppressWarnings("unused")
        @Specialization
        protected Object unSerialize(RConnection conn, RNull refhook) {
            return doUnserializeFromConnBase(conn, null);
        }
    }

    @RBuiltin(name = "serialize", kind = INTERNAL, parameterNames = {"object", "conn", "ascii", "version", "refhook"})
    public abstract static class Serialize extends Adapter {
        @Specialization
        protected Object serialize(Object object, RConnection conn, byte asciiLogical, RNull version, RNull refhook) {
            return doSerializeToConnBase(object, conn, asciiLogical, RRuntime.LOGICAL_NA, version, refhook);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object serialize(Object object, RNull conn, byte asciiLogical, RNull version, RNull refhook) {
            byte[] data = RSerialize.serialize(object, RRuntime.fromLogical(asciiLogical), false, RSerialize.DEFAULT_VERSION, null);
            return RDataFactory.createRawVector(data);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected Object serialize(Object object, Object conn, Object asciiLogical, Object version, Object refhook) {
            throw RError.error(this, RError.Message.INVALID_OR_UNIMPLEMENTED_ARGUMENTS);
        }
    }

    @RBuiltin(name = "serializeb", kind = INTERNAL, parameterNames = {"object", "conn", "xdr", "version", "refhook"})
    public abstract static class SerializeB extends Adapter {
        @Specialization
        protected Object serializeB(Object object, RConnection conn, byte xdrLogical, RNull version, RNull refhook) {
            if (!RRuntime.fromLogical(xdrLogical)) {
                throw RError.nyi(this, "xdr==FALSE");
            }
            return doSerializeToConnBase(object, conn, RRuntime.LOGICAL_FALSE, xdrLogical, version, refhook);
        }
    }
}
