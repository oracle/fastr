/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base.foreign;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.emptyIntegerVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.missingValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.nullValue;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;

public abstract class CairoProps extends RExternalBuiltinNode.Arg1 {

    static {
        Casts casts = new Casts(CairoProps.class);
        casts.arg(0).returnIf(nullValue().or(missingValue()), emptyIntegerVector()).asIntegerVector();
    }

    @Specialization
    protected byte cairoProps(@SuppressWarnings("unused") RAbstractIntVector param) {
        return RRuntime.LOGICAL_FALSE;
    }
}
