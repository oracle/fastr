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

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.ffi.impl.llvm.TruffleLLVM_Utils;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RTruffleObject;

/**
 * Created when a {@link RTruffleObject} subclass has no meaningful native representation,
 * nevertheless a {@link Message#TO_NATIVE} message is sent to it.
 */
public class NativePointer implements TruffleObject {

    /**
     * This is used when an {@link RNull} is stored in memory (LLVM).
     */
    private static final class NullNativePointer extends NativePointer {
        private NullNativePointer() {
            super(RNull.instance);
        }

        @Override
        protected long asPointerImpl() {
            return 0;
        }
    }

    public static final NullNativePointer NULL_NATIVEPOINTER = new NullNativePointer();

    private static Table[] table = new Table[16];
    private static int tableHwm;

    private static class Table {
        private final RTruffleObject object;
        private final long nativePointer;

        Table(RTruffleObject object, long nativePointer) {
            this.object = object;
            this.nativePointer = nativePointer;
        }
    }

    final RTruffleObject object;

    public NativePointer(RTruffleObject object) {
        this.object = object;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return NativePointerMRForeign.ACCESS;
    }

    public static boolean isInstance(TruffleObject obj) {
        return obj instanceof NativePointer;
    }

    public static TruffleObject check(TruffleObject object) {
        long nativePointer = TruffleLLVM_Utils.getNativeAddress(object);
        for (int i = tableHwm - 1; i >= 0; i--) {
            if (table[i].nativePointer == nativePointer) {
                return table[i].object;
            }
        }
        return null;
    }

    final long asPointer() {
        long result = asPointerImpl();
        boolean newPointer = true;
        for (int i = 0; i < tableHwm; i++) {
            if (table[i].nativePointer == result) {
                newPointer = false;
                break;
            }
        }
        if (newPointer) {
            // System.out.printf("as_pointer: %x from %s\n", result, object.getClass().getName());
            if (tableHwm >= table.length) {
                Table[] newTable = new Table[table.length * 2];
                System.arraycopy(table, 0, newTable, 0, table.length);
                table = newTable;
            }
            table[tableHwm++] = new Table(object, result);
        }
        return result;
    }

    protected long asPointerImpl() {
        return System.identityHashCode(object);
    }
}
