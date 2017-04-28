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

import java.nio.charset.StandardCharsets;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.engine.interop.UnsafeAdapter;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.ffi.DLL;

public class TruffleNFI_Utils {

    private static String getString(long address, int len) {
        byte[] byteArray = new byte[len];
        for (int i = 0; i < len; i++) {
            byteArray[i] = UnsafeAdapter.UNSAFE.getByte(address + i);
        }
        return new String(byteArray, StandardCharsets.UTF_8);
    }

    static String convertCstring(Object cstring, int len) {
        try {
            long address = (long) ForeignAccess.sendUnbox(Message.UNBOX.createNode(), (TruffleObject) cstring);
            return getString(address, len);
        } catch (UnsupportedMessageException ex) {
            throw RInternalError.shouldNotReachHere(ex);
        }
    }

    private static TruffleObject defaultLibrary;

    @TruffleBoundary
    private static void initDefaultLibrary() {
        if (defaultLibrary == null) {
            Env env = RContext.getInstance().getEnv();
            defaultLibrary = (TruffleObject) env.parse(Source.newBuilder("default").name("(load default)").mimeType("application/x-native").build()).call();
        }

    }

    /**
     * Looks up the symbol {@code name} in either the "default" library (e.g. C library symbols) or
     * in one of the libraries loaded through {@link DLL}, and binds the given NFI signature to the
     * result, returning the resulting Truffle function object. Failure is fatal.
     */
    static TruffleObject lookupAndBind(String name, boolean inDefaultLibrary, String signature) {
        initDefaultLibrary();
        try {
            TruffleObject symbol;
            if (inDefaultLibrary) {
                symbol = ((TruffleObject) ForeignAccess.sendRead(Message.READ.createNode(), defaultLibrary, name));
            } else {
                symbol = DLL.findSymbol(name, null).asTruffleObject();
            }
            return (TruffleObject) ForeignAccess.sendInvoke(Message.createInvoke(1).createNode(), symbol, "bind", signature);
        } catch (InteropException e) {
            throw RInternalError.shouldNotReachHere(e);
        }
    }

    /**
     * Returns the number of arguments in an NFI signature.
     */
    static int getArgCount(String signature) {
        int argCount = 0;
        int nestCount = -1;
        boolean type = false;
        for (int i = 0; i < signature.length(); i++) {
            char ch = signature.charAt(i);
            if (ch == '(') {
                nestCount++;
            } else if (ch == ')') {
                if (nestCount > 0) {
                    nestCount--;
                } else {
                    return type ? argCount + 1 : 0;
                }
            } else if (ch == ',') {
                if (nestCount == 0) {
                    argCount++;
                }
            } else {
                type = true;
            }
        }
        throw RInternalError.shouldNotReachHere();
    }

    public static void main(String[] args) {
        System.out.printf("argCount: %s%n", getArgCount(args[0]));
    }
}
