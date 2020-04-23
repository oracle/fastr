/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.library.utils;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RStringVector;

public abstract class Crc64 extends RExternalBuiltinNode.Arg1 {

    static {
        Casts casts = new Casts(Crc64.class);
        casts.arg(0).mustNotBeNull(RError.Message.INPUT_MUST_BE_STRING).mustBe(stringValue(), RError.Message.INPUT_MUST_BE_STRING);
    }

    @Specialization
    public String crc64(RStringVector x) {
        return crc(x);
    }

    @TruffleBoundary
    public static String crc(RStringVector x) {
        final String string = x.getDataAt(0);
        byte[] bytes = string.getBytes();
        bytes = crc64(bytes);
        long l = 0;
        for (int i = 0; i < bytes.length; i++) {
            l += (bytes[i] & 0xffL) << (8 * i);
        }
        return Long.toHexString(l);
    }

    @TruffleBoundary
    private static byte[] crc64(byte[] bytes) {
        org.tukaani.xz.check.CRC64 crc = new org.tukaani.xz.check.CRC64();
        crc.update(bytes);
        return crc.finish();
    }
}
