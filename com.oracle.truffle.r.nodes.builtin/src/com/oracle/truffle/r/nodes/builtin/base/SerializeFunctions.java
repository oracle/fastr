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
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

public class SerializeFunctions {

    // TODO This code needs to check for unopened connections as per GnuR,
    // which requires more publicly visible support in RConnection

    @RBuiltin(name = "unserializeFromConn", kind = INTERNAL, parameterNames = {"conn", "refhook"})
    public abstract static class UnserializeFromConn extends RInvisibleBuiltinNode {
        @Specialization
        protected Object doUnserializeFromConn(VirtualFrame frame, RConnection conn, @SuppressWarnings("unused") RNull refhook) {
            return doUnserializeFromConn(conn, null, RArguments.getDepth(frame));
        }

        @TruffleBoundary
        protected Object doUnserializeFromConn(RConnection conn, @SuppressWarnings("unused") REnvironment refhook, int depth) {
            controlVisibility();
            boolean wasOpen = true;
            try {
                wasOpen = conn.forceOpen("rb");
                if (!conn.canRead()) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.CONNECTION_NOT_OPEN_READ);
                }
                Object result = RSerialize.unserialize(conn, depth);
                return result;
            } catch (IOException ex) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.GENERIC, ex.getMessage());
            } finally {
                if (!wasOpen) {
                    try {
                        conn.internalClose();
                    } catch (IOException ex) {
                        throw RError.error(getEncapsulatingSourceSection(), RError.Message.GENERIC, ex.getMessage());
                    }
                }
            }
        }

        @Specialization
        protected Object doUnserializeFromConn(VirtualFrame frame, RConnection conn, @SuppressWarnings("unused") REnvironment refhook) {
            // TODO figure out what this really means?
            return doUnserializeFromConn(frame, conn, RNull.instance);
        }
    }

    @RBuiltin(name = "serializeToConn", kind = INTERNAL, parameterNames = {"object", "conn", "ascii", "version", "refhook"})
    public abstract static class SerializeToConn extends RInvisibleBuiltinNode {
        @Specialization
        protected Object doSerializeToConn(VirtualFrame frame, Object object, RConnection conn, byte asciiLogical, RNull version, RNull refhook) {
            return doSerializeToConn(object, conn, asciiLogical, version, refhook, RArguments.getDepth(frame));
        }

        @TruffleBoundary
        protected Object doSerializeToConn(Object object, RConnection conn, byte asciiLogical, @SuppressWarnings("unused") RNull version, @SuppressWarnings("unused") RNull refhook, int depth) {
            controlVisibility();
            boolean wasOpen = true;
            try {
                boolean ascii = RRuntime.fromLogical(asciiLogical);
                wasOpen = conn.forceOpen("wb");
                if (!conn.canWrite()) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.CONNECTION_NOT_OPEN_WRITE);
                }
                if (!ascii && conn.isTextMode()) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.BINARY_CONNECTION_REQUIRED);
                }
                RSerialize.serialize(conn, object, ascii, 2, null, depth);
                return RNull.instance;
            } catch (IOException ex) {
                throw RError.error(getEncapsulatingSourceSection(), RError.Message.GENERIC, ex.getMessage());
            } finally {
                if (!wasOpen) {
                    try {
                        conn.internalClose();
                    } catch (IOException ex) {
                        throw RError.error(getEncapsulatingSourceSection(), RError.Message.GENERIC, ex.getMessage());
                    }
                }
            }
        }
    }

}
