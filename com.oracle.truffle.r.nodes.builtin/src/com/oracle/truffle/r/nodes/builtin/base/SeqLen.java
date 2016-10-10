/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.*;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntSequence;

@RBuiltin(name = "seq_len", kind = PRIMITIVE, parameterNames = {"length.out"}, behavior = PURE)
public abstract class SeqLen extends RBuiltinNode {

    @Override
    protected void createCasts(CastBuilder casts) {
        // this is slightly different than what GNU R does as it will report coercion warning for:
        // seq_len(c("7", "b"))
        // GNU R (presumably) gets the first element before doing a coercion but I don't think we
        // can do it with our API
        casts.arg("length.out").asIntegerVector().shouldBe(size(1).or(size(0)), RError.Message.FIRST_ELEMENT_USED, "length.out").findFirst(RRuntime.INT_NA,
                        RError.Message.FIRST_ELEMENT_USED, "length.out").mustBe(gte(0), RError.Message.MUST_BE_COERCIBLE_INTEGER);
    }

    @Specialization
    protected RIntSequence seqLen(int length) {
        return RDataFactory.createIntSequence(1, 1, length);
    }
}
