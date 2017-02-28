/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "charmatch", kind = INTERNAL, parameterNames = {"x", "table", "noMatch"}, behavior = PURE)
public abstract class CharMatch extends RBuiltinNode.Arg3 {

    static {
        Casts casts = new Casts(CharMatch.class);
        casts.arg("x").mustBe(stringValue(), Message.ARG_IS_NOT_OF_MODE, "character");
        casts.arg("table").mustBe(stringValue(), Message.ARG_IS_NOT_OF_MODE, "character");
        casts.arg("noMatch").asIntegerVector().findFirst(RRuntime.INT_NA);
    }

    @Specialization
    protected RIntVector doCharMatch(RAbstractStringVector x, RAbstractStringVector table, int noMatchValue) {
        int[] ans = new int[x.getLength()];
        for (int i = 0; i < x.getLength(); i++) {
            int matchIndex = RRuntime.INT_NA;
            boolean perfect = false;
            final String matchString = x.getDataAt(i);
            for (int j = 0; j < table.getLength(); j++) {
                final String targetString = table.getDataAt(j);
                int matchLength = 0;
                while (matchLength < matchString.length() && (matchString.charAt(matchLength) == targetString.charAt(matchLength))) {
                    matchLength++;
                }
                /*
                 * Try to find an exact match and store its index. If there are multiple exact
                 * matches or there are multiple target strings which have source string as a proper
                 * prefix then store 0.
                 */
                if (matchLength == matchString.length()) {
                    if (targetString.length() == matchLength) {
                        if (perfect) {
                            matchIndex = 0;
                        } else {
                            perfect = true;
                            matchIndex = j + 1;
                        }
                    } else {
                        if (!perfect) {
                            if (matchIndex == RRuntime.INT_NA) {
                                matchIndex = j + 1;
                            } else {
                                matchIndex = 0;
                            }
                        }
                    }
                }
            }
            ans[i] = (matchIndex == RRuntime.INT_NA) ? noMatchValue : matchIndex;
        }
        return RDataFactory.createIntVector(ans, noMatchValue == RRuntime.INT_NA ? RDataFactory.INCOMPLETE_VECTOR : RDataFactory.COMPLETE_VECTOR);
    }
}
