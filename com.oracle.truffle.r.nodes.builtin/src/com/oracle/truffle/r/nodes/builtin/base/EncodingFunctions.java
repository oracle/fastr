/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public class EncodingFunctions {

    @RBuiltin(name = "Encoding", kind = INTERNAL, parameterNames = "x", behavior = PURE)
    public abstract static class Encoding extends RBuiltinNode {

        static {
            Casts casts = new Casts(Encoding.class);
            casts.arg("x").mustBe(stringValue(), RError.Message.CHAR_VEC_ARGUMENT);
        }

        @Specialization
        protected RStringVector encoding(@SuppressWarnings("unused") RAbstractStringVector x) {
            // TODO implement properly
            return RDataFactory.createStringVectorFromScalar("unknown");
        }
    }

    @RBuiltin(name = "setEncoding", kind = INTERNAL, parameterNames = {"x", "value"}, behavior = PURE)
    public abstract static class SetEncoding extends RBuiltinNode {

        static {
            Casts casts = new Casts(SetEncoding.class);
            casts.arg("x").defaultError(RError.Message.CHAR_VEC_ARGUMENT).mustBe(stringValue());
            // asStringVector is required for notEmpty() to receive a proper type in case of scalars
            casts.arg("value").defaultError(RError.Message.GENERIC, "a character vector 'value' expected").mustBe(stringValue()).asStringVector().mustBe(notEmpty(),
                            RError.Message.GENERIC, "'value' must be of positive length");
        }

        @Specialization
        protected Object setEncoding(RAbstractStringVector x, @SuppressWarnings("unused") RAbstractStringVector value) {
            // TODO implement properly
            return x;
        }
    }
}
