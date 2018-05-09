/*
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates
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
package com.oracle.truffle.r.nodes.objects;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.builtin;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RFunction;

// transcribed from src/library/methods/src/utils.c
public abstract class GetPrimName extends RExternalBuiltinNode.Arg1 {

    static {
        Casts casts = new Casts(GetPrimName.class);
        casts.arg(0).defaultError(RError.Message.GENERIC, "'R_get_primname' called on a non-primitive").mustBe(builtin());
    }

    @Specialization(guards = "f.isBuiltin()")
    protected String getPrimName(RFunction f) {
        return f.getName();
    }
}
