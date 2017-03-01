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

import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;

public final class ViewPortContext {
    public double xscalemin;
    public double yscalemin;
    public double xscalemax;
    public double yscalemax;

    private ViewPortContext() {
    }

    public static ViewPortContext createDefault() {
        ViewPortContext result = new ViewPortContext();
        result.xscalemin = 0;
        result.yscalemin = 0;
        result.xscalemax = 1;
        result.yscalemax = 1;
        return result;
    }

    public static final class VPContextFromVPNode extends Node {
        @Child private CastNode castVector = newCastBuilder().asDoubleVector().mustBe(Predef.size(2)).buildCastNode();

        public ViewPortContext execute(RList viewPort) {
            ViewPortContext result = new ViewPortContext();
            RAbstractDoubleVector x = castVec(viewPort.getDataAt(ViewPort.VP_XSCALE));
            result.xscalemin = x.getDataAt(0);
            result.xscalemax = x.getDataAt(1);
            RAbstractDoubleVector y = castVec(viewPort.getDataAt(ViewPort.VP_YSCALE));
            result.yscalemin = y.getDataAt(0);
            result.yscalemax = y.getDataAt(1);
            return result;
        }

        private RAbstractDoubleVector castVec(Object val) {
            return (RAbstractDoubleVector) castVector.execute(val);
        }
    }
}
