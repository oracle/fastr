/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 2001-3 Paul Murrell
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.fastrGrid;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.env.REnvironment;

public abstract class LInitGrid extends RExternalBuiltinNode.Arg1 {
    static {
        Casts casts = new Casts(LInitGrid.class);
        casts.arg(0).mustBe(REnvironment.class);
    }

    public static LInitGrid create() {
        return LInitGridNodeGen.create();
    }

    @Specialization
    public Object doEnv(REnvironment gridEnv) {
        GridContext context = GridContext.getContext();
        context.getGridState().init(gridEnv, context.getCurrentDevice());
        return RNull.instance;
    }
}
