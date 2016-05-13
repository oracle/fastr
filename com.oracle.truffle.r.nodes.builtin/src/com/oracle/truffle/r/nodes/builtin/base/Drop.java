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

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RBuiltinKind;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "drop", kind = RBuiltinKind.INTERNAL, parameterNames = {"x"})
public abstract class Drop extends RBuiltinNode {

    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    @Specialization
    protected RAbstractVector doDrop(RAbstractVector x, //
                    @Cached("createBinaryProfile()") ConditionProfile nullDimensions) {
        int[] dims = x.getDimensions();
        if (nullDimensions.profile(dims == null)) {
            return x;
        }
        int[] newDims = new int[dims.length];
        int count = 0;
        for (int i = 0; i < dims.length; i++) {
            if (dims[i] != 1) {
                newDims[count++] = dims[i];
            }
        }
        if (count == 0) {
            return x;
        }
        RAbstractVector result = x.copyWithNewDimensions(Arrays.copyOf(newDims, count));
        RList dimNames = x.getDimNames(attrProfiles);
        if (dimNames != null) {
            // TODO adjust
            assert false;
        }
        return result;
    }
}
