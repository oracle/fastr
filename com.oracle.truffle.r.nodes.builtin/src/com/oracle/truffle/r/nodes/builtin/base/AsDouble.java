/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.emptyDoubleVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.missingValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.opt.ReuseNonSharedNode;
import com.oracle.truffle.r.runtime.RDispatch;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;

@RBuiltin(name = "as.double", aliases = {"as.numeric"}, kind = PRIMITIVE, dispatch = RDispatch.INTERNAL_GENERIC, parameterNames = {"x", "..."}, behavior = PURE)
public abstract class AsDouble extends RBuiltinNode.Arg2 {

    private final ConditionProfile noAttributes = ConditionProfile.createBinaryProfile();

    @Child protected ReuseNonSharedNode reuseNonShared = ReuseNonSharedNode.create();

    static {
        Casts casts = new Casts(AsDouble.class);
        casts.arg("x").returnIf(missingValue().or(nullValue()), emptyDoubleVector()).asDoubleVector(false, false, false);
    }

    @Specialization
    protected RAbstractDoubleVector asDouble(RAbstractDoubleVector v, @SuppressWarnings("unused") RArgsValuesAndNames dotdotdot) {
        if (noAttributes.profile(v.getAttributes() == null)) {
            return v;
        } else {
            RAbstractDoubleVector res = (RAbstractDoubleVector) reuseNonShared.execute(v);
            res.resetAllAttributes(true);
            return res;
        }
    }
}
