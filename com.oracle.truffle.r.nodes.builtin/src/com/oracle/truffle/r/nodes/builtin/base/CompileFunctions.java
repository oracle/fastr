/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;

/**
 * FastR does not byte-compile, obviously, but these keep the package installation code happy.
 */

public class CompileFunctions {
    @RBuiltin(name = "compilePKGS", kind = INTERNAL, parameterNames = "enable", behavior = PURE)
    public abstract static class CompilePKGS extends RBuiltinNode {

        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("enable").asIntegerVector().findFirst(0);
        }

        @Specialization
        protected byte compilePKGS(@SuppressWarnings("unused") int enable) {
            return 0;
        }
    }

    @RBuiltin(name = "enableJIT", kind = INTERNAL, parameterNames = "level", behavior = PURE)
    public abstract static class EnableJIT extends RBuiltinNode {
        @Override
        protected void createCasts(CastBuilder casts) {
            casts.arg("level").asIntegerVector().findFirst(0);
        }

        @Specialization
        protected byte enableJIT(@SuppressWarnings("unused") int level) {
            return 0;
        }

    }
}
