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

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import com.oracle.truffle.r.runtime.data.RDoubleVector;

public final class CoordinatesFactory {
    private CoordinatesFactory() {
    }

    public static DoubleCoordinates createWithSameX(double x, double[] yCoords) {
        double[] xCoords = createdFilledArray(x, yCoords.length);
        return new DoubleCoordinates(xCoords, yCoords);
    }

    public static DoubleCoordinates createWithSameY(double[] xCoords, double y) {
        double[] yCoords = createdFilledArray(y, xCoords.length);
        return new DoubleCoordinates(xCoords, yCoords);
    }

    private static double[] createdFilledArray(double value, int size) {
        double[] result = new double[size];
        Arrays.fill(result, value);
        return result;
    }

    public static DoubleCoordinates createByXYVector(RDoubleVector xyVector) {
        int length = xyVector.getLength();
        double[] xCoords = IntStream.range(0, length).filter(i -> i % 2 == 0).mapToDouble(xyVector::getDataAt).toArray();
        double[] yCoords = IntStream.range(0, length).filter(i -> i % 2 != 0).mapToDouble(xyVector::getDataAt).toArray();
        return new DoubleCoordinates(xCoords, yCoords);
    }

    public static DoubleCoordinates createByXYPairs(double[] xyPairs) {
        int length = xyPairs.length;
        double[] xCoords = IntStream.range(0, length).filter(i -> i % 2 == 0).mapToDouble(i -> xyPairs[i]).toArray();
        double[] yCoords = IntStream.range(0, length).filter(i -> i % 2 != 0).mapToDouble(i -> xyPairs[i]).toArray();
        return new DoubleCoordinates(xCoords, yCoords);
    }

    public static DoubleCoordinates withRatioAndShift(Coordinates coordinates, double ratio, double shift) {
        return withRatioAndShift(coordinates, ratio, shift, shift);
    }

    public static DoubleCoordinates withRatioAndShift(Coordinates coordinates, double ratio, double xShift, double yShift) {
        double[] convertedX = applyRatioAndShiftTo(coordinates.getXCoordinatesAsDoubles(), ratio, xShift);
        double[] convertedY = applyRatioAndShiftTo(coordinates.getYCoordinatesAsDoubles(), ratio, yShift);
        return new DoubleCoordinates(convertedX, convertedY);
    }

    private static double[] applyRatioAndShiftTo(double[] coordinates, double ratio, double shift) {
        return DoubleStream.of(coordinates).map(d -> d * ratio + shift).toArray();
    }
}
