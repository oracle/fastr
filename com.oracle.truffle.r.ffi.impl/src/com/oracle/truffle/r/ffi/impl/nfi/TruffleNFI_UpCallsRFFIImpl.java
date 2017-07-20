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
package com.oracle.truffle.r.ffi.impl.nfi;

import com.oracle.truffle.r.ffi.impl.common.JavaUpCallsRFFIImpl;
import com.oracle.truffle.r.runtime.ffi.CharSXPWrapper;
import com.oracle.truffle.r.runtime.gnur.SEXPTYPE;

public class TruffleNFI_UpCallsRFFIImpl extends JavaUpCallsRFFIImpl {
    @Override
    public Object Rf_mkCharLenCE(Object bytes, int len, int encoding) {
        // "bytes" is actually a Long unboxed from a NativePointer
        // TODO: handle encoding properly
        return CharSXPWrapper.create(TruffleNFI_Utils.convertCstring(bytes, len));
    }

    @Override
    public Object INTEGER(Object x) {
        long arrayAddress = TruffleNFI_NativeArray.findArray(x);
        if (arrayAddress == 0) {
            Object array = super.INTEGER(x);
            arrayAddress = TruffleNFI_NativeArray.recordArray(x, array, SEXPTYPE.INTSXP);
        } else {
            TruffleNFI_Call.returnArrayExisting(SEXPTYPE.INTSXP, arrayAddress);
        }
        return x;
    }

    @Override
    public Object LOGICAL(Object x) {
        long arrayAddress = TruffleNFI_NativeArray.findArray(x);
        if (arrayAddress == 0) {
            Object array = super.LOGICAL(x);
            arrayAddress = TruffleNFI_NativeArray.recordArray(x, array, SEXPTYPE.LGLSXP);
        } else {
            TruffleNFI_Call.returnArrayExisting(SEXPTYPE.LGLSXP, arrayAddress);
        }
        return x;

    }

    @Override
    public Object REAL(Object x) {
        long arrayAddress = TruffleNFI_NativeArray.findArray(x);
        if (arrayAddress == 0) {
            Object array = super.REAL(x);
            arrayAddress = TruffleNFI_NativeArray.recordArray(x, array, SEXPTYPE.REALSXP);
        } else {
            TruffleNFI_Call.returnArrayExisting(SEXPTYPE.REALSXP, arrayAddress);
        }
        return x;

    }

    @Override
    public Object RAW(Object x) {
        long arrayAddress = TruffleNFI_NativeArray.findArray(x);
        if (arrayAddress == 0) {
            Object array = super.RAW(x);
            arrayAddress = TruffleNFI_NativeArray.recordArray(x, array, SEXPTYPE.RAWSXP);
        } else {
            TruffleNFI_Call.returnArrayExisting(SEXPTYPE.RAWSXP, arrayAddress);
        }
        return x;
    }

    @Override
    public Object R_CHAR(Object x) {
        long arrayAddress = TruffleNFI_NativeArray.findArray(x);
        if (arrayAddress == 0) {
            CharSXPWrapper charSXP = (CharSXPWrapper) x;
            Object array = charSXP.getContents().getBytes();
            arrayAddress = TruffleNFI_NativeArray.recordArray(x, array, SEXPTYPE.CHARSXP);
        } else {
            TruffleNFI_Call.returnArrayExisting(SEXPTYPE.CHARSXP, arrayAddress);
        }
        return x;
    }
}
