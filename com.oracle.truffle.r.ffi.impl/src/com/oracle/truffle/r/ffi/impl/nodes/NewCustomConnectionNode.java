/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nodes;

import static com.oracle.truffle.r.runtime.data.NativeDataAccess.readNativeString;

import java.io.IOException;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.InvalidConnection;
import com.oracle.truffle.r.runtime.conn.NativeConnections.NativeRConnection;
import com.oracle.truffle.r.runtime.data.RExternalPtr;

public abstract class NewCustomConnectionNode extends FFIUpCallNode.Arg4 {

    public static NewCustomConnectionNode create() {
        return NewCustomConnectionNodeGen.create();
    }

    @Specialization
    Object handleStrings(String description, String mode, String className, RExternalPtr connAddr) {
        try {
            return new NativeRConnection(description, mode, className, connAddr).asVector();
        } catch (IOException e) {
            return InvalidConnection.instance.asVector();
        }
    }

    protected static Node createAsPointerNode() {
        return Message.AS_POINTER.createNode();
    }

    @Specialization
    Object handleAddresses(TruffleObject description, TruffleObject mode, TruffleObject className, RExternalPtr connAddr,
                    @Cached("createAsPointerNode()") Node descriptionAsPtrNode, @Cached("createAsPointerNode()") Node modeAsPtrNode, @Cached("createAsPointerNode()") Node classNameAsPtrNode) {
        try {
            return handleAddresses(ForeignAccess.sendAsPointer(descriptionAsPtrNode, description), ForeignAccess.sendAsPointer(modeAsPtrNode, mode),
                            ForeignAccess.sendAsPointer(classNameAsPtrNode, className), connAddr);
        } catch (UnsupportedMessageException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    private static Object handleAddresses(long description, long mode, long className, RExternalPtr connAddr) {
        try {
            return new NativeRConnection(readNativeString(description), readNativeString(mode), readNativeString(className), connAddr).asVector();
        } catch (IOException e) {
            return InvalidConnection.instance.asVector();
        }
    }

}
