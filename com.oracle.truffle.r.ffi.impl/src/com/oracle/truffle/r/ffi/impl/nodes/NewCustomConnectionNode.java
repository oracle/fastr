/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.ffi.impl.nodes;

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.conn.ConnectionSupport.InvalidConnection;
import com.oracle.truffle.r.runtime.conn.NativeConnections.NativeRConnection;
import com.oracle.truffle.r.runtime.data.RExternalPtr;

@GenerateUncached
public abstract class NewCustomConnectionNode extends FFIUpCallNode.Arg4 {

    public static NewCustomConnectionNode create() {
        return NewCustomConnectionNodeGen.create();
    }

    public static NewCustomConnectionNode getUncached() {
        return NewCustomConnectionNodeGen.getUncached();
    }

    @Specialization
    @TruffleBoundary
    Object handleStrings(Object description, Object mode, Object className, RExternalPtr connAddr, @Cached("create()") NativeStringCastNode nscn) {
        try {
            return new NativeRConnection(nscn.executeObject(description), nscn.executeObject(mode), nscn.executeObject(className), connAddr).asVector();
        } catch (IOException e) {
            return InvalidConnection.instance.asVector();
        }
    }
}
