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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.ops.na.NACheck;

public class StartsEndsWithFunctions {

    private abstract static class Adapter extends RBuiltinNode {
        private final NACheck naCheck = NACheck.create();
        private final ConditionProfile singlePrefixProfile = ConditionProfile.createBinaryProfile();

        private static void argCast(Casts casts, String name) {
            casts.arg(name).mustBe(stringValue(), RError.SHOW_CALLER, RError.Message.NON_CHARACTER_OBJECTS).asStringVector();
        }

        protected static Casts createCasts(Class<? extends Adapter> extCls) {
            Casts casts = new Casts(extCls);
            argCast(casts, "x");
            argCast(casts, "prefix");
            return casts;
        }

        protected Object doIt(RAbstractStringVector xVec, RAbstractStringVector prefixVec, boolean startsWith) {
            int xLen = xVec.getLength();
            int prefixLen = prefixVec.getLength();
            int resultLen = (xLen > 0 && prefixLen > 0) ? ((xLen >= prefixLen) ? xLen : prefixLen) : 0;
            if (resultLen == 0) {
                return RDataFactory.createEmptyLogicalVector();
            }
            byte[] data = new byte[resultLen];
            if (singlePrefixProfile.profile(prefixLen == 1)) {
                String prefix = prefixVec.getDataAt(0);
                if (RRuntime.isNA(prefix)) {
                    return RDataFactory.createLogicalVector(resultLen, true);
                } else {
                    naCheck.enable(true);
                    for (int i = 0; i < xLen; i++) {
                        String x = xVec.getDataAt(i);
                        if (naCheck.check(x)) {
                            data[i] = RRuntime.LOGICAL_NA;
                        } else {
                            boolean ans = startsWith ? x.startsWith(prefix) : x.endsWith(prefix);
                            data[i] = RRuntime.asLogical(ans);
                        }
                    }
                }
            } else {
                naCheck.enable(true);
                for (int i = 0; i < resultLen; i++) {
                    String x = xVec.getDataAt(i % xLen);
                    String prefix = prefixVec.getDataAt(i % prefixLen);
                    if (naCheck.check(x) || naCheck.check(prefix)) {
                        data[i] = RRuntime.LOGICAL_NA;
                    } else {
                        boolean ans = startsWith ? x.startsWith(prefix) : x.endsWith(prefix);
                        data[i] = RRuntime.asLogical(ans);
                    }
                }
            }
            return RDataFactory.createLogicalVector(data, naCheck.neverSeenNA());
        }
    }

    @RBuiltin(name = "startsWith", kind = INTERNAL, parameterNames = {"x", "prefix"}, behavior = PURE)
    public abstract static class StartsWith extends Adapter {

        static {
            createCasts(StartsWith.class);
        }

        @Specialization
        protected Object startsWith(RAbstractStringVector x, RAbstractStringVector prefix) {
            return doIt(x, prefix, true);
        }
    }

    @RBuiltin(name = "endsWith", kind = INTERNAL, parameterNames = {"x", "prefix"}, behavior = PURE)
    public abstract static class EndsWith extends Adapter {

        static {
            createCasts(EndsWith.class);
        }

        @Specialization
        protected Object endsWith(RAbstractStringVector x, RAbstractStringVector prefix) {
            return doIt(x, prefix, false);
        }
    }
}
