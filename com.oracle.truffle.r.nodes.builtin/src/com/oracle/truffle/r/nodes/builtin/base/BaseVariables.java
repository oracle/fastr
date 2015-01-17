/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * The (global) variables defined in the {@code base} package, e.g. {@code .Platform}. The
 * definition and initialization is two-step process handled through the {@link RPackageVariables}
 * class.
 *
 * N.B. Some variables are assigned explicitly in the R source files associated with the base
 * package.
 */
public class BaseVariables implements RPackageVariables.Handler {
    // @formatter:off
    @CompilationFinal private static final String[] VARS = new String[]{
        ".BaseNamespaceEnv", ".GlobalEnv", ".Machine", ".Platform"
    };
    // @formatter:on

    // @formatter:off
    @CompilationFinal private static final String[] PLATFORM_NAMES = new String[] {
        "OS.type", "file.sep", "dynlib.ext", "GUI", "endian", "pkgType", "path.sep", "r_arch"
    };
    // @formatter:on

    // @formatter:off
    private static final String[] FORTRAN_NAMES = new String[] {
        "dchdc", "dqrcf", "dqrdc2", "dqrqty", "dqrqy", "dqrrsd", "dqrxb", "dtrco"
    };
    // @formatter: on

    public static final RStringVector NAME = RDataFactory.createStringVectorFromScalar("name");

private int initialized = -1;

    public BaseVariables() {
        RPackageVariables.registerHandler("base", this);
    }

    @Override
    public void preInitialize(REnvironment baseEnv) {
        // .Platform TODO be more accurate
        String[] platformData = new String[]{"unix", File.separator, ".so", "unknown", "little", "source", File.pathSeparator, ""};
        Object value = RDataFactory.createList(platformData, RDataFactory.createStringVector(PLATFORM_NAMES, RDataFactory.COMPLETE_VECTOR));
        baseEnv.safePut(".Platform", value);
        REnvironment baseNamespaceEnv = REnvironment.baseNamespaceEnv();
        baseEnv.safePut(".BaseNamespaceEnv", baseNamespaceEnv);
        for (String f : FORTRAN_NAMES) {
            baseNamespaceEnv.safePut(".F_" + f, RDataFactory.createList(new String[]{f}, NAME));
        }
    }

    public void initialize(REnvironment env) {
        if (initialized > 0) {
            return;
        } else if (initialized < 0) {
            for (String var : VARS) {
                Object value = null;
                switch (var) {
                    case ".GlobalEnv":
                        value = REnvironment.globalEnv();
                        break;
                    case ".Machine":
                        value = createMachine();
                        break;
                    default:
                        continue;
                }
                env.safePut(var, value);
            }
        } else {
            // nothing in this phase
        }
        initialized++;
    }

    // @formatter:off
    @CompilationFinal private static final String[] MACHINE_NAMES = new String[] {
        "double.eps",            "double.neg.eps",        "double.xmin",
        "double.xmax",           "double.base",           "double.digits",
        "double.rounding",       "double.guard",          "double.ulp.digits",
        "double.neg.ulp.digits", "double.exponent",       "double.min.exp",
        "double.max.exp",        "integer.max",           "sizeof.long",
        "sizeof.longlong",       "sizeof.longdouble",     "sizeof.pointer"
    };
    // @formatter:on

    private static RList createMachine() {
        Object[] values = new Object[MACHINE_NAMES.length];
        RAccuracyInfo acc = RAccuracyInfo.get();
        values[0] = acc.eps;
        values[1] = acc.epsneg;
        values[2] = acc.xmin;
        values[3] = acc.xmax;
        values[4] = acc.ibeta;
        values[5] = acc.it;
        values[6] = acc.irnd;
        values[7] = acc.ngrd;
        values[8] = acc.machep;
        values[9] = acc.negep;
        values[10] = acc.iexp;
        values[11] = acc.minexp;
        values[12] = acc.maxexp;
        values[13] = Integer.MAX_VALUE;
        // TODO platform specific
        values[14] = 8;
        values[15] = 8;
        values[16] = 16;
        values[17] = 8;
        return RDataFactory.createList(values, RDataFactory.createStringVector(MACHINE_NAMES, RDataFactory.COMPLETE_VECTOR));
    }

}
