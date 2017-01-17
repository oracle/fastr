/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.objects;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.builtin;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RFunction;

// transcribed from src/library/methods/src/utils.c
public abstract class GetPrimName extends RExternalBuiltinNode.Arg1 {

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg(0).defaultError(RError.NO_CALLER, RError.Message.GENERIC, "'R_get_primname' called on a non-primitive").mustNotBeNull().mustBe(builtin());
    }

    @Specialization(guards = "f.isBuiltin()")
    protected String getPrimName(RFunction f) {
        return f.getName();
    }

}
