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
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.graphics.core.geometry;

import java.util.stream.IntStream;

public final class IntCoordinates implements Coordinates {
    private final int[] xCoords;
    private final int[] yCoords;

    public IntCoordinates(int[] xCoords, int[] yCoords) {
        this.xCoords = xCoords;
        this.yCoords = yCoords;
    }

    @Override
    public double[] getXCoordinatesAsDoubles() {
        return toDouble(xCoords);
    }

    @Override
    public double[] getYCoordinatesAsDoubles() {
        return toDouble(yCoords);
    }

    @Override
    public int[] getXCoordinatesAsInts() {
        return xCoords;
    }

    @Override
    public int[] getYCoordinatesAsInts() {
        return yCoords;
    }

    private static double[] toDouble(int[] intArray) {
        return IntStream.of(intArray).mapToDouble(i -> i).toArray();
    }
}
