/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "crc64", kind = INTERNAL, parameterNames = {"x"}, behavior = PURE)
public abstract class CRC64 extends RBuiltinNode {

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("x").conf(x -> x.mustNotBeMissing(RError.SHOW_CALLER2, Message.ARGUMENTS_PASSED_INTERNAL_0_1, getRBuiltin().name())).mustNotBeNull(RError.NO_CALLER,
                        Message.INPUT_MUST_BE_STRING).mustBe(stringValue(), RError.NO_CALLER, Message.INPUT_MUST_BE_STRING);
    }

    @Specialization
    protected RAbstractStringVector crc64(RAbstractStringVector x) {
        final String string = x.getDataAt(0);
        byte[] bytes = string.getBytes();
        bytes = crc64(bytes);
        long l = 0;
        for (int i = 0; i < bytes.length; i++) {
            l += (bytes[i] & 0xffL) << (8 * i);
        }
        return RDataFactory.createStringVector(Long.toHexString(l));
    }

    @TruffleBoundary
    private static byte[] crc64(byte[] bytes) {
        org.tukaani.xz.check.CRC64 crc = new org.tukaani.xz.check.CRC64();
        crc.update(bytes);
        return crc.finish();
    }

}
