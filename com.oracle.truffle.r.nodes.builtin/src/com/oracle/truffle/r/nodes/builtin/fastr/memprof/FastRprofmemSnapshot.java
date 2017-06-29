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
package com.oracle.truffle.r.nodes.builtin.fastr.memprof;

import static com.oracle.truffle.r.nodes.builtin.fastr.memprof.FastRprofmem.castViewArg;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.instrument.memprof.MemAllocProfilerPaths;
import com.oracle.truffle.r.runtime.instrument.memprof.MemAllocProfilerStacks;

@RBuiltin(name = ".fastr.profmem.snapshot", visibility = OFF, kind = PRIMITIVE, parameterNames = {"name", "view"}, behavior = IO)
public abstract class FastRprofmemSnapshot extends RBuiltinNode.Arg2 {

    static {
        Casts casts = new Casts(FastRprofmemSnapshot.class);
        casts.arg("name").asStringVector().mustBe(singleElement()).findFirst();
        castViewArg(casts);
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RNull.instance, FastRprofmem.STACKS_VIEW};
    }

    @Specialization
    @TruffleBoundary
    public TruffleObject makeSnapshot(String name, String view) {
        MemAllocProfilerPaths snapshot = MemAllocProfilerStacks.getInstance().getStackPaths().getOrMakeSnapshot(name);

        if (FastRprofmem.HOTSPOTS_VIEW.equals(view)) {
            snapshot = snapshot.toHS();
        }

        return snapshot.toTruffleObject();
    }

}
