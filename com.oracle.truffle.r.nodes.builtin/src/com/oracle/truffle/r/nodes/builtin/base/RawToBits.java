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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractRawVector;

@RBuiltin(name = "rawToBits", kind = INTERNAL, parameterNames = {"x"}, behavior = PURE)
public abstract class RawToBits extends RBuiltinNode {

    static {
        Casts casts = new Casts(RawToBits.class);
        casts.arg("x").mustNotBeNull(RError.Message.ARGUMENT_MUST_BE_RAW_VECTOR, "x").mustBe(Predef.rawValue(), RError.Message.ARGUMENT_MUST_BE_RAW_VECTOR, "x");
    }

    @Specialization
    protected RAbstractRawVector rawToBits(RAbstractRawVector x) {
        byte[] result = new byte[8 * x.getLength()];
        int pos = 0;
        for (int j = 0; j < x.getLength(); j++) {
            byte temp = x.getRawDataAt(j);
            for (int i = 0; i < 8; i++) {
                result[pos++] = (byte) (temp & 1);
                temp >>= 1;
            }
        }
        return RDataFactory.createRawVector(result);
    }
}
