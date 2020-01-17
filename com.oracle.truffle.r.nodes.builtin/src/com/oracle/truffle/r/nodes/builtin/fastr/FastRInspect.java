/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Specialization;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.constant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RIntVector;
import com.oracle.truffle.r.runtime.data.RNull;
import java.io.IOException;

/**
 * Just a convenient way to inspect values in the Java debugger from the R shell.
 */
@RBuiltin(name = ".fastr.inspect", visibility = OFF, kind = PRIMITIVE, parameterNames = {"...", "inspectVectorData"}, behavior = COMPLEX)
public abstract class FastRInspect extends RBuiltinNode.Arg2 {

    static {
        Casts casts = new Casts(FastRInspect.class);
        casts.arg("inspectVectorData").mapMissing(constant(RRuntime.LOGICAL_FALSE)).mustBe(logicalValue()).asLogicalVector().mustBe(singleElement()).findFirst();
    }

    @Specialization
    public Object call(RArgsValuesAndNames args, byte inspectVectorData) {
        for (int i = 0; i < args.getLength(); i++) {
            Object arg = args.getArgument(i);
            if (RRuntime.fromLogical(inspectVectorData) && RRuntime.hasVectorData(arg)) {
                writeString(((RIntVector) arg).getData().getClass().getName(), true);
            } else {
                writeString(arg.getClass().getName(), true);
            }
        }
        return RNull.instance;
    }

    @TruffleBoundary
    private static void writeString(String msg, boolean nl) {
        try {
            StdConnections.getStdout().writeString(msg, nl);
        } catch (IOException ex) {
            throw RError.error(RError.NO_CALLER, RError.Message.GENERIC, ex.getMessage() == null ? ex : ex.getMessage());
        }
    }
}
