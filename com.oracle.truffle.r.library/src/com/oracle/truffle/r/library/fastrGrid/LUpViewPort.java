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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;

public abstract class LUpViewPort extends RExternalBuiltinNode.Arg1 {
    @Child private CastNode castParentToViewPort = newCastBuilder().mustBe(RList.class, Message.GENERIC, "cannot pop the top-level viewport ('grid' and 'graphics' output mixed?)").buildCastNode();

    static {
        Casts casts = new Casts(LUpViewPort.class);
        casts.arg(0).mustBe(numericValue()).asIntegerVector().findFirst(1).mustBe(gte(1));
    }

    public static LUpViewPort create() {
        return LUpViewPortNodeGen.create();
    }

    @Specialization
    Object upViewPort(int n) {
        GridState gridState = GridContext.getContext().getGridState();
        RList newViewPort = gridState.getViewPort();
        for (int i = 0; i < n; i++) {
            newViewPort = (RList) castParentToViewPort.doCast(newViewPort.getDataAt(ViewPort.PVP_PARENT));
        }
        gridState.setViewPort(newViewPort);

        // TODO: device changed? => calcViewportTransform for newViewPort
        // TODO: update the clipping region
        return RNull.instance;
    }
}
