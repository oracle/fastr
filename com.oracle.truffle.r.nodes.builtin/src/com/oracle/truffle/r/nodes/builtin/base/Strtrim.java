/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, 1997  Robert Gentleman and Ross Ihaka
 * Copyright (c) 1995-2014, The R Core Team
 * Copyright (c) 2002-2008, The R Foundation
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.util.function.BiFunction;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.ToLowerOrUpper.StringMapNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "strtrim", kind = INTERNAL, parameterNames = {"x", "width"}, behavior = PURE)
public abstract class Strtrim extends RBuiltinNode {

    static {
        Casts casts = new Casts(Strtrim.class);
        casts.arg("x").defaultError(Message.REQUIRES_CHAR_VECTOR, "strtrim()").mustBe(stringValue()).asStringVector(true, true, true);
        casts.arg("width").mustNotBeMissing().mustBe(nullValue().not()).asIntegerVector();
    }

    @Specialization
    protected RStringVector srtrim(RAbstractStringVector x, RAbstractIntVector width,
                    @Cached("create()") StringMapNode mapNode,
                    @Cached("createBinaryProfile()") ConditionProfile fitsProfile) {
        int len = x.getLength();
        int nw = width.getLength();
        if (nw == 0 || nw < len && (len % nw != 0)) {
            CompilerDirectives.transferToInterpreter();
            throw error(RError.Message.INVALID_ARGUMENT, "width");
        }
        for (int i = 0; i < nw; i++) {
            assert RRuntime.INT_NA < 0; // check for NA folded into < 0
            if (width.getDataAt(i) < 0) {
                CompilerDirectives.transferToInterpreter();
                throw error(RError.Message.INVALID_ARGUMENT, "width");
            }
        }
        BiFunction<String, Integer, String> function = (element, i) -> {
            // TODO multibyte character handling
            int w = width.getDataAt(i % nw);
            if (fitsProfile.profile(w >= element.length())) {
                return element;
            } else {
                return substring(element, w);
            }
        };
        return mapNode.apply(x, function);
    }

    @TruffleBoundary
    private static String substring(String element, int w) {
        return element.substring(0, w);
    }
}
