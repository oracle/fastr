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

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.numericValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.size;
import static com.oracle.truffle.r.nodes.builtin.casts.fluent.CastNodeBuilder.newCastBuilder;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.unary.CastNode;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;

/**
 * The vectors in this class represent a unit objects, therefore we cannot just have a double value
 * for them. However, the unit object should contain only single value.
 */
public class ViewPortLocation {
    public RAbstractDoubleVector x;
    public RAbstractDoubleVector y;
    public RAbstractDoubleVector width;
    public RAbstractDoubleVector height;
    public double hjust;
    public double vjust;

    public static final class VPLocationFromVPNode extends Node {
        @Child private CastNode castDoubleVector = newCastBuilder().mustBe(numericValue()).asDoubleVector().buildCastNode();
        @Child private CastNode castJustVector = newCastBuilder().mustBe(numericValue()).asDoubleVector().mustBe(size(2)).buildCastNode();

        public ViewPortLocation execute(RList viewPort) {
            ViewPortLocation r = new ViewPortLocation();
            r.x = vec(viewPort.getDataAt(ViewPort.VP_X));
            r.y = vec(viewPort.getDataAt(ViewPort.VP_Y));
            r.width = vec(viewPort.getDataAt(ViewPort.VP_WIDTH));
            r.height = vec(viewPort.getDataAt(ViewPort.VP_HEIGHT));
            RAbstractDoubleVector just = (RAbstractDoubleVector) castJustVector.execute(viewPort.getDataAt(ViewPort.VP_VALIDJUST));
            r.hjust = just.getDataAt(0);
            r.vjust = just.getDataAt(1);
            return r;
        }

        private RAbstractDoubleVector vec(Object val) {
            return (RAbstractDoubleVector) castDoubleVector.execute(val);
        }
    }
}
