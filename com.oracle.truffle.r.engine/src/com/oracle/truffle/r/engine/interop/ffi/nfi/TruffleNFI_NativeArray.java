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
package com.oracle.truffle.r.engine.interop.ffi.nfi;

import java.util.Arrays;

import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;

/**
 * Support for the {@code INTEGER, LOGICAL, ...} functions in the RFFI, which must return the same
 * array while an FFI call is in progress.
 */
public class TruffleNFI_NativeArray {
    private static int tableHwm;
    private static int[] hwmStack = new int[16];
    private static Info[] table = new Info[64];

    static {
        initTableElements(0);
    }

    private static class Info {
        /**
         * E.g., {@link RIntVector}.
         */
        Object x;
        /**
         * internal array, e.g. {@code int[]}.
         */
        Object array;
        /**
         * {@code null} unless {@code x} is an {@link RLogicalVector}, in which case it is the
         * underlying {@code byte[]}.
         */
        byte[] logicalByteArray;

        long arrayAddress;
    }

    private static void initTableElements(int startIndex) {
        for (int i = startIndex; i < table.length; i++) {
            table[i] = new Info();
        }
    }

    static void callEnter(int callDepth) {
        hwmStack[callDepth] = tableHwm;
    }

    static void callExit(int callDepth) {
        int oldHwm = hwmStack[callDepth - 1];
        for (int i = oldHwm; i < tableHwm; i++) {
            Info info = table[i];
            if (info.x != null) {
                if (info.logicalByteArray != null) {
                    boolean seenNA = false;
                    int[] xai = (int[]) info.array;
                    for (int j = 0; j < xai.length; j++) {
                        int xaival = xai[j];
                        byte xal;
                        if (xaival == RRuntime.INT_NA) {
                            seenNA = true;
                            xal = RRuntime.LOGICAL_NA;
                        } else {
                            xal = (byte) xaival;
                        }
                        info.logicalByteArray[j] = xal;
                    }
                    if (seenNA) {
                        RLogicalVector lv = (RLogicalVector) info.x;
                        lv.setComplete(false);
                    }
                }
                TruffleNFI_Call.freeArray(info.arrayAddress);
            }
        }
        tableHwm = oldHwm;
    }

    /**
     * Searches table for an entry matching {@code x}.
     *
     * @return the associated native array address or {@code 0} if not found.
     */
    static long findArray(Object x) {
        for (int i = 0; i < tableHwm; i++) {
            if (table[i].x == x) {
                return table[i].arrayAddress;
            }
        }
        return 0;
    }

    /**
     * Records that the {@code array} associated with object {@code x} has been requested by the
     * native code from, e.g., an {@code INTEGER(x)} function.
     *
     * @return the native array address
     */
    static long recordArray(Object x, Object array, SEXPTYPE type) {
        Object xa;
        byte[] logicalByteArray = null;
        boolean isString = false;
        switch (type) {
            case INTSXP:
            case REALSXP:
            case RAWSXP:
            case CHARSXP:
                isString = type == SEXPTYPE.CHARSXP;
                xa = array;
                break;

            case LGLSXP: {
                byte[] xal = (byte[]) array;
                // RFFI wants int*
                int[] xai = new int[xal.length];
                for (int i = 0; i < xai.length; i++) {
                    byte lval = xal[i];
                    xai[i] = RRuntime.isNA(lval) ? RRuntime.INT_NA : lval;
                }
                xa = xai;
                logicalByteArray = xal;
                break;
            }

            default:
                throw RInternalError.shouldNotReachHere();

        }
        if (tableHwm == table.length) {
            table = Arrays.copyOf(table, table.length * 2);
            initTableElements(tableHwm);
        }
        Info t = table[tableHwm];
        t.x = x;
        t.array = xa;
        t.logicalByteArray = logicalByteArray;
        t.arrayAddress = TruffleNFI_Call.returnArrayCreate(xa, isString);
        tableHwm++;
        return t.arrayAddress;
    }

}
