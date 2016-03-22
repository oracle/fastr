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
package com.oracle.truffle.r.library.graphics.core.geometry;

import java.util.stream.DoubleStream;

public final class DoubleCoordinates implements Coordinates {

    private final double[] xCoords;
    private final double[] yCoords;

    public DoubleCoordinates(double[] xCoords, double[] yCoords) {
        this.xCoords = xCoords;
        this.yCoords = yCoords;
    }

    @Override
    public double[] getXCoordinatesAsDoubles() {
        return xCoords;
    }

    @Override
    public double[] getYCoordinatesAsDoubles() {
        return yCoords;
    }

    @Override
    public int[] getXCoordinatesAsInts() {
        return toInt(getXCoordinatesAsDoubles());
    }

    @Override
    public int[] getYCoordinatesAsInts() {
        return toInt(getYCoordinatesAsDoubles());
    }

    private static int[] toInt(double[] doubleArray) {
        return DoubleStream.of(doubleArray).mapToInt(d -> (int) d).toArray();
    }
}
