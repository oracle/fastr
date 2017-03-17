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

import static com.oracle.truffle.r.library.fastrGrid.GridUtils.asDoubleVector;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
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

    public static ViewPortContext fromViewPort(RList viewPort) {
        ViewPortContext result = new ViewPortContext();
        RAbstractDoubleVector x = asDoubleVector(viewPort.getDataAt(ViewPort.VP_XSCALE));
        if (x.getLength() != 2) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "view-port xscale must be vector of size 2");
        }
        result.xscalemin = x.getDataAt(0);
        result.xscalemax = x.getDataAt(1);
        RAbstractDoubleVector y = asDoubleVector(viewPort.getDataAt(ViewPort.VP_YSCALE));
        if (y.getLength() != 2) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "view-port yscale must be vector of size 2");
        }
        result.yscalemin = y.getDataAt(0);
        result.yscalemax = y.getDataAt(1);
        return result;
    }
}
