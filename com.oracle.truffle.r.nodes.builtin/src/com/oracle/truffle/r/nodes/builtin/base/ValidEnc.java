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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

/* TODO Not sure we have enough state at present to implement this properly.
 * GNU R bases its check on state encoded with the object and performs the check
 * based on the values of the global variables "utf8locale" and "mbcslocale".
 */

@RBuiltin(name = "validEnc", kind = INTERNAL, parameterNames = {"x"}, behavior = PURE)
public abstract class ValidEnc extends RBuiltinNode.Arg1 {
    static {
        Casts casts = new Casts(ValidEnc.class);
        casts.arg("x").mustBe(stringValue()).asStringVector();
    }

    @Specialization
    protected RLogicalVector validEnc(RAbstractStringVector xVec) {
        byte[] data = new byte[xVec.getLength()];

        for (int i = 0; i < xVec.getLength(); i++) {
            @SuppressWarnings("unused")
            String x = xVec.getDataAt(i);
            data[i] = RRuntime.LOGICAL_TRUE;
        }
        RLogicalVector result = RDataFactory.createLogicalVector(data, RDataFactory.COMPLETE_VECTOR);
        return result;
    }
}
