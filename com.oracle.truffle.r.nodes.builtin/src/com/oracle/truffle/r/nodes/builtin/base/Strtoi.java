/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.eq;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.lte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.PrimitiveValueProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "strtoi", kind = INTERNAL, parameterNames = {"x", "base"}, behavior = PURE)
public abstract class Strtoi extends RBuiltinNode.Arg2 {

    static {
        Casts casts = new Casts(Strtoi.class);
        casts.arg("x").mustBe(stringValue()).asStringVector();
        // base == 0 || (base >= 2 && base <= 36)
        casts.arg("base").mustBe(integerValue()).asIntegerVector().findFirst().mustBe(eq(0).or(gte(2).and(lte(36))));
    }

    @Specialization
    protected RIntVector doStrtoi(RAbstractStringVector vec, int baseArg,
                    @Cached("createBinaryProfile()") ConditionProfile emptyProfile,
                    @Cached("createBinaryProfile()") ConditionProfile baseZeroProfile,
                    @Cached("createBinaryProfile()") ConditionProfile negateProfile,
                    @Cached("createBinaryProfile()") ConditionProfile incompleteProfile,
                    @Cached("createEqualityProfile()") PrimitiveValueProfile baseProfile) {
        int[] data = new int[vec.getLength()];
        boolean complete = true;
        for (int i = 0; i < data.length; i++) {
            int dataValue;
            String s = vec.getDataAt(i);
            if (emptyProfile.profile(s.length() == 0)) {
                dataValue = RRuntime.INT_NA;
            } else {
                boolean negate = false;
                int pos = 0;
                if (s.charAt(pos) == '+') {
                    // skip "+"
                    pos++;
                } else if (s.charAt(pos) == '-') {
                    negate = true;
                    pos++;
                }
                int base = baseArg;
                if (pos < s.length() && s.charAt(pos) == '0') {
                    // skip "0"
                    pos++;
                    if (pos < s.length() && (s.charAt(pos) == 'x' || s.charAt(pos) == 'X')) {
                        if (baseZeroProfile.profile(base == 0)) {
                            base = 16;
                        }
                        // skip "x" or "X"
                        pos++;
                    } else {
                        if (baseZeroProfile.profile(base == 0)) {
                            base = 8;
                        }
                        // go back (to parse the "0")
                        pos--;
                    }
                } else {
                    if (baseZeroProfile.profile(base == 0)) {
                        base = 10;
                    }
                }
                base = baseProfile.profile(base);
                if (pos == s.length()) {
                    // produce NA is no data is available
                    dataValue = RRuntime.INT_NA;
                } else {
                    dataValue = 0;
                    while (pos < s.length()) {
                        char c = s.charAt(pos++);
                        int digit;
                        if (base > 10) {
                            if (c >= '0' && c <= '9') {
                                digit = c - '0';
                            } else if (c >= 'a' && c < ('a' + base - 10)) {
                                digit = c - 'a' + 10;
                            } else if (c >= 'A' && c < ('A' + base - 10)) {
                                digit = c - 'A' + 10;
                            } else {
                                dataValue = RRuntime.INT_NA;
                                break;
                            }
                        } else {
                            if (c >= '0' && c < ('0' + base)) {
                                digit = c - '0';
                            } else {
                                dataValue = RRuntime.INT_NA;
                                break;
                            }
                        }
                        try {
                            dataValue = Math.addExact(Math.multiplyExact(dataValue, base), digit);
                        } catch (ArithmeticException e) {
                            dataValue = RRuntime.INT_NA;
                            break;
                        }
                    }
                }
                if (negateProfile.profile(negate)) {
                    // relies on -INT_NA == INT_NA
                    dataValue = -dataValue;
                }
                if (incompleteProfile.profile(dataValue == RRuntime.INT_NA)) {
                    complete = false;
                }
                data[i] = dataValue;
            }
        }
        return RDataFactory.createIntVector(data, complete);
    }
}
