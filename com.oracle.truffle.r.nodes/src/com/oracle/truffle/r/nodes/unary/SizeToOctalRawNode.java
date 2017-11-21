/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.unary;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RRawVector;
import com.oracle.truffle.r.runtime.data.nodes.SetDataAt;

public abstract class SizeToOctalRawNode extends UnaryNode {

    private Charset asciiCharset;

    public abstract RRawVector execute(Object size);

    @Specialization
    protected RRawVector doInt(int s) {
        return RDataFactory.createRawVector(toOctalAsciiString(s));
    }

    @TruffleBoundary
    private byte[] toOctalAsciiString(int s) {
        if (asciiCharset == null) {
            asciiCharset = Charset.forName("US-ASCII");
        }

        ByteBuffer encode = asciiCharset.encode(Integer.toOctalString(s));
        // reverse
        byte[] result = new byte[11];
        Arrays.fill(result, (byte) '0');
        for (int i = result.length - 1; i >= 0 && encode.hasRemaining(); i--) {
            result[i] = encode.get();
        }
        return result;
    }

    // Transcribed from ".../utils/src/stubs.c"
    @Specialization
    protected RRawVector doDouble(double size,
                    @Cached("create()") SetDataAt.Raw setDataNode) {

        double s = size;
        if (!RRuntime.isFinite(s) && s >= 0) {
            throw RError.error(RError.SHOW_CALLER, RError.Message.GENERIC, "size must be finite and >= 0");
        }

        RRawVector ans = RDataFactory.createRawVector(11);
        byte[] store = ans.getInternalStore();

        for (int i = 0; i < 11; i++) {
            double s2 = Math.floor(s / 8.0);
            double t = s - 8.0 * s2;
            s = s2;
            setDataNode.setDataAtAsObject(ans, store, 10 - i, (byte) (48.0 + t));
        }
        return ans;
    }

    @Specialization
    protected RRawVector doNull(@SuppressWarnings("unused") RNull n) {
        return RDataFactory.createRawVector(11);
    }

    public static SizeToOctalRawNode create() {
        return SizeToOctalRawNodeGen.create();

    }
}
