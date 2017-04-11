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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

@RBuiltin(name = "strrep", kind = INTERNAL, parameterNames = {"x", "times"}, behavior = PURE)
public abstract class Strrep extends RBuiltinNode.Arg2 {
    private final NACheck naCheck = NACheck.create();

    static {
        Casts casts = new Casts(Strrep.class);
        casts.arg("x").mustNotBeMissing().asStringVector();
        casts.arg("times").mustNotBeMissing().asIntegerVector();
    }

    @Specialization
    protected Object strrep(RAbstractStringVector xVec, RAbstractIntVector timesVec) {
        int xLen = xVec.getLength();
        int timesLen = timesVec.getLength();
        if (xLen == 0 || timesLen == 0) {
            return RDataFactory.createEmptyStringVector();
        }
        int resultLen = xLen > timesLen ? xLen : timesLen;
        int ix = 0;
        int itimes = 0;
        naCheck.enable(true);

        String[] data = new String[resultLen];
        for (int i = 0; i < resultLen; i++) {
            int times = timesVec.getDataAt(itimes);
            String x = xVec.getDataAt(ix);
            if (naCheck.check(x) || naCheck.check(times)) {
                data[i] = RRuntime.STRING_NA;
            } else {
                if (times < 0) {
                    throw error(RError.Message.INVALID_VALUE, "times");
                }
                if (times == 1) {
                    data[i] = x;
                } else {
                    StringBuffer sb = new StringBuffer();
                    for (int t = 0; t < times; t++) {
                        sb.append(x);
                    }
                    data[i] = sb.toString();
                }
            }
            ix = (++ix == xLen) ? 0 : ix;
            itimes = (++itimes == timesLen) ? 0 : itimes;
        }
        RStringVector result = RDataFactory.createStringVector(data, naCheck.neverSeenNA());
        if (resultLen == xLen) {
            copyNames(xVec, result);
        }
        return result;
    }

    @Specialization
    protected Object strrep(RNull xVec, RAbstractIntVector timesVec) {
        return RDataFactory.createEmptyStringVector(); // GnuR fails with segfault; return value
                                                       // adheres to non-internal strrep() result
    }

    @Specialization
    protected Object strrep(RAbstractStringVector xVec, RNull timesVec) {
        return RDataFactory.createEmptyStringVector(); // GnuR - infinite loop; return value adheres
                                                       // to non-internal strrep() result
    }

    @TruffleBoundary
    private static void copyNames(RAbstractStringVector xVec, RStringVector result) {
        result.copyNamesFrom(xVec);
    }
}
