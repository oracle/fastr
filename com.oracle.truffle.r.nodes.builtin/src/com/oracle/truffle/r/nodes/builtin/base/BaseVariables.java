/*
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.R-project.org/Licenses/
 */
package com.oracle.truffle.r.nodes.builtin.base;

import java.io.File;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.r.runtime.RAccuracyInfo;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Built-in initialization of some crucial {@code base} package variables.
 *
 * From GnuR platform.c
 */
public class BaseVariables {
    // @formatter:off
    @CompilationFinal(dimensions = 1) private static final String[] PLATFORM_NAMES = new String[] {
        "OS.type", "file.sep", "dynlib.ext", "GUI", "endian", "pkgType", "path.sep", "r_arch"
    };
    // @formatter:on

    public static void initialize(REnvironment baseEnv) {
        // .Platform TODO be more accurate
        Object[] platformData = new Object[]{"unix", File.separator, ".so", "unknown", "little", "source", File.pathSeparator, ""};
        Object value = RDataFactory.createList(platformData, RDataFactory.createStringVector(PLATFORM_NAMES, RDataFactory.COMPLETE_VECTOR));
        baseEnv.safePut(".Platform", value);
        REnvironment baseNamespaceEnv = REnvironment.baseNamespaceEnv();
        baseEnv.safePut(".BaseNamespaceEnv", baseNamespaceEnv);
        baseEnv.safePut(".Machine", createMachine());
    }

    // @formatter:off
    @CompilationFinal(dimensions = 1) private static final String[] MACHINE_NAMES = new String[] {
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
