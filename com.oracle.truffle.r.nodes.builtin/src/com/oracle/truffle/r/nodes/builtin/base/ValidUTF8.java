/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RVisibility.ON;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE_ARITHMETIC;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RStringVector;

/**
 * The {@code validUTF8 .Internal}.
 */
@RBuiltin(name = "validUTF8", visibility = ON, kind = INTERNAL, parameterNames = {"x"}, behavior = PURE_ARITHMETIC)
public abstract class ValidUTF8 extends RBuiltinNode.Arg1 {

    static {
        Casts casts = new Casts(ValidUTF8.class);
        casts.arg("x").mustNotBeMissing().mustNotBeNull().asStringVector();
    }

    @Specialization
    protected RLogicalVector validUTF8(RStringVector x) {
        /**
         * NB: Once we have a string it is too late to determine whether the string is UTF-8 valid.
         * The check must be done on the raw data before it is used to create the string.
         */
        byte[] result = new byte[x.getLength()];
        Arrays.fill(result, RRuntime.LOGICAL_TRUE);
        return RDataFactory.createLogicalVector(result, true);
    }

}
