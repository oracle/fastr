/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.builtins.RBehavior.READS_STATE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.ffi.impl.common.LibPaths;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RCompression;
import com.oracle.truffle.r.runtime.RVersionInfo;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI.ESoftVersionNode;

public abstract class VersionFunctions {

    @RBuiltin(name = "Version", kind = INTERNAL, parameterNames = {}, behavior = READS_STATE)
    public abstract static class RVersion extends RBuiltinNode.Arg0 {

        @Specialization
        @TruffleBoundary
        protected Object doRVersion() {
            return RDataFactory.createList(RVersionInfo.listValues(), RDataFactory.createStringVector(RVersionInfo.listNames(), RDataFactory.COMPLETE_VECTOR));
        }
    }

    @RBuiltin(name = "eSoftVersion", kind = INTERNAL, parameterNames = {}, behavior = READS_STATE)
    public abstract static class ExtSoftVersion extends RBuiltinNode.Arg0 {
        @Child private ESoftVersionNode eSoftVersionNode = BaseRFFI.ESoftVersionNode.create();

        static {
            Casts.noCasts(ExtSoftVersion.class);
        }

        @Specialization
        @TruffleBoundary
        protected RStringVector getSymbolInfo() {

            Map<String, String> eSoftVersion = eSoftVersionNode.eSoftVersion();

            List<String> libNames = new ArrayList<>();
            List<String> versions = new ArrayList<>();

            for (Map.Entry<String, String> versionEntry : eSoftVersion.entrySet()) {
                libNames.add(versionEntry.getKey());
                versions.add(versionEntry.getValue());
            }

            // BZIP2
            try {
                versions.add(RCompression.getBz2Version());
                libNames.add("bzip2");
            } catch (IOException e) {
                // ignore
            }

            // BLAS
            libNames.add("BLAS");
            versions.add(LibPaths.getBuiltinLibPath("Rblas"));

            RStringVector names = RDataFactory.createStringVector(libNames.toArray(new String[0]), true);
            return RDataFactory.createStringVector(versions.toArray(new String[0]), true, names);

        }
    }
}
