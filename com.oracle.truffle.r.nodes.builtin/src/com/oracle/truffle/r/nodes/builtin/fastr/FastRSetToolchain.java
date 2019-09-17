/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.io.IOException;
import java.nio.file.StandardCopyOption;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RNull;

@RBuiltin(name = "fastr.setToolchain", visibility = OFF, kind = PRIMITIVE, parameterNames = {"name"}, behavior = COMPLEX)
public abstract class FastRSetToolchain extends RBuiltinNode.Arg1 {

    static {
        Casts casts = new Casts(FastRSetToolchain.class);
        casts.arg("name").asStringVector().findFirst();
    }

    @TruffleBoundary
    @Specialization
    protected RNull setToolchain(String name) {
        String srcConf;
        if ("native".equals(name)) {
            srcConf = "Makeconf.native";
        } else if ("llvm".equals(name)) {
            srcConf = "Makeconf.llvm";
        } else {
            throw error(RError.Message.GENERIC, "Only 'native' or 'llvm' argument values accepted");
        }
        TruffleFile rHome = REnvVars.getRHomeTruffleFile(RContext.getInstance().getEnv());
        TruffleFile etc = rHome.resolve("etc");
        TruffleFile src = etc.resolve(srcConf);
        TruffleFile dst = etc.resolve("Makeconf");
        try {
            src.copy(dst, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RInternalError(String.format("Copying %s over etc/Makeconf failed", srcConf), e);
        }
        return RNull.instance;
    }

}
