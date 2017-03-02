/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.runtime.RRuntime;

@MessageResolution(receiverType = NativeIntegerArray.class)
public class NativeIntegerArrayMR {

    @Resolve(message = "READ")
    public abstract static class NIAReadNode extends Node {
        protected int access(NativeIntegerArray receiver, int index) {
            return receiver.read(index);
        }
    }

    @Resolve(message = "WRITE")
    public abstract static class NIAWriteNode extends Node {
        protected int access(NativeIntegerArray receiver, int index, int value) {
            if (value == RRuntime.INT_NA) {
                receiver.setIncomplete();
            }
            receiver.write(index, value);
            return value;
        }
    }

    @Resolve(message = "TO_NATIVE")
    public abstract static class NIAToNativeNode extends Node {
        protected Object access(NativeIntegerArray receiver) {
            return new IntegerNativePointer(receiver);
        }
    }

    @CanResolve
    public abstract static class NIACheck extends Node {

        protected static boolean test(TruffleObject receiver) {
            return receiver instanceof NativeIntegerArray;
        }
    }

    private static final class IntegerNativePointer extends NativePointer {
        private final NativeIntegerArray nativeIntegerArray;

        private IntegerNativePointer(NativeIntegerArray object) {
            super(object);
            this.nativeIntegerArray = object;
        }

        @Override
        protected long asPointerImpl() {
            long result = nativeIntegerArray.convertToNative();
            return result;
        }

    }
}
