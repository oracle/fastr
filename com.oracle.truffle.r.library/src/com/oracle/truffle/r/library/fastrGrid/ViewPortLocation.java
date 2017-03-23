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

import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asAbstractContainer;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asDoubleVector;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractDoubleVector;

/**
 * The vectors in this class represent a unit objects, therefore we cannot just have a double value
 * for them. However, the unit object should contain only single value.
 */
public class ViewPortLocation {
    public RAbstractContainer x;
    public RAbstractContainer y;
    public RAbstractContainer width;
    public RAbstractContainer height;
    public double hjust;
    public double vjust;

    public static ViewPortLocation fromViewPort(RList viewPort) {
        ViewPortLocation r = new ViewPortLocation();
        r.x = asAbstractContainer(viewPort.getDataAt(ViewPort.VP_X));
        r.y = asAbstractContainer(viewPort.getDataAt(ViewPort.VP_Y));
        r.width = asAbstractContainer(viewPort.getDataAt(ViewPort.VP_WIDTH));
        r.height = asAbstractContainer(viewPort.getDataAt(ViewPort.VP_HEIGHT));
        RAbstractDoubleVector just = asDoubleVector(viewPort.getDataAt(ViewPort.VP_VALIDJUST));
        if (just.getLength() != 2) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "Unexpected size of layout justification vector.");
        }
        r.hjust = just.getDataAt(0);
        r.vjust = just.getDataAt(1);
        return r;
    }
}
