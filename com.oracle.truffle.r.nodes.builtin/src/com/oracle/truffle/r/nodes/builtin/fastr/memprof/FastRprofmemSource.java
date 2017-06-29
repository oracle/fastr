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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.fastr.memprof.FastRprofmem.castSnapshotArg;
import static com.oracle.truffle.r.nodes.builtin.fastr.memprof.FastRprofmem.castViewArg;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.IO;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.instrument.memprof.MemAllocProfilerPaths;

@RBuiltin(name = ".fastr.profmem.source", visibility = OFF, kind = PRIMITIVE, parameterNames = {"id", "view", "snapshot"}, behavior = IO)
public abstract class FastRprofmemSource extends RBuiltinNode.Arg3 {

    static {
        Casts casts = new Casts(FastRprofmemSource.class);
        casts.arg("id").asIntegerVector().mustBe(singleElement()).findFirst().replaceNA(Integer.MAX_VALUE);
        castViewArg(casts);
        castSnapshotArg(casts);
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RRuntime.INT_NA, FastRprofmem.STACKS_VIEW, RNull.instance};
    }

    @Specialization
    @TruffleBoundary
    public Object showSource(int entryId, String view, TruffleObject snapshotTO) {
        MemAllocProfilerPaths paths = MemAllocProfilerPaths.fromTruffleObject(snapshotTO);
        return showSource(entryId, view, paths);

    }

    private static Object showSource(int entryId, String view, MemAllocProfilerPaths snap) {
        MemAllocProfilerPaths snapshot = snap;
        if (FastRprofmem.HOTSPOTS_VIEW.equals(view)) {
            snapshot = snapshot.toHotSpots();
        }

        FastRprofmem.getProfilerPrinter().source(snapshot, entryId);

        return RNull.instance;
    }

}
