/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.SUBSTITUTE;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "upper.tri", kind = SUBSTITUTE, parameterNames = {"x", "diag"})
// TODO Implement in R
public abstract class UpperTri extends RBuiltinNode {

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, RRuntime.LOGICAL_FALSE};
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.firstBoolean(1);
    }

    @Specialization
    protected RLogicalVector upperTri(RAbstractVector vector, boolean diag, //
                    @Cached("createBinaryProfile()") ConditionProfile nullDimensionsProfile, //
                    @Cached("createBinaryProfile()") ConditionProfile nonSquareProfile) {
        controlVisibility();
        int[] dim = vector.getDimensions();
        if (nullDimensionsProfile.profile(dim == null)) {
            dim = new int[]{vector.getLength(), 1};
        }
        int size = Math.min(dim[0], dim[1]);
        int nrow = dim[0];
        int length = vector.getLength();
        byte[] result = new byte[length];
        for (int i = 0; i < size; i++) {
            int cmp = diag ? i + 1 : i;
            for (int j = 0; j < cmp; j++) {
                int index = i * nrow + j;
                result[index] = RRuntime.LOGICAL_TRUE;
            }
        }
        if (nonSquareProfile.profile(dim[0] < dim[1])) {
            for (int i = size * size; i < length; i++) {
                result[i] = RRuntime.LOGICAL_TRUE;
            }
        }
        return RDataFactory.createLogicalVector(result, true, dim);
    }
}
