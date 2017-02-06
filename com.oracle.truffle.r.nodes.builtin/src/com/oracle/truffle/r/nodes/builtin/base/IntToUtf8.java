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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "intToUtf8", kind = INTERNAL, parameterNames = {"x", "multiple"}, behavior = PURE)
public abstract class IntToUtf8 extends RBuiltinNode {

    static {
        Casts casts = new Casts(IntToUtf8.class);
        casts.arg("x").asIntegerVector();
        casts.arg("multiple").mustNotBeNull().asLogicalVector().findFirst().map(toBoolean());
    }

    @Specialization
    protected String intToBits(@SuppressWarnings("unused") RNull x, @SuppressWarnings("unused") boolean multiple) {
        return "";
    }

    @Specialization(guards = "multiple")
    protected RAbstractStringVector intToBitsMultiple(RAbstractIntVector x, @SuppressWarnings("unused") boolean multiple,
                    @Cached("create()") NACheck na,
                    @Cached("createBinaryProfile()") ConditionProfile zeroProfile) {

        String[] result = new String[x.getLength()];
        na.enable(x);
        for (int j = 0; j < x.getLength(); j++) {
            int temp = x.getDataAt(j);
            if (na.check(temp)) {
                result[j] = RRuntime.STRING_NA;
            } else if (zeroProfile.profile(temp == 0)) {
                result[j] = "";
            } else {
                try {
                    result[j] = new String(new int[]{temp}, 0, 1);
                } catch (IllegalArgumentException e) {
                    throw RError.error(RError.SHOW_CALLER, Message.GENERIC, "illegal unicode code point");
                }
            }
        }
        return RDataFactory.createStringVector(result, na.neverSeenNA());
    }

    @Specialization(guards = "!multiple")
    protected String intToBits(RAbstractIntVector x, @SuppressWarnings("unused") boolean multiple,
                    @Cached("create()") NACheck na,
                    @Cached("createBinaryProfile()") ConditionProfile zeroProfile) {

        int[] result = new int[x.getLength()];
        na.enable(x);
        int pos = 0;
        for (int j = 0; j < x.getLength(); j++) {
            int temp = x.getDataAt(j);
            if (na.check(temp)) {
                return RRuntime.STRING_NA;
            } else if (zeroProfile.profile(temp != 0)) {
                result[pos++] = temp;
            }
        }
        try {
            return new String(result, 0, pos);
        } catch (IllegalArgumentException e) {
            throw RError.error(RError.SHOW_CALLER, Message.GENERIC, "illegal unicode code point");
        }
    }
}
