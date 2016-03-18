/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1998 Ross Ihaka
 * Copyright (c) 1998--2014, The R Core Team
 * Copyright (c) 2002--2010, The R Foundation
 * Copyright (C) 2005--2006, Morten Welinder
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.graphics.core.drawables;

import java.awt.Graphics2D;

import com.oracle.truffle.r.library.graphics.core.geometry.CoordinateSystem;
import com.oracle.truffle.r.library.graphics.core.geometry.Coordinates;

/**
 * Able to draw a polyline on {@link Graphics2D}.
 */
public class PolylineDrawableObject extends CoordinatesDrawableObject {
    public PolylineDrawableObject(CoordinateSystem coordinateSystem, Coordinates coordinates) {
        super(coordinateSystem, coordinates);
    }

    @Override
    public void drawOn(Graphics2D g2) {
        Coordinates coords = getDstCoordinates();
        int[] xCoords = coords.getXCoordinatesAsInts();
        g2.drawPolyline(xCoords, coords.getYCoordinatesAsInts(), xCoords.length);
    }
}
