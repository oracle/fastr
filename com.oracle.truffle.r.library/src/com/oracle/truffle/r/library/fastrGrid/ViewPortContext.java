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

import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asDoubleVector;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;

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

    public static ViewPortContext fromViewPort(RList viewPort) {
        ViewPortContext result = new ViewPortContext();
        RDoubleVector x = asDoubleVector(viewPort.getDataAt(ViewPort.VP_XSCALE));
        if (x.getLength() != 2) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "view-port xscale must be vector of size 2");
        }
        result.xscalemin = x.getDataAt(0);
        result.xscalemax = x.getDataAt(1);
        RDoubleVector y = asDoubleVector(viewPort.getDataAt(ViewPort.VP_YSCALE));
        if (y.getLength() != 2) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "view-port yscale must be vector of size 2");
        }
        result.yscalemin = y.getDataAt(0);
        result.yscalemax = y.getDataAt(1);
        return result;
    }
}
