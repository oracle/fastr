/*
 * Copyright (C) 2001-3 Paul Murrell
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates
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

import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asAbstractContainer;
import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asDoubleVector;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

/**
 * The vectors in this class represent a unit objects, therefore we cannot just have a double value
 * for them. However, the unit object should contain only single value.
 */
public final class ViewPortLocation {
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
        RDoubleVector just = asDoubleVector(viewPort.getDataAt(ViewPort.VP_VALIDJUST));
        if (just.getLength() != 2) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "Unexpected size of layout justification vector.");
        }
        r.hjust = just.getDataAt(0);
        r.vjust = just.getDataAt(1);
        return r;
    }
}
