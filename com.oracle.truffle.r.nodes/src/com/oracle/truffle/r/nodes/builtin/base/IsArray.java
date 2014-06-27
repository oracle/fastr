/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "is.array", kind = PRIMITIVE)
public abstract class IsArray extends RBuiltinNode {

    @Specialization
    public byte isType(RAbstractVector vector) {
        controlVisibility();
        return RRuntime.asLogical(vector.isArray());
    }

    @Specialization
    public byte isType(@SuppressWarnings("unused") RNull arg) {
        controlVisibility();
        return RRuntime.FALSE;
    }

    @Specialization
    public byte isType(@SuppressWarnings("unused") RFunction arg) {
        controlVisibility();
        return RRuntime.FALSE;
    }
}
