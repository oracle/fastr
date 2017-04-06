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
public abstract class DoSetViewPortBuiltin extends RBuiltinNode {

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
