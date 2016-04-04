/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.objects;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RFunction;

// transcribed from src/library/methods/utils.c
public abstract class GetPrimName extends RExternalBuiltinNode.Arg1 {

    @Specialization(guards = "f.isBuiltin()")
    protected String getPrimName(RFunction f) {
        return f.getName();
    }

    @Fallback
    protected Object getPrimName(@SuppressWarnings("unused") Object o) {
        throw RError.error(this, RError.Message.GENERIC, "'R_get_primname' called on a non-primitive");
    }
}
