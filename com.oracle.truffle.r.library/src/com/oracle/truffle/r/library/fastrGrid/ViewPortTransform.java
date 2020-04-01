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

import com.oracle.truffle.r.library.fastrGrid.device.GridDevice;
import com.oracle.truffle.r.runtime.data.RDoubleVector;
import com.oracle.truffle.r.runtime.data.RList;

/**
 * Holds the data of a viewport needed to perform transformations.
 */
public final class ViewPortTransform {
    /**
     * Angle in radians.
     */
    public final double rotationAngle;
    public final double[][] transform;
    public final Size size;

    private ViewPortTransform(double width, double height, double rotationAngle, double[][] transform) {
        this.size = new Size(width, height);
        this.rotationAngle = rotationAngle;
        this.transform = transform;
    }

    public static ViewPortTransform get(RList viewPort, GridDevice device) {
        if (ViewPort.updateDeviceSizeInVP(viewPort, device)) {
            // Note: GnuR sets incremental parameter to true, but don't we need to recalculate
            // the parent(s) as well?
            DoSetViewPort.calcViewportTransform(viewPort, viewPort.getDataAt(ViewPort.PVP_PARENT), true, device, GridState.getInitialGPar(device));
        }
        double width = Unit.cmToInches(GridUtils.asDouble(viewPort.getDataAt(ViewPort.PVP_WIDTHCM)));
        double height = Unit.cmToInches(GridUtils.asDouble(viewPort.getDataAt(ViewPort.PVP_HEIGHTCM)));
        double rotationAngle = GridUtils.asDouble(viewPort.getDataAt(ViewPort.VP_ANGLE));
        RDoubleVector trans = GridUtils.asDoubleVector(viewPort.getDataAt(ViewPort.PVP_TRANS));
        double[][] transform = TransformMatrix.fromFlat(trans);
        return new ViewPortTransform(width, height, rotationAngle, transform);
    }
}
