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
package com.oracle.truffle.r.ffi.impl.interop;

import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

@MessageResolution(receiverType = NativeLogicalArray.class)
public class NativeLogicalArrayMR {
    @Resolve(message = "READ")
    public abstract static class NLAReadNode extends Node {
        protected int access(NativeLogicalArray receiver, int index) {
            return receiver.read(index);
        }
    }

    @Resolve(message = "WRITE")
    public abstract static class NLAWriteNode extends Node {
        protected Object access(NativeLogicalArray receiver, int index, int value) {
            receiver.write(index, value);
            return value;
        }
    }

    @Resolve(message = "GET_SIZE")
    public abstract static class NLAGetSizeNode extends Node {
        protected int access(NativeLogicalArray receiver) {
            return receiver.data.length;
        }
    }

    @CanResolve
    public abstract static class NLACheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof NativeLogicalArray;
        }
    }
}
