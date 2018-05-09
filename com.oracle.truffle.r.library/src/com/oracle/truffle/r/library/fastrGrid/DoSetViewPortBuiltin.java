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

import static com.oracle.truffle.r.library.fastrGrid.DoSetViewPort.doSetViewPort;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.builtins.RBehavior;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RNull;

/**
 * On the top of what {@link DoSetViewPort} node does, this node sets the resulting view port as the
 * current view port in the current {@link GridState} instance. This builtin allows us to write some
 * of the grid code in R.
 */
@RBuiltin(name = ".fastr.grid.doSetViewPort", parameterNames = {"vp", "hasParent", "pushing"}, kind = RBuiltinKind.INTERNAL, behavior = RBehavior.COMPLEX)
public abstract class DoSetViewPortBuiltin extends RBuiltinNode.Arg3 {

    static {
        Casts casts = new Casts(DoSetViewPortBuiltin.class);
        casts.arg("vp").mustBe(RList.class);
        casts.arg("hasParent").mustBe(logicalValue()).asLogicalVector().findFirst().map(toBoolean());
        casts.arg("pushing").mustBe(logicalValue()).asLogicalVector().findFirst().map(toBoolean());
    }

    @Specialization
    RNull doIt(RList pushedVP, boolean hasParent, boolean pushing) {
        RList vp = doSetViewPort(pushedVP, hasParent, pushing);
        GridContext.getContext().getGridState().setViewPort(vp);
        return RNull.instance;
    }

    public static DoSetViewPortBuiltin create() {
        return DoSetViewPortBuiltinNodeGen.create();
    }
}
