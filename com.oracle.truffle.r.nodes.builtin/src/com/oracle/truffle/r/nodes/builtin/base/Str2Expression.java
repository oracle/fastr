/*
 *  Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
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
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.Engine.ParsedExpression;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RStringVector;

@RBuiltin(name = "str2expression", kind = INTERNAL, parameterNames = {"text"}, behavior = PURE)
public abstract class Str2Expression extends RBuiltinNode.Arg1 {

    static {
        Casts casts = new Casts(Str2Expression.class);
        casts.arg("text").mustNotBeMissing().asStringVector();
    }

    @TruffleBoundary
    @Specialization
    protected Object doIt(RStringVector text) {
        String mergedText = mergeLinesFromText(text);
        Source source = Source.newBuilder(RRuntime.R_LANGUAGE_ID, mergedText, "<str2expression>").build();
        ParsedExpression parseRes = RContext.getEngine().parse(source, false);
        return parseRes.getExpression();
    }

    private static String mergeLinesFromText(RStringVector text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.getLength(); i++) {
            sb.append(text.getDataAt(i));
            if (i < text.getLength() - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
