/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.runtime.RVisibility.ON;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.builtins.RBehavior;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;

/**
 * Allows to show the actual location of the source section of a provided function.
 */
@RBuiltin(name = ".fastr.srcinfo", visibility = ON, kind = PRIMITIVE, parameterNames = "fun", behavior = RBehavior.IO)
public abstract class FastRSourceInfo extends RBuiltinNode.Arg1 {

    static {
        Casts casts = new Casts(FastRSourceInfo.class);
        casts.arg("fun").defaultError(RError.Message.GENERIC, "Only functions are allowed.").mustBe(instanceOf(RFunction.class).or(instanceOf(RPairList.class)));
    }

    @Specialization
    @TruffleBoundary
    public Object srcInfo(RFunction fun) {
        return syntaxSrcInfo(fun.getRootNode().getSourceSection());
    }

    @Specialization(guards = "fun.isLanguage()")
    @TruffleBoundary
    public Object srcInfo(RPairList fun) {
        return syntaxSrcInfo(fun.getSourceSection());
    }

    private static Object syntaxSrcInfo(SourceSection source) {
        if (source != null) {
            String path = source.getSource().getPath();
            if (path != null) {
                return path + "#" + source.getStartLine();
            } else if (source.getSource().getURI() != null) {
                return source.getSource().getURI() + "#" + source.getStartLine();
            } else {
                return source.getSource().getName();
            }
        }
        return RNull.instance;
    }

    @Fallback
    public Object srcInfo(@SuppressWarnings("unused") Object o) {
        return RNull.instance;
    }
}
