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
import com.oracle.truffle.r.nodes.attributes.GetFixedAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastToVectorNode;
import com.oracle.truffle.r.nodes.unary.InheritsNode;
import com.oracle.truffle.r.nodes.unary.InheritsNodeGen;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "asCharacterFactor", kind = INTERNAL, parameterNames = "x", behavior = PURE)
public abstract class AsCharacterFactor extends RBuiltinNode.Arg1 {
    private static final RStringVector CLASS_FACTOR_VEC = RDataFactory.createStringVectorFromScalar(RRuntime.CLASS_FACTOR);

    @Child private InheritsNode inheritsNode = InheritsNodeGen.create();
    @Child private CastToVectorNode castToVectorNode = CastToVectorNode.create();
    @Child private GetFixedAttributeNode getLevelsAttrNode = GetFixedAttributeNode.create(RRuntime.LEVELS_ATTR_KEY);

    private final NACheck naCheck = NACheck.create();

    static {
        Casts.noCasts(AsCharacterFactor.class);
    }

    @Specialization
    protected RStringVector doAsCharacterFactor(Object x) {
        byte isFactor = (byte) inheritsNode.execute(x, CLASS_FACTOR_VEC, false);
        if (isFactor == RRuntime.LOGICAL_FALSE) {
            throw error(RError.Message.COERCE_NON_FACTOR);
        }
        RIntVector xVec = (RIntVector) x;
        int n = xVec.getLength();
        String[] data = new String[n];
        Object levsAttr = getLevelsAttrNode.execute(xVec);
        Object levs;
        if (levsAttr == null || !((levs = castToVectorNode.doCast(levsAttr)) instanceof RAbstractStringVector)) {
            throw error(RError.Message.MALFORMED_FACTOR);
        }
        RAbstractStringVector levsString = (RAbstractStringVector) levs;
        int nl = levsString.getLength();
        naCheck.enable(xVec);
        for (int i = 0; i < n; i++) {
            int xi = xVec.getDataAt(i);
            if (naCheck.check(xi)) {
                data[i] = RRuntime.STRING_NA;
            } else if (xi >= 1 && xi <= nl) {
                data[i] = levsString.getDataAt(xi - 1);
            } else {
                throw error(RError.Message.MALFORMED_FACTOR);
            }
        }
        return RDataFactory.createStringVector(data, naCheck.neverSeenNA());
    }
}
