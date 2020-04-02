/*
 * Copyright (C) 2001-3 Paul Murrell
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates
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
package com.oracle.truffle.r.library.fastrGrid;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.gte;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.nodes.unary.CastNode;
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
