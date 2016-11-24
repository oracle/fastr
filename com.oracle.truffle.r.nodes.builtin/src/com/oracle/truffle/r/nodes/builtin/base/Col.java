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
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

@RBuiltin(name = "col", kind = INTERNAL, parameterNames = {"dims"}, behavior = PURE)
public abstract class Col extends RBuiltinNode {

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("dims").defaultError(RError.SHOW_CALLER, RError.Message.MATRIX_LIKE_REQUIRED, "col").mustBe(integerValue()).asIntegerVector().mustBe(size(2));
    }

    @Specialization
    protected RIntVector col(RAbstractIntVector x) {
        int nrows = x.getDataAt(0);
        int ncols = x.getDataAt(1);
        int[] result = new int[nrows * ncols];
        for (int i = 0; i < ncols; i++) {
            int colstart = i * nrows;
            Arrays.fill(result, colstart, colstart + nrows, i + 1);
        }
        return RDataFactory.createIntVector(result, RDataFactory.COMPLETE_VECTOR, new int[]{nrows, ncols});
    }
}
